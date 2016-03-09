// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.tasks;

/**
 * <p>
 * {@link CanceledException} is an unchecked exception thrown to indicate that a
 * long-running operation has ended prematurely because cancelation was
 * requested.
 * </p>
 */
public class CanceledException extends RuntimeException {
    /**
     * Creates a new {@link CanceledException} with no message.
     */
    public CanceledException() {
        super();
    }

    /**
     * Creates a new {@link CanceledException} with the specified message.
     *
     * @param message
     *        the exception message
     */
    public CanceledException(final String message) {
        super(message);
    }
}
