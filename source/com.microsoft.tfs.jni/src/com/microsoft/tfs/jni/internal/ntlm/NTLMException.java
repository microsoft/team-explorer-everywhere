// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.ntlm;

import com.microsoft.tfs.jni.AuthenticationEngine.AuthenticationException;

public class NTLMException extends AuthenticationException {
    private static final long serialVersionUID = -6506242514228250181L;

    public NTLMException(final String message) {
        super(message);
    }

    public NTLMException(final String message, final Throwable t) {
        super(message, t);
    }
}