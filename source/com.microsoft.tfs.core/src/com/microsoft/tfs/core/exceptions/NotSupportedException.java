// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

/**
 * Exception thrown when an operation is not supported against the current
 * version of the server.
 *
 * @since TEE-SDK-10.1
 */
public class NotSupportedException extends TECoreException {

    public NotSupportedException() {
        super();
    }

    public NotSupportedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public NotSupportedException(final String message) {
        super(message);
    }

    public NotSupportedException(final Throwable cause) {
        super(cause);
    }

}
