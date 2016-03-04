// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

import java.text.MessageFormat;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.WRAPCredentials;

public class WRAPAuthScheme extends AuthorizationHeaderScheme {
    private boolean complete;

    public WRAPAuthScheme() {
        complete = false;
    }

    @Override
    public boolean supportsCredentials(final Credentials credentials) {
        return credentials instanceof WRAPCredentials;
    }

    @Override
    public void processChallenge(final String challenge) throws MalformedChallengeException {
        complete = true;
    }

    @Override
    public String getSchemeName() {
        return "WRAP";
    }

    @Override
    public String getParameter(final String name) {
        return null;
    }

    @Override
    public boolean isConnectionBased() {
        return false;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    protected String authenticate(final AuthScope authScope, final Credentials credentials, final HttpMethod method)
        throws AuthenticationException {
        return MessageFormat.format("WRAP access_token=\"{0}\"", ((WRAPCredentials) credentials).getAccessToken());
    }

    @Override
    public void authenticateProxy(
        final AuthScope authScope,
        final Credentials credentials,
        final HttpState state,
        final HttpMethod method) throws AuthenticationException {
        throw new AuthenticationException("WRAP credentials are not available for proxy authentication"); //$NON-NLS-1$
    }
}
