// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._WorkingFolder;

/**
 * Represents a {@link Mapping} between a server item (by path) and a local
 * path.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public final class WorkingFolder extends Mapping implements Cloneable {
    /**
     * A string which is used to represent a depth-one mapping when it is the
     * file part of a server path.
     */
    public static final String DEPTH_ONE_STRING = "*"; //$NON-NLS-1$

    public WorkingFolder(final _WorkingFolder wf) {
        super(wf);
    }

    public WorkingFolder(final String serverItem, final String localItem) {
        this(serverItem, localItem, WorkingFolderType.MAP);
    }

    /**
     * Creates a working folder object that maps a local path to a server path
     * with a recursion type of "full" (the traditional mapping style).
     *
     * @param serverItem
     *        the server item being mapped (must not be <code>null</code>)
     * @param localItem
     *        the local path being mapped (may be null for cloak mappings).
     * @param type
     *        the type of mapping to create (must not be <code>null</code>)
     */
    public WorkingFolder(final String serverItem, final String localItem, final WorkingFolderType type) {
        this(serverItem, localItem, type, RecursionType.FULL);
    }

    /**
     * Creates a working folder object that maps a local path to a server path.
     *
     * @param localItem
     *        the local path being mapped (may be null for cloak mappings).
     * @param serverItem
     *        the server item being mapped (must not be <code>null</code>)
     * @param type
     *        the type of mapping to create (must not be <code>null</code>)
     * @param recursion
     *        the type of recursion to use for the working folder (must not be
     *        <code>null</code>)
     */
    public WorkingFolder(
        final String serverItem,
        final String localItem,
        final WorkingFolderType type,
        final RecursionType recursion) {
        super(
            new _WorkingFolder(
                serverItem,
                type.getWebServiceObject(),
                Mapping.getDepthFromRecursion(recursion),
                LocalPath.nativeToTFS(localItem)));

        if (ServerPath.isServerPath(serverItem) == false) {
            throw new VersionControlException(
                MessageFormat.format(Messages.getString("WorkingFolder.PathIsNotValidServerPathFormat"), serverItem)); //$NON-NLS-1$
        }
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _WorkingFolder getWebServiceObject() {
        return (_WorkingFolder) webServiceObject;
    }

    /**
     * Gets the path to the local item in the native TFS path format, which is
     * Windows-style paths (even if this code is running on Unix).
     *
     * @return the local path in the native TFS path format (Windows-style)
     */
    public String getLocalItemRaw() {
        return getWebServiceObject().getLocal();
    }

    /**
     * Gets the path to the local item in this working folder mapping. Can be
     * null for cloaked mappings.
     *
     * @return the local path.
     */
    public String getLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getLocal());
    }

    /**
     * Sets the path to the local item in this working folder mapping. Can be
     * set to null for cloaked mappings.
     *
     * @param item
     *        the local path.
     */
    public void setLocalItem(final String item) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        getWebServiceObject().setLocal(LocalPath.nativeToTFS(item));
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "WorkingFolder [getServerItem()={0}, getLocalItem()={1}, getType()={2}, getDepth()={3}, isCloaked()={4}]", //$NON-NLS-1$
            getServerItem(),
            getLocalItem(),
            getType(),
            getDepth(),
            isCloaked());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof WorkingFolder == false) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        final WorkingFolder other = (WorkingFolder) obj;

        /*
         * This logic matches Visual Studio's OM as of TFS 2010 Beta 1.
         */

        final boolean serverItemsEqual = ServerPath.equals(getServerItem(), other.getServerItem());

        if (isCloaked() && other.isCloaked()) {
            return serverItemsEqual;
        }

        if (isCloaked() || other.isCloaked()) {
            return false;
        }

        if (serverItemsEqual
            && LocalPath.equals(getLocalItem(), other.getLocalItem())
            && getDepth().equals(other.getDepth())) {
            return getDepth() == other.getDepth();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * @return True if the two sets are identical, false otherwise
     */
    public static boolean areSetsEqual(final WorkingFolder[] set1, final WorkingFolder[] set2) {
        Check.notNull(set1, "set1"); //$NON-NLS-1$
        Check.notNull(set2, "set2"); //$NON-NLS-1$

        if (set1.length != set2.length) {
            return false;
        }

        final Comparator<WorkingFolder> c = new WorkingFolderComparator(WorkingFolderComparatorType.SERVER_PATH);

        Arrays.sort(set1, c);
        Arrays.sort(set2, c);

        int i;
        for (i = 0; i < set1.length; i++) {
            if (set1[i].getType() != set2[i].getType()) {
                break;
            }

            if (!ServerPath.equals(set1[i].getServerItem(), set2[i].getServerItem())) {
                break;
            }

            if (set1[i].getType() == WorkingFolderType.MAP
                && !LocalPath.equals(set1[i].getLocalItem(), set2[i].getLocalItem())) {
                break;
            }
        }

        return (i == set1.length);
    }

    public static Iterable<String> getWorkspaceRoots(final WorkingFolder[] workingFolders) {
        // Cloaked working folders have a null local item and appear at the top
        // of a sorted list when sorting working folders by local path.
        Arrays.sort(workingFolders, new WorkingFolderComparator(WorkingFolderComparatorType.LOCAL_PATH));

        final List<String> workspaceRoots = new ArrayList<String>();
        String currentRoot = null;

        for (final WorkingFolder workingFolder : workingFolders) {
            final String localItem = workingFolder.getLocalItem();
            if (null == localItem) {
                continue;
            }

            if (null == currentRoot || !LocalPath.isChild(currentRoot, localItem)) {
                if (null != currentRoot) {
                    workspaceRoots.add(currentRoot);
                }

                currentRoot = workingFolder.getLocalItem();
            }
        }

        if (null != currentRoot) {
            workspaceRoots.add(currentRoot);
        }

        return workspaceRoots;
    }

    /**
     * Uses this WorkingFolder object as the closest mapping to translate the
     * provided server item to a local item.
     *
     *
     * @param serverItem
     *        The server item to translate. This must be a subitem of this
     *        WorkingFolder's server item.
     * @return The local item of the provided server item
     */
    public String translateServerItemToLocalItem(final String serverItem) {
        if (null == getLocalItem()) {
            return null;
        }

        final WorkingFolder[] folders = new WorkingFolder[] {
            this
        };

        final PathTranslation translation = translateServerItemToLocalItem(serverItem, folders, false);

        if (translation == null) {
            return getLocalItem();
        } else {
            return translation.getTranslatedPath();
        }
    }

    /**
     * Uses this WorkingFolder object as the closest mapping to translate the
     * provided local item to a server item.
     *
     * Do not call this method on WorkingFolders which are cloaks, as they have
     * no local path. Furthermore, this method does not respect the Depth
     * property of a WorkingFolder.
     *
     * @param localItem
     *        The local item to translate. This must be a subitem of this
     *        WorkingFolder's local item.
     * @return The server item of the provided local item
     */
    public String translateLocalItemToServerItem(final String localItem) {
        if (null == getLocalItem()) {
            return null;
        }

        final WorkingFolder[] folders = new WorkingFolder[] {
            this
        };

        final PathTranslation translation = translateLocalItemToServerItem(localItem, folders);

        if (translation == null) {
            return getServerItem();
        } else {
            return translation.getTranslatedPath();
        }
    }

    @Override
    public Object clone() {
        return new WorkingFolder(getServerItem(), getLocalItem(), getType(), getDepth());
    }

    public static WorkingFolder[] clone(final WorkingFolder[] folders) {
        if (folders == null) {
            return null;
        }

        final WorkingFolder[] ret = new WorkingFolder[folders.length];

        for (int i = 0; i < folders.length; i++) {
            ret[i] = new WorkingFolder(
                folders[i].getServerItem(),
                folders[i].getLocalItem(),
                folders[i].getType(),
                folders[i].getDepth());
        }

        return ret;
    }

    public static String[] extractMappedPaths(final WorkingFolder[] workingFolders) {
        if (workingFolders == null) {
            return new String[0];
        }

        final List<String> mappedPaths = new ArrayList<String>(workingFolders.length);
        for (final WorkingFolder wf : workingFolders) {
            if (wf.getType() == WorkingFolderType.MAP) {
                mappedPaths.add(wf.getLocalItem());
            }
        }

        return mappedPaths.toArray(new String[mappedPaths.size()]);
    }

    public static String getLocalItemForServerItem(final String serverItem, final WorkingFolder[] folders) {
        return getLocalItemForServerItem(serverItem, folders, true);
    }

    public static String getLocalItemForServerItem(
        final String serverItem,
        final WorkingFolder[] folders,
        final boolean detectImplicitCloak) {
        if (serverItem == null) {
            return null;
        }

        final PathTranslation translation = translateServerItemToLocalItem(serverItem, folders, false);

        if (translation == null || translation.isCloaked()) {
            return null;
        }

        final String localItem = translation.getTranslatedPath();

        if (detectImplicitCloak && null != localItem) {
            final String reverseServer = getServerItemForLocalItem(localItem, folders);
            if (reverseServer == null || !ServerPath.equals(reverseServer, serverItem)) {
                // If the reverse translation does not give the server path that
                // was passed in, the item is implicitly cloaked.
                return null;
            }
        }

        return localItem;
    }

    public static String getServerItemForLocalItem(final String localItem, final WorkingFolder[] folders) {
        if (localItem == null) {
            return null;
        }

        final PathTranslation translation = translateLocalItemToServerItem(localItem, folders);

        if (translation == null || translation.isCloaked()) {
            return null;
        }

        return translation.getTranslatedPath();
    }

    /**
     * <p>
     * Translates a local path to a server path using the supplied working
     * folder mappings.
     * </p>
     * <p>
     * A {@link PathTranslation} is returned for items that are cloaked and the
     * translated item will be non-<code>null</code>.
     * </p>
     *
     * @param localPath
     *        the local path to translate into a server path (must not be
     *        <code>null</code> or empty)
     * @param folders
     *        the {@link WorkingFolder} mappings to translate with; can be
     *        arranged in any order (must not be <code>null</code>)
     * @return the working folder mapping that most precisely matches the given
     *         path (including cloak mappings), or <code>null</code> if the item
     *         is not mapped
     */
    public static PathTranslation translateLocalItemToServerItem(
        final String localPath,
        final WorkingFolder[] folders) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(folders, "folders"); //$NON-NLS-1$

        int localItemDepth = -1;
        int mappingLength = 0;
        WorkingFolder mapping = null;
        RecursionType depth = RecursionType.FULL;

        for (final WorkingFolder wf : folders) {
            if (wf == null || wf.isCloaked()) {
                continue;
            }

            final String wfLocalItem = wf.getLocalItem();

            if (LocalPath.isChild(wfLocalItem, localPath) && wfLocalItem.length() > mappingLength) {
                // This is the closest new mapping.
                mapping = wf;
                mappingLength = wfLocalItem.length();
                depth = wf.getDepth();

                if (wf.getDepth() == RecursionType.ONE_LEVEL) {
                    final int mappingDepth = LocalPath.getFolderDepth(wfLocalItem);

                    if (localItemDepth < 0) {
                        // Lazy initialization to save the calculation until a
                        // one-level is checked
                        localItemDepth = LocalPath.getFolderDepth(localPath);
                    }

                    if (mappingDepth + 1 == localItemDepth) {
                        // Mapped by parent, one-level.
                        depth = RecursionType.NONE;
                    } else if (mappingDepth != localItemDepth) {
                        // Unmapped; too far below this one-level mapping.
                        mapping = null;
                    }

                    /*
                     * Explicitly mapped to this exact item, one-level, depth is
                     * already set to one-level.
                     */
                }
            }
        }

        if (mapping != null) {
            final String mappingLocalItem = mapping.getLocalItem();
            final String mappingServerItem = mapping.getServerItem();

            final String serverPath = LocalPath.makeServer(localPath, mappingLocalItem, mappingServerItem);
            boolean isCloaked = false;

            /*
             * We have the server path for the local path, but the server path
             * could be cloaked.
             */
            final int mappingServerPathLength = mappingServerItem.length();
            for (final WorkingFolder wf : folders) {
                if (wf == null || wf.isCloaked() == false) {
                    continue;
                }

                final String wfServerItem = wf.getServerItem();

                if (wfServerItem.length() > mappingServerPathLength && ServerPath.isChild(wfServerItem, serverPath)) {
                    isCloaked = true;
                    break;
                }
            }

            return new PathTranslation(localPath, serverPath, isCloaked, depth);
        }

        return null;
    }

    /**
     * <p>
     * Translates a server path to a local path using the supplied working
     * folder mappings.
     * </p>
     * <p>
     * A {@link PathTranslation} is returned for items that are cloaked, but the
     * translated item will be <code>null</code>.
     * </p>
     *
     * @param serverPath
     *        the server path to translate into a local path (must not be
     *        <code>null</code> or empty)
     * @param folders
     *        the {@link WorkingFolder} mappings to translate with; can be
     *        arranged in any order (must not be <code>null</code>)
     * @param interpretOneLevelMappingsNormally
     *        if <code>true</code> working folder mappings with
     *        {@link RecursionType#ONE_LEVEL} are interpreted normally, if
     *        <code>false</code> {@link WorkingFolder} objects with
     *        {@link RecursionType#ONE_LEVEL} recursion types are interpreted as
     *        having {@link RecursionType#FULL} (useful for some UI methods
     *        which want to predict mapping locations even though an item may be
     *        too far below a one-level to be property considered "mapped")
     * @return the {@link PathTranslation} with the translation information (
     *         {@link PathTranslation#getTranslatedPath()} is <code>null</code>
     *         for cloaked items), or <code>null</code> if no appropriate
     *         working folder mapping was found
     */
    public static PathTranslation translateServerItemToLocalItem(
        final String serverPath,
        final WorkingFolder[] folders,
        final boolean interpretOneLevelMappingsNormally) {
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(folders, "folders"); //$NON-NLS-1$

        int serverItemDepth = -1;
        int mappingLength = 0;
        WorkingFolder mapping = null;
        RecursionType depth = RecursionType.FULL;

        for (final WorkingFolder wf : folders) {
            if (wf == null) {
                continue;
            }

            if (ServerPath.isChild(wf.getServerItem(), serverPath) && wf.getServerItem().length() > mappingLength) {
                // This is the closest new mapping.
                mapping = wf;
                mappingLength = wf.getServerItem().length();

                /*
                 * See Javadoc for why when interpretOneLevelMappingsNormally
                 * exists.
                 */
                depth = interpretOneLevelMappingsNormally ? wf.getDepth() : RecursionType.FULL;

                if (wf.getDepth() == RecursionType.ONE_LEVEL && interpretOneLevelMappingsNormally) {
                    final int mappingDepth = ServerPath.getFolderDepth(wf.getServerItem());

                    if (serverItemDepth < 0) {
                        // Lazy initialization to save the calculation until a
                        // one-level is checked
                        serverItemDepth = ServerPath.getFolderDepth(serverPath);
                    }

                    if (mappingDepth + 1 == serverItemDepth) {
                        // Mapped by parent, one-level.
                        depth = RecursionType.NONE;
                    } else if (mappingDepth != serverItemDepth) {
                        // Unmapped; too far below this one-level mapping.
                        mapping = null;
                    }

                    /*
                     * Explicitly mapped to this exact item, one-level, depth is
                     * already set to one-level.
                     */
                }
            }
        }

        final boolean isCloaked = (mapping != null) ? mapping.isCloaked() : false;

        if (mapping != null) {
            final String localPath =
                (isCloaked) ? null : ServerPath.makeLocal(serverPath, mapping.getServerItem(), mapping.getLocalItem());

            return new PathTranslation(serverPath, localPath, isCloaked, depth);
        }

        return null;
    }
}
