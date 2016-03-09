// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

/**
 * Thrown when the response from the server can't be parsed as a SOAP message
 * (invalid XML, etc.).
 */
public class InvalidServerResponseException extends TransportException {
    private static final long serialVersionUID = 4582482421504098494L;

    public InvalidServerResponseException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidServerResponseException(final String message) {
        super(message);
    }

    public InvalidServerResponseException(final Throwable cause) {
        super(cause);
    }
}
