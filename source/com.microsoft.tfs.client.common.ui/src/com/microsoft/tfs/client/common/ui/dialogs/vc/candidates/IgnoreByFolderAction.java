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
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;

public class IgnoreByFolderAction extends IgnoreAction {
    public IgnoreByFolderAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        // Text is set when enablement changes
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        // This text is used for the disabled case
        setText(Messages.getString("IgnoreByFolderAction.IgnoreFolder")); //$NON-NLS-1$

        if (changes.length != 1) {
            return false;
        }

        if (!changes[0].getChangeType().contains(ChangeType.ADD)) {
            return false;
        }

        final String localItem = changes[0].getPendingChange().getLocalItem();
        if (localItem != null) {
            final String parent = changes[0].getFolder();
            setText(MessageFormat.format(
                Messages.getString("IgnoreByFolderAction.IgnoreFolderFormat"), //$NON-NLS-1$
                getFolderName(changes[0])));

            /*
             * Must verify the grandparent folder is mapped for enablement,
             * since that's where the ignore entry must go.
             */
            if (parent != null) {
                final String grandParent = LocalPath.getParent(parent);

                if (grandParent != null
                    && LocalPath.getFolderDepth(grandParent) + 2 == LocalPath.getFolderDepth(localItem)
                    && repository.getWorkspace().isLocalPathMapped(grandParent)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void doRun() {
        final ChangeItem[] changes = (ChangeItem[]) SelectionUtils.selectionToArray(getSelection(), ChangeItem.class);

        final String folder = changes[0].getFolder();
        if (folder == null || folder.length() == 0) {
            return;
        }

        // Add the appropriate exclusion at the closest mapping
        final Command command = new AddTFSIgnoreExclusionsCommand(repository, folder);
        UICommandExecutorFactory.newUICommandExecutor(dialog.getShell()).execute(command);

        // Scan all because many could have been under the folder
        dialog.refreshCandidateTable(null);
    }
}
