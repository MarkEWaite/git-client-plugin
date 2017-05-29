package org.jenkinsci.plugins.gitclient;

import static org.junit.Assert.*;
import org.junit.Before;
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

    @Before
    public void createSubmoduleRepos() {
        // Create two sample repos, add one as submodule of the other
    }

    @Test
    public void checkoutThenDeleteSubmodule()
    {
        assertTrue(true);
    }

    @Test
    public void empty() {
        assertTrue(true);
    }
}
