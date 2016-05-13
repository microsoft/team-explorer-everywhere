// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config.auth;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.PreemptiveUsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.ws.runtime.client.SOAPRequest;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.client.TransportRequestHandler;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

/**
 * A default implementation of {@link TransportRequestHandler} that handles
 * {@link FederatedAuthException}s by using Basic authentication, if supported
 * by the server.
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
     *        an {@link ConfigurableHTTPClientFactory}; should be the same
     *        factory that {@link TFSConnection} is using so proxy settings,
     *        etc. can be used in derived classes.
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

        if (connectionInstanceData.getCredentials() == null) {
            log.debug("No credentials provided for Federated authentication."); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        /*
         * We previously converted these to
         * PreemptiveUsernamePasswordCredentials and they failed. There's
         * nothing more we can do.
         */
        if (connectionInstanceData.getCredentials() instanceof PreemptiveUsernamePasswordCredentials) {
            log.debug("UsernamePassword credentials provided for Federated authentication failed."); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        /*
         * Preemptive JWT credentials were provided and they failed. There's
         * nothing more we can do.
         */
        if (connectionInstanceData.getCredentials() instanceof JwtCredentials) {
            log.debug("JWT credentials provided for Federated authentication failed."); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        boolean basic = false;
        for (final String mechanism : exception.getMechanisms()) {
            if (!StringUtil.isNullOrEmpty(mechanism)) {
                final String[] parts = mechanism.split(" ", 2); //$NON-NLS-1$
                if ("Basic".equalsIgnoreCase(parts[0])) //$NON-NLS-1$
                {
                    basic = true;
                    break;
                }
            }
        }

        if (!basic) {
            log.debug("Basic authentication is not supported by the server"); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        /*
         * Can't do federated authentication unless we have a username and
         * password
         */
        if (!(connectionInstanceData.getCredentials() instanceof UsernamePasswordCredentials)) {
            log.debug(
                connectionInstanceData.getCredentials().getClass().getSimpleName()
                    + " credentials can't be used for Basic authentication"); //$NON-NLS-1$
            return Status.CONTINUE;
        }

        log.debug("Handling FederatedAuthException with basic credentials"); //$NON-NLS-1$

        final UsernamePasswordCredentials credentials =
            (UsernamePasswordCredentials) connectionInstanceData.getCredentials();

        /*
         * If HTTP Basic authentication is supported, convert our
         * UsernamePasswordCredentials to PreemptiveUsernamePasswordCredentials
         * so they will be provided to the server automatically. (Hosted service
         * does not use a standard 401 response so we need to provide
         * credentials preemptively.)
         */
        final Credentials newCredentials = PreemptiveUsernamePasswordCredentials.newFrom(credentials);

        connectionInstanceData.setCredentials(newCredentials);
        service.getHTTPClient().getState().setCredentials(AuthScope.ANY, newCredentials);

        return Status.COMPLETE;
    }
}
