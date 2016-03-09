/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/DeleteMethod.java,v 1.14
 * 2004/04/18 23:51:37 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import com.microsoft.tfs.core.httpclient.HttpMethodBase;

/**
 * Implements the HTTP DELETE method.
 * <p>
 * The HTTP DELETE method is defined in section 9.7 of
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>: <blockquote> The
 * DELETE method requests that the origin server delete the resource identified
 * by the Request-URI. This method MAY be overridden by human intervention (or
 * other means) on the origin server. </blockquote>
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author <a href="mailto:bcholmes@apache.org">B.C. Holmes</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 *
 * @version $Revision: 480424 $
 * @since 1.0
 */
public class DeleteMethod extends HttpMethodBase {

    // ----------------------------------------------------------- Constructors

    /**
     * No-arg constructor.
     *
     * @since 1.0
     */
    public DeleteMethod() {
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     *
     * @since 1.0
     */
    public DeleteMethod(final String uri) {
        super(uri);
    }

    // ----------------------------------------------------- HttpMethod Methods

    /**
     * Returns <tt>"DELETE"</tt>.
     *
     * @return <tt>"DELETE"</tt>
     *
     * @since 2.0
     */
    @Override
    public String getName() {
        return "DELETE";
    }

}
