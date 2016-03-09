/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/auth/AuthScope.java,v 1.2
 * 2004/06/23 06:50:25 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.auth;

import com.microsoft.tfs.core.httpclient.util.LangUtils;

/**
 * The class represents an authentication scope consisting of a host name, a
 * port number, a realm name and an authentication scheme name which
 * {@link com.microsoft.tfs.core.httpclient.Credentials} apply to.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author <a href="mailto:adrian@intencha.com">Adrian Sutton</a>
 *
 * @since 3.0
 */
public class AuthScope {
    /**
     * The <tt>null</tt> value represents any host. In the future versions of
     * HttpClient the use of this parameter will be discontinued.
     */
    public static final String ANY_HOST = null;

    /**
     * The <tt>-1</tt> value represents any port.
     */
    public static final int ANY_PORT = -1;

    /**
     * The <tt>null</tt> value represents any authentication scheme.
     */
    public static final String ANY_SCHEME = null;

    /**
     * Default scope matching any host, port, realm and authentication scheme.
     * In the future versions of HttpClient the use of this parameter will be
     * discontinued.
     */
    public static final AuthScope ANY = new AuthScope(ANY_HOST, ANY_PORT, ANY_SCHEME);

    /** The authentication scheme the credentials apply to. */
    private String scheme = null;

    /** The host the credentials apply to. */
    private String host = null;

    /** The port the credentials apply to. */
    private int port = -1;

    /**
     * Creates a new credentials scope for the given <tt>host</tt>,
     * <tt>port</tt>, <tt>realm</tt>, and <tt>authentication scheme</tt>.
     *
     * @param host
     *        the host the credentials apply to. May be set to <tt>null</tt> if
     *        credenticals are applicable to any host.
     * @param port
     *        the port the credentials apply to. May be set to negative value if
     *        credenticals are applicable to any port.
     * @param scheme
     *        the authentication scheme the credentials apply to. May be set to
     *        <tt>null</tt> if credenticals are applicable to any authentication
     *        scheme.
     *
     * @since 3.0
     */
    public AuthScope(final String host, final int port, final String scheme) {
        this.host = (host == null) ? ANY_HOST : host.toLowerCase();
        this.port = (port < 0) ? ANY_PORT : port;
        this.scheme = (scheme == null) ? ANY_SCHEME : scheme.toUpperCase();
    }

    /**
     * Creates a new credentials scope for the given <tt>host</tt>,
     * <tt>port</tt>, any realm name, and any authentication scheme.
     *
     * @param host
     *        the host the credentials apply to. May be set to <tt>null</tt> if
     *        credenticals are applicable to any host.
     * @param port
     *        the port the credentials apply to. May be set to negative value if
     *        credenticals are applicable to any port.
     *
     * @since 3.0
     */
    public AuthScope(final String host, final int port) {
        this(host, port, ANY_SCHEME);
    }

    /**
     * Creates a copy of the given credentials scope.
     *
     * @since 3.0
     */
    public AuthScope(final AuthScope authscope) {
        super();
        if (authscope == null) {
            throw new IllegalArgumentException("Scope may not be null");
        }
        host = authscope.getHost();
        port = authscope.getPort();
        scheme = authscope.getScheme();
    }

    /**
     * @return the host
     *
     * @since 3.0
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port
     *
     * @since 3.0
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the scheme type
     *
     * @since 3.0
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Determines if the given parameters are equal.
     *
     * @param p1
     *        the parameter
     * @param p2
     *        the other parameter
     * @return boolean true if the parameters are equal, otherwise false.
     */
    private static boolean paramsEqual(final String p1, final String p2) {
        if (p1 == null) {
            return p1 == p2;
        } else {
            return p1.equals(p2);
        }
    }

    /**
     * Determines if the given parameters are equal.
     *
     * @param p1
     *        the parameter
     * @param p2
     *        the other parameter
     * @return boolean true if the parameters are equal, otherwise false.
     */
    private static boolean paramsEqual(final int p1, final int p2) {
        return p1 == p2;
    }

    /**
     * Tests if the authentication scopes match.
     *
     * @return the match factor. Negative value signifies no match. Non-negative
     *         signifies a match. The greater the returned value the closer the
     *         match.
     *
     * @since 3.0
     */
    public int match(final AuthScope that) {
        int factor = 0;
        if (paramsEqual(scheme, that.scheme)) {
            factor += 1;
        } else {
            if (scheme != ANY_SCHEME && that.scheme != ANY_SCHEME) {
                return -1;
            }
        }
        if (paramsEqual(port, that.port)) {
            factor += 4;
        } else {
            if (port != ANY_PORT && that.port != ANY_PORT) {
                return -1;
            }
        }
        if (paramsEqual(host, that.host)) {
            factor += 8;
        } else {
            if (host != ANY_HOST && that.host != ANY_HOST) {
                return -1;
            }
        }
        return factor;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof AuthScope)) {
            return super.equals(o);
        }
        final AuthScope that = (AuthScope) o;
        return paramsEqual(host, that.host) && paramsEqual(port, that.port) && paramsEqual(scheme, that.scheme);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        if (scheme != null) {
            buffer.append(scheme.toUpperCase());
            buffer.append(' ');
        }
        if (host != null) {
            buffer.append('@');
            buffer.append(host);
            if (port >= 0) {
                buffer.append(':');
                buffer.append(port);
            }
        }
        return buffer.toString();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, host);
        hash = LangUtils.hashCode(hash, port);
        hash = LangUtils.hashCode(hash, scheme);
        return hash;
    }
}
