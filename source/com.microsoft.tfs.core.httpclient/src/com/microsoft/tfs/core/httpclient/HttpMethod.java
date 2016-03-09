/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpMethod.java,v 1.43
 * 2004/10/07 16:14:15 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import com.microsoft.tfs.core.httpclient.auth.AuthState;
import com.microsoft.tfs.core.httpclient.params.HttpMethodParams;

/**
 * <p>
 * HttpMethod interface represents a request to be sent via a
 * {@link HttpConnection HTTP connection} and a corresponding response.
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Rod Waldhoff
 * @author <a href="jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 480424 $ $Date: 2006-11-29 06:56:49 +0100 (Wed, 29 Nov
 *          2006) $
 *
 * @since 1.0
 */
public interface HttpMethod {

    // ------------------------------------------- Property Setters and Getters

    /**
     * Obtains the name of the HTTP method as used in the HTTP request line, for
     * example <tt>"GET"</tt> or <tt>"POST"</tt>.
     *
     * @return the name of this method
     */
    String getName();

    /**
     * Gets the host configuration for this method. The configuration specifies
     * the server, port, protocol, and proxy server via which this method will
     * send its HTTP request.
     *
     * @deprecated no longer applicable
     *
     * @return the HostConfiguration or <code>null</code> if none is set
     */
    @Deprecated
    HostConfiguration getHostConfiguration();

    /**
     * Sets the path of the HTTP method. It is responsibility of the caller to
     * ensure that the path is properly encoded (URL safe).
     *
     * @param path
     *        The path of the HTTP method. The path is expected to be URL
     *        encoded.
     */
    void setPath(String path);

    /**
     * Returns the path of the HTTP method.
     *
     * Calling this method <em>after</em> the request has been executed will
     * return the <em>actual</em> path, following any redirects automatically
     * handled by this HTTP method.
     *
     * @return the path of the HTTP method, in URL encoded form
     */
    String getPath();

    /**
     * Returns the URI for this method. The URI will be absolute if the host
     * configuration has been set and relative otherwise.
     *
     * @return the URI for this method
     *
     * @throws URIException
     *         if a URI cannot be constructed
     */
    URI getURI() throws URIException;

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
    void setURI(URI uri) throws URIException;

    /**
     * Defines how strictly the method follows the HTTP protocol specification.
     * (See RFC 2616 and other relevant RFCs.) In the strict mode the method
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
     *
     * @see #isStrictMode()
     */
    @Deprecated
    void setStrictMode(boolean strictMode);

    /**
     * Returns the value of the strict mode flag.
     *
     * @return <tt>true</tt> if strict mode is enabled, <tt>false</tt> otherwise
     *
     * @deprecated Use
     *             {@link com.microsoft.tfs.core.httpclient.params.HttpParams#setParameter(String, Object)}
     *             to exercise a more granular control over HTTP protocol
     *             strictness.
     *
     * @see #setStrictMode(boolean)
     */
    @Deprecated
    boolean isStrictMode();

    /**
     * Sets the specified request header, overwriting any previous value. Note
     * that header-name matching is case insensitive.
     *
     * @param headerName
     *        the header's name
     * @param headerValue
     *        the header's value
     *
     * @see #setRequestHeader(Header)
     * @see #getRequestHeader(String)
     * @see #removeRequestHeader(String)
     */
    void setRequestHeader(String headerName, String headerValue);

    /**
     * Sets the specified request header, overwriting any previous value. Note
     * that header-name matching is case insensitive.
     *
     * @param header
     *        the header to be set
     *
     * @see #setRequestHeader(String,String)
     * @see #getRequestHeader(String)
     * @see #removeRequestHeader(String)
     */
    void setRequestHeader(Header header);

