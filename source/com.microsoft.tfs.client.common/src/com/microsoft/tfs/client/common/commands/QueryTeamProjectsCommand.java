// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Command to query the Classification Service and obtain information about all
 * the Team Projects.
 */
public class QueryTeamProjectsCommand extends TFSConnectedCommand {
    private final TFSTeamProjectCollection connection;

    private ProjectInfo[] projects = null;

    public QueryTeamProjectsCommand(final TFSTeamProjectCollection connection) {
        this.connection = connection;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryTeamProjectsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryTeamProjectsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryTeamProjectsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(
            Messages.getString("QueryTeamProjectsCommand.ProgressMonitorTitle"), //$NON-NLS-1$
            IProgressMonitor.UNKNOWN);

        try {
            final CommonStructureClient cssClient =
                (CommonStructureClient) connection.getClient(CommonStructureClient.class);
            projects = cssClient.listProjects();
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public ProjectInfo[] getProjects() {
        return projects;
    }
}