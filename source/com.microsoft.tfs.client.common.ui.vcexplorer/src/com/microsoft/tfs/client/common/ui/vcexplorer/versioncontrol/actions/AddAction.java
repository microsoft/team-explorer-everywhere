// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.AddTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class AddAction extends TeamViewerAction {
    private String startingBrowseLocalPath;
    private String startingBrowseServerPath;

    public AddAction() {
        setName(Messages.getString("AddAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    public void doRun(final IAction action) {
        new AddTask(getShell(), getCurrentRepository(), startingBrowseLocalPath, startingBrowseServerPath).run();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (!action.isEnabled()) {
            return;
        }

        final TFSItem item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        if (item == null || !(item instanceof TFSFolder)) {
            return;
        }
        startingBrowseServerPath = item.getSourceServerPath();

        if (startingBrowseServerPath == null || ServerPath.isRootFolder(startingBrowseServerPath)) {
            action.setEnabled(false);
            return;
        }

        try {
            startingBrowseLocalPath = ((TFSFolder) item).getMappedLocalPath();

            if (startingBrowseLocalPath == null) {
                startingBrowseLocalPath = LocalPath.getPathRoot(LocalPath.getCurrentWorkingDirectory());
            }
        } catch (final PathTooLongException e) {
            action.setEnabled(false);
        }
    }
}
