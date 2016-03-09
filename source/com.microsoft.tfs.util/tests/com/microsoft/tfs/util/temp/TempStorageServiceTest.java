// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.temp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import junit.framework.TestCase;

public class TempStorageServiceTest extends TestCase {
    public void testCreateTempFile() throws IOException {
        final File file = TempStorageService.getInstance().createTempFile();
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());

        TempStorageService.getInstance().cleanUpItem(file);
        assertFalse(file.exists());
    }

    public void testCreateTempFileExtension() throws IOException {
        final String extension = ".fun"; //$NON-NLS-1$

        // Test with custom extension
        File file = TempStorageService.getInstance().createTempFile(extension);
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals("wrong extension", file.getName().substring(file.getName().lastIndexOf('.')), extension); //$NON-NLS-1$

        TempStorageService.getInstance().cleanUpItem(file);
        assertFalse(file.exists());

        // Test with null extension (should use default)
        file = TempStorageService.getInstance().createTempFile(null);
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals("wrong extension", file.getName().substring(file.getName().lastIndexOf('.')), ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$

        TempStorageService.getInstance().cleanUpItem(file);
        assertFalse(file.exists());

        // Test with empty extension (should use default)
        file = TempStorageService.getInstance().createTempFile(""); //$NON-NLS-1$
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals("wrong extension", file.getName().substring(file.getName().lastIndexOf('.') + 1), "tmp"); //$NON-NLS-1$ //$NON-NLS-2$

        TempStorageService.getInstance().cleanUpItem(file);
        assertFalse(file.exists());
    }

    public void testCreateTempFileCustomDirectory() throws IOException {
        final File directory = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
        final String extension = ".fun"; //$NON-NLS-1$

        // Test with custom extension
        final File file = TempStorageService.getInstance().createTempFile(directory, extension);
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertEquals("not in the correct custom directory", directory, file.getParentFile()); //$NON-NLS-1$
        assertEquals("wrong extension", file.getName().substring(file.getName().lastIndexOf('.')), extension); //$NON-NLS-1$

        TempStorageService.getInstance().cleanUpItem(file);
        assertFalse(file.exists());
    }

    public void testCreateTempDirectory() throws IOException {
        final File directory = TempStorageService.getInstance().createTempDirectory();
        assertNotNull(directory);
        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());

        TempStorageService.getInstance().cleanUpItem(directory);
        assertFalse(directory.exists());
    }

    public void testCleanUpAllItems() throws IOException {
        /*
         * Allocate a temp file and directory, put items in that directory.
         * After cleanup, both the temp file and temp directory should be gone.
         * Tests that cleaning up directories recursively deletes files.
         */

        final File file = TempStorageService.getInstance().createTempFile();
        assertNotNull(file);
        assertTrue(file.exists());
        assertTrue(file.isFile());

        final File directory = TempStorageService.getInstance().createTempDirectory();
        assertNotNull(directory);
        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());

        final File file1 = new File(directory, "file1"); //$NON-NLS-1$
        final FileOutputStream file1Stream = new FileOutputStream(file1);
        file1Stream.write("Fun".getBytes()); //$NON-NLS-1$
        file1Stream.close();
        assertTrue(file1.exists());

        final File file2 = new File(directory, "file2"); //$NON-NLS-1$
        final FileOutputStream file2Stream = new FileOutputStream(file2);
        file2Stream.write("Bar".getBytes()); //$NON-NLS-1$
        file2Stream.close();
        assertTrue(file2.exists());

        TempStorageService.getInstance().cleanUpAllItems();

        assertFalse(file.exists());
        assertFalse(directory.exists());
        assertFalse(file1.exists());
        assertFalse(file2.exists());
    }

    public void testForgetItem() throws IOException {
        /*
         * Create two temp files and two temp directories, forget one of each,
         * clean up items, ensure the forgotton ones stay and the others are
         * deleted.
         */

        final File deleteFile = TempStorageService.getInstance().createTempFile();
        final File keepFile = TempStorageService.getInstance().createTempFile();

        final File deleteDirectory = TempStorageService.getInstance().createTempDirectory();
        final File keepDirectory = TempStorageService.getInstance().createTempDirectory();

        assertTrue(deleteFile.exists());
        assertTrue(keepFile.exists());
        assertTrue(deleteDirectory.exists());
        assertTrue(keepDirectory.exists());

        TempStorageService.getInstance().forgetItem(keepFile);
        TempStorageService.getInstance().forgetItem(keepDirectory);

        // Forgetting a clean up item that doesn't exist should not throw
        TempStorageService.getInstance().forgetItem(new File("abcdefghijklmnop")); //$NON-NLS-1$

        TempStorageService.getInstance().cleanUpAllItems();

        assertFalse(deleteFile.exists());
        assertTrue(keepFile.exists());
        assertFalse(deleteDirectory.exists());
        assertTrue(keepDirectory.exists());

        keepFile.delete();
        keepDirectory.delete();
    }
}
