// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.ConvertFolderToBranchTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.VersionControlEditor;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class ConvertToBranchAction extends TeamViewerAction {
    private final static Log log = LogFactory.getLog(ConvertToBranchAction.class);
    private TFSFolder item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        if (item != null) {
            final String user = getCurrentRepository().getConnection().getAuthenticatedIdentity().getDisplayName();
            new ConvertFolderToBranchTask(getShell(), getCurrentRepository(), item, user).run();
            if (VersionControlEditor.getCurrent() != null) {
                VersionControlEditor.getCurrent().refresh(true);
            }
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        item = (TFSFolder) adaptSelectionFirstElement(TFSFolder.class);
        action.setEnabled(canConvertToBranch(item));
    }

    private boolean canConvertToBranch(final TFSFolder item) {
        // Not allowed for TFS versions < 2010
        if (getCurrentRepository().getVersionControlClient().getServiceLevel() == WebServiceLevel.PRE_TFS_2010) {
            return false;
        }
        // Not allowed if folder is allready deleted.
        if (item.isDeleted()) {
            return false;
        }

        // Not allowed if folder has pending changes
        final PendingChange pendingChange =
            getCurrentRepository().getPendingChangeCache().getPendingChangeByServerPath(item.getFullPath());
        if (pendingChange != null) {
            return false;
        }

        // Not allowed is already a branch.
        if (item.getExtendedItem() == null) {
            return false;
        }
        if (item.getExtendedItem().isBranch()) {
            return false;
        }

        return true;
    }
}
