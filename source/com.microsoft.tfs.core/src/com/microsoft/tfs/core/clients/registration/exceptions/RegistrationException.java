// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration.exceptions;

import com.microsoft.tfs.core.exceptions.TEClientException;

/**
 * Base class for registration client exceptions.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class RegistrationException extends TEClientException {
    public RegistrationException() {
        super();
    }

    public RegistrationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public RegistrationException(final String message) {
        super(message);
    }

    public RegistrationException(final Throwable cause) {
        super(cause);
    }
}
