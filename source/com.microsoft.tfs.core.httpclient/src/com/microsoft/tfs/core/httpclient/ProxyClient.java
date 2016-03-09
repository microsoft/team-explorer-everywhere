/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/ProxyClient.java,v 1.5
 * 2004/12/20 11:39:04 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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
import java.net.Socket;

import com.microsoft.tfs.core.httpclient.params.HttpClientParams;
import com.microsoft.tfs.core.httpclient.params.HttpConnectionManagerParams;
import com.microsoft.tfs.core.httpclient.params.HttpParams;

/**
 * A client that provides {@link java.net.Socket sockets} for communicating
 * through HTTP proxies via the HTTP CONNECT method. This is primarily needed
 * for non-HTTP protocols that wish to communicate via an HTTP proxy.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Michael Becke
 *
 * @since 3.0
 *
 * @version $Revision: 480424 $
 */
public class ProxyClient {

    // ----------------------------------------------------- Instance Variables

    /**
     * The {@link HttpState HTTP state} associated with this ProxyClient.
     */
    private HttpState state = new HttpState();

    /**
     * The {@link HttpClientParams collection of parameters} associated with
     * this ProxyClient.
     */
    private HttpClientParams params = null;

    /**
     * The {@link HostConfiguration host configuration} associated with the
     * ProxyClient
     */
    private HostConfiguration hostConfiguration = new HostConfiguration();

    /**
     * Creates an instance of ProxyClient using default {@link HttpClientParams
     * parameter set}.
     *
     * @see HttpClientParams
     */
    public ProxyClient() {
        this(new HttpClientParams());
    }

    /**
     * Creates an instance of ProxyClient using the given
     * {@link HttpClientParams parameter set}.
     *
     * @param params
     *        The {@link HttpClientParams parameters} to use.
     *
     * @see HttpClientParams
     */
    public ProxyClient(final HttpClientParams params) {
        super();
        if (params == null) {
            throw new IllegalArgumentException("Params may not be null");
        }
        this.params = params;
    }

    // ------------------------------------------------------------- Properties

    /**
     * Returns {@link HttpState HTTP state} associated with the ProxyClient.
     *
     * @see #setState(HttpState)
     * @return the shared client state
     */
    public synchronized HttpState getState() {
        return state;
    }

    /**
     * Assigns {@link HttpState HTTP state} for the ProxyClient.
     *
     * @see #getState()
     * @param state
     *        the new {@link HttpState HTTP state} for the client
     */
    public synchronized void setState(final HttpState state) {
        this.state = state;
    }

    /**
     * Returns the {@link HostConfiguration host configuration} associated with
     * the ProxyClient.
     *
     * @return {@link HostConfiguration host configuration}
     */
    public synchronized HostConfiguration getHostConfiguration() {
        return hostConfiguration;
    }

    /**
     * Assigns the {@link HostConfiguration host configuration} to use with the
     * ProxyClient.
     *
     * @param hostConfiguration
     *        The {@link HostConfiguration host configuration} to set
     */
    public synchronized void setHostConfiguration(final HostConfiguration hostConfiguration) {
        this.hostConfiguration = hostConfiguration;
    }

    /**
     * Returns {@link HttpClientParams HTTP protocol parameters} associated with
     * this ProxyClient.
     *
     * @see HttpClientParams
     */
    public synchronized HttpClientParams getParams() {
        return params;
    }

    /**
     * Assigns {@link HttpClientParams HTTP protocol parameters} for this
     * ProxyClient.
     *
     * @see HttpClientParams
     */
    public synchronized void setParams(final HttpClientParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }

