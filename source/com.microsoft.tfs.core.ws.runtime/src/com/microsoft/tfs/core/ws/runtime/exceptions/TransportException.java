// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

/**
 * Superclass of exceptions thrown because of a problem in the HTTP soap
 * transport (authentication, unparseable XML, etc.).
 */
public class TransportException extends ProxyException {
    private static final long serialVersionUID = 9221449660880962448L;

    public TransportException() {
        super();
    }

    public TransportException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TransportException(final String message) {
        super(message);
    }

    public TransportException(final Throwable cause) {
        super(cause);
    }
}
