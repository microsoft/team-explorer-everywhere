// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.auth;

public class AuthenticationSecurityException extends RuntimeException {
    private static final long serialVersionUID = 956101487854585943L;

    public AuthenticationSecurityException(final String message) {
        super(message);
    }

    public AuthenticationSecurityException(final Throwable cause) {
        super(cause);
    }

    public AuthenticationSecurityException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