    /**
     * Creates a socket that is connected, via the HTTP CONNECT method, to a
     * proxy.
     *
     * <p>
     * Even though HTTP CONNECT proxying is generally used for HTTPS tunneling,
     * the returned socket will not have been wrapped in an SSL socket.
     * </p>
     *
     * <p>
     * Both the proxy and destination hosts must be set via the
     * {@link #getHostConfiguration() host configuration} prior to calling this
     * method.
     * </p>
     *
     * @return the connect response
     *
     * @throws IOException
     * @throws HttpException
     *
     * @see #getHostConfiguration()
     */
    public ConnectResponse connect() throws IOException, HttpException {

        final HostConfiguration hostconf = getHostConfiguration();
        if (hostconf.getProxyHost() == null) {
            throw new IllegalStateException("proxy host must be configured");
        }
        if (hostconf.getHost() == null) {
            throw new IllegalStateException("destination host must be configured");
        }
        if (hostconf.getProtocol().isSecure()) {
            throw new IllegalStateException("secure protocol socket factory may not be used");
        }

        final ConnectMethod method = new ConnectMethod(getHostConfiguration());
        method.getParams().setDefaults(getParams());

        final DummyConnectionManager connectionManager = new DummyConnectionManager();
        connectionManager.setConnectionParams(getParams());

        final HttpMethodDirector director =
            new HttpMethodDirector(connectionManager, hostconf, getParams(), getState());

        director.executeMethod(method);

        final ConnectResponse response = new ConnectResponse();
        response.setConnectMethod(method);

        // only set the socket if the connect was successful
        if (method.getStatusCode() == HttpStatus.SC_OK) {
            response.setSocket(connectionManager.getConnection().getSocket());
        } else {
            connectionManager.getConnection().close();
        }

        return response;
    }

    /**
     * Contains the method used to execute the connect along with the created
     * socket.
     */
    public static class ConnectResponse {

        private ConnectMethod connectMethod;

        private Socket socket;

        private ConnectResponse() {
        }

        /**
         * Gets the method that was used to execute the connect. This method is
         * useful for analyzing the proxy's response when a connect fails.
         *
         * @return the connectMethod.
         */
        public ConnectMethod getConnectMethod() {
            return connectMethod;
        }

        /**
         * @param connectMethod
         *        The connectMethod to set.
         */
        private void setConnectMethod(final ConnectMethod connectMethod) {
            this.connectMethod = connectMethod;
        }

        /**
         * Gets the socket connected and authenticated (if appropriate) to the
         * configured HTTP proxy, or <code>null</code> if a connection could not
         * be made. It is the responsibility of the user to close this socket
         * when it is no longer needed.
         *
         * @return the socket.
         */
        public Socket getSocket() {
            return socket;
        }

        /**
         * @param socket
         *        The socket to set.
         */
        private void setSocket(final Socket socket) {
            this.socket = socket;
        }
    }

    /**
     * A connection manager that creates a single connection. Meant to be used
     * only once.
     */
    static class DummyConnectionManager implements HttpConnectionManager {

        private HttpConnection httpConnection;

        private HttpParams connectionParams;

        @Override
        public void closeIdleConnections(final long idleTimeout) {
        }

        public HttpConnection getConnection() {
            return httpConnection;
        }

        public void setConnectionParams(final HttpParams httpParams) {
            connectionParams = httpParams;
        }

        @Override
        public HttpConnection getConnectionWithTimeout(final HostConfiguration hostConfiguration, final long timeout) {

            httpConnection = new HttpConnection(hostConfiguration);
            httpConnection.setHttpConnectionManager(this);
            httpConnection.getParams().setDefaults(connectionParams);
            return httpConnection;
        }

        /**
         * @deprecated
         */
        @Override
        @Deprecated
        public HttpConnection getConnection(final HostConfiguration hostConfiguration, final long timeout)
            throws HttpException {
            return getConnectionWithTimeout(hostConfiguration, timeout);
        }

        @Override
        public HttpConnection getConnection(final HostConfiguration hostConfiguration) {
            return getConnectionWithTimeout(hostConfiguration, -1);
        }

        @Override
        public void releaseConnection(final HttpConnection conn) {
        }

        @Override
        public HttpConnectionManagerParams getParams() {
            return null;
        }

        @Override
        public void setParams(final HttpConnectionManagerParams params) {
        }
    }
}
