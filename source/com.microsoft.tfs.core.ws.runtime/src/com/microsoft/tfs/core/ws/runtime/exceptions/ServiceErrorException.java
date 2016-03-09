// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.exceptions;

/**
 * Thrown when a connection to a SOAP endpoint is denied due to a Service
 * Unavailable status code and an error message is included.
 */
public class ServiceErrorException extends TransportException {
    private static final long serialVersionUID = 1178183159739156364L;

    public ServiceErrorException(final String message) {
        super(message);
    }
}
