// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.commands.vc.DeleteCommand;
import com.microsoft.tfs.client.common.commands.vc.GetPendingChangesCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class DeleteAction extends TeamViewerAction {
    private TFSItem[] items;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        /*
         * Only query pending changes for the current workspace. This is to
         * emulate VisualStudio, which allows you to pend a delete if another
         * workspace has pending changes.
         */
        final GetPendingChangesCommand queryCommand =
            new GetPendingChangesCommand(getCurrentRepository(), TFSItemUtils.getItemSpecs(items), false);

        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return;
        }

        final PendingChange[] pendingChanges = queryCommand.getPendingChanges();

        /*
         * TODO: it's probably okay to continue if the pending change type is
         * 'delete'.
         */
        if (pendingChanges != null && pendingChanges.length > 0) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("DeleteAction.ItemHasChangesDialogTitle"), //$NON-NLS-1$
                Messages.getString("DeleteAction.ItemHasChangesDialogText")); //$NON-NLS-1$
            return;
        }

        final DeleteCommand deleteCommand = new DeleteCommand(getCurrentRepository(), TFSItemUtils.getFullPaths(items));

        final IStatus deleteStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(
            new ResourceChangingCommand(deleteCommand));

        if (!deleteStatus.isOK()) {
            return;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);

        action.setEnabled(ActionEnablementHelper.selectionContainsRoot(selection) == false
            && ActionEnablementHelper.selectionContainsProjectFolder(selection) == false
            && ActionEnablementHelper.selectionContainsNonLocalItem(selection) == false);
    }
}
