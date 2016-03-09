// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.framework.command.CommandInitializationRunnable;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.util.Check;

/**
 * An extension of the basic command but that requires you have a valid
 * {@link TFSConnection}. If the connection has not yet been made to the server,
 * or if the last SOAP request failed, the connection will be rebuild in a
 * cancellable and friendly manner.
 */
public abstract class TFSConnectedCommand extends TFSCommand {
    protected TFSConnection connection;

    protected TFSConnectedCommand() {
        addCommandInitializationRunnable(new TFSCommandInitializationRunnable());
    }

    protected TFSConnectedCommand(final TFSConnection connection) {
        this();

        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    protected void setConnection(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    /**
     * Subclasses may call to ensure that there is a valid connection to the TFS
     * server. It is recommended that all commands that intend to use a server
     * connection call this method. (Commands that take advantage strictly of
     * local workspaces need not call this method.)
     *
     * @param server
     *        the current {@link TFSConnection} (not <code>null</code>)
     * @param progressMonitor
     *        a progress monitor (or <code>null</code>)
     * @throws an
     *         {@link Exception} if the connection failed
     */
    protected void ensureConnected(final TFSConnection connection, final IProgressMonitor progressMonitor)
        throws Exception {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        /*
         * If we have a valid connection, simply use it.
         */
        if (connection.hasAuthenticated() && connection.getConnectivityFailureOnLastWebServiceCall() == false) {
            return;
        }

        /*
         * Otherwise, the last web service call failed (or we have never even
         * tried.) Try again to authenticate. Do so in a background thread so
         * that users can cancel.
         */
        final TFSCommandConnectionRunnable connectionRunnable = new TFSCommandConnectionRunnable(connection);
        new Thread(connectionRunnable, "TFS Connection").start(); //$NON-NLS-1$

        final boolean commandCancellable = isCancellable();
        setCancellable(true);

        progressMonitor.setTaskName(Messages.getString("TFSConnectedCommand.ConnectingTaskName")); //$NON-NLS-1$

        while (connectionRunnable.isComplete() == false) {
            if (progressMonitor.isCanceled()) {
                throw new CoreException(Status.CANCEL_STATUS);
            }

            Thread.sleep(100);
        }

        if (connectionRunnable.getConnectionFailure() != null) {
            throw connectionRunnable.getConnectionFailure();
        }

        setCancellable(commandCancellable);

        /*
         * Recheck cancellation, as it may have occurred before resetting the
         * cancellability state.
         */
        if (progressMonitor.isCanceled()) {
            throw new CoreException(Status.CANCEL_STATUS);
        }

        progressMonitor.setTaskName(getName());
    }

    private class TFSCommandInitializationRunnable implements CommandInitializationRunnable {
        @Override
        public void initialize(final IProgressMonitor progressMonitor) throws Exception {
            if (connection != null) {
                ensureConnected(connection, progressMonitor);
            }
        }

        @Override
        public void complete(final IProgressMonitor progressMonitor) {
        }
    }

    private static class TFSCommandConnectionRunnable implements Runnable {
        private final TFSConnection connection;

        private final Object connectionStatusLock = new Object();
        private boolean connectionComplete = false;
        private Exception connectionFailure = null;

        private TFSCommandConnectionRunnable(final TFSConnection connection) {
            Check.notNull(connection, "connection"); //$NON-NLS-1$

            this.connection = connection;
        }

        @Override
        public void run() {
            Exception failure = null;

            try {
                connection.authenticate();
            } catch (final Exception e) {
                failure = e;
            }

            synchronized (connectionStatusLock) {
                connectionComplete = true;
                connectionFailure = failure;
            }
        }

        public boolean isComplete() {
            synchronized (connectionStatusLock) {
                return connectionComplete;
            }
        }

        public Exception getConnectionFailure() {
            return connectionFailure;
        }
    }
}
