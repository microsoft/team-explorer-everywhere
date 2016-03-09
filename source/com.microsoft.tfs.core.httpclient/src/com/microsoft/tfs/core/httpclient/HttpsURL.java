/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpsURL.java,v 1.11
 * 2004/09/30 17:26:41 oglueck Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import com.microsoft.tfs.core.httpclient.util.URIUtil;

/**
 * The HTTPS URL.
 *
 * @author <a href="mailto:jericho at apache.org">Sung-Gu</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public class HttpsURL extends HttpURL {

    // ----------------------------------------------------------- Constructors

    /**
     * Create an instance as an internal use.
     */
    protected HttpsURL() {
    }

    /**
     * Construct a HTTPS URL as an escaped form of a character array with the
     * given charset to do escape encoding.
     *
     * @param escaped
     *        the HTTPS URL character sequence
     * @param charset
     *        the charset to do escape encoding
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @throws NullPointerException
     *         if <code>escaped</code> is <code>null</code>
     * @see #getProtocolCharset
     */
    public HttpsURL(final char[] escaped, final String charset) throws URIException, NullPointerException {
        protocolCharset = charset;
        parseUriReference(new String(escaped), true);
        checkValid();
    }

    /**
     * Construct a HTTPS URL as an escaped form of a character array.
     *
     * @param escaped
     *        the HTTPS URL character sequence
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @throws NullPointerException
     *         if <code>escaped</code> is <code>null</code>
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final char[] escaped) throws URIException, NullPointerException {
        parseUriReference(new String(escaped), true);
        checkValid();
    }

    /**
     * Construct a HTTPS URL from a given string with the given charset to do
     * escape encoding.
     *
     * @param original
     *        the HTTPS URL string
     * @param charset
     *        the charset to do escape encoding
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getProtocolCharset
     */
    public HttpsURL(final String original, final String charset) throws URIException {
        protocolCharset = charset;
        parseUriReference(original, false);
        checkValid();
    }

    /**
     * Construct a HTTPS URL from a given string.
     *
     * @param original
     *        the HTTPS URL string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String original) throws URIException {
        parseUriReference(original, false);
        checkValid();
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String host, final int port, final String path) throws URIException {
        this(null, host, port, path, null, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @param query
     *        the query string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String host, final int port, final String path, final String query) throws URIException {

        this(null, host, port, path, query, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param user
     *        the user name
     * @param password
     *        his or her password
     * @param host
     *        the host string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String user, final String password, final String host) throws URIException {

        this(user, password, host, -1, null, null, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param user
     *        the user name
     * @param password
     *        his or her password
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String user, final String password, final String host, final int port) throws URIException {

        this(user, password, host, port, null, null, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param user
     *        the user name
     * @param password
     *        his or her password
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String user, final String password, final String host, final int port, final String path)
        throws URIException {

        this(user, password, host, port, path, null, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param user
     *        the user name
     * @param password
     *        his or her password
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @param query
     *        The query string.
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(
        final String user,
        final String password,
        final String host,
        final int port,
        final String path,
        final String query) throws URIException {

        this(user, password, host, port, path, query, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * @param host
     *        the host string
     * @param path
     *        the path string
     * @param query
     *        the query string
     * @param fragment
     *        the fragment string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String host, final String path, final String query, final String fragment)
        throws URIException {

        this(null, host, -1, path, query, fragment);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * Note: The <code>userinfo</code> format is normally
     * <code>&lt;username&gt;:&lt;password&gt;</code> where username and
     * password must both be URL escaped.
     *
     * @param userinfo
     *        the userinfo string whose parts are URL escaped
     * @param host
     *        the host string
     * @param path
     *        the path string
     * @param query
     *        the query string
     * @param fragment
     *        the fragment string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(
        final String userinfo,
        final String host,
        final String path,
        final String query,
        final String fragment) throws URIException {

        this(userinfo, host, -1, path, query, fragment);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * Note: The <code>userinfo</code> format is normally
     * <code>&lt;username&gt;:&lt;password&gt;</code> where username and
     * password must both be URL escaped.
     *
     * @param userinfo
     *        the userinfo string whose parts are URL escaped
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String userinfo, final String host, final int port, final String path) throws URIException {

        this(userinfo, host, port, path, null, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * Note: The <code>userinfo</code> format is normally
     * <code>&lt;username&gt;:&lt;password&gt;</code> where username and
     * password must both be URL escaped.
     *
     * @param userinfo
     *        the userinfo string whose parts are URL escaped
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @param query
     *        the query string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(final String userinfo, final String host, final int port, final String path, final String query)
        throws URIException {

        this(userinfo, host, port, path, query, null);
    }

    /**
     * Construct a HTTPS URL from given components.
     *
     * Note: The <code>userinfo</code> format is normally
     * <code>&lt;username&gt;:&lt;password&gt;</code> where username and
     * password must both be URL escaped.
     *
     * @param userinfo
     *        the userinfo string whose parts are URL escaped
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @param query
     *        the query string
     * @param fragment
     *        the fragment string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(
        final String userinfo,
        final String host,
        final int port,
        final String path,
        final String query,
        final String fragment) throws URIException {

        // validate and contruct the URI character sequence
        final StringBuffer buff = new StringBuffer();
        if (userinfo != null || host != null || port != -1) {
            _scheme = DEFAULT_SCHEME; // in order to verify the own protocol
            buff.append(_default_scheme);
            buff.append("://");
            if (userinfo != null) {
                buff.append(userinfo);
                buff.append('@');
            }
            if (host != null) {
                buff.append(URIUtil.encode(host, URI.allowed_host));
                if (port != -1 || port != DEFAULT_PORT) {
                    buff.append(':');
                    buff.append(port);
                }
            }
        }
        if (path != null) { // accept empty path
            if (scheme != null && !path.startsWith("/")) {
                throw new URIException(URIException.PARSING, "abs_path requested");
            }
            buff.append(URIUtil.encode(path, URI.allowed_abs_path));
        }
        if (query != null) {
            buff.append('?');
            buff.append(URIUtil.encode(query, URI.allowed_query));
        }
        if (fragment != null) {
            buff.append('#');
            buff.append(URIUtil.encode(fragment, URI.allowed_fragment));
        }
        parseUriReference(buff.toString(), true);
        checkValid();
    }

    /**
     * Construct a HTTP URL from given components.
     *
     * @param user
     *        the user name
     * @param password
     *        his or her password
     * @param host
     *        the host string
     * @param port
     *        the port number
     * @param path
     *        the path string
     * @param query
     *        the query string
     * @param fragment
     *        the fragment string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpsURL(
        final String user,
        final String password,
        final String host,
        final int port,
        final String path,
        final String query,
        final String fragment) throws URIException {
        this(HttpURL.toUserinfo(user, password), host, port, path, query, fragment);
    }

    /**
     * Construct a HTTPS URL with a given relative HTTPS URL string.
     *
     * @param base
     *        the base HttpsURL
     * @param relative
     *        the relative HTTPS URL string
     * @throws URIException
     *         If {@link #checkValid()} fails
     */
    public HttpsURL(final HttpsURL base, final String relative) throws URIException {
        this(base, new HttpsURL(relative));
    }

    /**
     * Construct a HTTPS URL with a given relative URL.
     *
     * @param base
     *        the base HttpsURL
     * @param relative
     *        the relative HttpsURL
     * @throws URIException
     *         If {@link #checkValid()} fails
     */
    public HttpsURL(final HttpsURL base, final HttpsURL relative) throws URIException {
        super(base, relative);
        checkValid();
    }

    // -------------------------------------------------------------- Constants

    /**
     * Default scheme for HTTPS URL.
     */
    public static final char[] DEFAULT_SCHEME = {
        'h',
        't',
        't',
        'p',
        's'
    };

    /**
     * Default scheme for HTTPS URL.
     *
     * @deprecated Use {@link #DEFAULT_SCHEME} instead. This one doesn't conform
     *             to the project naming conventions.
     */
    @Deprecated
    public static final char[] _default_scheme = DEFAULT_SCHEME;

    /**
     * Default port for HTTPS URL.
     */
    public static final int DEFAULT_PORT = 443;

    /**
     * Default port for HTTPS URL.
     *
     * @deprecated Use {@link #DEFAULT_PORT} instead. This one doesn't conform
     *             to the project naming conventions.
     */
    @Deprecated
    public static final int _default_port = DEFAULT_PORT;

    /**
     * The serialVersionUID.
     */
    static final long serialVersionUID = 887844277028676648L;

    // ------------------------------------------------------------- The scheme

    /**
     * Get the scheme. You can get the scheme explicitly.
     *
     * @return the scheme
     */
    @Override
    public char[] getRawScheme() {
        return (_scheme == null) ? null : HttpsURL.DEFAULT_SCHEME;
    }

    /**
     * Get the scheme. You can get the scheme explicitly.
     *
     * @return the scheme null if empty or undefined
     */
    @Override
    public String getScheme() {
        return (_scheme == null) ? null : new String(HttpsURL.DEFAULT_SCHEME);
    }

    // --------------------------------------------------------------- The port

    /**
     * Get the port number.
     *
     * @return the port number
     */
    @Override
    public int getPort() {
        return (_port == -1) ? HttpsURL.DEFAULT_PORT : _port;
    }

    // ---------------------------------------------------------------- Utility

    /**
     * Verify the valid class use for construction.
     *
     * @throws URIException
     *         the wrong scheme use
     */
    @Override
    protected void checkValid() throws URIException {
        // could be explicit protocol or undefined.
        if (!(equals(_scheme, DEFAULT_SCHEME) || _scheme == null)) {
            throw new URIException(URIException.PARSING, "wrong class use");
        }
    }

}
