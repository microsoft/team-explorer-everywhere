// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public abstract class CompareWithBaseAction extends BaseAction {
    protected PendingChange selectedPendingChange;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (getSelectionSize() != 1) {
            action.setEnabled(false);
            return;
        }

        selectedPendingChange = getSelectedPendingChange();

        if (selectedPendingChange != null
            && selectedPendingChange.getItemType() == ItemType.FILE
            && selectedPendingChange.getLocalItem() != null
            && !PendingChangesHelpers.containsSymlinkChange(selectedPendingChange)) {
            action.setEnabled(true);
        } else {
            action.setEnabled(false);
        }
    }
}
