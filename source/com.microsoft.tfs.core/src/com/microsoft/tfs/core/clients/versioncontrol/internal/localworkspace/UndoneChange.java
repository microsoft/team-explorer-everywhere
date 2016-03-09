// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;

public class UndoneChange {
    private final LocalPendingChange pendingChange;
    private final ChangeType undoneChangeType;
    private String revertToServerItem;

    public UndoneChange(
        final LocalPendingChange pcEntry,
        final String revertToServerItem,
        final ChangeType undoneChangeType) {
        this.pendingChange = pcEntry;
        this.revertToServerItem = revertToServerItem;
        this.undoneChangeType = undoneChangeType;
    }

    /**
     * Returns the local pending change held by this instance.
     */
    public LocalPendingChange getPendingChange() {
        return pendingChange;
    }

    public ChangeType getUndoneChangeType() {
        return undoneChangeType;
    }

    public String getRevertToServerItem() {
        return revertToServerItem;
    }

    public void setRevertToServerItem(final String value) {
        revertToServerItem = value;
    }

    /**
     * Returns true if this is a pending add
     */
    public boolean isUndoingAdd() {
        return undoneChangeType.contains(ChangeType.ADD);
    }

    /**
     * Returns true if this is a pending rename
     */
    public boolean isUndoingRename() {
        return undoneChangeType.contains(ChangeType.RENAME);
    }

    /**
     * Returns true if this is a pending branch
     */
    public boolean isUndoingBranch() {
        return undoneChangeType.contains(ChangeType.BRANCH);
    }

    /**
     * Returns true if this pending change has a lock
     */
    public boolean isUndoingLock() {
        return undoneChangeType.contains(ChangeType.LOCK);
    }

    /**
     * Renames and deletes on folders are recursive changes. True if this change
     * is a recursive change.
     */
    public boolean isUndoingRecursiveChange() {
        return undoneChangeType.containsAny(ChangeType.RENAME_OR_DELETE)
            && (ItemType.FOLDER == pendingChange.getItemType());
    }

    /**
     * The change type of the pending change, minus the bits that are being
     * undone.
     */
    public ChangeType getRemainingChangeType() {
        return pendingChange.getChangeType().remove(undoneChangeType);
    }
}
