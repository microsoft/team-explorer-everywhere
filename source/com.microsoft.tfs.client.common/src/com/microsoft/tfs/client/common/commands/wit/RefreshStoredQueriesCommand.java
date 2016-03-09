// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.wit;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class RefreshStoredQueriesCommand extends TFSConnectedCommand {
    private final TFSServer server;
    private final ProjectCollection projectCollection;

    public RefreshStoredQueriesCommand(final TFSServer server, final ProjectCollection projectCollection) {
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(projectCollection, "projectCollection"); //$NON-NLS-1$

        this.server = server;
        this.projectCollection = projectCollection;

        setConnection(server.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("RefreshStoredQueriesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("RefreshStoredQueriesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("RefreshStoredQueriesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final ProjectInfo[] activeProjects = server.getProjectCache().getActiveTeamProjects();

        for (int i = 0; i < activeProjects.length; i++) {
            final Project project = projectCollection.get(activeProjects[i].getName());
            project.getQueryHierarchy().refresh();
        }

        return Status.OK_STATUS;
    }
}
