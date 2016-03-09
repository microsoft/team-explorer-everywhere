// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A utility class that creates dynamic proxy based listeners that are used in
 * conjuction with a {@link ListenerList}.
 * </p>
 *
 * <p>
 * You can't instantiate instances of {@link MulticastListenerProxy} - it is a
 * factory class with static methods that return new proxy listener instances.
 * </p>
 *
 * <p>
 * {@link MulticastListenerProxy} returns dynamically generated listener objects
 * that implement a specified listener interface. The interface is implemented
 * by multicasting each method call to a collection of listeners managed by a
 * specified {@link ListenerList}. Each listener in the {@link ListenerList}
 * must implemented the specified listener interface.
 * </p>
 *
 * <p>
 * Optionally, error handling can be controlled by specifying an instance of
 * {@link ListenerExceptionHandler}. If any exceptions are thrown by the real
 * listeners while multicasting the event to them, the exception will be handled
 * by the exception handler.
 * </p>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * ListenerList listenerList = ...
 * listenerList.addListener(...);
 * listenerList.addListener(...);
 *
 * IMyListener listener = (IMyListener)
 *    MulticastListenerProxy.createProxy(
 *       IMyListener.class,
 *       listenerList);
 *
 * listener.doSomething();
 * </pre>
 *
 * </p>
 *
 * @see ListenerList
 */
public class MulticastListenerProxy {
    private static final Log log = LogFactory.getLog(MulticastListenerProxy.class);

    /**
     * Creates a new multicasting listener proxy that implements the specified
     * listener interface and uses the specified {@link ListenerList}. The proxy
     * listener will use default exception handling provided by
     * {@link DefaultExceptionHandler#INSTANCE}. This convenience constructor is
     * fully equivalent to calling:
     *
     * <pre>
     * createProxy(listenerInterface, listenerList, DefaultExceptionHandler.INSTANCE);
     * </pre>
     *
     * @param listenerInterface
     *        The listener interface that the returned object will implement
     *        (must not be <code>null</code>)
     * @param listenerList
     *        The {@link ListenerList} that is delegated to by the proxy (must
     *        not be <code>null</code>)
     * @return a new proxy instance as described above
     */
    public static Object createProxy(final Class listenerInterface, final ListenerList listenerList) {
        return createProxy(listenerInterface, listenerList, DefaultExceptionHandler.INSTANCE);
    }

    /**
     * Creates a new multicasting listener proxy that implements the specified
     * listener interface and uses the specified {@link ListenerList}. The proxy
     * listener will use the specified {@link ListenerExceptionHandler} for
     * exception handling.
     *
     * @param listenerInterface
     *        The listener interface that the returned object will implement
     *        (must not be <code>null</code>)
     * @param listenerList
     *        The {@link ListenerList} that is delegated to by the proxy (must
     *        not be <code>null</code>)
     * @param exceptionHandler
     *        the {@link ListenerExceptionHandler} used to handle any exceptions
     *        thrown by the real listeners
     * @return a new proxy instance as described above
     */
    public static Object createProxy(
        final Class listenerInterface,
        final ListenerList listenerList,
        final ListenerExceptionHandler exceptionHandler) {
        Check.notNull(listenerInterface, "listenerInterface"); //$NON-NLS-1$
        Check.notNull(listenerList, "listenerList"); //$NON-NLS-1$
        Check.notNull(exceptionHandler, "exceptionHandler"); //$NON-NLS-1$

        final Class[] classesToProxy = new Class[] {
            listenerInterface
        };

        final InvocationHandler invocationHandler = new MLPInvocationHandler(listenerList, exceptionHandler);

        return Proxy.newProxyInstance(listenerInterface.getClassLoader(), classesToProxy, invocationHandler);
    }

    private static class MLPInvocationHandler implements InvocationHandler {
        private final ListenerList listenerList;
        private final ListenerExceptionHandler exceptionHandler;

        private MLPInvocationHandler(final ListenerList listenerList, final ListenerExceptionHandler exceptionHandler) {
            this.listenerList = listenerList;
            this.exceptionHandler = new ExceptionHandlerWrapper(exceptionHandler);
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            listenerList.foreachListener(exceptionHandler, new ListenerRunnable() {
                @Override
                public boolean run(final Object listener) throws Exception {
                    method.invoke(listener, args);
                    return true;
                }
            });

            return null;
        }

        private static class ExceptionHandlerWrapper implements ListenerExceptionHandler {
            private final ListenerExceptionHandler wrappedExceptionHandler;

            public ExceptionHandlerWrapper(final ListenerExceptionHandler wrappedExceptionHandler) {
                this.wrappedExceptionHandler = wrappedExceptionHandler;
            }

            @Override
            public boolean onException(
                final Object listener,
                final ListenerRunnable listenerRunnable,
                final ListenerList listenerList,
                final Throwable exception) {
                if (exception instanceof InvocationTargetException) {
                    return wrappedExceptionHandler.onException(
                        listener,
                        listenerRunnable,
                        listenerList,
                        ((InvocationTargetException) exception).getTargetException());
                } else {
                    log.error("invalid configuration detected", exception); //$NON-NLS-1$
                    return true;
                }
            }
        }
    }
}
