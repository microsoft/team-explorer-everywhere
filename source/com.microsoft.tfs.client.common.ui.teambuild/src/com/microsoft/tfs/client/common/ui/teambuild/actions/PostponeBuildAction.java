// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teambuild.commands.PostponeResumeCommand;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public class PostponeBuildAction extends QueuedBuildAction {

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void doRun(final IAction action) {
        final IQueuedBuild[] selectedBuilds = getSelectedQueuedBuilds();
        final boolean resume = selectedBuilds[0].getStatus().contains(QueueStatus.POSTPONED);

        final PostponeResumeCommand command = new PostponeResumeCommand(getBuildServer(), selectedBuilds, resume);

        final IStatus status = execute(command, false);
        if (status.getSeverity() == IStatus.OK) {
            final BuildExplorer buildExplorer = BuildExplorer.getInstance();
            if (buildExplorer != null) {
                buildExplorer.refresh();
            }

            BuildHelpers.getBuildManager().fireBuildPostponedOrResumedEvent(this, null);
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.teambuild.actions.QueuedBuildAction#onSelectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            // Check the queueStatus
            final IQueuedBuild queuedBuild = getSelectedQueuedBuild();
            if (queuedBuild == null || queuedBuild.getStatus() == null) {
                action.setEnabled(false);
                return;
            }
            if (queuedBuild.getStatus().containsAny(QueueStatus.combine(new QueueStatus[] {
                QueueStatus.CANCELED,
                QueueStatus.COMPLETED,
                QueueStatus.IN_PROGRESS
            }))) {
                action.setEnabled(false);
                return;
            }
            action.setChecked(queuedBuild.getStatus().contains(QueueStatus.POSTPONED));
        }
    }
}
