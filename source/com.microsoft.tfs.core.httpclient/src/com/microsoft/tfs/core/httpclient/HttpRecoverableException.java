/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/HttpRecoverableException.java,v 1.15
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

/**
 * <p>
 * Signals that an HTTP or HttpClient exception has occurred. This exception may
 * have been caused by a transient error and the request may be retried. It may
 * be possible to retrieve the underlying transient error via the inherited
 * {@link HttpException#getCause()} method.
 * </p>
 *
 * @deprecated no longer used
 *
 * @author Unascribed
 * @version $Revision: 480424 $ $Date: 2006-11-29 06:56:49 +0100 (Wed, 29 Nov
 *          2006) $
 */
@Deprecated
public class HttpRecoverableException extends HttpException {

    /**
     * Creates a new HttpRecoverableException with a <tt>null</tt> detail
     * message.
     */
    public HttpRecoverableException() {
        super();
    }

    /**
     * Creates a new HttpRecoverableException with the specified detail message.
     *
     * @param message
     *        exception message
     */
    public HttpRecoverableException(final String message) {
        super(message);
    }

}
