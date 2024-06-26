package org.jenkinsci.plugins.gitclient.verifier;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;
import java.io.File;
import org.jenkinsci.remoting.SerializableOnlyOverRemoting;

public abstract class HostKeyVerifierFactory implements SerializableOnlyOverRemoting {

    /**
     * @return Implementation of verifier for Command line git
     */
    public abstract AbstractCliGitHostKeyVerifier forCliGit(TaskListener listener);

    /**
     * @return Implementation of verifier for JGit
     */
    public abstract AbstractJGitHostKeyVerifier forJGit(TaskListener listener);

    @NonNull
    public File getKnownHostsFile() {
        return SshHostKeyVerificationStrategy.JGIT_KNOWN_HOSTS_FILE;
    }
}
