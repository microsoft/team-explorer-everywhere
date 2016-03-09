// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.HttpState;

public class CookieAuthScheme implements AuthScheme {
    @Override
    public boolean supportsCredentials(final Credentials credentials) {
        return (credentials instanceof CookieCredentials);
    }

    @Override
    public void processChallenge(final String challenge) throws MalformedChallengeException {
        throw new MalformedChallengeException("Cookie authentication is not challenge/response"); //$NON-NLS-1$
    }

    @Override
    public String getSchemeName() {
        return "cookie"; //$NON-NLS-1$
    }

    @Override
    public String getParameter(final String name) {
        return null;
    }

    /*
     * Cookie-based authentication scheme is request-based, not connection
     * based.
     */
    @Override
    public boolean isConnectionBased() {
        return false;
    }

    /*
     * Cookie-based authentication is not challenge/response, thus is
     * by-definition complete.
     */
    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public void authenticateHost(
        final AuthScope authscope,
        final Credentials credentials,
        final HttpState state,
        final HttpMethod method) throws AuthenticationException {
        if (!(credentials instanceof CookieCredentials)) {
            throw new AuthenticationException("Invalid credentials for cookie authentication");
        }

        state.addCookies(((CookieCredentials) credentials).getCookies());
    }

    @Override
    public void authenticateProxy(
        final AuthScope authScope,
        final Credentials credentials,
        final HttpState state,
        final HttpMethod method) throws AuthenticationException {
        if (!(credentials instanceof CookieCredentials)) {
            throw new AuthenticationException("Invalid credentials for cookie authentication");
        }

        state.addCookies(((CookieCredentials) credentials).getCookies());
    }
}
