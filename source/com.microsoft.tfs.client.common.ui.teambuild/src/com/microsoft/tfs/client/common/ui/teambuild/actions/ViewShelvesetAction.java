// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;

/**
 * View shelveset action for a queued build.
 */
public class ViewShelvesetAction extends QueuedBuildAction {

    @Override
    public void doRun(final IAction action) {
        final IQueuedBuild queuedBuild = getSelectedQueuedBuild();
        if (queuedBuild != null) {
            final TFSRepository repository =
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

            PendingChangesHelpers.showShelvesetDetails(getShell(), repository, queuedBuild.getShelvesetName());
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            final IQueuedBuild queuedBuild = getSelectedQueuedBuild();
            final BuildReason reason = queuedBuild.getReason();

            action.setEnabled(
                (reason.contains(BuildReason.CHECK_IN_SHELVESET) || reason.contains(BuildReason.VALIDATE_SHELVESET))
                    && queuedBuild.getShelvesetName() != null);
        }
    }
}
