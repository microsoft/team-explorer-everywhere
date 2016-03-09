// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.SetWorkingFolderCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.SetWorkingFolderDialog;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class SetWorkingFolderTask extends WorkingFolderTask {
    private SetWorkingFolderDialog dialog;

    public SetWorkingFolderTask(
        final Shell shell,
        final TFSRepository repository,
        final String serverPath,
        final boolean getLatestOnSuccess) {
        super(shell, repository, serverPath, getLatestOnSuccess);
    }

    @Override
    public BaseDialog getDialog() {
        final Workspace workspace = repository.getWorkspace();
        final String purpose = Messages.getString("SetWorkingFolderTask.CreateWorkspaceMapping"); //$NON-NLS-1$
        final String localPathHint = getLocalPathHint(workspace, serverPath);

        dialog = new SetWorkingFolderDialog(getShell(), workspace, serverPath, purpose, localPathHint, true);
        return dialog;
    }

    @Override
    public TFSCommand getCommand() {
        return new SetWorkingFolderCommand(
            repository,
            serverPath,
            dialog.getLocalFolder(),
            WorkingFolderType.MAP,
            dialog.getRecursionType(),
            false);
    }

    @Override
    public void getLatest() {
        final String title = Messages.getString("SetWorkingFolderTask.SetWorkingFolder"); //$NON-NLS-1$
        getLatestForServerPath(title);
    }
}
