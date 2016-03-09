/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/util/IdleConnectionHandler.java,v 1.2
 * 2004/05/13 02:40:36 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package com.microsoft.tfs.core.httpclient.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.HttpConnection;

/**
 * A helper class for connection managers to track idle connections.
 *
 * <p>
 * This class is not synchronized.
 * </p>
 *
 * @see com.microsoft.tfs.core.httpclient.HttpConnectionManager#closeIdleConnections(long)
 *
 * @since 3.0
 */
public class IdleConnectionHandler {

    private static final Log log = LogFactory.getLog(IdleConnectionHandler.class);

    /** Holds connections and the time they were added. */
    private final Map connectionToAdded = new HashMap();

    /**
     *
     */
    public IdleConnectionHandler() {
        super();
    }

    /**
     * Registers the given connection with this handler. The connection will be
     * held until {@link #remove(HttpConnection)} or
     * {@link #closeIdleConnections(long)} is called.
     *
     * @param connection
     *        the connection to add
     *
     * @see #remove(HttpConnection)
     */
    public void add(final HttpConnection connection) {

        final Long timeAdded = new Long(System.currentTimeMillis());

        if (log.isDebugEnabled()) {
            log.debug("Adding connection " + connection.getID() + " at: " + timeAdded);
        }

        connectionToAdded.put(connection, timeAdded);
    }

    /**
     * Removes the given connection from the list of connections to be closed
     * when idle.
     *
     * @param connection
     */
    public void remove(final HttpConnection connection) {
        log.debug("Removing connection " + connection.getID());
        connectionToAdded.remove(connection);
    }

    /**
     * Removes all connections referenced by this handler.
     */
    public void removeAll() {
        log.debug("Idle connections count = " + connectionToAdded.size());
        connectionToAdded.clear();
    }

    /**
     * Closes connections that have been idle for at least the given amount of
     * time.
     *
     * @param idleTime
     *        the minimum idle time, in milliseconds, for connections to be
     *        closed
     */
    public void closeIdleConnections(final long idleTime) {

        // the latest time for which connections will be closed
        final long idleTimeout = System.currentTimeMillis() - idleTime;

        if (log.isDebugEnabled()) {
            log.debug("Checking for connections, idleTimeout: " + idleTimeout);
        }

        final Iterator connectionIter = connectionToAdded.keySet().iterator();

        while (connectionIter.hasNext()) {
            final HttpConnection conn = (HttpConnection) connectionIter.next();
            final Long connectionTime = (Long) connectionToAdded.get(conn);
            if (connectionTime.longValue() <= idleTimeout) {
                if (log.isDebugEnabled()) {
                    log.debug("Closing connection " + conn.getID() + ", connection time: " + connectionTime);
                }
                connectionIter.remove();
                conn.close();
            }
        }
    }
}
