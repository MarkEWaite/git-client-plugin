package org.jenkinsci.plugins.gitclient;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;

import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.rules.TemporaryFolder;

/**
 * Test that addition and deletion of submodules within a branch are correctly
 * reflected in the checkout used by jobs.
 *
 * see JENKINS-38860 and JENKINS-41553.
 *
 * @author Mark Waite
 */
public class SubmoduleChangeTest {

    @Rule
    public GitClientSampleRepoRule submoduleRepo = new GitClientSampleRepoRule();

    @Rule
    public GitClientSampleRepoRule parentRepo = new GitClientSampleRepoRule();

    private GitClient parentGitClient;
    private String submoduleName;
    private String parentCommitSHA1;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private File repoRoot = null;

    private final String gitImplName = "git";

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static String createID() {
        return String.valueOf(COUNTER.getAndIncrement());
    }

    @BeforeClass
    public static void setCliGitDefaults() throws Exception {
        /* Command line git commands fail unless certain default values are set */
        CliGitCommand gitCmd = new CliGitCommand(null);
        gitCmd.setDefaults();
    }

    @Before
    public void addSubmoduleRepoToParent() throws Exception {
        // Create two sample repos, add one as submodule of the other
        submoduleRepo.init();
        submoduleName = "submodule-" + createID();
        parentRepo.init();
        parentGitClient = Git.with(StreamTaskListener.fromStderr(), new EnvVars()).in(parentRepo.getRoot()).getClient();
        assertNoSubmodule(parentGitClient);
        parentRepo.git("submodule", "add", submoduleRepo.getRoot().getAbsolutePath(), submoduleName);
        parentRepo.git("commit", "--message=Add-" + submoduleName);
        parentCommitSHA1 = parentRepo.head();
        // Unexpected failure on CentOS 6 with git 1.7.1
        assertSingleSubmodule(parentGitClient, submoduleRepo.head(), submoduleName);
    }

    private int lastFetchPath = -1;
    private final Random random = new Random();

    private void fetch(GitClient client, String remote, String firstRefSpec, String... optionalRefSpecs) throws Exception {
        List<RefSpec> refSpecs = new ArrayList<>();
        RefSpec refSpec = new RefSpec(firstRefSpec);
        refSpecs.add(refSpec);
        for (String refSpecString : optionalRefSpecs) {
            refSpecs.add(new RefSpec(refSpecString));
        }
        lastFetchPath = random.nextInt(2);
        switch (lastFetchPath) {
            default:
            case 0:
                if (remote.equals("origin")) {
                    /* If remote == "origin", randomly use default remote */
                    remote = random.nextBoolean() ? remote : null;
                }
                client.fetch(remote, refSpecs.toArray(new RefSpec[0]));
                break;
            case 1:
                URIish repoURL = new URIish(client.getRepository().getConfig().getString("remote", remote, "url"));
                boolean fetchTags = random.nextBoolean();
                boolean pruneBranches = random.nextBoolean();
                if (pruneBranches) {
                    client.fetch_().from(repoURL, refSpecs).tags(fetchTags).prune().execute();
                } else {
                    client.fetch_().from(repoURL, refSpecs).tags(fetchTags).execute();
                }
                break;
        }
    }

