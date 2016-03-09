// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.negotiate;

import com.microsoft.tfs.jni.AuthenticationEngine.AuthenticationException;

public class NegotiateException extends AuthenticationException {
    private static final long serialVersionUID = 3765975235233394597L;

    public NegotiateException(final String message) {
        super(message);
    }

    public NegotiateException(final String message, final Throwable t) {
        super(message, t);
    }
}