// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.auth;

import java.net.URI;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.WRAPCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.util.FederatedAuthenticationHelpers;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.client.SOAPRequest;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringHelpers;

/**
 * A default implementation of {@link TransportRequestHandler} that handles
 * {@link FederatedAuthException}s by getting OAuth WRAP credentials from Azure
 * ACS. The username and password in the profile supplied during construction is
 * used to get the WRAP credentials.
 *
 * @since TEE-SDK-11.0
 * @threadsafety unknown
 */
public class DefaultTransportRequestHandler implements TransportRequestHandler {
    private static final Log log = LogFactory.getLog(DefaultTransportRequestHandler.class);

    private final ConnectionInstanceData connectionInstanceData;
    private final ConfigurableHTTPClientFactory clientFactory;

    /**
     * Constructs a {@link DefaultTransportAuthHandler}.
     *
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} for this Connection
     * @param clientFactory
     *        an {@link ConfigurableHTTPClientFactory} to use to create new
     *        {@link HttpClient}s to get WRAP credentials with (must not be
     *        <code>null</code>); should be the same factory
     *        {@link TFSConnection} is using so proxy settings, etc., are used
     */
    public DefaultTransportRequestHandler(
        final ConnectionInstanceData connectionInstanceData,
        final ConfigurableHTTPClientFactory clientFactory) {
        Check.notNull(connectionInstanceData, "connectionInstanceData"); //$NON-NLS-1$
        Check.notNull(clientFactory, "clientFactory"); //$NON-NLS-1$

        this.connectionInstanceData = connectionInstanceData;
        this.clientFactory = clientFactory;
    }

    protected ConnectionInstanceData getConnectionInstanceData() {
        return this.connectionInstanceData;
    }

    protected ConfigurableHTTPClientFactory getClientFactory() {
        return this.clientFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status prepareRequest(final SOAPService service, final SOAPRequest request, final AtomicBoolean cancel) {
        return Status.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status handleException(
        final SOAPService service,
        final SOAPRequest request,
        final Exception exception,
        final AtomicBoolean cancel) {
        if (exception instanceof FederatedAuthException) {
            return handleFederatedAuthentication(service, request, (FederatedAuthException) exception, cancel);
        }

        return Status.CONTINUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Status handleSuccess(final SOAPService service, final SOAPRequest request) {
        return Status.CONTINUE;
    }

    private Status handleFederatedAuthentication(
        final SOAPService service,
        final SOAPRequest request,
        final FederatedAuthException exception,
        final AtomicBoolean cancel) {
        /*
         * Can't do federated authentication unless we have a username and
         * password
         */
        if (connectionInstanceData.getCredentials() == null
            || !(connectionInstanceData.getCredentials() instanceof UsernamePasswordCredentials)) {
            log.debug("No username in credentials, can't handle with service credentials"); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        /*
         * We previously converted these to
         * PreemptiveUsernamePasswordCredentials and they failed. There's
         * nothing more we can do.
         */
        if (connectionInstanceData.getCredentials() instanceof PreemptiveUsernamePasswordCredentials) {
            return Status.CONTINUE;
        }

        boolean basic = false;
        for (final String mechanism : exception.getMechanisms()) {
            if (!StringHelpers.isNullOrEmpty(mechanism)) {
                final String[] parts = mechanism.split(" ", 2); //$NON-NLS-1$
                if ("Basic".equalsIgnoreCase(parts[0])) //$NON-NLS-1$
                {
                    basic = true;
                    break;
                }
            }
        }

        final UsernamePasswordCredentials credentials =
            (UsernamePasswordCredentials) connectionInstanceData.getCredentials();

        if (basic) {
            log.debug("Handling FederatedAuthException with basic credentials"); //$NON-NLS-1$

            /*
             * If HTTP Basic authentication is supported, convert our
             * UsernamePasswordCredentials to
             * PreemptiveUsernamePasswordCredentials so they will be provided to
             * the server automatically. (Hosted service does not use a standard
             * 401 response so we need to provide credentials preemptively.)
             */
            final Credentials newCredentials = PreemptiveUsernamePasswordCredentials.newFrom(credentials);

            connectionInstanceData.setCredentials(newCredentials);
            service.getHTTPClient().getState().setCredentials(AuthScope.ANY, newCredentials);
        } else {
            log.debug("Handling FederatedAuthException with service credentials"); //$NON-NLS-1$

            final URI acsIssuerURI = URIUtils.newURI(exception.getFedAuthIssuer());
            final String acsAuthRealm = exception.getFedAuthRealm();

            /*
             * This call throws some informative runtime exceptions for auth/ACS
             * issues, which we want to throw from this method.
             */
            final String token = FederatedAuthenticationHelpers.getWRAPAccessToken(
                clientFactory,
                acsIssuerURI,
                acsAuthRealm,
                credentials.getUsername(),
                credentials.getPassword());

            log.trace(MessageFormat.format("Got service credentials token {0}", token)); //$NON-NLS-1$

            // This should be very rare.
            if (token == null) {
                throw new TECoreException(
                    MessageFormat.format(
                        Messages.getString("ServiceCredentialsHandler.TokenNotFoundInResponseFormat"), //$NON-NLS-1$
                        acsIssuerURI,
                        acsAuthRealm));
            }

            // Update the connection data.
            connectionInstanceData.setCredentials(new WRAPCredentials(token));

            // Apply the profile data to the existing client.
            clientFactory.configureClientCredentials(
                service.getClient(),
                service.getClient().getState(),
                connectionInstanceData);
        }

        return Status.COMPLETE;
    }
}
