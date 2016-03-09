// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.DeleteWorkingFolderCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.UncloakFolderMappingDialog;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class UncloakWorkingFolderTask extends WorkingFolderTask {
    public UncloakWorkingFolderTask(final Shell shell, final TFSRepository repository, final String serverPath) {
        super(shell, repository, serverPath, true);
    }

    @Override
    public BaseDialog getDialog() {
        final Workspace workspace = repository.getWorkspace();
        final String localPath = getLocalPathHint(workspace, serverPath);

        return new UncloakFolderMappingDialog(getShell(), workspace, serverPath, localPath);
    }

    @Override
    public TFSCommand getCommand() {
        final WorkingFolder workingFolder = repository.getWorkspace().getExactMappingForServerPath(serverPath);
        return new DeleteWorkingFolderCommand(repository, workingFolder);
    }

    @Override
    public void getLatest() {
        final String title = Messages.getString("UncloakWorkingFolderTask.Uncloak"); //$NON-NLS-1$
        getLatestForServerPath(title);
    }
}
