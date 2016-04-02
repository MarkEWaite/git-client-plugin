package org.jenkinsci.plugins.gitclient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.TemporaryDirectoryAllocator;

@RunWith(Parameterized.class)
public class SubmoduleTest {

    private final TemporaryDirectoryAllocator temporaryDirectoryAllocator;
    private final String gitImplementation;
    private final String initialBranch;
    private final String finalBranch;

    public SubmoduleTest(String implementation, String initialBranch, String finalBranch) {
        temporaryDirectoryAllocator = new TemporaryDirectoryAllocator();
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
        File repo = temporaryDirectoryAllocator.allocate();
        ws = new LocalWorkspace(repo, gitImplementation);
    }

    @After
    public void removeWorkspace() throws Exception {
        try {
            temporaryDirectoryAllocator.dispose();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    @Parameterized.Parameters(name = "impl:{0} initial-branch:{1} final-branch:{2}")
    public static Collection testScenarios() {
        List<Object[]> testArgs = new ArrayList<Object[]>();
        String[] gitImplementations = new String[]{"git", "jgit"};
        String[] branches = new String[]{"master", "tests/getSubmodules", "tests/noSubmodules"};
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
    public void localMirror() throws Exception {
        String mirrorName = ws.localMirror();
        File mirrorDir = new File(mirrorName);
        assertTrue("Mirror dir " + mirrorDir + " does not exist", mirrorDir.exists());
    }

    @Test
    @Ignore("Known to fail, work in progress")
    public void checkoutBranch() throws Exception {
        ws.cloneMirror();
        ws.getGitClient().checkoutBranch(initialBranch, "origin/" + initialBranch);
    }

    @Test
    @Ignore("Known to fail, work in progress")
    public void checkout() throws Exception {
        ws.cloneMirror();
        ws.getGitClient().checkout("origin/" + initialBranch, initialBranch);
    }
}
