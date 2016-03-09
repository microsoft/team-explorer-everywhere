/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/methods/GetMethod.java,v
 * 1.29 2004/06/13 20:22:19 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.HttpMethodBase;

/**
 * Implements the HTTP GET method.
 * <p>
 * The HTTP GET method is defined in section 9.3 of
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>: <blockquote> The
 * GET method means retrieve whatever information (in the form of an entity) is
 * identified by the Request-URI. If the Request-URI refers to a data-producing
 * process, it is the produced data which shall be returned as the entity in the
 * response and not the source text of the process, unless that text happens to
 * be the output of the process. </blockquote>
 * </p>
 * <p>
 * GetMethods will follow redirect requests from the http server by default.
 * This behavour can be disabled by calling setFollowRedirects(false).
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Sung-Gu Park
 * @author Sean C. Sullivan
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 *
 * @version $Revision: 480424 $
 * @since 1.0
 */
public class GetMethod extends HttpMethodBase {

    // -------------------------------------------------------------- Constants

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(GetMethod.class);

    // ----------------------------------------------------------- Constructors

    /**
     * No-arg constructor.
     *
     * @since 1.0
     */
    public GetMethod() {
        setFollowRedirects(true);
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     *
     * @since 1.0
     */
    public GetMethod(final String uri) {
        super(uri);
        LOG.trace("enter GetMethod(String)");
        setFollowRedirects(true);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Returns <tt>"GET"</tt>.
     *
     * @return <tt>"GET"</tt>
     *
     * @since 2.0
     */
    @Override
    public String getName() {
        return "GET";
    }

    // ------------------------------------------------------------- Properties

    /**
     * Recycles the HTTP method so that it can be used again. Note that all of
     * the instance variables will be reset once this method has been called.
     * This method will also release the connection being used by this HTTP
     * method.
     *
     * @see #releaseConnection()
     *
     * @since 1.0
     *
     * @deprecated no longer supported and will be removed in the future version
     *             of HttpClient
     */
    @Deprecated
    @Override
    public void recycle() {
        LOG.trace("enter GetMethod.recycle()");

        super.recycle();
        setFollowRedirects(true);
    }

}
