// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.DeleteWorkingFolderCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.RemoveFolderMappingDialog;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;

public class RemoveWorkingFolderTask extends WorkingFolderTask {
    final private WorkingFolder workingFolder;

    public RemoveWorkingFolderTask(final Shell shell, final TFSRepository repository, final String serverPath) {
        super(shell, repository, serverPath, true);
        this.workingFolder = repository.getWorkspace().getExactMappingForServerPath(serverPath);
    }

    @Override
    public BaseDialog getDialog() {
        return new RemoveFolderMappingDialog(
            getShell(),
            repository.getWorkspace(),
            serverPath,
            workingFolder.getLocalItem());
    }

    @Override
    public TFSCommand getCommand() {
        return new DeleteWorkingFolderCommand(repository, workingFolder);
    }

    @Override
    public void getLatest() {
        getLatestForLocalPath(workingFolder.getLocalItem(), true);
    }
}
