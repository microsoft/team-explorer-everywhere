// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.config.ConnectionAdvisor;
import com.microsoft.tfs.core.exceptions.TFSAccessException;
import com.microsoft.tfs.core.exceptions.TFSFederatedAuthException;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.TransportRequestHandlerCanceledException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;

/**
 * Attempt a connection to TFS in a TFS Framework (i.e. TFS2010 and up
 * compatible way) If this fails, then attempt to connect to a Pre-Framework
 * server (i.e. TFS 2008 or earlier)
 */
public class ConnectToConfigurationServerCommand extends TFSCommand implements ConnectCommand {
    private static final Log log = LogFactory.getLog(ConnectToConfigurationServerCommand.class);

    private final URI serverURI;
    private Credentials credentials;

    private TFSConnection connection;

    public ConnectToConfigurationServerCommand(final URI serverURI, final Credentials credentials) {
        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(credentials, "credentials"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.credentials = credentials;

        setCancellable(true);
        addExceptionHandler(new ConnectCommandExceptionHandler());
    }

    @Override
    public String getName() {
        return (Messages.getString("ConnectToConfigurationServerCommand.CommandName")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ConnectToConfigurationServerCommand.CommandErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("ConnectToConfigurationServerCommand.CommandName", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    public URI getServerURI() {
        return serverURI;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final ConnectionAdvisor connectionAdvisor = new UIClientConnectionAdvisor();

        String message;
        final String username = getUsername();

        if (username != null) {
            final String messageFormat =
                Messages.getString("ConnectToConfigurationServerCommand.ConnectingToServerAsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, serverURI.toString(), username);
        } else {
            final String messageFormat =
                Messages.getString("ConnectToConfigurationServerCommand.ConnectionToServerFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, serverURI.toString());
        }

        log.info(message);

        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        /*
         * URI fallbacks:
         * 
         * In previous TEE versions we did the following:First we try the given
         * URI as a configuration server URI, then remove the trailing "/tfs/"
         * and retry as a configuration server. Then we try as a project
         * collection, then remove the trailing "/tfs/" and retry as a project
         * collection. Fail ONLY on unauthorized errors.
         * 
         * In 14.0.4, let's allow at this point arbitrary TFS URIs, i.e.
         * http(s)://host[:port][/path][/collection][/project]... We need to
         * connect to a configuration server here, which has to be found either
         * with http(s)://host[:port]/path or with http(s)://host[:port] URI (if
         * we're connecting to a hosted service or the path is not configured on
         * on-premises TFS server). So we do not care of the entire URI path
         * string, but rather its first item.
         * 
         */

        final List<ConnectionURIAndType> connectionTypes = new ArrayList<ConnectionURIAndType>();

        final String pathCandidate = getFirstPathItem(serverURI.getPath());
        if (ServerURIUtils.isHosted(serverURI) || StringUtil.isNullOrEmpty(pathCandidate)) {
            // We have either a VSTS URI or a URI of TFS server with an empty
            // path configured. We should be able to find the configuration
            // server at the URI without any path.
            connectionTypes.add(
                new ConnectionURIAndType(
                    TFSConfigurationServer.class,
                    URIUtils.newURI(serverURI.getScheme(), serverURI.getAuthority())));
        } else {
            // We have an on-premises TFS server URI. The first path item might
            // be a server path or a collection name if the server is configured
            // with an empty path. The latter is much more rare situation, so
            // we'll try configuration server first and collection after that.

            final URI uriCandidate =
                URIUtils.newURI(serverURI.getScheme(), serverURI.getAuthority(), '/' + pathCandidate);

            connectionTypes.add(new ConnectionURIAndType(TFSConfigurationServer.class, uriCandidate));
            connectionTypes.add(new ConnectionURIAndType(TFSTeamProjectCollection.class, uriCandidate));
        }

        /*
         * Try to connect to this URL as a TFS2010 Configuration Server
         */

        connection = null;

        while (connectionTypes.size() > 0) {
            final ConnectionURIAndType connectionData = connectionTypes.remove(0);

            try {
                if (TFSConfigurationServer.class.equals(connectionData.getType())) {
                    connection = new TFSConfigurationServer(connectionData.getURI(), credentials, connectionAdvisor);
                } else if (TFSTeamProjectCollection.class.equals(connectionData.getType())) {
                    connection = new TFSTeamProjectCollection(connectionData.getURI(), credentials, connectionAdvisor);
                } else {
                    throw new RuntimeException("Unknown connection type"); //$NON-NLS-1$
                }

                connection.authenticate();
                break;
            } catch (final Exception e) {
                /*
                 * We may have updated the credentials (using the federated
                 * authentication data).
                 */
                if (connection != null) {
                    credentials = connection.getCredentials();

                    connection.close();
                    connection = null;
                }

                /*
                 * Always exit on unauthorized exceptions. For anything else, we
                 * assume that this is a project collection URL (either a
                 * TFS2010 project collection URL or a TFS2008 server URL.)
                 * Continue to retry.
                 *
                 * Do not assume that 2008 servers will return a 404 in this
                 * case. It is possible that a 500 is returned on non-TFS 2008
                 * requests. Visual Studio handles this "properly" - in that it
                 * downgrades in ANY non-authorization failure problem.
                 *
                 * Also exit on permissions problems returned from the server.
                 */
                if (e instanceof TFSUnauthorizedException
                    || e instanceof TFSAccessException
                    || e instanceof TFSFederatedAuthException) {
                    throw e;
                } else if (e instanceof TransportRequestHandlerCanceledException) {
                    return Status.CANCEL_STATUS;
                }

                /* Exit if there are no more formats to retry with. */
                if (connectionTypes.size() == 0) {
                    throw e;
                }

                final String messageFormat = "Server not found at {0}"; //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, e.getLocalizedMessage());
                log.info(message);
            }
        }

        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final Map<String, String> properties = new HashMap<String, String>();
        ClientTelemetryHelper.addContextProperties(properties, connection);
        ClientTelemetryHelper.sendCommandFinishedEvent(this, Status.OK_STATUS, properties);

        if (connection instanceof TFSTeamProjectCollection) {
            /*
             * Try to get the TFS2010+ Configuration Server from this
             * TFSTeamProjectCollection.
             */

            final TFSTeamProjectCollection collection = (TFSTeamProjectCollection) connection;

            if (collection.getConfigurationServer() != null) {
                connection = collection.getConfigurationServer();
            }
        }

        return Status.OK_STATUS;
    }

    @Override
    public TFSConnection getConnection() {
        return connection;
    }

    private String getUsername() {
        if (credentials != null && credentials instanceof UsernamePasswordCredentials) {
            return ((UsernamePasswordCredentials) credentials).getUsername();
        }

        return null;
    }

    private static String getFirstPathItem(final String path) {
        if (StringUtil.isNullOrEmpty(path) || path.equalsIgnoreCase("/")) { //$NON-NLS-1$
            return null;
        }

        final String[] pathItems = path.split("/"); //$NON-NLS-1$

        for (final String pathItem : pathItems) {
            if (!StringUtil.isNullOrEmpty(pathItem)) {
                return pathItem;
            }
        }

        return null;
    }

    private static final class ConnectionURIAndType {
        private final Class<? extends TFSConnection> type;
        private final URI uri;

        public ConnectionURIAndType(final Class<? extends TFSConnection> type, final URI uri) {
            Check.notNull(type, "type"); //$NON-NLS-1$
            Check.notNull(uri, "uri"); //$NON-NLS-1$

            this.type = type;
            this.uri = uri;
        }

        public Class<? extends TFSConnection> getType() {
            return type;
        }

        public URI getURI() {
            return uri;
        }
    }
}