    /**
     * Adds the specified request header, <em>not</em> overwriting any previous
     * value. If the same header is added multiple times, perhaps with different
     * values, multiple instances of that header will be sent in the HTTP
     * request. Note that header-name matching is case insensitive.
     *
     * @param headerName
     *        the header's name
     * @param headerValue
     *        the header's value
     *
     * @see #addRequestHeader(Header)
     * @see #getRequestHeader(String)
     * @see #removeRequestHeader(String)
     */
    void addRequestHeader(String headerName, String headerValue);

    /**
     * Adds the specified request header, <em>not</em> overwriting any previous
     * value. If the same header is added multiple times, perhaps with different
     * values, multiple instances of that header will be sent in the HTTP
     * request. Note that header-name matching is case insensitive.
     *
     * @param header
     *        the header
     *
     * @see #addRequestHeader(String,String)
     * @see #getRequestHeader(String)
     * @see #removeRequestHeader(String)
     */
    void addRequestHeader(Header header);

    /**
     * Gets the request header with the given name. If there are multiple
     * headers with the same name, there values will be combined with the ','
     * separator as specified by RFC2616. Note that header-name matching is case
     * insensitive.
     *
     * @param headerName
     *        the header name
     * @return the header
     */
    Header getRequestHeader(String headerName);

    /**
     * Removes all request headers with the given name. Note that header-name
     * matching is case insensitive.
     *
     * @param headerName
     *        the header name
     */
    void removeRequestHeader(String headerName);

    /**
     * Removes the given request header.
     *
     * @param header
     *        the header
     *
     * @since 3.0
     */
    void removeRequestHeader(Header header);

    /**
     * Returns <tt>true</tt> if the HTTP method should automatically follow HTTP
     * redirects (status code 302, etc.), <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if the method will automatically follow HTTP
     *         redirects, <tt>false</tt> otherwise
     */
    boolean getFollowRedirects();

    /**
     * Sets whether or not the HTTP method should automatically follow HTTP
     * redirects (status code 302, etc.)
     *
     * @param followRedirects
     *        <tt>true</tt> if the method will automatically follow redirects,
     *        <tt>false</tt> otherwise.
     */
    void setFollowRedirects(boolean followRedirects);

    /**
     * Sets the query string of the HTTP method. It is responsibility of the
     * caller to ensure that the path is properly encoded (URL safe). The string
     * must not include an initial '?' character.
     *
     * @param queryString
     *        the query to be used in the request, with no leading '?' character
     *
     * @see #getQueryString()
     * @see #setQueryString(NameValuePair[])
     */
    void setQueryString(String queryString);

    /**
     * Sets the query string of this HTTP method. The pairs are encoded as UTF-8
     * characters. To use a different charset the parameters can be encoded
     * manually using EncodingUtil and set as a single String.
     *
     * @param params
     *        An array of <code>NameValuePair</code>s to use as the query
     *        string. The name/value pairs will be automatically URL encoded and
     *        should not have been encoded previously.
     *
     * @see #getQueryString()
     * @see #setQueryString(String)
     * @see com.microsoft.tfs.core.httpclient.util.EncodingUtil#formUrlEncode(NameValuePair[],
     *      String)
     */
    void setQueryString(NameValuePair[] params);

    /**
     * Returns the query string of this HTTP method.
     *
     * @return the query string in URL encoded form, without a leading '?'.
     *
     * @see #setQueryString(NameValuePair[])
     * @see #setQueryString(String)
     */
    String getQueryString();

    /**
     * Returns the current request headers for this HTTP method. The returned
     * headers will be in the same order that they were added with
     * <code>addRequestHeader</code>. If there are multiple request headers with
     * the same name (e.g. <code>Cookie</code>), they will be returned as
     * multiple entries in the array.
     *
     * @return an array containing all of the request headers
     *
     * @see #addRequestHeader(Header)
     * @see #addRequestHeader(String,String)
     */
    Header[] getRequestHeaders();

    /**
     * Returns the request headers with the given name. Note that header-name
     * matching is case insensitive.
     *
     * @param headerName
     *        the name of the headers to be returned.
     * @return an array of zero or more headers
     *
     * @since 3.0
     */
    Header[] getRequestHeaders(String headerName);

