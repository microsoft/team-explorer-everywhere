// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security.exceptions;

public class SecurityNamespaceAlreadyExistsException extends SecurityServiceException {
    public SecurityNamespaceAlreadyExistsException() {
        super();
    }

    public SecurityNamespaceAlreadyExistsException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SecurityNamespaceAlreadyExistsException(final String message) {
        super(message);
    }

    public SecurityNamespaceAlreadyExistsException(final Throwable cause) {
        super(cause);
    }
}
