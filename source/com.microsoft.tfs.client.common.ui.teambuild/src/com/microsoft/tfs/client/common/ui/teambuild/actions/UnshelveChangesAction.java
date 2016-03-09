// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnshelveBuiltShelvesetTask;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;

public class UnshelveChangesAction extends BuildDetailAction {
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        final IBuildDetail buildDetail = getSelectedBuildDetail();
        if (buildDetail != null) {
            final TFSRepository repository =
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

            final UnshelveBuiltShelvesetTask unshelveTask =
                new UnshelveBuiltShelvesetTask(getShell(), repository, buildDetail, true);

            unshelveTask.run();
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            final IBuildDetail detail = getSelectedBuildDetail();

            action.setEnabled(detail.isBuildFinished()
                && (detail.getStatus().contains(BuildStatus.FAILED)
                    || detail.getStatus().contains(BuildStatus.PARTIALLY_SUCCEEDED))
                && detail.getReason().contains(BuildReason.CHECK_IN_SHELVESET));
        }
    }
}
