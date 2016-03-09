// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.AddCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.AddFilesWizard;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.util.Check;

public class AddTask extends BaseTask {
    private final TFSRepository repository;
    private final String startingBrowseLocalPath;
    private final String startingBrowseServerPath;

    public AddTask(final Shell shell, final TFSRepository repository, final String startingBrowseLocalPath) {
        this(shell, repository, startingBrowseLocalPath, null);
    }

    public AddTask(
        final Shell shell,
        final TFSRepository repository,
        final String startingBrowseLocalPath,
        final String startingBrowseServerPath) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.startingBrowseLocalPath = startingBrowseLocalPath;
        this.startingBrowseServerPath = startingBrowseServerPath;
    }

    @Override
    public IStatus run() {
        final AddFilesWizard wizard = new AddFilesWizard(startingBrowseLocalPath, startingBrowseServerPath);

        final WizardDialog dialog = new WizardDialog(getShell(), wizard);

        if (dialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        final String[] addPaths = wizard.getLocalPaths();
        if (addPaths.length == 0) {
            return Status.OK_STATUS;
        }

        final AddCommand command = new AddCommand(repository, addPaths);

        return getCommandExecutor().execute(command);
    }
}
