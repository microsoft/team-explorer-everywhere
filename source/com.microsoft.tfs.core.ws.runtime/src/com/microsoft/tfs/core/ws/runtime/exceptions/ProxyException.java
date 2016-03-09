// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

/**
 * Superclass of all exceptions in this package. This class lets the generated
 * proxy method implementations and beans throw a single type of exception,
 * which allows its handling to be simplified.
 *
 * Proxy means "web service proxy", not "HTTP proxy".
 */
public abstract class ProxyException extends RuntimeException {
    public ProxyException() {
        super();
    }

    public ProxyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ProxyException(final String message) {
        super(message);
    }

    public ProxyException(final Throwable cause) {
        super(cause);
    }
}
