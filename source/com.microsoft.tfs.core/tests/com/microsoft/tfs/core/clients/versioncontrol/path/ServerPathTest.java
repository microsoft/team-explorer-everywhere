// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import java.util.ArrayList;
import java.util.Arrays;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;

import junit.framework.TestCase;

public class ServerPathTest extends TestCase {

    /*
     * Test method for
     * 'com.microsoft.tfs.core.vc.ServerPath.getTeamProject(String)'
     */
    public void testGetTeamProject() {
        // Main case
        assertEquals("$/MyTeamProject", ServerPath.getTeamProject("$/MyTeamProject/MyPath/MyFile.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Exceptional cases
        assertNull("Teamproject should be null if null passed", ServerPath.getTeamProject(null)); //$NON-NLS-1$
        assertEquals("Root should be returned if passed", "$/", ServerPath.getTeamProject("$/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("If path passed is in root, just return it", "$/Abc.def", ServerPath.getTeamProject("$/Abc.def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    /*
     * Test method for
     * 'com.microsoft.tfs.core.vc.ServerPath.getTeamProjectName(String)'
     */
    public void testGetTeamProjectName() {
        // Main case
        assertEquals("MyTeamProject", ServerPath.getTeamProjectName("$/MyTeamProject/MyPath/MyFile.java")); //$NON-NLS-1$ //$NON-NLS-2$

        // Exceptional cases
        assertNull("Name should be null if null passed", ServerPath.getTeamProjectName(null)); //$NON-NLS-1$
    }

    public void testIsTeamProject() {
        // These are team project paths

        assertTrue(ServerPath.isTeamProject("$/A")); //$NON-NLS-1$
        assertTrue(ServerPath.isTeamProject("$/xyz")); //$NON-NLS-1$
        assertTrue(ServerPath.isTeamProject("$/some really long path")); //$NON-NLS-1$

        // These are not

        try {
            ServerPath.isTeamProject(null);
            assertTrue("should throw for null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        assertFalse(ServerPath.isTeamProject("")); //$NON-NLS-1$
        assertFalse(ServerPath.isTeamProject("x")); //$NON-NLS-1$
        assertFalse(ServerPath.isTeamProject("$/")); //$NON-NLS-1$
        assertFalse(ServerPath.isTeamProject("$/project/other")); //$NON-NLS-1$
        assertFalse(ServerPath.isTeamProject("$/project/other/")); //$NON-NLS-1$
        assertFalse(ServerPath.isTeamProject("$/project/other/even/more")); //$NON-NLS-1$
        assertFalse(ServerPath.isTeamProject("$/project/other/even/more/")); //$NON-NLS-1$
    }

    /*
     * Test method for
     * 'com.microsoft.tfs.core.vc.ServerPath.getTeamProjects(String[])'
     */
    public void testGetTeamProjects() {
        final String[] serverPaths = new String[] {
            "$/MyTeamProject/MyPath1/MyFile1.java", //$NON-NLS-1$
            "$/MyTeamProject/MyPath1/MyFile2.java", //$NON-NLS-1$
            "$/MyTeamProject/MyPath1/NestedPath/MyFile1.java", //$NON-NLS-1$
            "$/MyTeamProject/MyPath2/MyFile1.java", //$NON-NLS-1$
            "$/MyTeamProject/MyPath2/MyFile2.java", //$NON-NLS-1$
            "$/OtherTeamProject/MyPath1/MyFile3.java", //$NON-NLS-1$
            "$/MyTeamProject/MyPath1/MyFile4.java" //$NON-NLS-1$
        };

        final String[] actualTeamProjects = ServerPath.getTeamProjects(serverPaths);

        final ArrayList actualProjectList = new ArrayList(Arrays.asList(actualTeamProjects));

        assertEquals("2 distinct projects should be returned", 2, actualTeamProjects.length); //$NON-NLS-1$
        assertTrue("$/MyTeamProject should be in returned array", actualProjectList.contains("$/MyTeamProject")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue("$/OtherTeamProject should be in returned array", actualProjectList.contains("$/OtherTeamProject")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /*
     * Test method for
     * 'com.microsoft.tfs.core.vc.ServerPath.getTeamProjects(String[])'
     */
    public void testGetTeamProjectsNoItemsPassed() {
        final String[] serverPaths = new String[0];
        final String[] actualTeamProjects = ServerPath.getTeamProjects(serverPaths);
        assertEquals("Empty array should be returned if empty array passed", 0, actualTeamProjects.length); //$NON-NLS-1$
    }

    /*
     * Test method for
     * 'com.microsoft.tfs.core.vc.ServerPath.getTeamProjects(String[])'
     */
    public void testGetTeamProjectsNullPassed() {
        final String[] actualTeamProjects = ServerPath.getTeamProjects(null);
        assertEquals("Empty array should be returned if null passed", 0, actualTeamProjects.length); //$NON-NLS-1$
    }

    /*
     * Test method for
     * 'com.microsoft.tfs.core.vc.ServerPath.canonicalize(String)'
     */
    public void testCanonicalize() throws Exception {
        assertEquals("$ should turn into $/", "$/", ServerPath.canonicalize("$")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Tests to ensure that $/ is always well-formed as such.
        assertEquals("$/ should not have trailing slash removed", "$/", ServerPath.canonicalize("$/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/ should not have trailing slash removed", "$/", ServerPath.canonicalize("$//////")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Test equivalence when no $ is provided
        assertEquals("/ == $/", "$/", ServerPath.canonicalize("/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("/ == $/", "$/", ServerPath.canonicalize("////////.////.////")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("/ == $/", "$/SandboxAgile", ServerPath.canonicalize("/SandboxAgile/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Test to ensure that ./ is quashed
        assertEquals("Path should have /./ removed", "$/", ServerPath.canonicalize("$/./")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("Path should have /./ removed", "$/SandboxAgile", ServerPath.canonicalize("$/SandboxAgile/./")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("Path should have /./ removed", "$/SandboxAgile", ServerPath.canonicalize("$/./SandboxAgile/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("Path should have /./ removed", "$/SandboxAgile", ServerPath.canonicalize("$/./SandboxAgile/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals(
            "Path should have /./ removed", //$NON-NLS-1$
            "$/SandboxAgile/abcdef", //$NON-NLS-1$
            ServerPath.canonicalize("$/./SandboxAgile/./abcdef")); //$NON-NLS-1$

        // Tests to ensure that ../ takes us up a level
        assertEquals("Path should have /../ removed", "$/", ServerPath.canonicalize("$/SandboxAgile/../")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals(
            "Path should have /../ removed", //$NON-NLS-1$
            "$/SandboxAgile", //$NON-NLS-1$
            ServerPath.canonicalize("$/SandboxAgile/../SandboxAgile/")); //$NON-NLS-1$
        assertEquals(
            "Path should have /../ removed", //$NON-NLS-1$
            "$/SandboxCMMI", //$NON-NLS-1$
            ServerPath.canonicalize("$/SandboxWIT/../SandboxAgile/../SandboxCMMI/./")); //$NON-NLS-1$

        // Tests to ensure that we throw an exception when we try to back out of
        // the server root.
        expectCanonicalizeException("../ing beneath server root should throw", "$/../"); //$NON-NLS-1$ //$NON-NLS-2$
        expectCanonicalizeException("../ing beneath server root should throw", "$/SandboxAgile/../../"); //$NON-NLS-1$ //$NON-NLS-2$
        expectCanonicalizeException(
            "../ing beneath server root should throw", //$NON-NLS-1$
            "$/SandboxAgile/../SandboxCMMI/../../SandboxWIT/"); //$NON-NLS-1$

        // Tests elimination of trailing invalid characters (spaces, dots)
        // This is the way MSFT does it. It's not necessarily winning, just
        // compliant.
        assertEquals("Path should have trailing whitespace/dots removed", "$/", ServerPath.canonicalize("$/     .   ")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals(
            "Path should have trailing whitespace/dots removed", //$NON-NLS-1$
            "$/SandboxAgile/Fun.txt", //$NON-NLS-1$
            ServerPath.canonicalize("$/SandboxAgile/Fun.txt.  ")); //$NON-NLS-1$
        assertEquals(
            "Path should have whitespace/dot components removed", //$NON-NLS-1$
            "$/SandboxAgile/Fun.txt", //$NON-NLS-1$
            ServerPath.canonicalize("$/SandboxAgile/    .    /Fun.txt")); //$NON-NLS-1$

        // Test maximum component length
        assertEquals(
            "Testing maximum component length", //$NON-NLS-1$
            "$/SandboxAgile/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-/Fun.txt", //$NON-NLS-1$
            ServerPath.canonicalize(
                "$/SandboxAgile/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-/Fun.txt")); //$NON-NLS-1$
        expectCanonicalizeException(
            "Long components should throw", //$NON-NLS-1$
            "$/SandboxAgile/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-_/"); //$NON-NLS-1$

        // Test invalid characters and component names
        expectCanonicalizeException("Bad component names should throw", ""); //$NON-NLS-1$ //$NON-NLS-2$
        expectCanonicalizeException("Bad component names should throw", "$/nul"); //$NON-NLS-1$ //$NON-NLS-2$
        expectCanonicalizeException("Bad component names should throw", "$/COM2"); //$NON-NLS-1$ //$NON-NLS-2$
        expectCanonicalizeException("Bad characters in filenames should throw", "$/SandboxAgile/abc:def|"); //$NON-NLS-1$ //$NON-NLS-2$
        expectCanonicalizeException(
            "Dollar at beginning of component name should throw", //$NON-NLS-1$
            "$/SandboxCMMI/$Zippy/Pinhead.txt"); //$NON-NLS-1$
    }

    /*
     * Internal method to look for ServerPathFormatException when moving outside
     * project root.
     */
    private void expectCanonicalizeException(final String explanation, final String serverPath) {
        boolean caught = false;

        // should throw when we try to go deeper than /
        try {
            ServerPath.canonicalize(serverPath);
        } catch (final ServerPathFormatException e) {
            caught = true;
        }

        assertTrue(explanation + " for serverPath " + serverPath, caught); //$NON-NLS-1$
    }

    public void testMakeRelative() {
        try {
            LocalPath.makeRelative(null, null);
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            LocalPath.makeRelative("fun", null); //$NON-NLS-1$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            LocalPath.makeRelative(null, "fun"); //$NON-NLS-1$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        // Normal stuff.
        assertEquals("$/", ServerPath.makeRelative("$/", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("", ServerPath.makeRelative("", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("", ServerPath.makeRelative("$/fun", "$/fun")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("def", ServerPath.makeRelative("$/abc/def", "$/abc/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("def", ServerPath.makeRelative("$/abc/def", "$/abc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Test case insensitive.
        assertEquals("def", ServerPath.makeRelative("$/Abc/def", "$/abc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("def", ServerPath.makeRelative("$/AbC/def", "$/aBc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // No relative possible, should return first path.
        assertEquals("$/abc/def", ServerPath.makeRelative("$/abc/def", "$/zap")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("", ServerPath.makeRelative("", "$/zap")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public void testCombine() {
        try {
            ServerPath.combine(null, null);
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            ServerPath.combine("$/fun", null); //$NON-NLS-1$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            ServerPath.combine(null, "bar"); //$NON-NLS-1$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            // First path cannot be empty.
            ServerPath.combine("", "bar"); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        // Second path is empty.
        assertEquals("$/", ServerPath.combine("$/", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Second path is relative.
        assertEquals("$/abc", ServerPath.combine("$/", "abc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/abc/def", ServerPath.combine("$/abc", "def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/abc/def", ServerPath.combine("$/abc/", "def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Second path is absolute.
        assertEquals("$/def", ServerPath.combine("$/abc", "$/def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/def", ServerPath.combine("$/abc/", "$/def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/def", ServerPath.combine("$/abc", "/def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/def", ServerPath.combine("$/abc/", "/def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Canonicalized return path should never end with a slash.
        assertEquals("$/abc", ServerPath.combine("$/abc/", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/abc/def", ServerPath.combine("$/abc/", "def/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Support for alternate separators (they get canonicalized).
        assertEquals("$/abc/def", ServerPath.combine("$\\abc", "def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/def", ServerPath.combine("$\\abc", "$\\def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("$/def", ServerPath.combine("$\\abc", "\\def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public void testIsChild() {
        // Equal paths are sub items.
        assertTrue(ServerPath.isChild("$/", "$/")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(ServerPath.isChild("$/x", "$/x")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(ServerPath.isChild("$/some/long/path", "$/some/long/path")); //$NON-NLS-1$ //$NON-NLS-2$

        // Simple case-insensitive sanity test
        assertTrue(ServerPath.isChild("$/some/letters", "$/SOME/LETTERS")); //$NON-NLS-1$ //$NON-NLS-2$

        // Paths get canonicalized (so terminal slashes don't matter)
        assertTrue(ServerPath.isChild("$/x", "$/x/")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(ServerPath.isChild("$/x/", "$/x")); //$NON-NLS-1$ //$NON-NLS-2$

        // Some basic children and grandchildren
        assertTrue(ServerPath.isChild("$/", "$/a")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(ServerPath.isChild("$/", "$/a/")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(ServerPath.isChild("$/", "$/a/b")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(ServerPath.isChild("$/a", "$/a/b")); //$NON-NLS-1$ //$NON-NLS-2$

        // Non children
        assertFalse(ServerPath.isChild("$/a", "$/")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(ServerPath.isChild("$/a/b", "$/a")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testGetCommonParent() {
        // If one item is null, other item is returned
        assertEquals("$/first", ServerPath.getCommonParent("$/first", null)); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("$/second", ServerPath.getCommonParent(null, "$/second")); //$NON-NLS-1$//$NON-NLS-2$

        // Both cannot be null
        try {
            ServerPath.getCommonParent(null, null);
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // Items are equal, with varying terminal slashes
        assertEquals("$/", ServerPath.getCommonParent("$/", "$/")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/a", ServerPath.getCommonParent("$/a", "$/a")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/a/", ServerPath.getCommonParent("$/a/", "$/a")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/a", ServerPath.getCommonParent("$/a", "$/a/")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/A/B/C", ServerPath.getCommonParent("$/A/B/C", "$/A/B/C")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/A/B/C/", ServerPath.getCommonParent("$/A/B/C/", "$/A/B/C")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/A/B/C", ServerPath.getCommonParent("$/A/B/C", "$/A/B/C/")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$

        // Some ancestor is common
        assertEquals("$/", ServerPath.getCommonParent("$/a", "$/b")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/a", ServerPath.getCommonParent("$/a/b", "$/a/c")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals("$/a", ServerPath.getCommonParent("$/a/B/X", "$/a/C/Z")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
        assertEquals(
            "$/a/b", //$NON-NLS-1$
            ServerPath.getCommonParent("$/a/b/shorter/path", "$/a/b/a/much/longer/sub/path/than/the/other/thing")); //$NON-NLS-1$//$NON-NLS-2$

        // Simple case-insensitive test
        assertEquals("$/A".toLowerCase(), ServerPath.getCommonParent("$/A/B/C", "$/a/c/d").toLowerCase()); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
    }
}
