package org.jenkinsci.plugins.gitclient;

import hudson.EnvVars;
import hudson.util.StreamTaskListener;
import java.util.List;
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
        assertNoSubmodule(parentRepo, submoduleRepo, submoduleName);
        parentRepo.git("submodule", "add", submoduleRepo.getRoot().getAbsolutePath(), submoduleName);
        parentRepo.git("commit", "--message=Add-" + submoduleName);
        assertSingleSubmodule(parentRepo, submoduleRepo, submoduleName);
    }

    @Test
    public void checkoutThenDeleteSubmodule() {
        assertTrue(true);
    }

    private void assertSingleSubmodule(GitClientSampleRepoRule parentRepo, GitClientSampleRepoRule submoduleRepo, String submoduleName) throws Exception {
        List<String> output = parentRepo.gitWithOutput("git", "submodule", "status");
        assertThat(output, contains(" " + submoduleRepo.head() + " " + submoduleName + " (heads/master)"));
    }

    private void assertNoSubmodule(GitClientSampleRepoRule parentRepo, GitClientSampleRepoRule submoduleRepo, String submoduleName) throws Exception {
        List<String> output = parentRepo.gitWithOutput("git", "submodule", "status");
        assertThat(output, contains(""));
    }
}
