// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security.exceptions;

public class InvalidSecurityNamespaceDescriptionException extends SecurityServiceException {
    public InvalidSecurityNamespaceDescriptionException() {
        super();
    }

    public InvalidSecurityNamespaceDescriptionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidSecurityNamespaceDescriptionException(final String message) {
        super(message);
    }

    public InvalidSecurityNamespaceDescriptionException(final Throwable cause) {
        super(cause);
    }
}
