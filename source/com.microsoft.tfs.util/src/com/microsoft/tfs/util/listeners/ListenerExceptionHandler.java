// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

/**
 * <p>
 * A callback interface used by some {@link ListenerList} methods. Instances of
 * {@link ListenerExceptionHandler} are used to handle exceptions thrown by a
 * {@link ListenerRunnable}.
 * </p>
 * <p>
 * For a default implementation, see {@link DefaultExceptionHandler}.
 * </p>
 *
 * @see ListenerList
 * @see DefaultExceptionHandler
 */
public interface ListenerExceptionHandler {
    /**
     * Handle an exception thrown by a {@link ListenerRunnable}.
     *
     * @param listener
     *        the listener object being processed
     * @param listenerRunnable
     *        the {@link ListenerRunnable} that threw the exception
     * @param listenerList
     *        the {@link ListenerList} that contains the listener
     * @param exception
     *        the {@link Throwable} that was thrown
     * @return <code>true</code> to continue processing listeners with the
     *         {@link ListenerRunnable}, or <code>false</code> to stop the
     *         iteration
     */
    public boolean onException(
        Object listener,
        ListenerRunnable listenerRunnable,
        ListenerList listenerList,
        Throwable exception);
}
