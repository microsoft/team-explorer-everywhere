// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

/**
 *         Thrown when a command is missing required arguments or they are of
 *         the wrong format.
 */
public final class InvalidFreeArgumentException extends ArgumentException {
    static final long serialVersionUID = 4299629254302449850L;

    public InvalidFreeArgumentException() {
        super();
    }

    public InvalidFreeArgumentException(final String message) {
        super(message);
    }
}
