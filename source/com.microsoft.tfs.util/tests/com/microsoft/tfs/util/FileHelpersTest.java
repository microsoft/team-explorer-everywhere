// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

public class FileHelpersTest extends TestCase {
    public void testSimple() {
        assertTrue(FileHelpers.filenameMatches("abc", "abc")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("abc", "def")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testCaseSensitive() {
        assertTrue(FileHelpers.filenameMatches("abc", "abc", false)); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("abc", "ABC", false)); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(FileHelpers.filenameMatches("abcdef", "abc*", false)); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("abcdef", "ABC*", false)); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(FileHelpers.filenameMatches("abcXdef", "abc?def", false)); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("abcXdef", "abc?deF", false)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testCaseInsensitive() {
        assertTrue(FileHelpers.filenameMatches("abc", "abc", true)); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(FileHelpers.filenameMatches("abc", "ABC", true)); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(FileHelpers.filenameMatches("abcdef", "abc*", true)); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(FileHelpers.filenameMatches("abcdef", "ABC*", true)); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(FileHelpers.filenameMatches("abcXdef", "abc?def", true)); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(FileHelpers.filenameMatches("abcXdef", "abc?deF", true)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testLeadingStar() {
        assertTrue(FileHelpers.filenameMatches("filename.java", "*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("filename.java", "*.jav")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testTrailingStar() {
        assertTrue(FileHelpers.filenameMatches("filename.java", "filename.*")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("filename.java", "filenam.*")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEmbeddedStar() {
        assertTrue(FileHelpers.filenameMatches("filename.java", "file*.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("filename.java", "file*..java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEmbeddedQuestion() {
        assertTrue(FileHelpers.filenameMatches("filename.java", "file?ame.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("filename.java", "file?name.java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("filename.java", "fi?name.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testMultipleStars() {
        assertTrue(FileHelpers.filenameMatches("filename.java", "fil*e.j*a")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(FileHelpers.filenameMatches("abcxxxx", "a*b*c*")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testMultipleQuestions() {
        assertTrue(FileHelpers.filenameMatches("filename.java", "file?ame?java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testLiterals() {
        assertTrue(FileHelpers.filenameMatches("file*name?java", "file\\*name\\?java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(FileHelpers.filenameMatches("file*name.java", "file\\*name?java")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(FileHelpers.filenameMatches("file*name?java", "file\\*name.java")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private File makeSourceTempFile() {
        try {
            final File ret = File.createTempFile("source", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
            final FileWriter fw = new FileWriter(ret);
            fw.write("source\n"); //$NON-NLS-1$
            fw.close();
            return ret;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File makeTargetTempFile() {
        try {
            final File ret = File.createTempFile("target", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
            final FileWriter fw = new FileWriter(ret);
            fw.write("target\n"); //$NON-NLS-1$
            fw.close();
            return ret;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void testRenameWithNonexistingTarget() throws IOException {
        final File source = makeSourceTempFile();
        final File target = makeTargetTempFile();
        target.delete();
        assertTrue(source.exists());
        assertTrue(target.exists() == false);
        try {
            FileHelpers.rename(source, target);
            assertTrue(source.exists() == false);
            assertTrue(target.exists());
        } catch (final IOException e) {
            assertTrue(false);
        }
        target.delete();
    }

    public void testRenameWithExistingTarget() {
        final File source = makeSourceTempFile();
        final File target = makeTargetTempFile();
        assertTrue(source.exists());
        assertTrue(target.exists());
        try {
            FileHelpers.rename(source, target);
            assertTrue(source.exists() == false);
            assertTrue(target.exists());
        } catch (final IOException e) {
            assertTrue(false);
        }
        target.delete();
    }

    public void testRenameWithReadOnlySource() {
        final File source = makeSourceTempFile();
        final File target = makeTargetTempFile();
        assertTrue(source.exists());
        assertTrue(target.exists());
        source.setReadOnly();
        try {
            FileHelpers.rename(source, target);
            assertTrue(source.exists() == false);
            assertTrue(target.exists());
        } catch (final IOException e) {
            assertTrue(false);
        }
        target.delete();
    }

    public void testRenameWithReadOnlyTarget() {
        final File source = makeSourceTempFile();
        final File target = makeTargetTempFile();
        assertTrue(source.exists());
        assertTrue(target.exists());
        target.setReadOnly();
        try {
            FileHelpers.rename(source, target);
            assertTrue(source.exists() == false);
            assertTrue(target.exists());
        } catch (final IOException e) {
            assertTrue(false);
        }
        target.delete();
    }

    public void testRenameWithReadOnlySourceAndTarget() {
        final File source = makeSourceTempFile();
        final File target = makeTargetTempFile();
        assertTrue(source.exists());
        assertTrue(target.exists());
        source.setReadOnly();
        target.setReadOnly();
        try {
            FileHelpers.rename(source, target);
            assertTrue(source.exists() == false);
            assertTrue(target.exists());
        } catch (final IOException e) {
            assertTrue(false);
        }
        target.delete();
    }
}