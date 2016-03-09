// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._BranchRelative;
import ms.tfs.versioncontrol.clientservices._03._Item;

/**
 * Represents a single branch in a tree of branch history items.
 *
 * @since TEE-SDK-10.1
 */
public class BranchHistoryTreeItem implements Hierarchical {
    private BranchHistoryTreeItem parent;
    private boolean isRequested;
    private List<BranchHistoryTreeItem> children = new ArrayList<BranchHistoryTreeItem>();
    private Item item;
    private Item fromItem;
    private int level = 0;
    private boolean hasRelative = false;

    /**
     * Added for TFS2010, to track the relative's branch target change type
     * (when constructed via a {@link BranchRelative}, will be 0 otherwise).
     */
    private int branchToChangeTypeFlags = 0;

    public BranchHistoryTreeItem() {
    }

    public BranchHistoryTreeItem(final String serverPath, final int changesetId) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        item = new Item();
        item.setServerItem(serverPath);
        item.setChangeSetID(changesetId);
    }

    public BranchHistoryTreeItem(final _Item item) {
        if (item != null) {
            this.item = new Item(item);
        }
    }

    public static BranchHistoryTreeItem createFromRelativeToItem(final _BranchRelative relative) {
        final BranchHistoryTreeItem item = new BranchHistoryTreeItem(relative.getBranchToItem());

        item.isRequested = relative.isReqstd();
        item.branchToChangeTypeFlags = relative.getBctype();
        item.hasRelative = true;

        return item;
    }

    public boolean hasRelative() {
        return hasRelative;
    }

    /**
     * @return the isRequested
     */
    public boolean isRequested() {
        return isRequested;
    }

    /**
     * @param isRequested
     *        the isRequested to set
     */
    public void setRequested(final boolean isRequested) {
        this.isRequested = isRequested;
    }

    /**
     * @return the item
     */
    public Item getItem() {
        return item;
    }

    /**
     * @param item
     *        the item to set
     */
    public void setItem(final Item item) {
        this.item = item;
    }

    /**
     * @return the change type of the relative's branch target (data only
     *         available from TFS 2010, 0 for previous server versions).
     */
    public int getBranchToChangeTypeEx() {
        return branchToChangeTypeFlags;
    }

    /**
     * @return the parent
     */
    @Override
    public Object getParent() {
        return parent;
    }

    /**
     * @return the parent
     */
    public BranchHistoryTreeItem getParentBranchHistoryTreeItem() {
        return parent;
    }

    /**
     * @param parent
     *        the parent to set
     */
    public void setParent(final BranchHistoryTreeItem parent) {
        this.parent = parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.util.Hierarchical#getChildren()
     */
    @Override
    public Object[] getChildren() {
        return children.toArray();
    }

    public List<BranchHistoryTreeItem> getChildrenAsList() {
        return children;
    }

    public boolean addChild(final BranchHistoryTreeItem child) {
        return children.add(child);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.util.Hierarchical#hasChildren()
     */
    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @param level
     *        the level to set
     */
    public void setLevel(final int level) {
        this.level = level;
    }

    /**
     * @param children
     *        the children to set
     */
    public void setChildren(final List<BranchHistoryTreeItem> children) {
        this.children = children;
    }

    public Item getFromItem() {
        return fromItem;
    }

    public void setFromItem(final Item fromItem) {
        this.fromItem = fromItem;
    }

    public String getServerItem() {
        return item == null ? "" : item.getServerItem(); //$NON-NLS-1$
    }

    public int getFromItemChangesetID() {
        return fromItem == null ? 0 : fromItem.getChangeSetID();
    }

    @Override
    public int hashCode() {
        return getServerItem().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BranchHistoryTreeItem other = (BranchHistoryTreeItem) obj;
        return getServerItem().equals(other);
    }

    protected BranchHistoryTreeItem searchForRequestedItem() {
        if (this.isRequested()) {
            return this;
        }

        for (final BranchHistoryTreeItem child : children) {
            final BranchHistoryTreeItem requestedItem = child.searchForRequestedItem();
            if (requestedItem != null) {
                return requestedItem;
            }
        }

        return null;
    }
}
