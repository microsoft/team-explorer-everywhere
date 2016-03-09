// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.util.Check;

/**
 * Internal utilities to manage branch history items. Not public API.
 */
public class BranchHistoryUtil {
    /**
     * Updates the given {@link BranchHistory} object to include the latest
     * {@link Item}s from the server. (By default, the {@link BranchHistory}
     * will only include sparse items that do not have full details.
     *
     * NOTE: this method does NOT rebalance renames or deletions in the history
     * tree.
     *
     * @param workspace
     *        A {@link Workspace} to query from (not <code>null</code>)
     * @param versionSpec
     *        The {@link VersionSpec} to query items at (not <code>null</code>)
     * @param branchHistory
     *        The {@link BranchHistory} obtained by a server call, with sparse
     *        from and to {@link Item}s.
     */
    public static void updateServerItems(
        final Workspace workspace,
        final VersionSpec versionSpec,
        final BranchHistory branchHistory) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$
        Check.notNull(branchHistory, "branchHistory"); //$NON-NLS-1$

        final Map<String, Item> versionMap = createVersionMap(workspace, versionSpec, branchHistory);
        updateServerItems(versionMap, branchHistory);
    }

    /**
     * Maps (to) item server paths to latest version of the (to) item on the
     * server. This *will* include deleted items, but *will not* include items
     * that were deleted as part of a source rename.
     *
     * @param workspace
     * @param branchHistory
     * @return
     */
    private static Map<String, Item> createVersionMap(
        final Workspace workspace,
        final VersionSpec versionSpec,
        final BranchHistory branchHistory) {
        final Map<String, Item> latestVersionMap = new HashMap<String, Item>();

        final Set<ItemSpec> itemSpecSet = collectServerItems(branchHistory);

        final ItemSpec[] itemSpecs = itemSpecSet.toArray(new ItemSpec[itemSpecSet.size()]);

        if (itemSpecs.length > 0) {
            final ItemSet[] items = workspace.getClient().getItems(
                itemSpecs,
                versionSpec,
                DeletedState.ANY,
                ItemType.ANY,
                GetItemsOptions.INCLUDE_SOURCE_RENAMES);

            for (int i = 0; i < items.length; i++) {
                final Item[] branchItems = items[i].getItems();

                for (int j = 0; j < branchItems.length; j++) {
                    latestVersionMap.put(branchItems[j].getServerItem(), branchItems[j]);
                }
            }
        }

        return latestVersionMap;
    }

    private static Set<ItemSpec> collectServerItems(final Hierarchical branchHistoryItem) {
        final Set<ItemSpec> serverItems = new HashSet<ItemSpec>();

        /*
         * Only include BranchHistoryTreeItems (the BranchHistory server path is
         * "")
         */
        if (branchHistoryItem instanceof BranchHistoryTreeItem && !(branchHistoryItem instanceof BranchHistory)) {
            final BranchHistoryTreeItem treeItem = (BranchHistoryTreeItem) branchHistoryItem;

            if (treeItem.getItem() != null) {
                serverItems.add(new ItemSpec(treeItem.getItem().getServerItem(), RecursionType.NONE));
            }
        }

        final Object[] children = branchHistoryItem.getChildren();
        for (int i = 0; i < children.length; i++) {
            serverItems.addAll(collectServerItems((Hierarchical) children[i]));
        }

        return serverItems;
    }

    private static void updateServerItems(final Map<String, Item> versionItems, final Hierarchical branchHistoryItem) {
        /* Go depth first for rebalancing deletions */
        final Object[] children = branchHistoryItem.getChildren();

        for (int i = 0; i < children.length; i++) {
            updateServerItems(versionItems, (Hierarchical) children[i]);
        }

        if (branchHistoryItem instanceof BranchHistory) {
            return;
        }

        /* Examine myself for deletion, rename */
        final BranchHistoryTreeItem treeItem = (BranchHistoryTreeItem) branchHistoryItem;

        if (treeItem.getItem() != null) {
            final String serverItem = treeItem.getItem().getServerItem();
            final Item updatedItem = versionItems.get(serverItem);

            if (updatedItem != null) {
                treeItem.setItem(updatedItem);
            }
        }
    }
}
