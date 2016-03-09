// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.catalog.TeamProjectCollectionInfo;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.clients.framework.configuration.entities.ProjectCollectionEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.TeamFoundationServerEntity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class QueryProjectCollectionsCommand extends TFSConnectedCommand {
    private final static Log log = LogFactory.getLog(QueryProjectCollectionsCommand.class);

    private final TFSConfigurationServer connection;

    private TeamProjectCollectionInfo[] collections = null;

    public QueryProjectCollectionsCommand(final TFSConfigurationServer connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryProjectCollectionsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryProjectCollectionsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryProjectCollectionsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(
            Messages.getString("QueryProjectCollectionsCommand.ProgressMonitorText"), //$NON-NLS-1$
            IProgressMonitor.UNKNOWN);

        try {
            final TeamFoundationServerEntity teamFoundationServer = connection.getTeamFoundationServerEntity(true);

            if (teamFoundationServer != null) {
                final ProjectCollectionEntity[] projectCollections = teamFoundationServer.getProjectCollections();

                if (projectCollections != null) {
                    collections = new TeamProjectCollectionInfo[projectCollections.length];

                    for (int i = 0; i < projectCollections.length; i++) {
                        collections[i] = new TeamProjectCollectionInfo(
                            projectCollections[i].getInstanceID(),
                            projectCollections[i].getDisplayName(),
                            projectCollections[i].getDescription());
                    }
                }
            }
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    public TeamProjectCollectionInfo[] getProjectCollections() {
        return collections;
    }
}
