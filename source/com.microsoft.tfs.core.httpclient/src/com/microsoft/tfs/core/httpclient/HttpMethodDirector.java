/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpMethodDirector.java,v
 * 1.34 2005/01/14 19:40:39 olegk Exp $ $Revision: 486658 $ $Date: 2006-12-13
 * 15:05:50 +0100 (Wed, 13 Dec 2006) $
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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.auth.AuthChallengeException;
import com.microsoft.tfs.core.httpclient.auth.AuthChallengeParser;
import com.microsoft.tfs.core.httpclient.auth.AuthChallengeProcessor;
import com.microsoft.tfs.core.httpclient.auth.AuthScheme;
import com.microsoft.tfs.core.httpclient.auth.AuthScope;
import com.microsoft.tfs.core.httpclient.auth.AuthState;
import com.microsoft.tfs.core.httpclient.auth.AuthenticationException;
import com.microsoft.tfs.core.httpclient.auth.AuthorizationHeaderScheme;
import com.microsoft.tfs.core.httpclient.auth.CredentialsNotAvailableException;
import com.microsoft.tfs.core.httpclient.auth.CredentialsProvider;
import com.microsoft.tfs.core.httpclient.auth.MalformedChallengeException;
import com.microsoft.tfs.core.httpclient.params.HostParams;
import com.microsoft.tfs.core.httpclient.params.HttpClientParams;
import com.microsoft.tfs.core.httpclient.params.HttpConnectionParams;
import com.microsoft.tfs.core.httpclient.params.HttpMethodParams;
import com.microsoft.tfs.core.httpclient.params.HttpParams;

/**
 * Handles the process of executing a method including authentication,
 * redirection and retries.
 *
 * @since 3.0
 */
class HttpMethodDirector {
    private static final Log LOG = LogFactory.getLog(HttpMethodDirector.class);

    private ConnectMethod connectMethod;

    private final HttpState state;

    private final HostConfiguration hostConfiguration;

    private final HttpConnectionManager connectionManager;

    private final HttpClientParams params;

    private HttpConnection conn;

    /**
     * A flag to indicate if the connection should be released after the method
     * is executed.
     */
    private boolean releaseConnection = false;

    /** Authentication processor */
    private AuthChallengeProcessor authProcessor = null;

    private Set redirectLocations = null;

    public HttpMethodDirector(
        final HttpConnectionManager connectionManager,
        final HostConfiguration hostConfiguration,
        final HttpClientParams params,
        final HttpState state) {
        super();
        this.connectionManager = connectionManager;
        this.hostConfiguration = hostConfiguration;
        this.params = params;
        this.state = state;
        authProcessor = new AuthChallengeProcessor(this.params);
    }

