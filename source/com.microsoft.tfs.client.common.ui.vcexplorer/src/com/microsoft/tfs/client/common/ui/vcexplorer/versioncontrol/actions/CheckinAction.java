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
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckinTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class CheckinAction extends TeamViewerAction {
    private TFSItem[] items;

    public CheckinAction() {
        setName(Messages.getString("CheckinAction.ActionName")); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final TFSRepository repository = getCurrentRepository();
        final ItemSpec[] specs = TFSItemUtils.getItemSpecs(items);

        final GetPendingChangesCommand queryCommand = new GetPendingChangesCommand(repository, specs, false);
        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return;
        }

        final PendingChange[] changes = queryCommand.getPendingChanges();

        if (changes == null || changes.length == 0) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("CheckinAction.NoChangesDialogTitle"), //$NON-NLS-1$
                Messages.getString("CheckinAction.NoChangesDialogText")); //$NON-NLS-1$
            return;
        }

        final CheckinTask checkinTask = new CheckinTask(getShell(), repository, changes, null);
        checkinTask.run();
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

        action.setEnabled(ActionEnablementHelper.selectionContainsPendingChanges(selection, true));
    }
}
