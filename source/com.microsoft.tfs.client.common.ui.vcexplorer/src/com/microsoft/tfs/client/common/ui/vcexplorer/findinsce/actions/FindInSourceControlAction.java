// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.FindInSourceControlQuery;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.findinsce.tasks.FindInSourceControlTask;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.TeamViewerAction;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class FindInSourceControlAction extends TeamViewerAction {
    private String path = ServerPath.ROOT;

    public FindInSourceControlAction() {
        setName(Messages.getString("FindInSourceControlAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        final TFSItem item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        path = (item != null) ? item.getFullPath() : ServerPath.ROOT;
    }

    @Override
    public void doRun(final IAction action) {
        final FindInSourceControlTask findTask = new FindInSourceControlTask(getShell(), getCurrentRepository());
        findTask.setQuery(new FindInSourceControlQuery(path, null));
        findTask.run();
    }
}