    /**
     * Executes the method associated with this method director.
     *
     * @throws IOException
     * @throws HttpException
     */
    public void executeMethod(final HttpMethod method) throws IOException, HttpException {
        if (method == null) {
            throw new IllegalArgumentException("Method may not be null");
        }
        // Link all parameter collections to form the hierarchy:
        // Global -> HttpClient -> HostConfiguration -> HttpMethod
        hostConfiguration.getParams().setDefaults(params);
        method.getParams().setDefaults(hostConfiguration.getParams());

        // Generate default request headers
        final Collection defaults = (Collection) hostConfiguration.getParams().getParameter(HostParams.DEFAULT_HEADERS);
        if (defaults != null) {
            final Iterator i = defaults.iterator();
            while (i.hasNext()) {
                method.addRequestHeader((Header) i.next());
            }
        }

        try {
            final int maxRedirects = params.getIntParameter(HttpClientParams.MAX_REDIRECTS, 100);

            for (int redirectCount = 0;;) {
                // make sure the connection we have is appropriate
                if (conn != null && !hostConfiguration.hostEquals(conn)) {
                    conn.setLocked(false);
                    conn.releaseConnection();
                    conn = null;
                }

                // get a connection, if we need one
                if (conn == null) {
                    conn = connectionManager.getConnectionWithTimeout(
                        hostConfiguration,
                        params.getConnectionManagerTimeout());

                    conn.setLocked(true);

                    final Credentials preemptiveCredentials = getPreemptiveCredentials(conn.getHost(), conn.getPort());

                    if (preemptiveCredentials != null && method.getDoAuthentication()) {
                        method.getHostAuthState().setPreemptive(preemptiveCredentials);
                        method.getHostAuthState().setAuthAttempted(true);

                        /*
                         * Disabled. HttpClient used to force
                         * pre-emptive auth to the proxy if it was enabled for
                         * the host, but this doesn't work when the pre-emptive
                         * auth uses the Authorization header. Disabling will
                         * prevent Basic auth to proxies, which TEE never
                         * supported in the past because we never enabled
                         * pre-emptive authentication before Dev11.
                         */
                        // if (conn.isProxied() && !conn.isSecure())
                        // {
                        // Credentials preemptiveProxyCredentials =
                        // getPreemptiveCredentials(conn.getProxyHost(),
                        // conn.getProxyPort());
                        //
                        // if (preemptiveProxyCredentials != null)
                        // {
                        // method.getProxyAuthState().setPreemptive(preemptiveProxyCredentials);
                        // method.getProxyAuthState().setAuthAttempted(true);
                        // }
                        // }
                    }
                }

                if (method.getDoAuthentication()) {
                    authenticate(method);
                }

                executeWithRetry(method);
                if (connectMethod != null) {
                    fakeResponse(method);
                    break;
                }

                boolean retry = false;
                if (isRedirectNeeded(method)) {
                    if (processRedirectResponse(method)) {
                        retry = true;
                        ++redirectCount;
                        if (redirectCount >= maxRedirects) {
                            LOG.error("Narrowly avoided an infinite loop in execute");
                            throw new RedirectException("Maximum redirects (" + maxRedirects + ") exceeded");
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Execute redirect " + redirectCount + " of " + maxRedirects);
                        }
                    }
                }
                if (isAuthenticationNeeded(method)) {
                    if (processAuthenticationResponse(method)) {
                        LOG.debug("Retry authentication");
                        retry = true;
                    }
                }
                if (!retry) {
                    break;
                }
                // retry - close previous stream. Caution - this causes
                // responseBodyConsumed to be called, which may also close the
                // connection.
                if (method.getResponseBodyAsStream() != null) {
                    method.getResponseBodyAsStream().close();
                }

            } // end of retry loop
        } finally {
            if (conn != null) {
                conn.setLocked(false);
            }
            // If the response has been fully processed, return the connection
            // to the pool. Use this flag, rather than other tests (like
            // responseStream == null), as subclasses, might reset the stream,
            // for example, reading the entire response into a file and then
            // setting the file as the stream.
            if ((releaseConnection || method.getResponseBodyAsStream() == null) && conn != null) {
                conn.releaseConnection();
            }
        }
    }

    private Credentials getPreemptiveCredentials(final String host, final int port) {
        if (host == null) {
            return null;
        }

        /*
         * See what credential types are accepted for preemptive authentication
         */
        final Class[] preemptiveTypes = params.getPreemptiveAuthenticationTypes();

        if (preemptiveTypes == null) {
            return null;
        }

        /* Get the credentials for this host/port */
        final AuthScope authScope = new AuthScope(host, port);

        LOG.debug("Examining preemptive credentials for " + host + ":" + port);

        final Credentials credentials = state.getCredentials(authScope);

        if (credentials == null) {
            return null;
        }

        /* See if these are one of the preemptive credential types supported */
        for (int i = 0; i < preemptiveTypes.length; i++) {
            if (preemptiveTypes[i].isInstance(credentials)) {
                return credentials;
            }
        }

        return null;
    }

