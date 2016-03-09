// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions;

/**
 * <p>
 * A base class for input validation exceptions thrown by Core. These kind of
 * exceptions are encountered during spec parsing, server or local path parsing,
 * etc.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public abstract class InputValidationException extends TECoreException {
    public InputValidationException() {
        super();
    }

    public InputValidationException(final String message) {
        super(message);
    }

    public InputValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InputValidationException(final Throwable cause) {
        super(cause);
    }
}
