// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions.internal;

import com.microsoft.tfs.util.tasks.CanceledException;

/**
 * <p>
 * Used inside core to signal a user-initiated cancellation of the long-running
 * process. This exception is never thrown from core to users outside it. It is
 * only used to chain cancellation up through layers of core, and is a checked
 * exception so each layer is sure to clean up its resources.
 * </p>
 * <p>
 * Core methods which must signal cancellation to external users should throw an
 * unchecked {@link CanceledException} instead of this one.
 * </p>
 *
 * @threadsafety thread-safe
 */
public final class CoreCancelException extends Exception {
    public CoreCancelException() {
        super();
    }

    public CoreCancelException(final String message) {
        super(message);
    }

    public CoreCancelException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public CoreCancelException(final Throwable cause) {
        super(cause);
    }
}
