// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.config;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Thrown when information provided to a configuration class was invalid, or the
 * configuration could not be completed.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class IllegalConfigurationException extends TECoreException {
    public IllegalConfigurationException(final String message) {
        super(message);
    }

    public IllegalConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
