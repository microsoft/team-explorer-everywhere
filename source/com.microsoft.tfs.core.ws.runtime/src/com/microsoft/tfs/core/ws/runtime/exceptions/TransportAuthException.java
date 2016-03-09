// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

/**
 * Ancestor of all transport-layer authentication/authorization exceptions.
 * These may be resolvable via {@link TransportAuthHandler} so a web request can
 * be automatically retried, otherwise they're thrown.
 */
public class TransportAuthException extends TransportException {
    public TransportAuthException() {
        super();
    }

    public TransportAuthException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransportAuthException(final String message) {
        super(message);
    }

    public TransportAuthException(final Throwable cause) {
        super(cause);
    }
}
