// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.ws.runtime.Messages;
import com.microsoft.tfs.util.Check;

/**
 * Thrown when a connection to a SOAP endpoint is denied because of an
 * authorization failure to an HTTP proxy.
 */
public class ProxyUnauthorizedException extends TransportAuthException {
    private static final long serialVersionUID = 8688156619317277334L;

    private final String proxyHost;
    private final int proxyPort;
    private final Credentials credentials;

    public ProxyUnauthorizedException(final String proxyHost, final int proxyPort, final Credentials credentials) {
        Check.notNull(proxyHost, "proxyHost"); //$NON-NLS-1$

        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.credentials = credentials;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public String getMessage() {
        String message;

        if (credentials instanceof UsernamePasswordCredentials) {
            final UsernamePasswordCredentials upCrendentials = (UsernamePasswordCredentials) credentials;
            final String messageFormat = Messages.getString("ProxyUnauthorizedException.AuthorizationFailedAsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, getHostForMessage(), upCrendentials.getUsername());
        } else if (credentials != null) {
            final String messageFormat =
                Messages.getString("ProxyUnauthorizedException.AuthorizationFailedWithCredentialsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, getHostForMessage(), credentials);
        } else {
            final String messageFormat =
                Messages.getString("ProxyUnauthorizedException.AuthorizationFailedWithNoCredentialsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, getHostForMessage());
        }

        return message;
    }

    private String getHostForMessage() {
        if (proxyPort != 80) {
            return MessageFormat.format("{0}:{1}", proxyHost, Integer.toString(proxyPort)); //$NON-NLS-1$
        } else {
            return proxyHost;
        }
    }
}
