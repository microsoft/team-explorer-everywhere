// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.vc.RollbackTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.util.StringUtil;

public class RollbackAction extends TeamViewerAction {
    private TFSItem item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final TFSRepository repository = getCurrentRepository();
        final String itemPath = item.getSourceServerPath();
        final RollbackTask rollbackTask = new RollbackTask(getShell(), repository, itemPath);
        rollbackTask.run();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (!action.isEnabled()) {
            return;
        }

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);
        action.setEnabled(canRollback(item));
    }

    private boolean canRollback(final TFSItem item) {
        if (item == null) {
            return false;
        }

        final TFSRepository repository = getCurrentRepository();
        // Not allowed for TFS versions < 2010
        if (repository.getVersionControlClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            return false;
        }

        if (item.isDeleted() && !StringUtil.isNullOrEmpty(item.getMappedLocalPath())) {
            return true;
        }

        if (StringUtil.isNullOrEmpty(item.getLocalPath())) {
            return false;
        }

        if (PendingChangesHelpers.isPendingAdd(repository, item.getLocalPath())) {
            return false;
        }

        final String itemPath = item.getSourceServerPath();

        if (StringUtil.isNullOrEmpty(itemPath)) {
            return false;
        }

        if (!repository.getWorkspace().isServerPathMapped(itemPath)) {
            return false;
        }

        return true;
    }
}
