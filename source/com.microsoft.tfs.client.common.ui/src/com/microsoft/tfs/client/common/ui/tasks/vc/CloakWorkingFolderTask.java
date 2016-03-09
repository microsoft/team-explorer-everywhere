// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.SetWorkingFolderCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.CloakFolderMappingDialog;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolderType;
import com.microsoft.tfs.util.Check;

public class CloakWorkingFolderTask extends WorkingFolderTask {
    final private String localPath;

    public CloakWorkingFolderTask(
        final Shell shell,
        final TFSRepository repository,
        final String serverPath,
        final String localPath) {
        super(shell, repository, serverPath, true);

        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        this.localPath = localPath;
    }

    @Override
    public BaseDialog getDialog() {
        return new CloakFolderMappingDialog(getShell(), repository.getWorkspace(), serverPath, localPath);
    }

    @Override
    public TFSCommand getCommand() {
        return new SetWorkingFolderCommand(
            repository,
            serverPath,
            localPath,
            WorkingFolderType.CLOAK,
            RecursionType.FULL,
            false);
    }

    @Override
    public void getLatest() {
        getLatestForLocalPath(localPath, true);
    }
}
