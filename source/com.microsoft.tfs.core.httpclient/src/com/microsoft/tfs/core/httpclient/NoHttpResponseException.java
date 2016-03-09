/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/NoHttpResponseException.java,v 1.1
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

import java.io.IOException;

import com.microsoft.tfs.core.httpclient.util.ExceptionUtil;

/**
 * <p>
 * Signals that the target server failed to respond with a valid HTTP response.
 * </p>
 *
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 *
 * @version $Revision: 480424 $
 */
public class NoHttpResponseException extends IOException {

    /**
     * Creates a new NoHttpResponseException with a <tt>null</tt> detail
     * message.
     */
    public NoHttpResponseException() {
        super();
    }

    /**
     * Creates a new NoHttpResponseException with the specified detail message.
     *
     * @param message
     *        exception message
     */
    public NoHttpResponseException(final String message) {
        super(message);
    }

    /**
     * Creates a new NoHttpResponseException with the specified detail message
     * and cause.
     *
     * @param message
     *        the exception detail message
     * @param cause
     *        the <tt>Throwable</tt> that caused this exception, or
     *        <tt>null</tt> if the cause is unavailable, unknown, or not a
     *        <tt>Throwable</tt>
     *
     * @since 3.0
     */
    public NoHttpResponseException(final String message, final Throwable cause) {
        super(message);
        // If we're running on JDK 1.4 or later, tell Throwable what the cause
        // was
        ExceptionUtil.initCause(this, cause);
    }
}