    // ---------------------------------------------------------------- Queries

    /**
     * Returns <tt>true</tt> the method is ready to execute, <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if the method is ready to execute, <tt>false</tt>
     *         otherwise.
     */
    boolean validate();

    /**
     * Returns the status code associated with the latest response.
     *
     * @return The status code from the most recent execution of this method. If
     *         the method has not yet been executed, the result is undefined.
     */
    int getStatusCode();

    /**
     * Returns the status text (or "reason phrase") associated with the latest
     * response.
     *
     * @return The status text from the most recent execution of this method. If
     *         the method has not yet been executed, the result is undefined.
     */
    String getStatusText();

    /**
     * Returns the response headers from the most recent execution of this
     * request.
     *
     * @return A newly-created array containing all of the response headers, in
     *         the order in which they appeared in the response.
     */
    Header[] getResponseHeaders();

    /**
     * Returns the specified response header. Note that header-name matching is
     * case insensitive.
     *
     * @param headerName
     *        The name of the header to be returned.
     *
     * @return The specified response header. If the repsonse contained multiple
     *         instances of the header, its values will be combined using the
     *         ',' separator as specified by RFC2616.
     */
    Header getResponseHeader(String headerName);

    /**
     * Returns the response headers with the given name. Note that header-name
     * matching is case insensitive.
     *
     * @param headerName
     *        the name of the headers to be returned.
     * @return an array of zero or more headers
     *
     * @since 3.0
     */
    Header[] getResponseHeaders(String headerName);

    /**
     * Returns the response footers from the most recent execution of this
     * request.
     *
     * @return an array containing the response footers in the order that they
     *         appeared in the response. If the response had no footers, an
     *         empty array will be returned.
     */
    Header[] getResponseFooters();

    /**
     * Return the specified response footer. Note that footer-name matching is
     * case insensitive.
     *
     * @param footerName
     *        The name of the footer.
     * @return The response footer.
     */
    Header getResponseFooter(String footerName);

    /**
     * Returns the response body of the HTTP method, if any, as an array of
     * bytes. If the method has not yet been executed or the response has no
     * body, <code>null</code> is returned. Note that this method does not
     * propagate I/O exceptions. If an error occurs while reading the body,
     * <code>null</code> will be returned.
     *
     * @return The response body, or <code>null</code> if the body is not
     *         available.
     *
     * @throws IOException
     *         if an I/O (transport) problem occurs
     */
    byte[] getResponseBody() throws IOException;

    /**
     * Returns the response body of the HTTP method, if any, as a {@link String}
     * . If response body is not available or cannot be read, <tt>null</tt> is
     * returned. The raw bytes in the body are converted to a
     * <code>String</code> using the character encoding specified in the
     * response's <tt>Content-Type</tt> header, or ISO-8859-1 if the response
     * did not specify a character set.
     * <p>
     * Note that this method does not propagate I/O exceptions. If an error
     * occurs while reading the body, <code>null</code> will be returned.
     *
     * @return The response body converted to a <code>String</code>, or
     *         <code>null</code> if the body is not available.
     *
     * @throws IOException
     *         if an I/O (transport) problem occurs
     */
    String getResponseBodyAsString() throws IOException;

    /**
     * Returns the response body of the HTTP method, if any, as an InputStream.
     * If the response had no body or the method has not yet been executed,
     * <code>null</code> is returned. Additionally, <code>null</code> may be
     * returned if {@link #releaseConnection} has been called or if this method
     * was called previously and the resulting stream was closed.
     *
     * @return The response body, or <code>null</code> if it is not available
     *
     * @throws IOException
     *         if an I/O (transport) problem occurs
     */
    InputStream getResponseBodyAsStream() throws IOException;

