// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Some utility methods to manage files (copy a file, detect invalid NTFS
 * characters, detect whether the current filesystem ignores case, etc.).
 */
public final class FileHelpers {
    private static final String FORCE_HONOR_CASE_SYSPROP = "com.microsoft.tfs.util.FileHelpers.force-honor-case"; //$NON-NLS-1$

    private static final String FORCE_IGNORE_CASE_SYSPROP = "com.microsoft.tfs.util.FileHelpers.force-ignore-case"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(FileHelpers.class);

    /**
     * Statically initialized with the result of a filesystem case sensitivity
     * test.
     */
    private static boolean fileSystemIgnoresCase;

    /**
     * Characters not allowed in NTFS file paths ("C:\Folder\File.txt")
     */
    // @formatter:off
    private static final char[] INVALID_NTFS_PATH_CHARACTERS = new char[]
    {
        (char) 0, (char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7,
        (char) 8, (char) 9, (char) 10, (char) 11, (char) 12, (char) 13, (char) 14, (char) 15,
        (char) 16, (char) 17, (char) 18, (char) 19, (char) 20, (char) 21, (char) 22, (char) 23,
        (char) 24, (char) 25, (char) 26, (char) 27, (char) 28, (char) 29, (char) 30, (char) 31,
        '\"', '<', '>', '|'
    };
    // @formatter:on

    /**
     * Truth table based on {@link #INVALID_NTFS_FILE_NAME_CHARACTERS}.
     */
    // @formatter:off
    public final static boolean[] VALID_NTFS_FILE_NAME_CHAR_TABLE = {
        false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
        true,  true,  false, true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  false,
        true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  false, true,  false, true,  false, true,
        true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
        true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  false, true,  true,  true,
        true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
        true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  false, true,  true,  true
    };
    // @formatter:on

    private static char[] WILDCARD_CHARACTERS = new char[] {
        '?',
        '*'
    };

    /**
     * All entries in this list have length 3, and the implementation of
     * isReservedName depends upon this If you change the contents of this list
     * you will likely need to modify IsReservedName
     */
    private final static String[] RESERVED_NAMES_LENGTH3 = {
        "CON", //$NON-NLS-1$
        "PRN", //$NON-NLS-1$
        "AUX", //$NON-NLS-1$
        "NUL" //$NON-NLS-1$
    };

    static {
        /*
         * I dont know of a great way to test the running "platform" for
         * case-sensitivity in filesystems, because that behavior is usually a
         * property of the filesystem in use (and there may be many of those at
         * once).
         *
         * Sun/Oracle's Java File class simply hard-codes case-insensitive path
         * compares on Windows, case-sensitive on Unix, even though these
         * systems could be using filesystems that do the opposite.
         *
         * So here's a simple hard-coded test.
         */
        if (System.getProperty(FORCE_IGNORE_CASE_SYSPROP) != null) {
            fileSystemIgnoresCase = true;
        } else if (System.getProperty(FORCE_HONOR_CASE_SYSPROP) != null) {
            fileSystemIgnoresCase = false;
        } else if (Platform.isCurrentPlatform(Platform.WINDOWS) || Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            fileSystemIgnoresCase = true;
        } else {
            // Generic Unix and unknown
            fileSystemIgnoresCase = false;
        }
    }

    /**
     * @return true if the filesystem this JVM is running on ignores case when
     *         comparing file names (new File("A").equals(new File("a"))), false
     *         if it does not.
     */
    public static boolean doesFileSystemIgnoreCase() {
        return fileSystemIgnoresCase;
    }

    /**
     * Tests whether two files have the exact same contents by comparing every
     * byte. If the file lengths are different, or one or both files do not
     * exist, the files are not opened and false is returned.
     *
     * @param first
     *        the first file (not null).
     * @param second
     *        the second file (not null).
     * @return true if the files have identical sizes and contents, false if
     *         they differ.
     */
    public static boolean contentsEqual(final File first, final File second) throws FileNotFoundException, IOException {
        Check.notNull(first, "first"); //$NON-NLS-1$
        Check.notNull(second, "second"); //$NON-NLS-1$

        if (first.exists() == false || second.exists() == false || first.length() != second.length()) {
            return false;
        }

        // Lengths are equal, so only test first's length.
        if (first.length() == 0) {
            return true;
        }

        InputStream firstStream = null;
        InputStream secondStream = null;
        try {
            firstStream = new BufferedInputStream(new FileInputStream(first));
            secondStream = new BufferedInputStream(new FileInputStream(second));

            int firstValue;
            int secondValue;

            while (true) {
                firstValue = firstStream.read();
                secondValue = secondStream.read();

                if (firstValue != secondValue) {
                    return false;
                }

                // EOF on both.
                if (firstValue == -1) {
                    return true;
                }
            }
        } finally {
            try {
                firstStream.close();
            } catch (final IOException e) {
            }

            try {
                secondStream.close();
            } catch (final IOException e) {
            }
        }
    }

    /**
     * Check if the specified name is in the list of reserved NTFS names.
     *
     * @param name
     *        the file name to check
     * @return true if name is a reserved NTFS file name
     */
    public static boolean isReservedName(final String name) {
        /*
         * This method gets called *often* and is written for speed, even to the
         * point of being fragile with respect to changes to the reservedNames
         * and reservedNamesLength3 lists. Changes to the list of reserved names
         * will likely require code changes here.
         */

        // LPT1 -> LPT9, COM1 -> COM9 are reserved names.
        // LPT0 and COM0 are NOT reserved names.
        if (name.length() == 4 && Character.isDigit(name.charAt(3)) && name.charAt(3) != '0') {
            final String firstThree = name.substring(0, 3);
            if (firstThree.equalsIgnoreCase("LPT") || firstThree.equalsIgnoreCase("COM")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return true;
            }
        }

        // All of the strings in reservedNamesLength3 are length 3.
        if (name.length() == 3) {
            for (int i = 0; i < RESERVED_NAMES_LENGTH3.length; i++) {
                if (name.equalsIgnoreCase(RESERVED_NAMES_LENGTH3[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Tests the given name for validity in the NTFS namespace. Names should not
     * be full paths.Wildcards are not allowed.
     *
     * @param name
     *        the name (not full path) to test.
     * @return true if the name is valid in the NTFS namespace, false if it is
     *         invalid.
     */
    public static boolean isValidNTFSFileName(final String name) {
        return isValidNTFSFileName(name, false);
    }

    /**
     * Tests the given name for validity in the NTFS namespace. pathWildcards
     * are not allowed.
     *
     * @param path
     *        the full path to test.
     * @return true if the name is valid in the NTFS namespace, false if it is
     *         invalid.
     */
    public static boolean isValidNTFSPath(final String path) {
        return isValidNTFSFileName(path, false);
    }

    /**
     * Tests the given name for validity in the NTFS namespace. Wildcards can be
     * allowed. Names should not be full paths.
     *
     * @param name
     *        the name (not full path) to test.
     * @param permitWildcards
     *        whether to consider wildcard characters valid in the names.
     * @return true if the name is valid in the NTFS namespace, false if it is
     *         invalid.
     */
    public static boolean isValidNTFSFileName(final String name, final boolean permitWildcards) {
        for (int i = 0; i < name.length(); i++) {
            if (isValidNTFSFileNameCharacter(name.charAt(i)) == false) {
                return false;
            }

            if (permitWildcards == false) {
                for (int j = 0; j < WILDCARD_CHARACTERS.length; j++) {
                    if (name.charAt(i) == WILDCARD_CHARACTERS[j]) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Tests the given name for validity in the NTFS namespace. Wildcards can be
     * allowed.
     *
     * @param path
     *        the path (not full path) to test.
     * @param permitWildcards
     *        whether to consider wildcard characters valid in the path.
     * @return true if the path is valid in the NTFS namespace, false if it is
     *         invalid.
     */
    public static boolean isValidNTFSPath(final String path, final boolean permitWildcards) {
        for (int i = 0; i < path.length(); i++) {
            if (isValidNTFSPathCharacter(path.charAt(i)) == false) {
                return false;
            }

            if (permitWildcards == false) {
                for (int j = 0; j < WILDCARD_CHARACTERS.length; j++) {
                    if (path.charAt(i) == WILDCARD_CHARACTERS[j]) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Tests whether the given character is valid in an NTFS file name.
     *
     * @param c
     *        the character to test
     * @return true if the character is allowed in NTFS file names, false if it
     *         is disallowed
     */
    public static boolean isValidNTFSFileNameCharacter(final char c) {
        // All of our illegal characters are in the ASCII range (0x00 -> 0x7f),
        // so if this character has a code point higher than 0x7f, it must be
        // valid.
        if (c > '\u007f') {
            return true;
        }

        // This character is in our truth table.
        return VALID_NTFS_FILE_NAME_CHAR_TABLE[c];
    }

    /**
     * Tests whether the given character is valid in an NTFS file path.
     *
     * @param c
     *        the character to test
     * @return true if the character is allowed in NTFS file paths, false if it
     *         is disallowed
     */
    public static boolean isValidNTFSPathCharacter(final char c) {
        for (int i = 0; i < INVALID_NTFS_PATH_CHARACTERS.length; i++) {
            if (c == INVALID_NTFS_PATH_CHARACTERS[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a string with the contents of the given string except characters
     * which are invalid NTFS file name characters have been removed or
     * replaced.
     *
     * @param name
     *        the original name (not null)
     * @return the name with the invalid NTFS characters removed. This string
     *         may be empty (if the given string's characters were all invalid)
     */
    public static String removeInvalidNTFSFileNameCharacters(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        final StringBuilder sb = new StringBuilder();

        final int nameLength = name.length();
        for (int i = 0; i < nameLength; i++) {
            if (isValidNTFSFileNameCharacter(name.charAt(i))) {
                sb.append(name.charAt(i));
            }
        }

        return sb.toString();
    }

    /**
     * Returns a string with the contents of the given string except characters
     * which are invalid NTFS file path characters have been removed or
     * replaced.
     *
     * @param path
     *        the original path (not null)
     * @return the name with the invalid NTFS characters removed. This string
     *         may be empty (if the given string's characters were all invalid)
     */
    public static String removeInvalidNTFSPathCharacters(final String path) {
        Check.notNull(path, "name"); //$NON-NLS-1$

        final StringBuilder sb = new StringBuilder();

        final int nameLength = path.length();
        for (int i = 0; i < nameLength; i++) {
            if (isValidNTFSPathCharacter(path.charAt(i))) {
                sb.append(path.charAt(i));
            }
        }

        return sb.toString();
    }

    /**
     * @equivalence filenameMatches(filename, pattern,
     *              doesFileSystemIgnoreCase())
     */
    public static boolean filenameMatches(final String filename, final String pattern) {
        return filenameMatches(filename, pattern, doesFileSystemIgnoreCase());
    }

    /**
     * Tests the given filename to determine whether it matches the simple
     * glob-style pattern (ie, DOS-like '?' and '*' wildcards) given by pattern.
     * Note that the '\' character acts as an escape character, beware when
     * using DOS-style full paths.
     *
     * @param filename
     *        the file name to test
     * @param pattern
     *        the DOS-style globbing pattern to test against
     * @return true if the name matches the pattern, false if it does not.
     */
    public static boolean filenameMatches(final String filename, final String pattern, final boolean ignoreCase) {
        return (filenameMatchesInternal(filename, pattern, ignoreCase) == 1);
    }

    /**
     * Internal implementation for filenameMatches.
     *
     * Based heavily on wildmat 1.4 by Rich Salz (Apr 5, 1991)
     * http://groups.google.com/group/comp.sources.misc/msg/ebf19a3339debbcd []
     * character classes have been removed
     *
     * @param filename
     *        the file name to test
     * @param pattern
     *        the DOS-style globbing pattern to test against
     * @param ignoreCase
     *        <code>true</code> to do a case-insensitive match,
     *        <code>false</code> to do a case-sensitive match
     * @return 1 if the name matches the pattern, 0 or -1 if it does not.
     */
    private static int filenameMatchesInternal(final String filename, final String pattern, final boolean ignoreCase) {
        final int filenameLength = filename.length();
        final int patternLength = pattern.length();
        int f, p;

        for (f = 0, p = 0; p < patternLength; f++, p++) {
            switch (pattern.charAt(p)) {
                case '\\':
                    // disallow trailing \
                    if (++p == patternLength) {
                        return 0;
                        // fallthrough
                    }

                    // next character must match exactly
                default:
                    if (ignoreCase) {
                        if (!LocaleInvariantStringHelpers.caseInsensitiveEquals(
                            filename.charAt(f),
                            pattern.charAt(p))) {
                            return 0;
                        }
                    } else if (filename.charAt(f) != pattern.charAt(p)) {
                        return 0;
                    }

                    continue;

                    // ? allows exactly one character
                case '?':
                    continue;

                    // * allows arbitrary number of characters
                case '*':
                    // eat consecutive stars
                    while (p < patternLength && pattern.charAt(p) == '*') {
                        p++;
                    }

                    // trailing star matches everything
                    if (p == patternLength) {
                        return 1;
                    }

                    // recurse beginning at the next char after the *
                    while (f < filenameLength) {
                        final int match =
                            filenameMatchesInternal(filename.substring(f++), pattern.substring(p), ignoreCase);

                        if (match != 0) {
                            return match;
                        }
                    }

                    // abort - this will trickle down to prevent retrying with
                    // the next character
                    return -1;
            }
        }

        return ((f == filenameLength) ? 1 : 0);
    }

    /**
     * @see {@link #rename(File, File)}
     */
    public static void rename(final String source, final String target) throws IOException {
        Check.notNull(source, "source"); //$NON-NLS-1$ )
        Check.notNull(target, "target"); //$NON-NLS-1$

        rename(new File(source), new File(target));
    }

    /**
     * Like {@link File#renameTo(File)}, but on Windows it can overwrite
     * existing files. The algorithm doesn't provide atomicity, it fails fast,
     * and it tries to leave the files in their original states in the case of a
     * failure.
     * <p>
     * This method exists mainly because {@link File#renameTo(File)} behaves
     * very differently depending on platform. On Unix, it's mostly like
     * rename(2) (atomic transaction, obeys only directory permissions during
     * the operation, clobbers existing files). On Windows it won't overwrite
     * existing files no-matter the permissions involved. Deleting the
     * destination file first opens up a short race that complicates the
     * algorithm.
     * <p>
     * This method is <b>not</b> guaranteed to work across filesystems.
     *
     * @param source
     *        the existing file that will be renamed to the given target file.
     *        Not null.
     * @param target
     *        the file that the existing file will be renamed to. If this file
     *        exists it will be overwritten. Not null.
     */
    public static void rename(final File source, final File target) throws IOException {
        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(target, "target"); //$NON-NLS-1$

        if (source.exists() == false) {
            throw new FileNotFoundException(MessageFormat.format("Source file {0} does not exist", source)); //$NON-NLS-1$
        }

        File tempFile = null;

        if (target.getParent() != null) {
            createDirectoryIfNecessary(target.getParent());
        }

        if (target.exists()) {
            /*
             * The target exists, so we'll need to move it to a temp name.
             */
            tempFile = new File(target.getAbsolutePath() + "-" + System.currentTimeMillis() + ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$

            if (tempFile.exists()) {
                /*
                 * This is highly improbable.
                 */
                final String messageFormat = "Temp file {0} already exists before rename"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, tempFile);
                log.warn(message);
                throw new IOException(message);
            }

            if (target.renameTo(tempFile) == false) {
                final String messageFormat = "Could not rename target {0} to temp file for rename"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, target);
                log.warn(message);
                throw new IOException(message);
            }
        }

        /*
         * Do the main rename.
         */
        if (source.renameTo(target)) {
            /*
             * The main rename succeeded, so failing to delete the temp file is
             * not a fatal error (but annoying).
             */
            if (tempFile != null && tempFile.delete() == false) {
                final String messageFormat = "Error deleting temp file {0} after successful rename, leaving"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, tempFile);
                log.warn(message);
            }
        } else {
            log.warn("Main rename failed (source permissions problem?), trying to rename temp file back"); //$NON-NLS-1$

            /*
             * Target shouldn't exist (we renamed it to a temp file) unless some
             * other process put it there.
             */
            if (target.exists()) {
                final String messageFormat = "Target {0} exists when it should not, lost race to some other process?"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, target);
                log.warn(message);
                throw new IOException(message);
            }

            if (tempFile != null && tempFile.renameTo(target) == false) {
                final String messageFormat = "Error renaming temp file {0} back to target {1} after failed main rename"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, tempFile, target);
                log.warn(message);
                throw new IOException(message);
            }

            final String messageFormat = Messages.getString("FileHelpers.FailedToRenameFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, source, target);
            log.warn(message);
            throw new IOException(message);
        }
    }

    public static void deleteFileWithoutException(final String path) {
        try {
            new File(path).delete();
        } catch (final Throwable t) {
        }
    }

    public static boolean deleteDirectory(final String directoryPath) {
        return deleteDirectory(new File(directoryPath));
    }

    public static boolean deleteDirectory(final File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }

        /* Depth-first directory traversal */
        final File[] subdirectories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        });

        for (final File subdirectory : subdirectories) {
            if (!deleteDirectory(subdirectory)) {
                return false;
            }
        }

        final File[] files = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isFile();
            }
        });

        for (final File file : files) {
            if (!file.delete()) {
                return false;
            }
        }

        return directory.delete();
    }

    /**
     * Ensures that the parent folder of the given path exists, and creates it
     * if necessary.
     *
     * @param path
     */
    public static void createDirectoryIfNecessary(final String path) throws IOException {
        final File directory = new File(path);

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                final String messageFormat = Messages.getString("FileHelpers.FailedToCreateFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, path);
                throw new IOException(message);
            }
        } else if (!directory.isDirectory()) {
            final String messageFormat = Messages.getString("FileHelpers.FailedToCreateAlreadyExistsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, path);
            throw new IOException(message);
        }
    }
}
