// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.HttpState;

public abstract class AuthorizationHeaderScheme implements AuthScheme {
    /** The WWW authenticate challenge header. */
    public static final String HOST_CHALLENGE_HEADER = "WWW-Authenticate";

    /** The WWW authenticate response header. */
    public static final String HOST_RESPONSE_HEADER = "Authorization";

    /** The proxy authenticate challenge header. */
    public static final String PROXY_CHALLENGE_HEADER = "Proxy-Authenticate";

    /** The proxy authenticate response header. */
    public static final String PROXY_RESPONSE_HEADER = "Proxy-Authorization";

    @Override
    public void authenticateHost(
        final AuthScope authScope,
        final Credentials credentials,
        final HttpState state,
        final HttpMethod method) throws AuthenticationException {
        final String authString = authenticate(authScope, credentials, method);

        if (authString != null) {
            method.addRequestHeader(new Header(HOST_RESPONSE_HEADER, authString, true));
        }
    }

    @Override
    public void authenticateProxy(
        final AuthScope authScope,
        final Credentials credentials,
        final HttpState state,
        final HttpMethod method) throws AuthenticationException {
        final String authString = authenticate(authScope, credentials, method);

        if (authString != null) {
            method.addRequestHeader(new Header(PROXY_RESPONSE_HEADER, authString, true));
        }
    }

    /**
     * Provides the Authorization or Proxy-Authorization headers for
     * authentication with the given {@link Credentials}.
     *
     * @param authscope
     *        The authentication scope
     * @param credentials
     *        The set of credentials to be used for authentication
     * @param method
     *        The method being authenticated
     * @throws AuthenticationException
     *         if authorization string cannot be generated due to an
     *         authentication failure
     *
     * @since 3.0
     */
    protected abstract String authenticate(
        final AuthScope authScope,
        final Credentials credentials,
        final HttpMethod method) throws AuthenticationException;
}
