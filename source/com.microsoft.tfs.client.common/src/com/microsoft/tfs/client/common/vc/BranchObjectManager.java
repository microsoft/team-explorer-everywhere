// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.vc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public class BranchObjectManager {

    private final Workspace workspace;

    public BranchObjectManager(final Workspace workspace) {
        this.workspace = workspace;

    }

    public void convertFolderToBranchObject(
        final String item,
        final String owner,
        final String desc,
        final boolean recurse) {

        final BranchHistory history =
            workspace.getBranchHistory(new ItemSpec(item, RecursionType.NONE), LatestVersionSpec.INSTANCE);
        final BranchHistoryTreeItem requested = history.getRequestedItem();
        ItemIdentifier parentId = null;
        if (requested != null) {
            try {
                final BranchHistoryTreeItem parent = findParent(requested);
                if (parent != null) {
                    final String parentItem = parent.getServerItem();
                    final ItemIdentifier id = new ItemIdentifier(parentItem, LatestVersionSpec.INSTANCE, 0);
                    final BranchObject[] b = workspace.getClient().queryBranchObjects(id, RecursionType.NONE);
                    if (b != null && b.length > 0) {
                        parentId = id;
                    }
                }
            } catch (final Exception e) {
                // We have some problem accessing/querying the parent branch.
                // This is most likely a permission problem. Leave it alone and
                // don't disclose any more information than needed.
            }
        }
        // Convert folder to branch
        final ItemIdentifier itemId = new ItemIdentifier(item, LatestVersionSpec.INSTANCE, 0);
        final BranchProperties prop = BranchProperties.from(itemId, desc, owner, owner, parentId);
        createBranchObject(prop);

        // Connect children branched folders
        if (requested != null) {
            final BranchObject itemBranch = getBranchObject(itemId);
            final BranchObject parentBranch = getBranchObject(parentId);
            final HashMap connectedChildren = new HashMap();
            connectedChildren.put(itemBranch.getProperties().getRootItem(), itemBranch);
            if (parentBranch != null) {
                connectedChildren.put(parentBranch.getProperties().getRootItem(), parentBranch);
            }
            connectChildrenBranchedFolders(
                requested,
                itemBranch,
                parentBranch,
                recurse,
                owner,
                desc,
                connectedChildren);
        }
    }

    private void connectChildrenBranchedFolders(
        final BranchHistoryTreeItem item,
        final BranchObject itemBranch,
        final BranchObject parentBranch,
        final boolean recurse,
        final String owner,
        final String desc,
        final HashMap connectedChildren) {
        final BranchHistoryTreeItem[] children = findBranchedChildren(item);
        for (int i = 0; i < children.length; i++) {
            final BranchHistoryTreeItem child = children[i];
            final ItemIdentifier childItemId = new ItemIdentifier(child.getServerItem(), LatestVersionSpec.INSTANCE, 0);
            BranchObject childBranch = null;
            try {
                childBranch = getBranchObject(childItemId, connectedChildren);
            } catch (final Exception e) {
                // We have some problem accessing/querying the branch.
                // This is most likely a permission problem. Leave it alone and
                // don't disclose any more information than needed.
                continue;
            }

            // For a rename, the logical parent of the renamed branched folder
            // is
            // the same as the logical parent of the rename source branch.
            BranchObject logicalParent = itemBranch;
            final ChangeType childChangeType = ChangeType.fromIntFlags(0, child.getBranchToChangeTypeEx());
            if (childChangeType.contains(ChangeType.RENAME)) {
                if (parentBranch != null) {
                    logicalParent = parentBranch;
                }
            }

            // Connect an existing child branch object or convert a child
            // branched
            // folder into a branch object
            if (childBranch != null) {
                // If the child branched folder is already a branch but is not
                // connected to a parent, go ahead and connect it to this newly
                // converted branch.
                //
                // If the child branched folder is already connected to a
                // parent,
                // reconnect only if the old parent is deleted and the new
                // parent
                // is not (ie. favor non-deleted parent).
                final BranchProperties childProperties = childBranch.getProperties();
                if (childProperties.getParentBranch() == null) {
                    final BranchProperties prop = BranchProperties.from(childProperties, logicalParent);
                    updateBranchProperties(prop, true);
                    connectedChildren.put(childProperties.getRootItem(), childBranch);
                }
                // Child already connected... Need to check the existing
                // connected parent.
                // We only reconnect to new non-deleted parent. If the child
                // is already
                // connected to a parent branch BEFORE THIS conversion, we
                // do not
                // change the existing parent connection.
                else if (connectedChildren.containsKey(childProperties.getRootItem()) && !logicalParent.isDeleted()) {
                    final BranchObject existingParent =
                        getBranchObject(childProperties.getParentBranch(), connectedChildren);
                    if (existingParent != null && existingParent.isDeleted()) {
                        final BranchProperties prop = BranchProperties.from(childProperties, logicalParent);
                        updateBranchProperties(prop, true);
                        connectedChildren.put(childProperties.getRootItem(), childBranch);
                    }
                }
            } else if (recurse) {
                // If recursing, convert the child branched folder to a branch
                // and connect it to its parent.
                final BranchProperties childProperties =
                    BranchProperties.from(childItemId, desc, owner, owner, logicalParent.getProperties().getRootItem());
                createBranchObject(childProperties);
                childBranch = getBranchObject(childItemId);
                connectedChildren.put(childProperties.getRootItem(), childBranch);
            }
            // Recurse the children branched folders and convert/connect
            if (recurse) {
                connectChildrenBranchedFolders(
                    child,
                    childBranch,
                    logicalParent,
                    recurse,
                    owner,
                    desc,
                    connectedChildren);
            }
        }
    }

    private void createBranchObject(final BranchProperties prop) {
        updateBranchProperties(prop, false);
    }

    private boolean branchedForRenaming(final BranchHistoryTreeItem branchFolder) {
        return ChangeType.fromIntFlags(0, branchFolder.getBranchToChangeTypeEx()).contains(ChangeType.RENAME);
    }

    private BranchHistoryTreeItem[] findBranchedChildren(final BranchHistoryTreeItem branchFolder) {
        // Walk back the rename train
        BranchHistoryTreeItem item = branchFolder;
        BranchHistoryTreeItem lastName = branchFolder;
        final List children = new ArrayList();
        do {
            // Add the known immediate children
            final Object[] c = item.getChildren();
            for (int i = 0; i < c.length; i++) {
                final BranchHistoryTreeItem child = (BranchHistoryTreeItem) c[i];
                // Skip the renamed child since we are walking back up the
                // ancestry tree
                if (!child.getItem().getServerItem().equals(lastName.getItem().getServerItem())) {
                    children.add(child);
                }
            }

            // Stop when reaching the end of the rename train
            if (!branchedForRenaming(item)) {
                break;
            }

            lastName = item;
            item = item.getParentBranchHistoryTreeItem();
        } while (item != null);
        return (BranchHistoryTreeItem[]) children.toArray(new BranchHistoryTreeItem[0]);
    }

    private BranchHistoryTreeItem findParent(BranchHistoryTreeItem branchFolder) {
        while (branchedForRenaming(branchFolder)) {
            branchFolder = branchFolder.getParentBranchHistoryTreeItem();
        }
        return branchFolder.getParentBranchHistoryTreeItem();
    }

    private BranchObject getBranchObject(final ItemIdentifier item) {
        if (item == null) {
            return null;
        }
        final BranchObject[] branches = workspace.getClient().queryBranchObjects(item, RecursionType.NONE);
        if (branches.length == 1) {
            return branches[0];
        } else {
            return null;
        }
    }

    private BranchObject getBranchObject(final ItemIdentifier item, final HashMap connected) {
        if (item == null) {
            return null;
        }
        if (connected.containsKey(item)) {
            return (BranchObject) connected.get(item);
        }
        return getBranchObject(item);
    }

    private void updateBranchProperties(final BranchProperties prop, final boolean updateExisting) {
        workspace.getClient().updateBranchObject(prop, updateExisting);
    }

}
