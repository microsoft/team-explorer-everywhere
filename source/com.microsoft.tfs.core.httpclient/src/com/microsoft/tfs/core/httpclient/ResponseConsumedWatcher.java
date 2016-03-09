/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/ResponseConsumedWatcher.java,v 1.5
 * 2004/04/18 23:51:35 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
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
 * When a response stream has been consumed, various parts of the HttpClient
 * implementation need to respond appropriately.
 *
 * <p>
 * When one of the three types of {@link java.io.InputStream}, one of
 * AutoCloseInputStream (package), {@link ContentLengthInputStream}, or
 * {@link ChunkedInputStream} finishes with its content, either because all
 * content has been consumed, or because it was explicitly closed, it notifies
 * its corresponding method via this interface.
 * </p>
 *
 * @see ContentLengthInputStream
 * @see ChunkedInputStream
 * @author Eric Johnson
 */
interface ResponseConsumedWatcher {

    /**
     * A response has been consumed.
     */
    void responseConsumed();
}
