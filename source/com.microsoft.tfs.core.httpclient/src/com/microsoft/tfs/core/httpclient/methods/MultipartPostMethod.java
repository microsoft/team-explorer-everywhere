/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/MultipartPostMethod.java,v
 * 1.27 2004/10/06 03:39:59 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.HttpConnection;
import com.microsoft.tfs.core.httpclient.HttpException;
import com.microsoft.tfs.core.httpclient.HttpState;
import com.microsoft.tfs.core.httpclient.methods.multipart.FilePart;
import com.microsoft.tfs.core.httpclient.methods.multipart.Part;
import com.microsoft.tfs.core.httpclient.methods.multipart.StringPart;

/**
 * Implements the HTTP multipart POST method.
 * <p>
 * The HTTP multipart POST method is defined in section 3.3 of
 * <a href="http://www.ietf.org/rfc/rfc1867.txt">RFC1867</a>: <blockquote> The
 * media-type multipart/form-data follows the rules of all multipart MIME data
 * streams as outlined in RFC 1521. The multipart/form-data contains a series of
 * parts. Each part is expected to contain a content-disposition header where
 * the value is "form-data" and a name attribute specifies the field name within
 * the form, e.g., 'content-disposition: form-data; name="xxxxx"', where xxxxx
 * is the field name corresponding to that field. Field names originally in
 * non-ASCII character sets may be encoded using the method outlined in RFC
 * 1522. </blockquote>
 * </p>
 * <p>
 *
 * @author <a href="mailto:mattalbright@yahoo.com">Matthew Albright</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:adrian@ephox.com">Adrian Sutton</a>
 * @author <a href="mailto:mdiggory@latte.harvard.edu">Mark Diggory</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 2.0
 *
 * @deprecated Use
 *             {@link com.microsoft.tfs.core.httpclient.methods.multipart.MultipartRequestEntity}
 *             in conjunction with
 *             {@link com.microsoft.tfs.core.httpclient.methods.PostMethod}
 *             instead.
 */
@Deprecated
public class MultipartPostMethod extends ExpectContinueMethod {

    /** The Content-Type for multipart/form-data. */
    public static final String MULTIPART_FORM_CONTENT_TYPE = "multipart/form-data";

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(MultipartPostMethod.class);

    /** The parameters for this method */
    private final List parameters = new ArrayList();

