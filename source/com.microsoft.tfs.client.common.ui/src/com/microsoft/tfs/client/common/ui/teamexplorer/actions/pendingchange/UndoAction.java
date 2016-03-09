// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions.UndoPendingChangesAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class UndoAction extends BaseAction {
    private PendingChange[] selectedPendingChanges;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        selectedPendingChanges = getSelectedPendingChanges();
        action.setEnabled(selectedPendingChanges.length > 0);
    }

    @Override
    public void doRun(final IAction action) {
        Check.notNull(selectedPendingChanges, "selectedPendingChanges"); //$NON-NLS-1$

        final TFSRepository repository = getContext().getDefaultRepository();

        final ChangeItem[] changeitems =
            PendingChangesHelpers.pendingChangesToChangeItems(repository, selectedPendingChanges);
        UndoPendingChangesAction.undoPendingChanges(getShell(), repository, changeitems);
    }
}
