// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

import java.text.MessageFormat;

import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.core.httpclient.JwtCredentials;

public class JwtAuthScheme extends AuthorizationHeaderScheme {
    private boolean complete;

    public JwtAuthScheme() {
        complete = false;
    }

    @Override
    public boolean supportsCredentials(final Credentials credentials) {
        return credentials instanceof JwtCredentials;
    }

    @Override
    public void processChallenge(final String challenge) throws MalformedChallengeException {
        complete = true;
    }

    @Override
    public String getSchemeName() {
        return "Bearer";
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
        return MessageFormat.format("Bearer {0}", ((JwtCredentials) credentials).getAccessToken());
    }
}
