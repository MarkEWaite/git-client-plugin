package org.jenkinsci.plugins.gitclient;

import hudson.EnvVars;
import hudson.util.StreamTaskListener;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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

    private static final AtomicInteger COUNTER = new AtomicInteger();

    public static String createID() {
        return String.valueOf(COUNTER.getAndIncrement());
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
        // Unexpected failure on CentOS 6 with git 1.7.1
        assertSingleSubmodule(parentGitClient, submoduleRepo.head(), submoduleName);
    }

    @Test
    public void checkoutThenDeleteSubmodule() {
        // See GitAPITestCase#test_submodule_checkout_and_clean_transitions
        assertTrue(true);
    }

    private void assertSingleSubmodule(GitClient parentGitClient, String submoduleHead, String submoduleName) throws Exception {
        CliGitCommand parentGitCommand = new CliGitCommand(parentGitClient);
        String[] output = parentGitCommand.run("submodule", "status");
        assertThat(Arrays.asList(output), contains(" " + submoduleHead + " " + submoduleName + " (heads/master)"));
    }

    private void assertNoSubmodule(GitClient parentGitClient) throws Exception {
        CliGitCommand parentGitCommand = new CliGitCommand(parentGitClient);
        String[] output = parentGitCommand.run("submodule", "status");
        assertThat(Arrays.asList(output), contains(""));
    }
}
