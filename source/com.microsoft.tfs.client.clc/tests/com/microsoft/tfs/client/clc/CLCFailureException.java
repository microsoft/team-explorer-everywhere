// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

public final class CLCFailureException extends Exception {
    private static final long serialVersionUID = -2030171988963865290L;

    public CLCFailureException() {
        super();
    }

    public CLCFailureException(final String message) {
        super(message);
    }

    public CLCFailureException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CLCFailureException(final Throwable cause) {
        super(cause);
    }

}
