// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for {@link IdentityManagementService} exceptions.
 *
 * @since TEE-SDK-11.0
 */
public class IdentityManagementException extends TEClientException {
    public IdentityManagementException() {
        super();
    }

    public IdentityManagementException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public IdentityManagementException(final String message) {
        super(message);
    }

    public IdentityManagementException(final Throwable cause) {
        super(cause);
    }
}
