package org.jenkinsci.plugins.gitclient;

import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitTool;
import hudson.slaves.DumbSlave;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

import static org.jenkinsci.plugins.gitclient.CliGitAPIImpl.TIMEOUT_LOG_PREFIX;

/**
 * Created by a165807 on 2016-11-01.
 */
public class CliGitAPIImplWithJenkinsRuleTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void test_global_config_timeout() throws Exception {
        GitTool.onLoaded();
        GitTool gitTool = GitTool.getDefaultInstallation();
        Assert.assertNotNull(gitTool);
        Assert.assertNotNull(gitTool.getDescriptor());
        gitTool.getDescriptor().setGitDefaultTimeout(999);
        DumbSlave slave = j.createSlave();
        slave.setMode(Node.Mode.EXCLUSIVE);
        hudson.EnvVars env = new hudson.EnvVars();

        LogHandler handler = new LogHandler();
        handler.setLevel(Level.ALL);
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);

        TaskListener listener = new hudson.util.LogTaskListener(logger, Level.ALL);

        final String FORK_URL = "https://github.com/MarkEWaite/git-client-plugin";
        CliGitAPIImpl git = new CliGitAPIImpl("git", folder.getRoot(), listener, env);

        /* Confirm common git commands honor default timeout */
        git.init_().workspace(folder.getRoot().getAbsolutePath()).execute();
        git.clean();
        git.clone_().tags(true).shallow().depth(1).url("https://github.com/jenkinsci/git-client-plugin").reference(".").execute();
        git.checkout().branch("master").ref("origin/master").deleteBranchIfExist(true).execute();
        git.tag("tag-with-timeout", "tag created with a timeout");
        git.addNote("note with timeout", "ref/notes");
        git.addRemoteUrl("fork", FORK_URL);
        git.branch("master-branch-with-timeout");
        git.checkout().branch("git-client-1.21.0-with-timeout").ref("git-client-1.21.0").deleteBranchIfExist(true).execute();
        git.branch("some-other-branch-with-timeout");
        git.deleteBranch("master-branch-with-timeout");
        git.deleteTag("tag-with-timeout");

        ObjectId master = git.revParse("master");
        assertThat(master, is(not(ObjectId.zeroId())));

        Set<Branch> branches = git.getBranches();
        assertThat(branches, is(not(empty())));

        assertThat(git.getRemoteUrl("fork"), is(FORK_URL));

        assertTimeoutValues(handler, 999);
    }

    private void assertTimeoutValues(LogHandler handler, int timeoutValue) {
        int timeoutCount = 0;
        for (String message : handler.getMessages()) {
            if (message.contains("git ")) {
                /* All timeout values should use the defined value */
                assertThat(message, containsString(TIMEOUT_LOG_PREFIX + timeoutValue));
                timeoutCount++;
            }
        }
        /* Assert that timeout is set */
        assertThat(timeoutCount, greaterThan(0));
    }
}
