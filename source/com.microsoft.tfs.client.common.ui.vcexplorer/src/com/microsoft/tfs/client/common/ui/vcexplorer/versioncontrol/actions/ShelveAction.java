// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.commands.vc.GetPendingChangesCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractShelveTask;
import com.microsoft.tfs.client.common.ui.tasks.vc.ShelveWithPromptTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * Shelve the pending changes.
 */
public class ShelveAction extends TeamViewerAction {
    private TFSItem[] items;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final TFSRepository repository = getCurrentRepository();
        final ItemSpec[] itemSpecs = TFSItemUtils.getItemSpecs(items);

        final GetPendingChangesCommand queryCommand = new GetPendingChangesCommand(repository, itemSpecs, false);
        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return;
        }

        final PendingChange[] changes = queryCommand.getPendingChanges();

        if (changes == null || changes.length == 0) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("ShelveAction.DialogTitle"), //$NON-NLS-1$
                Messages.getString("ShelveAction.DialogText")); //$NON-NLS-1$
            return;
        }

        final AbstractShelveTask shelveTask = new ShelveWithPromptTask(getShell(), repository, changes);
        shelveTask.run();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);

        action.setEnabled(ActionEnablementHelper.selectionContainsPendingChanges(selection, true));
    }
}
