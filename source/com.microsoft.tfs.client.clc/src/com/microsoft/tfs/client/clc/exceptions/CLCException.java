// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         Thrown when a generic CLC error occured.
 */
public final class CLCException extends Exception {
    private static final long serialVersionUID = 7386799669711888263L;

    public CLCException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CLCException(final Throwable cause) {
        super(cause);
    }

    public CLCException() {
        super();
    }

    public CLCException(final String message) {
        super(message);
    }
}
