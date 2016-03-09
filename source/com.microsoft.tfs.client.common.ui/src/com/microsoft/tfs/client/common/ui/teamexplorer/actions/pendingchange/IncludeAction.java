// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class IncludeAction extends BaseAction {
    private PendingChange[] selectedPendingChanges;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        selectedPendingChanges = getSelectedPendingChanges();
        action.setEnabled(selectedPendingChanges.length > 0);
    }

    @Override
    public void doRun(final IAction action) {
        getModel().includePendingChanges(selectedPendingChanges);
    }
}
