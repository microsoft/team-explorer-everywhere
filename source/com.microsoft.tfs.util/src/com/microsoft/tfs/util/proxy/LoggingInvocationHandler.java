// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import org.apache.commons.logging.Log;

/**
 * A simple DelegatingInvocationHandler that simply times each method and writes
 * out call times to a Log instance.
 */
public class LoggingInvocationHandler implements DelegatingInvocationHandler {
    private final Object proxiedObject;
    private final Log log;

    /**
     * Creates a new LoggingInvocationHandler that will log method call times to
     * the given object.
     *
     * @param proxiedObject
     *        the object to proxy
     * @param log
     *        the log to write call times to
     */
    public LoggingInvocationHandler(final Object proxiedObject, final Log log) {
        this.proxiedObject = proxiedObject;
        this.log = log;
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
        final long stTime = System.currentTimeMillis();
        try {
            return method.invoke(proxiedObject, args);
        } catch (final InvocationTargetException ex) {
            throw ex.getCause();
        } finally {
            final long elapsed = System.currentTimeMillis() - stTime;
            if (log.isTraceEnabled()) {
                traceLog(method, args, elapsed);
            } else if (log.isDebugEnabled()) {
                debugLog(method, elapsed);
            }
        }
    }

    private void traceLog(final Method method, final Object[] args, final long elapsed) {
        final StringBuffer buffer = new StringBuffer();

        buffer.append(elapsed);
        buffer.append(": "); //$NON-NLS-1$

        buffer.append(method.getName());
        buffer.append("("); //$NON-NLS-1$

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                buffer.append(getArgumentRepresentation(args[i]));
                if (i < args.length - 1) {
                    buffer.append(","); //$NON-NLS-1$
                }
            }
        }

        buffer.append(")"); //$NON-NLS-1$

        log.trace(buffer.toString());
    }

    private String getArgumentRepresentation(final Object object) {
        if (object == null) {
            return null;
        }

        String s = null;

        if (object.getClass().isArray()) {
            s = object.getClass().getComponentType().getName();
            if (s.lastIndexOf(".") != -1) //$NON-NLS-1$
            {
                s = s.substring(s.lastIndexOf(".") + 1); //$NON-NLS-1$
            }
            s += "[]"; //$NON-NLS-1$
        } else if (object instanceof Calendar) {
            s = ((Calendar) object).getTime().toString();
        } else {
            s = object.toString();
            s = s.replace('\n', ' ');
            s = s.replace('\r', ' ');
            s = s.replace(',', ' ');
            if (s.length() > 40) {
                s = object.getClass().getName();
                if (s.lastIndexOf(".") != -1) //$NON-NLS-1$
                {
                    s = s.substring(s.lastIndexOf(".") + 1); //$NON-NLS-1$
                }
            }
        }

        return s;
    }

    private void debugLog(final Method method, final long elapsed) {
        log.debug(method.getName() + ": " + elapsed); //$NON-NLS-1$
    }
}
