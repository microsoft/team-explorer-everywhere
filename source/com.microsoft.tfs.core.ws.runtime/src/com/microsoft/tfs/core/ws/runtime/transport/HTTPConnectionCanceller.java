// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.transport;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.ActiveHttpMethods;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpMethod;
import com.microsoft.tfs.util.tasks.TaskMonitor;

/**
 * A thread that watches {@link ActiveHttpMethods} for cancelled
 * {@link TaskMonitor}s, and aborts the first active {@link HttpMethod}
 * associated with them. This is done in a separate thread because the thread
 * using the HttpMethod might be blocked using the socket, and we have no way of
 * doing the abort from the {@link TaskMonitor} process (as we generally wrap a
 * closed Eclipse IProgressMonitor implementation).
 * <p>
 * Aborting the {@link HttpMethod} closes the underlying socket, which unblocks
 * the thread that was blocked while using it.
 *
 * @threadsafety thread-safe
 */
public class HTTPConnectionCanceller extends Thread {
    private final static Log log = LogFactory.getLog(HTTPConnectionCanceller.class);

    /**
     * How long to wait between checks of {@link ActiveHttpMethods} structure
     * for cancelled monitors. Checks are cheap.
     */
    private final static long ACTIVE_METHODS_CHECK_MILLISECONDS = 1000;

    /**
     * How long to let other threads try to finish an {@link HttpMethod} that
     * belongs to a cancelled {@link TaskMonitor} before this class aborts it.
     * Voluntary shutdown by another thread is cleaner than method abort.
     */
    private final static long ABORT_WAIT_MILLISECONDS = 5000;

    /**
     * Collects the cancelled {@link TaskMonitor}s that have been observed by
     * this class, so that we only do one cancellation per {@link TaskMonitor}
     * (even if it would use multiple {@link HttpMethod}s in its lifetime).
     * <p>
     * The entries disappear when the task monitor is garbage collected (which
     * can normally happen quickly after the command that used it finishes).
     * <p>
     * The value is unused; this would be a WeakHashSet if Java had one.
     */
    private final Map<TaskMonitor, Object> cancelledMonitors = new WeakHashMap<TaskMonitor, Object>();

    /**
     * A queue of {@link HttpMethod}s that belonged to cancelled
     * {@link TaskMonitor}s that need aborted in the future. They stay in this
     * queue for a while to give other threads a chance to do a cleaner
     * cancellation via polling, but not all threads can do this (they may be
     * blocked, and that's why this canceller class exists).
     */
    private final List<MethodAbortInfo> scheduledAborts = new ArrayList<MethodAbortInfo>();

    /**
     * Creates a closer thread. Add clients to it with
     * {@link #addClient(HttpClient)}. The thread lives until it is interrupted.
     */
    public HTTPConnectionCanceller() {
        setDaemon(true);
        super.setName("HTTP Method Canceller"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            while (true) {
                abort();

                for (final Entry<Object, HttpMethod> entry : ActiveHttpMethods.getMonitorMethods().entrySet()) {
                    final Object key = entry.getKey();
                    if (key instanceof TaskMonitor) {
                        final TaskMonitor tm = (TaskMonitor) key;

                        // If it's cancelled but not yet handled by this class
                        if (tm.isCanceled() && !cancelledMonitors.containsKey(tm)) {
                            final HttpMethod method = entry.getValue();
                            if (method != null) {
                                // Prevent us from handling this TaskMonitor
                                // again
                                cancelledMonitors.put(tm, null);

                                // Schedule it to be aborted in the future
                                log.trace(MessageFormat.format(
                                    "Schedling abort for {0} in {1} ms", //$NON-NLS-1$
                                    method,
                                    ABORT_WAIT_MILLISECONDS));
                                scheduledAborts.add(
                                    new MethodAbortInfo(System.currentTimeMillis() + ABORT_WAIT_MILLISECONDS, method));
                            }
                        }
                    }
                }

                log.trace(MessageFormat.format("sleeping (aborted weak map size={0})", cancelledMonitors.size())); //$NON-NLS-1$
                Thread.sleep(ACTIVE_METHODS_CHECK_MILLISECONDS);
            }
        } catch (final InterruptedException e) {
            /*
             * If we were interrupted, we should log and quit.
             */
            log.warn("interrupted; exiting thread"); //$NON-NLS-1$
        } catch (final Throwable t) {
            log.error("unexpected error", t); //$NON-NLS-1$
        }
    }

    /**
     * Aborts the {@link HttpMethod}s that were scheduled to be aborted by a
     * previous check and have passed their wait period.
     */
    private void abort() {
        final Iterator<MethodAbortInfo> i = scheduledAborts.iterator();
        while (i.hasNext()) {
            final MethodAbortInfo info = i.next();

            if (System.currentTimeMillis() > info.abortAfter) {
                log.trace(MessageFormat.format("aborting method {0}", info.method)); //$NON-NLS-1$
                try {
                    info.method.abort();
                } catch (final Throwable t) {
                    log.warn(MessageFormat.format("error while aborting method {0}", info.method), t); //$NON-NLS-1$
                }

                i.remove();
            }
        }
    }

    private static class MethodAbortInfo {
        public final long abortAfter;
        public final HttpMethod method;

        public MethodAbortInfo(final long abortAfter, final HttpMethod method) {
            this.abortAfter = abortAfter;
            this.method = method;
        }
    }
}
