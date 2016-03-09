/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/ExpectContinueMethod.java,v
 * 1.13 2004/05/08 10:12:08 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.methods;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.HttpConnection;
import com.microsoft.tfs.core.httpclient.HttpException;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.HttpVersion;
import com.microsoft.tfs.core.httpclient.params.HttpMethodParams;

/**
 * <p>
 * This abstract class serves as a foundation for all HTTP methods that support
 * 'Expect: 100-continue' handshake.
 * </p>
 *
 * <p>
 * The purpose of the 100 (Continue) status (refer to section 10.1.1 of the RFC
 * 2616 for more details) is to allow a client that is sending a request message
 * with a request body to determine if the origin server is willing to accept
 * the request (based on the request headers) before the client sends the
 * request body. In some cases, it might either be inappropriate or highly
 * inefficient for the client to send the body if the server will reject the
 * message without looking at the body.
 * </p>
 *
 * <p>
 * 'Expect: 100-continue' handshake should be used with caution, as it may cause
 * problems with HTTP servers and proxies that do not support HTTP/1.1 protocol.
 * </p>
 *
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 2.0beta1
 */

public abstract class ExpectContinueMethod extends HttpMethodBase {

    /** LOG object for this class. */
    private static final Log LOG = LogFactory.getLog(ExpectContinueMethod.class);

    /**
     * No-arg constructor.
     *
     * @since 2.0
     */
    public ExpectContinueMethod() {
        super();
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     *
     * @since 2.0
     */
    public ExpectContinueMethod(final String uri) {
        super(uri);
    }

    /**
     * <p>
     * Returns <tt>true</tt> if the 'Expect: 100-Continue' handshake is
     * activated. The purpose of the 'Expect: 100-Continue' handshake to allow a
     * client that is sending a request message with a request body to determine
     * if the origin server is willing to accept the request (based on the
     * request headers) before the client sends the request body.
     * </p>
     *
     * @return <tt>true</tt> if 'Expect: 100-Continue' handshake is to be used,
     *         <tt>false</tt> otherwise.
     *
     * @since 2.0beta1
     *
     * @deprecated Use {@link HttpMethodParams}
     *
     * @see #getParams()
     * @see HttpMethodParams
     * @see HttpMethodParams#USE_EXPECT_CONTINUE
     */
    @Deprecated
    public boolean getUseExpectHeader() {
        return getParams().getBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, false);
    }

    /**
     * <p>
     * Activates 'Expect: 100-Continue' handshake. The purpose of the 'Expect:
     * 100-Continue' handshake to allow a client that is sending a request
     * message with a request body to determine if the origin server is willing
     * to accept the request (based on the request headers) before the client
     * sends the request body.
     * </p>
     *
     * <p>
     * The use of the 'Expect: 100-continue' handshake can result in noticable
     * peformance improvement for entity enclosing requests (such as POST and
     * PUT) that require the target server's authentication.
     * </p>
     *
     * <p>
     * 'Expect: 100-continue' handshake should be used with caution, as it may
     * cause problems with HTTP servers and proxies that do not support HTTP/1.1
     * protocol.
     * </p>
     *
     * @param value
     *        boolean value
     *
     * @since 2.0beta1
     *
     * @deprecated Use {@link HttpMethodParams}
     *
     * @see #getParams()
     * @see HttpMethodParams
     * @see HttpMethodParams#USE_EXPECT_CONTINUE
     */
    @Deprecated
    public void setUseExpectHeader(final boolean value) {
        getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, value);
    }

    /**
     * Returns <tt>true</tt> if there is a request body to be sent. 'Expect:
     * 100-continue' handshake may not be used if request body is not present
     *
     * @return boolean
     *
     * @since 2.0beta1
     */
    protected abstract boolean hasRequestContent();

    /**
     * Sets the <tt>Expect</tt> header if it has not already been set, in
     * addition to the "standard" set of headers.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    @Override
    protected void addRequestHeaders(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter ExpectContinueMethod.addRequestHeaders(HttpState, HttpConnection)");

        super.addRequestHeaders(state, conn);
        // If the request is being retried, the header may already be present
        final boolean headerPresent = (getRequestHeader("Expect") != null);
        // See if the expect header should be sent
        // = HTTP/1.1 or higher
        // = request body present

        if (getParams().isParameterTrue(HttpMethodParams.USE_EXPECT_CONTINUE)
            && getEffectiveVersion().greaterEquals(HttpVersion.HTTP_1_1)
            && hasRequestContent()) {
            if (!headerPresent) {
                setRequestHeader("Expect", "100-continue");
            }
        } else {
            if (headerPresent) {
                removeRequestHeader("Expect");
            }
        }
    }
}
