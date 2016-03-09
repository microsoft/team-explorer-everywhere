/*
 * $Header: $ $Revision: 480424 $ $Date: 2006-11-29 06:56:49 +0100 (Wed, 29 Nov
 * 2006) $
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

import com.microsoft.tfs.core.httpclient.HttpMethodBase;

/**
 * Implements the HTTP TRACE method.
 * <p>
 * The HTTP TRACE method is defined in section 9.6 of
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>: <blockquote> The
 * TRACE method is used to invoke a remote, application-layer loop- back of the
 * request message. The final recipient of the request SHOULD reflect the
 * message received back to the client as the entity-body of a 200 (OK)
 * response. The final recipient is either the origin server or the first proxy
 * or gateway to receive a Max-Forwards value of zero (0) in the request (see
 * section 14.31). A TRACE request MUST NOT include an entity. </blockquote>
 * </p>
 *
 * @author Sean C. Sullivan
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 *
 * @version $Revision: 480424 $
 * @since 2.0
 *
 */
public class TraceMethod extends HttpMethodBase {

    // ~ Constructors

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     *
     * @since 2.0
     *
     */
    public TraceMethod(final String uri) {
        super(uri);
        setFollowRedirects(false);
    }

    // ~ Methods

    /**
     * Returns <tt>"TRACE"</tt>.
     *
     * @return <tt>"TRACE"</tt>
     *
     * @since 2.0
     *
     */
    @Override
    public String getName() {
        return "TRACE";
    }

    /**
     * Recycles the HTTP method so that it can be used again. Note that all of
     * the instance variables will be reset once this method has been called.
     * This method will also release the connection being used by this HTTP
     * method.
     *
     * @see #releaseConnection()
     *
     * @since 2.0
     *
     * @deprecated no longer supported and will be removed in the future version
     *             of HttpClient
     */
    @Deprecated
    @Override
    public void recycle() {
        super.recycle();
        setFollowRedirects(false);
    }

}
