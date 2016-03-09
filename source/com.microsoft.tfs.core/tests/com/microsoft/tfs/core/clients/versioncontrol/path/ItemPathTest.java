// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.LocalPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;

import junit.framework.TestCase;

public class ItemPathTest extends TestCase {
    public void testCheckForIllegalDollarInPath() {
        // The method takes both server and local paths and disallows a dollar
        // sign following any server or local path separator.

        // These are valid

        ItemPath.checkForIllegalDollarInPath("$/"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc$"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc$/"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc$def/"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc$def/abc"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc$def/abc$"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("$/abc$def/abc$/"); //$NON-NLS-1$

        ItemPath.checkForIllegalDollarInPath("/"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc$"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc$/"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc$def/"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc$def/abc"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc$def/abc$"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("/abc$def/abc$/"); //$NON-NLS-1$

        ItemPath.checkForIllegalDollarInPath("c:\\"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc$"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc$\\"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc$def\\"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc$def\\abc"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc$def\\abc$"); //$NON-NLS-1$
        ItemPath.checkForIllegalDollarInPath("c:\\abc$def\\abc$\\"); //$NON-NLS-1$

        // These are invalid

        // server
        try {
            ItemPath.checkForIllegalDollarInPath("$/$"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final ServerPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("$/$abc"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final ServerPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("$/abc/$"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final ServerPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("$/abc/$def"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final ServerPathFormatException e) {
        }

        // Unix
        try {
            ItemPath.checkForIllegalDollarInPath("/$"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("/$abc"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("/abc/$"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("/abc/$def"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }

        // Windows
        try {
            ItemPath.checkForIllegalDollarInPath("c:\\$"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("c:\\$abc"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("c:\\abc\\$"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
        try {
            ItemPath.checkForIllegalDollarInPath("c:\\abc\\$def"); //$NON-NLS-1$
            assertTrue(false);
        } catch (final LocalPathFormatException e) {
        }
    }
}
