// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

/**
 * <p>
 * The {@link ListenerList} interface defines a collection-oriented object that
 * can hold listeners. The listeners managed by a {@link ListenerList} are
 * untyped, meaning that no type restrictions are imposed by the
 * {@link ListenerList} container on its members.
 * </p>
 *
 * <p>
 * This interface leaves many details unspecified and up to implementations. In
 * particular, the following should be specified by implementations:
 * <ul>
 * <li>How listeners are compared when adding, removing, or searching for
 * listeners</li>
 * <li>What the valid types of listeners are, if there are any type restrictions
 * </li>
 * <li>The thread safety and concurrency model of the implementation
 * </ul>
 * </p>
 *
 * <p>
 * For a default implementation, see {@link StandardListenerList}.
 * </p>
 *
 * @see StandardListenerList
 */
public interface ListenerList {
    /**
     * Adds a new listener to this {@link ListenerList}. If this
     * {@link ListenerList} already contains a listener that is considered equal
     * to the given listener, no modifications are made to this
     * {@link ListenerList}.
     *
     * @param listener
     *        a listener object to add (must not be <code>null</code>)
     * @return <code>true</code> if this collection was modified as a result of
     *         this operation
     */
    public boolean addListener(Object listener);

    /**
     * Removes a listener from this {@link ListenerList}. If this
     * {@link ListenerList} does not contain a listener that is considered equal
     * to the given listener, no modifications are made to this
     * {@link ListenerList}.
     *
     * @param listener
     *        a listener object to remove (must not be <code>null</code>)
     * @return <code>true</code> if this collection was modified as a result of
     *         this operation
     */
    public boolean removeListener(Object listener);

    /**
     * Determines if this {@link ListenerList} currently contains a listener
     * considered equal to the specified listener.
     *
     * @param listener
     *        a listener to test (must not be <code>null</code>)
     * @return <code>true</code> if this {@link ListenerList} contains a
     *         listener considered equal to specified listener
     */
    public boolean containsListener(Object listener);

    /**
     * Empties this {@link ListenerList} of all contained listeners.
     *
     * @return <code>true</code> if this collection was modified as a result of
     *         this operation
     */
    public boolean clear();

    /**
     * @return the number of contained listeners currently held in this
     *         {@link ListenerList}.
     */
    public int size();

    /**
     * @return all of the currently held listeners in this {@link ListenerList}
     *         as an {@link Object} array
     */
    public Object[] getListeners();

    /**
     * @param a
     *        determines the runtime type of the return array (must not be
     *        <code>null</code>)
     *
     * @see <code>java.util.Collection.toArray(Object[])</code>
     *
     * @return all of the currently held listeners in this {@link ListenerList}
     *         as an array - the runtime type of the array is determined by the
     *         argument
     */
    public Object[] getListeners(Object a[]);

    /**
     * <p>
     * Invokes the specified {@link ListenerRunnable} callback once for each
     * listener currently held in this {@link ListenerList}.
     * </p>
     *
     * <p>
     * Calling this method is fully equivalent to calling:
     *
     * <pre>
     * foreachListener(DefaultExceptionHandler.INSTANCE, runnable);
     * </pre>
     *
     * </p>
     *
     * @param runnable
     *        the callback object to use (must not be <code>null</code>)
     */
    public void foreachListener(ListenerRunnable runnable);

    /**
     * <p>
     * Invokes the specified {@link ListenerRunnable} callback once for each
     * listener currently held in this {@link ListenerList}.
     * </p>
     *
     * <p>
     * Any exceptions thrown by the given {@link ListenerRunnable} will be
     * handled by the given {@link ListenerExceptionHandler}.
     * </p>
     *
     * @param exceptionHandler
     *        exception handler callback to receive exceptions thrown by the
     *        {@link ListenerRunnable} (must not be <code>null</code>)
     * @param runnable
     *        the callback object to use (must not be <code>null</code>)
     */
    public void foreachListener(ListenerExceptionHandler exceptionHandler, ListenerRunnable runnable);
}
