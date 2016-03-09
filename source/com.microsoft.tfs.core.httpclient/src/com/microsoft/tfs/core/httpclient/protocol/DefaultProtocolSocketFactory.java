/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/protocol/DefaultProtocolSocketFactory
 * .java,v 1.10 2004/05/13 04:01:22 mbecke Exp $ $Revision: 480424 $ $Date:
 * 2006-11-29 06:56:49 +0100 (Wed, 29 Nov 2006) $
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

package com.microsoft.tfs.core.httpclient.protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import com.microsoft.tfs.core.httpclient.ConnectTimeoutException;
import com.microsoft.tfs.core.httpclient.params.HttpConnectionParams;

/**
 * The default class for creating protocol sockets. This class just uses the
 * {@link java.net.Socket socket} constructors.
 *
 * @author Michael Becke
 *
 * @since 2.0
 */
public class DefaultProtocolSocketFactory implements ProtocolSocketFactory {

    /**
     * The factory singleton.
     */
    private static final DefaultProtocolSocketFactory factory = new DefaultProtocolSocketFactory();

    /**
     * Gets an singleton instance of the DefaultProtocolSocketFactory.
     *
     * @return a DefaultProtocolSocketFactory
     */
    static DefaultProtocolSocketFactory getSocketFactory() {
        return factory;
    }

    /**
     * Constructor for DefaultProtocolSocketFactory.
     */
    public DefaultProtocolSocketFactory() {
        super();
    }

    /**
     * @see #createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort)
        throws IOException,
            UnknownHostException {
        return new Socket(host, port, localAddress, localPort);
    }

    /**
     * Attempts to get a new socket connection to the given host within the
     * given time limit.
     * <p>
     * This method employs several techniques to circumvent the limitations of
     * older JREs that do not support connect timeout. When running in JRE 1.4
     * or above reflection is used to call Socket#connect(SocketAddress
     * endpoint, int timeout) method. When executing in older JREs a controller
     * thread is executed. The controller thread attempts to create a new socket
     * within the given limit of time. If socket constructor does not return
     * until the timeout expires, the controller terminates and throws an
     * {@link ConnectTimeoutException}
     * </p>
     *
     * @param host
     *        the host name/IP
     * @param port
     *        the port on the host
     * @param localAddress
     *        the local host name/IP to bind the socket to
     * @param localPort
     *        the port on the local machine
     * @param params
     *        {@link HttpConnectionParams Http connection parameters}
     *
     * @return Socket a new socket
     *
     * @throws IOException
     *         if an I/O error occurs while creating the socket
     * @throws UnknownHostException
     *         if the IP address of the host cannot be determined
     * @throws ConnectTimeoutException
     *         if socket cannot be connected within the given time limit
     *
     * @since 3.0
     */
    @Override
    public Socket createSocket(
        final String host,
        final int port,
        final InetAddress localAddress,
        final int localPort,
        final HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        final int timeout = params.getConnectionTimeout();
        final SocketFactory socketFactory = SocketFactory.getDefault();
        final Socket socket = socketFactory.createSocket();
        final SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
        final SocketAddress remoteaddr = new InetSocketAddress(host, port);
        socket.bind(localaddr);
        socket.connect(remoteaddr, timeout);
        return socket;
    }

    /**
     * @see ProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
        return new Socket(host, port);
    }

    /**
     * All instances of DefaultProtocolSocketFactory are the same.
     */
    @Override
    public boolean equals(final Object obj) {
        return ((obj != null) && obj.getClass().equals(getClass()));
    }

    /**
     * All instances of DefaultProtocolSocketFactory have the same hash code.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