    private void authenticate(final HttpMethod method) {
        try {
            if (conn.isProxied() && !conn.isSecure()) {
                authenticateProxy(method);
            }
            authenticateHost(method);
        } catch (final AuthenticationException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private boolean cleanAuthHeaders(final HttpMethod method, final String name) {
        final Header[] authheaders = method.getRequestHeaders(name);
        boolean clean = true;
        for (int i = 0; i < authheaders.length; i++) {
            final Header authheader = authheaders[i];
            if (authheader.isAutogenerated()) {
                method.removeRequestHeader(authheader);
            } else {
                clean = false;
            }
        }
        return clean;
    }

    private void authenticateHost(final HttpMethod method) throws AuthenticationException {
        // Clean up existing authentication headers
        if (!cleanAuthHeaders(method, AuthorizationHeaderScheme.HOST_RESPONSE_HEADER)) {
            // User defined authentication header(s) present
            return;
        }
        final AuthState authstate = method.getHostAuthState();
        final AuthScheme authscheme = authstate.getAuthScheme();
        if (authscheme == null) {
            return;
        }
        if (authstate.isAuthRequested() || !authscheme.isConnectionBased()) {
            String host = method.getParams().getVirtualHost();
            if (host == null) {
                host = conn.getHost();
            }
            final int port = conn.getPort();
            final AuthScope authscope = new AuthScope(host, port, authscheme.getSchemeName());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticating with " + authscope);
            }
            final Credentials credentials = state.getCredentials(authscope);
            if (credentials != null) {
                authscheme.authenticateHost(authscope, credentials, state, method);
            } else {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Required credentials not available for " + authscope);
                    if (method.getHostAuthState().isPreemptive()) {
                        LOG.warn("Preemptive authentication requested but no default " + "credentials available");
                    }
                }
            }
        }
    }

