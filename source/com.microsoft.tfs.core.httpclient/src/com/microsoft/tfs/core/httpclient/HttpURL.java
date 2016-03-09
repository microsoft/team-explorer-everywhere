/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpURL.java,v 1.18
 * 2004/09/30 17:26:41 oglueck Exp $ $Revision: 507324 $ $Date: 2007-02-14
 * 01:12:11 +0100 (Wed, 14 Feb 2007) $
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
 * The HTTP URL.
 *
 * @author <a href="mailto:jericho at apache.org">Sung-Gu</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 */
public class HttpURL extends URI {

    // ----------------------------------------------------------- Constructors

    /** Create an instance as an internal use. */
    protected HttpURL() {
    }

    /**
     * Construct a HTTP URL as an escaped form of a character array with the
     * given charset to do escape encoding.
     *
     * @param escaped
     *        the HTTP URL character sequence
     * @param charset
     *        the charset string to do escape encoding
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @throws NullPointerException
     *         if <code>escaped</code> is <code>null</code>
     * @see #getProtocolCharset
     */
    public HttpURL(final char[] escaped, final String charset) throws URIException, NullPointerException {
        protocolCharset = charset;
        parseUriReference(new String(escaped), true);
        checkValid();
    }

    /**
     * Construct a HTTP URL as an escaped form of a character array.
     *
     * @param escaped
     *        the HTTP URL character sequence
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @throws NullPointerException
     *         if <code>escaped</code> is <code>null</code>
     * @see #getDefaultProtocolCharset
     */
    public HttpURL(final char[] escaped) throws URIException, NullPointerException {
        parseUriReference(new String(escaped), true);
        checkValid();
    }

    /**
     * Construct a HTTP URL from a given string with the given charset to do
     * escape encoding.
     *
     * @param original
     *        the HTTP URL string
     * @param charset
     *        the charset string to do escape encoding
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getProtocolCharset
     */
    public HttpURL(final String original, final String charset) throws URIException {
        protocolCharset = charset;
        parseUriReference(original, false);
        checkValid();
    }

