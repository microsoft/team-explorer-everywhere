/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/ConnectTimeoutException.java,v 1.5
 * 2004/07/05 22:46:58 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import java.io.InterruptedIOException;

import com.microsoft.tfs.core.httpclient.util.ExceptionUtil;

/**
 * A timeout while connecting to an HTTP server or waiting for an available
 * connection from an HttpConnectionManager.
 *
 * @author <a href="mailto:laura@lwerner.org">Laura Werner</a>
 *
 * @since 3.0
 */
public class ConnectTimeoutException extends InterruptedIOException {

    /**
     * Creates a ConnectTimeoutException with a <tt>null</tt> detail message.
     */
    public ConnectTimeoutException() {
        super();
    }

    /**
     * Creates a ConnectTimeoutException with the specified detail message.
     *
     * @param message
     *        The exception detail message
     */
    public ConnectTimeoutException(final String message) {
        super(message);
    }

    /**
     * Creates a new ConnectTimeoutException with the specified detail message
     * and cause.
     *
     * @param message
     *        the exception detail message
     * @param cause
     *        the <tt>Throwable</tt> that caused this exception, or
     *        <tt>null</tt> if the cause is unavailable, unknown, or not a
     *        <tt>Throwable</tt>
     */
    public ConnectTimeoutException(final String message, final Throwable cause) {
        super(message);
        // If we're running on JDK 1.4 or later, tell Throwable what the cause
        // was
        ExceptionUtil.initCause(this, cause);
    }

}
