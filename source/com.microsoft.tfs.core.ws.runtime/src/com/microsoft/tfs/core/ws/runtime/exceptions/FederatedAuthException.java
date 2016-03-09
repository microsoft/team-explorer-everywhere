// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.util.Check;

/**
 * Thrown when a connection to a SOAP endpoint is denied because of an
 * authorization failure and the server has provided a URL for federated
 * authentication.
 *
 * @threadsafety unknown
 */
@SuppressWarnings("serial")
public class FederatedAuthException extends TransportAuthException {
    private final String serverURI;
    private final String authenticationURL;
    private final String fedAuthIssuer;
    private final String fedAuthRealm;
    private final String[] mechanisms;
    private final Credentials credentials;
    private final String serverErorMessage;

    public FederatedAuthException(
        final String serverURI,
        final String authenticationURL,
        final String fedAuthIssuer,
        final String fedAuthRealm,
        final String[] mechanisms,
        final Credentials credentials,
        final String serverErorMessage) {
        super(serverURI);

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(authenticationURL, "authenticationURL"); //$NON-NLS-1$
        Check.notNull(fedAuthIssuer, "fedAuthIssuer"); //$NON-NLS-1$
        Check.notNull(fedAuthRealm, "fedAuthRealm"); //$NON-NLS-1$
        Check.notNull(mechanisms, "mechanisms"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.authenticationURL = authenticationURL;
        this.fedAuthIssuer = fedAuthIssuer;
        this.fedAuthRealm = fedAuthRealm;
        this.mechanisms = mechanisms;
        this.credentials = credentials;
        this.serverErorMessage = serverErorMessage;
    }

    public String getServerURI() {
        return serverURI;
    }

    public String getAuthenticationURL() {
        return authenticationURL;
    }

    public String getFedAuthIssuer() {
        return this.fedAuthIssuer;
    }

    public String getFedAuthRealm() {
        return fedAuthRealm;
    }

    public String[] getMechanisms() {
        return mechanisms;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMessage() {
        return serverErorMessage;
    }

}
