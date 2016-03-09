// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.path;

import java.io.File;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

/**
 * TFS version control repository path utility functions.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public abstract class ServerPath {
    /**
     * Occasionally code must format a path using the root without the slash.
     */
    public static final String ROOT_NAME_ONLY = "$"; //$NON-NLS-1$

    /**
     * All server paths begin with this string. The slash is important (the
     * server seems to always reject access to "$").
     */
    public static final String ROOT = "$/"; //$NON-NLS-1$

    /**
     * Allowed path separator characters in repository paths. All characters are
     * equivalent. Forward slash ('/') is the preferred character.
     */
    public static final char[] SEPARATOR_CHARACTERS = {
        '/',
        '\\'
    };

    /**
     * The preferred separator character.
     */
    public static final char PREFERRED_SEPARATOR_CHARACTER = '/';

    /**
     * Longest allowable TFS directory component part
     */
    public static final int MAXIMUM_COMPONENT_LENGTH = VersionControlConstants.MAX_SERVER_PATH_COMPONENT_SIZE;

    /**
     * Compares well-formed server path strings in a top-down fashion (parents
     * sort before their children).
     */
    public static final Comparator<String> TOP_DOWN_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String path1, final String path2) {
            return ServerPath.compareTopDown(path1, path2);
        };
    };

    /**
     * Compares well-formed server path strings in a bottom-up fashion (children
     * sort before their parents).
     */
    public static final Comparator<String> BOTTOM_UP_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(final String path1, final String path2) {
            return ServerPath.compareBottomUp(path1, path2);
        };
    };

    /**
     * Compares two repository paths. Case is ignored. null values are not
     * permitted.
     *
     * @param path1
     *        the first repository path (must not be <code>null</code>)
     * @param path2
     *        the second repository path (must not be <code>null</code>)
     * @return true if the paths are equal, false if not.
     */
    public final static boolean equals(final String path1, final String path2) {
        return equals(path1, path2, true);
    }

    /**
     * Computes a hashCode from the specified server path. NOTE: This version of
     * hashCode only pairs with the equals method that takes two strings and
     * should not be used in conjunction with it.
     *
     * @param path
     *        the server path to compute a hash code for.
     * @return a hash code
     */
    public final static int hashCode(final String path) {
        return LocaleInvariantStringHelpers.caseInsensitiveHashCode(path);
    }

    /**
     * Compares two repository paths, optionally ignoring case. null values are
     * not permitted.
     *
     * @param path1
     *        the first repository path (must not be <code>null</code>)
     * @param path2
     *        the second repository path (must not be <code>null</code>)
     * @param ignoreCase
     *        true if we should ignore case
     * @return true if the paths are equal, false if not.
     */
    public final static boolean equals(final String path1, final String path2, final boolean ignoreCase) {
        if (path1 == path2) {
            return true;
        }

        /*
         * Visual Studio uses String.Equals(StringComparison.OrdinalIgnoreCase)
         * on the strings with no normalization.
         *
         * Java's Server.equalsIgnoreCase() provides a similar invariant
         * (non-Locale-sensitive), case-insensitive test.
         */
        if (ignoreCase) {
            return path1.equalsIgnoreCase(path2);
        } else {
            return path1.equals(path2);
        }
    }

    public static ItemValidationError checkServerItem(
        final AtomicReference<String> item,
        final String parameterName,
        final boolean allowNull,
        final boolean allowWildcards,
        final boolean allow8Dot3Paths,
        final boolean checkReservedCharacters) {
        // use old server path length for backward compatibility
        return checkServerItem(
            item,
            parameterName,
            allowNull,
            allowWildcards,
            allow8Dot3Paths,
            checkReservedCharacters,
            VersionControlConstants.MAX_SERVER_PATH_SIZE_OLD);
    }

    /**
     *
     *
     *
     * @param item
     * @param parameterName
     * @param allowNull
     * @param allowWildcards
     * @param allow8Dot3Paths
     * @param checkReservedCharacters
     * @return
     */
    public static ItemValidationError checkServerItem(
        final AtomicReference<String> item,
        final String parameterName,
        final boolean allowNull,
        final boolean allowWildcards,
        final boolean allow8Dot3Paths,
        final boolean checkReservedCharacters,
        final int maxServerPathLength) {
        final String inItem = item.get();
        if (inItem == null || inItem.length() == 0) {
            if (!allowNull) {
                throw new IllegalArgumentException(parameterName);
            }
        } else if (!allowWildcards && ServerPath.isWildcard(inItem)) {
            return ItemValidationError.WILDCARD_NOT_ALLOWED;
        } else if (inItem.length() > maxServerPathLength) {
            return ItemValidationError.REPOSITORY_PATH_TOO_LONG;
        } else {
            item.set(ServerPath.canonicalize(inItem));

            if (!allow8Dot3Paths) {
                LocalPath.check8Dot3Aliases(inItem);
            }
        }

        return ItemValidationError.NONE;
    }

    /**
     * @param path
     *        the path to test (may be <code>null</code>)
     * @return true if the given path is equal to {@link #ROOT},
     *         <code>false</code> if it is not
     */
    public final static boolean isRootFolder(final String path) {
        return ROOT.equals(path);
    }

    /**
     * <p>
     * Tests the given paths for a parent-child relationship. A path is a child
     * of another if the object it describes would reside below the object
     * described by the parent path in the TFS repository. Case is ignored.
     * </p>
     * <p>
     * A possible child that is equivalent to the parent path (both refer to the
     * same object) is considered a child. This is compatible with Visual
     * Studio's implementation.
     * </p>
     *
     * @param parentPath
     *        the server path to the parent item (must not be <code>null</code>)
     * @param possibleChild
     *        the server path of the possible child item (must not be
     *        <code>null</code>)
     * @return true if possibleChild is a child of parentPath.
     */
    public final static boolean isChild(String parentPath, String possibleChild) throws ServerPathFormatException {
        Check.notNull(parentPath, "parentPath"); //$NON-NLS-1$
        Check.notNull(possibleChild, "possibleChild"); //$NON-NLS-1$

        // Canonicalize the paths for easy comparison.
        parentPath = ServerPath.canonicalize(parentPath);
        possibleChild = ServerPath.canonicalize(possibleChild);

        // Ignoring case, if the parent matches all the way up to the length of
        // the child...
        if (parentPath.regionMatches(true, 0, possibleChild, 0, parentPath.length())) {
            // If the paths are the same length, they are equal, and therefore
            // one is a child (see method JDoc).
            if (parentPath.length() == possibleChild.length()) {
                return true;
            }

            // If the parent ends with a separator (then the child also has one
            // in the right place), so it's a match.
            if (ServerPath.isSeparator(parentPath.charAt(parentPath.length() - 1))) {
                return true;
            }

            // If the child has a separator right beyond where we just did the
            // compare,
            // then it is a child.
            if (ServerPath.isSeparator(possibleChild.charAt(parentPath.length()))) {
                return true;
            }
        }

        return false;
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
     *        pattern can only match if the recursive parameter is true. Not
     *        null.
     * @param secondItemWildcardPattern
     *        the wildcard pattern to apply to secondItemFolderPath. If null,
     *        wildcards are not evaluated and the first item path is tested
     *        whether it equals or is a child of the second item folder path
     *        (depending on the recursive value).
     * @param recursive
     *        if true, the wildcard pattern will apply to secondItemFolderPath
     *        and all its possible children. If false, the wildcard pattern will
     *        only match direct children of secondItemFolderPath.
     * @return true if the firstItemPath matches the wildcard specification,
     *         false if it does not.
     * @throws ServerPathFormatException
     *         if one of the supplied paths is not a valid server path.
     */
    public final static boolean matchesWildcard(
        String firstItemPath,
        String secondItemFolderPath,
        final String secondItemWildcardPattern,
        final boolean recursive) throws ServerPathFormatException {
        Check.notNull(firstItemPath, "firstItemPath"); //$NON-NLS-1$
        Check.notNull(secondItemFolderPath, "secondItemFolderPath"); //$NON-NLS-1$

        firstItemPath = ServerPath.canonicalize(firstItemPath);
        secondItemFolderPath = ServerPath.canonicalize(secondItemFolderPath);

        String firstItemFolder = null;
        String firstItemName = null;

        if (secondItemWildcardPattern == null || secondItemWildcardPattern.length() == 0) {
            firstItemFolder = firstItemPath;
        } else {
            firstItemFolder = ServerPath.getParent(firstItemPath);

            /*
             * If the folder part of the first item path is the same as the
             * input, there is no file.
             */
            if (ServerPath.equals(firstItemFolder, firstItemPath) == false) {
                firstItemName = ServerPath.getFileName(firstItemPath);
            }
        }

        /*
         * Test the folder part.
         *
         * If recursion is on and the first item folder isn't a child of the
         * second item folder path, there can be no match.
         */
        if (recursive) {
            if (ServerPath.isChild(secondItemFolderPath, firstItemFolder) == false) {
                return false;
            }
        } else {
            /*
             * Recursion is off, and they don't match exactly, so there can be
             * no match.
             */
            if (ServerPath.equals(firstItemFolder, secondItemFolderPath) == false) {
                return false;
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
     * Returns a new version of the given repository path that is fully rooted
     * and canonicalized. Use this function to sanitize user input or to expand
     * partial paths encountered in server data. Note: strips trailing slashes
     * from directories, except $/.
     *
     * @param serverPath
     *        the repository path string to clean up.
     * @return the cleaned up path.
     * @throws ServerPathFormatException
     *         when the path cannot be cleaned up.
     */
    public final static String canonicalize(String serverPath) throws ServerPathFormatException {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        int serverPathLength = serverPath.length();

        // empty string is not valid
        if (serverPathLength == 0) {
            throw new ServerPathFormatException(Messages.getString("ServerPath.AServerPathCannotBeAnEmptyString")); //$NON-NLS-1$
        }

        /*
         * Do a quicker check up front to see if the path is OK. If it's not,
         * we'll try to fix it up and/or throw the appropriate exception.
         *
         * This is a huge win, since almost all of the server paths we ever
         * encounter in the program (and especially at large scale, like
         * translating hundreds of thousands of server paths to local paths) are
         * already canonicalized.
         */
        if (isCanonicalizedPath(serverPath, true)) {
            return serverPath;
        }

        final List<String> newComponents = new ArrayList<String>();
        final StringBuilder currentComponent = new StringBuilder(serverPathLength);
        int position = 0;

        // A simple conversion for $ to $/
        if (serverPath.equals(ServerPath.ROOT_NAME_ONLY)) {
            serverPath = ServerPath.ROOT;
            serverPathLength = 2;
        }

        // prepend a $ if necessary (next check will take care of the following
        // /)
        if (serverPath.charAt(0) == '$') {
            position++;
        }

        newComponents.add("$"); //$NON-NLS-1$

        // the path must begin with one of: /, $/, \, $\
        if (position >= serverPathLength || ServerPath.isSeparator(serverPath.charAt(position)) == false) {
            throw new ServerPathFormatException(
                MessageFormat.format(Messages.getString("ServerPath.AServerPathMustBeAbsoluteFormat"), serverPath)); //$NON-NLS-1$
        }

        boolean illegalDollarInPath = false;

        // walk the rest of the given path
        for (; position <= serverPathLength; position++) {
            // end of the string or directory separators, append the current
            // component to the list
            if (position == serverPathLength || ServerPath.isSeparator(serverPath.charAt(position))) {
                // squash multiple concurrent separators
                if (currentComponent.length() == 0) {
                    // Ignore
                }
                // single dot components are thrown away
                else if (currentComponent.toString().equals(".")) //$NON-NLS-1$
                {
                    // Ignore
                }
                // double dot components mean to pop the last off the stack
                else if (currentComponent.toString().equals("..")) //$NON-NLS-1$
                {
                    if (newComponents.size() <= 1) {
                        throw new ServerPathFormatException(
                            MessageFormat.format(
                                Messages.getString("ServerPath.ServerPathRefersToInvalidPathOutsideFolderFormat"), //$NON-NLS-1$
                                serverPath,
                                ServerPath.ROOT));
                    }

                    newComponents.remove(newComponents.size() - 1);
                }
                // otherwise, pop this bad boy on the directory stack
                else {
                    final String cleaned = ServerPath.cleanupComponent(currentComponent).toString();

                    /*
                     * Match Visual Studio behavior of ignoring directories that
                     * contain nothing but spaces and dots.
                     */
                    if (cleaned.length() == 0) {
                        // Ignore
                    } else if (cleaned.length() > MAXIMUM_COMPONENT_LENGTH) {
                        throw new ServerPathFormatException(MessageFormat.format(
                            //@formatter:off
                            Messages.getString("ServerPath.ServerPathComponentIsLongerthanTheMaximumCharactersFormat"), //$NON-NLS-1$
                            //@formatter:on
                            cleaned,
                            MAXIMUM_COMPONENT_LENGTH));
                    } else if (FileHelpers.isReservedName(cleaned)) {
                        throw new ServerPathFormatException(
                            MessageFormat.format(
                                Messages.getString("ServerPath.ServerPathContainsAnInvalidDirectoryComponentFormat"), //$NON-NLS-1$
                                serverPath,
                                cleaned));
                    } else {
                        if (cleaned.charAt(0) == '$') {
                            illegalDollarInPath = true;
                        }

                        // Good component, just add.
                        newComponents.add(cleaned);
                    }
                }

                currentComponent.setLength(0);
            }
            // a non-directory separator, non-dot, but valid in NTFS filenames
            else if (ServerPath.isValidPathCharacter(serverPath.charAt(position)) == true) {
                currentComponent.append(serverPath.charAt(position));
            }
            // invalid character
            else {
                throw new ServerPathFormatException(
                    MessageFormat.format(
                        Messages.getString("ServerPath.TheCharacterIsNotPermittedInServerPathsFormat"), //$NON-NLS-1$
                        serverPath.charAt(position)));
            }
        }

        if (newComponents.size() == 1) {
            return ServerPath.ROOT;
        }

        // join components with a slash
        final StringBuilder newPath = new StringBuilder();
        for (int i = 0; i < newComponents.size(); i++) {
            if (i > 0) {
                newPath.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
            }

            newPath.append(newComponents.get(i));
        }

        /*
         * We were checking for illegal dollar in the path during the loop
         * through the string. Throw the same exception as checkForIllegalDollar
         * would, if the flag was raised stating we had an illegal dollar
         * somewhere.
         */
        if (illegalDollarInPath) {
            throw new ServerPathFormatException(
                MessageFormat.format(
                    Messages.getString("ItemPath.InvalidPathDollarSignFormat"), //$NON-NLS-1$
                    newPath.toString()));
        }

        return newPath.toString();
    }

    /**
     * Returns true if the path is canonicalized. The path must not contain a $
     * at the beginning of a path part, or any illegal characters.
     */
    public static boolean isCanonicalizedPath(final String serverItem, final boolean allowSemicolon) {
        if (serverItem.length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
            return false;
        }

        // The path is not legal if it does not start with $/.
        if (!serverItem.startsWith(ROOT)) {
            return false;
        }

        // If the path is $/, it is legal.
        if (2 == serverItem.length()) {
            return true;
        }

        // The path is not legal if it ends with a separator character.
        if (serverItem.length() > 2 && serverItem.charAt(serverItem.length() - 1) == PREFERRED_SEPARATOR_CHARACTER) {
            return false;
        }

        int pathPartLength = 0;

        for (int i = 2; i < serverItem.length(); i++) {
            final char c = serverItem.charAt(i);

            if (c == PREFERRED_SEPARATOR_CHARACTER) {
                if (!isCanonicalizedPathPart(serverItem, i, pathPartLength)) {
                    return false;
                }

                pathPartLength = 0;
                continue;
            }

            // The $ character is not permitted to lead a path part.
            if (0 == pathPartLength && c == ROOT.charAt(0)) {
                return false;
            }

            // Look up each character in the NTFS valid characters truth table.
            if (!FileHelpers.isValidNTFSFileNameCharacter(c)) {
                return false;
            }

            // The semicolon character is not legal anywhere in a version
            // control path.
            if (!allowSemicolon && c == ';') {
                return false;
            }

            // Wildcard characters are not legal in a version control path.
            if (c == '*' || c == '?') {
                return false;
            }

            pathPartLength++;
        }

        // Check the last path part.
        if (!isCanonicalizedPathPart(serverItem, serverItem.length(), pathPartLength)) {
            return false;
        }

        return true;
    }

    private static boolean isCanonicalizedPathPart(final String serverItem, final int i, final int pathPartLength) {
        // It's not legal to have two separators next to each other.
        if (0 == pathPartLength) {
            return false;
        } else if (2 == pathPartLength) {
            // It's not legal to have a path part which is just '..'
            if (serverItem.charAt(i - 1) == '.' && serverItem.charAt(i - 2) == '.') {
                return false;
            }
        } else if (3 == pathPartLength || 4 == pathPartLength) {
            // All the reserved names are of length 3 or 4 (NUL, COM1, etc.)
            if (FileHelpers.isReservedName(serverItem.substring(i - pathPartLength, i))) {
                return false;
            }
        } else if (pathPartLength > MAXIMUM_COMPONENT_LENGTH) {
            return false;
        }

        if (serverItem.charAt(i - 1) == '.' || Character.isWhitespace(serverItem.charAt(i - 1))) {
            // It is not legal to end a path part with whitespace or a dot.
            return false;
        }

        return true;
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
     * @param serverPath
     *        the path to the server item to describe (must not be
     *        <code>null</code>)
     * @param relativeTo
     *        the path that the first parameter will be described relative to
     *        (must not be <code>null</code>)
     * @return the relative path, or the unaltered given server path if it could
     *         not be made relative to the second path.
     */
    public static String makeRelative(final String serverPath, final String relativeTo) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(relativeTo, "relativeTo"); //$NON-NLS-1$

        /*
         * Use regionMatches() for a locale-invariant "starts with" test.
         */
        if (serverPath.regionMatches(true, 0, relativeTo, 0, relativeTo.length())) {
            /*
             * Compare lengths of canonical strings.
             */
            if (serverPath.length() == relativeTo.length()) {
                return ""; //$NON-NLS-1$
            }

            /*
             * If the relativeTo path ends in a separator, we have a relative
             * path to express.
             */
            if (relativeTo.length() > 0 && ServerPath.isSeparator(relativeTo.charAt(relativeTo.length() - 1))) {
                return serverPath.substring(relativeTo.length());
            }

            /*
             * If the given path's last character is a separator, then we also
             * have a relative path.
             */
            if (ServerPath.isSeparator(serverPath.charAt(relativeTo.length()))) {
                return serverPath.substring(relativeTo.length() + 1);
            }
        }

        return serverPath;
    }

    /**
     * <p>
     * Maps a server path to a local path, given a parent server path of the
     * path to be mapped, and a local path that corresponds to the parent.
     * </p>
     * <p>
     * Character case is ignored during string comparison, so strings with
     * mismatched-in-case common elements will still succeed in being made
     * relative.
     * </p>
     * <p>
     * Paths are not normalized (for ending separators, case, etc.). It is the
     * caller's responsibility to make sure the relativeToServerPath path can be
     * matched.
     * </p>
     *
     * @param serverPath
     *        the server path to convert to a local path (must not be
     *        <code>null</code>)
     * @param relativeToServerPath
     *        the parent server path (must not be <code>null</code> and must be
     *        a parent of <code>serverPath</code>)
     * @param localRoot
     *        the local path that corresponds to
     *        <code>relativeToServerPath</code> (must not be <code>null</code>)
     * @return the corresponding local path (never <code>null</code>)
     */
    public static String makeLocal(final String serverPath, final String relativeToServerPath, final String localRoot) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(relativeToServerPath, "relativeToServerPath"); //$NON-NLS-1$
        Check.notNull(localRoot, "localRoot"); //$NON-NLS-1$

        // ServerPath.canonicalize() checks for illegal dollar
        final String relativePart = ServerPath.makeRelative(ServerPath.canonicalize(serverPath), relativeToServerPath);

        /*
         * Convert any allowed separator characters into this platform's
         * preferred separator.
         */
        final StringBuilder relativeBuffer = new StringBuilder(relativePart);
        for (int j = 0; j < ServerPath.SEPARATOR_CHARACTERS.length; j++) {
            for (int k = 0; k < relativeBuffer.length(); k++) {
                if (relativeBuffer.charAt(k) == ServerPath.SEPARATOR_CHARACTERS[j]) {
                    relativeBuffer.setCharAt(k, File.separatorChar);
                }
            }
        }

        return LocalPath.combine(localRoot, relativeBuffer.toString());
    }

    /**
     * Strips trailing spaces and dots from a pathname, for canonicalization.
     *
     * @param sb
     *        {@link StringBuilder} to strip
     * @return Stripped {@link StringBuilder} (may be empty)
     */
    private final static StringBuilder cleanupComponent(final StringBuilder s) {
        while (s.length() > 0
            && (s.charAt(s.length() - 1) == '.' || Character.isWhitespace(s.charAt(s.length() - 1)))) {
            s.setLength(s.length() - 1);
        }

        return s;
    }

    /**
     * Tests whether the given character is valid in a repository path component
     * (file/folder name).
     *
     * @param c
     *        the character to test.
     * @return true if the character is allowed in a path component
     *         (file/folder), false if not.
     */
    public final static boolean isValidPathCharacter(final char c) {
        final char[] invalidCharacters = {
            '"',
            '/',
            ':',
            '<',
            '>',
            '\\',
            '|'
        };

        // All the control characters are not allowed.
        if (c <= 31) {
            return false;
        }

        for (int i = 0; i < invalidCharacters.length; i++) {
            if (invalidCharacters[i] == c) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether the given character is a valid repository path separator
     * character (as defined by {@link ServerPath#SEPARATOR_CHARACTERS}).
     *
     * @param c
     *        the character to test.
     * @return true if the character is a valid separator character, false
     *         otherwise.
     */
    public final static boolean isSeparator(final char c) {
        for (int i = 0; i < ServerPath.SEPARATOR_CHARACTERS.length; i++) {
            if (ServerPath.SEPARATOR_CHARACTERS[i] == c) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether the path supplied is a server path (not local path).
     *
     * @param path
     *        the path to test (must not be <code>null</code>)
     * @return true if the path describes a version control server path and not
     *         a local path or some other string.
     */
    public final static boolean isServerPath(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        // If the path doesn't have enough characters for $/, it
        // fails.
        if (path.length() < 2) {
            return false;
        }

        // If it does start with $/ or $\, it's a server item.
        return path.startsWith("$/") || path.startsWith("$\\"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Compares two server paths (ordinal character comparison) placing children
     * after their parents.
     * <p>
     * If sorting paths for display to the user, better to use a
     * {@link Collator}-based comparison instead of this one.
     *
     * @param pathA
     *        the first path to compare (must not be <code>null</code>)
     * @param pathB
     *        the second path to compare (must not be <code>null</code>)
     * @return see Object.compareTo().
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

        return pathA.compareToIgnoreCase(pathB);
    }

    /**
     * Compares two server paths (ordinal character comparison) placing parents
     * after their children.
     * <p>
     * If sorting paths for display to the user, better to use a
     * {@link Collator}-based comparison instead of this one.
     *
     * @param pathA
     *        the first path to compare (must not be <code>null</code>)
     * @param pathB
     *        the second path to compare (must not be <code>null</code>)
     * @return see Object.compareTo().
     */
    public final static int compareBottomUp(final String pathA, final String pathB) {
        return 0 - ServerPath.compareTopDown(pathA, pathB);
    }

    /**
     * Takes a repository path as a {@link StringBuilder} and parses out the top
     * node in the path and returns it as the parent. The remaining path is then
     * shortened to only include the rest of the path, minus the top node.
     *
     * @param path
     *        the path to find the top node in (must not be <code>null</code>)
     * @return the first node in the path that was supplied.
     */
    public final static String getFirstNode(final StringBuilder path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        int i = 0;

        for (i = 0; i < path.length(); i++) {
            if (ServerPath.isSeparator(path.charAt(i))) {
                break;
            }
        }

        String parent = null;

        if (i == path.length()) {
            parent = path.toString();
            path.delete(0, path.length()); // we're done
        } else if (i == path.length() - 1) {
            parent = path.substring(0, i);
            path.delete(0, path.length()); // we're done
        } else {
            parent = path.substring(0, i);
            path.delete(0, i + 1);
        }

        return parent;
    }

    /**
     * Tests whether the given server path contains wildcard characters in its
     * final path element. Wildcards in initial path elements are ignored.
     *
     * @param serverPath
     *        the server path to test for wildcards (in last element only). Not
     *        null.
     * @return true if the last path element contains wildcards, false
     *         otherwise.
     */
    public static boolean isWildcard(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        /*
         * Find the last occurance of any valid or character.
         */
        int largestIndex = -1;
        for (int i = 0; i < ServerPath.SEPARATOR_CHARACTERS.length; i++) {
            largestIndex = Math.max(largestIndex, serverPath.lastIndexOf(ServerPath.SEPARATOR_CHARACTERS[i]));
        }

        /*
         * If we found one at the last character, this path has an "empty" last
         * element, and thus denotes a directory that shouldn't be considered
         * with wildcards.
         */
        if (largestIndex == serverPath.length() - 1) {
            return false;
        }

        /*
         * Call the Wildcard class's method with the remainder of the string.
         */
        return Wildcard.isWildcard(serverPath.substring(largestIndex + 1));
    }

    /**
     * Gets just the folder part of the given server path, which is all of the
     * string up to the last component (the file part). If the given path
     * describes a folder but does not end in a separator, the last folder is
     * discarded.
     *
     * @param serverPath
     *        the server path of which to return the folder part (must not be
     *        <code>null</code>)
     * @return a server path with only the folder part of the given path, ending
     *         in a separator character.
     */
    public static String getParent(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        int largestIndex = -1;
        for (int i = 0; i < ServerPath.SEPARATOR_CHARACTERS.length; i++) {
            largestIndex = Math.max(largestIndex, serverPath.lastIndexOf(ServerPath.SEPARATOR_CHARACTERS[i]));
        }

        if (largestIndex != -1) {
            final String parent = serverPath.substring(0, largestIndex);

            /* Canonicalize root path to ensure trailing separator */
            if (ServerPath.equals(parent, ServerPath.ROOT_NAME_ONLY)) {
                return ServerPath.ROOT;
            }

            return parent;
        }

        return serverPath + ServerPath.SEPARATOR_CHARACTERS[0];
    }

    /**
     * Gets just the file part of the given server path, which is all of the
     * string after the last path component. If there are no separators, the
     * entire string is returned. If the string ends in a separator, an empty
     * string is returned.
     *
     * @param serverPath
     *        the server path from which to parse the file part (must not be
     *        <code>null</code>)
     * @return the file name at the end of the given server path, or the given
     *         path if no separator characters were found, or an empty string if
     *         the given path ends with a separator.
     */
    public static String getFileName(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        int largestIndex = -1;
        for (int i = 0; i < ServerPath.SEPARATOR_CHARACTERS.length; i++) {
            largestIndex = Math.max(largestIndex, serverPath.lastIndexOf(ServerPath.SEPARATOR_CHARACTERS[i]));
        }

        if (largestIndex == -1) {
            return serverPath;
        }

        /*
         * Add 1 to return the part after the sep, unless that would be longer
         * than the string ("$/a/b/" would be that case).
         */
        if (largestIndex + 1 < serverPath.length()) {
            return serverPath.substring(largestIndex + 1);
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * Tests whether one path is a direct child of another path (which would be
     * the parent).
     *
     * @param serverFolderPath
     *        the server path to the parent folder (must not be
     *        <code>null</code>)
     * @param serverPossibleChild
     *        the server path of the possible child item (must not be
     *        <code>null</code>)
     * @return true if the serverPossibleChild is a child of the
     *         serverFolderPath
     */
    public static boolean isDirectChild(final String serverFolderPath, final String serverPossibleChild) {
        Check.notNull(serverFolderPath, "serverFolderPath"); //$NON-NLS-1$
        Check.notNull(serverPossibleChild, "serverPossibleChild"); //$NON-NLS-1$

        return ServerPath.equals(serverFolderPath, ServerPath.getParent(serverPossibleChild));
    }

    /**
     * For a passed server path, calculate the project path. This is currently
     * just the first folder name with the "$/" to replicate the behaviour of
     * the MS Client.
     *
     * @param serverPath
     *        String path of a server item (for example
     *        "$/TeamProject/MyPath/MyItem.java").
     * @return String containing path to Team project that the serverPath
     *         resides in i.e. "$/TeamProject". If an error occured or Team
     *         Project was not part of the path, returns null.
     */
    public static String getTeamProject(String serverPath) {
        if (serverPath == null) {
            // If we were passed null then just return null.
            return null;
        }

        try {
            serverPath = ServerPath.canonicalize(serverPath);
        } catch (final ServerPathFormatException e) {
            return null;
        }

        if (serverPath.length() <= 2) {
            // If the server path is root then return it.
            return serverPath;
        }
        // Look for a seperator after the initial '$/'
        final int seperatorPos = serverPath.indexOf('/', 2);
        if (seperatorPos < 0) {
            // We didn't have one - just return what was passed to mimic
            // Microsoft behaviour
            return serverPath;
        }
        return serverPath.substring(0, seperatorPos);

    }

    /**
     * Perform getTeamProject for an array of serverPaths and return an array of
     * unique team projects that those serverPaths are in.
     *
     * @see ServerPath#getTeamProject(String)
     */
    public static String[] getTeamProjects(final String[] serverPaths) {
        final HashSet<String> projects = new HashSet<String>();

        if (serverPaths != null) {
            for (int i = 0; i < serverPaths.length; i++) {
                projects.add(ServerPath.getTeamProject(serverPaths[i]));
            }
        }

        return projects.toArray(new String[projects.size()]);
    }

    /**
     * Returns the name of the team project that the item resides in. Currently
     * this is just the first folder name of the item without the "$/".
     *
     * @param serverPath
     *        String path of a server item (for example
     *        "$/TeamProject/MyPath/MyItem.java").
     * @return String containing the name of the Team Project that the item
     *         resides in i.e. "TeamProject". Null if the given server path was
     *         null or no team project name was found in the path (i.e. in the
     *         case of the root path, "$/").
     */
    public static String getTeamProjectName(final String serverPath) {
        final String projectName = null;

        if (serverPath != null && serverPath.length() > 2) {
            final String fullProject = ServerPath.getTeamProject(serverPath);

            if (fullProject == null) {
                return null;
            }

            return fullProject.substring(2);
        }

        return projectName;
    }

    /**
     * Return whether the passed server item is a team project ($/proja,
     * $/projb, not $/proja/b nor $/). The item must be in the canonical server
     * path format for folders (no trailing slash except for the root folder).
     *
     * @param serverFolder
     *        a fully qualified server folder item (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the item is a team project (is an element
     *         directly under the server root folder), <code>false</code>
     *         otherwise
     */
    public static boolean isTeamProject(final String serverFolder) {
        Check.notNull(serverFolder, "item"); //$NON-NLS-1$

        return getFolderDepth(serverFolder, 2) == 1;
    }

    /**
     * Returns the depth of the item described by path, where the root folder is
     * depth 0, team projects are at depth 1, and so on.
     *
     * @param serverPath
     *        the server path to test (must not be <code>null</code>)
     * @return the depth from root, where root is 0, team projects are 1, etc.
     */
    public static int getFolderDepth(final String serverPath) {
        return ServerPath.getFolderDepth(serverPath, Integer.MAX_VALUE);
    }

    /**
     * Returns the depth of the item described by path, where the root folder is
     * depth 0, team projects are at depth 1, "$/Proj/a" is 2, and so on.
     *
     * @param serverPath
     *        the server path to test (must not be <code>null</code>)
     * @param maxDepth
     *        the maximum depth to search.
     * @return the depth from root, where root is 0, team projects are 1, etc.
     */
    public static int getFolderDepth(final String serverPath, final int maxDepth) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        int depth = 0;

        if (ServerPath.equals(serverPath, ServerPath.ROOT) == false) {
            for (int i = serverPath.indexOf("/"); i != -1 && maxDepth > depth; i = serverPath.indexOf("/", i + 1)) //$NON-NLS-1$ //$NON-NLS-2$
            {
                depth++;
            }
        }

        return depth;
    }

    /**
     * Combines the two given paths into one path string, using Team Foundation
     * Server's preferred path separator character. If path2 is an absolute
     * path, it is returned as the entire return value (path1 is discarded).
     *
     * @param parent
     *        the first (left-side) path component (must not be
     *        <code>null</code> or empty).
     * @param relative
     *        the second (right-side) path component (must not be
     *        <code>null</code>)
     */
    public static String combine(final String parent, final String relative) {
        Check.notNullOrEmpty(parent, "parent"); //$NON-NLS-1$
        Check.notNull(relative, "relative"); //$NON-NLS-1$

        String combinedString;

        if (relative.length() == 0 || relative.length() == 1 && relative.charAt(0) == '$') {
            /*
             * The relative part was empty or was the root, so use the parent as
             * the full combined string.
             */
            combinedString = parent;
        } else {
            if (ServerPath.isSeparator(relative.charAt(0))
                || relative.charAt(0) == '$' && relative.length() >= 2 && ServerPath.isSeparator(relative.charAt(1))) {
                /*
                 * The relative part starts with a separator or is an absolute
                 * path ("$/"), use it as the full combined string.
                 */
                combinedString = relative;
            } else {
                /*
                 * Combine the strings. If the parent doesn't end with a
                 * separator, we'll have to add one.
                 */
                String separator = ""; //$NON-NLS-1$
                if (parent.length() > 0
                    && parent.charAt(parent.length() - 1) != ServerPath.PREFERRED_SEPARATOR_CHARACTER) {
                    separator = "" + ServerPath.PREFERRED_SEPARATOR_CHARACTER; //$NON-NLS-1$
                }

                if (relative.charAt(0) == '$') {
                    /*
                     * This is the silly case where the relative string started
                     * with "$" but did not start with "$/" (would have matched
                     * the outer if). Just skip over the dollar sign and
                     * combine.
                     */
                    combinedString = parent + separator + relative.substring(1);
                } else {
                    combinedString = parent + separator + relative;
                }

            }
        }

        return ServerPath.canonicalize(combinedString);
    }

    /**
     * Splits the given server path into its components.
     *
     * @param serverPath
     *        The server path to split (must not be <code>null</code>)
     * @return An array of the components of the path
     */
    public static String[] split(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        final List<String> segments = new ArrayList<String>();
        final StringBuilder currentSegment = new StringBuilder();
        final int len = serverPath.length();

        for (int i = 0; i < len; i++) {
            final char c = serverPath.charAt(i);

            if (ServerPath.isSeparator(c)) {
                if (currentSegment.length() > 0) {
                    String s = currentSegment.toString();

                    if (ServerPath.ROOT_NAME_ONLY.equals(s)) {
                        s = ServerPath.ROOT;
                    }

                    segments.add(s);
                    currentSegment.setLength(0);
                }
            } else {
                currentSegment.append(c);
            }
        }

        if (currentSegment.length() > 0) {
            segments.add(currentSegment.toString());
        }

        return segments.toArray(new String[segments.size()]);
    }

    /**
     * Returns the hierarchy leading up to this path. Root path will be at index
     * 0, full path to team project will be at index 1, etc.
     *
     * @param serverPath
     *        The server path to obtain the hierarchy for (must not be
     *        <code>null</code>)
     * @return An array of strings representing the hierarchy leading up to this
     *         server path
     */
    public static String[] getHierarchy(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        final String[] segments = split(serverPath);
        final String[] hierarchy = new String[segments.length];

        for (int i = segments.length; i > 0; i--) {
            final StringBuilder currentPathBuffer = new StringBuilder();

            for (int j = 0; j < i; j++) {
                currentPathBuffer.append(segments[j]);
                if (j > 0 && j < i - 1) {
                    currentPathBuffer.append(ServerPath.PREFERRED_SEPARATOR_CHARACTER);
                }
            }

            final String currentPath = currentPathBuffer.toString();
            /*
             * If we wanted "root last" instead of "root first", then use
             * Math.abs(i - segments.length) for the hierarchy index
             */
            hierarchy[i - 1] = currentPath;
        }

        return hierarchy;
    }

    /**
     * Given two canonical server paths, return the farthest item from $/ that
     * parents both items. If one of the two paths is null, the other will be
     * returned. If both paths are null, an {@link IllegalArgumentException} is
     * thrown.
     *
     * @param path1
     *        a server path starting with $/ (must not be <code>null</code> or
     *        empty)
     * @param path2
     *        a server path starting with $/ (must not be <code>null</code> or
     *        empty)
     * @return the farthest item from $/ that parents both path1 and path2
     */
    public static String getCommonParent(final String path1, final String path2) {
        if (null == path1 && null == path2) {
            throw new IllegalArgumentException("path1"); //$NON-NLS-1$
        } else if (null == path1) {
            return path2;
        } else if (null == path2) {
            return path1;
        }

        String commonParent = path1;

        while (!isChild(commonParent, path2)) {
            commonParent = getParent(commonParent);
        }

        return commonParent;
    }

    /**
     * This method throws an InvalidPathException if there is a dollar sign
     * ('$') that follows a path separator ('/') since no part of a path is
     * allowed to start with a dollar sign.
     *
     * @param path
     *        the path to check (path must already be canonicalized)
     */
    public static void checkForIllegalDollarInPath(final String path) {
        ItemPath.checkForIllegalDollarInPath(path);
    }
}
