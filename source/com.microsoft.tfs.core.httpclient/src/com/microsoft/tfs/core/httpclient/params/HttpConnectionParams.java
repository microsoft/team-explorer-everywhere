/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/params/HttpConnectionParams.java,v
 * 1.6 2004/09/15 20:32:21 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.params;

/**
 * This class represents a collection of HTTP protocol parameters applicable to
 * {@link com.microsoft.tfs.core.httpclient.HttpConnection HTTP connections}.
 * Protocol parameters may be linked together to form a hierarchy. If a
 * particular parameter value has not been explicitly defined in the collection
 * itself, its value will be drawn from the parent collection of parameters.
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 480424 $
 *
 * @since 3.0
 */
public class HttpConnectionParams extends DefaultHttpParams {

    /**
     * Defines the default socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds
     * which is the timeout for waiting for data. A timeout value of zero is
     * interpreted as an infinite timeout. This value is used when no socket
     * timeout is set in the {@link HttpMethodParams HTTP method parameters}.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     *
     * @see java.net.SocketOptions#SO_TIMEOUT
     */
    public static final String SO_TIMEOUT = "http.socket.timeout";

    /**
     * Determines whether Nagle's algorithm is to be used. The Nagle's algorithm
     * tries to conserve bandwidth by minimizing the number of segments that are
     * sent. When applications wish to decrease network latency and increase
     * performance, they can disable Nagle's algorithm (that is enable
     * TCP_NODELAY). Data will be sent earlier, at the cost of an increase in
     * bandwidth consumption.
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     *
     * @see java.net.SocketOptions#TCP_NODELAY
     */
    public static final String TCP_NODELAY = "http.tcp.nodelay";

    /**
     * Determines a hint the size of the underlying buffers used by the platform
     * for outgoing network I/O. This value is a suggestion to the kernel from
     * the application about the size of buffers to use for the data to be sent
     * over the socket.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     *
     * @see java.net.SocketOptions#SO_SNDBUF
     */
    public static final String SO_SNDBUF = "http.socket.sendbuffer";

    /**
     * Determines a hint the size of the underlying buffers used by the platform
     * for incoming network I/O. This value is a suggestion to the kernel from
     * the application about the size of buffers to use for the data to be
     * received over the socket.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     *
     * @see java.net.SocketOptions#SO_RCVBUF
     */
    public static final String SO_RCVBUF = "http.socket.receivebuffer";

    /**
     * Sets SO_LINGER with the specified linger time in seconds. The maximum
     * timeout value is platform specific. Value <tt>0</tt> implies that the
     * option is disabled. Value <tt>-1</tt> implies that the JRE default is
     * used. The setting only affects socket close.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     *
     * @see java.net.SocketOptions#SO_LINGER
     */
    public static final String SO_LINGER = "http.socket.linger";

    /**
     * Determines the timeout until a connection is etablished. A value of zero
     * means the timeout is not used. The default value is zero.
     * <p>
     * This parameter expects a value of type {@link Integer}.
     * </p>
     */
    public static final String CONNECTION_TIMEOUT = "http.connection.timeout";

    /**
     * Determines whether stale connection check is to be used. Disabling stale
     * connection check may result in slight performance improvement at the risk
     * of getting an I/O error when executing a request over a connection that
     * has been closed at the server side.
     * <p>
     * This parameter expects a value of type {@link Boolean}.
     * </p>
     */
    public static final String STALE_CONNECTION_CHECK = "http.connection.stalecheck";

    /**
     * Creates a new collection of parameters with the collection returned by
     * {@link #getDefaultParams()} as a parent. The collection will defer to its
     * parent for a default value if a particular parameter is not explicitly
     * set in the collection itself.
     *
     * @see #getDefaultParams()
     */
    public HttpConnectionParams() {
        super();
    }

    /**
     * Returns the default socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds
     * which is the timeout for waiting for data. A timeout value of zero is
     * interpreted as an infinite timeout. This value is used when no socket
     * timeout is set in the {@link HttpMethodParams HTTP method parameters}.
     *
     * @return timeout in milliseconds
     */
    public int getSoTimeout() {
        return getIntParameter(SO_TIMEOUT, 0);
    }

    /**
     * Sets the default socket timeout (<tt>SO_TIMEOUT</tt>) in milliseconds
     * which is the timeout for waiting for data. A timeout value of zero is
     * interpreted as an infinite timeout. This value is used when no socket
     * timeout is set in the {@link HttpMethodParams HTTP method parameters}.
     *
     * @param timeout
     *        Timeout in milliseconds
     */
    public void setSoTimeout(final int timeout) {
        setIntParameter(SO_TIMEOUT, timeout);
    }

