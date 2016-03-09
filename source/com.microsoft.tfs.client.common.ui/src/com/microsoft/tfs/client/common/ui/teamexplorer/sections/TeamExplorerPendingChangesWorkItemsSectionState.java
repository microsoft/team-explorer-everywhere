// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

public class TeamExplorerPendingChangesWorkItemsSectionState {

    private final boolean workItemCompositeVisible;
    private final String workItemId;

    public TeamExplorerPendingChangesWorkItemsSectionState(
        final boolean workItemCompositeVisible,
        final String workItemId) {
        this.workItemCompositeVisible = workItemCompositeVisible;
        this.workItemId = workItemId;
    }

    public boolean isWorkItemCompositeVisible() {
        return workItemCompositeVisible;
    }

    public String getWorkItemId() {
        return workItemId;
    }
}
