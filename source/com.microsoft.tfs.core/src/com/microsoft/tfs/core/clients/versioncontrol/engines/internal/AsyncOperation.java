// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.util.concurrent.CompletionService;

import com.microsoft.tfs.util.Check;

/**
 * Base class for async operations run with a {@link CompletionService}.
 * Currently handles just fatal error state.
 */
public class AsyncOperation {
    /**
     * The first fatal error we encountered during the operation. Setting this
     * will stop the get operation, because the outer processing loop will check
     * for it.
     *
     * Synchronized on this.
     */
    private Throwable fatalError = null;

    public AsyncOperation() {
        super();
    }

    /**
     * @return the first fatal error set for this state, or null if no fatal
     *         error was encountered. Processing of the get operation should
     *         stop when a fatal error is returned.
     */
    public synchronized Throwable getFatalError() {
        return fatalError;
    }

    /**
     * Sets the fatal error encountered during the get operation. Once the error
     * has been set, subsequent calls will not overwrite the older error.
     *
     * @param t
     *        the error to set; if an error was previously set, this method does
     *        nothing (must not be <code>null</code>)
     */
    public synchronized void setFatalError(final Throwable t) {
        Check.notNull(t, "t"); //$NON-NLS-1$

        if (fatalError == null) {
            fatalError = t;
        }
    }
}