    /*
     * JENKINS-38860, JENKINS-41553, and JENKINS-43977 report that changes to
     * the submodule structure in a branch are not applied correctly to the
     * checkout after those structure changes were made on the remote repo.
     *
     * Steps:
     * 1. Create a repository with a branch that references a submodule
     * 2. Checkout branch from repository, confirm expected structure
     * 3. Delete the submodule in the branch, add another submodule
     * 4. Checkout branch from repository, confirm modified structure
     *
     * @throws java.lang.Exception on error
     */
    @Test
    public void checkoutAfterSubmoduleStructureChange() throws Exception {
        /**
         * Step 1 is completed in @Before method.
         *
         * Step 2 - checkout branch and update submodule
         */
        repoRoot = tempFolder.newFolder();
        GitClient gitClient = Git.with(TaskListener.NULL, new EnvVars()).in(repoRoot).using(gitImplName).getClient();
        File gitDir = gitClient.getRepository().getDirectory();
        assertFalse("Already found " + gitDir, gitDir.isDirectory());
        gitClient.init_().workspace(repoRoot.getAbsolutePath()).execute();
        assertTrue("Missing " + gitDir, gitDir.isDirectory());
        gitClient.setRemoteUrl("origin", parentRepo.getRoot().getAbsolutePath());
        fetch(gitClient, "origin", "+refs/heads/*:refs/remotes/origin/*");
        gitClient.checkoutBranch("master", parentCommitSHA1);
        gitClient.submoduleInit();
        gitClient.submoduleUpdate().execute();
        assertSingleSubmodule(gitClient, submoduleRepo.head(), submoduleName);

        /*
         * Step 3a - delete submodule in the parent repo's branch
         * See https://stackoverflow.com/questions/1260748/how-do-i-remove-a-submodule
         * mv submoduleName submoduleName.tmp
         * git submodule deinit -f submoduleName
         * rm -rf .git/modules/submoduleName
         * git rm -rf submoduleName
         * git commit -a -m "Remove submodule submoduleName"
         */
        File submoduleDir = new File(parentRepo.getRoot(), submoduleName);
        assertTrue("Did not find submodule dir " + submoduleDir.getName(), submoduleDir.exists());
        File submoduleDirTmp = new File(parentRepo.getRoot(), submoduleName + ".tmp");
        submoduleDir.renameTo(submoduleDirTmp);
        assertFalse("Failed to rename submodule dir " + submoduleDir.getName(), submoduleDir.exists());
        assertTrue("Did not find renamed submodule dir " + submoduleDirTmp.getName(), submoduleDirTmp.exists());
        parentRepo.git("submodule", "deinit", "-f", submoduleName);
        File parentModulesDir = new File(parentRepo.getRoot(), ".git/modules/" + submoduleName);
        assertTrue("Did not find " + parentModulesDir.getAbsolutePath(), parentModulesDir.exists());
        FileUtils.deleteDirectory(parentModulesDir);
        assertFalse("Did not delete " + parentModulesDir.getAbsolutePath(), parentModulesDir.exists());
        parentRepo.git("rm", "-rf", submoduleName);
        parentRepo.git("commit", "-a", "-m", "Remove submodule " + submoduleName);
        parentCommitSHA1 = parentRepo.head();

        fetch(gitClient, "origin", "+refs/heads/*:refs/remotes/origin/*");
        gitClient.checkoutBranch("master", parentCommitSHA1);
        // assertNoSubmodule(gitClient);  // fails - checkoutBranch does not make all necessary changes

        /*
         * Step 3b - add a submodule in parent repo's branch
         */

 /*
         * Step 4 Checkout branch, confirm modified structure
         */
    }

    private void assertSingleSubmodule(GitClient parentGitClient, String submoduleHead, String submoduleName) throws Exception {
        CliGitCommand parentGitCommand = new CliGitCommand(parentGitClient);
        String[] output = parentGitCommand.run("submodule", "status");
        assertThat(Arrays.asList(output), contains(" " + submoduleHead + " " + submoduleName + " (heads/master)"));
        // Assert that .gitmodules contains only expected values
        // Assert that .git/modules direcfory contains only expected values
    }

    private void assertNoSubmodule(GitClient parentGitClient) throws Exception {
        CliGitCommand parentGitCommand = new CliGitCommand(parentGitClient);
        String[] output = parentGitCommand.run("submodule", "status");
        assertThat(Arrays.asList(output), contains(""));
        // Assert that .git/modules direcfory is empty
        File gitModulesDir = new File(parentGitClient.getRepository().getDirectory(), "modules");
        if (gitModulesDir.exists()) {
            assertThat("Submodules not removed", Arrays.asList(gitModulesDir.list()), is(empty()));
        }
        // Assert that .gitmodules does not exist or is empty
    }
}