    /**
     * No-arg constructor.
     */
    public MultipartPostMethod() {
        super();
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     */
    public MultipartPostMethod(final String uri) {
        super(uri);
    }

    /**
     * Returns <tt>true</tt>
     *
     * @return <tt>true</tt>
     *
     * @since 2.0beta1
     */
    @Override
    protected boolean hasRequestContent() {
        return true;
    }

    /**
     * Returns <tt>"POST"</tt>.
     *
     * @return <tt>"POST"</tt>
     */
    @Override
    public String getName() {
        return "POST";
    }

    /**
     * Adds a text field part
     *
     * @param parameterName
     *        The name of the parameter.
     * @param parameterValue
     *        The value of the parameter.
     */
    public void addParameter(final String parameterName, final String parameterValue) {
        LOG.trace("enter addParameter(String parameterName, String parameterValue)");
        final Part param = new StringPart(parameterName, parameterValue);
        parameters.add(param);
    }

    /**
     * Adds a binary file part
     *
     * @param parameterName
     *        The name of the parameter
     * @param parameterFile
     *        The name of the file.
     * @throws FileNotFoundException
     *         If the file cannot be found.
     */
    public void addParameter(final String parameterName, final File parameterFile) throws FileNotFoundException {
        LOG.trace("enter MultipartPostMethod.addParameter(String parameterName, " + "File parameterFile)");
        final Part param = new FilePart(parameterName, parameterFile);
        parameters.add(param);
    }

    /**
     * Adds a binary file part with the given file name
     *
     * @param parameterName
     *        The name of the parameter
     * @param fileName
     *        The file name
     * @param parameterFile
     *        The file
     * @throws FileNotFoundException
     *         If the file cannot be found.
     */
    public void addParameter(final String parameterName, final String fileName, final File parameterFile)
        throws FileNotFoundException {
        LOG.trace(
            "enter MultipartPostMethod.addParameter(String parameterName, " + "String fileName, File parameterFile)");
        final Part param = new FilePart(parameterName, fileName, parameterFile);
        parameters.add(param);
    }

    /**
     * Adds a part.
     *
     * @param part
     *        The part to add.
     */
    public void addPart(final Part part) {
        LOG.trace("enter addPart(Part part)");
        parameters.add(part);
    }

    /**
     * Returns all parts.
     *
     * @return an array of containing all parts
     */
    public Part[] getParts() {
        return (Part[]) parameters.toArray(new Part[parameters.size()]);
    }

    /**
     * Adds a <tt>Content-Length</tt> request header, as long as no
     * <tt>Content-Length</tt> request header already exists.
     *
     * @param state
     *        current state of http requests
     * @param conn
     *        the connection to use for I/O
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @since 3.0
     */
    protected void addContentLengthRequestHeader(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter EntityEnclosingMethod.addContentLengthRequestHeader(" + "HttpState, HttpConnection)");

        if (getRequestHeader("Content-Length") == null) {
            final long len = getRequestContentLength();
            addRequestHeader("Content-Length", String.valueOf(len));
        }
        removeRequestHeader("Transfer-Encoding");
    }

    /**
     * Adds a <tt>Content-Type</tt> request header.
     *
     * @param state
     *        current state of http requests
     * @param conn
     *        the connection to use for I/O
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     *
     * @since 3.0
     */
    protected void addContentTypeRequestHeader(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter EntityEnclosingMethod.addContentTypeRequestHeader(" + "HttpState, HttpConnection)");

        if (!parameters.isEmpty()) {
            final StringBuffer buffer = new StringBuffer(MULTIPART_FORM_CONTENT_TYPE);
            if (Part.getBoundary() != null) {
                buffer.append("; boundary=");
                buffer.append(Part.getBoundary());
            }
            setRequestHeader("Content-Type", buffer.toString());
        }
    }

    /**
     * Populates the request headers map to with additional
     * {@link com.microsoft.tfs.core.httpclient.Header headers} to be submitted
     * to the given {@link HttpConnection}.
     *
     * <p>
     * This implementation adds tt>Content-Length</tt> and <tt>Content-Type</tt>
     * headers, when appropriate.
     * </p>
     *
     * <p>
     * Subclasses may want to override this method to to add additional headers,
     * and may choose to invoke this implementation (via <tt>super</tt>) to add
     * the "standard" headers.
     * </p>
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
     *
     * @see #writeRequestHeaders
     */
    @Override
    protected void addRequestHeaders(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter MultipartPostMethod.addRequestHeaders(HttpState state, " + "HttpConnection conn)");
        super.addRequestHeaders(state, conn);
        addContentLengthRequestHeader(state, conn);
        addContentTypeRequestHeader(state, conn);
    }

    /**
     * Writes the request body to the given {@link HttpConnection connection}.
     *
     * @param state
     *        the {@link HttpState state} information associated with this
     *        method
     * @param conn
     *        the {@link HttpConnection connection} used to execute this HTTP
     *        method
     *
     * @return <tt>true</tt>
     *
     * @throws IOException
     *         if an I/O (transport) error occurs. Some transport exceptions can
     *         be recovered from.
     * @throws HttpException
     *         if a protocol exception occurs. Usually protocol exceptions
     *         cannot be recovered from.
     */
    @Override
    protected boolean writeRequestBody(final HttpState state, final HttpConnection conn)
        throws IOException,
            HttpException {
        LOG.trace("enter MultipartPostMethod.writeRequestBody(HttpState state, " + "HttpConnection conn)");
        final OutputStream out = conn.getRequestOutputStream();
        Part.sendParts(out, getParts());
        return true;
    }

    /**
     * <p>
     * Return the length of the request body.
     * </p>
     *
     * <p>
     * Once this method has been invoked, the request parameters cannot be
     * altered until the method is {@link #recycle recycled}.
     * </p>
     *
     * @return The request content length.
     */
    protected long getRequestContentLength() throws IOException {
        LOG.trace("enter MultipartPostMethod.getRequestContentLength()");
        return Part.getLengthOfParts(getParts());
    }

    /**
     * Recycles the HTTP method so that it can be used again. Note that all of
     * the instance variables will be reset once this method has been called.
     * This method will also release the connection being used by this HTTP
     * method.
     *
     * @see #releaseConnection()
     *
     * @deprecated no longer supported and will be removed in the future version
     *             of HttpClient
     */
    @Deprecated
    @Override
    public void recycle() {
        LOG.trace("enter MultipartPostMethod.recycle()");
        super.recycle();
        parameters.clear();
    }
}
