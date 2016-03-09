// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.DeleteLabelCommand;
import com.microsoft.tfs.client.common.commands.vc.EditLabelCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryLabelsCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.vc.EditLabelDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.EditLabelDialog.EditLabelResults;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResultStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.util.Check;

public class EditLabelTask extends BaseTask {
    private final TFSRepository repository;
    private final VersionControlLabel label;

    private boolean isDeleted = false;
    private VersionControlLabel newLabel;

    public EditLabelTask(final Shell shell, final TFSRepository repository, final VersionControlLabel label) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(label, "label"); //$NON-NLS-1$

        this.repository = repository;
        this.label = label;
    }

    @Override
    public IStatus run() {
        // requery so we can get item details
        final QueryLabelsCommand queryCommand =
            new QueryLabelsCommand(repository, label.getName(), label.getScope(), label.getOwner(), true);

        IStatus queryStatus = getCommandExecutor().execute(queryCommand);
        final VersionControlLabel[] selectedLabels = queryCommand.getLabels();

        if (queryStatus.isOK() && selectedLabels == null || selectedLabels.length != 1) {
            queryStatus = new Status(
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("EditLabelTask.CouldNotQueryLabel"), //$NON-NLS-1$
                null);
        }

        if (!queryStatus.isOK()) {
            return queryStatus;
        }

        final EditLabelDialog editLabelDialog = new EditLabelDialog(getShell(), repository, selectedLabels[0]);

        if (editLabelDialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        final EditLabelResults editResults = editLabelDialog.getEditLabelResults();

        // refresh the search results if they removed the label
        if (editResults.getLabel() == null) {
            final DeleteLabelCommand deleteCommand = new DeleteLabelCommand(repository, selectedLabels[0]);
            final IStatus deleteStatus = getCommandExecutor().execute(deleteCommand);

            if (!deleteStatus.isOK()) {
                ErrorDialog.openError(
                    getShell(),
                    Messages.getString("EditLabelTask.DeleteErrorDialogTitle"), //$NON-NLS-1$
                    null,
                    deleteStatus);
                return deleteStatus;
            }

            final LabelResult[] deleteResults = deleteCommand.getResults();

            newLabel = null;

            /*
             * If the only result is DELETED, then we can simply remove this
             * from the view
             */
            if (deleteResults.length != 1 || !deleteResults[0].getStatus().equals(LabelResultStatus.DELETED)) {
                isDeleted = true;
            }
        }
        // otherwise, just update the view with the new label info
        else {
            final VersionControlLabel newLabel = editResults.getLabel();

            final EditLabelCommand editCommand = new EditLabelCommand(
                repository,
                selectedLabels[0],
                newLabel,
                editResults.getDeletes(),
                editResults.getAdds());
            final IStatus editStatus = getCommandExecutor().execute(editCommand);

            if (!editStatus.isOK()) {
                ErrorDialog.openError(
                    getShell(),
                    Messages.getString("EditLabelTask.EditErrorDialogTitle"), //$NON-NLS-1$
                    null,
                    editStatus);
                return editStatus;
            }

            this.newLabel = newLabel;
        }

        return Status.OK_STATUS;
    }

    public VersionControlLabel getLabel() {
        return newLabel;
    }

    /**
     * Queries the result of the edit / delete operation to ensure that it is
     * "safe" - ie, the server validated that the expected behavior is correct.
     * Useful in the UI to do a quick remove of a label from a table (when
     * true), or to requery the list of labels (when false).
     *
     * @return true if the edit / delete was successful, false if ambiguous
     */
    public boolean isDeleted() {
        return isDeleted;
    }
}
