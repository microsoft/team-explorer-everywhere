// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.LocalPathFormatException;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class LocalPathTest extends TestCase {
    public void testGetFolderDepth() {
        Platform platform;

        try {
            LocalPath.getFolderDepth(null);
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        try {
            LocalPath.getFolderDepth(""); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        /*
         * getFolderDepth lets us specify an acting platform because it doesn't
         * call into platform filesystem code. Makes testing better.
         */

        // /////////////////////////////////////////////////////////////
        // Test as Windows
        // /////////////////////////////////////////////////////////////
        platform = Platform.WINDOWS;

        try {
            LocalPath.getFolderDepth("C:", platform); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            LocalPath.getFolderDepth("abc", platform); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            LocalPath.getFolderDepth("a\\b", platform); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            LocalPath.getFolderDepth("x\\", platform); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            LocalPath.getFolderDepth("x\\b\\", platform); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // Incomplete UNC

        try {
            LocalPath.getFolderDepth("\\\\", platform); //$NON-NLS-1$
            assertTrue("incomplete UNC", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server", platform); //$NON-NLS-1$
            assertTrue("incomplete UNC", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server\\", platform); //$NON-NLS-1$
            assertTrue("incomplete UNC", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        assertEquals(0, LocalPath.getFolderDepth("\\", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("\\a", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("\\a\\", platform)); //$NON-NLS-1$

        assertEquals(0, LocalPath.getFolderDepth("c:\\", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("c:\\a", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("c:\\a\\", platform)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getFolderDepth("c:\\a\\bar", platform)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getFolderDepth("c:\\a\\bar\\", platform)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getFolderDepth("c:\\a\\b\\zap", platform)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getFolderDepth("c:\\a\\b\\zap\\", platform)); //$NON-NLS-1$

        assertEquals(0, LocalPath.getFolderDepth("\\\\server\\share1", platform)); //$NON-NLS-1$
        assertEquals(0, LocalPath.getFolderDepth("\\\\server\\share1\\", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("\\\\server\\share1\\a", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("\\\\server\\share1\\a\\", platform)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getFolderDepth("\\\\server\\share1\\a\\b", platform)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getFolderDepth("\\\\server\\share1\\a\\b\\", platform)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getFolderDepth("\\\\server\\share1\\a\\b\\zap", platform)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getFolderDepth("\\\\server\\share1\\a\\b\\zap\\", platform)); //$NON-NLS-1$

        // /////////////////////////////////////////////////////////////
        // Test as Unix
        // /////////////////////////////////////////////////////////////
        platform = Platform.GENERIC_UNIX;

        try {
            LocalPath.getFolderDepth("abc", platform); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        assertEquals(0, LocalPath.getFolderDepth("/", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("/a", platform)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getFolderDepth("/a/", platform)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getFolderDepth("/a/b", platform)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getFolderDepth("/a/b/", platform)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getFolderDepth("/a/b/zap", platform)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getFolderDepth("/a/b/zap/", platform)); //$NON-NLS-1$

        // UNC not supported

        try {
            LocalPath.getFolderDepth("\\\\", platform); //$NON-NLS-1$
            assertTrue("Should not support UNC on Unix", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server", platform); //$NON-NLS-1$
            assertTrue("Should not support UNC on Unix", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server\\", platform); //$NON-NLS-1$
            assertTrue("Should not support UNC on Unix", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server\\share", platform); //$NON-NLS-1$
            assertTrue("Should not support UNC on Unix", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server\\share\\", platform); //$NON-NLS-1$
            assertTrue("Should not support UNC on Unix", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }

        try {
            LocalPath.getFolderDepth("\\\\server\\share\\file", platform); //$NON-NLS-1$
            assertTrue("Should not support UNC on Unix", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
            // expected
        }
    }

    public void testMakeRelative() {
        try {
            LocalPath.makeRelative(null, null);
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            LocalPath.makeRelative("abc", null); //$NON-NLS-1$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        try {
            LocalPath.makeRelative(null, "abc"); //$NON-NLS-1$
            assertTrue("Should have thrown", false); //$NON-NLS-1$
        } catch (final RuntimeException e) {
        }

        if (File.separatorChar == '/') {
            // Normal stuff.
            assertEquals("", LocalPath.makeRelative("/", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("", LocalPath.makeRelative("", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("", LocalPath.makeRelative("/a", "/a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("b", LocalPath.makeRelative("/a/b", "/a/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("b", LocalPath.makeRelative("/a/b", "/a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Test case insensitive.
            assertEquals("bar", LocalPath.makeRelative("/Abc/bar", "/abc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("bar", LocalPath.makeRelative("/abC/bar", "/aBc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // No relative possible, should return first path.
            assertEquals("/a/b", LocalPath.makeRelative("/a/b", "/zap")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("", LocalPath.makeRelative("", "/zap")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else if (File.separatorChar == '\\') {
            assertEquals("c:\\", LocalPath.makeRelative("c:\\", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("", LocalPath.makeRelative("", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("", LocalPath.makeRelative("c:\\a", "c:\\a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("b", LocalPath.makeRelative("c:\\a\\b", "c:\\a\\")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("b", LocalPath.makeRelative("c:\\a\\b", "c:\\a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Test case insensitive.
            assertEquals("b", LocalPath.makeRelative("c:\\Abc\\b", "c:\\abc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("b", LocalPath.makeRelative("c:\\AbC\\b", "c:\\aBc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // No relative possible, should return first path.
            assertEquals("c:\\abc\\d", LocalPath.makeRelative("c:\\abc\\d", "c:\\zap")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("", LocalPath.makeRelative("", "c:\\zap")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    public void testNativeToTFS() {
        assertNull(LocalPath.nativeToTFS(null));

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            // Windows paths are left alone

            assertEquals("c:\\", LocalPath.nativeToTFS("c:\\")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("C:\\file.txt", LocalPath.nativeToTFS("C:\\file.txt")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("D:\\folder\\file.txt", LocalPath.nativeToTFS("D:\\folder\\file.txt")); //$NON-NLS-1$ //$NON-NLS-2$

            assertEquals("U:\\", LocalPath.nativeToTFS("U:\\")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\file.txt", LocalPath.nativeToTFS("U:\\file.txt")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\folder\\file.txt", LocalPath.nativeToTFS("U:\\folder\\file.txt")); //$NON-NLS-1$ //$NON-NLS-2$

            // Multiple slashes are preserved
            assertEquals("U:\\\\", LocalPath.nativeToTFS("U:\\\\")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\\\a", LocalPath.nativeToTFS("U:\\\\a")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\\\a\\\\b", LocalPath.nativeToTFS("U:\\\\a\\\\b")); //$NON-NLS-1$ //$NON-NLS-2$

        } else if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            assertEquals("U:\\", LocalPath.nativeToTFS("/")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\a", LocalPath.nativeToTFS("/a")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\a\\", LocalPath.nativeToTFS("/a/"));//$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\a\\b", LocalPath.nativeToTFS("/a/b")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\home\\path\\some\\file", LocalPath.nativeToTFS("/home/path/some/file")); //$NON-NLS-1$ //$NON-NLS-2$

            // Multiple slashes are preserved
            assertEquals("U:\\\\", LocalPath.nativeToTFS("//")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\\\a", LocalPath.nativeToTFS("//a")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals("U:\\\\a\\\\b", LocalPath.nativeToTFS("//a//b")); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void testexpandTildeToHome() {
        /*
         * Most of these tests are not run on Windows because the feature is not
         * supported there. We do verify the implementation leaves the string
         * alone on that platform.
         */
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            // On Windows, expandTildeToHome returns the string unaltered.
            assertEquals(LocalPath.expandTildeToHome("~"), "~"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~fun"), "~fun"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~/fun"), "~/fun"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("abc"), "abc"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("abc~"), "abc~"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("c:\\a\\b"), "c:\\a\\b"); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            /*
             * These tests assume the existance of a "bin" user who has home
             * directory "/bin". Also assumes System.getProperty("user.home")
             * returns a string without a trailing separator (may depend on
             * passwd entry).
             */

            // No leading ~, so no alteration.
            assertEquals(LocalPath.expandTildeToHome("/home/bin"), "/home/bin"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("/home/~"), "/home/~"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("/~"), "/~"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("/~bar"), "/~bar"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("fun.txt"), "fun.txt"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("../fun.txt"), "../fun.txt"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome(" ../fun.txt"), "../fun.txt"); //$NON-NLS-1$ //$NON-NLS-2$

            // Empty string and whitespace strings are made empty.
            assertEquals(LocalPath.expandTildeToHome(""), ""); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome(" "), ""); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome(" \t"), ""); //$NON-NLS-1$ //$NON-NLS-2$

            // Normal expansion cases.
            assertEquals(LocalPath.expandTildeToHome("~bin"), "/bin"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~bin/"), "/bin/"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~bin/blah.txt"), "/bin/blah.txt"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~bin/~bin.txt"), "/bin/~bin.txt"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~bin/bin.txt~"), "/bin/bin.txt~"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~"), System.getProperty("user.home")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~/"), System.getProperty("user.home") + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(LocalPath.expandTildeToHome("~/blah.txt"), System.getProperty("user.home") + "/blah.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(LocalPath.expandTildeToHome("~/~/blah.txt"), System.getProperty("user.home") + "/~/blah.txt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(LocalPath.expandTildeToHome("~/blah.txt~"), System.getProperty("user.home") + "/blah.txt~"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(LocalPath.expandTildeToHome("~/~"), System.getProperty("user.home") + "/~"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Should not get altered, because they are invalid expansions.
            assertEquals(LocalPath.expandTildeToHome("~~"), "~~"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~~bin"), "~~bin"); //$NON-NLS-1$ //$NON-NLS-2$

            // A user who doesn't exist; strings unaltered.
            assertEquals(LocalPath.expandTildeToHome("~garbageuser"), "~garbageuser"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~garbageuser/"), "~garbageuser/"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~garbageuser/asdf"), "~garbageuser/asdf"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~garbageuser/asdf/fdsa"), "~garbageuser/asdf/fdsa"); //$NON-NLS-1$ //$NON-NLS-2$

            // Leading whitespace should be eaten.
            assertEquals(LocalPath.expandTildeToHome(" ~bin"), "/bin"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome(" ~"), System.getProperty("user.home")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("\t~"), System.getProperty("user.home")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("\t   \t~"), System.getProperty("user.home")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("  ~/"), System.getProperty("user.home") + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(LocalPath.expandTildeToHome(" ~/ack/splat"), System.getProperty("user.home") + "/ack/splat"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Trailing whitespace is preserved for valid expansions.
            assertEquals(LocalPath.expandTildeToHome("~bin/\t"), "/bin/\t"); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(LocalPath.expandTildeToHome("~bin/file.c \t"), "/bin/file.c \t"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    public void testIsPathRooted() {
        assertFalse(LocalPath.isPathRooted("fun.txt")); //$NON-NLS-1$
        assertFalse(LocalPath.isPathRooted("some\\directory\\fun.txt")); //$NON-NLS-1$
        assertFalse(LocalPath.isPathRooted("some/directory/fun.txt")); //$NON-NLS-1$

        // Covers all the Unix cases (must start with slash)
        assertTrue(LocalPath.isPathRooted("/fun.txt")); //$NON-NLS-1$
        assertTrue(LocalPath.isPathRooted("/dir/fun.txt")); //$NON-NLS-1$

        // Windows matches when starts with backslash (covers UNC) or second
        // char is colon (drive letter)
        assertTrue(LocalPath.isPathRooted("\\fun.txt")); //$NON-NLS-1$
        assertTrue(LocalPath.isPathRooted("\\some\\directory\\fun.txt")); //$NON-NLS-1$
        assertTrue(LocalPath.isPathRooted("Y:\\some\\directory\\fun.txt")); //$NON-NLS-1$
        assertTrue(LocalPath.isPathRooted("\\\\server\\share\\file.txt")); //$NON-NLS-1$
    }

    public void testCheck8Dot3Aliases() {
        /*
         * Windows 8.3 aliases look like "SOMETH~1.TXT". Extensions are
         * optional, but the base name is truncated to <= 6 chars, then a tilde
         * (~), then a digit.
         */

        // Valid (should not throw)
        LocalPath.check8Dot3Aliases("c:\\fun"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\fun.bar"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\some long file name with.123.xyz.789"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\fun\\"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\abc.def\\"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\some long file name with.123.xyz.789\\"); //$NON-NLS-1$

        LocalPath.check8Dot3Aliases("/fun"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/abc.def"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/some long file name with.123.xyz.789"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/fun/"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/abc.def/"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/some long file name with.123.xyz.789/"); //$NON-NLS-1$

        // Valid (not quite 8.3)

        LocalPath.check8Dot3Aliases("c:\\ABCDEF~X"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\ABCDEF~~.TXT"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\ABCDEF~X\\"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\ABCDEF~~.TXT\\"); //$NON-NLS-1$

        LocalPath.check8Dot3Aliases("/ABCDEF~X"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/ABCDEF~~.TXT"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/ABCDEF~X/"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("/ABCDEF~~.TXT/"); //$NON-NLS-1$

        LocalPath.check8Dot3Aliases("c:\\ThisFileIsTooLongToMatch~1"); //$NON-NLS-1$
        LocalPath.check8Dot3Aliases("c:\\ThisFileIsTooLongToMatch~1.TXT"); //$NON-NLS-1$

        try {
            LocalPath.check8Dot3Aliases("c:\\ABC~1"); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final LocalPathFormatException e) {
        }

        try {
            LocalPath.check8Dot3Aliases("c:\\ABC~1\\"); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final LocalPathFormatException e) {
        }

        try {
            LocalPath.check8Dot3Aliases("c:\\ABC~1.XYZ"); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final LocalPathFormatException e) {
        }

        try {
            LocalPath.check8Dot3Aliases("c:\\ABC~9.XYZ\\"); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final LocalPathFormatException e) {
        }

        try {
            LocalPath.check8Dot3Aliases("/unix/path/to/dos/file/ABCDEF~9.XYZ"); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final LocalPathFormatException e) {
        }

        try {
            LocalPath.check8Dot3Aliases("/unix/path/to/dos/directory/ABCDEF~9.XYZ/"); //$NON-NLS-1$
            assertTrue("should have thrown", false); //$NON-NLS-1$
        } catch (final LocalPathFormatException e) {
        }
    }

    public void testHasVersionControlReservedCharacter() {
        final AtomicReference<Character> c = new AtomicReference<Character>();

        try {
            LocalPath.hasVersionControlReservedCharacter(null, null);
            assertTrue("should throw for null path", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        // Valid chars, null char reference

        assertFalse(LocalPath.hasVersionControlReservedCharacter("abc:xyz", null)); //$NON-NLS-1$

        // Valid chars, non-null char reference

        assertFalse(LocalPath.hasVersionControlReservedCharacter("", c)); //$NON-NLS-1$
        assertNull(c.get());

        assertFalse(LocalPath.hasVersionControlReservedCharacter("abc", c)); //$NON-NLS-1$
        assertNull(c.get());

        // Invalid chars, null char reference

        assertTrue(LocalPath.hasVersionControlReservedCharacter(";", null)); //$NON-NLS-1$

        // Invalid chars, non-null char reference

        c.set(null);
        assertTrue(LocalPath.hasVersionControlReservedCharacter(";", c)); //$NON-NLS-1$
        assertEquals(';', c.get().charValue());

        c.set(null);
        assertTrue(LocalPath.hasVersionControlReservedCharacter("a;", c)); //$NON-NLS-1$
        assertEquals(';', c.get().charValue());

        c.set(null);
        assertTrue(LocalPath.hasVersionControlReservedCharacter(";b", c)); //$NON-NLS-1$
        assertEquals(';', c.get().charValue());

        c.set(null);
        assertTrue(LocalPath.hasVersionControlReservedCharacter("abc;xyz", c)); //$NON-NLS-1$
        assertEquals(';', c.get().charValue());
    }

    public void testGetPathRoot() {
        assertNull(LocalPath.getPathRoot(null, Platform.GENERIC_UNIX));

        Platform p = Platform.WINDOWS;

        // Empty and bare roots
        assertEquals("", LocalPath.getPathRoot("", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("", LocalPath.getPathRoot("   ", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("", LocalPath.getPathRoot("   x", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\", LocalPath.getPathRoot("\\", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\\\", LocalPath.getPathRoot("\\\\", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/", LocalPath.getPathRoot("/", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:", LocalPath.getPathRoot("C:", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\", LocalPath.getPathRoot("C:\\", p)); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals("", LocalPath.getPathRoot("abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/", LocalPath.getPathRoot("/abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\", LocalPath.getPathRoot("\\abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\\\\server\\share", LocalPath.getPathRoot("\\\\server\\share\\item", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:", LocalPath.getPathRoot("C:abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:", LocalPath.getPathRoot("C:abc\\and\\more", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\", LocalPath.getPathRoot("C:\\abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("C:\\", LocalPath.getPathRoot("C:\\abc\\and\\more", p)); //$NON-NLS-1$ //$NON-NLS-2$

        p = Platform.GENERIC_UNIX;

        // Emtpy and bare roots
        assertEquals("", LocalPath.getPathRoot("", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("", LocalPath.getPathRoot("   ", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("", LocalPath.getPathRoot("   x", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/", LocalPath.getPathRoot("/", p)); //$NON-NLS-1$ //$NON-NLS-2$

        // Files in root
        assertEquals("", LocalPath.getPathRoot("abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/", LocalPath.getPathRoot("/abc", p)); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/", LocalPath.getPathRoot("/abc/and/more", p)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testGetRootLength() {
        try {
            LocalPath.getRootLength(null, Platform.GENERIC_UNIX);
            assertTrue("should throw for null path", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        try {
            LocalPath.getRootLength("/abc", null); //$NON-NLS-1$
            assertTrue("should throw for null platform", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        Platform p = Platform.WINDOWS;

        // Empty and bare roots
        assertEquals(0, LocalPath.getRootLength("", p)); //$NON-NLS-1$
        assertEquals(0, LocalPath.getRootLength("   ", p)); //$NON-NLS-1$
        assertEquals(0, LocalPath.getRootLength("   x", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("\\", p)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getRootLength("\\\\", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("/", p)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getRootLength("C:", p)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getRootLength("C:\\", p)); //$NON-NLS-1$

        // Files in root
        assertEquals(0, LocalPath.getRootLength("abc", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("/abc", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("\\abc", p)); //$NON-NLS-1$
        assertEquals(14, LocalPath.getRootLength("\\\\server\\share\\item", p)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getRootLength("C:abc", p)); //$NON-NLS-1$
        assertEquals(2, LocalPath.getRootLength("C:abc\\and\\more", p)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getRootLength("C:\\abc", p)); //$NON-NLS-1$
        assertEquals(3, LocalPath.getRootLength("C:\\abc\\and\\more", p)); //$NON-NLS-1$

        p = Platform.GENERIC_UNIX;

        // Emtpy and bare roots
        assertEquals(0, LocalPath.getRootLength("", p)); //$NON-NLS-1$
        assertEquals(0, LocalPath.getRootLength("   ", p)); //$NON-NLS-1$
        assertEquals(0, LocalPath.getRootLength("   x", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("/", p)); //$NON-NLS-1$

        // Files in root
        assertEquals(0, LocalPath.getRootLength("abc", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("/abc", p)); //$NON-NLS-1$
        assertEquals(1, LocalPath.getRootLength("/abc/and/more", p)); //$NON-NLS-1$
    }

    public void testGetCommonPathPrefix() {
        // Both params can't be null or empty

        try {
            LocalPath.getCommonPathPrefix(null, null);
            assertTrue("should throw for null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }
        try {
            LocalPath.getCommonPathPrefix(null, "abc"); //$NON-NLS-1$
            assertTrue("should throw for null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }
        try {
            LocalPath.getCommonPathPrefix("abc", null); //$NON-NLS-1$
            assertTrue("should throw for null", false); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }
        try {
            LocalPath.getCommonPathPrefix("", "abc"); //$NON-NLS-1$//$NON-NLS-2$
            assertTrue("should throw for empty", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }
        try {
            LocalPath.getCommonPathPrefix("abc", ""); //$NON-NLS-1$//$NON-NLS-2$
            assertTrue("should throw for empty", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        /*
         * Note: Canonical input paths for this method never end in a filesystem
         * separator except for drive roots on Windows and the root path on
         * Unix.
         */

        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            assertEquals("/", LocalPath.getCommonPathPrefix("/", "/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/", LocalPath.getCommonPathPrefix("/", "/fun")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/", LocalPath.getCommonPathPrefix("/fun", "/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/", LocalPath.getCommonPathPrefix("/fun", "/bar")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Test case insensitive (even on Unix)
            assertEquals("/abc".toLowerCase(), LocalPath.getCommonPathPrefix("/abc", "/ABC/DEF").toLowerCase()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            assertEquals("/fun", LocalPath.getCommonPathPrefix("/fun", "/fun")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/fun", LocalPath.getCommonPathPrefix("/fun", "/fun/a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/fun", LocalPath.getCommonPathPrefix("/fun", "/fun/a/b")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            assertEquals("/abc/def", LocalPath.getCommonPathPrefix("/abc/def", "/abc/def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/abc/def", LocalPath.getCommonPathPrefix("/abc/def", "/abc/def/a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/abc/def", LocalPath.getCommonPathPrefix("/abc/def/a", "/abc/def/x")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("/abc/def", LocalPath.getCommonPathPrefix("/abc/def/a/b", "/abc/def/x/y")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            assertEquals("c:\\", LocalPath.getCommonPathPrefix("c:\\", "c:\\")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\", LocalPath.getCommonPathPrefix("c:\\", "c:\\abc")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\", LocalPath.getCommonPathPrefix("c:\\abc", "c:\\")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\", LocalPath.getCommonPathPrefix("c:\\abc", "c:\\def")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            // Should fixup bare drive letters
            assertEquals("c:\\", LocalPath.getCommonPathPrefix("c:", "c:")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\", LocalPath.getCommonPathPrefix("c:", "c:\\")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

            // Test case insensitive
            assertEquals(
                "c:\\abc".toLowerCase(), //$NON-NLS-1$
                LocalPath.getCommonPathPrefix("c:\\abc", "C:\\ABC\\DEF").toLowerCase()); //$NON-NLS-1$ //$NON-NLS-2$

            assertEquals("c:\\abc", LocalPath.getCommonPathPrefix("c:\\abc", "c:\\abc")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\abc", LocalPath.getCommonPathPrefix("c:\\abc", "c:\\abc\\a")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\abc", LocalPath.getCommonPathPrefix("c:\\abc", "c:\\abc\\a\\b")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

            assertEquals("c:\\abc\\def", LocalPath.getCommonPathPrefix("c:\\abc\\def", "c:\\abc\\def")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\abc\\def", LocalPath.getCommonPathPrefix("c:\\abc\\def", "c:\\abc\\def\\a")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\abc\\def", LocalPath.getCommonPathPrefix("c:\\abc\\def\\a", "c:\\abc\\def\\x")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("c:\\abc\\def", LocalPath.getCommonPathPrefix("c:\\abc\\def\\a\\b", "c:\\abc\\def\\x\\y")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

            // Different drives
            assertNull(LocalPath.getCommonPathPrefix("c:\\", "d:\\")); //$NON-NLS-1$//$NON-NLS-2$
            assertNull(LocalPath.getCommonPathPrefix("c:\\", "d:\\abc")); //$NON-NLS-1$//$NON-NLS-2$
            assertNull(LocalPath.getCommonPathPrefix("c:\\", "d:\\abc\\def")); //$NON-NLS-1$//$NON-NLS-2$

            // One UNC, one local
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr\\shr", "d:\\abc\\def")); //$NON-NLS-1$//$NON-NLS-2$
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr\\shr\\a", "d:\\abc\\def")); //$NON-NLS-1$//$NON-NLS-2$
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr\\shr\\a", "d:\\")); //$NON-NLS-1$//$NON-NLS-2$

            // Different servers
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr1\\shr", "\\\\svr2\\shr")); //$NON-NLS-1$//$NON-NLS-2$
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr1\\shr\\a", "\\\\svr2\\shr\\a")); //$NON-NLS-1$//$NON-NLS-2$

            // Same server, different shares
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr\\shr1", "\\\\svr\\shr2")); //$NON-NLS-1$//$NON-NLS-2$
            assertNull(LocalPath.getCommonPathPrefix("\\\\svr\\shr1\\a", "\\\\svr\\shr2\\b")); //$NON-NLS-1$//$NON-NLS-2$

            // Same server, same share, different paths
            assertEquals("\\\\svr\\shr", LocalPath.getCommonPathPrefix("\\\\svr\\shr", "\\\\svr\\shr")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("\\\\svr\\shr", LocalPath.getCommonPathPrefix("\\\\svr\\shr\\a", "\\\\svr\\shr\\x")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals("\\\\svr\\shr", LocalPath.getCommonPathPrefix("\\\\svr\\shr\\a\\b", "\\\\svr\\shr\\x\\b")); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
            assertEquals(
                "\\\\svr\\shr\\a", //$NON-NLS-1$
                LocalPath.getCommonPathPrefix("\\\\svr\\shr\\a\\abc", "\\\\svr\\shr\\a\\def")); //$NON-NLS-1$ //$NON-NLS-2$
            assertEquals(
                "\\\\svr\\shr\\a\\b", //$NON-NLS-1$
                LocalPath.getCommonPathPrefix("\\\\svr\\shr\\a\\b\\abc", "\\\\svr\\shr\\a\\b\\def")); //$NON-NLS-1$ //$NON-NLS-2$

        }
    }
}
