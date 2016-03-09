/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/SimpleHttpConnectionManager.java,v
 * 1.23 2004/10/16 22:40:08 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.params.HttpConnectionManagerParams;

/**
 * A connection manager that provides access to a single HttpConnection. This
 * manager makes no attempt to provide exclusive access to the contained
 * HttpConnection.
 *
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author Eric Johnson
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Laura Werner
 *
 * @since 2.0
 */
public class SimpleHttpConnectionManager implements HttpConnectionManager {

    private static final Log LOG = LogFactory.getLog(SimpleHttpConnectionManager.class);

    private static final String MISUSE_MESSAGE = "SimpleHttpConnectionManager being used incorrectly.  Be sure that"
        + " HttpMethod.releaseConnection() is always called and that only one thread"
        + " and/or method is using this connection manager at a time.";

    /**
     * Since the same connection is about to be reused, make sure the previous
     * request was completely processed, and if not consume it now.
     *
     * @param conn
     *        The connection
     */
    static void finishLastResponse(final HttpConnection conn) {
        final InputStream lastResponse = conn.getLastResponseInputStream();
        if (lastResponse != null) {
            conn.setLastResponseInputStream(null);
            try {
                lastResponse.close();
            } catch (final IOException ioe) {
                conn.close();
            }
        }
    }

    /** The http connection */
    protected HttpConnection httpConnection;

    /**
     * Collection of parameters associated with this connection manager.
     */
    private HttpConnectionManagerParams params = new HttpConnectionManagerParams();

    /**
     * The time the connection was made idle.
     */
    private long idleStartTime = Long.MAX_VALUE;

    /**
     * Used to test if {@link #httpConnection} is currently in use (i.e. checked
     * out). This is only used as a sanity check to help debug cases where this
     * connection manager is being used incorrectly. It will not be used to
     * enforce thread safety.
     */
    private volatile boolean inUse = false;

    private boolean alwaysClose = false;

    /**
     * The connection manager created with this constructor will try to keep the
     * connection open (alive) between consecutive requests if the alwaysClose
     * parameter is set to <tt>false</tt>. Otherwise the connection manager will
     * always close connections upon release.
     *
     * @param alwaysClose
     *        if set <tt>true</tt>, the connection manager will always close
     *        connections upon release.
     */
    public SimpleHttpConnectionManager(final boolean alwaysClose) {
        super();
        this.alwaysClose = alwaysClose;
    }

    /**
     * The connection manager created with this constructor will always try to
     * keep the connection open (alive) between consecutive requests.
     */
    public SimpleHttpConnectionManager() {
        super();
    }

    /**
     * @see HttpConnectionManager#getConnection(HostConfiguration)
     */
    @Override
    public HttpConnection getConnection(final HostConfiguration hostConfiguration) {
        return getConnection(hostConfiguration, 0);
    }

    /**
     * Gets the staleCheckingEnabled value to be set on HttpConnections that are
     * created.
     *
     * @return <code>true</code> if stale checking will be enabled on
     *         HttpConections
     *
     * @see HttpConnection#isStaleCheckingEnabled()
     *
     * @deprecated Use
     *             {@link HttpConnectionManagerParams#isStaleCheckingEnabled()},
     *             {@link HttpConnectionManager#getParams()}.
     */
    @Deprecated
    public boolean isConnectionStaleCheckingEnabled() {
        return params.isStaleCheckingEnabled();
    }

    /**
     * Sets the staleCheckingEnabled value to be set on HttpConnections that are
     * created.
     *
     * @param connectionStaleCheckingEnabled
     *        <code>true</code> if stale checking will be enabled on
     *        HttpConections
     *
     * @see HttpConnection#setStaleCheckingEnabled(boolean)
     *
     * @deprecated Use
     *             {@link HttpConnectionManagerParams#setStaleCheckingEnabled(boolean)}
     *             , {@link HttpConnectionManager#getParams()}.
     */
    @Deprecated
    public void setConnectionStaleCheckingEnabled(final boolean connectionStaleCheckingEnabled) {
        params.setStaleCheckingEnabled(connectionStaleCheckingEnabled);
    }

    /**
     * This method always returns the same connection object. If the connection
     * is already open, it will be closed and the new host configuration will be
     * applied.
     *
     * @param hostConfiguration
     *        The host configuration specifying the connection details.
     * @param timeout
     *        this parameter has no effect. The connection is always returned
     *        immediately.
     * @since 3.0
     */
    @Override
    public HttpConnection getConnectionWithTimeout(final HostConfiguration hostConfiguration, final long timeout) {

        if (httpConnection == null) {
            httpConnection = new HttpConnection(hostConfiguration);
            httpConnection.setHttpConnectionManager(this);
            httpConnection.getParams().setDefaults(params);
        } else {

            // make sure the host and proxy are correct for this connection
            // close it and set the values if they are not
            if (!hostConfiguration.hostEquals(httpConnection) || !hostConfiguration.proxyEquals(httpConnection)) {

                if (httpConnection.isOpen()) {
                    httpConnection.close();
                }

                httpConnection.setHost(hostConfiguration.getHost());
                httpConnection.setPort(hostConfiguration.getPort());
                httpConnection.setProtocol(hostConfiguration.getProtocol());
                httpConnection.setLocalAddress(hostConfiguration.getLocalAddress());

                httpConnection.setProxyHost(hostConfiguration.getProxyHost());
                httpConnection.setProxyPort(hostConfiguration.getProxyPort());
            } else {
                finishLastResponse(httpConnection);
            }
        }

        // remove the connection from the timeout handler
        idleStartTime = Long.MAX_VALUE;

        if (inUse) {
            LOG.warn(MISUSE_MESSAGE);
        }
        inUse = true;

        return httpConnection;
    }

    /**
     * @see HttpConnectionManager#getConnection(HostConfiguration, long)
     *
     * @deprecated Use #getConnectionWithTimeout(HostConfiguration, long)
     */
    @Override
    @Deprecated
    public HttpConnection getConnection(final HostConfiguration hostConfiguration, final long timeout) {
        return getConnectionWithTimeout(hostConfiguration, timeout);
    }

    /**
     * @see HttpConnectionManager#releaseConnection(com.microsoft.tfs.core.httpclient.HttpConnection)
     */
    @Override
    public void releaseConnection(final HttpConnection conn) {
        if (conn != httpConnection) {
            throw new IllegalStateException("Unexpected release of an unknown connection.");
        }
        if (alwaysClose) {
            httpConnection.close();
        } else {
            // make sure the connection is reuseable
            finishLastResponse(httpConnection);
        }

        inUse = false;

        // track the time the connection was made idle
        idleStartTime = System.currentTimeMillis();
    }

    /**
     * Returns {@link HttpConnectionManagerParams parameters} associated with
     * this connection manager.
     *
     * @since 2.1
     *
     * @see HttpConnectionManagerParams
     */
    @Override
    public HttpConnectionManagerParams getParams() {
        return params;
    }

    /**
     * Assigns {@link HttpConnectionManagerParams parameters} for this
     * connection manager.
     *
     * @since 2.1
     *
     * @see HttpConnectionManagerParams
     */
    @Override
    public void setParams(final HttpConnectionManagerParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }

    /**
     * @since 3.0
     */
    @Override
    public void closeIdleConnections(final long idleTimeout) {
        final long maxIdleTime = System.currentTimeMillis() - idleTimeout;
        if (idleStartTime <= maxIdleTime) {
            httpConnection.close();
        }
    }

    /**
     * since 3.1
     */
    public void shutdown() {
        httpConnection.close();
    }

}
