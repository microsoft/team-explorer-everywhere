// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

/**
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class TeamFoundationInvalidServerNameException extends TECoreException {
    public TeamFoundationInvalidServerNameException() {
        super();
    }

    public TeamFoundationInvalidServerNameException(final String message) {
        super(message);
    }

    public TeamFoundationInvalidServerNameException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TeamFoundationInvalidServerNameException(final Throwable cause) {
        super(cause);
    }
}
