/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpMethodBase.java,v
 * 1.222 2005/01/14 21:16:40 olegk Exp $ $Revision: 539441 $ $Date: 2007-05-18
 * 14:56:55 +0200 (Fri, 18 May 2007) $
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.auth.AuthState;
import com.microsoft.tfs.core.httpclient.cookie.CookiePolicy;
import com.microsoft.tfs.core.httpclient.cookie.CookieSpec;
import com.microsoft.tfs.core.httpclient.cookie.CookieVersionSupport;
import com.microsoft.tfs.core.httpclient.cookie.MalformedCookieException;
import com.microsoft.tfs.core.httpclient.params.HttpMethodParams;
import com.microsoft.tfs.core.httpclient.protocol.Protocol;
import com.microsoft.tfs.core.httpclient.util.EncodingUtil;
import com.microsoft.tfs.core.httpclient.util.ExceptionUtil;

/**
 * An abstract base implementation of HttpMethod.
 * <p>
 * At minimum, subclasses will need to override:
 * <ul>
 * <li>{@link #getName} to return the approriate name for this method</li>
 * </ul>
 * </p>
 *
 * <p>
 * When a method requires additional request headers, subclasses will typically
 * want to override:
 * <ul>
 * <li>{@link #addRequestHeaders addRequestHeaders(HttpState,HttpConnection)} to
 * write those headers</li>
 * </ul>
 * </p>
 *
 * <p>
 * When a method expects specific response headers, subclasses may want to
 * override:
 * <ul>
 * <li>{@link #processResponseHeaders
 * processResponseHeaders(HttpState,HttpConnection)} to handle those headers
 * </li>
 * </ul>
 * </p>
 *
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Rodney Waldhoff
 * @author Sean C. Sullivan
 * @author <a href="mailto:dion@apache.org">dIon Gillard</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:dims@apache.org">Davanum Srinivas</a>
 * @author Ortwin Glueck
 * @author Eric Johnson
 * @author Michael Becke
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 * @author Christian Kohlschuetter
 *
 * @version $Revision: 539441 $ $Date: 2007-05-18 14:56:55 +0200 (Fri, 18 May
 *          2007) $
 */
