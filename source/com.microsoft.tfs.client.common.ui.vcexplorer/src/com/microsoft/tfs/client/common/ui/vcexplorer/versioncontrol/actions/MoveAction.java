// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.commands.vc.RefreshPendingChangesCommand;
import com.microsoft.tfs.client.common.commands.vc.RenameCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.MoveDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;

public class MoveAction extends TeamViewerAction {
    private TFSItem item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        /*
         * MSFT difference: they disable Move / Rename actions if the local
         * parent is not mapped. I believe this is not obvious as to why they're
         * disabled. We check here and raise a dialog instead.
         */
        final String selectedServerPath = item.getFullPath();
        final String selectedServerParent = ServerPath.getParent(selectedServerPath);

        if (getCurrentRepository().getWorkspace().isServerPathMapped(selectedServerParent) == false) {
            final String messageFormat = Messages.getString("MoveAction.AddMappingDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, selectedServerParent);

            MessageDialog.openError(
                getShell(),
                Messages.getString("MoveAction.ParentNotMappedDialogTitle"), //$NON-NLS-1$
                message);
            return;
        }

        final MoveDialog moveDialog = new MoveDialog(getShell(), getCurrentRepository(), selectedServerPath);

        if (moveDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final String destinationPath = moveDialog.getDestinationServerPath();
        final String sourcePath = item.getFullPath();

        doMoveOperation(sourcePath, destinationPath);
    }

    protected void doMoveOperation(final String sourcePath, final String destinationPath) {
        /*
         * check to see if we're moving an item to itself, in which case we do
         * nothing. don't use ServerPath.equals here, use strict String
         * (case-sensitive) equality.
         */
        if (sourcePath.equals(destinationPath)) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("MoveAction.MoveDialogTitle"), //$NON-NLS-1$
                Messages.getString("MoveAction.MoveDialogText")); //$NON-NLS-1$
            return;
        }

        final RenameCommand renameCommand = new RenameCommand(getCurrentRepository(), sourcePath, destinationPath);
        final IStatus renameStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
            new ResourceChangingCommand(renameCommand));

        if (renameStatus.getSeverity() == IStatus.ERROR) {
            return;
        }

        /*
         * Rename operations can be implicitly undone by the server, we need to
         * refresh pending changes
         */
        final RefreshPendingChangesCommand refreshCommand = new RefreshPendingChangesCommand(getCurrentRepository());
        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(refreshCommand);
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        final TFSRepository repository = getCurrentRepository();

        try {
            action.setEnabled((item != null
                && repository != null
                && repository.getWorkspace() != null
                && repository.getWorkspace().isServerPathMapped(item.getFullPath()))
                && (ActionEnablementHelper.selectionContainsNonLocalItem(selection) == false
                    || ActionEnablementHelper.selectionContainsPendingChangesOfAnyChangeType(
                        selection,
                        false,
                        ChangeType.combine(new ChangeType[] {
                            ChangeType.ADD,
                            ChangeType.BRANCH
            })))
                && ActionEnablementHelper.selectionContainsProjectFolder(selection) == false
                && ActionEnablementHelper.selectionContainsRoot(selection) == false);
        } catch (final PathTooLongException e) {
            action.setEnabled(false);
        }
    }
}
