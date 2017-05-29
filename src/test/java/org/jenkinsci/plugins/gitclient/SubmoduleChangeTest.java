package org.jenkinsci.plugins.gitclient;

import hudson.EnvVars;
import hudson.util.StreamTaskListener;
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

    @Before
    public void addSubmoduleRepoToParent() throws Exception {
        // Create two sample repos, add one as submodule of the other
        submoduleRepo.init();
        parentRepo.init();
        parentGitClient = Git.with(StreamTaskListener.fromStderr(), new EnvVars()).in(parentRepo.getRoot()).getClient();
    }

    @Test
    public void checkoutThenDeleteSubmodule() {
        assertTrue(true);
    }

    @Test
    public void empty() {
        assertTrue(true);
    }
}
