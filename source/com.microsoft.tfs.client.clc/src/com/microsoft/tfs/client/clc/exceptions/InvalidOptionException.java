// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         Thrown when an option is invalid for a command.
 */
public final class InvalidOptionException extends ArgumentException {
    static final long serialVersionUID = 2801064369527930628L;

    public InvalidOptionException() {
        super();
    }

    public InvalidOptionException(final String message) {
        super(message);
    }
}
