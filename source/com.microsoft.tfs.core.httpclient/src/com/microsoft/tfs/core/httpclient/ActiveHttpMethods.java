// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks which {@link HttpConnections}s are currently being used inside which
 * monitor objects (for example, a com.microsoft.tfs.util.tasks.TaskMonitor) to
 * facilitate cancellation by some other thread.
 * <p>
 * Normal usage is like:
 * <p>
 * <ol>
 * <li>Application code begins a command, calls {@link #setMonitor(Object)}</li>
 * <li>HttpClient calls {@link #setMethod(HttpMethod)} before executing some
 * {@link HttpMethod}</li>
 * <li>The method finishes, HttpClient calls {@link #clearMethod()}</li>
 * <li>Possibly more methods execute repeating steps 2 and 3</li>
 * <li>Application code finishes the command, calls {@link #clearMonitor()}</li>
 * </ol>
 * <p>
 * The following access rules must be followed:
 * <ul>
 * <li>Each call to {@link #setMonitor(Object)} must be followed by a call to
 * {@link #clearMonitor()}</li>
 * <li>Each call to {@link #setMethod(HttpMethod)} must be followed by a call to
 * {@link #clearMethod()}</li>
 * <li>Calls to {@link #setMethod(HttpMethod)} and {@link #clearMethod()} must
 * happen between the calls to {@link #setMonitor(Object)} and
 * {@link #clearMonitor()} (the nesting is required)</li>
 * </ul>
 * <p>
 * This class is optional for applications, they are not required register
 * monitors at all, but cancellation might not be available if they don't.
 *
 * @threadsafety thread-safe
 */
public abstract class ActiveHttpMethods {
    /**
     * Tracks which monitor is used for this thread. Provides implicit context
     * for {@link #setMethod(HttpMethod)} (because HttpClient, which calls
     * {@link #setMethod(HttpMethod)}, won't know anything about monitors).
     */
    private static final ThreadLocal<Object> monitor = new ThreadLocal<Object>();

    /**
     * Maps the monitor used for this thread to the executing {@link HttpMethod}
     * , if any. The map is updated on {@link #setMethod(HttpMethod)}, and the
     * monitor key is read from the thread-local {@link #monitor} if it's set
     * (if it wasn't set, nothing is put in the map).
     */
    private static final Map<Object, HttpMethod> methods = new HashMap<Object, HttpMethod>();

    public static void setMonitor(final Object mon) {
        if (mon == null) {
            throw new IllegalArgumentException("monitor must not be null");
        }

        monitor.set(mon);
    }

    public static void clearMonitor() {
        monitor.remove();
    }

    public static void setMethod(final HttpMethod method) {
        if (method == null) {
            throw new IllegalArgumentException("method must not be null");
        }

        final Object mon = monitor.get();

        if (mon == null) {
            // No outer monitor on this thread, nothing to track
            return;
        }

        synchronized (methods) {
            methods.put(mon, method);
        }
    }

    public static void clearMethod() {
        final Object mon = monitor.get();

        if (mon == null) {
            // No outer monitor on this thread, nothing to track
            return;
        }

        synchronized (methods) {
            methods.remove(mon);
        }
    }

    /**
     * @return a copy of the monitor to {@link HttpMethod} map
     */
    public static Map<Object, HttpMethod> getMonitorMethods() {
        synchronized (methods) {
            return new HashMap<Object, HttpMethod>(methods);
        }
    }
}
