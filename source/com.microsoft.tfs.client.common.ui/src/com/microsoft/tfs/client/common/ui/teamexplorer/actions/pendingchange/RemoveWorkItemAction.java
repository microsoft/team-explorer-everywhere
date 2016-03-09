// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;

public class RemoveWorkItemAction extends TeamExplorerBaseAction {
    private WorkItemCheckinInfo[] selectedItems;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        selectedItems = (WorkItemCheckinInfo[]) selectionToArray(WorkItemCheckinInfo.class);

        if (selectedItems.length == 0) {
            action.setEnabled(false);
            return;
        }

        action.setEnabled(true);
    }

    @Override
    public void doRun(final IAction action) {
        // Remove associated work items from the model.
        final PendingChangesViewModel model = TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();
        model.dissociateWorkItems(selectedItems);
    }
}
