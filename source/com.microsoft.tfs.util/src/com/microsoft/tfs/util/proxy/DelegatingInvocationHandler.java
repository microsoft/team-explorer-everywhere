// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.proxy;

import java.lang.reflect.InvocationHandler;

/**
 * An InvocationHandler that wraps another object, and handles invocations by
 * delegating some or all method calls to the underlying object.
 *
 * Many uses of Java dynamic proxies will involve an InvocationHandler that
 * could be considered a DelegatingInvocationHandler.
 *
 * Explicit use of DelegatingInvocationHandler allows for generic code that can
 * "unwrap" proxied objects regardless of how many layers of proxying there are.
 */
public interface DelegatingInvocationHandler extends InvocationHandler {
    /**
     * @return the object being wrapped and delegated to by this
     *         InvocationHandler
     */
    public Object getProxiedObject();
}
