/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/util/HttpURLConnection.java,v 1.15
 * 2004/04/18 23:51:38 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpMethod;

/**
 * Provides a <code>HttpURLConnection</code> wrapper around HttpClient's
 * <code>HttpMethod</code>. This allows existing code to easily switch to
 * HttpClieht without breaking existing interfaces using the JDK
 * <code>HttpURLConnection</code>.
 *
 * Note 1: The current implementations wraps only a connected
 * <code>HttpMethod</code>, ie a method that has alreayd been used to connect to
 * an HTTP server.
 *
 * Note 2: It is a best try effort as different version of the JDK have
 * different behaviours for <code>HttpURLConnection</code> (And I'm not even
 * including the numerous <code>HttpURLConnection</code> bugs!).
 *
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 *
 * @since 2.0
 *
 * @version $Id: HttpURLConnection.java 480424 2006-11-29 05:56:49Z bayard $
 */
public class HttpURLConnection extends java.net.HttpURLConnection {

    // -------------------------------------------------------- Class Variables

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(HttpURLConnection.class);

    // ----------------------------------------------------- Instance Variables

    /**
     * The <code>HttpMethod</code> object that was used to connect to the HTTP
     * server. It contains all the returned data.
     */
    private HttpMethod method;

    /**
     * The URL to which we are connected
     */
    private URL url;

    // ----------------------------------------------------------- Constructors

    /**
     * Creates an <code>HttpURLConnection</code> from a <code>HttpMethod</code>.
     *
     * @param method
     *        the theMethod that was used to connect to the HTTP server and
     *        which contains the returned data.
     * @param url
     *        the URL to which we are connected (includes query string)
     */
    public HttpURLConnection(final HttpMethod method, final URL url) {
        super(url);
        this.method = method;
        this.url = url;
    }

