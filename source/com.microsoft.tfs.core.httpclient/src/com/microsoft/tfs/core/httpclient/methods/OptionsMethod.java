/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/OptionsMethod.java,v 1.15
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

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpConnection;
import com.microsoft.tfs.core.httpclient.HttpMethodBase;
import com.microsoft.tfs.core.httpclient.HttpState;

/**
 * Implements the HTTP OPTIONS method.
 * <p>
 * The HTTP OPTIONS method is defined in section 9.2 of
 * <a href="http://www.ietf.org/rfc/rfc2616.txt">RFC2616</a>: <blockquote> The
 * OPTIONS method represents a request for information about the communication
 * options available on the request/response chain identified by the
 * Request-URI. This method allows the client to determine the options and/or
 * requirements associated with a resource, or the capabilities of a server,
 * without implying a resource action or initiating a resource
 * retrieval. </blockquote>
 * </p>
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 *
 * @version $Revision: 480424 $
 * @since 1.0
 */
public class OptionsMethod extends HttpMethodBase {

    // --------------------------------------------------------- Class Variables

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(OptionsMethod.class);

    // ----------------------------------------------------------- Constructors

    /**
     * Method constructor.
     *
     * @since 1.0
     */
    public OptionsMethod() {
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     *
     * @since 1.0
     */
    public OptionsMethod(final String uri) {
        super(uri);
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * Methods allowed.
     */
    private final Vector methodsAllowed = new Vector();

    // --------------------------------------------------------- Public Methods

    /**
     * Get the name.
     *
     * @return "OPTIONS"
     * @since 2.0
     */
    @Override
    public String getName() {
        return "OPTIONS";
    }

    /**
     * Is the specified method allowed ?
     *
     * @param method
     *        The method to check.
     * @return true if the specified method is allowed.
     * @since 1.0
     */
    public boolean isAllowed(final String method) {
        checkUsed();
        return methodsAllowed.contains(method);
    }

    /**
     * Get a list of allowed methods.
     *
     * @return An enumeration of all the allowed methods.
     *
     * @since 1.0
     */
    public Enumeration getAllowedMethods() {
        checkUsed();
        return methodsAllowed.elements();
    }

    // ----------------------------------------------------- HttpMethod Methods

    /**
     * <p>
     * This implementation will parse the <tt>Allow</tt> header to obtain the
     * set of methods supported by the resource identified by the Request-URI.
     * </p>
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @see #readResponse
     * @see #readResponseHeaders
     * @since 2.0
     */
    @Override
    protected void processResponseHeaders(final HttpState state, final HttpConnection conn) {
        LOG.trace("enter OptionsMethod.processResponseHeaders(HttpState, HttpConnection)");

        final Header allowHeader = getResponseHeader("allow");
        if (allowHeader != null) {
            final String allowHeaderValue = allowHeader.getValue();
            final StringTokenizer tokenizer = new StringTokenizer(allowHeaderValue, ",");
            while (tokenizer.hasMoreElements()) {
                final String methodAllowed = tokenizer.nextToken().trim().toUpperCase();
                methodsAllowed.addElement(methodAllowed);
            }
        }
    }

    /**
     * Return true if the method needs a content-length header in the request.
     *
     * @return true if a content-length header will be expected by the server
     *
     * @since 1.0
     *
     * @deprecated only entity enclosing methods set content length header
     */
    @Deprecated
    public boolean needContentLength() {
        return false;
    }

}
