// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A simple DelegatingInvocationHandler that delays for a specified number of
 * milliseconds before delegating each method call to the wrapped object.
 */
public class DelayingInvocationHandler implements DelegatingInvocationHandler {
    private final Object proxiedObject;
    private final long delayMillis;

    /**
     * Creates a new DelayingInvocationHandler.
     *
     * @param proxiedObject
     *        the object to proxy
     * @param delayMillis
     *        the number of milliseconds to delay before each call
     */
    public DelayingInvocationHandler(final Object proxiedObject, final long delayMillis) {
        this.proxiedObject = proxiedObject;
        this.delayMillis = delayMillis;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.proxy.DelegatingInvocationHandler#getProxiedObject
     * ()
     */
    @Override
    public Object getProxiedObject() {
        return proxiedObject;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
     * java.lang.reflect.Method, java.lang.Object[])
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        try {
            Thread.sleep(delayMillis);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        try {
            return method.invoke(proxiedObject, args);
        } catch (final InvocationTargetException ex) {
            throw ex.getCause();
        }
    }
}
