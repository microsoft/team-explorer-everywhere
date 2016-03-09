// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

public class WorkspaceLocalItemPair {
    private WorkspaceLocalItem committed;
    private WorkspaceLocalItem uncommitted;

    public WorkspaceLocalItem getCommitted() {
        return committed;
    }

    public WorkspaceLocalItem getUncommitted() {
        return uncommitted;
    }

    public void setCommitted(final WorkspaceLocalItem value) {
        committed = value;
    }

    public void setUncommitted(final WorkspaceLocalItem value) {
        uncommitted = value;
    }

    public String getServerItem() {
        if (getCommitted() != null) {
            return getCommitted().getServerItem();
        }

        if (getUncommitted() != null) {
            return getUncommitted().getServerItem();
        }

        return null;
    }
}
