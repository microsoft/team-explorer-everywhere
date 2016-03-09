/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/ProxyHost.java,v 1.2
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

/**
 * Holds all of the variables needed to describe an HTTP connection to a proxy.
 * Proxy hosts always use plain HTTP connection when communicating with clients.
 *
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 * @author Laura Werner
 *
 * @since 3.0
 */
public class ProxyHost extends HttpHost {

    /**
     * Copy constructor for HttpHost
     *
     * @param httpproxy
     *        the HTTP host to copy details from
     */
    public ProxyHost(final ProxyHost httpproxy) {
        super(httpproxy);
    }

    /**
     * Constructor for ProxyHost.
     *
     * @param hostname
     *        the hostname (IP or DNS name). Can be <code>null</code>.
     * @param port
     *        the port. Value <code>-1</code> can be used to set default
     *        protocol port
     */
    public ProxyHost(final String hostname, final int port) {
        super(hostname, port, Protocol.getProtocol("http"));
    }

    /**
     * Constructor for HttpHost.
     *
     * @param hostname
     *        the hostname (IP or DNS name). Can be <code>null</code>.
     */
    public ProxyHost(final String hostname) {
        this(hostname, -1);
    }

    /**
     * @throws CloneNotSupportedException
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        final ProxyHost copy = (ProxyHost) super.clone();
        return copy;
    }

}