public abstract class HttpMethodBase implements HttpMethod {
    // -------------------------------------------------------------- Constants

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpMethodBase.class);

    // ----------------------------------------------------- Instance variables

    /** Request headers, if any. */
    private final HeaderGroup requestHeaders = new HeaderGroup();

    /** The Status-Line from the response. */
    protected StatusLine statusLine = null;

    /** Response headers, if any. */
    private HeaderGroup responseHeaders = new HeaderGroup();

    /** Response trailer headers, if any. */
    private final HeaderGroup responseTrailerHeaders = new HeaderGroup();

    /** Path of the HTTP method. */
    private String path = null;

    /** Query string of the HTTP method, if any. */
    private String queryString = null;

    /**
     * The response body of the HTTP method, assuming it has not be intercepted
     * by a sub-class.
     */
    private InputStream responseStream = null;

    /** The connection that the response stream was read from. */
    private HttpConnection responseConnection = null;

    /** Buffer for the response */
    private byte[] responseBody = null;

    /** True if the HTTP method should automatically follow HTTP redirects. */
    private boolean followRedirects = false;

    /**
     * True if the HTTP method should automatically handle HTTP authentication
     * challenges.
     */
    private boolean doAuthentication = true;

    /** HTTP protocol parameters. */
    private HttpMethodParams params = new HttpMethodParams();

    /** Host authentication state */
    private final AuthState hostAuthState = new AuthState();

    /** Proxy authentication state */
    private final AuthState proxyAuthState = new AuthState();

    /** True if this method has already been executed. */
    private boolean used = false;

    /**
     * Count of how many times did this HTTP method transparently handle a
     * recoverable exception.
     */
    private int recoverableExceptionCount = 0;

    /** the host for this HTTP method, can be null */
    private HttpHost httphost = null;

    /**
     * Handles method retries
     *
     * @deprecated no loner used
     */
    @Deprecated
    private MethodRetryHandler methodRetryHandler;

    /** True if the connection must be closed when no longer needed */
    private boolean connectionCloseForced = false;

    /** Number of milliseconds to wait for 100-contunue response. */
    private static final int RESPONSE_WAIT_TIME_MS = 3000;

    /** HTTP protocol version used for execution of this method. */
    protected HttpVersion effectiveVersion = null;

    /** Whether the execution of this method has been aborted */
    private volatile boolean aborted = false;

    /**
     * Whether the HTTP request has been transmitted to the target server it its
     * entirety
     */
    private boolean requestSent = false;

    /** Actual cookie policy */
    private CookieSpec cookiespec = null;

    /**
     * Default initial size of the response buffer if content length is unknown.
     */
    private static final int DEFAULT_INITIAL_BUFFER_SIZE = 4 * 1024; // 4 kB

    // ----------------------------------------------------------- Constructors

    /**
     * No-arg constructor.
     */
    public HttpMethodBase() {
    }

    /**
     * Constructor specifying a URI. It is responsibility of the caller to
     * ensure that URI elements (path & query parameters) are properly encoded
     * (URL safe).
     *
     * @param uri
     *        either an absolute or relative URI. The URI is expected to be
     *        URL-encoded
     *
     * @throws IllegalArgumentException
     *         when URI is invalid
     * @throws IllegalStateException
     *         when protocol of the absolute URI is not recognised
     */
    public HttpMethodBase(String uri) throws IllegalArgumentException, IllegalStateException {

        try {

            // create a URI and allow for null/empty uri values
            if (uri == null || uri.equals("")) {
                uri = "/";
            }
            final String charset = getParams().getUriCharset();
            setURI(new URI(uri, true, charset));
        } catch (final URIException e) {
            throw new IllegalArgumentException("Invalid uri '" + uri + "': " + e.getMessage());
        }
    }

    // ------------------------------------------- Property Setters and Getters

    /**
     * Obtains the name of the HTTP method as used in the HTTP request line, for
     * example <tt>"GET"</tt> or <tt>"POST"</tt>.
     *
     * @return the name of this method
     */
    @Override
    public abstract String getName();

    /**
     * Returns the URI of the HTTP method
     *
     * @return The URI
     *
     * @throws URIException
     *         If the URI cannot be created.
     *
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getURI()
     */
    @Override
    public URI getURI() throws URIException {
        final StringBuffer buffer = new StringBuffer();
        if (httphost != null) {
            buffer.append(httphost.getProtocol().getScheme());
            buffer.append("://");
            buffer.append(httphost.getHostName());
            final int port = httphost.getPort();
            if (port != -1 && port != httphost.getProtocol().getDefaultPort()) {
                buffer.append(":");
                buffer.append(port);
            }
        }
        buffer.append(path);
        if (queryString != null) {
            buffer.append('?');
            buffer.append(queryString);
        }
        final String charset = getParams().getUriCharset();
        return new URI(buffer.toString(), true, charset);
    }

    /**
     * Sets the URI for this method.
     *
     * @param uri
     *        URI to be set
     *
     * @throws URIException
     *         if a URI cannot be set
     *
     * @since 3.0
     */
    @Override
    public void setURI(final URI uri) throws URIException {
        // only set the host if specified by the URI
        if (uri.isAbsoluteURI()) {
            httphost = new HttpHost(uri);
        }
        // set the path, defaulting to root
        setPath(uri.getPath() == null ? "/" : uri.getEscapedPath());
        setQueryString(uri.getEscapedQuery());
    }

    /**
     * Sets whether or not the HTTP method should automatically follow HTTP
     * redirects (status code 302, etc.)
     *
     * @param followRedirects
     *        <tt>true</tt> if the method will automatically follow redirects,
     *        <tt>false</tt> otherwise.
     */
    @Override
    public void setFollowRedirects(final boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Returns <tt>true</tt> if the HTTP method should automatically follow HTTP
     * redirects (status code 302, etc.), <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if the method will automatically follow HTTP
     *         redirects, <tt>false</tt> otherwise.
     */
    @Override
    public boolean getFollowRedirects() {
        return followRedirects;
    }

    /**
     * Sets whether version 1.1 of the HTTP protocol should be used per default.
     *
     * @param http11
     *        <tt>true</tt> to use HTTP/1.1, <tt>false</tt> to use 1.0
     *
     * @deprecated Use {@link HttpMethodParams#setVersion(HttpVersion)}
     */
    @Deprecated
    public void setHttp11(final boolean http11) {
        if (http11) {
            params.setVersion(HttpVersion.HTTP_1_1);
        } else {
            params.setVersion(HttpVersion.HTTP_1_0);
        }
    }

    /**
     * Returns <tt>true</tt> if the HTTP method should automatically handle HTTP
     * authentication challenges (status code 401, etc.), <tt>false</tt>
     * otherwise
     *
     * @return <tt>true</tt> if authentication challenges will be processed
     *         automatically, <tt>false</tt> otherwise.
     *
     * @since 2.0
     */
    @Override
    public boolean getDoAuthentication() {
        return doAuthentication;
    }

    /**
     * Sets whether or not the HTTP method should automatically handle HTTP
     * authentication challenges (status code 401, etc.)
     *
     * @param doAuthentication
     *        <tt>true</tt> to process authentication challenges authomatically,
     *        <tt>false</tt> otherwise.
     *
     * @since 2.0
     */
    @Override
    public void setDoAuthentication(final boolean doAuthentication) {
        this.doAuthentication = doAuthentication;
    }

    // ---------------------------------------------- Protected Utility Methods

    /**
     * Returns <tt>true</tt> if version 1.1 of the HTTP protocol should be used
     * per default, <tt>false</tt> if version 1.0 should be used.
     *
     * @return <tt>true</tt> to use HTTP/1.1, <tt>false</tt> to use 1.0
     *
     * @deprecated Use {@link HttpMethodParams#getVersion()}
     */
    @Deprecated
    public boolean isHttp11() {
        return params.getVersion().equals(HttpVersion.HTTP_1_1);
    }

    /**
     * Sets the path of the HTTP method. It is responsibility of the caller to
     * ensure that the path is properly encoded (URL safe).
     *
     * @param path
     *        the path of the HTTP method. The path is expected to be
     *        URL-encoded
     */
    @Override
    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Adds the specified request header, NOT overwriting any previous value.
     * Note that header-name matching is case insensitive.
     *
     * @param header
     *        the header to add to the request
     */
    @Override
    public void addRequestHeader(final Header header) {
        LOG.trace("HttpMethodBase.addRequestHeader(Header)");

        if (header == null) {
            LOG.debug("null header value ignored");
        } else {
            getRequestHeaderGroup().addHeader(header);
        }
    }

    /**
     * Use this method internally to add footers.
     *
     * @param footer
     *        The footer to add.
     */
    @Override
    public void addResponseFooter(final Header footer) {
        getResponseTrailerHeaderGroup().addHeader(footer);
    }

    /**
     * Gets the path of this HTTP method. Calling this method <em>after</em> the
     * request has been executed will return the <em>actual</em> path, following
     * any redirects automatically handled by this HTTP method.
     *
     * @return the path to request or "/" if the path is blank.
     */
    @Override
    public String getPath() {
        return (path == null || path.equals("")) ? "/" : path;
    }

    /**
     * Sets the query string of this HTTP method. The caller must ensure that
     * the string is properly URL encoded. The query string should not start
     * with the question mark character.
     *
     * @param queryString
     *        the query string
     *
     * @see EncodingUtil#formUrlEncode(NameValuePair[], String)
     */
    @Override
    public void setQueryString(final String queryString) {
        this.queryString = queryString;
    }

    /**
     * Sets the query string of this HTTP method. The pairs are encoded as UTF-8
     * characters. To use a different charset the parameters can be encoded
     * manually using EncodingUtil and set as a single String.
     *
     * @param params
     *        an array of {@link NameValuePair}s to add as query string
     *        parameters. The name/value pairs will be automcatically URL
     *        encoded
     *
     * @see EncodingUtil#formUrlEncode(NameValuePair[], String)
     * @see #setQueryString(String)
     */
    @Override
    public void setQueryString(final NameValuePair[] params) {
        LOG.trace("enter HttpMethodBase.setQueryString(NameValuePair[])");
        queryString = EncodingUtil.formUrlEncode(params, "UTF-8");
    }

    /**
     * Gets the query string of this HTTP method.
     *
     * @return The query string
     */
    @Override
    public String getQueryString() {
        return queryString;
    }

    /**
     * Set the specified request header, overwriting any previous value. Note
     * that header-name matching is case-insensitive.
     *
     * @param headerName
     *        the header's name
     * @param headerValue
     *        the header's value
     */
    @Override
    public void setRequestHeader(final String headerName, final String headerValue) {
        final Header header = new Header(headerName, headerValue);
        setRequestHeader(header);
    }

    /**
     * Sets the specified request header, overwriting any previous value. Note
     * that header-name matching is case insensitive.
     *
     * @param header
     *        the header
     */
    @Override
    public void setRequestHeader(final Header header) {

        final Header[] headers = getRequestHeaderGroup().getHeaders(header.getName());

        for (int i = 0; i < headers.length; i++) {
            getRequestHeaderGroup().removeHeader(headers[i]);
        }

        getRequestHeaderGroup().addHeader(header);

    }

    /**
     * Returns the specified request header. Note that header-name matching is
     * case insensitive. <tt>null</tt> will be returned if either
     * <i>headerName</i> is <tt>null</tt> or there is no matching header for
     * <i>headerName</i>.
     *
     * @param headerName
     *        The name of the header to be returned.
     *
     * @return The specified request header.
     *
     * @since 3.0
     */
    @Override
    public Header getRequestHeader(final String headerName) {
        if (headerName == null) {
            return null;
        } else {
            return getRequestHeaderGroup().getCondensedHeader(headerName);
        }
    }

    /**
     * Returns an array of the requests headers that the HTTP method currently
     * has
     *
     * @return an array of my request headers.
     */
    @Override
    public Header[] getRequestHeaders() {
        return getRequestHeaderGroup().getAllHeaders();
    }

    /**
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getRequestHeaders(java.lang.String)
     */
    @Override
    public Header[] getRequestHeaders(final String headerName) {
        return getRequestHeaderGroup().getHeaders(headerName);
    }

    /**
     * Gets the {@link HeaderGroup header group} storing the request headers.
     *
     * @return a HeaderGroup
     *
     * @since 2.0beta1
     */
    protected HeaderGroup getRequestHeaderGroup() {
        return requestHeaders;
    }

    /**
     * Gets the {@link HeaderGroup header group} storing the response trailer
     * headers as per RFC 2616 section 3.6.1.
     *
     * @return a HeaderGroup
     *
     * @since 2.0beta1
     */
    protected HeaderGroup getResponseTrailerHeaderGroup() {
        return responseTrailerHeaders;
    }

    /**
     * Gets the {@link HeaderGroup header group} storing the response headers.
     *
     * @return a HeaderGroup
     *
     * @since 2.0beta1
     */
    protected HeaderGroup getResponseHeaderGroup() {
        return responseHeaders;
    }

    /**
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getResponseHeaders(java.lang.String)
     *
     * @since 3.0
     */
    @Override
    public Header[] getResponseHeaders(final String headerName) {
        return getResponseHeaderGroup().getHeaders(headerName);
    }

    /**
     * Returns the response status code.
     *
     * @return the status code associated with the latest response.
     */
    @Override
    public int getStatusCode() {
        return statusLine.getStatusCode();
    }

    /**
     * Provides access to the response status line.
     *
     * @return the status line object from the latest response.
     * @since 2.0
     */
    @Override
    public StatusLine getStatusLine() {
        return statusLine;
    }

    /**
     * Checks if response data is available.
     *
     * @return <tt>true</tt> if response data is available, <tt>false</tt>
     *         otherwise.
     */
    private boolean responseAvailable() {
        return (responseBody != null) || (responseStream != null);
    }

    /**
     * Returns an array of the response headers that the HTTP method currently
     * has in the order in which they were read.
     *
     * @return an array of response headers.
     */
    @Override
    public Header[] getResponseHeaders() {
        return getResponseHeaderGroup().getAllHeaders();
    }

    /**
     * Gets the response header associated with the given name. Header name
     * matching is case insensitive. <tt>null</tt> will be returned if either
     * <i>headerName</i> is <tt>null</tt> or there is no matching header for
     * <i>headerName</i>.
     *
     * @param headerName
     *        the header name to match
     *
     * @return the matching header
     */
    @Override
    public Header getResponseHeader(final String headerName) {
        if (headerName == null) {
            return null;
        } else {
            return getResponseHeaderGroup().getCondensedHeader(headerName);
        }
    }

    /**
     * Return the length (in bytes) of the response body, as specified in a
     * <tt>Content-Length</tt> header.
     *
     * <p>
     * Return <tt>-1</tt> when the content-length is unknown.
     * </p>
     *
     * @return content length, if <tt>Content-Length</tt> header is available.
     *         <tt>0</tt> indicates that the request has no body. If
     *         <tt>Content-Length</tt> header is not present, the method returns
     *         <tt>-1</tt>.
     */
    public long getResponseContentLength() {
        final Header[] headers = getResponseHeaderGroup().getHeaders("Content-Length");
        if (headers.length == 0) {
            return -1;
        }
        if (headers.length > 1) {
            LOG.warn("Multiple content-length headers detected");
        }
        for (int i = headers.length - 1; i >= 0; i--) {
            final Header header = headers[i];
            try {
                return Long.parseLong(header.getValue());
            } catch (final NumberFormatException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Invalid content-length value: " + e.getMessage());
                }
            }
            // See if we can have better luck with another header, if present
        }
        return -1;
    }

    /**
     * Returns the response body of the HTTP method, if any, as an array of
     * bytes. If response body is not available or cannot be read, returns
     * <tt>null</tt>. Buffers the response and this method can be called several
     * times yielding the same result each time.
     *
     * Note: This will cause the entire response body to be buffered in memory.
     * A malicious server may easily exhaust all the VM memory. It is strongly
     * recommended, to use getResponseAsStream if the content length of the
     * response is unknown or resonably large.
     *
     * @return The response body.
     *
     * @throws IOException
     *         If an I/O (transport) problem occurs while obtaining the response
     *         body.
     */
    @Override
    public byte[] getResponseBody() throws IOException {
        if (responseBody == null) {
            final InputStream instream = getResponseBodyAsStream();
            if (instream != null) {
                final long contentLength = getResponseContentLength();
                if (contentLength > Integer.MAX_VALUE) { // guard below cast
                                                         // from overflow
                    throw new IOException("Content too large to be buffered: " + contentLength + " bytes");
                }
                final int limit = getParams().getIntParameter(HttpMethodParams.BUFFER_WARN_TRIGGER_LIMIT, 1024 * 1024);
                if ((contentLength == -1) || (contentLength > limit)) {
                    LOG.warn(
                        "Going to buffer response body of large or unknown size. "
                            + "Using getResponseBodyAsStream instead is recommended.");
                }
                LOG.debug("Buffering response body");
                final ByteArrayOutputStream outstream =
                    new ByteArrayOutputStream(contentLength > 0 ? (int) contentLength : DEFAULT_INITIAL_BUFFER_SIZE);
                final byte[] buffer = new byte[4096];
                int len;
                while ((len = instream.read(buffer)) > 0) {
                    outstream.write(buffer, 0, len);
                }
                outstream.close();
                setResponseStream(null);
                responseBody = outstream.toByteArray();
            }
        }
        return responseBody;
    }

    /**
     * Returns the response body of the HTTP method, if any, as an array of
     * bytes. If response body is not available or cannot be read, returns
     * <tt>null</tt>. Buffers the response and this method can be called several
     * times yielding the same result each time.
     *
     * Note: This will cause the entire response body to be buffered in memory.
     * This method is safe if the content length of the response is unknown,
     * because the amount of memory used is limited.
     * <p>
     *
     * If the response is large this method involves lots of array copying and
     * many object allocations, which makes it unsuitable for high-performance /
     * low-footprint applications. Those applications should use
     * {@link #getResponseBodyAsStream()}.
     *
     * @param maxlen
     *        the maximum content length to accept (number of bytes).
     * @return The response body.
     *
     * @throws IOException
     *         If an I/O (transport) problem occurs while obtaining the response
     *         body.
     */
    public byte[] getResponseBody(final int maxlen) throws IOException {
        if (maxlen < 0) {
            throw new IllegalArgumentException("maxlen must be positive");
        }
        if (responseBody == null) {
            final InputStream instream = getResponseBodyAsStream();
            if (instream != null) {
                // we might already know that the content is larger
                final long contentLength = getResponseContentLength();
                if ((contentLength != -1) && (contentLength > maxlen)) {
                    throw new HttpContentTooLargeException("Content-Length is " + contentLength, maxlen);
                }

                LOG.debug("Buffering response body");
                final ByteArrayOutputStream rawdata =
                    new ByteArrayOutputStream(contentLength > 0 ? (int) contentLength : DEFAULT_INITIAL_BUFFER_SIZE);
                final byte[] buffer = new byte[2048];
                int pos = 0;
                int len;
                do {
                    len = instream.read(buffer, 0, Math.min(buffer.length, maxlen - pos));
                    if (len == -1) {
                        break;
                    }
                    rawdata.write(buffer, 0, len);
                    pos += len;
                } while (pos < maxlen);

                setResponseStream(null);
                // check if there is even more data
                if (pos == maxlen) {
                    if (instream.read() != -1) {
                        throw new HttpContentTooLargeException(
                            "Content-Length not known but larger than " + maxlen,
                            maxlen);
                    }
                }
                responseBody = rawdata.toByteArray();
            }
        }
        return responseBody;
    }

    /**
     * Returns the response body of the HTTP method, if any, as an
     * {@link InputStream}. If response body is not available, returns
     * <tt>null</tt>. If the response has been buffered this method returns a
     * new stream object on every call. If the response has not been buffered
     * the returned stream can only be read once.
     *
     * @return The response body or <code>null</code>.
     *
     * @throws IOException
     *         If an I/O (transport) problem occurs while obtaining the response
     *         body.
     */
    @Override
    public InputStream getResponseBodyAsStream() throws IOException {
        if (responseStream != null) {
            return responseStream;
        }
        if (responseBody != null) {
            final InputStream byteResponseStream = new ByteArrayInputStream(responseBody);
            LOG.debug("re-creating response stream from byte array");
            return byteResponseStream;
        }
        return null;
    }

    /**
     * Returns the response body of the HTTP method, if any, as a {@link String}
     * . If response body is not available or cannot be read, returns
     * <tt>null</tt> The string conversion on the data is done using the
     * character encoding specified in <tt>Content-Type</tt> header. Buffers the
     * response and this method can be called several times yielding the same
     * result each time.
     *
     * Note: This will cause the entire response body to be buffered in memory.
     * A malicious server may easily exhaust all the VM memory. It is strongly
     * recommended, to use getResponseAsStream if the content length of the
     * response is unknown or resonably large.
     *
     * @return The response body or <code>null</code>.
     *
     * @throws IOException
     *         If an I/O (transport) problem occurs while obtaining the response
     *         body.
     */
    @Override
    public String getResponseBodyAsString() throws IOException {
        byte[] rawdata = null;
        if (responseAvailable()) {
            rawdata = getResponseBody();
        }
        if (rawdata != null) {
            return EncodingUtil.getString(rawdata, getResponseCharSet());
        } else {
            return null;
        }
    }

    /**
     * Returns the response body of the HTTP method, if any, as a {@link String}
     * . If response body is not available or cannot be read, returns
     * <tt>null</tt> The string conversion on the data is done using the
     * character encoding specified in <tt>Content-Type</tt> header. Buffers the
     * response and this method can be called several times yielding the same
     * result each time.
     * </p>
     *
     * Note: This will cause the entire response body to be buffered in memory.
     * This method is safe if the content length of the response is unknown,
     * because the amount of memory used is limited.
     * <p>
     *
     * If the response is large this method involves lots of array copying and
     * many object allocations, which makes it unsuitable for high-performance /
     * low-footprint applications. Those applications should use
     * {@link #getResponseBodyAsStream()}.
     *
     * @param maxlen
     *        the maximum content length to accept (number of bytes). Note that,
     *        depending on the encoding, this is not equal to the number of
     *        characters.
     * @return The response body or <code>null</code>.
     *
     * @throws IOException
     *         If an I/O (transport) problem occurs while obtaining the response
     *         body.
     */
    public String getResponseBodyAsString(final int maxlen) throws IOException {
        if (maxlen < 0) {
            throw new IllegalArgumentException("maxlen must be positive");
        }
        byte[] rawdata = null;
        if (responseAvailable()) {
            rawdata = getResponseBody(maxlen);
        }
        if (rawdata != null) {
            return EncodingUtil.getString(rawdata, getResponseCharSet());
        } else {
            return null;
        }
    }

    /**
     * Returns an array of the response footers that the HTTP method currently
     * has in the order in which they were read.
     *
     * @return an array of footers
     */
    @Override
    public Header[] getResponseFooters() {
        return getResponseTrailerHeaderGroup().getAllHeaders();
    }

    /**
     * Gets the response footer associated with the given name. Footer name
     * matching is case insensitive. <tt>null</tt> will be returned if either
     * <i>footerName</i> is <tt>null</tt> or there is no matching footer for
     * <i>footerName</i> or there are no footers available. If there are
     * multiple footers with the same name, there values will be combined with
     * the ',' separator as specified by RFC2616.
     *
     * @param footerName
     *        the footer name to match
     * @return the matching footer
     */
    @Override
    public Header getResponseFooter(final String footerName) {
        if (footerName == null) {
            return null;
        } else {
            return getResponseTrailerHeaderGroup().getCondensedHeader(footerName);
        }
    }

    /**
     * Sets the response stream.
     *
     * @param responseStream
     *        The new response stream.
     */
    protected void setResponseStream(final InputStream responseStream) {
        this.responseStream = responseStream;
    }

    /**
     * Returns a stream from which the body of the current response may be read.
     * If the method has not yet been executed, if
     * <code>responseBodyConsumed</code> has been called, or if the stream
     * returned by a previous call has been closed, <code>null</code> will be
     * returned.
     *
     * @return the current response stream
     */
    protected InputStream getResponseStream() {
        return responseStream;
    }

    /**
     * Returns the status text (or "reason phrase") associated with the latest
     * response.
     *
     * @return The status text.
     */
    @Override
    public String getStatusText() {
        return statusLine.getReasonPhrase();
    }

    /**
     * Defines how strictly HttpClient follows the HTTP protocol specification
     * (RFC 2616 and other relevant RFCs). In the strict mode HttpClient
     * precisely implements the requirements of the specification, whereas in
     * non-strict mode it attempts to mimic the exact behaviour of commonly used
     * HTTP agents, which many HTTP servers expect.
     *
     * @param strictMode
     *        <tt>true</tt> for strict mode, <tt>false</tt> otherwise
     *
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpParams#setParameter(String, Object)}
     *             to exercise a more granular control over HTTP protocol
     *             strictness.
     */
    @Override
    @Deprecated
    public void setStrictMode(final boolean strictMode) {
        if (strictMode) {
            params.makeStrict();
        } else {
            params.makeLenient();
        }
    }

    /**
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpParams#setParameter(String, Object)}
     *             to exercise a more granular control over HTTP protocol
     *             strictness.
     *
     * @return <tt>false</tt>
     */
    @Override
    @Deprecated
    public boolean isStrictMode() {
        return false;
    }

    /**
     * Adds the specified request header, NOT overwriting any previous value.
     * Note that header-name matching is case insensitive.
     *
     * @param headerName
     *        the header's name
     * @param headerValue
     *        the header's value
     */
    @Override
    public void addRequestHeader(final String headerName, final String headerValue) {
        addRequestHeader(new Header(headerName, headerValue));
    }

    /**
     * Tests if the connection should be force-closed when no longer needed.
     *
     * @return <code>true</code> if the connection must be closed
     */
    protected boolean isConnectionCloseForced() {
        return connectionCloseForced;
    }

    /**
     * Sets whether or not the connection should be force-closed when no longer
     * needed. This value should only be set to <code>true</code> in abnormal
     * circumstances, such as HTTP protocol violations.
     *
     * @param b
     *        <code>true</code> if the connection must be closed,
     *        <code>false</code> otherwise.
     */
    protected void setConnectionCloseForced(final boolean b) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Force-close connection: " + b);
        }
        connectionCloseForced = b;
    }

    /**
     * Tests if the connection should be closed after the method has been
     * executed. The connection will be left open when using HTTP/1.1 or if
     * <tt>Connection:
     * keep-alive</tt> header was sent.
     *
     * @param conn
     *        the connection in question
     *
     * @return boolean true if we should close the connection.
     */
    protected boolean shouldCloseConnection(final HttpConnection conn) {
        // Connection must be closed due to an abnormal circumstance
        if (isConnectionCloseForced()) {
            LOG.debug("Should force-close connection.");
            return true;
        }

        Header connectionHeader = null;
        // In case being connected via a proxy server
        if (!conn.isTransparent()) {
            // Check for 'proxy-connection' directive
            connectionHeader = responseHeaders.getFirstHeader("proxy-connection");
        }
        // In all cases Check for 'connection' directive
        // some non-complaint proxy servers send it instread of
        // expected 'proxy-connection' directive
        if (connectionHeader == null) {
            connectionHeader = responseHeaders.getFirstHeader("connection");
        }
        // In case the response does not contain any explict connection
        // directives, check whether the request does
        if (connectionHeader == null) {
            connectionHeader = requestHeaders.getFirstHeader("connection");
        }
        if (connectionHeader != null) {
            if (connectionHeader.getValue().equalsIgnoreCase("close")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Should close connection in response to directive: " + connectionHeader.getValue());
                }
                return true;
            } else if (connectionHeader.getValue().equalsIgnoreCase("keep-alive")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Should NOT close connection in response to directive: " + connectionHeader.getValue());
                }
                return false;
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unknown directive: " + connectionHeader.toExternalForm());
                }
            }
        }
        LOG.debug("Resorting to protocol version default close connection policy");
        // missing or invalid connection header, do the default
        if (effectiveVersion.greaterEquals(HttpVersion.HTTP_1_1)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Should NOT close connection, using " + effectiveVersion.toString());
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Should close connection, using " + effectiveVersion.toString());
            }
        }
        return effectiveVersion.lessEquals(HttpVersion.HTTP_1_0);
    }

    /**
     * Tests if the this method is ready to be executed.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} to be used
     * @throws HttpException
     *         If the method is in invalid state.
     */
    private void checkExecuteConditions(final HttpState state, final HttpConnection conn) throws HttpException {

        if (state == null) {
            throw new IllegalArgumentException("HttpState parameter may not be null");
        }
        if (conn == null) {
            throw new IllegalArgumentException("HttpConnection parameter may not be null");
        }
        if (aborted) {
            throw new IllegalStateException("Method has been aborted");
        }
        if (!validate()) {
            throw new ProtocolException("HttpMethodBase object not valid");
        }
    }

    /**
     * Executes this method using the specified <code>HttpConnection</code> and
     * <code>HttpState</code>.
     *
     * @param state
     *        {@link HttpState state} information to associate with this
     *        request. Must be non-null.
     * @param conn
     *        the {@link HttpConnection connection} to used to execute this HTTP
     *        method. Must be non-null.
     *
     * @return the integer status code if one was obtained, or <tt>-1</tt>
     *
     * @throws IOException
     *         if an I/O (transport) error occurs
     * @throws HttpException
     *         if a protocol exception occurs.
     */
    @Override
    public int execute(final HttpState state, final HttpConnection conn) throws HttpException, IOException {

        LOG.trace("enter HttpMethodBase.execute(HttpState, HttpConnection)");

        // this is our connection now, assign it to a local variable so
        // that it can be released later
        responseConnection = conn;

        checkExecuteConditions(state, conn);
        statusLine = null;
        connectionCloseForced = false;

        conn.setLastResponseInputStream(null);

        // determine the effective protocol version
        if (effectiveVersion == null) {
            effectiveVersion = params.getVersion();
        }

        try {
            ActiveHttpMethods.setMethod(this);

            writeRequest(state, conn);
            requestSent = true;
            readResponse(state, conn);
            // the method has successfully executed
            used = true;

            return statusLine.getStatusCode();
        } catch (final HttpException e) {
            LOG.error("Protocol exception executing " + getName(), e);
            throw e;
        } catch (final IOException e) {
            LOG.error("IO exception executing " + getName(), e);
            throw e;
        } finally {
            ActiveHttpMethods.clearMethod();
        }
    }

    /**
     * Aborts the execution of this method.
     *
     * @since 3.0
     */
    @Override
    public void abort() {
        if (aborted) {
            return;
        }
        aborted = true;
        final HttpConnection conn = responseConnection;
        if (conn != null) {
            conn.close();
        }
    }

    /**
     * Returns <tt>true</tt> if the HTTP method has been already {@link #execute
     * executed}, but not {@link #recycle recycled}.
     *
     * @return <tt>true</tt> if the method has been executed, <tt>false</tt>
     *         otherwise
     */
    @Override
    public boolean hasBeenUsed() {
        return used;
    }

    /**
     * Recycles the HTTP method so that it can be used again. Note that all of
     * the instance variables will be reset once this method has been called.
     * This method will also release the connection being used by this HTTP
     * method.
     *
     * @see #releaseConnection()
     *
     * @deprecated no longer supported and will be removed in the future version
     *             of HttpClient
     */
    @Override
    @Deprecated
    public void recycle() {
        LOG.trace("enter HttpMethodBase.recycle()");

        releaseConnection();

        path = null;
        followRedirects = false;
        doAuthentication = true;
        queryString = null;
        getRequestHeaderGroup().clear();
        getResponseHeaderGroup().clear();
        getResponseTrailerHeaderGroup().clear();
        statusLine = null;
        effectiveVersion = null;
        aborted = false;
        used = false;
        params = new HttpMethodParams();
        responseBody = null;
        recoverableExceptionCount = 0;
        connectionCloseForced = false;
        hostAuthState.invalidate();
        proxyAuthState.invalidate();
        cookiespec = null;
        requestSent = false;
    }

    /**
     * Releases the connection being used by this HTTP method. In particular the
     * connection is used to read the response(if there is one) and will be held
     * until the response has been read. If the connection can be reused by
     * other HTTP methods it is NOT closed at this point.
     *
     * @since 2.0
     */
    @Override
    public void releaseConnection() {
        try {
            if (responseStream != null) {
                try {
                    // FYI - this may indirectly invoke responseBodyConsumed.
                    responseStream.close();
                } catch (final IOException ignore) {
                }
            }
        } finally {
            ensureConnectionRelease();
        }
    }

    /**
     * Remove the request header associated with the given name. Note that
     * header-name matching is case insensitive.
     *
     * @param headerName
     *        the header name
     */
    @Override
    public void removeRequestHeader(final String headerName) {

        final Header[] headers = getRequestHeaderGroup().getHeaders(headerName);
        for (int i = 0; i < headers.length; i++) {
            getRequestHeaderGroup().removeHeader(headers[i]);
        }

    }

    /**
     * Removes the given request header.
     *
     * @param header
     *        the header
     */
    @Override
    public void removeRequestHeader(final Header header) {
        if (header == null) {
            return;
        }
        getRequestHeaderGroup().removeHeader(header);
    }

    // ---------------------------------------------------------------- Queries

    /**
     * Returns <tt>true</tt> the method is ready to execute, <tt>false</tt>
     * otherwise.
     *
     * @return This implementation always returns <tt>true</tt>.
     */
    @Override
    public boolean validate() {
        return true;
    }

    /**
     * Returns the actual cookie policy
     *
     * @return cookie spec
     */
    @SuppressWarnings("unchecked")
    private CookieSpec getCookieSpec() {
        if (cookiespec == null) {
            cookiespec = CookiePolicy.getCookieSpec(params.getCookiePolicy());
            cookiespec.setValidDateFormats(
                (Collection<SimpleDateFormat>) params.getParameter(HttpMethodParams.DATE_PATTERNS));
        }
        return cookiespec;
    }

    /**
     * Generates <tt>Cookie</tt> request headers for those {@link Cookie cookie}
     * s that match the given host, port and path.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected void addCookieRequestHeader(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {

        LOG.trace("enter HttpMethodBase.addCookieRequestHeader(HttpState, " + "HttpConnection)");

        final Header[] cookieheaders = getRequestHeaderGroup().getHeaders("Cookie");
        for (int i = 0; i < cookieheaders.length; i++) {
            final Header cookieheader = cookieheaders[i];
            if (cookieheader.isAutogenerated()) {
                getRequestHeaderGroup().removeHeader(cookieheader);
            }
        }

        final CookieSpec matcher = getCookieSpec();
        String host = params.getVirtualHost();
        if (host == null) {
            host = conn.getHost();
        }

        final Cookie[] cookies = matcher.match(host, conn.getPort(), getPath(), conn.isSecure(), state.getCookies());

        if ((cookies != null) && (cookies.length > 0)) {
            if (getParams().isParameterTrue(HttpMethodParams.SINGLE_COOKIE_HEADER)) {
                // In strict mode put all cookies on the same header
                final String s = matcher.formatCookies(cookies);
                getRequestHeaderGroup().addHeader(new Header("Cookie", s, true));
            } else {
                // In non-strict mode put each cookie on a separate header
                for (int i = 0; i < cookies.length; i++) {
                    final String s = matcher.formatCookie(cookies[i]);
                    getRequestHeaderGroup().addHeader(new Header("Cookie", s, true));
                }
            }
            if (matcher instanceof CookieVersionSupport) {
                final CookieVersionSupport versupport = (CookieVersionSupport) matcher;
                final int ver = versupport.getVersion();
                boolean needVersionHeader = false;
                for (int i = 0; i < cookies.length; i++) {
                    if (ver != cookies[i].getVersion()) {
                        needVersionHeader = true;
                    }
                }
                if (needVersionHeader) {
                    // Advertise cookie version support
                    getRequestHeaderGroup().addHeader(versupport.getVersionHeader());
                }
            }
        }
    }

    /**
     * Generates <tt>Host</tt> request header, as long as no <tt>Host</tt>
     * request header already exists.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected void addHostRequestHeader(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.addHostRequestHeader(HttpState, " + "HttpConnection)");

        // Per 19.6.1.1 of RFC 2616, it is legal for HTTP/1.0 based
        // applications to send the Host request-header.
        // TODO: Add the ability to disable the sending of this header for
        // HTTP/1.0 requests.
        String host = params.getVirtualHost();
        if (host != null) {
            LOG.debug("Using virtual host name: " + host);
        } else {
            host = conn.getHost();
        }
        final int port = conn.getPort();

        // Note: RFC 2616 uses the term "internet host name" for what goes on
        // the
        // host line. It would seem to imply that host should be blank if the
        // host is a number instead of an name. Based on the behavior of web
        // browsers, and the fact that RFC 2616 never defines the phrase
        // "internet
        // host name", and the bad behavior of HttpClient that follows if we
        // send blank, I interpret this as a small misstatement in the RFC,
        // where
        // they meant to say "internet host". So IP numbers get sent as host
        // entries too. -- Eric Johnson 12/13/2002
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding Host request header");
        }

        // appends the port only if not using the default port for the protocol
        if (conn.getProtocol().getDefaultPort() != port) {
            host += (":" + port);
        }

        setRequestHeader("Host", host);
    }

    /**
     * Generates <tt>Proxy-Connection: Keep-Alive</tt> request header when
     * communicating via a proxy server.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected void addProxyConnectionHeader(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.addProxyConnectionHeader(" + "HttpState, HttpConnection)");
        if (!conn.isTransparent()) {
            if (getRequestHeader("Proxy-Connection") == null) {
                addRequestHeader("Proxy-Connection", "Keep-Alive");
            }
        }
    }

    /**
     * Generates all the required request {@link Header header}s to be submitted
     * via the given {@link HttpConnection connection}.
     *
     * <p>
     * This implementation adds <tt>User-Agent</tt>, <tt>Host</tt>,
     * <tt>Cookie</tt>, <tt>Authorization</tt>, <tt>Proxy-Authorization</tt> and
     * <tt>Proxy-Connection</tt> headers, when appropriate.
     * </p>
     *
     * <p>
     * Subclasses may want to override this method to to add additional headers,
     * and may choose to invoke this implementation (via <tt>super</tt>) to add
     * the "standard" headers.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @see #writeRequestHeaders
     */
    protected void addRequestHeaders(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.addRequestHeaders(HttpState, " + "HttpConnection)");

        addUserAgentRequestHeader(state, conn);
        addHostRequestHeader(state, conn);
        addCookieRequestHeader(state, conn);
        addProxyConnectionHeader(state, conn);
    }

    /**
     * Generates default <tt>User-Agent</tt> request header, as long as no
     * <tt>User-Agent</tt> request header already exists.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected void addUserAgentRequestHeader(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.addUserAgentRequestHeaders(HttpState, " + "HttpConnection)");

        if (getRequestHeader("User-Agent") == null) {
            String agent = (String) getParams().getParameter(HttpMethodParams.USER_AGENT);
            if (agent == null) {
                agent = "Jakarta Commons-HttpClient";
            }
            setRequestHeader("User-Agent", agent);
        }
    }

    /**
     * Throws an {@link IllegalStateException} if the HTTP method has been
     * already {@link #execute executed}, but not {@link #recycle recycled}.
     *
     * @throws IllegalStateException
     *         if the method has been used and not recycled
     */
    protected void checkNotUsed() throws IllegalStateException {
        if (used) {
            throw new IllegalStateException("Already used.");
        }
    }

    /**
     * Throws an {@link IllegalStateException} if the HTTP method has not been
     * {@link #execute executed} since last {@link #recycle recycle}.
     *
     *
     * @throws IllegalStateException
     *         if not used
     */
    protected void checkUsed() throws IllegalStateException {
        if (!used) {
            throw new IllegalStateException("Not Used.");
        }
    }

    // ------------------------------------------------- Static Utility Methods

    /**
     * Generates HTTP request line according to the specified attributes.
     *
     * @param connection
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     * @param name
     *        the method name generate a request for
     * @param requestPath
     *        the path string for the request
     * @param query
     *        the query string for the request
     * @param version
     *        the protocol version to use (e.g. HTTP/1.0)
     *
     * @return HTTP request line
     */
    protected static String generateRequestLine(
        final HttpConnection connection,
        final String name,
        final String requestPath,
        final String query,
        final String version) {
        LOG.trace("enter HttpMethodBase.generateRequestLine(HttpConnection, " + "String, String, String, String)");

        final StringBuffer buf = new StringBuffer();
        // Append method name
        buf.append(name);
        buf.append(" ");
        // Absolute or relative URL?
        if (!connection.isTransparent()) {
            final Protocol protocol = connection.getProtocol();
            buf.append(protocol.getScheme().toLowerCase());
            buf.append("://");
            buf.append(connection.getHost());
            if ((connection.getPort() != -1) && (connection.getPort() != protocol.getDefaultPort())) {
                buf.append(":");
                buf.append(connection.getPort());
            }
        }
        // Append path, if any
        if (requestPath == null) {
            buf.append("/");
        } else {
            if (!connection.isTransparent() && !requestPath.startsWith("/")) {
                buf.append("/");
            }
            buf.append(requestPath);
        }
        // Append query, if any
        if (query != null) {
            if (query.indexOf("?") != 0) {
                buf.append("?");
            }
            buf.append(query);
        }
        // Append protocol
        buf.append(" ");
        buf.append(version);
        buf.append("\r\n");

        return buf.toString();
    }

    /**
     * This method is invoked immediately after
     * {@link #readResponseBody(HttpState,HttpConnection)} and can be overridden
     * by sub-classes in order to provide custom body processing.
     *
     * <p>
     * This implementation does nothing.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @see #readResponse
     * @see #readResponseBody
     */
    protected void processResponseBody(final HttpState state, final HttpConnection conn) {
    }

    /**
     * This method is invoked immediately after
     * {@link #readResponseHeaders(HttpState,HttpConnection)} and can be
     * overridden by sub-classes in order to provide custom response headers
     * processing.
     *
     * <p>
     * This implementation will handle the <tt>Set-Cookie</tt> and
     * <tt>Set-Cookie2</tt> headers, if any, adding the relevant cookies to the
     * given {@link HttpState}.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @see #readResponse
     * @see #readResponseHeaders
     */
    protected void processResponseHeaders(final HttpState state, final HttpConnection conn) {
        LOG.trace("enter HttpMethodBase.processResponseHeaders(HttpState, " + "HttpConnection)");

        final CookieSpec parser = getCookieSpec();

        // process set-cookie headers
        Header[] headers = getResponseHeaderGroup().getHeaders("set-cookie");
        processCookieHeaders(parser, headers, state, conn);

        // see if the cookie spec supports cookie versioning.
        if (parser instanceof CookieVersionSupport) {
            final CookieVersionSupport versupport = (CookieVersionSupport) parser;
            if (versupport.getVersion() > 0) {
                // process set-cookie2 headers.
                // Cookie2 will replace equivalent Cookie instances
                headers = getResponseHeaderGroup().getHeaders("set-cookie2");
                processCookieHeaders(parser, headers, state, conn);
            }
        }
    }

    /**
     * This method processes the specified cookie headers. It is invoked from
     * within {@link #processResponseHeaders(HttpState,HttpConnection)}
     *
     * @param headers
     *        cookie {@link Header}s to be processed
     * @param state
     *        the {@link HttpState state} information associated with this HTTP
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     */
    protected void processCookieHeaders(
        final CookieSpec parser,
        final Header[] headers,
        final HttpState state,
        final HttpConnection conn) {
        LOG.trace("enter HttpMethodBase.processCookieHeaders(Header[], HttpState, " + "HttpConnection)");

        String host = params.getVirtualHost();
        if (host == null) {
            host = conn.getHost();
        }
        for (int i = 0; i < headers.length; i++) {
            final Header header = headers[i];
            Cookie[] cookies = null;
            try {
                cookies = parser.parse(host, conn.getPort(), getPath(), conn.isSecure(), header);
            } catch (final MalformedCookieException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Invalid cookie header: \"" + header.getValue() + "\". " + e.getMessage());
                }
            }
            if (cookies != null) {
                for (int j = 0; j < cookies.length; j++) {
                    final Cookie cookie = cookies[j];
                    try {
                        parser.validate(host, conn.getPort(), getPath(), conn.isSecure(), cookie);
                        state.addCookie(cookie);
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Cookie accepted: \"" + parser.formatCookie(cookie) + "\"");
                        }
                    } catch (final MalformedCookieException e) {
                        if (LOG.isWarnEnabled()) {
                            LOG.warn("Cookie rejected: \"" + parser.formatCookie(cookie) + "\". " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is invoked immediately after
     * {@link #readStatusLine(HttpState,HttpConnection)} and can be overridden
     * by sub-classes in order to provide custom response status line
     * processing.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @see #readResponse
     * @see #readStatusLine
     */
    protected void processStatusLine(final HttpState state, final HttpConnection conn) {
    }

    /**
     * Reads the response from the given {@link HttpConnection connection}.
     *
     * <p>
     * The response is processed as the following sequence of actions:
     *
     * <ol>
     * <li>{@link #readStatusLine(HttpState,HttpConnection)} is invoked to read
     * the request line.</li>
     * <li>{@link #processStatusLine(HttpState,HttpConnection)} is invoked,
     * allowing the method to process the status line if desired.</li>
     * <li>{@link #readResponseHeaders(HttpState,HttpConnection)} is invoked to
     * read the associated headers.</li>
     * <li>{@link #processResponseHeaders(HttpState,HttpConnection)} is invoked,
     * allowing the method to process the headers if desired.</li>
     * <li>{@link #readResponseBody(HttpState,HttpConnection)} is invoked to
     * read the associated body (if any).</li>
     * <li>{@link #processResponseBody(HttpState,HttpConnection)} is invoked,
     * allowing the method to process the response body if desired.</li>
     * </ol>
     *
     * Subclasses may want to override one or more of the above methods to to
     * customize the processing. (Or they may choose to override this method if
     * dramatically different processing is required.)
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected void readResponse(final HttpState state, final HttpConnection conn) throws IOException, HttpException {
        LOG.trace("enter HttpMethodBase.readResponse(HttpState, HttpConnection)");
        // Status line & line may have already been received
        // if 'expect - continue' handshake has been used
        while (statusLine == null) {
            readStatusLine(state, conn);
            processStatusLine(state, conn);
            readResponseHeaders(state, conn);
            processResponseHeaders(state, conn);

            final int status = statusLine.getStatusCode();
            if ((status >= 100) && (status < 200)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Discarding unexpected response: " + statusLine.toString());
                }
                statusLine = null;
            }
        }
        readResponseBody(state, conn);
        processResponseBody(state, conn);
    }

    /**
     * Read the response body from the given {@link HttpConnection}.
     *
     * <p>
     * The current implementation wraps the socket level stream with an
     * appropriate stream for the type of response (chunked, content-length, or
     * auto-close). If there is no response body, the connection associated with
     * the request will be returned to the connection manager.
     * </p>
     *
     * <p>
     * Subclasses may want to override this method to to customize the
     * processing.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @see #readResponse
     * @see #processResponseBody
     */
    protected void readResponseBody(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.readResponseBody(HttpState, HttpConnection)");

        // assume we are not done with the connection if we get a stream
        final InputStream stream = readResponseBody(conn);
        if (stream == null) {
            // done using the connection!
            responseBodyConsumed();
        } else {
            conn.setLastResponseInputStream(stream);
            setResponseStream(stream);
        }
    }

    /**
     * Returns the response body as an {@link InputStream input stream}
     * corresponding to the values of the <tt>Content-Length</tt> and
     * <tt>Transfer-Encoding</tt> headers. If no response body is available
     * returns <tt>null</tt>.
     * <p>
     *
     * @see #readResponse
     * @see #processResponseBody
     *
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    private InputStream readResponseBody(final HttpConnection conn) throws HttpException, IOException {

        LOG.trace("enter HttpMethodBase.readResponseBody(HttpConnection)");

        responseBody = null;
        InputStream is = conn.getResponseInputStream();
        if (Wire.CONTENT_WIRE.enabled()) {
            is = new WireLogInputStream(is, Wire.CONTENT_WIRE);
        }
        final boolean canHaveBody = canResponseHaveBody(statusLine.getStatusCode());
        InputStream result = null;
        final Header transferEncodingHeader = responseHeaders.getFirstHeader("Transfer-Encoding");
        // We use Transfer-Encoding if present and ignore Content-Length.
        // RFC2616, 4.4 item number 3
        if (transferEncodingHeader != null) {

            final String transferEncoding = transferEncodingHeader.getValue();
            if (!"chunked".equalsIgnoreCase(transferEncoding) && !"identity".equalsIgnoreCase(transferEncoding)) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Unsupported transfer encoding: " + transferEncoding);
                }
            }
            final HeaderElement[] encodings = transferEncodingHeader.getElements();
            // The chunked encoding must be the last one applied
            // RFC2616, 14.41
            final int len = encodings.length;
            if ((len > 0) && ("chunked".equalsIgnoreCase(encodings[len - 1].getName()))) {
                // if response body is empty
                if (conn.isResponseAvailable(conn.getParams().getSoTimeout())) {
                    result = new ChunkedInputStream(is, this);
                } else {
                    if (getParams().isParameterTrue(HttpMethodParams.STRICT_TRANSFER_ENCODING)) {
                        throw new ProtocolException("Chunk-encoded body declared but not sent");
                    } else {
                        LOG.warn("Chunk-encoded body missing");
                    }
                }
            } else {
                LOG.info("Response content is not chunk-encoded");
                // The connection must be terminated by closing
                // the socket as per RFC 2616, 3.6
                setConnectionCloseForced(true);
                result = is;
            }
        } else {
            final long expectedLength = getResponseContentLength();
            if (expectedLength == -1) {
                if (canHaveBody && effectiveVersion.greaterEquals(HttpVersion.HTTP_1_1)) {
                    final Header connectionHeader = responseHeaders.getFirstHeader("Connection");
                    String connectionDirective = null;
                    if (connectionHeader != null) {
                        connectionDirective = connectionHeader.getValue();
                    }
                    if (!"close".equalsIgnoreCase(connectionDirective)) {
                        LOG.info("Response content length is not known");
                        setConnectionCloseForced(true);
                    }
                }
                result = is;
            } else {
                result = new ContentLengthInputStream(is, expectedLength);
            }
        }

        // See if the response is supposed to have a response body
        if (!canHaveBody) {
            result = null;
        }
        // if there is a result - ALWAYS wrap it in an observer which will
        // close the underlying stream as soon as it is consumed, and notify
        // the watcher that the stream has been consumed.
        if (result != null) {

            result = new AutoCloseInputStream(result, new ResponseConsumedWatcher() {
                @Override
                public void responseConsumed() {
                    responseBodyConsumed();
                }
            });
        }

        return result;
    }

    /**
     * Reads the response headers from the given {@link HttpConnection
     * connection}.
     *
     * <p>
     * Subclasses may want to override this method to to customize the
     * processing.
     * </p>
     *
     * <p>
     * "It must be possible to combine the multiple header fields into one
     * "field-name: field-value" pair, without changing the semantics of the
     * message, by appending each subsequent field-value to the first, each
     * separated by a comma." - HTTP/1.0 (4.3)
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @see #readResponse
     * @see #processResponseHeaders
     */
    protected void readResponseHeaders(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.readResponseHeaders(HttpState," + "HttpConnection)");

        getResponseHeaderGroup().clear();

        final Header[] headers =
            HttpParser.parseHeaders(conn.getResponseInputStream(), getParams().getHttpElementCharset());
        // Wire logging moved to HttpParser
        getResponseHeaderGroup().setHeaders(headers);
    }

    /**
     * Read the status line from the given {@link HttpConnection}, setting my
     * {@link #getStatusCode status code} and {@link #getStatusText status text}
     * .
     *
     * <p>
     * Subclasses may want to override this method to to customize the
     * processing.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @see StatusLine
     */
    protected void readStatusLine(final HttpState state, final HttpConnection conn) throws IOException, HttpException {
        LOG.trace("enter HttpMethodBase.readStatusLine(HttpState, HttpConnection)");
        LOG.debug("Socket timeout is " + conn.getSocket().getSoTimeout());

        final int maxGarbageLines =
            getParams().getIntParameter(HttpMethodParams.STATUS_LINE_GARBAGE_LIMIT, Integer.MAX_VALUE);

        // read out the HTTP status string
        int count = 0;
        String s;
        do {
            s = conn.readLine(getParams().getHttpElementCharset());
            if (s == null && count == 0) {
                // The server just dropped connection on us
                throw new NoHttpResponseException("The server " + conn.getHost() + " failed to respond");
            }
            if (Wire.HEADER_WIRE.enabled()) {
                Wire.HEADER_WIRE.input(s + "\r\n");
            }
            if (s != null && StatusLine.startsWithHTTP(s)) {
                // Got one
                break;
            } else if (s == null || count >= maxGarbageLines) {
                // Giving up
                throw new ProtocolException(
                    "The server " + conn.getHost() + " failed to respond with a valid HTTP response");
            }
            count++;
        } while (true);

        // create the status line from the status string
        statusLine = new StatusLine(s);

        // check for a valid HTTP-Version
        final String versionStr = statusLine.getHttpVersion();
        if (getParams().isParameterFalse(HttpMethodParams.UNAMBIGUOUS_STATUS_LINE) && versionStr.equals("HTTP")) {
            getParams().setVersion(HttpVersion.HTTP_1_0);
            if (LOG.isWarnEnabled()) {
                LOG.warn("Ambiguous status line (HTTP protocol version missing):" + statusLine.toString());
            }
        } else {
            effectiveVersion = HttpVersion.parse(versionStr);
        }

    }

    // ------------------------------------------------------ Protected Methods

    /**
     * <p>
     * Sends the request via the given {@link HttpConnection connection}.
     * </p>
     *
     * <p>
     * The request is written as the following sequence of actions:
     * </p>
     *
     * <ol>
     * <li>{@link #writeRequestLine(HttpState, HttpConnection)} is invoked to
     * write the request line.</li>
     * <li>{@link #writeRequestHeaders(HttpState, HttpConnection)} is invoked to
     * write the associated headers.</li>
     * <li><tt>\r\n</tt> is sent to close the head part of the request.</li>
     * <li>{@link #writeRequestBody(HttpState, HttpConnection)} is invoked to
     * write the body part of the request.</li>
     * </ol>
     *
     * <p>
     * Subclasses may want to override one or more of the above methods to to
     * customize the processing. (Or they may choose to override this method if
     * dramatically different processing is required.)
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected void writeRequest(final HttpState state, final HttpConnection conn) throws IOException, HttpException {
        LOG.trace("enter HttpMethodBase.writeRequest(HttpState, HttpConnection)");
        writeRequestLine(state, conn);
        writeRequestHeaders(state, conn);
        conn.writeLine(); // close head
        if (Wire.HEADER_WIRE.enabled()) {
            Wire.HEADER_WIRE.output("\r\n");
        }

        final HttpVersion ver = getParams().getVersion();
        final Header expectheader = getRequestHeader("Expect");
        String expectvalue = null;
        if (expectheader != null) {
            expectvalue = expectheader.getValue();
        }
        if ((expectvalue != null) && (expectvalue.compareToIgnoreCase("100-continue") == 0)) {
            if (ver.greaterEquals(HttpVersion.HTTP_1_1)) {

                // make sure the status line and headers have been sent
                conn.flushRequestOutputStream();

                final int readTimeout = conn.getParams().getSoTimeout();
                try {
                    conn.setSocketTimeout(RESPONSE_WAIT_TIME_MS);
                    readStatusLine(state, conn);
                    processStatusLine(state, conn);
                    readResponseHeaders(state, conn);
                    processResponseHeaders(state, conn);

                    if (statusLine.getStatusCode() == HttpStatus.SC_CONTINUE) {
                        // Discard status line
                        statusLine = null;
                        LOG.debug("OK to continue received");
                    } else {
                        return;
                    }
                } catch (final InterruptedIOException e) {
                    if (!ExceptionUtil.isSocketTimeoutException(e)) {
                        throw e;
                    }
                    // Most probably Expect header is not recongnized
                    // Remove the header to signal the method
                    // that it's okay to go ahead with sending data
                    removeRequestHeader("Expect");
                    LOG.info("100 (continue) read timeout. Resume sending the request");
                } finally {
                    conn.setSocketTimeout(readTimeout);
                }

            } else {
                removeRequestHeader("Expect");
                LOG.info("'Expect: 100-continue' handshake is only supported by " + "HTTP/1.1 or higher");
            }
        }

        writeRequestBody(state, conn);
        // make sure the entire request body has been sent
        conn.flushRequestOutputStream();
    }

    /**
     * Writes the request body to the given {@link HttpConnection connection}.
     *
     * <p>
     * This method should return <tt>true</tt> if the request body was actually
     * sent (or is empty), or <tt>false</tt> if it could not be sent for some
     * reason.
     * </p>
     *
     * <p>
     * This implementation writes nothing and returns <tt>true</tt>.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @return <tt>true</tt>
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    protected boolean writeRequestBody(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        return true;
    }

    /**
     * Writes the request headers to the given {@link HttpConnection connection}
     * .
     *
     * <p>
     * This implementation invokes
     * {@link #addRequestHeaders(HttpState,HttpConnection)}, and then writes
     * each header to the request stream.
     * </p>
     *
     * <p>
     * Subclasses may want to override this method to to customize the
     * processing.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @see #addRequestHeaders
     * @see #getRequestHeaders
     */
    protected void writeRequestHeaders(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.writeRequestHeaders(HttpState," + "HttpConnection)");
        addRequestHeaders(state, conn);

        final String charset = getParams().getHttpElementCharset();

        final Header[] headers = getRequestHeaders();
        for (int i = 0; i < headers.length; i++) {
            final String s = headers[i].toExternalForm();
            if (Wire.HEADER_WIRE.enabled()) {
                Wire.HEADER_WIRE.output(s);
            }
            conn.print(s, charset);
        }
    }

    /**
     * Writes the request line to the given {@link HttpConnection connection}.
     *
     * <p>
     * Subclasses may want to override this method to to customize the
     * processing.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @see #generateRequestLine
     */
    protected void writeRequestLine(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter HttpMethodBase.writeRequestLine(HttpState, HttpConnection)");
        final String requestLine = getRequestLine(conn);
        if (Wire.HEADER_WIRE.enabled()) {
            Wire.HEADER_WIRE.output(requestLine);
        }
        conn.print(requestLine, getParams().getHttpElementCharset());
    }

    /**
     * Returns the request line.
     *
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @return The request line.
     */
    private String getRequestLine(final HttpConnection conn) {
        return HttpMethodBase.generateRequestLine(
            conn,
            getName(),
            getPath(),
            getQueryString(),
            effectiveVersion.toString());
    }

    /**
     * Returns {@link HttpMethodParams HTTP protocol parameters} associated with
     * this method.
     *
     * @return HTTP parameters.
     *
     * @since 3.0
     */
    @Override
    public HttpMethodParams getParams() {
        return params;
    }

    /**
     * Assigns {@link HttpMethodParams HTTP protocol parameters} for this
     * method.
     *
     * @since 3.0
     *
     * @see HttpMethodParams
     */
    @Override
    public void setParams(final HttpMethodParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        this.params = params;
    }

    /**
     * Returns the HTTP version used with this method (may be <tt>null</tt> if
     * undefined, that is, the method has not been executed)
     *
     * @return HTTP version.
     *
     * @since 3.0
     */
    public HttpVersion getEffectiveVersion() {
        return effectiveVersion;
    }

    /**
     * Per RFC 2616 section 4.3, some response can never contain a message body.
     *
     * @param status
     *        - the HTTP status code
     *
     * @return <tt>true</tt> if the message may contain a body, <tt>false</tt>
     *         if it can not contain a message body
     */
    private static boolean canResponseHaveBody(final int status) {
        LOG.trace("enter HttpMethodBase.canResponseHaveBody(int)");

        boolean result = true;

        if ((status >= 100 && status <= 199) || (status == 204) || (status == 304)) { // NOT
                                                                                      // MODIFIED
            result = false;
        }

        return result;
    }

    /**
     * Returns the character set from the <tt>Content-Type</tt> header.
     *
     * @param contentheader
     *        The content header.
     * @return String The character set.
     */
    protected String getContentCharSet(final Header contentheader) {
        LOG.trace("enter getContentCharSet( Header contentheader )");
        String charset = null;
        if (contentheader != null) {
            final HeaderElement values[] = contentheader.getElements();
            // I expect only one header element to be there
            // No more. no less
            if (values.length == 1) {
                final NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    // If I get anything "funny"
                    // UnsupportedEncondingException will result
                    charset = param.getValue();
                }
            }
        }
        if (charset == null) {
            charset = getParams().getContentCharset();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Default charset used: " + charset);
            }
        }
        return charset;
    }

    /**
     * Returns the character encoding of the request from the
     * <tt>Content-Type</tt> header.
     *
     * @return String The character set.
     */
    public String getRequestCharSet() {
        return getContentCharSet(getRequestHeader("Content-Type"));
    }

    /**
     * Returns the character encoding of the response from the
     * <tt>Content-Type</tt> header.
     *
     * @return String The character set.
     */
    public String getResponseCharSet() {
        return getContentCharSet(getResponseHeader("Content-Type"));
    }

    /**
     * @deprecated no longer used
     *
     *             Returns the number of "recoverable" exceptions thrown and
     *             handled, to allow for monitoring the quality of the
     *             connection.
     *
     * @return The number of recoverable exceptions handled by the method.
     */
    @Deprecated
    public int getRecoverableExceptionCount() {
        return recoverableExceptionCount;
    }

    /**
     * A response has been consumed.
     *
     * <p>
     * The default behavior for this class is to check to see if the connection
     * should be closed, and close if need be, and to ensure that the connection
     * is returned to the connection manager - if and only if we are not still
     * inside the execute call.
     * </p>
     *
     */
    protected void responseBodyConsumed() {

        // make sure this is the initial invocation of the notification,
        // ignore subsequent ones.
        responseStream = null;
        if (responseConnection != null) {
            responseConnection.setLastResponseInputStream(null);

            // At this point, no response data should be available.
            // If there is data available, regard the connection as being
            // unreliable and close it.

            if (shouldCloseConnection(responseConnection)) {
                responseConnection.close();
            } else {
                try {
                    if (responseConnection.isResponseAvailable()) {
                        final boolean logExtraInput = getParams().isParameterTrue(HttpMethodParams.WARN_EXTRA_INPUT);

                        if (logExtraInput) {
                            LOG.warn("Extra response data detected - closing connection");
                        }
                        responseConnection.close();
                    }
                } catch (final IOException e) {
                    LOG.warn(e.getMessage());
                    responseConnection.close();
                }
            }
        }
        connectionCloseForced = false;
        ensureConnectionRelease();
    }

    /**
     * Insure that the connection is released back to the pool.
     */
    private void ensureConnectionRelease() {
        if (responseConnection != null) {
            responseConnection.releaseConnection();
            responseConnection = null;
        }
    }

    /**
     * Returns the {@link HostConfiguration host configuration}.
     *
     * @return the host configuration
     *
     * @deprecated no longer applicable
     */
    @Override
    @Deprecated
    public HostConfiguration getHostConfiguration() {
        final HostConfiguration hostconfig = new HostConfiguration();
        hostconfig.setHost(httphost);
        return hostconfig;
    }

    /**
     * Sets the {@link HostConfiguration host configuration}.
     *
     * @param hostconfig
     *        The hostConfiguration to set
     *
     * @deprecated no longer applicable
     */
    @Deprecated
    public void setHostConfiguration(final HostConfiguration hostconfig) {
        if (hostconfig != null) {
            httphost = new HttpHost(hostconfig.getHost(), hostconfig.getPort(), hostconfig.getProtocol());
        } else {
            httphost = null;
        }
    }

    /**
     * Returns the {@link MethodRetryHandler retry handler} for this HTTP method
     *
     * @return the methodRetryHandler
     *
     * @deprecated use {@link HttpMethodParams}
     */
    @Deprecated
    public MethodRetryHandler getMethodRetryHandler() {
        return methodRetryHandler;
    }

    /**
     * Sets the {@link MethodRetryHandler retry handler} for this HTTP method
     *
     * @param handler
     *        the methodRetryHandler to use when this method executed
     *
     * @deprecated use {@link HttpMethodParams}
     */
    @Deprecated
    public void setMethodRetryHandler(final MethodRetryHandler handler) {
        methodRetryHandler = handler;
    }

    /**
     * This method is a dirty hack intended to work around current (2.0) design
     * flaw that prevents the user from obtaining correct status code, headers
     * and response body from the preceding HTTP CONNECT method.
     *
     * TODO: Remove this as soon as possible
     */
    void fakeResponse(
        final StatusLine statusline,
        final HeaderGroup responseheaders,
        final InputStream responseStream) {
        // set used so that the response can be read
        used = true;
        statusLine = statusline;
        responseHeaders = responseheaders;
        responseBody = null;
        this.responseStream = responseStream;
    }

    /**
     * Returns the target host {@link AuthState authentication state}
     *
     * @return host authentication state
     *
     * @since 3.0
     */
    @Override
    public AuthState getHostAuthState() {
        return hostAuthState;
    }

    /**
     * Returns the proxy {@link AuthState authentication state}
     *
     * @return host authentication state
     *
     * @since 3.0
     */
    @Override
    public AuthState getProxyAuthState() {
        return proxyAuthState;
    }

    /**
     * Tests whether the execution of this method has been aborted
     *
     * @return <tt>true</tt> if the execution of this method has been aborted,
     *         <tt>false</tt> otherwise
     *
     * @since 3.0
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * Returns <tt>true</tt> if the HTTP has been transmitted to the target
     * server in its entirety, <tt>false</tt> otherwise. This flag can be useful
     * for recovery logic. If the request has not been transmitted in its
     * entirety, it is safe to retry the failed method.
     *
     * @return <tt>true</tt> if the request has been sent, <tt>false</tt>
     *         otherwise
     */
    @Override
    public boolean isRequestSent() {
        return requestSent;
    }

}
