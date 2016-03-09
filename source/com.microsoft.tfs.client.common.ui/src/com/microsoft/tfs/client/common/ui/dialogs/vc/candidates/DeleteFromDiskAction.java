// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;

public class DeleteFromDiskAction extends CandidateAction {
    public DeleteFromDiskAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        setText(Messages.getString("DeleteFromDiskAction.DeleteFromDiskActionText")); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        final DeleteFromDiskCommand command = new DeleteFromDiskCommand(changes);
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(dialog.getShell());
        executor.execute(command);

        dialog.refreshCandidateTable(changes);
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        if (selection.size() == 0) {
            return false;
        }

        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // Ensure all are adds
        boolean nonAdd = false;
        for (final ChangeItem change : changes) {
            if (!change.getChangeType().contains(ChangeType.ADD)) {
                nonAdd = true;
                break;
            }
        }

        return !nonAdd;
    }

    public boolean isVisible(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // Show if any is an add
        for (final ChangeItem change : changes) {
            if (change.getChangeType().contains(ChangeType.ADD)) {
                return true;
            }
        }

        return false;
    }
}