// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

public class EndpointNotFoundException extends TransportException {
    private static final long serialVersionUID = -9039338961252302353L;

    private final int statusCode;

    /**
     * Constructs an {@link EndpointNotFoundException} that arose due to the
     * given HTTP status code.
     *
     * @param message
     *        An error message
     * @param statusCode
     *        The HTTP status code that caused this error
     */
    public EndpointNotFoundException(final String message, final int statusCode) {
        super(message);

        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
