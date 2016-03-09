// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.ConvertFolderToBranchCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.dialogs.vc.ConvertFolderToBranchDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;

public class ConvertFolderToBranchTask extends BaseTask {
    private final TFSItem item;
    private final String user;
    private final TFSRepository repository;

    public ConvertFolderToBranchTask(
        final Shell shell,
        final TFSRepository repository,
        final TFSItem item,
        final String user) {
        super(shell);
        this.repository = repository;
        this.item = item;
        this.user = user;
    }

    @Override
    public IStatus run() {
        final ConvertFolderToBranchDialog dialog = new ConvertFolderToBranchDialog(getShell(), item, user, repository);
        if (dialog.open() == Dialog.OK) {
            final ConvertFolderToBranchCommand cmd = new ConvertFolderToBranchCommand(
                repository,
                item.getSourceServerPath(),
                dialog.getOwner(),
                dialog.getDescription(),
                dialog.isRecursive());
            return getCommandExecutor().execute(new ResourceChangingCommand(cmd));
        }
        return Status.CANCEL_STATUS;
    }
}
