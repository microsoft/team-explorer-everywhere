// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.framework.command.ExtensionPointAsyncObjectWaiter;
import com.microsoft.tfs.client.common.repository.RepositoryConflictException;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;

class ProjectManagerRepositoryJob extends Job {
    private final Object lock = new Object();

    private final ProjectConnectionManager connectionManager;

    private final ProjectManagerDataProvider dataProvider;
    private final WorkspaceInfo cachedWorkspace;

    private final TFSTeamProjectCollection connection;
    private final ProjectManagerConnectionJob connectionJob;

    private TFSRepository repository;

    ProjectManagerRepositoryJob(
        final ProjectConnectionManager connectionManager,
        final ProjectManagerDataProvider dataProvider,
        final WorkspaceInfo cachedWorkspace,
        final TFSTeamProjectCollection connection) {
        super(MessageFormat.format(
            Messages.getString("ProjectManagerRepositoryJob.JobNameFormat"), //$NON-NLS-1$
            ProjectConnectionManager.getConnectionName(cachedWorkspace)));

        this.connectionManager = connectionManager;
        this.dataProvider = dataProvider;
        this.cachedWorkspace = cachedWorkspace;

        this.connection = connection;
        connectionJob = null;
    }

    ProjectManagerRepositoryJob(
        final ProjectConnectionManager connectionManager,
        final ProjectManagerDataProvider dataProvider,
        final WorkspaceInfo cachedWorkspace,
        final ProjectManagerConnectionJob connectionJob) {
        super(MessageFormat.format(
            Messages.getString("ProjectManagerRepositoryJob.JobNameFormat"), //$NON-NLS-1$
            ProjectConnectionManager.getConnectionName(cachedWorkspace)));

        this.connectionManager = connectionManager;
        this.dataProvider = dataProvider;
        this.cachedWorkspace = cachedWorkspace;

        connection = null;
        this.connectionJob = connectionJob;
    }

    public final WorkspaceInfo getCachedWorkspace() {
        synchronized (lock) {
            return cachedWorkspace;
        }
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        TFSTeamProjectCollection connection;

        synchronized (lock) {
            /* Wait for the connection job to finish */
            if (connectionJob != null) {
                try {
                    new ExtensionPointAsyncObjectWaiter().joinJob(connectionJob);
                } catch (final InterruptedException e) {
                    connectionManager.connectionFailed(cachedWorkspace);

                    return new Status(
                        Status.CANCEL,
                        TFSEclipseClientPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("ProjectManagerRepositoryJob.InterruptionWhileConnectingToServer"), //$NON-NLS-1$
                        e);
                }

                if (!connectionJob.getResult().isOK()) {
                    connectionManager.connectionFailed(cachedWorkspace);

                    return connectionJob.getResult();
                }

                connection = connectionJob.getConnection();
            } else {
                connection = this.connection;
            }

            final Workspace workspace = cachedWorkspace.getWorkspace(connection);

            if (workspace == null) {
                connectionManager.connectionFailed(cachedWorkspace);

                return new Status(
                    Status.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    0,
                    MessageFormat.format(
                        Messages.getString("ProjectManagerRepositoryJob.CouldNotLocateWorkspaceOnServerFormat"), //$NON-NLS-1$
                        cachedWorkspace.getName(),
                        ProjectConnectionManager.getConnectionName(cachedWorkspace)),
                    null);
            }

            try {
                repository =
                    TFSEclipseClientPlugin.getDefault().getRepositoryManager().getOrCreateRepository(workspace);
            } catch (final RepositoryConflictException e) {
                boolean notifyConflict = true;

                /* Allow the data provider (the UI) to resolve this issue. */
                if (dataProvider.getConnectionConflictHandler().resolveRepositoryConflict()) {
                    try {
                        repository =
                            TFSEclipseClientPlugin.getDefault().getRepositoryManager().getOrCreateRepository(workspace);
                    } catch (final RepositoryConflictException f) {
                        repository = null;
                    }
                } else {
                    /* User cancelled, don't give them another popup */
                    notifyConflict = false;
                    repository = null;
                }

                /* The data provider could not resolve this issue. Fail. */
                if (repository == null) {
                    if (notifyConflict) {
                        dataProvider.getConnectionConflictHandler().notifyRepositoryConflict();
                    }

                    connectionManager.connectionFailed(cachedWorkspace);

                    return new Status(
                        Status.INFO,
                        TFSEclipseClientPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("ProjectManagerRepositoryJob.ConnectionToDifferentServerAlreadyExists"), //$NON-NLS-1$
                        e);
                }
            }
        }

        return Status.OK_STATUS;
    }

    public TFSRepository getRepository() {
        synchronized (lock) {
            return repository;
        }
    }
}
