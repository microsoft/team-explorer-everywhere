// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.exceptions.InputValidationException;

/**
 * Thrown when a TFS repository server path format is invalid.
 *
 * @since TEE-SDK-10.1
 */
public class ServerPathFormatException extends InputValidationException {
    public ServerPathFormatException() {
        super();
    }

    public ServerPathFormatException(final String message) {
        super(message);
    }

    public ServerPathFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ServerPathFormatException(final Throwable cause) {
        super(cause);
    }
}
