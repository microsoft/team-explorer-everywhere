// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import java.text.MessageFormat;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link SingleListenerFacade} wraps a {@link ListenerList} and presents the
 * facade of a single listener object instead of a list of (possibly multiple)
 * listener objects. Using this class, publishing an event to a list of
 * listeners is simple: you make a single method call to the single listener
 * object provided by this class.
 * </p>
 *
 * <p>
 * There are two cases for this facade. In the first case, there is exactly one
 * listener in the {@link ListenerList} that this class wraps. If so, the
 * {@link #getListener()} method simply returns that listener. Since having a
 * single registered listener is a common case, this special handling provides
 * the best performance in that common case. The other case is that there are 0
 * or more than one listeners in the {@link ListenerList}. In that case, a
 * special proxy listener is generated and cached (making use of the
 * {@link MulticastListenerProxy} class). This proxy listener will iterate over
 * and invoke the listeners in the {@link ListenerList} when it is invoked.
 * </p>
 *
 * <p>
 * The {@link #getListener()} method will never return <code>null</code>. For
 * very performance-sensitive uses of this class, it may be desirable to avoid
 * generating a proxy listener if there are no listeners registered with the
 * {@link ListenerList}. In this special case, call
 * {@link #getListener(boolean)} and pass <code>true</code> for the
 * <code>allowNull</code> parameter. If this parameter is <code>true</code> and
 * there are exactly 0 listeners registered, the overhead of generating a proxy
 * listener will be avoided and <code>null</code> will be returned. Most of the
 * time, it's advisable to take the slight performance hit and avoid cluttering
 * up the client class with <code>null</code> checks.
 * </p>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * SingleListenerFacade facade = new SingleListenerFacade(
 *    IMyListener.class);
 * facade.addListener(...);
 * ((IMyListener) facade.getListener()).doSomething();
 * </pre>
 *
 * </p>
 *
 * @see ListenerList
 * @see MulticastListenerProxy
 */
public class SingleListenerFacade {
    /**
     * The type of listener we will generate if we need to generate a proxy
     * listener (never <code>null</code>).
     */
    private final Class listenerInterface;

    /**
     * The {@link ListenerList} we're wrapping (never <code>null</code>).
     */
    private final ListenerList listenerList;

    /**
     * The {@link ListenerExceptionHandler} we will use if we need to generate a
     * proxy listener (never <code>null</code>).
     */
    private final ListenerExceptionHandler exceptionHandler;

    /**
     * A lock used to access to the proxy listener field (never
     * <code>null</code>).
     */
    private final Object proxyListenerLock = new Object();

    /**
     * The generated proxy listener (initially <code>null</code>).
     */
    private Object proxyListener;

    /**
     * Creates a new {@link SingleListenerFacade} using the specified listener
     * interface class. This {@link SingleListenerFacade} will instantiate and
     * wrap a new {@link StandardListenerList}. The generated proxy listener, if
     * any, will use the default exception handler provided by
     * {@link DefaultExceptionHandler#INSTANCE}. This convenience constructor is
     * fully equivalent to:
     *
     * <pre>
     * new SingleListenerFacade(listenerInterface, new StandardListenerList(), DefaultExceptionHandler.INSTANCE);
     * </pre>
     *
     * @param listenerInterface
     *        The interface that the proxy listener will implement (must not be
     *        <code>null</code>)
     */
    public SingleListenerFacade(final Class listenerInterface) {
        this(listenerInterface, new StandardListenerList(), DefaultExceptionHandler.INSTANCE);
    }

    /**
     * Creates a new {@link SingleListenerFacade} using the specified listener
     * interface class that wraps the specified {@link ListenerList}. The
     * generated proxy listener, if any, will use the default exception handler
     * provided by {@link DefaultExceptionHandler#INSTANCE}. This convenience
     * constructor is fully equivalent to:
     *
     * <pre>
     * new SingleListenerFacade(listenerInterface, listenerList, DefaultExceptionHandler.INSTANCE);
     * </pre>
     *
     * @param listenerInterface
     *        The interface that the proxy listener will implement (must not be
     *        <code>null</code>)
     * @param listenerList
     *        The {@link ListenerList} to wrap (must not be <code>null</code>)
     */
    public SingleListenerFacade(final Class listenerInterface, final ListenerList listenerList) {
        this(listenerInterface, listenerList, DefaultExceptionHandler.INSTANCE);
    }

    /**
     * Creates a new {@link SingleListenerFacade} using the specified listener
     * interface class that wraps the specified {@link ListenerList}. The
     * generated proxy listener, if any, will use the specified exception
     * handler.
     *
     * @param listenerInterface
     *        The interface that the proxy listener will implement (must not be
     *        <code>null</code>)
     * @param listenerList
     *        The {@link ListenerList} to wrap (must not be <code>null</code>)
     * @param exceptionHandler
     *        The {@link ListenerExceptionHandler} that the generated proxy
     *        listener will use (must not be <code>null</code>)
     */
    public SingleListenerFacade(
        final Class listenerInterface,
        final ListenerList listenerList,
        final ListenerExceptionHandler exceptionHandler) {
        Check.notNull(listenerInterface, "listenerInterface"); //$NON-NLS-1$
        Check.notNull(listenerList, "listenerList"); //$NON-NLS-1$
        Check.notNull(exceptionHandler, "exceptionHandler"); //$NON-NLS-1$

        this.listenerInterface = listenerInterface;
        this.listenerList = listenerList;
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * getListenerList().addListener(listener);
     * </pre>
     */
    public boolean addListener(final Object listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        if (!listenerInterface.isInstance(listener)) {
            final String messageFormat = "the specified listener type [{0}] is not an instance of [{1}]"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, listener.getClass().getName(), listenerInterface.getName());
            throw new IllegalArgumentException(message);
        }

        return listenerList.addListener(listener);
    }

    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * getListenerList().removeListener(listener);
     * </pre>
     */
    public boolean removeListener(final Object listener) {
        return listenerList.removeListener(listener);
    }

    /**
     * @return the {@link ListenerList} being wrapped by this facade
     */
    public ListenerList getListenerList() {
        return listenerList;
    }

    /**
     * Obtains a single listener object that represents all of the listeners
     * being managed by this {@link SingleListenerFacade}. See the documentation
     * above for details on how this listener is generated.
     *
     * @return a listener object as described above (never <code>null</code>)
     */
    public Object getListener() {
        return getListener(false);
    }

    /**
     * Obtains a single listener object that represents all of the listeners
     * being managed by this {@link SingleListenerFacade}. See the documentation
     * above for details on how this listener is generated.
     *
     * @param allowNull
     *        if <code>true</code> and there are exactly 0 listeners registered,
     *        <code>null</code> will be returned instead of generating a proxy
     *        listener
     * @return a listener object as described above
     */
    public Object getListener(final boolean allowNull) {
        synchronized (proxyListenerLock) {
            if (proxyListener != null) {
                return proxyListener;
            }
        }

        final int size = listenerList.size();
        if (size == 0 && allowNull) {
            return null;
        }
        if (size == 1) {
            final Object[] listeners = listenerList.getListeners();
            if (listeners.length == 1) {
                return listeners[0];
            }
        }

        final Object createdProxyListener =
            MulticastListenerProxy.createProxy(listenerInterface, listenerList, exceptionHandler);

        synchronized (proxyListenerLock) {
            proxyListener = createdProxyListener;
            return proxyListener;
        }
    }
}
