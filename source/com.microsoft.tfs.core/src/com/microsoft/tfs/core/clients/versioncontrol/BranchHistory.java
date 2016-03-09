// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import ms.tfs.versioncontrol.clientservices._03._Item;

/**
 * Represents the history of a branch.
 *
 * @since TEE-SDK-10.1
 */
public class BranchHistory extends BranchHistoryTreeItem {
    private BranchHistoryTreeItem requestedItem = null;

    public BranchHistory() {
    }

    public BranchHistory(final _Item item) {
        super(item);
    }

    /**
     * @return the requestedItem
     */
    public BranchHistoryTreeItem getRequestedItem() {
        if (requestedItem == null) {
            requestedItem = searchForRequestedItem();
        }

        return requestedItem;
    }

    /**
     * @param requestedItem
     *        the requestedItem to set
     */
    public void setRequestedItem(final BranchHistoryTreeItem requestedItem) {
        this.requestedItem = requestedItem;
    }
}
