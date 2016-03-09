// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.tfsitem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.vc.QueryItemsExtendedCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;

public class TFSItemFactory {
    public static TFSItem getItem(final ExtendedItem extendedItem) {
        return getItem(extendedItem, null);
    }

    public static TFSItem getItem(final ExtendedItem extendedItem, final TFSRepository repository) {
        if (extendedItem.getItemType() == ItemType.FILE) {
            return new TFSFile(extendedItem);
        } else if (extendedItem.getItemType() == ItemType.FOLDER) {
            return new TFSFolder(extendedItem, repository);
        }
        return new TFSItem(extendedItem);
    }

    public static TFSItem createWithChildren(final ExtendedItem[] items) {
        return createWithChildren(items, false);
    }

    public static TFSItem createWithChildren(final ExtendedItem[] items, final boolean isPartialTree) {
        final TFSFolder root = (TFSFolder) getItem(items[0]);
        if (!isPartialTree) {
            root.setChildrenEmpty();
        }
        createWithChildren(root, items, 1, isPartialTree);
        return root;
    }

    public static TFSItem createImplicitWithChildren(final ExtendedItem[] extendedItems, final boolean b) {
        /* Note: extended items must all be at the same level */
        if (extendedItems == null || extendedItems.length == 0) {
            return null;
        }

        final ServerItemPath implicitPath = new ServerItemPath(extendedItems[0].getTargetServerItem()).getParent();
        final TFSFolder root = new TFSFolder(implicitPath);

        for (int i = 0; i < extendedItems.length; i++) {
            root.addChild(getItem(extendedItems[i]));
        }

        /* Add implicit pending adds here */
        final TFSItem[] implicitChildren = getImplicitAdds(root.getRepository(), root);
        for (int i = 0; i < implicitChildren.length; i++) {
            /* Note: don't update return value here */
            root.addChild(implicitChildren[i]);
        }

        return root;
    }

    private static int createWithChildren(
        final TFSFolder root,
        final ExtendedItem[] items,
        final int startIndex,
        final boolean isPartialTree) {
        int currentIndex = startIndex;
        Check.notNull(root, "root"); //$NON-NLS-1$
        Check.notNull(root.getFullPath(), "root.getFullPath()"); //$NON-NLS-1$
        while (currentIndex < items.length) {
            final ExtendedItem current = items[currentIndex];
            final String parentOfCurrent = ServerPath.getParent(current.getTargetServerItem());

            if (ServerPath.isDirectChild(root.getFullPath(), current.getTargetServerItem())) {
                // current is a direct child of root.
                final TFSItem directChild = getItem(current);
                if (directChild instanceof TFSFolder) {
                    // if the item is a folder, we set its children empty
                    // initially
                    // since the entire tree we're building is precomputed off
                    // a single server query, we don't want any empty folders to
                    // subsequently query the server for their children

                    if (!isPartialTree) {
                        ((TFSFolder) directChild).setChildrenEmpty();
                    }
                }
                root.addChild(directChild);

                ++currentIndex;
            } else if (ServerPath.isChild(root.getFullPath(), current.getTargetServerItem())) {
                // This is a descendent.
                final TFSFolder childParent = (TFSFolder) root.getChildByFullPath(parentOfCurrent);
                currentIndex = createWithChildren(childParent, items, currentIndex, isPartialTree);
            } else {
                // the current item is a sibling of root, so return
                // the current index to the caller who will know how to
                // process the sibling
                return currentIndex;
            }
        }

        /* Add implicit pending adds here */
        final TFSItem[] implicitChildren = getImplicitAdds(root.getRepository(), root);
        for (int i = 0; i < implicitChildren.length; i++) {
            /* Note: don't update return value here */
            root.addChild(implicitChildren[i]);
        }

        return items.length;
    }

    /**
     * Get item at the selected path. The server will accept a locally mapped
     * path or a server path.
     */
    public static TFSItem getItemAtPath(final TFSRepository repository, final String path) {
        /*
         * Extended branch information is requested so it may be used later in
         * the
         */
        final QueryItemsExtendedCommand queryCommand = new QueryItemsExtendedCommand(
            repository,
            path,
            ItemType.ANY,
            DeletedState.NON_DELETED,
            RecursionType.NONE,
            GetItemsOptions.INCLUDE_BRANCH_INFO);

        final IStatus queryStatus = new CommandExecutor().execute(queryCommand);

        if (!queryStatus.isOK()) {
            return null;
        }

        final ExtendedItem[][] extendedItems = queryCommand.getItems();

        if (extendedItems.length != 1 || extendedItems[0].length != 1) {
            return null;
        }

        return getItem(extendedItems[0][0]);
    }

    public static TFSFolder createRootWithChildren(final ExtendedItem[] items, final boolean isPartialTree) {
        if (items.length == 0) {
            return getRoot();
        } else if (new ServerItemPath(items[0].getTargetServerItem()).isRoot()) {
            return (TFSFolder) createWithChildren(items, isPartialTree);
        } else {
            final TFSFolder root = getRoot();
            createWithChildren(root, items, 0, isPartialTree);
            return root;
        }
    }

    public static TFSFolder getRoot(final TFSRepository repository) {
        return new TFSFolder(ServerItemPath.ROOT, repository);
    }

    public static TFSFolder getRoot() {
        return new TFSFolder(ServerItemPath.ROOT);
        // QueryCommand queryCommand = new QueryCommand("$/", ItemType.Folder,
        // RecursionType.OneLevel, false);
        // TFSItem[] items = queryCommand.query();
        // if (items.length >= 1)
        // {
        // return items[0];
        // }
        // else
        // {
        // return null;
        // }
    }

    public static TFSItem[] getImplicitAdds(TFSRepository repository, final TFSFolder parent) {
        if (repository == null) {
            repository =
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
        }

        final Set implicitAddPathSet = new HashSet();
        final List implicitAddList = new ArrayList();

        /*
         * Add in pending changes. We could have an add for a file well beneath
         * us that implicitly adds a folder at this level (but there is no
         * pending add for the folder itself). In this case, this would not be
         * caught by the QueryItemsExtended call, so we need to add it in from
         * the pending change cache.
         */
        final PendingChange[] pendingChanges =
            repository.getPendingChangeCache().getPendingChangesByServerPathRecursive(parent.getFullPath());

        for (int i = 0; i < pendingChanges.length; i++) {
            /*
             * Get the path relative to ourselves, then get only the directory
             * component (ie, our child.)
             */
            String relative = ServerPath.makeRelative(pendingChanges[i].getServerItem(), parent.getFullPath());

            final int firstSlash = relative.indexOf("/"); //$NON-NLS-1$

            /*
             * If there's no slash, this is actually the pending change.
             * Otherwise, this is an implicitly added parent folder.
             */
            boolean isParentFolder = false;
            if (firstSlash >= 0) {
                relative = relative.substring(0, firstSlash);
                isParentFolder = true;
            }

            if (relative.length() > 0) {
                final String childPath = ServerPath.combine(parent.getFullPath(), relative);

                if (parent.getChildByFullPath(childPath) == null && !implicitAddPathSet.contains(childPath)) {
                    if (isParentFolder) {
                        implicitAddList.add(new TFSFolder(new ServerItemPath(childPath)));
                    }

                    implicitAddPathSet.add(childPath);
                }
            }
        }

        return (TFSItem[]) implicitAddList.toArray(new TFSItem[implicitAddList.size()]);
    }
}