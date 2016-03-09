// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         Thrown when an option encounters an error parsing the values it
 *         requires.
 *
 *         This class is thread-safe.
 */
public final class InvalidOptionValueException extends ArgumentException {
    static final long serialVersionUID = -2995312545756728598L;

    public InvalidOptionValueException() {
        super();
    }

    public InvalidOptionValueException(final String message) {
        super(message);
    }
}
