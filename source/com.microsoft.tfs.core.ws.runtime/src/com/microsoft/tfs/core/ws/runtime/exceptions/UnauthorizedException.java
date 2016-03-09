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
 * authorization failure.
 */
public class UnauthorizedException extends TransportAuthException {
    private static final long serialVersionUID = 7897261585407536688L;

    private final String uri;
    private final Credentials credentials;

    public UnauthorizedException(final String uri, final Credentials credentials) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        this.uri = uri;
        this.credentials = credentials;
    }

    public String getURI() {
        return uri;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    public String getMessage() {
        String message;

        if (credentials instanceof UsernamePasswordCredentials) {
            final UsernamePasswordCredentials upCrendentials = (UsernamePasswordCredentials) credentials;
            final String messageFormat = Messages.getString("UnauthorizedException.AuthorizationFailedAsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, uri, upCrendentials.getUsername());
        } else if (credentials != null) {
            final String messageFormat =
                Messages.getString("UnauthorizedException.AuthorizationFailedWithCredentialsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, uri, credentials);
        } else {
            final String messageFormat =
                Messages.getString("UnauthorizedException.AuthorizationFailedWithNoCredentialsFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, uri);
        }

        return message;
    }
}
