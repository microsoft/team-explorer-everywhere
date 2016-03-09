// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public class PinBuildAction extends QueuedBuildAction {
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void doRun(final IAction action) {
        final IQueuedBuild[] queuedBuilds = getSelectedQueuedBuilds();

        final TFSServer server = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getServer(
            getBuildServer().getConnection());

        if (server == null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("PinBuildAction.NotConnectedErrorTitle"), //$NON-NLS-1$
                Messages.getString("PinBuildAction.NotConnectedErrorMessage")); //$NON-NLS-1$
            return;
        }

        for (int i = 0; i < queuedBuilds.length; i++) {
            server.getBuildStatusManager().addWatchedBuild(queuedBuilds[i]);
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

            if (queuedBuild.getStatus().containsAny(QueueStatus.CANCELED.combine(QueueStatus.COMPLETED))) {
                action.setEnabled(false);
                return;
            }
        }
    }
}
