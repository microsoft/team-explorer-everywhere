// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.memento;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * <p>
 * Thrown when an operation on a {@link Memento} cannot succeed because of a
 * document formatting error or other problem.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class MementoException extends TECoreException {
    public MementoException() {
        super();
    }

    public MementoException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MementoException(final String message) {
        super(message);
    }

    public MementoException(final Throwable cause) {
        super(cause);
    }

}
