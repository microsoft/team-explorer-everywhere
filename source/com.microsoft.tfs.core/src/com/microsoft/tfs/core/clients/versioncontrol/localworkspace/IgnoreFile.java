// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.FileEncodingDetector;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

/**
 * Used by {@link LocalItemExclusionEvaluator}.
 *
 * @threadsafety thread-safe
 */
public class IgnoreFile {
    private final static Log log = LogFactory.getLog(IgnoreFile.class);

    private final static char c_includePrefix = '!';
    private final static char c_commentPrefix = '#';

    private final String m_directory;
    private final List<IgnoreEntry> m_ignoreEntries = new ArrayList<IgnoreEntry>();

    public IgnoreFile(final String directory) {
        m_directory = directory;
    }

    public void addEntry(final IgnoreEntry ignoreEntry) {
        m_ignoreEntries.add(ignoreEntry);
    }

    public static IgnoreFile load(final String directory) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$

        IgnoreFile toReturn = null;

        try {
            final String fileName = new File(directory, LocalItemExclusionEvaluator.IGNORE_FILE_NAME).getAbsolutePath();

            if (new File(fileName).exists()) {
                toReturn = new IgnoreFile(directory);

                // Discover the current encoding of the file and use that to
                // write
                // out the data

                final FileEncoding tfsEncoding =
                    FileEncodingDetector.detectEncoding(fileName, FileEncoding.AUTOMATICALLY_DETECT);

                Charset charset = CodePageMapping.getCharset(tfsEncoding.getCodePage(), false);

                if (charset == null) {
                    /*
                     * getCharset() couldn't find a Java Charset for the code
                     * page. This can happen if the file was detected as
                     * FileEncoding.BINARY.
                     */
                    charset = CodePageMapping.getCharset(FileEncoding.getDefaultTextEncoding().getCodePage());
                }

                FileInputStream fileStream = null;
                BufferedReader streamReader = null;

                try {
                    fileStream = new FileInputStream(fileName);
                    streamReader = new BufferedReader(new InputStreamReader(fileStream, charset.name()));

                    String line;

                    while (null != (line = streamReader.readLine())) {
                        addExcludeEntry(directory, toReturn, line);
                    }
                } finally {
                    if (fileStream != null) {
                        IOUtils.closeSafely(fileStream);
                    }

                    if (streamReader != null) {
                        IOUtils.closeSafely(streamReader);
                    }
                }
            }
        } catch (final Exception ex) {
            log.warn("Error loading ignore file", ex); //$NON-NLS-1$

            return null;
        }

