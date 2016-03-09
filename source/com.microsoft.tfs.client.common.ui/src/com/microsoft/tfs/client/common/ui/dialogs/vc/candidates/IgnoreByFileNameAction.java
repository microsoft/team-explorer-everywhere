// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.commands.vc.AddTFSIgnoreExclusionsCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SelectionUtils;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;

public class IgnoreByFileNameAction extends IgnoreAction {
    public IgnoreByFileNameAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        // Text is set when enablement changes
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // This text is used for the disabled case
        setText(Messages.getString("IgnoreByFileNameAction.IgnoreFileName")); //$NON-NLS-1$

        if (changes.length != 1) {
            return false;
        }

        if (!changes[0].getChangeType().contains(ChangeType.ADD)) {
            return false;
        }

        setText(MessageFormat.format(
            Messages.getString("IgnoreByFileNameAction.IgnoreFileNameFormat"), //$NON-NLS-1$
            changes[0].getName()));
        return true;
    }

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        final String fileName = getFileName(changes[0]);
        if (fileName == null || fileName.length() == 0) {
            return;
        }

        // Add the appropriate exclusion at the closest mapping
        final Command command =
            new AddTFSIgnoreExclusionsCommand(repository, changes[0].getPendingChange().getLocalItem(), fileName);
        UICommandExecutorFactory.newUICommandExecutor(dialog.getShell()).execute(command);

        dialog.refreshCandidateTable(changes);
    }
}