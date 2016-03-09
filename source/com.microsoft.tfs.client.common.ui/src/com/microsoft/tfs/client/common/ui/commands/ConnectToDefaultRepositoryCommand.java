// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.credentials.CredentialsHelper;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;

/**
 * Connects to the "default" (last used) TFS workspace for a given profile.
 *
 * A composite of the {@link ConnectToProjectCollectionCommand} and
 * {@link GetDefaultRepositoryCommand}s.
 *
 * @threadsafety unknown
 */
public class ConnectToDefaultRepositoryCommand extends TFSCommand implements ConnectCommand {
    private final ConnectToConfigurationServerCommand connectCommand;

    private TFSServer server;
    private TFSRepository repository;

    public ConnectToDefaultRepositoryCommand(final URI serverURI, final Credentials credentials) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        this.connectCommand = new ConnectToConfigurationServerCommand(serverURI, credentials);

        setCancellable(true);
    }

    @Override
    public String getName() {
        return connectCommand.getName();
    }

    @Override
    public String getErrorDescription() {
        return connectCommand.getErrorDescription();
    }

    @Override
    public String getLoggingDescription() {
        return connectCommand.getLoggingDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), 5);

        /* Connect to the server */
        final SubProgressMonitor connectMonitor = new SubProgressMonitor(progressMonitor, 1);

        try {
            final IStatus connectStatus = new CommandExecutor(connectMonitor).execute(connectCommand);

            if (!connectStatus.isOK()) {
                return connectStatus;
            }
        } finally {
            connectMonitor.done();
        }

        checkForCancellation(progressMonitor);

        final TFSConnection configurationServer = connectCommand.getConnection();
        final TFSTeamProjectCollection connection;

        final SubProgressMonitor projectCollectionMonitor = new SubProgressMonitor(progressMonitor, 1);

        try {
            final GetDefaultProjectCollectionCommand projectCollectionCommand =
                new GetDefaultProjectCollectionCommand(configurationServer);

            final IStatus projectCollectionStatus =
                new CommandExecutor(projectCollectionMonitor).execute(projectCollectionCommand);

            if (!projectCollectionStatus.isOK()) {
                return projectCollectionStatus;
            }

            connection = projectCollectionCommand.getConnection();
        } finally {
            projectCollectionMonitor.done();
        }

        checkForCancellation(progressMonitor);

        /* Create PAT for EGit access to VSTS if needed */
        final SubProgressMonitor patMonitor = new SubProgressMonitor(progressMonitor, 1);
        patMonitor.beginTask(getName(), 1);

        try {
            if (connection.isHosted()) {
                if (CredentialsHelper.hasAccountCodeAccessToken(connection)) {
                    if (!CredentialsHelper.isAccountCodeAccessTokenValid(connection)) {
                        CredentialsHelper.refreshAccountCodeAccessToken(connection);
                    }
                } else if (!CredentialsHelper.hasAlternateCredentials(connection)) {
                    CredentialsHelper.createAccountCodeAccessToken(connection);
                }
            }
        } finally {
            patMonitor.done();
        }

        checkForCancellation(progressMonitor);

        /* Build a TFSServer from this connection. */
        final SubProgressMonitor serverMonitor = new SubProgressMonitor(progressMonitor, 1);
        serverMonitor.beginTask(getName(), 1);

        try {
            server = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getOrCreateServer(
                connection);
        } finally {
            serverMonitor.done();
        }

        checkForCancellation(progressMonitor);

        /* Connect to the last-used Workspace, build a TFSRepository. */
        final SubProgressMonitor repositoryMonitor = new SubProgressMonitor(progressMonitor, 1);

        try {
            final GetDefaultWorkspaceCommand workspaceCommand = new GetDefaultWorkspaceCommand(connection);

            final IStatus workspaceStatus = new CommandExecutor(repositoryMonitor).execute(workspaceCommand);

            if (workspaceStatus == null || workspaceStatus.isOK()) {
                repository = new TFSRepository(workspaceCommand.getWorkspace());
            }

            return workspaceStatus;
        } finally {
            repositoryMonitor.done();
        }
    }

    @Override
    public TFSConnection getConnection() {
        if (server == null) {
            return null;
        }

        return server.getConnection();
    }

    public TFSServer getServer() {
        return server;
    }

    public TFSRepository getRepository() {
        return repository;
    }
}
