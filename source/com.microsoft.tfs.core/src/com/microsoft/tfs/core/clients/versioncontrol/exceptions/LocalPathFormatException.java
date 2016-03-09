// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Thrown when a local path string is invalid for use as a mapped TFS item.
 *
 * @since TEE-SDK-11.0
 */
public class LocalPathFormatException extends InputValidationException {
    public LocalPathFormatException() {
        super();
    }

    public LocalPathFormatException(final String message) {
        super(message);
    }

    public LocalPathFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public LocalPathFormatException(final Throwable cause) {
        super(cause);
    }
}
