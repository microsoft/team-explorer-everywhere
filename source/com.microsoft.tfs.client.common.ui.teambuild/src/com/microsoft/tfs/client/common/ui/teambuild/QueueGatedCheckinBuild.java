// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.commands.IQueueGatedCheckinBuild;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.teambuild.commands.QueueBuildCommand;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;

/**
 * Implements the extension point to queue a gated check-in build.
 */
public class QueueGatedCheckinBuild implements IQueueGatedCheckinBuild {
    private IQueuedBuild queuedBuild;

    /**
     * Queue a build for a gated check-in for the specified build definition.
     */
    @Override
    public boolean queueBuild(
        final Shell shell,
        final IBuildDefinition buildDefinition,
        final String shelvesetName,
        final String gatedCheckinTicket) {
        return queueBuild(shell, buildDefinition, buildDefinition.getURI(), shelvesetName, gatedCheckinTicket);
    }

    /**
     * Queue a build for a gated check-in for the specified build definition.
     */
    @Override
    public boolean queueBuild(
        final Shell shell,
        final IBuildDefinition buildDefinition,
        final String buildDefinitionURI,
        final String shelvesetName,
        final String gatedCheckinTicket) {
        final IBuildRequest request = buildDefinition.createBuildRequest();
        request.setReason(BuildReason.CHECK_IN_SHELVESET);
        request.setShelvesetName(shelvesetName);
        request.setGatedCheckInTicket(gatedCheckinTicket);
        request.setBuildDefinitionURI(buildDefinitionURI);

        final QueueBuildCommand command = new QueueBuildCommand(request);
        final ICommandExecutor commandExecutor = UICommandExecutorFactory.newUICommandExecutor(shell);

        final IStatus status = commandExecutor.execute(command);

        // This is not executed async because we need to get the Queued
        // Build back to then display it in the build explorer.
        if (status.getSeverity() == IStatus.OK) {
            // Get the queued build and open build explorer.
            queuedBuild = command.getQueuedBuild();

            // Add this build to the watched builds.
            final TFSTeamProjectCollection collection = buildDefinition.getBuildServer().getConnection();
            final TFSServer server =
                TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getServer(collection);
            server.getBuildStatusManager().addWatchedBuild(queuedBuild);

            return true;
        }

        return false;
    }

    @Override
    public IQueuedBuild getQueuedBuild() {
        return queuedBuild;
    }
}
