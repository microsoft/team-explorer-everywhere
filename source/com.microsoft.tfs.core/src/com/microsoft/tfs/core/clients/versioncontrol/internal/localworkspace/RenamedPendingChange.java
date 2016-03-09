// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;

public class RenamedPendingChange {
    private final String oldTargetServerItem;
    private final String targetServerItem;
    private LocalPendingChange pendingChange;
    private WorkspaceLocalItem localVersion;

    public RenamedPendingChange(final String oldTargetServerItem, final String targetServerItem) {
        this.oldTargetServerItem = oldTargetServerItem;
        this.targetServerItem = targetServerItem;
    }

    public String getOldTargetServerItem() {
        return oldTargetServerItem;
    }

    public String getTargetServerItem() {
        return targetServerItem;
    }

    public LocalPendingChange getPendingChange() {
        return pendingChange;
    }

    public void setPendingChange(final LocalPendingChange value) {
        pendingChange = value;
    }

    public WorkspaceLocalItem getLocalVersion() {
        return localVersion;
    }

    public void setLocalVersion(final WorkspaceLocalItem value) {
        localVersion = value;
    }
}
