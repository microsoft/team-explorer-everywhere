// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.framework.background.BackgroundTaskEvent;
import com.microsoft.tfs.client.common.framework.background.BackgroundTaskListener;
import com.microsoft.tfs.client.common.framework.background.IBackgroundTask;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public final class ServerManager {
    private static final Log log = LogFactory.getLog(ServerManager.class);

    /**
     * This lock arbitrates access {@link #defaultServer}
     */
    private final Object lock = new Object();

    /**
     * This protects server creation to ensure only one is created at a time.
     */
    private final Object creationLock = new Object();

    /**
     * The actively managed server
     */
    private TFSServer defaultServer;

    /**
     * A set of the currently running connection jobs as {@link IBackgroundTask}
     * s. Clients may register these jobs for cancellability or reporting when
     * connecting to the server, but are not required to.
     */
    private final Set<IBackgroundTask> backgroundConnectionTasks = new HashSet<IBackgroundTask>();

    private final SingleListenerFacade listeners = new SingleListenerFacade(ServerManagerListener.class);
    private final SingleListenerFacade backgroundTaskListeners = new SingleListenerFacade(BackgroundTaskListener.class);

    public ServerManager() {
    }

    public final void addListener(final ServerManagerListener listener) {
        listeners.addListener(listener);
    }

    public final void removeListener(final ServerManagerListener listener) {
        listeners.removeListener(listener);
    }

    public void addBackgroundConnectionTaskListener(final BackgroundTaskListener listener) {
        backgroundTaskListeners.addListener(listener);
    }

    public void removeBackgroundConnectionTaskListener(final BackgroundTaskListener listener) {
        backgroundTaskListeners.removeListener(listener);
    }

    public void backgroundConnectionTaskStarted(final IBackgroundTask task) {
        synchronized (backgroundConnectionTasks) {
            backgroundConnectionTasks.add(task);
        }

        getBackgroundTaskListener().onBackgroundTaskStarted(new BackgroundTaskEvent(this, task));
    }

    public void backgroundConnectionTaskFinished(final IBackgroundTask task) {
        synchronized (backgroundConnectionTasks) {
            backgroundConnectionTasks.remove(task);
        }

        getBackgroundTaskListener().onBackgroundTaskFinished(new BackgroundTaskEvent(this, task));
    }

    public Set<IBackgroundTask> getBackgroundConnectionTasks() {
        synchronized (backgroundConnectionTasks) {
            return new HashSet<IBackgroundTask>(backgroundConnectionTasks);
        }
    }

    public final TFSServer getDefaultServer() {
        synchronized (lock) {
            return defaultServer;
        }
    }

    public final TFSServer getServer(URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        uri = URIUtils.removeTrailingSlash(uri);

        synchronized (lock) {
            if (defaultServer != null
                && uri.equals(URIUtils.removeTrailingSlash(defaultServer.getConnection().getBaseURI()))) {
                return defaultServer;
            }
            return null;
        }
    }

    public final TFSServer getServer(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final URI uri = URIUtils.removeTrailingSlash(connection.getBaseURI());

        return getServer(uri);
    }

    public final TFSServer getOrCreateServer(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final URI uri = URIUtils.removeTrailingSlash(connection.getBaseURI());

        TFSServer existingServer = getServer(uri);

        if (existingServer != null) {
            return existingServer;
        }

        final TFSServer server;

        /**
         * Lock for server creation.
         */
        synchronized (creationLock) {
            /**
             * Check to see if another thread created a server for the same URI
             * while this thread was blocked on the creationLock
             */
            existingServer = getServer(uri);

            if (existingServer != null) {
                return existingServer;
            }

            /**
             * Do the auto-refresh iff we've connected.
             */
            server = new TFSServer(connection);

            /**
             * Lock for map update
             */
            synchronized (lock) {
                final String messageFormat = "replaceServerInternal: creating new server, key=[{0}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, uri.toString());
                log.trace(message);

                defaultServer = server;
            }
        }
        getListener().onServerAdded(new ServerManagerEvent(this, server));

        getListener().onDefaultServerChanged(new ServerManagerEvent(this, server));

        return server;
    }

    public final void removeServer(final TFSServer server) {
        Check.notNull(server, "server"); //$NON-NLS-1$

        synchronized (lock) {
            if (server != defaultServer) {
                throw new IllegalArgumentException("The specified server is not in the ServerManager"); //$NON-NLS-1$
            }
            defaultServer = null;
        }

        getListener().onServerRemoved(new ServerManagerEvent(this, server));

        getListener().onDefaultServerChanged(new ServerManagerEvent(this, null));
    }

    private ServerManagerListener getListener() {
        return (ServerManagerListener) listeners.getListener();
    }

    private BackgroundTaskListener getBackgroundTaskListener() {
        return (BackgroundTaskListener) backgroundTaskListeners.getListener();
    }

}