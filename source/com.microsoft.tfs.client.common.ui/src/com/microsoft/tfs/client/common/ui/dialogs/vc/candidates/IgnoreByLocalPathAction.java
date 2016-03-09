// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.commands.vc.AddTFSIgnoreExclusionsCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class IgnoreByLocalPathAction extends IgnoreAction {
    public IgnoreByLocalPathAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        // Text is set when enablement changes
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        if (changes.length == 1) {
            setText(Messages.getString("IgnoreByLocalPathAction.IgnoreLocalItem")); //$NON-NLS-1$
        } else {
            setText(Messages.getString("IgnoreByLocalPathAction.IgnoreLocalItems")); //$NON-NLS-1$
        }

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

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        final String[] localItems = PendingChange.toLocalItems(ChangeItem.getPendingChanges(changes));

        final Command command = new AddTFSIgnoreExclusionsCommand(repository, localItems);
        UICommandExecutorFactory.newUICommandExecutor(dialog.getShell()).execute(command);

        dialog.refreshCandidateTable(changes);
    }
}
