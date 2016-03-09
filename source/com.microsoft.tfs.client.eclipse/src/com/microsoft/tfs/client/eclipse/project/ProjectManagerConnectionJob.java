// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.Check;

class ProjectManagerConnectionJob extends Job {
    private static final Log log = LogFactory.getLog(ProjectManagerConnectionJob.class);

    private final Object lock = new Object();

    private final ProjectManagerDataProvider dataProvider;
    private final URI serverURI;

    private final boolean stayOffline;

    private Credentials credentials;

    private TFSTeamProjectCollection connection;

    protected ProjectManagerConnectionJob(
        final ProjectManagerDataProvider dataProvider,
        final URI serverURI,
        final boolean stayOffline) {
        super(MessageFormat.format(
            Messages.getString("ProjectManagerConnectionJob.ConnectingToServerFormat"), //$NON-NLS-1$
            serverURI));

        Check.notNull(dataProvider, "dataProvider"); //$NON-NLS-1$
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$

        this.dataProvider = dataProvider;
        this.serverURI = serverURI;
        this.stayOffline = stayOffline;
    }

    ProjectManagerConnectionJob(
        final ProjectManagerDataProvider dataProvider,
        final URI serverURI,
        final Credentials credentials,
        final boolean stayOffline) {
        this(dataProvider, serverURI, stayOffline);

        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        this.credentials = credentials;
    }

    protected void setCredentials(final Credentials credentials) {
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        synchronized (lock) {
            this.credentials = credentials;
        }
    }

    protected ProjectManagerDataProvider getDataProvider() {
        synchronized (lock) {
            return dataProvider;
        }
    }

    public URI getServerURI() {
        return serverURI;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        ProjectManagerDataProvider dataProvider;

        URI serverURI;
        Credentials credentials;

        /*
         * Get a copy under a lock, for visibility (and so that we needn't
         * synchronize this entire method)
         */
        synchronized (lock) {
            dataProvider = this.dataProvider;
            serverURI = this.serverURI;
            credentials = this.credentials;
        }

        final ConnectionAdvisor connectionAdvisor = dataProvider.getConnectionAdvisor();

        if (connectionAdvisor == null) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("ProjectManagerConnectionJob.AConnectionAdvisorCouldNotBeCreatedForThisConnection"), //$NON-NLS-1$
                null);
        }

        /* Connect to the TFS Server */
        TFSTeamProjectCollection connection = null;

        do {
            try {
                log.info(MessageFormat.format(
                    "Connecting to server {0}", //$NON-NLS-1$
                    serverURI));

                connection = new TFSTeamProjectCollection(serverURI, credentials, connectionAdvisor);

                if (!stayOffline) {
                    connection.authenticate();
                }
            } catch (final Exception e) {
                log.warn(MessageFormat.format("Could not connect to Team Foundation Server: {0}", e.getMessage())); //$NON-NLS-1$
                connection = null;

                if (e instanceof TFSUnauthorizedException) {
                    credentials = dataProvider.getCredentials(serverURI, credentials, e.getLocalizedMessage());

                    if (credentials == null) {
                        return Status.CANCEL_STATUS;
                    }
                } else if (e instanceof TransportRequestHandlerCanceledException) {
                    return Status.CANCEL_STATUS;
                } else {
                    /* Try to get a better connection. */
                    connection = dataProvider.promptForConnection(serverURI, credentials, e.getLocalizedMessage());

                    if (connection == null) {
                        return Status.CANCEL_STATUS;
                    }
                }
            }
        } while (connection == null);

        synchronized (lock) {
            this.connection = connection;
        }

        if (!stayOffline) {
            dataProvider.notifyConnectionEstablished(connection);
        }

        return Status.OK_STATUS;
    }

    protected void setConnection(final TFSTeamProjectCollection connection) {
        synchronized (lock) {
            this.connection = connection;
        }
    }

    public TFSTeamProjectCollection getConnection() {
        synchronized (lock) {
            return connection;
        }
    }
}
