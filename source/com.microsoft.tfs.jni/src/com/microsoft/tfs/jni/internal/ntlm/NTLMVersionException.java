// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.ntlm;

public class NTLMVersionException extends RuntimeException {
    private static final long serialVersionUID = -8068040318048854261L;

    public NTLMVersionException(final String message) {
        super(message);
    }
}
