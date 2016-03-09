// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.util.TeamContextCache;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.util.Check;

public class QueryActiveTeamProjectsCommand extends TFSConnectedCommand {
    private final TFSTeamProjectCollection connection;
    private final ProjectInfo[] projects;

    private ProjectInfo[] activeProjects;

    public QueryActiveTeamProjectsCommand(final TFSTeamProjectCollection connection, final ProjectInfo[] allProjects) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(allProjects, "allProjects"); //$NON-NLS-1$

        this.connection = connection;
        projects = allProjects;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryActiveTeamProjectsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryActiveTeamProjectsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /* Let the project cache handle this */
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        activeProjects = TeamContextCache.getInstance().getActiveTeamProjects(connection, projects);
        return Status.OK_STATUS;
    }

    public ProjectInfo[] getActiveProjects() {
        return activeProjects;
    }
}
