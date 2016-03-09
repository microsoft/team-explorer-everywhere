// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.item;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

/**
 * Represents a server-side item path (file or folder) in the form
 * $/Project/Folder/File. May be useful as it overrides equals() and hashCode().
 * Most calls are proxied to {@link ServerPath} for maintainability.
 *
 * This was ItemPath in the old UI code.
 */
public class ServerItemPath {
    public static final ServerItemPath ROOT = new ServerItemPath(ServerPath.ROOT);

    private final String fullPath;

    /**
     * Create a new {@link ServerItemPath} given a full string path, like
     * $/a1/a2/a3. See the note above about trailing slashes.
     *
     * @param fullPath
     *        full path to create item path with - must not be null
     */
    public ServerItemPath(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        fullPath = ServerPath.canonicalize(path);
    }

    /**
     * Obtain the full path of this ItemPath. This full path never includes a
     * trailing forward slash, unless this ItemPath represents root, in which
     * case the full path always includes a trailing forward slash.
     *
     * @return full path, never null
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * Obtain the file part of this ItemPath. The simple name is the unqualified
     * string that is the last segment of this item path. It represents the
     * folder or file name. If this ItemPath represents the root, then
     * getFilePart() will return "".
     *
     * @return name, never null
     */
    public String getFilePart() {
        return ServerPath.getFileName(fullPath);
    }

    /**
     * Obtain the simple name of this ItemPath. The simple name is the
     * unqualified string that is the last segment of this item path. It
     * represents the folder or file name. If this ItemPath represents the root,
     * then getName() will return "$".
     *
     * @return name, never null
     */
    public String getName() {
        if (isRoot()) {
            return ServerPath.ROOT_NAME_ONLY;
        }

        return ServerPath.getFileName(fullPath);
    }

    /**
     * Obtain an ItemPath object representing the parent of this ItemPath. The
     * parent is the same path but with the last segment removed.
     *
     * @return the parent ItemPath or null if this ItemPath is the root
     */
    public ServerItemPath getParent() {
        if (isRoot()) {
            return null;
        }

        return new ServerItemPath(ServerPath.getParent(fullPath));
    }

    /**
     * @return true if this ItemPath represents the root path, "$/"
     */
    public boolean isRoot() {
        return ServerPath.equals(fullPath, ServerPath.ROOT);
    }

    /**
     * Checks to see if this ItemPath is an ancestor path of the given path, or
     * the paths are equal. See also
     * {@link ServerItemPath#isStrictParentOf(ServerItemPath)}.
     *
     * @param path
     *        possible descendant path of this one
     * @return true if this path is an ancestor of the given path
     */
    public boolean isParentOf(final ServerItemPath possibleChild) {
        return ServerPath.isChild(fullPath, possibleChild.getFullPath());
    }

    /**
     * Checks to see if this ItemPath is an ancestor path of the given path, or
     * the paths are equal. This requires that the path be strictly beneath this
     * path.
     *
     * @param path
     *        possible descendant path of this one
     * @return true if this path is an ancestor of the given path
     */
    public boolean isStrictParentOf(final ServerItemPath possibleChild) {
        return (isParentOf(possibleChild) && !equals(possibleChild));
    }

    /**
     * Creates an ItemPath that represents a child of this ItemPath. That is,
     * the child path will return an ItemPath equivalent to this one for
     * getParent().
     *
     * @param childName
     *        the name of the child path to create
     * @return a child item path
     */
    public ServerItemPath combine(final String relative) {
        return new ServerItemPath(ServerPath.combine(fullPath, relative));
    }

    @Override
    public String toString() {
        return fullPath;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ServerItemPath)) {
            return false;
        }

        final ServerItemPath other = (ServerItemPath) obj;
        return ServerPath.equals(fullPath, other.getFullPath());
    }

    @Override
    public int hashCode() {
        return fullPath.toLowerCase().hashCode();
    }

    public int getFolderDepth() {
        return ServerPath.getFolderDepth(fullPath);
    }

    public String getPathSection(final int index) {
        return split()[index];
    }

    /**
     * Convenience method to determine the team project path of an ItemPath.
     *
     * @return The ServerItemPath to the Team Project or null if this is the
     *         root path.
     */
    public ServerItemPath getTeamProject() {
        if (isRoot()) {
            return null;
        }

        return getHierarchy()[1];
    }

    private String[] split() {
        final String[] split = ServerPath.split(fullPath);

        /* Sanity check, should always be true */
        if (ServerPath.equals(split[0], ServerPath.ROOT)) {
            split[0] = ServerPath.ROOT_NAME_ONLY;
        }

        return split;
    }

    public ServerItemPath[] getHierarchy() {
        final String[] paths = ServerPath.getHierarchy(fullPath);

        if (paths == null) {
            return null;
        }

        final ServerItemPath[] items = new ServerItemPath[paths.length];
        for (int i = 0; i < paths.length; i++) {
            items[i] = new ServerItemPath(paths[i]);
        }

        return items;
    }
}
