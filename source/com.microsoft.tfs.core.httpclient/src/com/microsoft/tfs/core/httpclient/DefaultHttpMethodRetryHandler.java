/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/DefaultHttpMethodRetryHandler.java,v
 * 1.3 2004/12/20 11:47:46 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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
import java.io.InterruptedIOException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * The default {@link HttpMethodRetryHandler} used by {@link HttpMethod}s.
 *
 * @author Michael Becke
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 */
public class DefaultHttpMethodRetryHandler implements HttpMethodRetryHandler {

    private static final String MAX_REQUEST_RETRY_PROPERTY = "com.microsoft.tfs.core.maxRequestRetry"; //$NON-NLS-1$
    private static final int MAX_REQUEST_RETRY_DEFAULT = 3;

    private static Class SSL_HANDSHAKE_EXCEPTION = null;

    static {
        try {
            SSL_HANDSHAKE_EXCEPTION = Class.forName("javax.net.ssl.SSLHandshakeException");
        } catch (final ClassNotFoundException ignore) {
        }
    }

    /** the number of times a method will be retried */
    private final int retryCount;

    /**
     * Whether or not methods that have successfully sent their request will be
     * retried
     */
    private final boolean requestSentRetryEnabled;

    /**
     * Creates a new DefaultHttpMethodRetryHandler.
     *
     * @param retryCount
     *        the number of times a method will be retried
     * @param requestSentRetryEnabled
     *        if true, methods that have successfully sent their request will be
     *        retried
     */
    public DefaultHttpMethodRetryHandler(final int retryCount, final boolean requestSentRetryEnabled) {
        super();
        this.retryCount = retryCount;
        this.requestSentRetryEnabled = requestSentRetryEnabled;
    }

    /**
     * Creates a new DefaultHttpMethodRetryHandler that retries up to 3 times
     * but does not retry methods that have successfully sent their requests.
     */
    public DefaultHttpMethodRetryHandler() {
        this(Integer.getInteger(MAX_REQUEST_RETRY_PROPERTY, MAX_REQUEST_RETRY_DEFAULT), false);
    }

    /**
     * Used <code>retryCount</code> and <code>requestSentRetryEnabled</code> to
     * determine if the given method should be retried.
     *
     * @see HttpMethodRetryHandler#retryMethod(HttpMethod, IOException, int)
     */
    @Override
    public boolean retryMethod(final HttpMethod method, final IOException exception, final int executionCount) {
        if (method == null) {
            throw new IllegalArgumentException("HTTP method may not be null");
        }
        if (exception == null) {
            throw new IllegalArgumentException("Exception parameter may not be null");
        }
        // HttpMethod interface is the WORST thing ever done to HttpClient
        if (method instanceof HttpMethodBase) {
            if (((HttpMethodBase) method).isAborted()) {
                return false;
            }
        }
        if (executionCount > retryCount) {
            // Do not retry if over max retry count
            return false;
        }
        if (exception instanceof NoHttpResponseException) {
            // Retry if the server dropped connection on us
            return true;
        }
        if (exception instanceof InterruptedIOException || exception instanceof SocketException) {
            // Timeout
            return !method.isRequestSent();
        }
        if (exception instanceof UnknownHostException) {
            // Unknown host
            return false;
        }
        if (exception instanceof NoRouteToHostException) {
            // Host unreachable
            return false;
        }
        if (SSL_HANDSHAKE_EXCEPTION != null && SSL_HANDSHAKE_EXCEPTION.isInstance(exception)) {
            // SSL handshake exception
            return false;
        }
        if (!method.isRequestSent() || requestSentRetryEnabled) {
            // Retry if the request has not been sent fully or
            // if it's OK to retry methods that have been sent
            return true;
        }
        // otherwise do not retry
        return false;
    }

    /**
     * @return <code>true</code> if this handler will retry methods that have
     *         successfully sent their request, <code>false</code> otherwise
     */
    public boolean isRequestSentRetryEnabled() {
        return requestSentRetryEnabled;
    }

    /**
     * @return the maximum number of times a method will be retried
     */
    public int getRetryCount() {
        return retryCount;
    }
}