    private void authenticateProxy(final HttpMethod method) throws AuthenticationException {
        // Clean up existing authentication headers
        if (!cleanAuthHeaders(method, AuthorizationHeaderScheme.PROXY_RESPONSE_HEADER)) {
            // User defined authentication header(s) present
            return;
        }
        final AuthState authstate = method.getProxyAuthState();
        final AuthScheme authscheme = authstate.getAuthScheme();
        if (authscheme == null) {
            return;
        }
        if (authstate.isAuthRequested() || !authscheme.isConnectionBased()) {
            final AuthScope authscope =
                new AuthScope(conn.getProxyHost(), conn.getProxyPort(), authscheme.getSchemeName());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Authenticating with " + authscope);
            }

            final Credentials credentials = state.getProxyCredentials(authscope);

            if (credentials != null && authscheme.supportsCredentials(credentials)) {
                authscheme.authenticateProxy(authscope, credentials, state, method);
            } else {
                if (method.getProxyAuthState().isPreemptive()) {
                    LOG.debug("Preemptive authentication requested but no default proxy credentials available");
                } else {
                    LOG.warn("Required proxy credentials not available for " + authscope);
                }
            }
        }
    }

    /**
     * Applies connection parameters specified for a given method
     *
     * @param method
     *        HTTP method
     *
     * @throws IOException
     *         if an I/O occurs setting connection parameters
     */
    private void applyConnectionParams(final HttpMethod method) throws IOException {
        int timeout = 0;
        // see if a timeout is given for this method
        Object param = method.getParams().getParameter(HttpMethodParams.SO_TIMEOUT);
        if (param == null) {
            // if not, use the default value
            param = conn.getParams().getParameter(HttpConnectionParams.SO_TIMEOUT);
        }
        if (param != null) {
            timeout = ((Integer) param).intValue();
        }
        conn.setSocketTimeout(timeout);
    }

    /**
     * Executes a method with the current hostConfiguration.
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    private void executeWithRetry(final HttpMethod method) throws IOException, HttpException {

        /**
         * How many times did this transparently handle a recoverable exception?
         */
        int execCount = 0;
        // loop until the method is successfully processed, the retryHandler
        // returns false or a non-recoverable exception is thrown
        try {
            while (true) {
                execCount++;
                try {

                    if (LOG.isTraceEnabled()) {
                        LOG.trace(
                            "Attempt number " + execCount + " to process request with HTTPConnection " + conn.getID());
                    }
                    if (conn.getParams().isStaleCheckingEnabled()) {
                        conn.closeIfStale();
                    }
                    if (!conn.isOpen()) {
                        // this connection must be opened before it can be used
                        // This has nothing to do with opening a secure tunnel
                        conn.open();
                        if (conn.isProxied() && conn.isSecure() && !(method instanceof ConnectMethod)) {
                            // we need to create a secure tunnel before we can
                            // execute the real method
                            if (!executeConnect()) {
                                // abort, the connect method failed
                                LOG.debug(
                                    "Connection openinig filed",
                                    new Exception("Fake exception created just to print out the call stack. Ignore."));
                                return;
                            }
                        }
                    }
                    applyConnectionParams(method);
                    method.execute(state, conn);
                    break;
                } catch (final HttpException e) {
                    LOG.error("Protocol exception: ", e);
                    // filter out protocol exceptions which cannot be recovered
                    // from
                    throw e;
                } catch (final IOException e) {
                    LOG.debug("Closing the connection.");
                    conn.close();
                    // test if this method should be retried
                    // ========================================
                    // this code is provided for backward compatibility with 2.0
                    // will be removed in the next major release
                    if (method instanceof HttpMethodBase) {
                        final MethodRetryHandler handler = ((HttpMethodBase) method).getMethodRetryHandler();
                        if (handler != null) {
                            if (!handler.retryMethod(
                                method,
                                conn,
                                new HttpRecoverableException(e.getMessage()),
                                execCount,
                                method.isRequestSent())) {
                                LOG.debug(
                                    "Method retry handler returned false. "
                                        + "Automatic recovery will not be attempted");
                                throw e;
                            }
                        }
                    }
                    // ========================================
                    HttpMethodRetryHandler handler =
                        (HttpMethodRetryHandler) method.getParams().getParameter(HttpMethodParams.RETRY_HANDLER);
                    if (handler == null) {
                        handler = new DefaultHttpMethodRetryHandler();
                    }
                    if (!handler.retryMethod(method, e, execCount)) {
                        LOG.debug("Method retry handler returned false. " + "Automatic recovery will not be attempted");
                        throw e;
                    }
                    if (LOG.isInfoEnabled()) {
                        LOG.info(
                            "I/O exception ("
                                + e.getClass().getName()
                                + ") caught when processing request: "
                                + e.getMessage());
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(e.getMessage(), e);
                    }

                    final long delayTime = (10000 * (long) Math.pow(2, execCount - 1));
                    LOG.info("Retrying request in " + delayTime + " ms");
                    try {
                        Thread.sleep(delayTime);
                    } catch (final InterruptedException ex) {
                        throw e;
                    }
                }
            }
        } catch (final IOException e) {
            if (conn.isOpen()) {
                LOG.debug("Closing the connection.");
                conn.close();
            }
            releaseConnection = true;
            throw e;
        } catch (final RuntimeException e) {
            if (conn.isOpen()) {
                LOG.debug("Closing the connection.");
                conn.close();
            }
            releaseConnection = true;
            throw e;
        }
    }

    /**
     * Executes a ConnectMethod to establish a tunneled connection.
     *
     * @return <code>true</code> if the connect was successful
     *
     * @throws IOException
     * @throws HttpException
     */
    private boolean executeConnect() throws IOException, HttpException {
        connectMethod = new ConnectMethod(hostConfiguration);
        connectMethod.getParams().setDefaults(hostConfiguration.getParams());

        int code;
        for (;;) {
            if (!conn.isOpen()) {
                conn.open();
            }

            /*
             * Do not do preemptive proxy credentials. This only applies to
             * Basic and Digest authentication methods, which we have
             * historically not supported for proxy credentials. (We have
             * historically only supported NTLM proxies.)
             *
             * See also executeMethod() above for preemptive proxy
             * authentication for HTTP requests.
             */

            // @formatter:off
            /*
            Credentials preemptiveCredentials = getPreemptiveCredentials(conn.getProxyHost(), conn.getProxyPort());

            if (preemptiveCredentials != null)
            {
                LOG.debug("Preemptively sending default proxy credentials");

                connectMethod.getProxyAuthState().setPreemptive(preemptiveCredentials);
                connectMethod.getProxyAuthState().setAuthAttempted(true);
            }
            */
            // @formatter:on

            try {
                authenticateProxy(connectMethod);
            } catch (final AuthenticationException e) {
                LOG.error(e.getMessage(), e);
            }

            applyConnectionParams(connectMethod);
            connectMethod.execute(state, conn);
            code = connectMethod.getStatusCode();
            boolean retry = false;
            final AuthState authstate = connectMethod.getProxyAuthState();
            authstate.setAuthRequested(code == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED);
            if (authstate.isAuthRequested()) {
                if (processAuthenticationResponse(connectMethod)) {
                    retry = true;
                }
            }
            if (!retry) {
                break;
            }
            if (connectMethod.getResponseBodyAsStream() != null) {
                connectMethod.getResponseBodyAsStream().close();
            }
        }
        if ((code >= 200) && (code < 300)) {
            conn.tunnelCreated();
            // Drop the connect method, as it is no longer needed
            connectMethod = null;
            return true;
        } else {
            conn.close();
            return false;
        }
    }

    /**
     * Fake response
     *
     * @param method
     * @return
     */

    private void fakeResponse(final HttpMethod method) throws IOException, HttpException {
        // What is to follow is an ugly hack.
        // I REALLY hate having to resort to such
        // an appalling trick
        // The only feasible solution is to split monolithic
        // HttpMethod into HttpRequest/HttpResponse pair.
        // That would allow to execute CONNECT method
        // behind the scene and return CONNECT HttpResponse
        // object in response to the original request that
        // contains the correct status line, headers &
        // response body.
        LOG.debug("CONNECT failed, fake the response for the original method");
        // Pass the status, headers and response stream to the wrapped
        // method.
        // To ensure that the connection is not released more than once
        // this method is still responsible for releasing the connection.
        // This will happen when the response body is consumed, or when
        // the wrapped method closes the response connection in
        // releaseConnection().
        if (method instanceof HttpMethodBase) {
            ((HttpMethodBase) method).fakeResponse(
                connectMethod.getStatusLine(),
                connectMethod.getResponseHeaderGroup(),
                connectMethod.getResponseBodyAsStream());
            method.getProxyAuthState().setAuthScheme(connectMethod.getProxyAuthState().getAuthScheme());
            connectMethod = null;
        } else {
            releaseConnection = true;
            LOG.warn("Unable to fake response on method as it is not derived from HttpMethodBase.");
        }
    }

    /**
     * Process the redirect response.
     *
     * @return <code>true</code> if the redirect was successful
     */
    private boolean processRedirectResponse(final HttpMethod method) throws RedirectException {
        // get the location header to find out where to redirect to
        final Header locationHeader = method.getResponseHeader("location");
        if (locationHeader == null) {
            // got a redirect response, but no location header
            LOG.error("Received redirect response " + method.getStatusCode() + " but no location header");
            return false;
        }
        final String location = locationHeader.getValue();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Redirect requested to location '" + location + "'");
        }

        // rfc2616 demands the location value be a complete URI
        // Location = "Location" ":" absoluteURI
        URI redirectUri = null;
        URI currentUri = null;

        try {
            currentUri =
                new URI(conn.getProtocol().getScheme(), null, conn.getHost(), conn.getPort(), method.getPath());

            final String charset = method.getParams().getUriCharset();
            redirectUri = new URI(location, true, charset);

            if (redirectUri.isRelativeURI()) {
                if (params.isParameterTrue(HttpClientParams.REJECT_RELATIVE_REDIRECT)) {
                    LOG.warn("Relative redirect location '" + location + "' not allowed");
                    return false;
                } else {
                    // location is incomplete, use current values for defaults
                    LOG.debug("Redirect URI is not absolute - parsing as relative");
                    redirectUri = new URI(currentUri, redirectUri);
                }
            } else {
                // Reset the default params
                method.getParams().setDefaults(params);
            }
            method.setURI(redirectUri);
            hostConfiguration.setHost(redirectUri);
        } catch (final URIException ex) {
            throw new InvalidRedirectLocationException("Invalid redirect location: " + location, location, ex);
        }

        if (params.isParameterFalse(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS)) {
            if (redirectLocations == null) {
                redirectLocations = new HashSet();
            }
            redirectLocations.add(currentUri);
            try {
                if (redirectUri.hasQuery()) {
                    redirectUri.setQuery(null);
                }
            } catch (final URIException e) {
                // Should never happen
                return false;
            }

            if (redirectLocations.contains(redirectUri)) {
                throw new CircularRedirectException("Circular redirect to '" + redirectUri + "'");
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Redirecting from '" + currentUri.getEscapedURI() + "' to '" + redirectUri.getEscapedURI());
        }
        // And finally invalidate the actual authentication scheme
        method.getHostAuthState().invalidate();
        return true;
    }

    /**
     * Processes a response that requires authentication
     *
     * @param method
     *        the current {@link HttpMethod HTTP method}
     *
     * @return <tt>true</tt> if the authentication challenge can be responsed
     *         to, (that is, at least one of the requested authentication scheme
     *         is supported, and matching credentials have been found),
     *         <tt>false</tt> otherwise.
     */
    private boolean processAuthenticationResponse(final HttpMethod method) {
        LOG.trace("enter HttpMethodBase.processAuthenticationResponse(" + "HttpState, HttpConnection)");

        try {
            switch (method.getStatusCode()) {
                case HttpStatus.SC_UNAUTHORIZED:
                    return processWWWAuthChallenge(method);
                case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                    return processProxyAuthChallenge(method);
                default:
                    return false;
            }
        } catch (final Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            return false;
        }
    }

    private boolean processWWWAuthChallenge(final HttpMethod method)
        throws MalformedChallengeException,
            AuthenticationException {
        final AuthState authstate = method.getHostAuthState();

        final Map<String, String> challenges = AuthChallengeParser.parseChallenges(
            method.getResponseHeaders(AuthorizationHeaderScheme.HOST_CHALLENGE_HEADER));

        if (challenges.isEmpty()) {
            LOG.debug("Authentication challenge(s) not found");
            return false;
        }

        final Credentials globalCredentials = state.getCredentials(AuthScope.ANY);

        AuthScheme authscheme = null;
        try {
            authscheme = authProcessor.processChallenge(authstate, challenges, globalCredentials);
        } catch (final AuthChallengeException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage());
            }
        }
        if (authscheme == null) {
            return false;
        }
        String host = method.getParams().getVirtualHost();
        if (host == null) {
            host = conn.getHost();
        }
        final int port = conn.getPort();
        final AuthScope authscope = new AuthScope(host, port, authscheme.getSchemeName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Authentication scope: " + authscope);
        }
        if (authstate.isAuthAttempted() && authscheme.isComplete()) {
            // Already tried and failed
            final Credentials credentials = promptForCredentials(authscheme, method.getParams(), authscope);
            if (credentials == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Failure authenticating with " + authscope);
                }
                return false;
            } else {
                return true;
            }
        } else {
            authstate.setAuthAttempted(true);
            Credentials credentials = state.getCredentials(authscope);
            if (credentials == null) {
                credentials = promptForCredentials(authscheme, method.getParams(), authscope);
            }
            if (credentials == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("No credentials available for " + authscope);
                }
                return false;
            } else {
                return true;
            }
        }
    }

    private boolean processProxyAuthChallenge(final HttpMethod method)
        throws MalformedChallengeException,
            AuthenticationException {
        final AuthState authstate = method.getProxyAuthState();

        final Map<String, String> proxyChallenges = AuthChallengeParser.parseChallenges(
            method.getResponseHeaders(AuthorizationHeaderScheme.PROXY_CHALLENGE_HEADER));

        if (proxyChallenges.isEmpty()) {
            LOG.debug("Proxy authentication challenge(s) not found");
            return false;
        }
        AuthScheme authscheme = null;
        try {
            authscheme = authProcessor.processChallenge(authstate, proxyChallenges);
        } catch (final AuthChallengeException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(e.getMessage());
            }
        }
        if (authscheme == null) {
            return false;
        }
        final AuthScope authscope = new AuthScope(conn.getProxyHost(), conn.getProxyPort(), authscheme.getSchemeName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Proxy authentication scope: " + authscope);
        }
        if (authstate.isAuthAttempted() && authscheme.isComplete()) {
            // Already tried and failed
            final Credentials credentials = promptForProxyCredentials(authscheme, method.getParams(), authscope);
            if (credentials == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Failure authenticating with " + authscope);
                }
                return false;
            } else {
                return true;
            }
        } else {
            authstate.setAuthAttempted(true);
            Credentials credentials = state.getProxyCredentials(authscope);
            if (credentials == null) {
                credentials = promptForProxyCredentials(authscheme, method.getParams(), authscope);
            }
            if (credentials == null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("No credentials available for " + authscope);
                }
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Tests if the {@link HttpMethod method} requires a redirect to another
     * location.
     *
     * @param method
     *        HTTP method
     *
     * @return boolean <tt>true</tt> if a retry is needed, <tt>false</tt>
     *         otherwise.
     */
    private boolean isRedirectNeeded(final HttpMethod method) {
        switch (method.getStatusCode()) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_SEE_OTHER:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
                LOG.debug("Redirect required");
                if (method.getFollowRedirects()) {
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
        } // end of switch
    }

    /**
     * Tests if the {@link HttpMethod method} requires authentication.
     *
     * @param method
     *        HTTP method
     *
     * @return boolean <tt>true</tt> if a retry is needed, <tt>false</tt>
     *         otherwise.
     */
    private boolean isAuthenticationNeeded(final HttpMethod method) {
        method.getHostAuthState().setAuthRequested(method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED);
        method.getProxyAuthState().setAuthRequested(
            method.getStatusCode() == HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED);
        if (method.getHostAuthState().isAuthRequested() || method.getProxyAuthState().isAuthRequested()) {
            LOG.debug("Authorization required");
            if (method.getDoAuthentication()) { // process authentication
                                                // response
                return true;
            } else { // let the client handle the authenticaiton
                LOG.info("Authentication requested but doAuthentication is " + "disabled");
                return false;
            }
        } else {
            return false;
        }
    }

    private Credentials promptForCredentials(
        final AuthScheme authScheme,
        final HttpParams params,
        final AuthScope authscope) {
        LOG.debug("Credentials required");
        Credentials creds = null;
        final CredentialsProvider credProvider =
            (CredentialsProvider) params.getParameter(CredentialsProvider.PROVIDER);
        if (credProvider != null) {
            try {
                creds = credProvider.getCredentials(authScheme, authscope.getHost(), authscope.getPort(), false);
            } catch (final CredentialsNotAvailableException e) {
                LOG.warn(e.getMessage());
            }
            if (creds != null) {
                state.setCredentials(authscope, creds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(authscope + " new credentials given");
                }
            }
        } else {
            LOG.debug("Credentials provider not available");
        }
        return creds;
    }

    private Credentials promptForProxyCredentials(
        final AuthScheme authScheme,
        final HttpParams params,
        final AuthScope authscope) {
        LOG.debug("Proxy credentials required");
        Credentials creds = null;
        final CredentialsProvider credProvider =
            (CredentialsProvider) params.getParameter(CredentialsProvider.PROVIDER);
        if (credProvider != null) {
            try {
                creds = credProvider.getCredentials(authScheme, authscope.getHost(), authscope.getPort(), true);
            } catch (final CredentialsNotAvailableException e) {
                LOG.warn(e.getMessage());
            }
            if (creds != null) {
                state.setProxyCredentials(authscope, creds);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(authscope + " new credentials given");
                }
            }
        } else {
            LOG.debug("Proxy credentials provider not available");
        }
        return creds;
    }

    /**
     * @return
     */
    public HostConfiguration getHostConfiguration() {
        return hostConfiguration;
    }

    /**
     * @return
     */
    public HttpState getState() {
        return state;
    }

    /**
     * @return
     */
    public HttpConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * @return
     */
    public HttpParams getParams() {
        return params;
    }
}
