// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.util.LocaleUtil;

public class DeleteBuildAgentCommand extends TFSCommand {
    private final IBuildServer buildServer;
    private final IBuildAgent buildAgent;

    /**
     * @param buildServer
     * @param buildAgent
     */
    public DeleteBuildAgentCommand(final IBuildServer buildServer, final IBuildAgent buildAgent) {
        this.buildServer = buildServer;
        this.buildAgent = buildAgent;
    }

    @Override
    public String getName() {
        return (Messages.getString("DeleteBuildAgentCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("DeleteBuildAgentCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("DeleteBuildAgentCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    /**
     * @see com.microsoft.tfs.client.common.framework.command.Command#doRun(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        buildServer.deleteBuildAgents(new IBuildAgent[] {
            buildAgent
        });

        // Repopulate cache
        TeamBuildCache.getInstance(buildServer, buildAgent.getTeamProject()).getBuildControllers(true);

        return Status.OK_STATUS;
    }

}
