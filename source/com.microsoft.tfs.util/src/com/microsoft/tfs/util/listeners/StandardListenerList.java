// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link StandardListenerList} is a standard implementation of
 * {@link ListenerList}. This implementation is intended for use in most
 * situations where a {@link ListenerList} is needed. This implementation
 * provides thread safety and good performance for most normal uses of the
 * publisher-subscriber pattern.
 * </p>
 *
 * <p>
 * With regard to concurrency, there are two categories of methods on this
 * class. One category is methods that modify the list of listeners: these are
 * {@link #addListener(Object)}, {@link #removeListener(Object)}, and
 * {@link #clear()}. All of the other methods are in the second category - they
 * do not modify the list of listeners. The following properties are enforced by
 * this class:
 * <ul>
 * <li>Only one modify operation is permitted to happen at a time</li>
 * <li>Any number of non-modify operations are permitted to happen at a time
 * </li>
 * <li>Any number of non-modify operations are permitted to happen at the same
 * time as a single modify operation</li>
 * </ul>
 * In summary, a lock is aquired and held for all of the modify operations. The
 * non-modify operations are not impacted by this lock in any way, and the
 * non-modify operations do not aquire locks. For example, this means that one
 * thread can add a listener to the list while another thread is concurrently
 * iterating over the listeners in the list.
 * </p>
 *
 * <p>
 * The way in which a {@link StandardListenerList} instance compares listeners
 * is configurable. At construction time, you can pass in a {@link Comparator}
 * object which will be used to make all comparisons between listeners. Several
 * predefined, commonly used {@link Comparator}s are available through the
 * {@link Comparators} class in this package.
 * </p>
 *
 * <p>
 * The design of this class is based in part on the <code>Publisher</code> class
 * from Allen Holub's book "Holub on Patterns".
 * </p>
 *
 * @see ListenerList
 * @see Comparator
 * @see Comparators
 */
public class StandardListenerList implements ListenerList {
    /**
     * Used to compare listeners - never <code>null</code>.
     */
    private final Comparator comparator;

    /**
     * This lock is aquired and held for all operations that modify the list of
     * listeners.
     */
    private final Object modifyLock = new Object();

    /**
     * The starting node of the linked list of listeners - initially
     * <code>null</code>. It is volatile because the non-modify operations do
     * not aquire any lock before accessing this field.
     */
    private volatile ListenerNode listeners;

    /**
     * Create a new, empty {@link StandardListenerList} with a default
     * {@link Comparator}. The default comparator is identity-based and will
     * consider listeners equal only if they are the same object. This
     * convenience constructor is fully equivalent to:
     *
     * <pre>
     * new StandardListenerList(com.microsoft.tfs.util.listeners.Comparators.IDENTITY)
     * </pre>
     */
    public StandardListenerList() {
        this(Comparators.IDENTITY);
    }

    /**
     * Create a new, empty {@link StandardListenerList} with a specified
     * {@link Comparator}. As mentioned above, the {@link Comparator} will be
     * used to make all comparisons between listeners stored in this
     * {@link ListenerList}. Some {@link Comparator}s that are commonly used
     * with listeners are predefined in the {@link Comparators} class in this
     * package.
     *
     * @param listenerComparator
     *        a {@link Comparator} used to make all comparisons between
     *        listeners stored in this {@link ListenerList} (must not be
     *        <code>null</code>)
     */
    public StandardListenerList(final Comparator listenerComparator) {
        Check.notNull(listenerComparator, "listenerComparator"); //$NON-NLS-1$

        comparator = listenerComparator;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.listeners.ListenerList#addListener(java.lang.
     * Object )
     */
    @Override
    public boolean addListener(final Object listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (modifyLock) {
            if (listeners == null) {
                listeners = new ListenerNode(listener, comparator, null);
                return true;
            }

            final ListenerNodeHolder holder = new ListenerNodeHolder();
            if (listeners.addListener(listener, holder)) {
                listeners = holder.getNode();
                return true;
            }

            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.listeners.ListenerList#clear()
     */
    @Override
    public boolean clear() {
        synchronized (modifyLock) {
            final boolean listenersExist = (listeners != null);
            listeners = null;
            return listenersExist;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.listeners.ListenerList#containsListener(java.lang
     * .Object)
     */
    @Override
    public boolean containsListener(final Object listenerToTest) {
        Check.notNull(listenerToTest, "listenerToTest"); //$NON-NLS-1$

        for (ListenerNode node = listeners; node != null; node = node.next) {
            if (comparator.compare(listenerToTest, node.listener) == 0) {
                return true;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.listeners.ListenerList#foreachListener(com.
     * microsoft .tfs.util.listeners.ListenerRunnable)
     */
    @Override
    public void foreachListener(final ListenerRunnable runnable) {
        foreachListener(DefaultExceptionHandler.INSTANCE, runnable);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.listeners.ListenerList#foreachListener(com.
     * microsoft .tfs.util.listeners.ListenerExceptionHandler,
     * com.microsoft.tfs.util.listeners.ListenerRunnable)
     */
    @Override
    public void foreachListener(final ListenerExceptionHandler exceptionHandler, final ListenerRunnable runnable) {
        boolean keepGoing = true;

        for (ListenerNode node = listeners; node != null && keepGoing; node = node.next) {
            try {
                keepGoing = runnable.run(node.listener);
            } catch (final Exception e) {
                keepGoing = exceptionHandler.onException(node.listener, runnable, this, e);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.listeners.ListenerList#getListeners()
     */
    @Override
    public Object[] getListeners() {
        return getListeners(new Object[] {});
    }

    @Override
    public Object[] getListeners(final Object[] a) {
        final List list = new ArrayList();

        for (ListenerNode node = listeners; node != null; node = node.next) {
            list.add(node.listener);
        }

        return list.toArray(a);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.listeners.ListenerList#removeListener(java.lang
     * .Object)
     */
    @Override
    public boolean removeListener(final Object listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        synchronized (modifyLock) {
            if (listeners == null) {
                return false;
            }

            final ListenerNodeHolder holder = new ListenerNodeHolder();
            if (listeners.removeListener(listener, holder)) {
                listeners = holder.getNode();
                return true;
            }

            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.listeners.ListenerList#size()
     */
    @Override
    public int size() {
        int count = 0;

        for (ListenerNode node = listeners; node != null; node = node.next) {
            ++count;
        }

        return count;
    }

    private static class ListenerNode {
        public final Object listener;
        public final Comparator listenerComparator;
        public final ListenerNode next;

        public ListenerNode(final Object listener, final Comparator listenerComparator, final ListenerNode next) {
            this.listener = listener;
            this.listenerComparator = listenerComparator;
            this.next = next;
        }

        public boolean removeListener(final Object listenerToRemove, final ListenerNodeHolder holder) {
            if (listenerComparator.compare(listener, listenerToRemove) == 0) {
                holder.setNode(next);
                return true;
            }

            if (next == null) {
                return false;
            } else {
                if (next.removeListener(listenerToRemove, holder)) {
                    holder.setNode(new ListenerNode(listener, listenerComparator, holder.getNode()));
                    return true;
                } else {
                    return false;
                }
            }
        }

        public boolean addListener(final Object listenerToAdd, final ListenerNodeHolder holder) {
            if (listenerComparator.compare(listener, listenerToAdd) == 0) {
                return false;
            }

            if (next == null) {
                final ListenerNode node = new ListenerNode(listenerToAdd, listenerComparator, null);
                holder.setNode(new ListenerNode(listener, listenerComparator, node));
                return true;
            } else {
                if (next.addListener(listenerToAdd, holder)) {
                    holder.setNode(new ListenerNode(listener, listenerComparator, holder.getNode()));
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private static class ListenerNodeHolder {
        private ListenerNode node;

        public ListenerNode getNode() {
            return node;
        }

        public void setNode(final ListenerNode node) {
            this.node = node;
        }
    }
}
