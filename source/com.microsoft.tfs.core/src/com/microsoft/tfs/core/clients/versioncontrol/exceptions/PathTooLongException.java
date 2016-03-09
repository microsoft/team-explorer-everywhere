// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Thrown when a server or local path string is too long to be used with TFS
 * version control.
 *
 * @since TEE-SDK-11.0
 */
public class PathTooLongException extends InputValidationException {
    public PathTooLongException() {
        super();
    }

    public PathTooLongException(final String message) {
        super(message);
    }

    public PathTooLongException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public PathTooLongException(final Throwable cause) {
        super(cause);
    }
}