    /**
     * Determines whether Nagle's algorithm is to be used. The Nagle's algorithm
     * tries to conserve bandwidth by minimizing the number of segments that are
     * sent. When applications wish to decrease network latency and increase
     * performance, they can disable Nagle's algorithm (that is enable
     * TCP_NODELAY). Data will be sent earlier, at the cost of an increase in
     * bandwidth consumption.
     *
     * @param value
     *        <tt>true</tt> if the Nagle's algorithm is to NOT be used (that is
     *        enable TCP_NODELAY), <tt>false</tt> otherwise.
     */
    public void setTcpNoDelay(final boolean value) {
        setBooleanParameter(TCP_NODELAY, value);
    }

    /**
     * Tests if Nagle's algorithm is to be used.
     *
     * @return <tt>true</tt> if the Nagle's algorithm is to NOT be used (that is
     *         enable TCP_NODELAY), <tt>false</tt> otherwise.
     */
    public boolean getTcpNoDelay() {
        return getBooleanParameter(TCP_NODELAY, true);
    }

    /**
     * Returns a hint the size of the underlying buffers used by the platform
     * for outgoing network I/O. This value is a suggestion to the kernel from
     * the application about the size of buffers to use for the data to be sent
     * over the socket.
     *
     * @return the hint size of the send buffer
     */
    public int getSendBufferSize() {
        return getIntParameter(SO_SNDBUF, -1);
    }

    /**
     * Sets a hint the size of the underlying buffers used by the platform for
     * outgoing network I/O. This value is a suggestion to the kernel from the
     * application about the size of buffers to use for the data to be sent over
     * the socket.
     *
     * @param size
     *        the hint size of the send buffer
     */
    public void setSendBufferSize(final int size) {
        setIntParameter(SO_SNDBUF, size);
    }

    /**
     * Returns a hint the size of the underlying buffers used by the platform
     * for incoming network I/O. This value is a suggestion to the kernel from
     * the application about the size of buffers to use for the data to be
     * received over the socket.
     *
     * @return the hint size of the send buffer
     */
    public int getReceiveBufferSize() {
        return getIntParameter(SO_RCVBUF, -1);
    }

    /**
     * Sets a hint the size of the underlying buffers used by the platform for
     * incoming network I/O. This value is a suggestion to the kernel from the
     * application about the size of buffers to use for the data to be received
     * over the socket.
     *
     * @param size
     *        the hint size of the send buffer
     */
    public void setReceiveBufferSize(final int size) {
        setIntParameter(SO_RCVBUF, size);
    }

    /**
     * Returns linger-on-close timeout. Value <tt>0</tt> implies that the option
     * is disabled. Value <tt>-1</tt> implies that the JRE default is used.
     *
     * @return the linger-on-close timeout
     */
    public int getLinger() {
        return getIntParameter(SO_LINGER, -1);
    }

    /**
     * Returns linger-on-close timeout. This option disables/enables immediate
     * return from a close() of a TCP Socket. Enabling this option with a
     * non-zero Integer timeout means that a close() will block pending the
     * transmission and acknowledgement of all data written to the peer, at
     * which point the socket is closed gracefully. Value <tt>0</tt> implies
     * that the option is disabled. Value <tt>-1</tt> implies that the JRE
     * default is used.
     *
     * @param value
     *        the linger-on-close timeout
     */
    public void setLinger(final int value) {
        setIntParameter(SO_LINGER, value);
    }

    /**
     * Returns the timeout until a connection is etablished. A value of zero
     * means the timeout is not used. The default value is zero.
     *
     * @return timeout in milliseconds.
     */
    public int getConnectionTimeout() {
        return getIntParameter(CONNECTION_TIMEOUT, 0);
    }

    /**
     * Sets the timeout until a connection is etablished. A value of zero means
     * the timeout is not used. The default value is zero.
     *
     * @param timeout
     *        Timeout in milliseconds.
     */
    public void setConnectionTimeout(final int timeout) {
        setIntParameter(CONNECTION_TIMEOUT, timeout);
    }

    /**
     * Tests whether stale connection check is to be used. Disabling stale
     * connection check may result in slight performance improvement at the risk
     * of getting an I/O error when executing a request over a connection that
     * has been closed at the server side.
     *
     * @return <tt>true</tt> if stale connection check is to be used,
     *         <tt>false</tt> otherwise.
     */
    public boolean isStaleCheckingEnabled() {
        return getBooleanParameter(STALE_CONNECTION_CHECK, true);
    }

    /**
     * Defines whether stale connection check is to be used. Disabling stale
     * connection check may result in slight performance improvement at the risk
     * of getting an I/O error when executing a request over a connection that
     * has been closed at the server side.
     *
     * @param value
     *        <tt>true</tt> if stale connection check is to be used,
     *        <tt>false</tt> otherwise.
     */
    public void setStaleCheckingEnabled(final boolean value) {
        setBooleanParameter(STALE_CONNECTION_CHECK, value);
    }
}
