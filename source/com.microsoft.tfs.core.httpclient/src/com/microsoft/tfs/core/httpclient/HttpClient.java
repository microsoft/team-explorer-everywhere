/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpClient.java,v 1.98
 * 2004/10/07 16:14:15 olegk Exp $ $Revision: 509577 $ $Date: 2007-02-20
 * 15:28:18 +0100 (Tue, 20 Feb 2007) $
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
import java.security.Provider;
import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.params.HttpClientParams;

/**
 * <p>
 * An HTTP "user-agent", containing an {@link HttpState HTTP state} and one or
 * more {@link HttpConnection HTTP connections}, to which {@link HttpMethod HTTP
 * methods} can be applied.
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author <a href="mailto:rwaldhoff@apache.org">Rodney Waldhoff</a>
 * @author Sean C. Sullivan
 * @author <a href="mailto:dion@apache.org">dIon Gillard</a>
 * @author Ortwin Gl?ck
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author Sam Maloney
 * @author Laura Werner
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 509577 $ $Date: 2007-02-20 15:28:18 +0100 (Tue, 20 Feb
 *          2007) $
 */
public class HttpClient {

    // -------------------------------------------------------------- Constants

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpClient.class);

    static {

        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug("Java version: " + System.getProperty("java.version"));
                LOG.debug("Java vendor: " + System.getProperty("java.vendor"));
                LOG.debug("Java class path: " + System.getProperty("java.class.path"));
                LOG.debug("Operating system name: " + System.getProperty("os.name"));
                LOG.debug("Operating system architecture: " + System.getProperty("os.arch"));
                LOG.debug("Operating system version: " + System.getProperty("os.version"));

                final Provider[] providers = Security.getProviders();
                for (int i = 0; i < providers.length; i++) {
                    final Provider provider = providers[i];
                    LOG.debug(provider.getName() + " " + provider.getVersion() + ": " + provider.getInfo());
                }
            } catch (final SecurityException ignore) {
            }
        }
    }

    // ----------------------------------------------------------- Constructors

    /**
     * Creates an instance of HttpClient using default {@link HttpClientParams
     * parameter set}.
     *
     * @see HttpClientParams
     */
    public HttpClient() {
        this(new HttpClientParams());
    }

    /**
     * Creates an instance of HttpClient using the given {@link HttpClientParams
     * parameter set}.
     *
     * @param params
     *        The {@link HttpClientParams parameters} to use.
     *
     * @see HttpClientParams
     *
     * @since 3.0
     */
    public HttpClient(final HttpClientParams params) {
        super();
        if (params == null) {
            throw new IllegalArgumentException("Params may not be null");
        }
        this.params = params;
        httpConnectionManager = null;
        final Class clazz = params.getConnectionManagerClass();
        if (clazz != null) {
            try {
                httpConnectionManager = (HttpConnectionManager) clazz.newInstance();
            } catch (final Exception e) {
                LOG.warn(
                    "Error instantiating connection manager class, defaulting to" + " SimpleHttpConnectionManager",
                    e);
            }
        }
        if (httpConnectionManager == null) {
            httpConnectionManager = new SimpleHttpConnectionManager();
        }
        if (httpConnectionManager != null) {
            httpConnectionManager.getParams().setDefaults(this.params);
        }
    }

    /**
     * Creates an instance of HttpClient with a user specified
     * {@link HttpClientParams parameter set} and {@link HttpConnectionManager
     * HTTP connection manager}.
     *
     * @param params
     *        The {@link HttpClientParams parameters} to use.
     * @param httpConnectionManager
     *        The {@link HttpConnectionManager connection manager} to use.
     *
     * @since 3.0
     */
    public HttpClient(final HttpClientParams params, final HttpConnectionManager httpConnectionManager) {
        super();
        if (httpConnectionManager == null) {
            throw new IllegalArgumentException("httpConnectionManager cannot be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("Params may not be null");
        }
        this.params = params;
        this.httpConnectionManager = httpConnectionManager;
        this.httpConnectionManager.getParams().setDefaults(this.params);
    }

    /**
     * Creates an instance of HttpClient with a user specified
     * {@link HttpConnectionManager HTTP connection manager}.
     *
     * @param httpConnectionManager
     *        The {@link HttpConnectionManager connection manager} to use.
     *
     * @since 2.0
     */
    public HttpClient(final HttpConnectionManager httpConnectionManager) {
        this(new HttpClientParams(), httpConnectionManager);
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The {@link HttpConnectionManager connection manager} being used to manage
     * connections for this HttpClient
     */
    private HttpConnectionManager httpConnectionManager;

    /**
     * The {@link HttpState HTTP state} associated with this HttpClient.
     */
    private HttpState state = new HttpState();

    /**
     * The {@link HttpClientParams collection of parameters} associated with
     * this HttpClient.
     */
    private HttpClientParams params = null;

    /**
     * The {@link HostConfiguration host configuration} associated with the
     * HttpClient
     */
    private HostConfiguration hostConfiguration = new HostConfiguration();

    // ------------------------------------------------------------- Properties

    /**
     * Returns {@link HttpState HTTP state} associated with the HttpClient.
     *
     * @see #setState(HttpState)
     * @return the shared client state
     */
    public synchronized HttpState getState() {
        return state;
    }

    /**
     * Assigns {@link HttpState HTTP state} for the HttpClient.
     *
     * @see #getState()
     * @param state
     *        the new {@link HttpState HTTP state} for the client
     */
    public synchronized void setState(final HttpState state) {
        this.state = state;
    }

    /**
     * Defines how strictly the method follows the HTTP protocol specification
     * (see RFC 2616 and other relevant RFCs).
     *
     * In the strict mode the method precisely implements the requirements of
     * the specification, whereas in non-strict mode it attempts to mimic the
     * exact behaviour of commonly used HTTP agents, which many HTTP servers
     * expect.
     *
     * @param strictMode
     *        <tt>true</tt> for strict mode, <tt>false</tt> otherwise
     *
     * @see #isStrictMode()
     *
     * @deprecated Use {@link HttpClientParams#setParameter(String, Object)} to
     *             exercise a more granular control over HTTP protocol
     *             strictness.
     */
    @Deprecated
    public synchronized void setStrictMode(final boolean strictMode) {
        if (strictMode) {
            params.makeStrict();
        } else {
            params.makeLenient();
        }
    }

    /**
     * Returns the value of the strict mode flag.
     *
     * @return <tt>true</tt> if strict mode is enabled, <tt>false</tt> otherwise
     *
     * @see #setStrictMode(boolean)
     *
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpClientParams#getParameter(String)}
     *             to exercise a more granular control over HTTP protocol
     *             strictness.
     */
    @Deprecated
    public synchronized boolean isStrictMode() {
        return false;
    }

    /**
     * Sets the socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds which is
     * the timeout for waiting for data. A timeout value of zero is interpreted
     * as an infinite timeout.
     *
     * @param newTimeoutInMilliseconds
     *        Timeout in milliseconds
     *
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpConnectionManagerParams#setSoTimeout(int)}
     *             , {@link HttpConnectionManager#getParams()}.
     *
     */
    @Deprecated
    public synchronized void setTimeout(final int newTimeoutInMilliseconds) {
        params.setSoTimeout(newTimeoutInMilliseconds);
    }

    /**
     * Sets the timeout in milliseconds used when retrieving an
     * {@link HttpConnection HTTP connection} from the
     * {@link HttpConnectionManager HTTP connection manager}.
     *
     * @param timeout
     *        the timeout in milliseconds
     *
     * @see HttpConnectionManager#getConnection(HostConfiguration, long)
     *
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpClientParams#setConnectionManagerTimeout(long)}
     *             , {@link HttpClient#getParams()}
     */
    @Deprecated
    public synchronized void setHttpConnectionFactoryTimeout(final long timeout) {
        params.setConnectionManagerTimeout(timeout);
    }

    /**
     * Sets the timeout until a connection is etablished. A value of zero means
     * the timeout is not used. The default value is zero.
     *
     * @see HttpConnection#setConnectionTimeout(int)
     * @param newTimeoutInMilliseconds
     *        Timeout in milliseconds.
     *
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpConnectionManagerParams#setConnectionTimeout(int)}
     *             , {@link HttpConnectionManager#getParams()}.
     */
    @Deprecated
    public synchronized void setConnectionTimeout(final int newTimeoutInMilliseconds) {
        httpConnectionManager.getParams().setConnectionTimeout(newTimeoutInMilliseconds);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Executes the given {@link HttpMethod HTTP method}.
     *
     * @param method
     *        the {@link HttpMethod HTTP method} to execute.
     * @return the method's response code
     *
     * @throws IOException
     *         If an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         If a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    public int executeMethod(final HttpMethod method) throws IOException, HttpException {

        LOG.trace("enter HttpClient.executeMethod(HttpMethod)");
        // execute this method and use its host configuration, if it has one
        return executeMethod(null, method, null);
    }

    /**
     * Executes the given {@link HttpMethod HTTP method} using custom
     * {@link HostConfiguration host configuration}.
     *
     * @param hostConfiguration
     *        The {@link HostConfiguration host configuration} to use. If
     *        <code>null</code>, the host configuration returned by
     *        {@link #getHostConfiguration} will be used.
     * @param method
     *        the {@link HttpMethod HTTP method} to execute.
     * @return the method's response code
     *
     * @throws IOException
     *         If an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         If a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     * @since 2.0
     */
    public int executeMethod(final HostConfiguration hostConfiguration, final HttpMethod method)
        throws IOException,
            HttpException {

        LOG.trace("enter HttpClient.executeMethod(HostConfiguration,HttpMethod)");

        return executeMethod(hostConfiguration, method, null);
    }

    /**
     * Executes the given {@link HttpMethod HTTP method} using the given custom
     * {@link HostConfiguration host configuration} with the given custom
     * {@link HttpState HTTP state}.
     *
     * @param hostconfig
     *        The {@link HostConfiguration host configuration} to use. If
     *        <code>null</code>, the host configuration returned by
     *        {@link #getHostConfiguration} will be used.
     * @param method
     *        the {@link HttpMethod HTTP method} to execute.
     * @param state
     *        the {@link HttpState HTTP state} to use when executing the method.
     *        If <code>null</code>, the state returned by {@link #getState} will
     *        be used.
     *
     * @return the method's response code
     *
     * @throws IOException
     *         If an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         If a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     * @since 2.0
     */
    public int executeMethod(HostConfiguration hostconfig, final HttpMethod method, final HttpState state)
        throws IOException,
            HttpException {

        LOG.trace("enter HttpClient.executeMethod(HostConfiguration,HttpMethod,HttpState)");

        if (method == null) {
            throw new IllegalArgumentException("HttpMethod parameter may not be null");
        }
        final HostConfiguration defaulthostconfig = getHostConfiguration();
        if (hostconfig == null) {
            hostconfig = defaulthostconfig;
        }
        final URI uri = method.getURI();
        if (hostconfig == defaulthostconfig || uri.isAbsoluteURI()) {
            // make a deep copy of the host defaults
            hostconfig = (HostConfiguration) hostconfig.clone();
            if (uri.isAbsoluteURI()) {
                hostconfig.setHost(uri);
            }
        }

        final HttpMethodDirector methodDirector = new HttpMethodDirector(
            getHttpConnectionManager(),
            hostconfig,
            params,
            (state == null ? getState() : state));
        methodDirector.executeMethod(method);
        return method.getStatusCode();
    }

    /**
     * Returns the default host.
     *
     * @return The default host.
     *
     * @deprecated use #getHostConfiguration()
     */
    @Deprecated
    public String getHost() {
        return hostConfiguration.getHost();
    }

    /**
     * Returns the default port.
     *
     * @return The default port.
     *
     * @deprecated use #getHostConfiguration()
     */
    @Deprecated
    public int getPort() {
        return hostConfiguration.getPort();
    }

    /**
     * Returns the {@link HostConfiguration host configuration} associated with
     * the HttpClient.
     *
     * @return {@link HostConfiguration host configuration}
     *
     * @since 2.0
     */
    public synchronized HostConfiguration getHostConfiguration() {
        return hostConfiguration;
    }

    /**
     * Assigns the {@link HostConfiguration host configuration} to use with the
     * HttpClient.
     *
     * @param hostConfiguration
     *        The {@link HostConfiguration host configuration} to set
     *
     * @since 2.0
     */
    public synchronized void setHostConfiguration(final HostConfiguration hostConfiguration) {
        this.hostConfiguration = hostConfiguration;
    }

    /**
     * Returns the {@link HttpConnectionManager HTTP connection manager}
     * associated with the HttpClient.
     *
     * @return {@link HttpConnectionManager HTTP connection manager}
     *
     * @since 2.0
     */
    public synchronized HttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }

    /**
     * Assigns the {@link HttpConnectionManager HTTP connection manager} to use
     * with the HttpClient.
     *
     * @param httpConnectionManager
     *        The {@link HttpConnectionManager HTTP connection manager} to set
     *
     * @since 2.0
     */
    public synchronized void setHttpConnectionManager(final HttpConnectionManager httpConnectionManager) {
        this.httpConnectionManager = httpConnectionManager;
        if (this.httpConnectionManager != null) {
            this.httpConnectionManager.getParams().setDefaults(params);
        }
    }

    /**
     * Returns {@link HttpClientParams HTTP protocol parameters} associated with
     * this HttpClient.
     *
     * @since 3.0
     *
     * @see HttpClientParams
     */
    public HttpClientParams getParams() {
        return params;
    }

    /**
     * Assigns {@link HttpClientParams HTTP protocol parameters} for this
     * HttpClient.
     *
     * @since 3.0
     *
     * @see HttpClientParams
     */
    public void setParams(final HttpClientParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }

}
