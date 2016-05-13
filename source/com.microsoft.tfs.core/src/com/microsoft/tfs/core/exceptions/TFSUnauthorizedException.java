// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.JwtCredentials;
import com.microsoft.tfs.core.util.TFSUser;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Thrown when a web service class was used and the HTTP connection was rejected
 * because of an authorization error. This exception is thrown instead of
 * {@link UnauthorizedException} to add extra information and hide the
 * lower-level type.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class TFSUnauthorizedException extends TECoreException {
    private static final Log log = LogFactory.getLog(TFSUnauthorizedException.class);

    public TFSUnauthorizedException(final UnauthorizedException e) {
        super(buildMessage(e), e);
    }

    /**
     * Builds more detailed message strings than
     * {@link UnauthorizedException#getMessage()} because it can use the
     * {@link ProfileUtils} class to expand NT and Kerberos credentials.
     *
     * @param e
     *        the {@link UnauthorizedException} to generate a message string for
     *        (must not be <code>null</code>)
     * @return a message string describing the authorization failure
     */
    private static String buildMessage(final UnauthorizedException e) {
        Check.notNull(e, "e"); //$NON-NLS-1$

        final String displayURIString = getDisplayURIString(e.getURI());

        final Credentials credentials = e.getCredentials();

        if (credentials instanceof DefaultNTCredentials) {
            final String username = UserNameUtil.getCurrentUserName();
            final String domain = UserNameUtil.getCurrentUserDomain();

            if (username != null) {
                return MessageFormat.format(
                    Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingAsFormat"), //$NON-NLS-1$
                    displayURIString,
                    new TFSUser(username, domain).toString());
            } else {
                return MessageFormat.format(
                    Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingAsCurrentUserFormat"), //$NON-NLS-1$
                    displayURIString);
            }
        } else if (credentials instanceof UsernamePasswordCredentials) {
            final UsernamePasswordCredentials upCrendentials = (UsernamePasswordCredentials) credentials;

            return MessageFormat.format(
                Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingAsFormat"), //$NON-NLS-1$
                displayURIString,
                upCrendentials.getUsername());
        } else if (credentials instanceof CookieCredentials) {
            return MessageFormat.format(
                //@formatter:off
                Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingUsingFederatedCredentialsFormat"), //$NON-NLS-1$
                //@formatter:on
                displayURIString);
        } else if (credentials instanceof JwtCredentials) {
            return MessageFormat.format(
                Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingUsingServiceCredentialsFormat"), //$NON-NLS-1$
                displayURIString);
        }

        if (credentials != null) {
            return MessageFormat.format(
                Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingUsingCredentialsFormat"), //$NON-NLS-1$
                displayURIString,
                credentials);
        }

        // No credentials available.
        return MessageFormat.format(
            Messages.getString("TFSUnauthorizedException.AccessDeniedAuthenticatingNoCredentialsFormat"), //$NON-NLS-1$
            displayURIString);
    }

    /**
     * Gets a URI string for display, which uses only the scheme, host, and port
     * from the original URI string.
     *
     * @param uriString
     *        the original URI string for which authorization failed (must not
     *        be <code>null</code>)
     * @return a URI string with the display information for the given URI
     *         string (never <code>null</code>)
     */
    private static String getDisplayURIString(final String uriString) {
        Check.notNull(uriString, "uriString"); //$NON-NLS-1$

        String displayURIString;
        try {
            final URI serverURI = new URI(uriString);
            displayURIString = new URI(
                serverURI.getScheme(),
                null,
                serverURI.getHost(),
                serverURI.getPort(),
                "/", //$NON-NLS-1$
                null,
                null).toString();
        } catch (final URISyntaxException uriSyntaxException) {
            // This should be very rare.
            log.error(
                MessageFormat.format("Could not construct message URI for '{0}', returning raw URI string", uriString), //$NON-NLS-1$
                uriSyntaxException);

            // Fall back to the original URI (with path part and all; better
            // than nothing for the user)
            displayURIString = uriString;
        }

        return displayURIString;
    }
}