        return toReturn;
    }

    /**
     * Load IgnoreFile using a pattern array
     *
     * @param directory
     * @param ignorePatterns
     * @return
     */
    public static IgnoreFile load(final String directory, final String[] ignorePatterns) {
        Check.notNull(directory, "directory"); //$NON-NLS-1$
        Check.notNull(ignorePatterns, "ignorePatterns"); //$NON-NLS-1$

        IgnoreFile toReturn = null;
        toReturn = new IgnoreFile(directory);

        for (final String pattern : ignorePatterns) {
            addExcludeEntry(directory, toReturn, pattern);
        }

        return toReturn;
    }

    /**
     * Add an exclude entry into IgnoreFile
     *
     * @param directory
     * @param toReturn
     * @param pattern
     * @return
     */
    private static void addExcludeEntry(final String directory, final IgnoreFile toReturn, String pattern) {
        try {
            if (pattern.length() > 0 && ((pattern.charAt(0) == '\ufeff') || (pattern.charAt(0) == '\ufffe'))) {
                pattern = pattern.substring(1);
            }

            pattern = pattern.trim();
            if (pattern.length() > 0) {
                if (pattern.charAt(0) == c_commentPrefix
                    || (pattern.charAt(0) == '\ufeff')
                        && (pattern.length() > 1 && pattern.charAt(1) == c_commentPrefix)) {
                    return;
                }

                // Convert forward and backslashes to native
                pattern = pattern.replace("/", File.separator); //$NON-NLS-1$
                pattern = pattern.replace("\\", File.separator); //$NON-NLS-1$

                boolean isExcluded = true;
                boolean isRecursive = true;
                boolean isFolderOnly = false;

                if (pattern.charAt(0) == c_includePrefix && pattern.length() > 1) {
                    isExcluded = false;
                    pattern = pattern.substring(1);
                }

                if (pattern.charAt(0) == File.separatorChar && pattern.length() > 1) {
                    isRecursive = false;
                    pattern = pattern.substring(1);
                }

                if (pattern.charAt(pattern.length() - 1) == File.separatorChar && pattern.length() > 1) {
                    isFolderOnly = true;
                    pattern = pattern.substring(0, pattern.length() - 1);
                }

                pattern = pattern.trim();
                if (pattern.length() > 0) {
                    toReturn.addEntry(
                        new IgnoreEntry(LocalPath.combine(directory, pattern), isExcluded, isRecursive, isFolderOnly));
                }
            }
        } catch (final Exception ex) {
            log.warn("Error parsing ignore file line", ex); //$NON-NLS-1$
        }
    }

    /**
     * Iterates through this IgnoreFile's list of exclusions to determine if the
     * item should be included, excluded, or if there is no result either way
     * from this IgnoreFile.
     *
     * @param path
     *        the local item to check (must not be <code>null</code> or empty)
     * @param isFolder
     *        <code>true</code> if the local item to check is a folder;
     *        <code>false</code> otherwise
     * @param startPath
     *        a local item which parents path. Only the path parts after
     *        startPath are checked (must not be <code>null</code> or empty)
     * @param exclusion
     *        if the return value is non-<code>null</code>, the exclusion which
     *        was applied (may be <code>null</code>)
     * @return <code>null</code> if no result, <code>true</code> if the item is
     *         excluded, <code>false</code> if the item is included
     */
    public Boolean isExcluded(
        final String path,
        final boolean isFolder,
        final String startPath,
        final AtomicReference<String> exclusion) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$
        Check.notNullOrEmpty(startPath, "startPath"); //$NON-NLS-1$

        // Debug.Assert(FileSpec.IsSubItem(path, startPath));

        if (exclusion != null) {
            exclusion.set(null);
        }

        Boolean toReturn = null;

        for (final IgnoreEntry ignoreEntry : m_ignoreEntries) {
            // Index of the first character of the first path part that we are
            // going to evaluate for exclusions.
            int i = startPath.length();

            if (startPath.charAt(startPath.length() - 1) != File.separatorChar) {
                i++;
            }

            int k = Integer.MAX_VALUE;

            if (null != ignoreEntry.path) {
                if (!LocalPath.isChild(ignoreEntry.path, path)) {
                    // This IgnoreEntry applies only to items under a certain
                    // path, and we don't meet the criteria.
                    continue;
                }

                k = ignoreEntry.path.length();

                if (ignoreEntry.path.charAt(ignoreEntry.path.length() - 1) != File.separatorChar) {
                    k++;
                }

                // Skip our start path part ahead
                i = Math.max(i, k);
            }

            while (i >= 0 && i < path.length()) {
                if (!ignoreEntry.isRecursive && i > k) {
                    break;
                }

                // Calculate the length of the current path part.
                int j = path.indexOf(File.separatorChar, i);

                if (j < 0) {
                    j = path.length();
                }

                // Don't match IsFolderOnly entries with an item that has
                // isFolder = false,
                // if this is the last path part.
                if (ignoreEntry.isFolderOnly && !isFolder && j == path.length()) {
                    break;
                }

                j -= i;

                boolean match = false;

                if (ignoreEntry.isEndsWith) {
                    if (ignoreEntry.pattern.length() <= j
                        && 0 == compareCaseInsensitive(
                            path,
                            i + j - ignoreEntry.pattern.length(),
                            ignoreEntry.pattern,
                            0,
                            ignoreEntry.pattern.length())) {
                        match = true;
                    }
                } else if (ignoreEntry.isStartsWith) {
                    if (ignoreEntry.pattern.length() <= j
                        && 0 == compareCaseInsensitive(path, i, ignoreEntry.pattern, 0, ignoreEntry.pattern.length())) {
                        match = true;
                    }
                } else if (ignoreEntry.isComplex) {
                    if (ItemPath.matchesWildcardFile(path.substring(i, i + j), ignoreEntry.pattern)) {
                        match = true;
                    }
                } else {
                    if (ignoreEntry.pattern.length() == j
                        && 0 == compareCaseInsensitive(path, i, ignoreEntry.pattern, 0, ignoreEntry.pattern.length())) {
                        match = true;
                    }
                }

                if (match) {
                    if (ignoreEntry.isExcluded) {
                        toReturn = true;
                        if (exclusion != null) {
                            exclusion.set(ignoreEntry.originalExclusion);
                        }
                    }
                    // Otherwise this is an inclusion; only match if this
                    // is the last path part which matched
                    else if (i + j == path.length()) {
                        toReturn = false;
                        if (exclusion != null) {
                            exclusion.set(ignoreEntry.originalExclusion);
                        }
                    }
                }

                i += (j + 1);
            }
        }

        // No declaration either way (excluded or included)
        return toReturn;
    }

    public String getDirectory() {
        return m_directory;
    }

    public String getFullPath() {
        if (m_directory == null || m_directory.length() == 0) {
            return ""; //$NON-NLS-1$
        }

        return new File(m_directory, LocalItemExclusionEvaluator.IGNORE_FILE_NAME).getAbsolutePath();
    }

    /**
     * Similar to .NET's String.Compare(String, Int32, String, Int32, Int32,
     * StringComparison), but always does a locale-invariant case-insensitive
     * comparison instead of honoring a StringComparison arg.
     * <p>
     * This method is here and not in {@link LocaleInvariantStringHelpers}
     * because it does bounds checking in the .NET way, which differs from
     * Java's {@link String#regionMatches(boolean, int, String, int, int)}
     * enough to keep it separate. But the match algorithm at the bottom could
     * be useful in a new method there.
     */
    private int compareCaseInsensitive(
        final String strA,
        final int offsetA,
        final String strB,
        final int offsetB,
        final int length) {
        if (strA == null && strB != null) {
            return -1;
        } else if (strA != null && strB == null) {
            return 1;
        }
        // .NET is documented to throw when length > 0 and one is not null
        else if ((strA == null || strB == null) && length > 0) {
            throw new IndexOutOfBoundsException("length cannot be > 0 with null string arguments"); //$NON-NLS-1$
        }

        // End values exclusive
        final int endA = offsetA + length;
        final int endB = offsetB + length;

        if (endA > strA.length()) {
            throw new IndexOutOfBoundsException("offsetA + length is past the end of strA"); //$NON-NLS-1$
        }

        if (endB > strB.length()) {
            throw new IndexOutOfBoundsException("offsetB + length is past the end of strB"); //$NON-NLS-1$
        }

        /*
         * We checked that we won't walk off the end of either string, so we can
         * use just one of the end indices (endA) to limit our loop.
         */
        for (int iA = offsetA, iB = offsetB; iA < endA; iA++, iB++) {
            char a = strA.charAt(iA);
            char b = strB.charAt(iB);

            if (a != b) {
                // Try both as upper case
                a = Character.toUpperCase(a);
                b = Character.toUpperCase(b);
                if (a != b) {
                    // Try both as lower case
                    a = Character.toLowerCase(a);
                    b = Character.toLowerCase(b);
                    if (a != b) {
                        // Unicode difference works
                        return a - b;
                    }
                }
            }
        }

        // Got to end of both segments without a difference
        return 0;
    }
}
