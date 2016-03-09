// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.utils;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * Utility class for helping with the handling of build paths.
 *
 * @since TEE-SDK-10.1
 */
public class BuildPath {
    public static final String PATH_SEPERATOR = "\\"; //$NON-NLS-1$
    public static final char PATH_SEPERATOR_CHAR = '\\';
    public static final String RECURSION_OPERATOR = "*"; //$NON-NLS-1$
    public static final String SLASH_RECURSION_OPERATOR = "\\*"; //$NON-NLS-1$
    public static final String ROOT_FOLDER = "\\"; //$NON-NLS-1$

    /**
     * Returns just the name portion of a path. If the path contains only a team
     * project then the file name will be empty.
     *
     * @param buildPath
     *        The path from which to retrieve the item name
     * @return A string representing the leaf name of the path (the last part
     *         following the \).
     */
    public static String getItemName(final String buildPath) {
        Check.notNullOrEmpty(buildPath, "buildPath"); //$NON-NLS-1$
        final int itemPos = buildPath.lastIndexOf(PATH_SEPERATOR);
        if (itemPos <= 0) {
            return ""; //$NON-NLS-1$
        }
        return buildPath.substring(itemPos + 1);
    }

    /**
     * Returns the team project portion of the build path.
     *
     * @param buildPath
     *        The path from which to retrieve the team project.
     * @return The team project portion of the build path.
     */
    public static String getTeamProject(final String buildPath) {
        Check.notNullOrEmpty(buildPath, "buildPath"); //$NON-NLS-1$
        if (buildPath.charAt(0) != PATH_SEPERATOR_CHAR) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("BuildPath.PathMustBeginWithBackslashFormat"), //$NON-NLS-1$
                    buildPath));
        }

        final int seperatorPos = buildPath.indexOf(PATH_SEPERATOR, 1);
        if (seperatorPos < 0) {
            if (buildPath.length() <= 1) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("BuildPath.PathMustSpecifyATeamProjectFormat"), //$NON-NLS-1$
                        buildPath));
            }
            // Path is in format \TeamProject
            return buildPath.substring(1);
        }
        // else path is in format \TeamProject\Something
        return buildPath.substring(1, seperatorPos);
    }

    /**
     * Return the build path built from TeamProject and item - in form
     * $/TeamProject/item
     *
     * @param teamProject
     *        The team project that the build belongs to.
     * @param itemName
     *        The item in the Team Project.
     * @return String containing build path in the form $/TeamProject/item.
     */
    public static String combine(final String teamProject, final String itemName) {
        final StringBuffer path = new StringBuffer(ROOT_FOLDER);
        path.append(teamProject).append(PATH_SEPERATOR_CHAR);

        if (itemName != null) {
            path.append(itemName);
        }

        return path.toString();
    }

    /**
     * Given a root and a path this method will return a combined path which is
     * canonicalized and rooted at the new root.
     *
     * @param rootPath
     *        The new root which should be used
     * @param relativePath
     *        The path which should be appended to the new root
     * @return A fully-qualified, canonicalized path formed by combining the two
     *         paths
     */
    public static String root(final String rootPath, final String relativePath) {
        // We need to make sure that the path on the right is not rooted.
        // Otherwise we get into a situation where Combine() will ignore the
        // left-hand side.
        if (relativePath != null && relativePath.length() > 0) {
            // Trim path separator chars from the front of the relative path.
            int i;
            for (i = 0; i < relativePath.length(); i++) {
                if (relativePath.charAt(i) != PATH_SEPERATOR_CHAR) {
                    break;
                }
            }

            String relativePart;
            if (i == 0) {
                relativePart = relativePath;
            } else {
                relativePart = relativePath.substring(i);
            }

            return combine(rootPath, relativePart);
        } else {
            return combine(rootPath, relativePath);
        }
    }

}
