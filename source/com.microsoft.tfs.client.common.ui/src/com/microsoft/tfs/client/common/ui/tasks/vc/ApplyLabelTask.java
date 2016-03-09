// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.CreateLabelCommand;
import com.microsoft.tfs.client.common.commands.vc.DeleteLabelCommand;
import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ApplyLabelDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.EditLabelDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.EditLabelDialog.EditLabelResults;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.util.Check;

public class ApplyLabelTask extends BaseTask {
    private final TFSRepository repository;
    private final String labelServerPath;

    public ApplyLabelTask(final Shell shell, final TFSRepository repository, final String labelServerPath) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(labelServerPath, "labelServerPath"); //$NON-NLS-1$

        this.repository = repository;
        this.labelServerPath = labelServerPath;
    }

    @Override
    public IStatus run() {
        final ApplyLabelDialog applyLabelDialog = new ApplyLabelDialog(getShell(), repository, labelServerPath);

        if (applyLabelDialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        VersionControlLabel newLabel;
        LabelItemSpec[] newLabelItemSpecs;

        /* Edit the label before continuing */
        if (applyLabelDialog.isEditLabel()) {
            final EditLabelDialog editLabelDialog = new EditLabelDialog(
                getShell(),
                repository,
                applyLabelDialog.getServerItem(),
                applyLabelDialog.getRecursionType(),
                applyLabelDialog.getVersionSpec());

            editLabelDialog.setName(applyLabelDialog.getName());
            editLabelDialog.setComment(applyLabelDialog.getComment());

            if (editLabelDialog.open() != IDialogConstants.OK_ID) {
                return Status.CANCEL_STATUS;
            }

            final EditLabelResults editResults = editLabelDialog.getEditLabelResults();

            if (editResults.getLabel() == null) {
                return Status.CANCEL_STATUS;
            }

            newLabel = editResults.getLabel();
            newLabelItemSpecs = editResults.getAdds();
        }
        /* Go ahead and create the label with the given values */
        else {
            newLabel = new VersionControlLabel(
                applyLabelDialog.getName(),
                VersionControlConstants.AUTHENTICATED_USER,
                null,
                null,
                applyLabelDialog.getComment());

            newLabelItemSpecs = new LabelItemSpec[] {
                new LabelItemSpec(
                    new ItemSpec(applyLabelDialog.getServerItem(), applyLabelDialog.getRecursionType()),
                    applyLabelDialog.getVersionSpec(),
                    false)
            };
        }

        final CommandList newLabelCommand = new CommandList(
            Messages.getString("ApplyLabelTask.CreateLabelCommandText"), //$NON-NLS-1$
            Messages.getString("ApplyLabelTask.CreateLabelCommandErrorText")); //$NON-NLS-1$

        final VersionControlLabel[] deleteExisting = applyLabelDialog.getDeleteExisting();

        if (deleteExisting != null && deleteExisting.length > 0) {
            for (int i = 0; i < deleteExisting.length; i++) {
                final DeleteLabelCommand deleteCommand = new DeleteLabelCommand(repository, deleteExisting[i]);
                newLabelCommand.addCommand(deleteCommand);
            }
        }

        final CreateLabelCommand createCommand = new CreateLabelCommand(repository, newLabel, newLabelItemSpecs);
        newLabelCommand.addCommand(createCommand);

        final IStatus newLabelStatus = getCommandExecutor().execute(newLabelCommand);

        if (!newLabelStatus.isOK()) {
            return newLabelStatus;
        }

        return Status.OK_STATUS;
    }
}
