/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/HttpMethodRetryHandler.java,v 1.2
 * 2004/09/14 20:11:31 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

/**
 * A handler for determining if an HttpMethod should be retried after a
 * recoverable exception during execution.
 *
 * <p>
 * Classes implementing this interface must synchronize access to shared data as
 * methods of this interfrace may be executed from multiple threads
 * </p>
 *
 * @see HttpMethod#execute(HttpState, HttpConnection)
 *
 * @author Michael Becke
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 */
public interface HttpMethodRetryHandler {

    /**
     * Determines if a method should be retried after an
     * HttpRecoverableException occurs during execution.
     *
     * @param method
     *        the method being executed
     * @param exception
     *        the exception that occurred
     * @param executionCount
     *        the number of times this method has been unsuccessfully executed
     *
     * @return <code>true</code> if the method should be retried,
     *         <code>false</code> otherwise
     */
    boolean retryMethod(HttpMethod method, IOException exception, int executionCount);

}
