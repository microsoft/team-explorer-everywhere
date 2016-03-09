// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link ListenerCategory} identifies a category of listeners managed by a
 * {@link MultiListenerList}.
 * </p>
 *
 * <p>
 * A {@link ListenerCategory} contains two pieces of data. The first is a
 * <b>selector</b> {@link Object} that is used to differentiate categories in a
 * {@link MultiListenerList}. The selector objects are compared against each
 * other for equality. The second is a listener interface {@link Class} that is
 * used when generating "single listener" proxies. For many uses of
 * {@link MultiListenerList}, these two pieces of data will be the same - the
 * listener interface. However, it is occasionally useful to have them be
 * different when a {@link MultiListenerList} manages separate categories of
 * listeners that share one listener interface.
 * </p>
 *
 * @see MultiListenerList
 */
public class ListenerCategory {
    private final Object selector;
    private final Class listenerInterface;

    /**
     * Creates a new {@link ListenerCategory} with the specified listener
     * interface class. The selector of this category is also the specified
     * listener interface class.
     *
     * @param listenerInterface
     *        the listener interface for this {@link ListenerCategory} (must not
     *        be <code>null</code>)
     */
    public ListenerCategory(final Class listenerInterface) {
        this(listenerInterface, listenerInterface);
    }

    /**
     * Creates a new {@link ListenerCategory} with the specified listener
     * interface class and selector integer.
     *
     * @param selector
     *        the selector integer for this {@link ListenerCategory} - will be
     *        converted into an {@link Integer} to use as a selector
     *        {@link Object}
     * @param listenerInterface
     *        the listener interface for this {@link ListenerCategory} (must not
     *        be <code>null</code>)
     */
    public ListenerCategory(final int selector, final Class listenerInterface) {
        this(new Integer(selector), listenerInterface);
    }

    /**
     * Creates a new {@link ListenerCategory} with the specified listener
     * interface class and selector object.
     *
     * @param selector
     *        the selector object for this {@link ListenerCategory} (must not be
     *        <code>null</code>)
     * @param listenerInterface
     *        the listener interface for this {@link ListenerCategory} (must not
     *        be <code>null</code>)
     */
    public ListenerCategory(final Object selector, final Class listenerInterface) {
        Check.notNull(selector, "selector"); //$NON-NLS-1$
        Check.notNull(listenerInterface, "listenerInterface"); //$NON-NLS-1$

        this.selector = selector;
        this.listenerInterface = listenerInterface;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return selector.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ListenerCategory) {
            final ListenerCategory other = (ListenerCategory) obj;
            return selector.equals(other.selector);
        }
        return false;
    }

    /**
     * @return the selector {@link Object} of this {@link ListenerCategory}
     *         (will never be <code>null</code>)
     */
    public Object getSelector() {
        return selector;
    }

    /**
     * @return the listener interface {@link Class} of this
     *         {@link ListenerCategory} (will never be <code>null</code>)
     */
    public Class getListenerInterface() {
        return listenerInterface;
    }
}
