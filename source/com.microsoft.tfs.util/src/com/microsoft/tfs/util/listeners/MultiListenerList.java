// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link MultiListenerList} provides support for managing multiple lists of
 * listeners. Many classes support more that one listener type. Instead of
 * managing a separate {@link ListenerList} instance for each type of listener,
 * a single {@link MultiListenerList} instance can be used to manage all of the
 * listener types.
 * </p>
 *
 * <p>
 * {@link MultiListenerList} manages multiple, independent {@link ListenerList}
 * s. Each independent list is identified by a {@link ListenerCategory}. Most
 * operations done against a {@link MultiListenerList} must supply a
 * {@link ListenerCategory} as a argument to select the proper sub-list of
 * listeners to use. It is recommended that clients of this class create
 * {@link ListenerCategory} instances as private static final constants in their
 * class.
 * </p>
 *
 * <p>
 * In addition, {@link MultiListenerList} provides the same kind of "single
 * listener" facade that {@link SingleListenerFacade} does. By calling
 * {@link #getListener(ListenerCategory)} you can obtain a "single listener"
 * object for the specified category that multicasts to all of the real
 * listeners for that category.
 * </p>
 *
 * @see ListenerList
 * @see ListenerCategory
 * @see SingleListenerFacade
 */
public class MultiListenerList {
    private final Map<ListenerCategory, SingleListenerFacade> map =
        new HashMap<ListenerCategory, SingleListenerFacade>();
    private final Object lock = new Object();

    /**
     * Adds a listener to this {@link MultiListenerList} in the specified
     * {@link ListenerCategory}.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     * @param category
     *        the {@link ListenerCategory} to add the listener in (must not be
     *        <code>null</code>)
     * @return <code>true</code> if the {@link ListenerList} specified by the
     *         category was modified as a result of this operation
     */
    public boolean addListener(final Object listener, final ListenerCategory category) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        Check.notNull(category, "category"); //$NON-NLS-1$

        final SingleListenerFacade facade = getSingleListenerFacade(category, true);

        return facade.addListener(listener);
    }

    /**
     * Removes a listener from this {@link MultiListenerList} in the specified
     * {@link ListenerCategory}.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     * @param category
     *        the {@link ListenerCategory} to remove the listener from (must not
     *        be <code>null</code>)
     * @return <code>true</code> if the {@link ListenerList} specified by the
     *         category was modified as a result of this operation
     */
    public boolean removeListener(final Object listener, final ListenerCategory category) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        Check.notNull(category, "category"); //$NON-NLS-1$

        final SingleListenerFacade facade = getSingleListenerFacade(category, false);

        if (facade == null) {
            return false;
        }

        return facade.removeListener(listener);
    }

    /**
     * Obtains the {@link ListenerList} for the given {@link ListenerCategory}.
     *
     * @param category
     *        the {@link ListenerCategory} to get the {@link ListenerList} for
     *        (must not be <code>null</code>)
     * @return a {@link ListenerList} (never <code>null</code>)
     */
    public ListenerList getListenerList(final ListenerCategory category) {
        return getListenerList(category, true);
    }

    /**
     * Obtains the {@link ListenerList} for the given {@link ListenerCategory}.
     * This method can be used to avoid creating the {@link ListenerList} for
     * the category if it does not already exist.
     *
     * @param category
     *        the {@link ListenerCategory} to get the {@link ListenerList} for
     *        (must not be <code>null</code>)
     * @param create
     *        <code>true</code> to create the {@link ListenerList} if it does
     *        not already exist
     * @return a {@link ListenerList}, or <code>null</code> if
     *         <code>create</code> is <code>false</code> and no
     *         {@link ListenerList} exists for the specified category
     */
    public ListenerList getListenerList(final ListenerCategory category, final boolean create) {
        final SingleListenerFacade facade = getSingleListenerFacade(category, create);

        if (facade == null) {
            return null;
        }

        return facade.getListenerList();
    }

    /**
     * Gets a "single listener" facade for the specified
     * {@link ListenerCategory}. This method is the equivalent of the
     * {@link SingleListenerFacade} class operation.
     *
     * @param category
     *        the {@link ListenerCategory} to get a listener for (must not be
     *        <code>null</code>)
     * @return a listener object for the given category (never <code>null</code>
     *         )
     */
    public Object getListener(final ListenerCategory category) {
        return getListener(category, false);
    }

    /**
     * Gets a "single listener" facade for the specified
     * {@link ListenerCategory}. This method is the equivalent of the
     * {@link SingleListenerFacade} class operation. This method can be used to
     * avoid creating the listener proxy if there is no {@link ListenerList} for
     * the specified category or if no listeners have been registered for that
     * category.
     *
     * @param category
     *        the {@link ListenerCategory} to get a listener for (must not be
     *        <code>null</code>)
     * @param allowNull
     *        <code>true</code> to avoid creating a listener proxy if it is not
     *        neccessary to
     * @return a listener object for the given category, or <code>null</code> if
     *         <code>allowNull</code> was <code>true</code> and no listener
     *         proxy needed to be created
     */
    public Object getListener(final ListenerCategory category, final boolean allowNull) {
        final SingleListenerFacade facade = getSingleListenerFacade(category, !allowNull);

        if (facade == null) {
            return null;
        }

        return facade.getListener(allowNull);
    }

    /**
     * Removes all listeners for a given category.
     *
     * @param category
     *        the {@link ListenerCategory} to remove listeners for (must not be
     *        <code>null</code>)
     * @return <code>true</code> if this collection was modified as a result of
     *         this operation
     */
    public boolean clear(final ListenerCategory category) {
        final SingleListenerFacade facade = getSingleListenerFacade(category, false);

        if (facade != null) {
            return facade.getListenerList().clear();
        }

        return false;
    }

    /**
     * Removes all listeners.
     *
     * @return <code>true</code> if this collection was modified as a result of
     *         this operation
     */
    public boolean clear() {
        boolean modified = false;

        synchronized (lock) {
            for (final SingleListenerFacade facade : map.values()) {
                if (facade.getListenerList().clear()) {
                    modified = true;
                }
            }

            map.clear();
        }

        return modified;
    }

    private SingleListenerFacade getSingleListenerFacade(final ListenerCategory category, final boolean create) {
        synchronized (lock) {
            SingleListenerFacade facade = map.get(category);
            if (facade == null && create) {
                facade = new SingleListenerFacade(category.getListenerInterface());
                map.put(category, facade);
            }
            return facade;
        }
    }
}
