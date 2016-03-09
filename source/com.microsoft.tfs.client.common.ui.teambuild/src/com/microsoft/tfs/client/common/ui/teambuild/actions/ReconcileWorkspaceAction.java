// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.tasks.vc.ReconcileGatedCheckinTask;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;

public class ReconcileWorkspaceAction extends BuildDetailAction {
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        final IBuildDetail buildDetail = getSelectedBuildDetail();

        if (buildDetail != null) {
            final ReconcileGatedCheckinTask task = new ReconcileGatedCheckinTask(
                getShell(),
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository(),
                buildDetail);

            if (task.run().isOK()) {
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer().getBuildStatusManager().removeWatchedBuild(
                    buildDetail);
            }
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            final IBuildDetail detail = getSelectedBuildDetail();

            action.setEnabled(detail.isBuildFinished()
                && (detail.getStatus().contains(BuildStatus.SUCCEEDED)
                    || detail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED))
                && detail.getReason().contains(BuildReason.CHECK_IN_SHELVESET)
                && InformationNodeConverters.getChangesetID(detail.getInformation()) > 0);
        }
    }
}
