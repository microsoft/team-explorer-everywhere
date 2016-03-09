// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

/**
 * <p>
 * This interface defines a callback object that is called by
 * {@link ListenerList} when iterating over listeners. By using this callback
 * mechanism, client code does not have to write the iteration or error handling
 * boilerplate code associated with listener iteration.
 * </p>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * ListenerList listenerList = ...
 * listenerList.foreachListener(new ListenerRunnable() {
 *     public boolean run(Object object) throws Exception {
 *        MyListenerType listener = (MyListenerType) object;
 *        listener.doSomething();
 *        return true;
 *     }
 * });
 * </pre>
 *
 * </p>
 */
public interface ListenerRunnable {
    /**
     * Process a listener contained in a {@link ListenerList}.
     *
     * @param listener
     *        the listener object being processed (never <code>null</code>)
     * @return <code>true</code> to continue the iteration, or
     *         <code>false</code> to stop the iteration
     */
    public boolean run(Object listener) throws Exception;
}
