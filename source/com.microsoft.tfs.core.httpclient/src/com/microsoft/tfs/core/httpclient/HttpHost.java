/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/HttpHost.java,v 1.3
 * 2005/01/14 21:16:40 olegk Exp $ $Revision: 510587 $ $Date: 2007-02-22
 * 17:56:08 +0100 (Thu, 22 Feb 2007) $
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

import com.microsoft.tfs.core.httpclient.protocol.Protocol;
import com.microsoft.tfs.core.httpclient.util.LangUtils;

/**
 * Holds all of the variables needed to describe an HTTP connection to a host.
 * This includes remote host, port and protocol.
 *
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Laura Werner
 *
 * @since 3.0
 */
public class HttpHost implements Cloneable {

    /** The host to use. */
    private String hostname = null;

    /** The port to use. */
    private int port = -1;

    /** The protocol */
    private Protocol protocol = null;

    /**
     * Constructor for HttpHost.
     *
     * @param hostname
     *        the hostname (IP or DNS name). Can be <code>null</code>.
     * @param port
     *        the port. Value <code>-1</code> can be used to set default
     *        protocol port
     * @param protocol
     *        the protocol. Value <code>null</code> can be used to set default
     *        protocol
     */
    public HttpHost(final String hostname, final int port, final Protocol protocol) {
        super();
        if (hostname == null) {
            throw new IllegalArgumentException("Host name may not be null");
        }
        if (protocol == null) {
            throw new IllegalArgumentException("Protocol may not be null");
        }
        this.hostname = hostname;
        this.protocol = protocol;
        if (port >= 0) {
            this.port = port;
        } else {
            this.port = this.protocol.getDefaultPort();
        }
    }

    /**
     * Constructor for HttpHost.
     *
     * @param hostname
     *        the hostname (IP or DNS name). Can be <code>null</code>.
     * @param port
     *        the port. Value <code>-1</code> can be used to set default
     *        protocol port
     */
    public HttpHost(final String hostname, final int port) {
        this(hostname, port, Protocol.getProtocol("http"));
    }

    /**
     * Constructor for HttpHost.
     *
     * @param hostname
     *        the hostname (IP or DNS name). Can be <code>null</code>.
     */
    public HttpHost(final String hostname) {
        this(hostname, -1, Protocol.getProtocol("http"));
    }

    /**
     * URI constructor for HttpHost.
     *
     * @param uri
     *        the URI.
     */
    public HttpHost(final URI uri) throws URIException {
        this(uri.getHost(), uri.getPort(), Protocol.getProtocol(uri.getScheme()));
    }

    /**
     * Copy constructor for HttpHost
     *
     * @param httphost
     *        the HTTP host to copy details from
     */
    public HttpHost(final HttpHost httphost) {
        super();
        init(httphost);
    }

    private void init(final HttpHost httphost) {
        hostname = httphost.hostname;
        port = httphost.port;
        protocol = httphost.protocol;
    }

    /**
     * @throws CloneNotSupportedException
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final HttpHost copy = (HttpHost) super.clone();
        copy.init(this);
        return copy;
    }

    /**
     * Returns the host name (IP or DNS name).
     *
     * @return the host name (IP or DNS name), or <code>null</code> if not set
     */
    public String getHostName() {
        return hostname;
    }

    /**
     * Returns the port.
     *
     * @return the host port, or <code>-1</code> if not set
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the protocol.
     *
     * @return The protocol.
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Return the host uri.
     *
     * @return The host uri.
     */
    public String toURI() {
        final StringBuffer buffer = new StringBuffer(50);
        buffer.append(protocol.getScheme());
        buffer.append("://");
        buffer.append(hostname);
        if (port != protocol.getDefaultPort()) {
            buffer.append(':');
            buffer.append(port);
        }
        return buffer.toString();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer(50);
        buffer.append(toURI());
        return buffer.toString();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object o) {

        if (o instanceof HttpHost) {
            // shortcut if we're comparing with ourselves
            if (o == this) {
                return true;
            }
            final HttpHost that = (HttpHost) o;
            if (!hostname.equalsIgnoreCase(that.hostname)) {
                return false;
            }
            if (port != that.port) {
                return false;
            }
            if (!protocol.equals(that.protocol)) {
                return false;
            }
            // everything matches
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = LangUtils.HASH_SEED;
        hash = LangUtils.hashCode(hash, hostname);
        hash = LangUtils.hashCode(hash, port);
        hash = LangUtils.hashCode(hash, protocol);
        return hash;
    }

}
