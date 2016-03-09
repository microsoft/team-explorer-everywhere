// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;

public class RenameCalculationEntry {
    private String targetServerItem;
    private String sourceServerItem;
    private final LocalPendingChange pendingChange;
    private final boolean undoingChange;

    public RenameCalculationEntry(
        final String targetServerItem,
        final String sourceServerItem,
        final LocalPendingChange pcEntry,
        final boolean undoingChange) {
        this.targetServerItem = targetServerItem;
        this.sourceServerItem = sourceServerItem;
        this.pendingChange = pcEntry;
        this.undoingChange = undoingChange;
    }

    public String getTargetServerItem() {
        return targetServerItem;
    }

    public void setTargetServerItem(final String value) {
        targetServerItem = value;
    }

    public String getSourceServerItem() {
        return sourceServerItem;
    }

    public void setSourceServerItem(final String value) {
        sourceServerItem = value;
    }

    public LocalPendingChange getPendingChange() {
        return pendingChange;
    }

    public boolean isUndoingChange() {
        return undoingChange;
    }
}
