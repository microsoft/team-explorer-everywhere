// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.LocalPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Contains static utility methods that apply to both LocalPath and ServerPath
 * strings.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public abstract class ItemPath {
    /**
     * Canonicalizes a local or server path. A path should be canonicalized
     * before being converted to a TFS path via tfsToNative().
     *
     * @param localOrServerPath
     *        the local or server path to canonicalize.
     * @return the canonical version of the input string, null if the input
     *         string was null or an empty string if the niput string was empty.
     */
    public static String canonicalize(final String localOrServerPath) {
        if (localOrServerPath == null) {
            return null;
        }
        if (localOrServerPath.length() == 0) {
            return localOrServerPath;
        }

        if (ServerPath.isServerPath(localOrServerPath)) {
            try {
                return ServerPath.canonicalize(localOrServerPath);
            } catch (final ServerPathFormatException e) {
                return localOrServerPath;
                /*
                 * This shouldn't happen often because isServerPath will return
                 * false in most cases.
                 */
            }
        } else {
            return LocalPath.canonicalize(localOrServerPath);
        }
    }

    /**
     * Validates the item as a server or local path.
     */
    public static ItemValidationError checkItem(
        final AtomicReference<String> item,
        final String parameterName,
        final boolean allowNull,
        final boolean allowWildcards,
        final boolean allow8Dot3Paths,
        final boolean checkReservedCharacters) {
        return checkItem(
            item,
            parameterName,
            allowNull,
            allowWildcards,
            allow8Dot3Paths,
            checkReservedCharacters,
            VersionControlConstants.MAX_SERVER_PATH_SIZE_OLD);
    }

    /**
     * Validates the item as a server or local path.
     */
    public static ItemValidationError checkItem(
        final AtomicReference<String> item,
        final String parameterName,
        final boolean allowNull,
        final boolean allowWildcards,
        final boolean allow8Dot3Paths,
        final boolean checkReservedCharacters,
        final int maxServerPathLength) {
        if (item != null && ServerPath.isServerPath(item.get())) {
            return ServerPath.checkServerItem(
                item,
                parameterName,
                allowNull,
                allowWildcards,
                allow8Dot3Paths,
                checkReservedCharacters,
                maxServerPathLength);
        } else {
            return LocalPath.checkLocalItem(
                item.get(),
                parameterName,
                allowNull,
                allowWildcards,
                allow8Dot3Paths,
                checkReservedCharacters);
        }
    }

    /**
     * Accepts a local path or server path for conversion to TFS path
     * conventions. Since only local paths need converted (server paths are
     * always the same convention), this method detects whether the path passed
     * in is local or server. If the path is local, it is converted. If the path
     * is server, it is returned unaltered.
     *
     * @param localOrServerPath
     *        the local or server path to convert to TFS path format.
     * @return the converted string.
     */
    public static String smartNativeToTFS(final String localOrServerPath) {
        if (ServerPath.isServerPath(localOrServerPath) == false) {
            return LocalPath.nativeToTFS(localOrServerPath);
        } else {
            return localOrServerPath;
        }
    }

    /**
     * Accepts a local path or server path for conversion from TFS path
     * conventions. Since only local paths need converted (server paths are
     * always the same convention), this method detects whether the path passed
     * in is local or server. If the path is local, it is converted. If the path
     * is server, it is returned unaltered.
     *
     * @param localOrServerPath
     *        the local or server path to convert from TFS path format to native
     *        path format.
     * @return the converted string.
     */
    public static String smartTFSToNative(final String localOrServerPath) {
        if (ServerPath.isServerPath(localOrServerPath) == false) {
            return LocalPath.tfsToNative(localOrServerPath);
        } else {
            return localOrServerPath;
        }
    }

    /**
     *
     *
     *
     * @param localOrServerPath
     * @param index
     * @param count
     * @return
     */
    public static boolean isWildcard(final String localOrServerPath, final int index, final int count) {
        return isWildcard(localOrServerPath.substring(index, index + count));
    }

    /**
     * Tests whether the given local or server path contains wildcard characters
     * in its final path element. Wildcards in initial path elements
     * (intermediate directories) are ignored.
     *
     * @param localOrServerPath
     *        the local or server path to test for wildcards (in last element
     *        only). If <code>null</code> or empty, result is always false.
     * @return true if the last path element contains wildcards, false
     *         otherwise.
     */
    public static boolean isWildcard(final String localOrServerPath) {
        if (localOrServerPath == null || localOrServerPath.length() == 0) {
            return false;
        }

        if (ServerPath.isServerPath(localOrServerPath)) {
            return ServerPath.isWildcard(localOrServerPath);
        } else {
            return LocalPath.isWildcard(localOrServerPath);
        }
    }

    /**
     * Compare a string against a wildcard and return true if it matches, false
     * if not. This does a case insensitive compare with the Ordinal culture.
     *
     * @param fileName
     *        String to match against the pattern.
     * @param wildcardPattern
     *        Pattern (using * and ? for wildcards) to match against
     * @return true if the wildcard matches, false if it does not
     */
    public static boolean matchesWildcardFile(final String fileName, final String wildcardPattern) {
        return matchesWildcardFile(fileName, 0, wildcardPattern, 0);
    }

    /**
     * Tests a file name (not full path) against a wildcard pattern for a match.
     * This is useful for the file part of local and server paths.
     *
     * Character case is ignored during wildcard matching.
     *
     * @param fileName
     *        the file name (not a full path) to test against the wildcard
     *        pattern (must not be <code>null</code>)
     * @param wildcardPattern
     *        the wildcard pattern to use (must not be <code>null</code>)
     * @return true if the wildcard pattern matches the given file name, false
     *         if it does not.
     */
    public static boolean matchesWildcardFile(
        final String fileName,
        int fileIndex,
        final String wildcardPattern,
        int wildcardIndex) {
        // This method is called in tight loops, and recursively; don't check
        // the arguments for nulls

        while (wildcardIndex < wildcardPattern.length()) {
            if (wildcardPattern.charAt(wildcardIndex) == '*') {
                /*
                 * We've hit a wild card. Skip any sequence of multiple wildcard
                 * characters.
                 */
                do {
                    ++wildcardIndex;
                } while (wildcardIndex < wildcardPattern.length() && wildcardPattern.charAt(wildcardIndex) == '*');

                /*
                 * Read characters until we can find a match.
                 */
                while (true) {
                    /*
                     * Do we immediately match the rest of the string? Call
                     * ourselves recursively to decide.
                     */
                    if (ItemPath.matchesWildcardFile(fileName, fileIndex, wildcardPattern, wildcardIndex)) {
                        return true;
                    }

                    // If we're done, we don't have any match.
                    if (fileIndex == fileName.length()) {
                        return false;
                    }

                    /*
                     * Match Visual Studio's implementation, which does not
                     * require a trailing period to match at the end.
                     */
                    if (fileName.charAt(fileIndex) == '.'
                        && fileName.lastIndexOf('.', fileName.length() - 1) == fileIndex) {
                        if (wildcardPattern.length() == wildcardIndex + 1
                            && wildcardPattern.charAt(wildcardIndex) == '.') {
                            return false;
                        }
                    }

                    // Step to the next character to find a match.
                    ++fileIndex;
                }
            }

            // If we're at the end of the string, we can decide immediately.
            if (fileIndex == fileName.length()) {
                /*
                 * Match Visual Studio's implementation, which does not require
                 * a trailing period to match at the end.
                 */
                if (wildcardPattern.charAt(wildcardIndex) == '.'
                    && wildcardPattern.lastIndexOf('.', wildcardPattern.length() - 1) == wildcardIndex
                    && ItemPath.matchesWildcardFile(fileName, fileIndex, wildcardPattern, wildcardIndex + 1)) {
                    return true;
                }

                // No match.
                return false;
            }

            /*
             * If we're at an index in both strings where the characters aren't
             * the same, we don't have a match (unless a question mark is in the
             * pattern).
             */

            if (!fileName.regionMatches(true, fileIndex, wildcardPattern, wildcardIndex, 1)
                && wildcardPattern.charAt(wildcardIndex) != '?') {
                return false;
            }

            // Go to the next character in each string.
            ++fileIndex;
            ++wildcardIndex;
        }

        /*
         * We've walked the whole pattern. If we've also walked the whole input
         * file name, or we only have a trailing period, we have a match.
         */
        return fileIndex == fileName.length()
            || fileName.charAt(fileIndex) == '.' && fileIndex + 1 == fileName.length();
    }

    /**
     * This method throws an {@link InputValidationException} if there is a
     * dollar sign ('$') that follows a path separator since no part of a path
     * is allowed to start with a dollar sign.
     *
     * @param serverOrLocalPath
     *        the path to check (path must already be canonicalized) (may be
     *        <code>null</code>)
     */
    public static void checkForIllegalDollarInPath(final String serverOrLocalPath) throws InputValidationException {
        if (serverOrLocalPath == null) {
            return;
        }

        final int length = serverOrLocalPath.length();

        // Dollar signs are not allowed at the beginning of any part of the
        // path.
        for (int i = 1; i < length; i++) {
            /*
             * We can test with just ServerPath.isSeparator because TFS permits
             * both backslashes and forward slashes in server paths, and that
             * also covers local paths on Unix and Windows.
             */
            if (serverOrLocalPath.charAt(i) == '$' && ServerPath.isSeparator(serverOrLocalPath.charAt(i - 1))) {
                final String message = MessageFormat.format(
                    Messages.getString("ItemPath.InvalidPathDollarSignFormat"), //$NON-NLS-1$
                    serverOrLocalPath);

                if (ServerPath.isServerPath(serverOrLocalPath)) {
                    throw new ServerPathFormatException(message);
                } else {
                    throw new LocalPathFormatException(message);
                }
            }
        }
    }

    public static boolean equals(final String serverOrLocalPath1, final String serverOrLocalPath2) {
        if (ServerPath.isServerPath(serverOrLocalPath1) && ServerPath.isServerPath(serverOrLocalPath2)) {
            return ServerPath.equals(serverOrLocalPath1, serverOrLocalPath2);
        }
        return LocalPath.equals(serverOrLocalPath1, serverOrLocalPath2);
    }

    public static int hashcode(final String serverOrLocalPath) {
        if (ServerPath.isServerPath(serverOrLocalPath)) {
            return ServerPath.hashCode(serverOrLocalPath);
        }
        return LocalPath.hashCode(serverOrLocalPath);
    }
}