    /**
     * Returns <tt>true</tt> if the HTTP method has been already {@link #execute
     * executed}, but not {@link #recycle recycled}.
     *
     * @return <tt>true</tt> if the method has been executed, <tt>false</tt>
     *         otherwise
     */
    boolean hasBeenUsed();

    // --------------------------------------------------------- Action Methods

    /**
     * Executes this method using the specified <code>HttpConnection</code> and
     * <code>HttpState</code>.
     *
     * @param state
     *        the {@link HttpState state} information to associate with this
     *        method
     * @param connection
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         If an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         If a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @return the integer status code if one was obtained, or <tt>-1</tt>
     */
    int execute(HttpState state, HttpConnection connection) throws HttpException, IOException;

    /**
     * Aborts the execution of the HTTP method.
     *
     * @see #execute(HttpState, HttpConnection)
     *
     * @since 3.0
     */
    void abort();

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
    @Deprecated
    void recycle();

    /**
     * Releases the connection being used by this HTTP method. In particular the
     * connection is used to read the response (if there is one) and will be
     * held until the response has been read. If the connection can be reused by
     * other HTTP methods it is NOT closed at this point.
     * <p>
     * After this method is called, {@link #getResponseBodyAsStream} will return
     * <code>null</code>, and {@link #getResponseBody} and
     * {@link #getResponseBodyAsString} <em>may</em> return <code>null</code>.
     */
    void releaseConnection();

    /**
     * Add a footer to this method's response.
     * <p>
     * <b>Note:</b> This method is for internal use only and should not be
     * called by external clients.
     *
     * @param footer
     *        the footer to add
     *
     * @since 2.0
     */
    void addResponseFooter(Header footer);

    /**
     * Returns the Status-Line from the most recent response for this method, or
     * <code>null</code> if the method has not been executed.
     *
     * @return the status line, or <code>null</code> if the method has not been
     *         executed
     *
     * @since 2.0
     */
    StatusLine getStatusLine();

    /**
     * Returns <tt>true</tt> if the HTTP method should automatically handle HTTP
     * authentication challenges (status code 401, etc.), <tt>false</tt>
     * otherwise
     *
     * @return <tt>true</tt> if authentication challenges will be processed
     *         automatically, <tt>false</tt> otherwise.
     *
     * @since 2.0
     *
     * @see #setDoAuthentication(boolean)
     */
    boolean getDoAuthentication();

    /**
     * Sets whether or not the HTTP method should automatically handle HTTP
     * authentication challenges (status code 401, etc.)
     *
     * @param doAuthentication
     *        <tt>true</tt> to process authentication challenges automatically,
     *        <tt>false</tt> otherwise.
     *
     * @since 2.0
     *
     * @see #getDoAuthentication()
     */
    void setDoAuthentication(boolean doAuthentication);

    /**
     * Returns {@link HttpMethodParams HTTP protocol parameters} associated with
     * this method.
     *
     * @since 3.0
     *
     * @see HttpMethodParams
     */
    public HttpMethodParams getParams();

    /**
     * Assigns {@link HttpMethodParams HTTP protocol parameters} for this
     * method.
     *
     * @since 3.0
     *
     * @see HttpMethodParams
     */
    public void setParams(final HttpMethodParams params);

    /**
     * Returns the target host {@link AuthState authentication state}
     *
     * @return host authentication state
     *
     * @since 3.0
     */
    public AuthState getHostAuthState();

    /**
     * Returns the proxy {@link AuthState authentication state}
     *
     * @return host authentication state
     *
     * @since 3.0
     */
    public AuthState getProxyAuthState();

    /**
     * Returns <tt>true</tt> if the HTTP has been transmitted to the target
     * server in its entirety, <tt>false</tt> otherwise. This flag can be useful
     * for recovery logic. If the request has not been transmitted in its
     * entirety, it is safe to retry the failed method.
     *
     * @return <tt>true</tt> if the request has been sent, <tt>false</tt>
     *         otherwise
     */
    boolean isRequestSent();

}
