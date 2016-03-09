// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.transport;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.util.Check;

/**
 * A thread that monitors multiple {@link HttpClient}s' connection managers for
 * idle connections (those that have not done work for
 * {@link #IDLE_CONNECTION_CLOSE_TIMEOUT_MILLISECONDS} milliseconds and closes
 * those connections.
 * <p>
 * A set of weak references is maintained to the {@link HttpClient}s and when
 * any reference goes null, the reference itself is removed from the set and the
 * client is no longer serviced. Any valid reference is serviced.
 * <p>
 *
 * @threadsafety thread-safe
 */
public class IdleHTTPConnectionCloser extends Thread {
    private final static Log log = LogFactory.getLog(IdleHTTPConnectionCloser.class);

    /**
     * Every time an HttpClient is passed to {@link #addClient(HttpClient)} we
     * create a {@link WeakReference} to wrap it, and that is stored in this
     * set.
     * <p>
     * The set is synchronized on itself.
     */
    private final static Set clientWeakReferences = new HashSet();

    /**
     * Number of milliseconds after which a connection that has performed no
     * work will be considered idle (and closed when the thread wakes after
     * sleeping {@link #THREAD_IDLE_CHECK_PERIOD_MILLISECONDS} milliseconds).
     */
    private final static int IDLE_CONNECTION_CLOSE_TIMEOUT_MILLISECONDS = 120000;

    /**
     * Number of milliseconds between this thread's check and close of any
     * connections that have been idle for at least
     * {@link #IDLE_CONNECTION_CLOSE_TIMEOUT_MILLISECONDS} milliseconds. This is
     * precisely the number of milliseconds the thread sleeps between checks.
     */
    private final static int THREAD_IDLE_CHECK_PERIOD_MILLISECONDS = 10000;

    /**
     * Creates a closer thread. Add clients to it with
     * {@link #addClient(HttpClient)}. The thread lives until it is interrupted.
     */
    public IdleHTTPConnectionCloser() {
        setDaemon(true);
        super.setName("Idle HTTP Connection Closer"); //$NON-NLS-1$
    }

    /**
     * Adds a client to be monitored by this thread.
     *
     * @param client
     *        the client whose connection manager should be monitored for idle
     *        connections that should be closed (not null).
     */
    public void addClient(final HttpClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        synchronized (clientWeakReferences) {
            clientWeakReferences.add(new WeakReference(client));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        try {
            while (true) {
                /*
                 * Determine which clients we must service, removing any old
                 * references as we iterate.
                 */
                ArrayList clients = new ArrayList();
                synchronized (clientWeakReferences) {
                    for (final Iterator i = clientWeakReferences.iterator(); i.hasNext();) {
                        final WeakReference reference = (WeakReference) i.next();
                        final HttpClient client = (HttpClient) reference.get();

                        // If the reference is now null, remove it from the set.
                        if (client == null) {
                            log.debug("client reference went null, removing"); //$NON-NLS-1$
                            i.remove();
                            continue;
                        }

                        clients.add(client);
                    }
                }

                if (clients.size() == 0) {
                    log.trace("no active clients"); //$NON-NLS-1$
                } else {
                    for (final Iterator i = clients.iterator(); i.hasNext();) {
                        HttpClient client = (HttpClient) i.next();
                        Check.notNull(client, "client"); //$NON-NLS-1$

                        final String messageFormat = "closing connections for {0} idle longer than {1} ms"; //$NON-NLS-1$
                        final String message = MessageFormat.format(
                            messageFormat,
                            client.toString(),
                            IDLE_CONNECTION_CLOSE_TIMEOUT_MILLISECONDS);
                        log.trace(message);

                        client.getHttpConnectionManager().closeIdleConnections(
                            IDLE_CONNECTION_CLOSE_TIMEOUT_MILLISECONDS);

                        // Make sure the client is out of scope before we enter
                        // sleep.
                        client = null;
                    }
                }

                // Remove client references before sleeping.
                clients = null;

                // Sleep for a while.
                final String messageFormat = "sleeping {0} ms"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, THREAD_IDLE_CHECK_PERIOD_MILLISECONDS);
                log.trace(message);
                Thread.sleep(THREAD_IDLE_CHECK_PERIOD_MILLISECONDS);
            }
        } catch (final InterruptedException e) {
            /*
             * If we were interrupted, we should log and quit.
             */
            log.warn("interrupted; exiting thread", e); //$NON-NLS-1$
        } catch (final Throwable t) {
            log.error("unexpected error", t); //$NON-NLS-1$
        }
    }
}
