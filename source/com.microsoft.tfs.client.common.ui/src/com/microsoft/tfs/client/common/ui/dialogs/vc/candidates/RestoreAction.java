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

public class RestoreAction extends CandidateAction {
    public RestoreAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        // Text is set when enablement changes
    }

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        final RestoreItemsCommand command = new RestoreItemsCommand(repository.getWorkspace(), changes);
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(dialog.getShell());
        executor.execute(command);

        dialog.refreshCandidateTable(changes);
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        if (changes.length == 1) {
            setText(Messages.getString("RestoreAction.RestoreActionSingleItemText")); //$NON-NLS-1$
        } else {
            setText(Messages.getString("RestoreAction.RestoreActionMultipleItemsText")); //$NON-NLS-1$
        }

        // Ensure all are deletes
        boolean nonDelete = false;
        for (final ChangeItem change : changes) {
            if (!change.getChangeType().contains(ChangeType.DELETE)) {
                nonDelete = true;
                break;
            }
        }

        return !nonDelete;
    }

    public boolean isVisible(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // Enable for any delete
        for (final ChangeItem change : changes) {
            if (change.getChangeType().contains(ChangeType.DELETE)) {
                return true;
            }
        }

        return false;
    }
}
