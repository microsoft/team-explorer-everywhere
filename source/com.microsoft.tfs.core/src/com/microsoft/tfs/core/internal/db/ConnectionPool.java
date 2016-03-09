// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;

public class ConnectionPool {
    private static final Log log = LogFactory.getLog(ConnectionPool.class);

    private static final int POOL_SIZE = 10;

    private final List pooledConnections = new ArrayList();
    private final Map givenConnections = new HashMap();
    private boolean shutdown = false;

    private final ConnectionConfiguration connectionConfiguration;

    public ConnectionPool(final ConnectionConfiguration connectionConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
    }

    public synchronized DBConnection getConnection() {
        if (shutdown) {
            throw new IllegalStateException(Messages.getString("ConnectionPool.ConnectionPoolHasBeenShutdown")); //$NON-NLS-1$
        }
        if (pooledConnections.size() > 0) {
            final DBConnection connection = (DBConnection) pooledConnections.remove(0);
            givenConnections.put(connection, MessageFormat.format(
                "gave connection to [{0}] at [{1}]", //$NON-NLS-1$
                Thread.currentThread().getName(),
                new Date()));
            return connection;
        } else if (givenConnections.size() < POOL_SIZE) {
            final DBConnection connection = connectionConfiguration.createNewConnection();
            givenConnections.put(connection, MessageFormat.format(
                "gave connection to [{0}] at [{1}]", //$NON-NLS-1$
                Thread.currentThread().getName(),
                new Date()));
            return connection;
        }
        throw new IllegalStateException(MessageFormat.format("no connections left in pool ({0})", givenConnections)); //$NON-NLS-1$
    }

    public synchronized void releaseConnection(final DBConnection connection) {
        if (shutdown) {
            throw new IllegalStateException(Messages.getString("ConnectionPool.ConnectionPoolHasBeenShutdown")); //$NON-NLS-1$
        }
        pooledConnections.add(connection);
        givenConnections.remove(connection);
    }

    public void executeWithPooledConnection(final DBTask task) {
        final DBConnection connection = getConnection();
        try {
            task.performTask(connection);
        } finally {
            releaseConnection(connection);
        }
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }

        if (givenConnections.size() > 0) {
            log.warn(MessageFormat.format(
                "shut down connection pool: some connections are still out ({0})", //$NON-NLS-1$
                givenConnections));
        }

        for (final Iterator it = pooledConnections.iterator(); it.hasNext();) {
            final DBConnection connection = (DBConnection) it.next();
            connection.close();
        }

        if (connectionConfiguration.getDriverClass().equals("org.hsqldb.jdbcDriver")) //$NON-NLS-1$
        {
            final DBConnection connection = connectionConfiguration.createNewConnection();
            connection.createStatement("SHUTDOWN").executeUpdate(); //$NON-NLS-1$
        }

        connectionConfiguration.releaseLock();

        shutdown = true;
    }
}
