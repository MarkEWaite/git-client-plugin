package org.jenkinsci.plugins.gitclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class SubmoduleTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private final String gitImplementation;
    private final String initialBranch;
    private final String finalBranch;

    public SubmoduleTest(String implementation, String initialBranch, String finalBranch) {
        gitImplementation = implementation;
        this.initialBranch = initialBranch;
        this.finalBranch = finalBranch;
    }

    private LocalWorkspace ws;

    /*
     * Starting branch to consider:
     * - master
     * - tests/getSubmodules
     * - tests/noSubmodules
     *
     * Ending branch to consider:
     * - master
     * - tests/getSubmodules
     * - tests/noSubmodules
     *
     * Operations to consider in submodule tests:
     * - fetch/clone
     * - submodule init
     * - submodule update
     * - clean
     * - modify workspace files
     *
     * File states to consider in submodule tests (modify workspace files):
     * - tracked and unmodified
     * - tracked and modified
     * - untracked
     */
    @Before
    public void createWorkspace() throws Exception {
        /* Should copy ntp, firewall, and sshkeys repos into a reference repo */
        ws = new LocalWorkspace(tempFolder.newFolder(), gitImplementation);
    }

    @Parameterized.Parameters(name = "impl:{0} start:{1} end:{2}")
    public static Collection testScenarios() {
        List<Object[]> testArgs = new ArrayList<>();
        String[] gitImplementations = new String[]{"git" , "jgit", "jgitapache"};
        String[] branches = new String[]{"tests/getSubmodules", "tests/notSubmodules", "master"};
        for (String implementation : gitImplementations) {
            for (String initialBranch : branches) {
                for (String finalBranch : branches) {
                    Object[] arg = {implementation, initialBranch, finalBranch};
                    testArgs.add(arg);
                }
            }
        }
        return testArgs;
    }

    @Test
    public void checkoutBranch() throws Exception {
        ws.cloneMirror();
        GitClient git = ws.getGitClient();
        git.checkoutBranch(initialBranch, "origin/" + initialBranch);
        // git.submoduleInit();
        // git.submoduleUpdate().ref(ws.repoPath()).execute();
        git.checkoutBranch(finalBranch, "origin/" + finalBranch);
        // git.submoduleInit();
        // git.submoduleUpdate().execute();
    }

    @Test
    public void checkout() throws Exception {
        ws.cloneMirror();
        GitClient git = ws.getGitClient();
        if (!initialBranch.equals("master")) {
            /* Fails to checkout master branch, since it already exists */
            git.checkout("origin/" + initialBranch, initialBranch);
        }
        // git.submoduleInit();
        // git.submoduleUpdate().execute();
        if (!initialBranch.equals(finalBranch) && !finalBranch.equals("master")) {
            ObjectId branchId = git.revParse("origin/" + finalBranch);
            git.checkout(branchId.getName(), finalBranch);
        }
        // git.submoduleInit();
        // git.submoduleUpdate().execute();
    }
}
