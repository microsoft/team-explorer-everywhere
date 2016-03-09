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

public class IgnoreByExtensionAction extends IgnoreAction {
    public IgnoreByExtensionAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        // Text is set when enablement changes
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // This text is used for the disabled case
        setText(Messages.getString("IgnoreByExtensionAction.IgnoreExtension")); //$NON-NLS-1$

        if (changes.length != 1) {
            return false;
        }

        if (!changes[0].getChangeType().contains(ChangeType.ADD)) {
            return false;
        }

        final String extension = getExtension(changes[0]);
        if (extension == null || extension.length() == 0) {
            return false;
        }

        setText(MessageFormat.format(Messages.getString("IgnoreByExtensionAction.IgnoreExtensionFormat"), extension)); //$NON-NLS-1$
        return true;
    }

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        final String extension = getExtension(changes[0]);
        if (extension == null || extension.length() == 0) {
            return;
        }

        // Add the appropriate exclusion at the closest mapping
        final String exclusion = "*" + extension; //$NON-NLS-1$
        final Command command =
            new AddTFSIgnoreExclusionsCommand(repository, changes[0].getPendingChange().getLocalItem(), exclusion);
        UICommandExecutorFactory.newUICommandExecutor(dialog.getShell()).execute(command);

        // Scan everything because the wildcard could have matched them all
        dialog.refreshCandidateTable(null);
    }
}