    /**
     * Construct a HTTP URL from a given string.
     *
     * @param original
     *        the HTTP URL string
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpURL(final String original) throws URIException {
        parseUriReference(original, false);
        checkValid();
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(final String host, final int port, final String path) throws URIException {
        this(null, null, host, port, path, null, null);
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(final String host, final int port, final String path, final String query) throws URIException {

        this(null, null, host, port, path, query, null);
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
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpURL(final String user, final String password, final String host) throws URIException {

        this(user, password, host, -1, null, null, null);
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
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpURL(final String user, final String password, final String host, final int port) throws URIException {

        this(user, password, host, port, null, null, null);
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
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpURL(final String user, final String password, final String host, final int port, final String path)
        throws URIException {

        this(user, password, host, port, path, null, null);
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
     *        The query string.
     * @throws URIException
     *         If {@link #checkValid()} fails
     * @see #getDefaultProtocolCharset
     */
    public HttpURL(
        final String user,
        final String password,
        final String host,
        final int port,
        final String path,
        final String query) throws URIException {

        this(user, password, host, port, path, query, null);
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(final String host, final String path, final String query, final String fragment)
        throws URIException {

        this(null, null, host, -1, path, query, fragment);
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(
        final String userinfo,
        final String host,
        final String path,
        final String query,
        final String fragment) throws URIException {

        this(userinfo, host, -1, path, query, fragment);
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(final String userinfo, final String host, final int port, final String path) throws URIException {

        this(userinfo, host, port, path, null, null);
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(final String userinfo, final String host, final int port, final String path, final String query)
        throws URIException {

        this(userinfo, host, port, path, query, null);
    }

    /**
     * Construct a HTTP URL from given components.
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
    public HttpURL(
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
    public HttpURL(
        final String user,
        final String password,
        final String host,
        final int port,
        final String path,
        final String query,
        final String fragment) throws URIException {
        this(toUserinfo(user, password), host, port, path, query, fragment);
    }

    protected static String toUserinfo(final String user, final String password) throws URIException {
        if (user == null) {
            return null;
        }
        final StringBuffer usrinfo = new StringBuffer(20); // sufficient for
                                                           // real
        // world
        usrinfo.append(URIUtil.encode(user, URI.allowed_within_userinfo));
        if (password == null) {
            return usrinfo.toString();
        }
        usrinfo.append(':');
        usrinfo.append(URIUtil.encode(password, URI.allowed_within_userinfo));
        return usrinfo.toString();
    }

    /**
     * Construct a HTTP URL with a given relative URL string.
     *
     * @param base
     *        the base HttpURL
     * @param relative
     *        the relative HTTP URL string
     * @throws URIException
     *         If {@link #checkValid()} fails
     */
    public HttpURL(final HttpURL base, final String relative) throws URIException {
        this(base, new HttpURL(relative));
    }

    /**
     * Construct a HTTP URL with a given relative URL.
     *
     * @param base
     *        the base HttpURL
     * @param relative
     *        the relative HttpURL
     * @throws URIException
     *         If {@link #checkValid()} fails
     */
    public HttpURL(final HttpURL base, final HttpURL relative) throws URIException {
        super(base, relative);
        checkValid();
    }

    // -------------------------------------------------------------- Constants

    /**
     * Default scheme for HTTP URL.
     */
    public static final char[] DEFAULT_SCHEME = {
        'h',
        't',
        't',
        'p'
    };

    /**
     * Default scheme for HTTP URL.
     *
     * @deprecated Use {@link #DEFAULT_SCHEME} instead. This one doesn't conform
     *             to the project naming conventions.
     */
    @Deprecated
    public static final char[] _default_scheme = DEFAULT_SCHEME;

    /**
     * Default port for HTTP URL.
     */
    public static final int DEFAULT_PORT = 80;

    /**
     * Default port for HTTP URL.
     *
     * @deprecated Use {@link #DEFAULT_PORT} instead. This one doesn't conform
     *             to the project naming conventions.
     */
    @Deprecated
    public static final int _default_port = DEFAULT_PORT;

    /**
     * The serialVersionUID.
     */
    static final long serialVersionUID = -7158031098595039459L;

    // ------------------------------------------------------------- The scheme

    /**
     * Get the scheme. You can get the scheme explicitly.
     *
     * @return the scheme
     */
    @Override
    public char[] getRawScheme() {
        return (_scheme == null) ? null : HttpURL.DEFAULT_SCHEME;
    }

    /**
     * Get the scheme. You can get the scheme explicitly.
     *
     * @return the scheme null if empty or undefined
     */
    @Override
    public String getScheme() {
        return (_scheme == null) ? null : new String(HttpURL.DEFAULT_SCHEME);
    }

    // --------------------------------------------------------------- The port

    /**
     * Get the port number.
     *
     * @return the port number
     */
    @Override
    public int getPort() {
        return (_port == -1) ? HttpURL.DEFAULT_PORT : _port;
    }

    // ----------------------------------------------------------- The userinfo

    /**
     * Set the raw-escaped user and password.
     *
     * @param escapedUser
     *        the raw-escaped user
     * @param escapedPassword
     *        the raw-escaped password; could be null
     * @throws URIException
     *         escaped user not valid or user required; escaped password not
     *         valid or username missed
     */
    public void setRawUserinfo(final char[] escapedUser, final char[] escapedPassword) throws URIException {

        if (escapedUser == null || escapedUser.length == 0) {
            throw new URIException(URIException.PARSING, "user required");
        }
        if (!validate(escapedUser, within_userinfo)
            || ((escapedPassword != null) && !validate(escapedPassword, within_userinfo))) {
            throw new URIException(URIException.ESCAPING, "escaped userinfo not valid");
        }
        final String username = new String(escapedUser);
        final String password = (escapedPassword == null) ? null : new String(escapedPassword);
        final String userinfo = username + ((password == null) ? "" : ":" + password);
        final String hostname = new String(getRawHost());
        final String hostport = (_port == -1) ? hostname : hostname + ":" + _port;
        final String authority = userinfo + "@" + hostport;
        _userinfo = userinfo.toCharArray();
        _authority = authority.toCharArray();
        setURI();
    }

    /**
     * Set the raw-escaped user and password.
     *
     * @param escapedUser
     *        the escaped user
     * @param escapedPassword
     *        the escaped password; could be null
     * @throws URIException
     *         escaped user not valid or user required; escaped password not
     *         valid or username missed
     * @throws NullPointerException
     *         null user
     */
    public void setEscapedUserinfo(final String escapedUser, final String escapedPassword)
        throws URIException,
            NullPointerException {

        setRawUserinfo(escapedUser.toCharArray(), (escapedPassword == null) ? null : escapedPassword.toCharArray());
    }

    /**
     * Set the user and password.
     *
     * @param user
     *        the user
     * @param password
     *        the password; could be null
     * @throws URIException
     *         encoding error or username missed
     * @throws NullPointerException
     *         null user
     */
    public void setUserinfo(final String user, final String password) throws URIException, NullPointerException {
        // set the charset to do escape encoding
        final String charset = getProtocolCharset();
        setRawUserinfo(
            encode(user, within_userinfo, charset),
            (password == null) ? null : encode(password, within_userinfo, charset));
    }

    /**
     * Set the raw-escaped user.
     *
     * @param escapedUser
     *        the raw-escaped user
     * @throws URIException
     *         escaped user not valid or user required
     */
    public void setRawUser(final char[] escapedUser) throws URIException {
        if (escapedUser == null || escapedUser.length == 0) {
            throw new URIException(URIException.PARSING, "user required");
        }
        if (!validate(escapedUser, within_userinfo)) {
            throw new URIException(URIException.ESCAPING, "escaped user not valid");
        }
        final String username = new String(escapedUser);
        final char[] rawPassword = getRawPassword();
        final String password = rawPassword == null ? null : new String(rawPassword);
        final String userinfo = username + ((password == null) ? "" : ":" + password);
        final String hostname = new String(getRawHost());
        final String hostport = (_port == -1) ? hostname : hostname + ":" + _port;
        final String authority = userinfo + "@" + hostport;
        _userinfo = userinfo.toCharArray();
        _authority = authority.toCharArray();
        setURI();
    }

    /**
     * Set the escaped user string.
     *
     * @param escapedUser
     *        the escaped user string
     * @throws URIException
     *         escaped user not valid
     * @throws NullPointerException
     *         null user
     */
    public void setEscapedUser(final String escapedUser) throws URIException, NullPointerException {
        setRawUser(escapedUser.toCharArray());
    }

    /**
     * Set the user string.
     *
     * @param user
     *        the user string
     * @throws URIException
     *         user encoding error
     * @throws NullPointerException
     *         null user
     */
    public void setUser(final String user) throws URIException, NullPointerException {
        setRawUser(encode(user, allowed_within_userinfo, getProtocolCharset()));
    }

    /**
     * Get the raw-escaped user.
     *
     * @return the raw-escaped user
     */
    public char[] getRawUser() {
        if (_userinfo == null || _userinfo.length == 0) {
            return null;
        }
        final int to = indexFirstOf(_userinfo, ':');
        // String.indexOf(':', 0, _userinfo.length, _userinfo, 0, 1, 0);
        if (to == -1) {
            return _userinfo; // only user.
        }
        final char[] result = new char[to];
        System.arraycopy(_userinfo, 0, result, 0, to);
        return result;
    }

    /**
     * Get the escaped user
     *
     * @return the escaped user
     */
    public String getEscapedUser() {
        final char[] user = getRawUser();
        return (user == null) ? null : new String(user);
    }

    /**
     * Get the user.
     *
     * @return the user name
     * @throws URIException
     *         If {@link #decode} fails
     */
    public String getUser() throws URIException {
        final char[] user = getRawUser();
        return (user == null) ? null : decode(user, getProtocolCharset());
    }

    /**
     * Set the raw-escaped password.
     *
     * @param escapedPassword
     *        the raw-escaped password; could be null
     * @throws URIException
     *         escaped password not valid or username missed
     */
    public void setRawPassword(final char[] escapedPassword) throws URIException {
        if (escapedPassword != null && !validate(escapedPassword, within_userinfo)) {
            throw new URIException(URIException.ESCAPING, "escaped password not valid");
        }
        if (getRawUser() == null || getRawUser().length == 0) {
            throw new URIException(URIException.PARSING, "username required");
        }
        final String username = new String(getRawUser());
        final String password = escapedPassword == null ? null : new String(escapedPassword);
        // an emtpy string is allowed as a password
        final String userinfo = username + ((password == null) ? "" : ":" + password);
        final String hostname = new String(getRawHost());
        final String hostport = (_port == -1) ? hostname : hostname + ":" + _port;
        final String authority = userinfo + "@" + hostport;
        _userinfo = userinfo.toCharArray();
        _authority = authority.toCharArray();
        setURI();
    }

    /**
     * Set the escaped password string.
     *
     * @param escapedPassword
     *        the escaped password string; could be null
     * @throws URIException
     *         escaped password not valid or username missed
     */
    public void setEscapedPassword(final String escapedPassword) throws URIException {
        setRawPassword((escapedPassword == null) ? null : escapedPassword.toCharArray());
    }

    /**
     * Set the password string.
     *
     * @param password
     *        the password string; could be null
     * @throws URIException
     *         encoding error or username missed
     */
    public void setPassword(final String password) throws URIException {
        setRawPassword((password == null) ? null : encode(password, allowed_within_userinfo, getProtocolCharset()));
    }

    /**
     * Get the raw-escaped password.
     *
     * @return the raw-escaped password
     */
    public char[] getRawPassword() {
        final int from = indexFirstOf(_userinfo, ':');
        if (from == -1) {
            return null; // null or only user.
        }
        final int len = _userinfo.length - from - 1;
        final char[] result = new char[len];
        System.arraycopy(_userinfo, from + 1, result, 0, len);
        return result;
    }

    /**
     * Get the escaped password.
     *
     * @return the escaped password
     */
    public String getEscapedPassword() {
        final char[] password = getRawPassword();
        return (password == null) ? null : new String(password);
    }

    /**
     * Get the password.
     *
     * @return the password
     * @throws URIException
     *         If {@link #decode(char[],String)} fails.
     */
    public String getPassword() throws URIException {
        final char[] password = getRawPassword();
        return (password == null) ? null : decode(password, getProtocolCharset());
    }

    // --------------------------------------------------------------- The path

    /**
     * Get the raw-escaped current hierarchy level.
     *
     * @return the raw-escaped current hierarchy level
     * @throws URIException
     *         If {@link #getRawCurrentHierPath(char[])} fails.
     */
    @Override
    public char[] getRawCurrentHierPath() throws URIException {
        return (_path == null || _path.length == 0) ? rootPath : super.getRawCurrentHierPath(_path);
    }

    /**
     * Get the level above the this hierarchy level.
     *
     * @return the raw above hierarchy level
     * @throws URIException
     *         If {@link #getRawCurrentHierPath(char[])} fails.
     */
    @Override
    public char[] getRawAboveHierPath() throws URIException {
        final char[] path = getRawCurrentHierPath();
        return (path == null || path.length == 0) ? rootPath : getRawCurrentHierPath(path);
    }

    /**
     * Get the raw escaped path.
     *
     * @return the path '/' if empty or undefined
     */
    @Override
    public char[] getRawPath() {
        final char[] path = super.getRawPath();
        return (path == null || path.length == 0) ? rootPath : path;
    }

    // -------------------------------------------------------------- The query

    /**
     * Set the query as the name and value pair.
     *
     * @param queryName
     *        the query string.
     * @param queryValue
     *        the query string.
     * @throws URIException
     *         incomplete trailing escape pattern Or unsupported character
     *         encoding
     * @throws NullPointerException
     *         null query
     * @see #encode
     */
    public void setQuery(final String queryName, final String queryValue) throws URIException, NullPointerException {

        final StringBuffer buff = new StringBuffer();
        // set the charset to do escape encoding
        final String charset = getProtocolCharset();
        buff.append(encode(queryName, allowed_within_query, charset));
        buff.append('=');
        buff.append(encode(queryValue, allowed_within_query, charset));
        _query = buff.toString().toCharArray();
        setURI();
    }

    /**
     * Set the query as the name and value pairs.
     *
     * @param queryName
     *        the array of the query string.
     * @param queryValue
     *        the array of the query string.
     * @throws URIException
     *         incomplete trailing escape pattern, unsupported character
     *         encoding or wrong array size
     * @throws NullPointerException
     *         null query
     * @see #encode
     */
    public void setQuery(final String[] queryName, final String[] queryValue)
        throws URIException,
            NullPointerException {

        final int length = queryName.length;
        if (length != queryValue.length) {
            throw new URIException("wrong array size of query");
        }

        final StringBuffer buff = new StringBuffer();
        // set the charset to do escape encoding
        final String charset = getProtocolCharset();
        for (int i = 0; i < length; i++) {
            buff.append(encode(queryName[i], allowed_within_query, charset));
            buff.append('=');
            buff.append(encode(queryValue[i], allowed_within_query, charset));
            if (i + 1 < length) {
                buff.append('&');
            }
        }
        _query = buff.toString().toCharArray();
        setURI();
    }

    // ---------------------------------------------------------------- Utility

    /**
     * Verify the valid class use for construction.
     *
     * @throws URIException
     *         the wrong scheme use
     */
    protected void checkValid() throws URIException {
        // could be explicit protocol or undefined.
        if (!(equals(_scheme, DEFAULT_SCHEME) || _scheme == null)) {
            throw new URIException(URIException.PARSING, "wrong class use");
        }
    }

    /**
     * Once it's parsed successfully, set this URI.
     *
     * @see #getRawURI
     */
    @Override
    protected void setURI() {
        // set _uri
        final StringBuffer buf = new StringBuffer();
        // ^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?
        if (_scheme != null) {
            buf.append(_scheme);
            buf.append(':');
        }
        if (_is_net_path) {
            buf.append("//");
            if (_authority != null) { // has_authority
                if (_userinfo != null) { // by default, remove userinfo part
                    if (_host != null) {
                        buf.append(_host);
                        if (_port != -1) {
                            buf.append(':');
                            buf.append(_port);
                        }
                    }
                } else {
                    buf.append(_authority);
                }
            }
        }
        if (_opaque != null && _is_opaque_part) {
            buf.append(_opaque);
        } else if (_path != null) {
            // _is_hier_part or _is_relativeURI
            if (_path.length != 0) {
                buf.append(_path);
            }
        }
        if (_query != null) { // has_query
            buf.append('?');
            buf.append(_query);
        }
        // ignore the fragment identifier
        _uri = buf.toString().toCharArray();
        hash = 0;
    }

}
