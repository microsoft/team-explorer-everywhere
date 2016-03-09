// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.server.cache.buildstatus.BuildStatusManager;
import com.microsoft.tfs.client.common.server.cache.project.ServerProjectCache;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.client.common.wit.QueryDocumentService;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.util.Check;

final public class TFSServer {
    private final Object refreshLock = new Object();
    private volatile boolean isRefreshing = false;

    private final TFSTeamProjectCollection connection;

    private final ServerProjectCache projectCache;
    private final QueryDocumentService queryDocumentService;
    private final BuildStatusManager buildStatusManager;

    /**
     * This is package private and should only be created by the
     * {@link ServerManager}.
     *
     * Creates a new {@link TFSServer} for the given
     * {@link TFSTeamProjectCollection}. If this connection has been established
     * then the server data will be refreshed.
     *
     * @param workspace
     *        the TFS {@link Workspace} (not <code>null</code>)
     */
    TFSServer(final TFSTeamProjectCollection connection) {
        this(connection, ConnectionHelper.isConnected(connection));
    }

    /**
     * Creates a new {@link TFSServer} for the given
     * {@link TFSTeamProjectCollection}. If this connection has been established
     * then the server data will be refreshed.
     *
     * @param workspace
     *        the TFS {@link Workspace} (not <code>null</code>)
     * @param refresh
     *        <code>true</code> to refresh caches, <code>false</code> otherwise
     */
    private TFSServer(final TFSTeamProjectCollection connection, final boolean refresh) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;

        projectCache = new ServerProjectCache(connection);
        queryDocumentService = new QueryDocumentService(connection);
        buildStatusManager = new BuildStatusManager(connection);

        if (refresh) {
            refresh(true);
        }
    }

    public String getName() {
        return connection.getName();
    }

    public TFSTeamProjectCollection getConnection() {
        return connection;
    }

    public ServerProjectCache getProjectCache() {
        return projectCache;
    }

    public QueryDocumentService getQueryDocumentService() {
        return queryDocumentService;
    }

    public BuildStatusManager getBuildStatusManager() {
        return buildStatusManager;
    }

    public void refresh() {
        refresh(false);
    }

    public void refresh(final boolean async) {
        if (!async) {
            refresh(new NullProgressMonitor());
        } else if (!isRefreshing) {
            final String messageFormat = Messages.getString("TFSServer.ProgressTitleFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, connection.getName());
            final Job refreshJob = new Job(message) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    refresh(monitor);
                    return Status.OK_STATUS;
                }
            };
            refreshJob.schedule();
        }
    }

    private void refresh(final IProgressMonitor progressMonitor) {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        synchronized (refreshLock) {
            isRefreshing = true;

            try {
                String messageFormat = Messages.getString("TFSServer.ProgressStatusFormat"); //$NON-NLS-1$
                String message = MessageFormat.format(messageFormat, connection.getName());
                progressMonitor.beginTask(message, 5);

                messageFormat = Messages.getString("TFSServer.ProgressStepRefreshRegistrationFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, connection.getName());
                progressMonitor.subTask(message);
                connection.getRegistrationClient().refresh(true);
                progressMonitor.worked(1);

                messageFormat = Messages.getString("TFSServer.ProgressStepRefreshProjectFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, connection.getName());
                progressMonitor.subTask(message);
                projectCache.refresh();
                progressMonitor.worked(1);

                messageFormat = Messages.getString("TFSServer.ProgressStepRefreshingMetadataFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, connection.getName());
                progressMonitor.subTask(message);
                connection.getWorkItemClient().refreshCache();
                progressMonitor.worked(1);

                /* Ensure the server supported features cache is primed */
                messageFormat = Messages.getString("TFSServer.ProgressStepRefreshingFeaturesFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, connection.getName());
                progressMonitor.subTask(message);
                connection.getVersionControlClient().getServerSupportedFeatures();
                progressMonitor.worked(1);

                messageFormat = Messages.getString("TFSServer.ProgressStepRefreshingBuildStatusFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, connection.getName());
                progressMonitor.subTask(message);
                buildStatusManager.refresh();
                progressMonitor.worked(1);

                progressMonitor.done();
            } finally {
                isRefreshing = false;
            }
        }
    }

    /**
     * Closes this TFSServer connection. Should be called whenever disconnecting
     * from a server.
     */
    public void close() {
        buildStatusManager.stop();
    }

    public boolean connectionsEquivalent(final TFSServer server) {
        if (server == null) {
            return false;
        }

        return connectionsEquivalent(server.getConnection());
    }

    public boolean connectionsEquivalent(final TFSTeamProjectCollection connection) {
        if (connection == null) {
            return false;
        }

        return ServerURIUtils.equals(this.getConnection().getBaseURI(), connection.getBaseURI());
    }
}
