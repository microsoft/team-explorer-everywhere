// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.StopCancelBuildCommand;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public class StopBuildAction extends QueuedBuildAction {

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void doRun(final IAction action) {
        final IQueuedBuild[] queuedBuilds = getSelectedQueuedBuilds();
        final boolean canStopSelection = canStopSelection(queuedBuilds);
        String message;
        if (canStopSelection) {
            message = Messages.getString("StopBuildAction.StopSelectedBuild"); //$NON-NLS-1$
        } else {
            message = Messages.getString("StopBuildAction.CancelSelectedBuilds"); //$NON-NLS-1$
        }

        if (!MessageBoxHelpers.dialogConfirmPrompt(
            getShell(),
            Messages.getString("StopBuildAction.StopBuildDialogTitle"), //$NON-NLS-1$
            message)) {
            return;
        }

        final StopCancelBuildCommand command =
            new StopCancelBuildCommand(getBuildServer(), queuedBuilds, canStopSelection);

        final IStatus status = execute(command, false);

        if (status.getSeverity() == IStatus.OK) {
            final BuildExplorer buildExplorer = BuildExplorer.getInstance();
            if (buildExplorer != null) {
                buildExplorer.refresh();
            }

            final TFSServer server =
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();

            for (int i = 0; i < queuedBuilds.length; i++) {
                server.getBuildStatusManager().removeWatchedBuild(queuedBuilds[i]);
            }

            BuildHelpers.getBuildManager().fireBuildStoppedEvent(this, null);
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

            if (queuedBuild.getStatus().containsAny(QueueStatus.QUEUED.combine(QueueStatus.POSTPONED))) {
                action.setText(Messages.getString("StopBuildAction.CancelActionText")); //$NON-NLS-1$
            } else {
                action.setText(Messages.getString("StopBuildAction.StopActionText")); //$NON-NLS-1$
            }

            if (queuedBuild.getStatus().containsAny(QueueStatus.CANCELED.combine(QueueStatus.COMPLETED))) {
                action.setEnabled(false);
                return;
            }

        }
    }

    protected boolean canStopSelection(final IQueuedBuild[] queuedBuilds) {
        if (queuedBuilds.length == 0) {
            return false;
        }
        for (int i = 0; i < queuedBuilds.length; i++) {
            if (!queuedBuilds[i].getStatus().contains(QueueStatus.IN_PROGRESS)) {
                return false;
            }
        }
        return true;
    }

}
