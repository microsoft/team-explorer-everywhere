// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.LocalPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * Static methods to process local (disk) paths.
 * </p>
 *
 * @warning Character case is not consistently ignored among all methods of this
 *          class. The host platform's conventional behavior with regards to
 *          case sensitivity will determine whether two strings differing only
 *          in case reference the same file in most methods, but a few are
 *          documented to only work without respect to character case. Look at
 *          the Javadoc of each method for details.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public abstract class LocalPath {
    public final static char TFS_PREFERRED_LOCAL_PATH_SEPARATOR = '\\';

    public final static String GENERAL_LOCAL_PATH_SEPARATOR = "/"; //$NON-NLS-1$

    /*
     * What exactly is semicolon used for in VC? This field could have a better
     * name.
     */
    /**
     * Characters used by VC that cannot appear in mapped local paths.
     */
    private final static char[] VERSION_CONTROL_SEPARATORS = new char[] {
        ';'
    };

    /**
     * Matches if the base name portion of the file ends with ~[0-9]\ or ~[0-9]/
     * or ~[0-9]. or ~[0-9]
     */
    private static Pattern EIGHT_DOT_THREE_CHECKER =
        Pattern.compile("[\\\\/]([^\\\\/]*~\\d+)(\\.[^.]{1,3})?([\\\\/]|$)"); //$NON-NLS-1$

    /**
     * Compares well-formed local path strings in a top-down fashion (parents
     * sort before their children).
     */
    public static final Comparator<String> TOP_DOWN_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String path1, final String path2) {
            return LocalPath.compareTopDown(path1, path2);
        };
    };

    /**
     * Compares well-formed local path strings in a bottom-up fashion (children
     * sort before their parents).
     */
    public static final Comparator<String> BOTTOM_UP_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String path1, final String path2) {
            return LocalPath.compareBottomUp(path1, path2);
        };
    };

    /**
     * Gets the full path to the current working directory.
     *
     * @return the full path to the current working directory.
     */
    public final static String getCurrentWorkingDirectory() {
        /*
         * The canonical path is the best path to return, because it eliminates
         * the . at the end of the path ("/a/b/."). The path with the dot is
         * just as valid as the path without it, but it's prettier without it.
         */
        try {
            return new File(".").getCanonicalPath(); //$NON-NLS-1$
        } catch (final IOException e) {
            return new File(".").getAbsolutePath(); //$NON-NLS-1$
        }
    }

    /**
     * <p>
     * Compares two local paths for equality. Character case is honored when the
     * operating system is case-sensitive (
     * {@link FileHelpers#doesFileSystemIgnoreCase()}). Paths are not normalized
     * (".." segments removed) before comparison.
     * </p>
     * <p>
     * Two null paths are equal, but a null path does not equal a non-null path.
     * </p>
     *
     * @param path1
     *        the first local path (may be <code>null</code>)
     * @param path2
     *        the second local path (may be <code>null</code>)
     * @return true if the paths are equal, false if not.
     */
    public final static boolean equals(final String path1, final String path2) {
        return LocalPath.equals(path1, path2, false);
    }

    /**
     * <p>
     * Compares two local paths for equality, optionally forcing the comparison
     * to ignore character case. Paths are not normalized (".." segments
     * removed) before comparison.
     * </p>
     * <p>
     * Two null paths are equal, but a null path does not equal a non-null path.
     * </p>
     *
     * @see LocalPath#hashCode(String)
     *
     * @param path1
     *        the first local path (may be <code>null</code>)
     * @param path2
     *        the second local path (may be <code>null</code>)
     * @param forceIgnoreCase
     *        if <code>true</code>, case is always ignored in the comparison. If
     *        <code>false</code>, case is only ignored if
     *        {@link FileHelpers#doesFileSystemIgnoreCase()} returns
     *        <code>true</code>
     * @return true if the paths are equal, false if not.
     */
    public final static boolean equals(final String path1, final String path2, final boolean forceIgnoreCase) {
        if (path1 == path2) {
            return true;
        }

        if (path1 == null || path2 == null) {
            return false;
        }

        /*
         * Visual Studio avoids hitting the operating system for this
         * comparison, doing a String.Equals(StringComparison.OrdinalIgnoreCase)
         * on the strings with no normalization.
         *
         * Java's Server.equalsIgnoreCase() provides a similar locale-invariant
         * case-insensitive test.
         */
        if (forceIgnoreCase || FileHelpers.doesFileSystemIgnoreCase()) {
            return path1.equalsIgnoreCase(path2);
        } else {
            return path1.equals(path2);
        }
    }

    public final static boolean startsWith(final String path, final String prefix) {
        if (path.equals(prefix)) {
            return true;
        }

        if (path == null || prefix == null) {
            return false;
        }

        if (FileHelpers.doesFileSystemIgnoreCase()) {
            return LocaleInvariantStringHelpers.caseInsensitiveStartsWith(path, prefix);
        } else {
            return path.startsWith(prefix);
        }
    }

    public final static boolean endsWith(final String path, final String suffix) {
        if (path.equals(suffix)) {
            return true;
        }

        if (path == null || suffix == null) {
            return false;
        }

        if (FileHelpers.doesFileSystemIgnoreCase()) {
            return LocaleInvariantStringHelpers.caseInsensitiveEndsWith(path, suffix);
        } else {
            return path.endsWith(suffix);
        }
    }

    /**
     * Compare two file paths in a platform-default-case-sensitivity fashion on
     * every portion of the path except for the last item in the path. The last
     * item in the path is compared in a case-sensitive fashion.
     *
     * @param path1
     *        the first local path (must not be <code>null</code>)
     * @param path2
     *        the second local path (must not be <code>null</code>)
     * @see FileHelpers#doesFileSystemIgnoreCase()
     * @return true if the paths differ, false if they are the same
     */
    public final static boolean lastPartEqualsCaseSensitive(final String path1, final String path2) {
        final String parent1 = getDirectory(path1);
        final String parent2 = getDirectory(path2);

        if (equals(parent1, parent2) == false) {
            return false;
        }

        return getFileName(path1).equals(getFileName(path2));
    }

    /**
     * Returns a hash code for the given local path. This method for calculating
     * hash codes must be used when {@link LocalPath#equals(String, String)} is
     * used to test for equality so the case-sensitivity behavior matches (and
     * the hashCode/equals contract is maintained).
     *
     * @param localPath
     *        the local path to compute the hash code for (must not be
     *        <code>null</code>)
     * @return the hash code value
     */
    public final static int hashCode(final String localPath) {
        /*
         * LocalPath.equals(String, String) lets Java's String do the comparison
         * without converting the string to upper/lower, and does this in a
         * local-invariant way (which performs nicely).
         *
         * Computing a hash code isn't as simple because String.toLowerCase()
         * and String.toUpperCase() are locale-sensitive and may convert case
         * differently than equals(). So use a simple variation on
         * String.hashCode() that upper-cases in a locale-invariant way.
         *
         * java.io.File XORs a constant when computing its hashes. Let's assume
         * it's a good thing and do the same.
         */
        if (FileHelpers.doesFileSystemIgnoreCase()) {
            return LocaleInvariantStringHelpers.caseInsensitiveHashCode(localPath) ^ 1234321;
        } else {
            return localPath.hashCode() ^ 1234321;
        }
    }

    /**
     * Tests whether one path is a direct child of another path (which would be
     * the parent).
     *
     * @param parentPath
     *        the path to the parent folder (must not be <code>null</code>)
     * @param possibleChild
     *        the path of the possible child item (must not be <code>null</code>
     *        )
     * @return true if the possibleChild is a direct child of the parentPath
     */
    public static boolean isDirectChild(final String parentPath, final String possibleChild) {
        Check.notNull(parentPath, "parentPath"); //$NON-NLS-1$
        Check.notNull(possibleChild, "possibleChild"); //$NON-NLS-1$

        return LocalPath.equals(parentPath, LocalPath.getParent(possibleChild));
    }

    /**
     * <p>
     * Tests the given paths for a parent-child relationship. A path is a child
     * of another if the object it describes would reside below the object
     * described by the parent path in the local filesystem. Case is respected
     * on a per-platform basis.
     * <p>
     * </p>
     * A possible child that is equivalent to the parent path (both refer to the
     * same object) is considered a child. This is compatible with Visual
     * Studio's behavior.
     * </p>
     * <p>
     * The given paths are not canonicalized before testing.
     * </p>
     * <p>
     * The given paths must be in the OS's native path format.
     * </p>
     *
     * @param parentPath
     *        the local path to the parent item (not null).
     * @param possibleChild
     *        the local path of the possible child item (not null).
     * @return true if possibleChild is a child of parentPath, false otherwise
     *         (including I/O errors accessing either path).
     */
    public final static boolean isChild(final String parentPath, final String possibleChild) {
        Check.notNull(parentPath, "parentPath"); //$NON-NLS-1$
        Check.notNull(possibleChild, "possibleChild"); //$NON-NLS-1$

        // See this methods Javadoc for why this is true.
        if ((FileHelpers.doesFileSystemIgnoreCase() && parentPath.equalsIgnoreCase(possibleChild))
            || parentPath.equals(possibleChild)) {
            return true;
        }

        final File parent = new File(parentPath);
        final File child = new File(possibleChild);

        /*
         * This may be less efficient than is otherwise possible, but it should
         * respect all the platform rules about file and directory naming. We
         * walk up the possible child's path, testing each parent directory
         * along the way. If it matches the given parentPath, the possible child
         * is indeed a child.
         */

        File tmp = child.getParentFile();
        while (tmp != null) {
            // If this temp parent is equal to the given parent, we have a
            // match.
            if (tmp.equals(parent)) {
                return true;
            }

            // Keep walking back up.
            tmp = tmp.getParentFile();
        }

        return false;
    }

    /**
     * Tests whether a directory is empty (contains no files or directories).
     *
     * @param localPath
     *        the local directory path to test (must not be <code>null</code>)
     * @return true if the given path contains no directories or files, false if
     *         it contains directories or files.
     */
    public final static boolean isDirectoryEmpty(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        return new File(localPath).list().length == 0;
    }

    /**
     * Converts a TFS-style path into this platform's pathstyle. On Windows, the
     * string is unaltered, on Unix the drive letter is removed and the
     * backslashes are converted to slashes.
     *
     * @param localPath
     *        the TFS-style local path string to convert. If null, null is
     *        returned.
     * @return this platform's preferred style of local path string, null if the
     *         given localPath was null.
     */
    public final static String tfsToNative(String localPath) {
        if (localPath == null) {
            return null;
        }

        Check.notEmpty(localPath, "localPath"); //$NON-NLS-1$

        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) && localPath.startsWith("U:")) //$NON-NLS-1$
        {
            // Remove the leading "U:" and convert backslashes to our favorite
            // separator.
            localPath =
                localPath.substring(2).replace(LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR, File.separatorChar);
        }

        return localPath;
    }

    /**
     * Converts this platform's file path to TFS's preferred separator
     * (backslash) and roots it in a DOS-style drive letter if not already
     * rooted in one.
     *
     * @param localPath
     *        the absolute local path string to convert to TFS-style separators.
     *        If null, null is returned.
     * @return the TFS-style path, which looks like an absolute Windows path
     *         name, null if the given localPath was null.
     */
    public final static String nativeToTFS(String localPath) {
        if (localPath == null) {
            return null;
        }

        Check.notEmpty(localPath, "localPath"); //$NON-NLS-1$

        /*
         * Unix paths must be altered by prepending a drive letter, and flipping
         * the slashes. Avoid double-converting if the path has already been
         * swapped.
         */
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) && !localPath.startsWith("U:")) //$NON-NLS-1$
        {
            localPath = "U:" + localPath.replace(File.separatorChar, LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR); //$NON-NLS-1$
        }

        return localPath;
    }

    /**
     * Calls {@link #getFolderDepth(String, Platform)} with
     * {@link Platform#getCurrentPlatform()} as the {@link Platform} argument.
     *
     * @see #getFolderDepth(String, Platform)
     */
    public static int getFolderDepth(final String localPath) {
        return getFolderDepth(localPath, Platform.getCurrentPlatform());
    }

    /**
     * <p>
     * Returns the depth of the item described by path, where the root folder is
     * depth 0, items in root are 1, items below that are 2, etc. UNC paths are
     * supported on Windows, but are not supported on other platforms (an
     * exception is thrown). The path must be absolute, well-formed, and must
     * not contain . or .. segments.
     * </p>
     * <h3>UNC Paths</h3>
     * <p>
     * On Windows, UNC paths and drive letters are supported. In this case a
     * string like "\\server\share\" is the root path (depth 0). If the share
     * name part is not specified, {@link IllegalArgumentException} is thrown.
     * </p>
     * <p>
     * On non-Windows platforms, parsing UNC and drive letter paths is undefined
     * because those paths would not be valid absolute local paths (all absolute
     * paths must start with "/" and UNC and drive letters do not).
     * </p>
     * <p>
     * Safe for remote paths. Does not access the local disk or network shares.
     * </p>
     * <p>
     * <ul>
     * <li>\ will return 0</li>
     * <li>\a will return 1</li>
     * <li>c:\ will return 0</li>
     * <li>c:\a will return 1</li>
     * <li>c:\a\ will return 1</li>
     * <li>c:\a\b will return 2</li>
     * <li>c:\a\b\ will return 2</li>
     * <li>c:\a\b\zap will return 3</li>
     * <li>c:\a\b\zap\ will return 3</li>
     * <li>\\localhost\share1 will return 0</li>
     * <li>\\localhost\share1\ will return 0</li>
     * <li>\\localhost\share1\a will return 1</li>
     * <li>\\localhost\share1\a\ will return 1</li>
     * <li>\\localhost\share1\a\bar will return 2</li>
     * <li>\\localhost\share1\a\bar\ will return 2</li>
     * <li>/ will return 0</li>
     * <li>/a will return 1</li>
     * <li>/a/ will return 1</li>
     * <li>/a/b will return 2</li>
     * <li>/a/b/ will return 2</li>
     * <li>/a/b/zap will return 3</li>
     * <li>/a/b/zap/ will return 3</li>
     * </p>
     *
     * @param localPath
     *        the local path to test (not <code>null</code> or empty)
     * @param platform
     *        the {@link Platform} whose path conventions should be used (must
     *        not be <code>null</code>)
     * @return the depth from root, where root is 0, first directories are 1,
     *         etc.
     * @throws IllegalArgumentException
     *         if the path is not absolute, or is in an invalid format, or
     *         another argument requirement was not satisfied
     */
    public static int getFolderDepth(final String localPath, final Platform platform) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(platform, "forPlatform"); //$NON-NLS-1$

        final boolean isWindows = platform.contains(Platform.WINDOWS);
        final boolean isUnix = platform.contains(Platform.GENERIC_UNIX);

        /*
         * To avoid hitting the local disk (or blocking on network paths) avoid
         * the File class. Since the input path won't contain relative segments,
         * we can count separators.
         *
         * First detect the type of path and isolate the root prefix.
         */

        String separator;
        if (isWindows) {
            separator = "\\"; //$NON-NLS-1$

            /*
             * Quick test for root.
             */
            if (localPath.equals(separator)) {
                return 0;
            }

            /*
             * Quick check for the slightly exceptional rules around drive roots
             * on Windows. A path of "C:" is a relative path--absolute paths
             * require the backslash, like "C:\".
             */
            if (localPath.length() == 2 && localPath.charAt(1) == ':') {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("LocalPath.DriveRootPathNotAbsoluteFormat"), //$NON-NLS-1$
                        localPath,
                        separator));
            }
        } else if (isUnix) {
            // Generic Unix
            separator = "/"; //$NON-NLS-1$

            /*
             * Quick test for root.
             */
            if (localPath.equals(separator)) {
                return 0;
            }

            // Quick check for absolute paths.
            if (localPath.charAt(0) != separator.charAt(0)) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("LocalPath.PathNotAbsoluteMustStartWithSeparatorFormat"), //$NON-NLS-1$
                        localPath,
                        separator));
            }
        } else {
            throw new RuntimeException(
                MessageFormat.format(Messages.getString("LocalPath.UnsupportedPlatformFormat"), platform.toString())); //$NON-NLS-1$
        }

        final String[] segments = localPath.split(Pattern.quote(separator));

        if (isWindows) {
            if (localPath.startsWith("\\\\")) //$NON-NLS-1$
            {
                /*
                 * UNC path. Ensure both the server and share name are present
                 * (first two segments are empty, third is server, fourth is
                 * share name).
                 */
                if (segments.length < 4 || segments[2].length() == 0 || segments[3].length() == 0) {
                    throw new IllegalArgumentException(
                        MessageFormat.format(
                            Messages.getString("LocalPath.UNCPathIncompleteFormat"), //$NON-NLS-1$
                            localPath));
                }

                return segments.length - 4;
            }

            /*
             * Absolute files may start with a backslash or a drive letter,
             * colon, backslash. The "path is single backslash" case was handled
             * above.
             */
            if (localPath.startsWith(separator)
                || (localPath.length() > 2
                    && localPath.charAt(1) == ':'
                    && localPath.charAt(2) == separator.charAt(0))) {
                /*
                 * In the first case, segments contains an empty leading element
                 * (because of initial backslash). In the second case, segments
                 * contains the drive spec as first element. Either way,
                 * subtract one to get depth.
                 */

                return segments.length - 1;
            }

            throw new IllegalArgumentException(
                MessageFormat.format(Messages.getString("LocalPath.PathNotAbsoluteFormat"), localPath)); //$NON-NLS-1$
        } else if (isUnix) {
            /*
             * Unix root folder was handled above.
             */
            return segments.length - 1;
        } else {
            throw new RuntimeException(
                MessageFormat.format(Messages.getString("LocalPath.UnsupportedPlatformFormat"), platform.toString())); //$NON-NLS-1$
        }
    }

    /**
     * Combines the two given paths into one path string, using this platform's
     * preferred path separator character. If relative is an absolute path, it
     * is returned as the entire return value (parent is discarded).
     *
     * @param parent
     *        the first (left-side) path component (must not be
     *        <code>null</code>)
     * @param relative
     *        the second (right-side) path component (must not be
     *        <code>null</code>)
     */
    public final static String combine(final String parent, final String relative) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.notNull(relative, "relative"); //$NON-NLS-1$

        /*
         * Return the relative path if it's already absolute, since I'm not sure
         * Java's File does this in the same way.
         */
        final File relativeFile = new File(relative);
        if (relativeFile.isAbsolute()) {
            return relative;
        }

        /*
         * Let Java combine the paths.
         */
        return new File(parent, relative).getAbsolutePath();
    }

    /**
     * Returns all directories leading up to (and including) the given path,
     * from the root of the local filesystem. Note that canonical parents are
     * returned.
     *
     * @param path
     *        the path to get parent paths for (must not be <code>null</code>)
     * @return the canonical parent path strings
     */
    public static String[] getHierarchy(final String path) {
        final List<String> hierarchyList = new ArrayList<String>();

        File subfile = new File(path).getAbsoluteFile();

        while (subfile != null) {
            hierarchyList.add(subfile.getPath());
            subfile = subfile.getParentFile();
        }

        final int len = hierarchyList.size();
        final String[] hierarchy = new String[len];

        for (int i = 0; i < len; i++) {
            hierarchy[i] = hierarchyList.get(len - i - 1);
        }

        return hierarchy;
    }

    /**
     * Get file hierarchy from fromPath to toPath e.g. if fromPath is
     * /usr/local/bin, toPath is /usr, hierarchy is [usr, local, bin] fromPath
     * has to be beneath (child of) toPath
     *
     * @param fromPath
     * @param Path
     * @return
     */
    public static String[] getHierarchy(final String fromPath, final String toPath) {
        if (StringUtil.isNullOrEmpty(toPath)) {
            return getHierarchy(fromPath);
        } else if (StringUtil.isNullOrEmpty(fromPath)) {
            return getHierarchy(toPath);
        }

        final List<String> hierarchyList = new ArrayList<String>();

        final File toFile = new File(toPath);
        File subfile = new File(fromPath).getAbsoluteFile();
        while (subfile != null) {
            hierarchyList.add(subfile.getPath());

            if (subfile.equals(toFile)) {
                break;
            }
            subfile = subfile.getParentFile();
        }

        final int len = hierarchyList.size();
        final String[] hierarchy = new String[len];

        for (int i = 0; i < len; i++) {
            hierarchy[i] = hierarchyList.get(len - i - 1);
        }

        return hierarchy;
    }

    /**
     * Returns the parent directory name of the given local path. Note that this
     * differs from {@link #getDirectory(String)} in that it will always return
     * the parent path, regardless of whether path is a file or folder.
     *
     * @param path
     *        the local path (file or directory) to get the parent directory of
     *        (must not be <code>null</code>)
     * @return the parent directory of the given path.
     */
    public static String getParent(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        return new File(path).getParent();
    }

    /**
     * Returns the directory part of the local path. If the path describes a
     * directory, the same path is returned. If the path describes a file, the
     * path to its parent directory is returned.
     *
     * @param path
     *        the local path (file or directory) to get the directory part of
     *        (must not be <code>null</code>)
     * @return the directory part of the given path.
     */
    public static String getDirectory(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        final File f = new File(path);
        if (f.isDirectory()) {
            return f.getPath();
        } else {
            return f.getParent();
        }
    }

    /**
     * Gets just the file part of the given local path, which is all of the
     * string after the last path component. If there are no separators, the
     * entire string is returned. If the string ends in a separator, an empty
     * string is returned.
     *
     * @param localPath
     *        the local path from which to parse the file part (must not be
     *        <code>null</code>)
     * @return the file name at the end of the given local path, or the given
     *         path if no separator characters were found, or an empty string if
     *         the given path ends with a separator.
     */
    public static String getFileName(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        final int index = localPath.lastIndexOf(File.separator);

        if (index == -1) {
            return localPath;
        }

        /*
         * Add 1 to return the part after the sep, unless that would be longer
         * than the string ("$/a/b/" would be that case).
         */
        if (index + 1 < localPath.length()) {
            return localPath.substring(index + 1);
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Gets the extension of the given local path -- everything from the last
     * '.' of the file part (including the '.'). If there is no dot, an empty
     * string is returned.
     *
     * @param localPath
     *        the local path to parse the extension from (must not be
     *        <code>null</code>)
     * @return the file extension at the end of the given path, or an empty
     *         string if no extension separator ('.') exists.
     */
    public static String getFileExtension(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        final String localFilePart = LocalPath.getFileName(localPath);

        final int index = localFilePart.lastIndexOf('.');

        if (index == -1 || localFilePart.length() <= index + 1) {
            return ""; //$NON-NLS-1$
        }

        return localFilePart.substring(index);
    }

    /**
     * Return the the last path component in the supplied path. If an empty
     * string is supplied for the path, an empty string is returned.
     *
     * @param path
     *        the path string to examine (must not be <code>null</code>)
     * @return the last path component of the given path string, which would be
     *         the file name if the path string references a file, and the
     *         directory furthest down the hierarchy if the path references a
     *         directory.
     */
    public final static String getLastComponent(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        return new File(path).getName();
    }

    /**
     * Compares two local paths (ordinal character comparison) placing children
     * after their parents. Ignores case if the current platform does. Paths are
     * not normalized (".." segments removed) before comparison.
     * <p>
     * If sorting paths for display to the user, better to use a
     * {@link Collator}-based comparison instead of this one.
     *
     * @param pathA
     *        the first path to compare (must not be <code>null</code>)
     * @param pathB
     *        the second path to compare (must not be <code>null</code>)
     * @return see {@link Comparable#compareTo(Object)}
     * @see FileHelpers#doesFileSystemIgnoreCase()
     */
    public final static int compareTopDown(final String pathA, final String pathB) {
        /*
         * Visual Studio's implementation ultimately compares path segments
         * using CultureInfo.InvariantCulture.CompareInfo.Compare(...,
         * StringComparison.OrdinalIgnoreCase).
         *
         * Java's Server.equalsIgnoreCase() provides a similar locale-invariant
         * case-insensitive test.
         */

        if (FileHelpers.doesFileSystemIgnoreCase()) {
            return pathA.compareToIgnoreCase(pathB);
        } else {
            return pathA.compareTo(pathB);
        }
    }

    /**
     * Compares two server paths (ordinal character comparison) placing parents
     * after their children. Ignores case if the current platform does. Paths
     * are not normalized (".." segments removed) before comparison.
     * <p>
     * If sorting paths for display to the user, better to use a
     * {@link Collator}-based comparison instead of this one.
     *
     * @param pathA
     *        the first path to compare (must not be <code>null</code>)
     * @param pathB
     *        the second path to compare (must not be <code>null</code>)
     * @return see {@link Comparable#compareTo(Object)}
     * @see FileHelpers#doesFileSystemIgnoreCase()
     */
    public final static int compareBottomUp(final String pathA, final String pathB) {
        return 0 - LocalPath.compareTopDown(pathA, pathB);
    }

    /**
     * <p>
     * Gets the canonical form of the given path. On Unix platforms (including
     * Mac OS X), a path that begins with "~" or "~user", where user is a user
     * name on this host, is expanded to the full path to the absolute path to
     * the user's home directory (and any other path components, of course).
     * </p>
     * <p>
     * On case-preserving but case-insensitive filesystems, this implementation
     * differs from Java's {@link File#getCanonicalPath()} in one important way:
     * if the given path string refers to a filesystem object (file or
     * directory) that exists on disk but has a name (not path) that differs
     * only in character case, the path is still made canonical according to
     * {@link File#getCanonicalPath()} but the character case of the final path
     * element (object's name) is taken from the given path. If the given path
     * does not exist on disk, or does exist but the final elements have
     * matching character case, the path returned by
     * {@link File#getCanonicalPath()} is returned. This behavior allows us to
     * make a relative path string "canonical" (in the {@link LocalPath} way)
     * and support the case-changing rename case (where only the case of the
     * file changes).
     * </p>
     * <p>
     * On case-sensitive filesystems (most Unixes, not Mac OS X) this method
     * behaves exactly like Java's {@link File#getCanonicalPath()}.
     * </p>
     *
     * @param path
     *        the path to canonicalize. If null is given, null is returned.
     * @return the canonical version of the given path, null if path was null.
     */
    public final static String canonicalize(final String path) {
        if (path == null) {
            return null;
        }

        /*
         * Expand any leading tilde sequences to home directory names (on Unix
         * only). This method won't do the JNI call unless we're on Unix and the
         * string needs it (starts with ~), so we can call it often.
         */
        final String expandedPath = LocalPath.expandTildeToHome(path);

        final File child = new File(expandedPath).getAbsoluteFile();
        final File parent = child.getParentFile();

        if (parent == null) {
            return child.getPath(); // this is already an absolute path
        }

        File canonicalFile;
        final String name = child.getName();

        try {
            if (name.equals(".") || name.equals("..")) //$NON-NLS-1$//$NON-NLS-2$
            {
                /*
                 * symlink is always false in this case
                 */
                canonicalFile = child.getCanonicalFile();
            } else {
                canonicalFile = new File(parent.getCanonicalFile(), name);
            }
        } catch (final IOException e) {
            canonicalFile = child; // this is already an absolute path
        }

        final String canonicalPath = canonicalFile.getPath();

        // Be compatible with .NET's path length limits, which apply to all of
        // TFS VC
        if (canonicalPath.length() > VersionControlConstants.MAX_LOCAL_PATH_SIZE) {
            throw new PathTooLongException(
                MessageFormat.format(
                    Messages.getString("LocalPath.InvalidPathTooLongCanonicalizeFormat"), //$NON-NLS-1$
                    Integer.toString(VersionControlConstants.MAX_LOCAL_PATH_SIZE),
                    canonicalPath));
        }

        return canonicalPath;
    }

    /**
     * <p>
     * Tests whether the given local path contains wildcard characters in its
     * final path element. Wildcards in initial path elements (intermediate
     * directories) are ignored.
     * </p>
     * <p>
     * Character case is ignored during wildcard matching.
     * </p>
     *
     * @param localPath
     *        the local path to test for wildcards (in last element only). Not
     *        null.
     * @return true if the last path element contains wildcards, false
     *         otherwise.
     */
    public static boolean isWildcard(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        /*
         * Find the last occurance of any valid separator character.
         */
        final int largestIndex = localPath.lastIndexOf(File.separator);

        /*
         * If we found one at the last character, this path has an "empty" last
         * element, and thus denotes a directory that shouldn't be considered
         * with wildcards.
         */
        if (largestIndex == localPath.length() - 1) {
            return false;
        }

        /*
         * Call the Wildcard class's method with the remainder of the string.
         */
        return Wildcard.isWildcard(localPath.substring(largestIndex + 1));
    }

    /**
     * <p>
     * Matches one item against a wildcard tuple (item path and wildcard pattern
     * to apply to that item path), optionally allowing recursive matches.
     * </p>
     * <p>
     * Character case is ignored during wildcard matching.
     * </p>
     *
     * @param firstItemPath
     *        the item to test against the wildcard pattern. This is a full path
     *        (must not be <code>null</code>)
     * @param secondItemFolderPath
     *        a path where the wildcard pattern will be applied to find the item
     *        described by firstItemPath. If this parameter is a child of
     *        firstItemPath, there wildcard pattern will never match. If this
     *        parameter is a direct parent of firstItemPath, the wildcard can
     *        match. If this parameter is a grandparent (or greater), the
     *        pattern can only match if the recursive parameter is true. If this
     *        parameter is null, false is returned.
     * @param secondItemWildcardPattern
     *        the wildcard pattern to apply to secondItemFolderPath.
     * @param recursive
     *        if true, the wildcard pattern will apply to secondItemFolderPath
     *        and all its possible children. If false, the wildcard pattern will
     *        only match direct children of secondItemFolderPath.
     * @return true if the firstItemPath matches the wildcard specification,
     *         false if it does not.
     */
    public final static boolean matchesWildcard(
        String firstItemPath,
        String secondItemFolderPath,
        final String secondItemWildcardPattern,
        final boolean recursive) {
        Check.notNull(firstItemPath, "firstItemPath"); //$NON-NLS-1$

        firstItemPath = LocalPath.canonicalize(firstItemPath);
        secondItemFolderPath = LocalPath.canonicalize(secondItemFolderPath);

        String firstItemFolder = null;
        String firstItemName = null;

        if (secondItemWildcardPattern == null || secondItemWildcardPattern.length() == 0) {
            firstItemFolder = firstItemPath;
        } else {
            firstItemFolder = LocalPath.getDirectory(firstItemPath);

            /*
             * If the folder part of the first item path is the same as the
             * input, there is no file.
             */
            if (LocalPath.equals(firstItemFolder, firstItemPath) == false) {
                firstItemName = LocalPath.getLastComponent(firstItemPath);
            }
        }

        /*
         * Test the folder part if there is one.
         *
         * If recursion is on and the first item folder isn't a child of the
         * second item folder path, there can be no match.
         */
        if (secondItemFolderPath != null) {
            if (recursive) {
                if (LocalPath.isChild(secondItemFolderPath, firstItemFolder) == false) {
                    return false;
                }
            } else {
                /*
                 * Recursion is off, and they don't match exactly, so there can
                 * be no match.
                 */
                if (LocalPath.equals(firstItemFolder, secondItemFolderPath) == false) {
                    return false;
                }
            }
        }

        /*
         * If there is no file part to test, we have a match.
         */
        if (firstItemName == null) {
            return true;
        }

        /*
         * Test the file part using the generic wildcard match test.
         */
        return ItemPath.matchesWildcardFile(firstItemName, 0, secondItemWildcardPattern, 0);
    }

    /**
     * <p>
     * Returns a new string describing the first given path made relative to the
     * second given path.
     * </p>
     * <p>
     * Character case is ignored during string comparison, so strings with
     * mismatched-in-case common elements will still succeed in being made
     * relative.
     * </p>
     * <p>
     * Paths are not normalized (for ending separators, case, etc.). It is the
     * caller's responsibility to make sure the relativeTo path can be matched.
     * </p>
     *
     * @param localPath
     *        the path to the local item to describe (must not be
     *        <code>null</code>)
     * @param relativeTo
     *        the path that the first parameter will be described relative to
     *        (must not be <code>null</code>)
     * @return the relative path, or the unaltered given local path if it could
     *         not be made relative to the second path.
     */
    public static String makeRelative(final String localPath, final String relativeTo) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(relativeTo, "relativeTo"); //$NON-NLS-1$

        /*
         * The absolute path must start with the relativeTo path, else it's not
         * relative. We're using a case-insensitve compare because any working
         * folder items won't be allowed to conflict in case only (because the
         * server would not allow it).
         *
         * Use regionMatches() for a locale-invariant "starts with" test.
         */
        if (localPath.regionMatches(true, 0, relativeTo, 0, relativeTo.length())) {
            if (localPath.length() == relativeTo.length()) {
                return ""; //$NON-NLS-1$
            }

            /*
             * If the relativeTo path ends in a separator, we have a relative
             * path to express.
             */
            if (relativeTo.length() > 0 && relativeTo.charAt(relativeTo.length() - 1) == File.separatorChar) {
                return localPath.substring(relativeTo.length());
            }

            /*
             * If the given path's last character is a separator, then we also
             * have a relative path.
             */
            if (localPath.charAt(relativeTo.length()) == File.separatorChar) {
                return localPath.substring(relativeTo.length() + 1);
            }
        }

        /*
         * Return the local path unaltered.
         */
        return localPath;
    }

    /**
     * <p>
     * Maps a local path to a server path, given a parent local path of the path
     * to be mapped, and a server path that corresponds to the parent.
     * </p>
     * <p>
     * Character case is ignored during string comparison, so strings with
     * mismatched-in-case common elements will still succeed in being made
     * relative.
     * </p>
     * <p>
     * Paths are not normalized (for ending separators, case, etc.). It is the
     * caller's responsibility to make sure the relativeToLocalPath path can be
     * matched.
     * </p>
     *
     * @param localPath
     *        the local path to convert to a server path (must not be
     *        <code>null</code>)
     * @param relativeToLocalPath
     *        the parent local path (must not be <code>null</code> and must be a
     *        parent of <code>localPath</code>)
     * @param serverRoot
     *        the server path that corresponds to
     *        <code>relativeToLocalPath</code> (must not be <code>null</code>)
     * @return the corresponding server path (never <code>null</code>)
     */
    public static String makeServer(final String localPath, final String relativeToLocalPath, final String serverRoot) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(relativeToLocalPath, "relativeToLocalPath"); //$NON-NLS-1$
        Check.notNull(serverRoot, "serverRoot"); //$NON-NLS-1$

        final String relativePart = LocalPath.makeRelative(localPath, relativeToLocalPath);

        /*
         * Convert this platform's separator characters into TFS's separator
         * character.
         */
        final StringBuilder relativeBuffer = new StringBuilder(relativePart);
        for (int k = 0; k < relativeBuffer.length(); k++) {
            if (relativeBuffer.charAt(k) == File.separatorChar) {
                relativeBuffer.setCharAt(k, ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            }
        }

        /*
         * If the relative path begins with a separator, remove it, so we can
         * always add one later.
         */
        if (relativeBuffer.length() > 0 && relativeBuffer.charAt(0) == ServerPath.PREFERRED_SEPARATOR_CHARACTER) {
            relativeBuffer.deleteCharAt(0);
        }

        /*
         * ServerPath.combine() would work to combine these parts, but it
         * doesn't check for illegal dollars in the path (it specifically
         * permits the relative part to start with a dollar; other dollars are
         * caught--legacy behavior).
         *
         * We've enusred the relative part doesn't start with a separator, so
         * combine here.
         */

        // Checks for illegal dollar
        return ServerPath.canonicalize(
            serverRoot + ServerPath.PREFERRED_SEPARATOR_CHARACTER + relativeBuffer.toString());
    }

    /**
     * Returns the given path string without trailing separators (as specified
     * by File.separator).
     *
     * @param path
     *        the string to strip trailing separators from.
     * @return the given string with all trailing separators removed.
     */
    public static String removeTrailingSeparators(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        final int length = path.length();
        int index = path.length() - 1;

        while (index > 0 && path.charAt(index) == File.separatorChar) {
            index--;
        }

        return index < length - 1 ? path.substring(0, index + 1) : path;
    }

    /**
     * Returns the given path string with a trailing separator (as specified by
     * File.separator).
     *
     * @param path
     *        the string to add a trailing separator.
     * @return the given string with a trailing separator added.
     */
    public static String addTrailingSeparator(final String path) {
        if (path == null) {
            return null;
        }
        return removeTrailingSeparators(path) + File.separatorChar;
    }

    /**
     * <p>
     * Expands any "~user" or "~" sequences in the given local path string to
     * the absolute paths to those directories, preserving any trailing path
     * parts. The JNI home directory lookup is only performed if this platform
     * is not Windows and the "~user" format is encountered.
     * </p>
     * <p>
     * Also, leading whitespace is always removed from the given path string.
     * </p>
     *
     * @param pathString
     *        the path string to expand for tildes (must not be
     *        <code>null</code>)
     * @return the path string with "~user" and "~" expanded; the given string
     *         is returned unaltered (except for the removal of leading
     *         whitespace) if the username cannot be found or does not start
     *         with a "~".
     */
    protected static String expandTildeToHome(String pathString) {
        Check.notNull(pathString, "pathString"); //$NON-NLS-1$

        /*
         * This method must return on non-Unix because we don't have an
         * implementation of nativeGetHomeDirectory() for those platforms.
         */
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) == false) {
            return pathString;
        }

        /*
         * Strip any leading whitespace so we can match against the beginning of
         * the string.
         */
        pathString = StringUtil.trimBegin(pathString);

        if (pathString.length() == 0) {
            return pathString;
        }

        if (pathString.charAt(0) == '~') {
            String replacement;
            final int sepIndex = pathString.indexOf(File.separatorChar);

            if (pathString.length() == 1 || sepIndex == 1) {
                /*
                 * "~" or "~/<...>"
                 *
                 * Get our home directory as the replacement.
                 */
                replacement = LocalPath.removeTrailingSeparators(System.getProperty("user.home")); //$NON-NLS-1$
            } else {
                /*
                 * "~bar" or "~bar/<...>"
                 *
                 * Get bar's home directory as the replacement.
                 */

                final String homeDirectory = PlatformMiscUtils.getInstance().getHomeDirectory(
                    pathString.substring(1, sepIndex != -1 ? sepIndex : pathString.length()));

                /*
                 * If the username could not be found in the database, we want
                 * to return the string unmolested (except for the whitespace we
                 * stripped).
                 */
                if (homeDirectory == null) {
                    return pathString;
                }

                replacement = LocalPath.removeTrailingSeparators(homeDirectory);
            }

            if (sepIndex == -1) {
                /*
                 * No separator was found, so we return the replacement as the
                 * whole string.
                 */
                return replacement;
            } else {
                /*
                 * Return the replacement plus the remainder of the string.
                 *
                 * Example: "/home/userId" + "/a/b.txt"
                 */
                return replacement + pathString.substring(sepIndex);
            }
        }

        return pathString;
    }

    public static ItemValidationError checkLocalItem(
        final String item,
        final String parameterName,
        final boolean allowNull,
        final boolean allowWildcards,
        final boolean allow8Dot3Paths,
        final boolean checkReservedCharacters) {
        if (item == null || item.length() == 0) {
            if (!allowNull) {
                throw new NullPointerException(parameterName);
            }
        } else {
            if (ServerPath.isServerPath(item)) {
                return ItemValidationError.LOCAL_ITEM_REQUIRED;
            } else if (!allowWildcards && ItemPath.isWildcard(item)) {
                return ItemValidationError.WILDCARD_NOT_ALLOWED;
            } else if (item.length() > VersionControlConstants.MAX_LOCAL_PATH_SIZE) {
                throw new PathTooLongException(
                    MessageFormat.format(
                        Messages.getString("LocalPath.InvalidPathTooLongCheckItemFormat"), //$NON-NLS-1$
                        item,
                        Integer.toString(VersionControlConstants.MAX_LOCAL_PATH_SIZE)));
            } else if (!isPathRooted(item)) {
                // Path is relative
                throw new LocalPathFormatException(
                    MessageFormat.format(
                        Messages.getString("LocalPath.InvalidPathFormat"), //$NON-NLS-1$
                        item));
            } else if (item.startsWith("\\") && item.startsWith("\\\\") == false) //$NON-NLS-1$ //$NON-NLS-2$
            {
                // Path does not contain a drive letter or
                // UNC volume
                throw new LocalPathFormatException(
                    MessageFormat.format(
                        Messages.getString("LocalPath.InvalidPathFormat"), //$NON-NLS-1$
                        item));
            }

            if (!allow8Dot3Paths) {
                check8Dot3Aliases(item);
            }
            if (checkReservedCharacters) {
                final AtomicReference<Character> c = new AtomicReference<Character>();
                if (LocalPath.hasVersionControlReservedCharacter(item, c)) {
                    throw new LocalPathFormatException(
                        MessageFormat.format(
                            Messages.getString("LocalPath.InvalidPathInvalidCharFormat"), //$NON-NLS-1$
                            item,
                            c.get().charValue()));
                }
            }
        }

        return ItemValidationError.NONE;
    }

    /**
     * Returns true if the path contains any character which is a version
     * control reserved character.
     *
     * @param path
     *        the string to check for invalid characters (must not be
     *        <code>null</code>)
     * @param c
     *        a reference to a {@link Character} where the invalid detected
     *        character will be stored if the method returns <code>true</code>
     *        (no character is stored if the method returns <code>false</code> )
     *        (may be <code>null</code>)
     * @return true if the path contains any version control reserved
     *         characters, false if it does not
     */
    public static boolean hasVersionControlReservedCharacter(final String path, final AtomicReference<Character> c) {
        for (int i = 0; i < VERSION_CONTROL_SEPARATORS.length; i++) {
            if (path.indexOf(VERSION_CONTROL_SEPARATORS[i]) != -1) {
                if (c != null) {
                    c.set(VERSION_CONTROL_SEPARATORS[i]);
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the given item contains any 8.3 aliases and throws if it
     * does.
     */
    public static void check8Dot3Aliases(final String item) {
        final Matcher matcher = EIGHT_DOT_THREE_CHECKER.matcher(item);

        if (matcher.find()) {
            /*
             * Group 0 is entire string; group 1 is base name; group 2
             * (optional) is optional extension; group 3 (optional) is
             * terminating slash or EOL.
             */
            final String baseNameGroup = matcher.group(1);

            if (baseNameGroup != null && baseNameGroup.length() <= 8) {
                throw new LocalPathFormatException(
                    MessageFormat.format(
                        Messages.getString("LocalPath.Invalid8Dot3PathFormat"), //$NON-NLS-1$
                        item));
            }
        }
    }

    /**
     * Tests whether the given path starts with a drive root prefix. A path is
     * considered rooted if it starts with a backslash ("\"), slash ("/
     * "), or a drive letter and a colon (":"). The Java File roots list is not
     * consulted.
     *
     * @param path
     *        the local path to test (may be <code>null</code>)
     * @return true if the path is rooted, false otherwise (including if the
     *         path was <code>null</code> or empty)
     */
    public static boolean isPathRooted(final String path) {
        /*
         * This is similar to the .NET framework implementation of
         * Path.IsPathRooted.
         */
        if (path != null) {
            final int length = path.length();

            if ((length >= 1 && (path.charAt(0) == '\\' || path.charAt(0) == '/'))
                || (length >= 2 && path.charAt(1) == ':')) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the root portion of the given path. The resulting string consists
     * of those rightmost characters of the path that constitute the root of the
     * path. Possible patterns for the resulting string are: An empty string (a
     * relative path on the current drive), "/" an absolute Unix path, "\" (an
     * absolute path on the current drive), "X:" (a relative path on a given
     * drive, where X is the drive letter),
     * "X:\" (an absolute path on a given drive), and "\\server\share" (a UNC
     * path for a given server and share name). The resulting string is
     * <code>null</code> if path is <code>null</code>.
     *
     * @param path
     *        the path to get the root for (may be <code>null</code>)
     * @return the root portion of the path (<code>null</code> if a
     *         <code>null</code> path was given)
     */
    public static String getPathRoot(final String path) {
        return getPathRoot(path, Platform.getCurrentPlatform());
    }

    protected static String getPathRoot(final String path, final Platform platform) {
        /*
         * This is similar to the .NET framework implementation of
         * Path.GetPathRoot(String).
         */

        if (path == null) {
            return null;
        }

        /*
         * .NET sanitizes the path before measuring the root. Maybe we should do
         * that, but this.canonicalize() would be wrong because it makes "a"
         * into "/current/working/directory/a".
         */

        return path.substring(0, getRootLength(path, platform));
    }

    /**
     * @see #getRootLength(String, Platform)
     */
    protected static int getRootLength(final String path) {
        return getRootLength(path, Platform.getCurrentPlatform());
    }

    /**
     * Gets the length of the root DirectoryInfo or whatever DirectoryInfo
     * markers are specified for the first part of the DirectoryInfo name.
     *
     * From the .NET Path class implementation, but with NTFS validity
     * enforcements on all platforms because we might send the chars to TFS
     * later.
     *
     * @param path
     *        the local path to get the root length for (must not be
     *        <code>null</code>)
     * @param platform
     *        the {@link Platform} whose path conventions should be used (must
     *        not be <code>null</code>)
     */
    protected static int getRootLength(final String path, final Platform platform) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(platform, "platform"); //$NON-NLS-1$

        /*
         * This is similar to the .NET framework implementation of
         * Path.GetRootLength(String).
         */

        /*
         * Always use NTFS rules so TFS can accept these paths later if we map
         * them.
         */
        if (FileHelpers.isValidNTFSPath(path, false) == false) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("LocalPath.PathContainsInvalidNTFSCharactersFormat"), //$NON-NLS-1$
                    path));
        }

        int i = 0;
        final int length = path.length();

        /*
         * Use the .NET framework per-platform implementations.
         */
        if (platform.contains(Platform.WINDOWS)) {
            if (length >= 1 && (path.charAt(0) == '\\' || path.charAt(0) == '/')) {
                // handles UNC names and directories off current drive's root.
                i = 1;
                if (length >= 2 && (path.charAt(1) == '\\' || path.charAt(1) == '/')) {
                    i = 2;
                    int n = 2;
                    while (i < length && ((path.charAt(i) != '\\' && path.charAt(i) != '/') || --n > 0)) {
                        i++;
                    }
                }
            } else if (length >= 2 && path.charAt(1) == ':') {
                // handles A:\bar.
                i = 2;
                if (length >= 3 && (path.charAt(2) == '\\' || path.charAt(2) == '/')) {
                    i++;
                }
            }
            return i;
        } else {
            /*
             * Assuming generic Unix here.
             */

            if (length >= 1 && path.charAt(0) == '/') {
                i = 1;
            }
            return i;
        }
    }

    /**
     * This method throws an InvalidPathException if there is a dollar sign
     * ('$') that follows a path separator ('/' or '\') since no part of a path
     * is allowed to start with a dollar sign.
     *
     * @param path
     *        the path to check (path must already be canonicalized)
     */
    public static void checkForIllegalDollarInPath(final String path) {
        ItemPath.checkForIllegalDollarInPath(path);
    }

    /**
     * Given two canonicalized paths (that never end with a filesystem separator
     * except for a drive letter root on Windows), returns the longest path
     * prefix which is common to both paths.
     * <p>
     * <ul>
     * <li>The computed common prefix will end with a directory separator if
     * both input strings contained separators at that index.</li>
     * <li>The computed common prefix will not end with a directory separator if
     * one or both of the input strings did not contain a separator at the
     * position that follows the common prefix text.</li>
     * <li>On Windows, which has drive letters, there will always be a non-
     * <code>null</code> prefix for files that reside on the same drive: the
     * drive letter and a trailing backslash and possibly more directory parts.
     * </li>
     * <li>On Unix, two files at the root directory ("/") have no directory name
     * parts preceding them (the slash is not properly a directory name), but it
     * would be confusing to return <code>null</code> in this case since these
     * paths do have a common parent, so "/" is returned.
     * </ul>
     *
     * @param path1
     *        the first path (must not be <code>null</code> or empty)
     * @param path2
     *        the second path (must not be <code>null</code> or empty)
     * @return longest path prefix common to both paths. <code>null</code> if
     *         the paths have nothing in common
     */
    public static String getCommonPathPrefix(final String path1, final String path2) {
        Check.notNullOrEmpty(path1, "path1"); //$NON-NLS-1$
        Check.notNullOrEmpty(path2, "path2"); //$NON-NLS-1$

        final boolean atLeastOneUncPath = Platform.isCurrentPlatform(Platform.WINDOWS) && path1.startsWith("\\\\"); //$NON-NLS-1$
        int commonLength = 0, lastSeparatorIndex = 0, separatorCount = 0;

        while (commonLength < path1.length() && commonLength < path2.length()) {
            if (Character.toUpperCase(path1.charAt(commonLength)) != Character.toUpperCase(
                path2.charAt(commonLength))) {
                break;
            }

            if (path1.charAt(commonLength) == File.separatorChar) {
                lastSeparatorIndex = commonLength;
                separatorCount++;
            }

            commonLength++;
        }

        if (0 == commonLength || (atLeastOneUncPath && separatorCount < 3)) {
            return null;
        }

        /*
         * Handle the case on Unix where the files only have the root drive in
         * common.
         */
        if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX) && commonLength == 1) {
            Check.isTrue(path1.charAt(0) == File.separatorChar, "Path not canonicalized (must start with / on Unix)"); //$NON-NLS-1$
            return File.separator;
        }

        final boolean path1EndsOnPathPart =
            commonLength == path1.length() || path1.charAt(commonLength) == File.separatorChar;

        final boolean path2EndsOnPathPart =
            commonLength == path2.length() || path2.charAt(commonLength) == File.separatorChar;

        if (!path1EndsOnPathPart || !path2EndsOnPathPart) {
            // Back up one path part
            commonLength = lastSeparatorIndex;
            separatorCount--;
        }

        if (0 == commonLength || (atLeastOneUncPath && separatorCount < 3)) {
            return null;
        }

        String toReturn = path1.substring(0, commonLength);

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            if (2 == toReturn.length() && toReturn.charAt(1) == ':') {
                // Drive roots need to have a \ on the end
                toReturn += File.separator;
            }
        }

        return toReturn;
    }

    /**
     * Helper method to check whether a file/dir exists
     *
     * @param filePath
     * @return
     */
    public static boolean exists(final String filePath) {
        if (StringUtil.isNullOrEmpty(filePath)) {
            return false;
        }

        return new File(filePath).exists();
    }
}
