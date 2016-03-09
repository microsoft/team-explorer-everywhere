// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.TFSUser;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyUnauthorizedException;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Thrown when a web service class was used and the HTTP connection was rejected
 * because of an authorization error from the HTTP proxy. This exception is
 * thrown instead of {@link ProxyUnauthorizedException} to add extra information
 * and hide the lower-level type.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class HTTPProxyUnauthorizedException extends TECoreException {
    public HTTPProxyUnauthorizedException(final ProxyUnauthorizedException e) {
        super(buildMessage(e), e);
    }

    /**
     * Builds more detailed message strings than
     * {@link ProxyUnauthorizedException#getMessage()} because it can use the
     * {@link ProfileUtils} class to expand NT and Kerberos credentials.
     *
     * @param e
     *        the {@link ProxyUnauthorizedException} to generate a message
     *        string for (must not be <code>null</code>)
     * @return a message string describing the authorization failure
     */
    private static String buildMessage(final ProxyUnauthorizedException e) {
        Check.notNull(e, "e"); //$NON-NLS-1$

        /*
         * Display a host:port string instead of URI because getting the scheme
         * (protocol) from the exception is difficult, and we don't want to
         * mislead users about http vs. https by guessing as this will only
         * complicate troubleshooting.
         */
        final String displayHostString = e.getProxyHost() + ":" + e.getProxyPort(); //$NON-NLS-1$

        final Credentials credentials = e.getCredentials();

        if (credentials instanceof DefaultNTCredentials) {
            final String username = UserNameUtil.getCurrentUserName();
            final String domain = UserNameUtil.getCurrentUserDomain();

            if (username != null) {
                return MessageFormat.format(
                    Messages.getString("HttpProxyUnauthorizedException.AccessDeniedAuthenticatingAsFormat"), //$NON-NLS-1$
                    displayHostString,
                    new TFSUser(username, domain).toString());
            } else {
                return MessageFormat.format(
                    Messages.getString("HttpProxyUnauthorizedException.AccessDeniedAuthenticatingAsCurrentUserFormat"), //$NON-NLS-1$
                    displayHostString);
            }
        } else if (credentials instanceof UsernamePasswordCredentials) {
            final UsernamePasswordCredentials upCrendentials = (UsernamePasswordCredentials) credentials;

            return MessageFormat.format(
                Messages.getString("HttpProxyUnauthorizedException.AccessDeniedAuthenticatingAsFormat"), //$NON-NLS-1$
                displayHostString,
                upCrendentials.getUsername());
        }

        if (credentials != null) {
            return MessageFormat.format(
                Messages.getString("HttpProxyUnauthorizedException.AccessDeniedAuthenticatingUsingCredentialsFormat"), //$NON-NLS-1$
                displayHostString,
                credentials);
        }

        // No credentials available.
        return MessageFormat.format(
            Messages.getString("HttpProxyUnauthorizedException.AccessDeniedAuthenticatingNoCredentialsFormat"), //$NON-NLS-1$
            displayHostString);
    }

}
