// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.serverstatus.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for server status client exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class ServerStatusException extends TEClientException {
    public ServerStatusException() {
        super();
    }

    public ServerStatusException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ServerStatusException(final String message) {
        super(message);
    }

    public ServerStatusException(final Throwable cause) {
        super(cause);
    }

}
