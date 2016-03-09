// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * ProxySupport contains some static helper methods to make working with Java
 * dynamic proxies easier.
 */
public class ProxySupport {
    public static Object unwrap(final Object proxy) {
        if (proxy == null) {
            return null;
        } else if (Proxy.isProxyClass(proxy.getClass())) {
            final InvocationHandler handler = Proxy.getInvocationHandler(proxy);
            if (handler instanceof DelegatingInvocationHandler) {
                final Object proxiedObject = ((DelegatingInvocationHandler) handler).getProxiedObject();
                return unwrap(proxiedObject);
            }
            return null;
        } else {
            return proxy;
        }
    }

    public static Object createProxy(final Class interfaceClass, final DelegatingInvocationHandler invocationHandler) {
        return Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[] {
            interfaceClass
        }, invocationHandler);
    }
}