    /**
     * Create an instance.
     *
     * @param url
     *        The URL.
     * @see java.net.HttpURLConnection#HttpURLConnection(URL)
     */
    protected HttpURLConnection(final URL url) {
        super(url);
        throw new RuntimeException("An HTTP URL connection can only be " + "constructed from a HttpMethod class");
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Gets an input stream for the HttpMethod response body.
     *
     * @throws IOException
     *         If an IO problem occurs.
     * @return The input stream.
     * @see java.net.HttpURLConnection#getInputStream()
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getResponseBodyAsStream()
     */
    @Override
    public InputStream getInputStream() throws IOException {
        LOG.trace("enter HttpURLConnection.getInputStream()");
        return method.getResponseBodyAsStream();
    }

    /**
     * Not yet implemented. Return the error stream.
     *
     * @see java.net.HttpURLConnection#getErrorStream()
     */
    @Override
    public InputStream getErrorStream() {
        LOG.trace("enter HttpURLConnection.getErrorStream()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#disconnect()
     */
    @Override
    public void disconnect() {
        LOG.trace("enter HttpURLConnection.disconnect()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @throws IOException
     *         If an IO problem occurs.
     * @see java.net.HttpURLConnection#connect()
     */
    @Override
    public void connect() throws IOException {
        LOG.trace("enter HttpURLConnection.connect()");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @return true if we are using a proxy.
     * @see java.net.HttpURLConnection#usingProxy()
     */
    @Override
    public boolean usingProxy() {
        LOG.trace("enter HttpURLConnection.usingProxy()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Return the request method.
     *
     * @return The request method.
     * @see java.net.HttpURLConnection#getRequestMethod()
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getName()
     */
    @Override
    public String getRequestMethod() {
        LOG.trace("enter HttpURLConnection.getRequestMethod()");
        return method.getName();
    }

    /**
     * Return the response code.
     *
     * @return The response code.
     * @throws IOException
     *         If an IO problem occurs.
     * @see java.net.HttpURLConnection#getResponseCode()
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getStatusCode()
     */
    @Override
    public int getResponseCode() throws IOException {
        LOG.trace("enter HttpURLConnection.getResponseCode()");
        return method.getStatusCode();
    }

    /**
     * Return the response message
     *
     * @return The response message
     * @throws IOException
     *         If an IO problem occurs.
     * @see java.net.HttpURLConnection#getResponseMessage()
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getStatusText()
     */
    @Override
    public String getResponseMessage() throws IOException {
        LOG.trace("enter HttpURLConnection.getResponseMessage()");
        return method.getStatusText();
    }

    /**
     * Return the header field
     *
     * @param name
     *        the name of the header
     * @return the header field.
     * @see java.net.HttpURLConnection#getHeaderField(String)
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getResponseHeaders()
     */
    @Override
    public String getHeaderField(final String name) {
        LOG.trace("enter HttpURLConnection.getHeaderField(String)");
        // Note: Return the last matching header in the Header[] array, as in
        // the JDK implementation.
        final Header[] headers = method.getResponseHeaders();
        for (int i = headers.length - 1; i >= 0; i--) {
            if (headers[i].getName().equalsIgnoreCase(name)) {
                return headers[i].getValue();
            }
        }

        return null;
    }

    /**
     * Return the header field key
     *
     * @param keyPosition
     *        The key position
     * @return The header field key.
     * @see java.net.HttpURLConnection#getHeaderFieldKey(int)
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getResponseHeaders()
     */
    @Override
    public String getHeaderFieldKey(final int keyPosition) {
        LOG.trace("enter HttpURLConnection.getHeaderFieldKey(int)");

        // Note: HttpClient does not consider the returned Status Line as
        // a response header. However, getHeaderFieldKey(0) is supposed to
        // return null. Hence the special case below ...

        if (keyPosition == 0) {
            return null;
        }

        // Note: HttpClient does not currently keep headers in the same order
        // that they are read from the HTTP server.

        final Header[] headers = method.getResponseHeaders();
        if (keyPosition < 0 || keyPosition > headers.length) {
            return null;
        }

        return headers[keyPosition - 1].getName();
    }

    /**
     * Return the header field at the specified position
     *
     * @param position
     *        The position
     * @return The header field.
     * @see java.net.HttpURLConnection#getHeaderField(int)
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#getResponseHeaders()
     */
    @Override
    public String getHeaderField(final int position) {
        LOG.trace("enter HttpURLConnection.getHeaderField(int)");

        // Note: HttpClient does not consider the returned Status Line as
        // a response header. However, getHeaderField(0) is supposed to
        // return the status line. Hence the special case below ...

        if (position == 0) {
            return method.getStatusLine().toString();
        }

        // Note: HttpClient does not currently keep headers in the same order
        // that they are read from the HTTP server.

        final Header[] headers = method.getResponseHeaders();
        if (position < 0 || position > headers.length) {
            return null;
        }

        return headers[position - 1].getValue();
    }

    /**
     * Return the URL
     *
     * @return The URL.
     * @see java.net.HttpURLConnection#getURL()
     */
    @Override
    public URL getURL() {
        LOG.trace("enter HttpURLConnection.getURL()");
        return url;
    }

    // Note: We don't implement the following methods so that they default to
    // the JDK implementation. They will all call
    // <code>getHeaderField(String)</code> which we have overridden.

    // java.net.HttpURLConnection#getHeaderFieldDate(String, long)
    // java.net.HttpURLConnection#getContentLength()
    // java.net.HttpURLConnection#getContentType()
    // java.net.HttpURLConnection#getContentEncoding()
    // java.net.HttpURLConnection#getDate()
    // java.net.HttpURLConnection#getHeaderFieldInt(String, int)
    // java.net.HttpURLConnection#getExpiration()
    // java.net.HttpURLConnection#getLastModified()

    /**
     * Not available: the data must have already been retrieved.
     */
    @Override
    public void setInstanceFollowRedirects(final boolean isFollowingRedirects) {
        LOG.trace("enter HttpURLConnection.setInstanceFollowRedirects(boolean)");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     */
    @Override
    public boolean getInstanceFollowRedirects() {
        LOG.trace("enter HttpURLConnection.getInstanceFollowRedirects()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setRequestMethod(String)
     */
    @Override
    public void setRequestMethod(final String method) throws ProtocolException {
        LOG.trace("enter HttpURLConnection.setRequestMethod(String)");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getPermission()
     */
    @Override
    public Permission getPermission() throws IOException {
        LOG.trace("enter HttpURLConnection.getPermission()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getContent()
     */
    @Override
    public Object getContent() throws IOException {
        LOG.trace("enter HttpURLConnection.getContent()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not yet implemented.
     */
    @Override
    public Object getContent(final Class[] classes) throws IOException {
        LOG.trace("enter HttpURLConnection.getContent(Class[])");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * @see java.net.HttpURLConnection#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        LOG.trace("enter HttpURLConnection.getOutputStream()");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setDoInput(boolean)
     */
    @Override
    public void setDoInput(final boolean isInput) {
        LOG.trace("enter HttpURLConnection.setDoInput()");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getDoInput()
     */
    @Override
    public boolean getDoInput() {
        LOG.trace("enter HttpURLConnection.getDoInput()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setDoOutput(boolean)
     */
    @Override
    public void setDoOutput(final boolean isOutput) {
        LOG.trace("enter HttpURLConnection.setDoOutput()");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getDoOutput()
     */
    @Override
    public boolean getDoOutput() {
        LOG.trace("enter HttpURLConnection.getDoOutput()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setAllowUserInteraction(boolean)
     */
    @Override
    public void setAllowUserInteraction(final boolean isAllowInteraction) {
        LOG.trace("enter HttpURLConnection.setAllowUserInteraction(boolean)");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getAllowUserInteraction()
     */
    @Override
    public boolean getAllowUserInteraction() {
        LOG.trace("enter HttpURLConnection.getAllowUserInteraction()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setUseCaches(boolean)
     */
    @Override
    public void setUseCaches(final boolean isUsingCaches) {
        LOG.trace("enter HttpURLConnection.setUseCaches(boolean)");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getUseCaches()
     */
    @Override
    public boolean getUseCaches() {
        LOG.trace("enter HttpURLConnection.getUseCaches()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setIfModifiedSince(long)
     */
    @Override
    public void setIfModifiedSince(final long modificationDate) {
        LOG.trace("enter HttpURLConnection.setIfModifiedSince(long)");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getIfModifiedSince()
     */
    @Override
    public long getIfModifiedSince() {
        LOG.trace("enter HttpURLConnection.getIfmodifiedSince()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#getDefaultUseCaches()
     */
    @Override
    public boolean getDefaultUseCaches() {
        LOG.trace("enter HttpURLConnection.getDefaultUseCaches()");
        throw new RuntimeException("Not implemented yet");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setDefaultUseCaches(boolean)
     */
    @Override
    public void setDefaultUseCaches(final boolean isUsingCaches) {
        LOG.trace("enter HttpURLConnection.setDefaultUseCaches(boolean)");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not available: the data must have already been retrieved.
     *
     * @see java.net.HttpURLConnection#setRequestProperty(String,String)
     */
    @Override
    public void setRequestProperty(final String key, final String value) {
        LOG.trace("enter HttpURLConnection.setRequestProperty()");
        throw new RuntimeException("This class can only be used with already" + "retrieved data");
    }

    /**
     * Not yet implemented.
     *
     * @see java.net.HttpURLConnection#getRequestProperty(String)
     */
    @Override
    public String getRequestProperty(final String key) {
        LOG.trace("enter HttpURLConnection.getRequestProperty()");
        throw new RuntimeException("Not implemented yet");
    }

}
