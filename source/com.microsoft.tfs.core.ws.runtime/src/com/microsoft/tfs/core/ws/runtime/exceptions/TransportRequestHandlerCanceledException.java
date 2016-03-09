// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

/**
 * Thrown by {@link TransportAuthHandler}s to cancel the
 * authentication/authorization process
 *
 * @threadsafety unknown
 */
public class TransportRequestHandlerCanceledException extends RuntimeException {
    public TransportRequestHandlerCanceledException() {
        super("User canceled transport request handling"); //$NON-NLS-1$
    }

    public TransportRequestHandlerCanceledException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransportRequestHandlerCanceledException(final String message) {
        super(message);
    }

    public TransportRequestHandlerCanceledException(final Throwable cause) {
        super(cause);
    }
}
