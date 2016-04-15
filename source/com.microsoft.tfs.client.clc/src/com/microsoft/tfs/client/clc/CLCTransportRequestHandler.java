// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.prompt.Prompt;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.console.input.Input;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.config.auth.DefaultTransportRequestHandler;
import com.microsoft.tfs.core.config.httpclient.ConfigurableHTTPClientFactory;
import com.microsoft.tfs.core.config.httpclient.DefaultHTTPClientFactory;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.ws.runtime.client.SOAPRequest;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;

/**
 * A {@link TransportAuthHandler} for the command-line client, which can prompt
 * for authentication information.
 *
 * @threadsafety unknown
 */
public class CLCTransportRequestHandler extends DefaultTransportRequestHandler {
    private static final Log log = LogFactory.getLog(CLCTransportRequestHandler.class);

    private final Display display;
    private final Input input;
    private final boolean loginOptionSpecified;
    private final boolean persistCredentials;
    private final boolean usePersistanceCredentialsManager;
    private final boolean prompt;

    /**
     * When the user provides new credentials to retry a failed request, the
     * code to persist the new credentials is saved here to be run by
     * {@link #handleSuccess(SOAPService, SOAPRequest)}.
     */
    private Runnable handleSuccessRunnable;

    /**
     * Creates a {@link CLCTransportAuthHandler} that updates the given profile
     * with new auth data using the given {@link DefaultHTTPClientFactory}'s
     * credential update methods.
     *
     * @param connectionInstanceData
     *        the {@link ConnectionInstanceData} for this Connection (must not
     *        be <code>null</code>
     * @param clientFactory
     *        the {@link DefaultHTTPClientFactory} to use to apply credentials
     *        from the profile to the {@link HttpClient} (must not be
     *        <code>null</code>)
     * @param loginOptionSpecified
     *        pass <code>true</code> if the user specified the login option on
     *        the command line (prints a message instead of prompting),
     *        <code>false</code> if the option was not set
     * @param persistCredentials
     *        if <code>true</code> new credentials gathered by prompting are
     *        saved immediately, if <code>false</code> they are not saved
     *
     * @param usePersistanceCredentialsManager
     *        if <code>true</code> PersistanceCredentialsManager may be used
     *
     * @param prompt
     *        if <code>true</code> allow prompting
     */
    public CLCTransportRequestHandler(
        final ConnectionInstanceData connectionInstanceData,
        final ConfigurableHTTPClientFactory clientFactory,
        final Display display,
        final Input input,
        final boolean loginOptionSpecified,
        final boolean persistCredentials,
        final boolean usePersistanceCredentialsManager,
        final boolean prompt) {
        super(connectionInstanceData, clientFactory);

        this.display = display;
        this.input = input;
        this.loginOptionSpecified = loginOptionSpecified;
        this.persistCredentials = persistCredentials;
        this.usePersistanceCredentialsManager = usePersistanceCredentialsManager;
        this.prompt = prompt;
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
        /*
         * Super method handles FederatedAuthException with service credentials
         * if credentials were specified. Try that first.
         */
        if (super.handleException(service, request, exception, cancel) == Status.COMPLETE) {
            log.debug("DefaultTransportAuthHandler handled auth exception for us"); //$NON-NLS-1$
            return Status.COMPLETE;
        }

        log.debug("DefaultTransportAuthHandler did not handle auth exception"); //$NON-NLS-1$

        /*
         * If the user specified credentials with an option, don't prompt.
         */
        if (loginOptionSpecified) {
            return Status.CONTINUE;
        }

        /*
         * Currently the CLC has no good way to handle federated auth
         * exceptions, because those require web browsers.
         */
        if (exception instanceof FederatedAuthException || exception instanceof UnauthorizedException) {
            return handleUsernamePasswordAuthentication(service, request, exception, cancel);
        }

        return Status.CONTINUE;
    }

    private Status handleUsernamePasswordAuthentication(
        final SOAPService service,
        final SOAPRequest request,
        final Exception exception,
        final AtomicBoolean cancel) {
        // Ensure the runnable from a previous try doesn't get run
        handleSuccessRunnable = null;

        final ConnectionInstanceData connectionInstanceData = getConnectionInstanceData();

        if (exception instanceof FederatedAuthException) {
            /*
             * If this is a federated exception, then display a message if and
             * only if there were specified credentials that failed. If we're
             * holding DefaultNTCredentials, then those simply won't work
             * because there's no Kerberos on hosted. No need to bother the user
             * with a message about that.
             */

            final Credentials attemptedCredentials = ((FederatedAuthException) exception).getCredentials();

            if (attemptedCredentials != null && attemptedCredentials instanceof UsernamePasswordCredentials) {
                display.printLine(
                    MessageFormat.format(
                        Messages.getString("CLCTransportRequestHandler.FederatedAuthFailedFormat"), //$NON-NLS-1$
                        ((FederatedAuthException) exception).getServerURI(),
                        ((UsernamePasswordCredentials) attemptedCredentials).getUsername()));
            }
        } else if (exception instanceof UnauthorizedException) {
            /*
             * If this is an unauthorized exception then our credentials failed
             * against an on-premises server. Convert the exception to a
             * TFSUnauthorizedException because it's got good error reporting.
             */
            final TFSUnauthorizedException detailedException =
                new TFSUnauthorizedException((UnauthorizedException) exception);

            display.printLine(detailedException.getLocalizedMessage());
        }

        // Prompt for all fields regardless of old credentials
        UsernamePasswordCredentials newCredentials = null;
        if (Prompt.interactiveLoginAllowed() && ServerURIUtils.isHosted(service.getEndpoint()) && prompt) {
            /*
             * If we are making request against hosted services, attempt to
             * recreate the oauth2 token or pat.
             */
            newCredentials = Prompt.getCredentialsInteractively(service.getEndpoint(), display, persistCredentials);
        }

        if (newCredentials == null && prompt) {
            // If there is no creds for team services, then prompt (could be
            // domain creds for TFS, or basic auth for VSTS)
            newCredentials = Prompt.getCredentials(display, input, null, null);
        }

        if (newCredentials != null) {
            // Apply the credentials data to the existing client.
            connectionInstanceData.setCredentials(newCredentials);

            getClientFactory().configureClientCredentials(
                service.getClient(),
                service.getClient().getState(),
                connectionInstanceData);

            if (persistCredentials) {
                final UsernamePasswordCredentials credsToBeCached = newCredentials;
                // Postpone saving the credentials until the request succeeds
                handleSuccessRunnable = new Runnable() {
                    @Override
                    public void run() {
                        CredentialsManagerFactory.getCredentialsManager(
                            Command.CLC_PERSISTENCE_PROVIDER,
                            usePersistanceCredentialsManager).setCredentials(
                                new CachedCredentials(connectionInstanceData.getServerURI(), credsToBeCached));
                    }
                };
            }

            return Status.COMPLETE;
        }

        /*
         * The user didn't supply enough information when prompted, so let other
         * handlers have a try.
         */
        return Status.CONTINUE;
    }

    @Override
    public Status handleSuccess(final SOAPService service, final SOAPRequest request) {
        if (handleSuccessRunnable != null) {
            handleSuccessRunnable.run();
        }

        handleSuccessRunnable = null;

        return Status.CONTINUE;
    }
}
