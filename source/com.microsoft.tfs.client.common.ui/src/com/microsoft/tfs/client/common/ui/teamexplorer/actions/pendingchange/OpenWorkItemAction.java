// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;

public class OpenWorkItemAction extends TeamExplorerBaseAction {
    private WorkItemCheckinInfo[] selectedItems;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        selectedItems = (WorkItemCheckinInfo[]) selectionToArray(WorkItemCheckinInfo.class);

        if (selectedItems.length != 1) {
            action.setEnabled(false);
            return;
        }

        action.setEnabled(true);
    }

    @Override
    public void doRun(final IAction action) {
        WorkItemEditorHelper.openEditor(getContext().getServer(), selectedItems[0].getWorkItem().getID());
    }
}
