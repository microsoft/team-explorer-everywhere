// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.filesystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;

import com.microsoft.tfs.jni.ExecHelpers;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.WellKnownSID;
import com.microsoft.tfs.util.Platform;

import junit.framework.TestCase;

public class NativeFileSystemTest extends TestCase {
    private NativeFileSystem nativeImpl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.nativeImpl = new NativeFileSystem();
    }

    public void testGetOwner() throws Exception {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        final File testFile = File.createTempFile("testGetOwner", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        assertNotNull("got null owner for a file that should exist", nativeImpl.getOwner(testFile.getAbsolutePath())); //$NON-NLS-1$
        assertTrue("sid not long enough", nativeImpl.getOwner(testFile.getAbsolutePath()).length() > 8); //$NON-NLS-1$

        testFile.delete();
    }

    public void testSetOwner() throws IOException {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        final File testFile = File.createTempFile("testSetOwner", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        final String existingOwner = nativeImpl.getOwner(testFile.getAbsolutePath());
        assertNotNull("got null owner for a file that should exist", existingOwner); //$NON-NLS-1$

        // This is not a terribly useful test, but we'll throw on any error
        nativeImpl.setOwner(testFile.getAbsolutePath(), existingOwner);

        assertEquals("SID is not existing owner", existingOwner, nativeImpl.getOwner(testFile.getAbsolutePath())); //$NON-NLS-1$

        testFile.delete();
    }

    public void testGrantInheritableFullControl() throws IOException {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        final File firstFile = File.createTempFile("testGrantInheritableFullControl", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        // Grant full control to existing user (should always succeed)

        nativeImpl.grantInheritableFullControl(
            firstFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getCurrentIdentityUser(),
            null);

        // Copy ACL from first to second, which is the same anyway (should
        // always succeed)

        final File secondFile = File.createTempFile("testGrantInheritableFullControl2", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        nativeImpl.grantInheritableFullControl(
            secondFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getCurrentIdentityUser(),
            firstFile.getAbsolutePath());

        // Remove all entries from second file
        removeAllAccessEntriesICACLS(secondFile);

        // Grant permissions without copy from first file
        nativeImpl.grantInheritableFullControl(
            secondFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getCurrentIdentityUser(),
            null);

        ensureHasEntries(secondFile);

        // Remove all entries from second file
        removeAllAccessEntriesICACLS(secondFile);

        // Grant permissions with copy from first file
        nativeImpl.grantInheritableFullControl(
            secondFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getCurrentIdentityUser(),
            firstFile.getAbsolutePath());

        ensureHasEntries(secondFile);

        secondFile.delete();
        firstFile.delete();
    }

    public void testCopyExistingDACLEntries() throws IOException {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        final File firstFile = File.createTempFile("testCopyExistingDACLEntries", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final File secondFile = File.createTempFile("testCopyExistingDACLEntries", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        // Grant full control to Guests on first
        removeAllAccessEntriesICACLS(firstFile);
        nativeImpl.grantInheritableFullControl(
            firstFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getWellKnownSID(WellKnownSID.WinBuiltinGuestsSid, null),
            null);

        // Ensure first has a guests entry
        String[] args = new String[] {
            "icacls", //$NON-NLS-1$
            firstFile.getAbsolutePath()
        };
        StringBuffer output = new StringBuffer();
        assertEquals("error reading ACLs", 0, ExecHelpers.exec(args, output)); //$NON-NLS-1$
        assertTrue(output.toString().contains("BUILTIN\\Guests")); //$NON-NLS-1$

        // Copy those entries
        nativeImpl.copyExplicitDACLEntries(firstFile.getAbsolutePath(), secondFile.getAbsolutePath());

        // Ensure second has a guests entry
        args = new String[] {
            "icacls", //$NON-NLS-1$
            secondFile.getAbsolutePath()
        };
        output = new StringBuffer();
        assertEquals("error reading ACLs", 0, ExecHelpers.exec(args, output)); //$NON-NLS-1$
        assertTrue(output.toString().contains("BUILTIN\\Guests")); //$NON-NLS-1$

        secondFile.delete();
        firstFile.delete();
    }

    private void ensureHasEntries(final File file) {
        final String[] args = new String[] {
            "icacls", //$NON-NLS-1$
            file.getAbsolutePath()
        };
        final StringBuffer output = new StringBuffer();
        assertEquals("error reading ACLs", 0, ExecHelpers.exec(args, output)); //$NON-NLS-1$

        // The first ACL is on the first line, after the file name and a space
        final String firstLine = output.toString().split("\n")[0]; //$NON-NLS-1$
        final String[] parts = firstLine.split(" "); //$NON-NLS-1$
        assertEquals("missing ACL", 2, parts.length); //$NON-NLS-1$
        assertTrue("missing ACL text", parts[1].trim().length() > 0); //$NON-NLS-1$
    }

    private void removeAllAccessEntriesICACLS(final File file) {
        // Reset to inherited ACLs
        String[] args = new String[] {
            "icacls", //$NON-NLS-1$
            file.getAbsolutePath(),
            "/reset" //$NON-NLS-1$
        };
        assertEquals("failed to reset ACLs", 0, ExecHelpers.exec(args, null)); //$NON-NLS-1$

        // Remove inheritance, deleting all
        args = new String[] {
            "icacls", //$NON-NLS-1$
            file.getAbsolutePath(),
            "/inheritance:r" //$NON-NLS-1$
        };

        assertEquals("failed to remove ACLs", 0, ExecHelpers.exec(args, null)); //$NON-NLS-1$

        // Read back ACLs (hope for an empty list)
        args = new String[] {
            "icacls", //$NON-NLS-1$
            file.getAbsolutePath()
        };
        final StringBuffer output = new StringBuffer();
        assertEquals("error reading ACLs", 0, ExecHelpers.exec(args, output)); //$NON-NLS-1$

        // There's a trailing space and a LF (no CR)
        assertEquals(
            "first line should have no acl following file name", //$NON-NLS-1$
            file.getAbsolutePath() + " ", //$NON-NLS-1$
            output.toString().split("\n")[0]); //$NON-NLS-1$
    }

    public void testRemoveExplicitAllowEntries() throws IOException

    {
        // Not supported on Unix
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        final File firstFile = File.createTempFile("testRemoveExplicitAllowEntries", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        // Remove all entires (even inherited)
        removeAllAccessEntriesICACLS(firstFile);

        // Grant one explicit allow
        nativeImpl.grantInheritableFullControl(
            firstFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getCurrentIdentityUser(),
            null);

        ensureHasEntries(firstFile);

        // Remove with the native
        nativeImpl.removeExplicitAllowEntries(
            firstFile.getAbsolutePath(),
            PlatformMiscUtils.getInstance().getCurrentIdentityUser());

        ensureHasNoEntries(firstFile, PlatformMiscUtils.getInstance().getCurrentIdentityUser());
    }

    private void ensureHasNoEntries(final File file, final String sid) {
        // Must use "*" prefix for actual SIDs (not used for usernames)
        final String[] args = new String[] {
            "icacls", //$NON-NLS-1$
            file.getAbsolutePath(),
            "/findsid", //$NON-NLS-1$
            "*" + sid //$NON-NLS-1$
        };
        final StringBuffer output = new StringBuffer();

        assertEquals("error reading ACLs", 0, ExecHelpers.exec(args, output)); //$NON-NLS-1$

        final String firstLine = output.toString().split("\n")[0]; //$NON-NLS-1$
        assertTrue(
            "an entry matching the SID was found when it should not be there", //$NON-NLS-1$
            firstLine.contains("No files with a matching SID was found")); //$NON-NLS-1$
    }

    public void testIsReadOnly() throws Exception {
        final File testFile = File.createTempFile("testIsReadOnly", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            final ExecFileSystem efs = new ExecFileSystem();

            final FileSystemAttributes attributes = nativeImpl.getAttributes(testFilePath);
            assertNotNull(attributes);

            // Make read-only
            attributes.setReadOnly(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isReadOnly());

            // Make writable
            attributes.setReadOnly(false);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertFalse(nativeImpl.getAttributes(testFilePath).isReadOnly());

            // Set back again
            attributes.setReadOnly(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isReadOnly());

        } finally {
            testFile.delete();
        }
    }

    public void testExistingFileAttributes() throws Exception {
        final File testFile = File.createTempFile("testFileExists", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String existsFilePath = testFile.getAbsolutePath();

        try {
            assertTrue(nativeImpl.getAttributes(existsFilePath).exists());

            /* Ensure we detect as the appropriate type of file */
            assertFalse(nativeImpl.getAttributes(existsFilePath).isDirectory());

            /* Check for (uncommon) windows bits */
            assertEquals(
                Platform.isCurrentPlatform(Platform.WINDOWS),
                nativeImpl.getAttributes(existsFilePath).isArchive());
            assertFalse(nativeImpl.getAttributes(existsFilePath).isSystem());
            assertFalse(nativeImpl.getAttributes(existsFilePath).isHidden());
        } finally {
            testFile.delete();
        }
    }

    public void testNonExistingFileAttributes() throws Exception {
        final File testFile = File.createTempFile("testFileExists", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String doesNotExistFilePath = testFile.getAbsolutePath();
        testFile.delete();

        assertFalse(nativeImpl.getAttributes(doesNotExistFilePath).exists());

        assertFalse(nativeImpl.getAttributes(doesNotExistFilePath).isDirectory());

        /* Check for (uncommon) windows bits */
        assertFalse(nativeImpl.getAttributes(doesNotExistFilePath).isArchive());
        assertFalse(nativeImpl.getAttributes(doesNotExistFilePath).isSystem());
        assertFalse(nativeImpl.getAttributes(doesNotExistFilePath).isHidden());
    }

    public void testIsDirectory() throws Exception {
        final File testFile = File.createTempFile("testIsDirectory", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();
        final String testFolderPath = testFile.getParent();

        try {
            assertFalse(nativeImpl.getAttributes(testFilePath).isDirectory());

            assertTrue(nativeImpl.getAttributes(testFolderPath).isDirectory());
        } finally {
            testFile.delete();
        }
    }

    public void testSetReadOnly() throws Exception {
        final File testFile = File.createTempFile("testSetReadOnly", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            final ExecFileSystem efs = new ExecFileSystem();

            final FileSystemAttributes attributes = efs.getAttributes(testFilePath);
            assertNotNull(attributes);

            // Make read-only
            attributes.setReadOnly(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isReadOnly());

            // Mac: set the immutable bit, ensure we remove it properly
            testSetImmutable(testFilePath);

            // Make writable
            attributes.setReadOnly(false);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertFalse(efs.getAttributes(testFilePath).isReadOnly());

            // Set back again
            attributes.setReadOnly(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isReadOnly());

        } finally {
            testFile.delete();
        }
    }

    public void testIsOwnerOnly() throws Exception {
        final File testFile = File.createTempFile("testIsOwnerOnly", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                // On Windows, these methods always fail.
                assertFalse(nativeImpl.getAttributes(testFilePath).isOwnerOnly());
                return;
            }

            final ExecFileSystem efs = new ExecFileSystem();
            final FileSystemAttributes attributes = nativeImpl.getAttributes(testFilePath);
            assertNotNull(attributes);

            // Set owner only
            attributes.setOwnerOnly(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isOwnerOnly());

            // Unset owner only
            attributes.setOwnerOnly(false);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertFalse(nativeImpl.getAttributes(testFilePath).isOwnerOnly());

            // Set back again
            attributes.setOwnerOnly(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isOwnerOnly());

        } finally {
            testFile.delete();
        }
    }

    public void testSetOwnerOnly() throws Exception {
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return;
        }

        final File testFile = File.createTempFile("testSetOwnerOnly", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            final ExecFileSystem efs = new ExecFileSystem();

            final FileSystemAttributes attributes = efs.getAttributes(testFilePath);
            assertNotNull(attributes);

            // Make read-only
            attributes.setOwnerOnly(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isOwnerOnly());

            // Make writable
            attributes.setOwnerOnly(false);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertFalse(efs.getAttributes(testFilePath).isOwnerOnly());

            // Set back again
            attributes.setOwnerOnly(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isOwnerOnly());

        } finally {
            testFile.delete();
        }
    }

    public void testIsPublicWritable() throws Exception {
        final File testFile = File.createTempFile("testIsPublicWritable", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                // On Windows, these methods always fail.
                assertFalse(nativeImpl.getAttributes(testFilePath).isPublicWritable());
                return;
            }

            final ExecFileSystem efs = new ExecFileSystem();
            final FileSystemAttributes attributes = nativeImpl.getAttributes(testFilePath);
            assertNotNull(attributes);

            if (efs.getAttributes(testFilePath).isPublicWritable()) {
                // Umask is set as ?00, i.e. all files are public writable.
                // Nothing to test.
                return;
            }

            // Set public writable
            attributes.setPublicWritable(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isPublicWritable());

            // Unset public writable
            attributes.setPublicWritable(false);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertFalse(nativeImpl.getAttributes(testFilePath).isPublicWritable());

            // Set back again
            attributes.setPublicWritable(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isPublicWritable());

        } finally {
            testFile.delete();
        }
    }

    public void testSetPublicWritable() throws Exception {
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return;
        }

        final File testFile = File.createTempFile("testSetPublicWritable", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            final ExecFileSystem efs = new ExecFileSystem();

            final FileSystemAttributes attributes = efs.getAttributes(testFilePath);
            assertNotNull(attributes);

            if (efs.getAttributes(testFilePath).isPublicWritable()) {
                // Umask is set as ?00, i.e. all files are public writable.
                // Nothing to test.
                return;
            }

            // Make public writable
            attributes.setPublicWritable(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isPublicWritable());

            // Make writable
            attributes.setPublicWritable(false);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertFalse(efs.getAttributes(testFilePath).isPublicWritable());

            // Set back again
            attributes.setPublicWritable(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isPublicWritable());
        } finally {
            testFile.delete();
        }
    }

    public void testIsExecutable() throws Exception {
        final File testFile = File.createTempFile("testIsExecutable", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                // On Windows, these methods always pretend to succeed.
                assertTrue(nativeImpl.getAttributes(testFilePath).isExecutable());
                return;
            }

            final ExecFileSystem efs = new ExecFileSystem();
            final FileSystemAttributes attributes = nativeImpl.getAttributes(testFilePath);
            assertNotNull(attributes);

            // Set executable
            attributes.setExecutable(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isExecutable());

            // Set executable
            attributes.setExecutable(false);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertFalse(nativeImpl.getAttributes(testFilePath).isExecutable());

            // Set back again
            attributes.setExecutable(true);
            assertTrue(efs.setAttributes(testFilePath, attributes));
            assertTrue(nativeImpl.getAttributes(testFilePath).isExecutable());

        } finally {
            testFile.delete();
        }
    }

    public void testSetExecutable() throws Exception {
        final File testFile = File.createTempFile("testSetExecutable", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final String testFilePath = testFile.getAbsolutePath();

        try {
            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                return;
            }

            final ExecFileSystem efs = new ExecFileSystem();

            final FileSystemAttributes attributes = efs.getAttributes(testFilePath);
            assertNotNull(attributes);

            // Set executable
            attributes.setExecutable(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isExecutable());

            // Mac: set the immutable bit, ensure we remove it properly
            testSetImmutable(testFilePath);

            // Unset executable
            attributes.setExecutable(false);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertFalse(efs.getAttributes(testFilePath).isExecutable());

            // Mac: set the immutable bit, ensure we remove it properly
            testSetImmutable(testFilePath);

            // Set back again
            attributes.setExecutable(true);
            assertTrue(nativeImpl.setAttributes(testFilePath, attributes));
            assertTrue(efs.getAttributes(testFilePath).isExecutable());

        } finally {
            testFile.delete();
        }
    }

    /*
     * Helper method: sets the immutable flag on MacOS X to ensure we can clear
     * it properly
     */
    private void testSetImmutable(final String filename) throws Exception {
        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            final String[] args = new String[] {
                "chflags", //$NON-NLS-1$
                "uchg", //$NON-NLS-1$
                filename
            };

            final StringBuffer output = new StringBuffer();

            assertEquals(ExecHelpers.exec(args, output), 0);
        }
    }

    public void testGetModificationTime() throws Exception {
        /* No "touch" command on windows */
        if (!Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
            return;
        }

        /* Make sure we can handle modification time with some various times */

        /* current time */
        testGetModificationTime(Calendar.getInstance());

        /* unix epoch */
        final Calendar epoch = Calendar.getInstance();
        epoch.setTimeInMillis(0);
        testGetModificationTime(epoch);

        /* before the epoch */
        final Calendar preEpoch = Calendar.getInstance();
        epoch.setTimeInMillis(-86400000);
        testGetModificationTime(preEpoch);

        /* yesterday */
        final Calendar yesterday = Calendar.getInstance();
        epoch.setTimeInMillis(yesterday.getTimeInMillis() - 86400000);
        testGetModificationTime(preEpoch);
    }

    private void testGetModificationTime(final Calendar time) throws Exception {
        final File testFile = File.createTempFile("testGetFileSize", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            final FileSystemTime nowExpectedTime = new FileSystemTime(time.getTimeInMillis() / 1000);

            final String[] args = new String[] {
                "touch", //$NON-NLS-1$
                "-t", //$NON-NLS-1$
                MessageFormat.format(
                    "{0}{1}{2}{3}{4}.{5}", //$NON-NLS-1$
                    Integer.toString(time.get(Calendar.YEAR)),
                    (time.get(Calendar.MONTH) + 1) < 10 ? "0" + (time.get(Calendar.MONTH) + 1) //$NON-NLS-1$
                        : time.get(Calendar.MONTH) + 1,
                    time.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + time.get(Calendar.DAY_OF_MONTH) //$NON-NLS-1$
                        : time.get(Calendar.DAY_OF_MONTH),
                    time.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + time.get(Calendar.HOUR_OF_DAY) //$NON-NLS-1$
                        : time.get(Calendar.HOUR_OF_DAY),
                    time.get(Calendar.MINUTE) < 10 ? "0" + time.get(Calendar.MINUTE) : time.get(Calendar.MINUTE), //$NON-NLS-1$
                    time.get(Calendar.SECOND) < 10 ? "0" + time.get(Calendar.SECOND) : time.get(Calendar.SECOND)), //$NON-NLS-1$
                testFile.getAbsolutePath()
            };

            assertEquals(ExecHelpers.exec(args, new StringBuffer()), 0);

            final FileSystemTime nativeTime =
                nativeImpl.getAttributes(testFile.getAbsolutePath()).getModificationTime();

            assertEquals(nowExpectedTime, nativeTime);
            assertTrue(nowExpectedTime.compareTo(nativeTime) == 0);
        } finally {
            testFile.delete();
        }
    }

    public void testGetFileSize() throws Exception {
        final File testFile = File.createTempFile("testGetFileSize", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        final FileOutputStream outputStream = new FileOutputStream(testFile);

        final int fileSize = (int) (Math.random() * 1024 * 1024);

        for (int i = 0; i < fileSize; i++) {
            outputStream.write((int) (Math.random() * 254));
        }

        outputStream.close();

        assertTrue(nativeImpl.getAttributes(testFile.getAbsolutePath()).exists());
        assertEquals(fileSize, nativeImpl.getAttributes(testFile.getAbsolutePath()).getSize());

        testFile.delete();
    }

    public void testIsSymbolicLink() throws Exception {
        final File origFile = File.createTempFile("testIsSymbolicLink", ".orig"); //$NON-NLS-1$ //$NON-NLS-2$
        final String origFilePath = origFile.getAbsolutePath();

        final File linkFile = File.createTempFile("testIsSymbolicLink", ".link"); //$NON-NLS-1$ //$NON-NLS-2$
        final String linkFilePath = linkFile.getAbsolutePath();
        linkFile.delete();

        try {
            final ExecFileSystem efs = new ExecFileSystem();
            efs.createSymbolicLink(origFilePath, linkFilePath);

            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                // On Windows, these methods always return false.
                final FileSystemAttributes attributes = nativeImpl.getAttributes(linkFilePath);

                assertFalse(attributes.isSymbolicLink());
                return;
            }

            assertTrue(linkFile.exists());
            assertTrue(nativeImpl.getAttributes(linkFilePath).exists());
            assertTrue(nativeImpl.getAttributes(linkFilePath).isSymbolicLink());
        } finally {
            // Delete the link (not the original temp file).
            linkFile.delete();

            origFile.delete();
        }
    }

    public void testCreateSymbolicLink() throws Exception {
        final File origFile = File.createTempFile("testIsSymbolicLink", ".orig"); //$NON-NLS-1$ //$NON-NLS-2$
        final String origFilePath = origFile.getAbsolutePath();

        final File linkFile = File.createTempFile("testIsSymbolicLink", ".link"); //$NON-NLS-1$ //$NON-NLS-2$
        final String linkFilePath = linkFile.getAbsolutePath();
        linkFile.delete();

        try {
            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                // On Windows, these methods always return false.
                assertFalse(nativeImpl.createSymbolicLink(origFilePath, linkFilePath));
                return;
            }

            final ExecFileSystem efs = new ExecFileSystem();

            assertTrue(nativeImpl.createSymbolicLink(origFilePath, linkFilePath));
            assertTrue(linkFile.exists());
            assertTrue(efs.getAttributes(linkFilePath).isSymbolicLink());
        } finally {
            // Delete the link (not the original temp file).
            linkFile.delete();

            origFile.delete();
        }
    }

    public void testCreateTempFileSecure() throws Exception {
        final File tempFile = nativeImpl.createTempFileSecure("TEST", null); //$NON-NLS-1$

        assertTrue(tempFile.exists());

        // On Windows, this is equivalent to File.createTempFile()
        if (!Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final FileSystemAttributes attr = nativeImpl.getAttributes(tempFile.getAbsolutePath());
            assertTrue(attr.isOwnerOnly());
        }
    }
}