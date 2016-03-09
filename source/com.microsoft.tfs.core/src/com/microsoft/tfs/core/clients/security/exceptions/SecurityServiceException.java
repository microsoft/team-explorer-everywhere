// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security.exceptions;

import com.microsoft.tfs.core.exceptions.TECoreException;

public class SecurityServiceException extends TECoreException {
    public SecurityServiceException() {
        super();
    }

    public SecurityServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SecurityServiceException(final String message) {
        super(message);
    }

    public SecurityServiceException(final Throwable cause) {
        super(cause);
    }
}
