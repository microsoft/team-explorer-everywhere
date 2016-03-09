/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/InputStreamRequestEntity
 * .java,v 1.4 2004/05/17 21:46:03 olegk Exp $ $Revision: 480424 $ $Date:
 * 2006-11-29 06:56:49 +0100 (Wed, 29 Nov 2006) $
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
 *
 * [Additional notices, if required by prior licensing conditions]
 */
package com.microsoft.tfs.core.httpclient.methods;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A RequestEntity that contains an InputStream.
 *
 * @since 3.0
 */
public class InputStreamRequestEntity implements RequestEntity {

    /**
     * The content length will be calculated automatically. This implies
     * buffering of the content.
     */
    public static final int CONTENT_LENGTH_AUTO = -2;

    private static final Log LOG = LogFactory.getLog(InputStreamRequestEntity.class);

    private long contentLength;

    private InputStream content;

    /** The buffered request body, if any. */
    private byte[] buffer = null;

    /** The content type */
    private final String contentType;

    /**
     * Creates a new InputStreamRequestEntity with the given content and a
     * content type of {@link #CONTENT_LENGTH_AUTO}.
     *
     * @param content
     *        The content to set.
     */
    public InputStreamRequestEntity(final InputStream content) {
        this(content, null);
    }

    /**
     * Creates a new InputStreamRequestEntity with the given content, content
     * type, and a content length of {@link #CONTENT_LENGTH_AUTO}.
     *
     * @param content
     *        The content to set.
     * @param contentType
     *        The type of the content, or <code>null</code>.
     */
    public InputStreamRequestEntity(final InputStream content, final String contentType) {
        this(content, CONTENT_LENGTH_AUTO, contentType);
    }

    /**
     * Creates a new InputStreamRequestEntity with the given content and content
     * length.
     *
     * @param content
     *        The content to set.
     * @param contentLength
     *        The content size in bytes or a negative number if not known. If
     *        {@link #CONTENT_LENGTH_AUTO} is given the content will be buffered
     *        in order to determine its size when {@link #getContentLength()} is
     *        called.
     */
    public InputStreamRequestEntity(final InputStream content, final long contentLength) {
        this(content, contentLength, null);
    }

    /**
     * Creates a new InputStreamRequestEntity with the given content, content
     * length, and content type.
     *
     * @param content
     *        The content to set.
     * @param contentLength
     *        The content size in bytes or a negative number if not known. If
     *        {@link #CONTENT_LENGTH_AUTO} is given the content will be buffered
     *        in order to determine its size when {@link #getContentLength()} is
     *        called.
     * @param contentType
     *        The type of the content, or <code>null</code>.
     */
    public InputStreamRequestEntity(final InputStream content, final long contentLength, final String contentType) {
        if (content == null) {
            throw new IllegalArgumentException("The content cannot be null");
        }
        this.content = content;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.methods.RequestEntity#getContentType()
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    /**
     * Buffers request body input stream.
     */
    private void bufferContent() {

        if (buffer != null) {
            // Already been buffered
            return;
        }
        if (content != null) {
            try {
                final ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                final byte[] data = new byte[4096];
                int l = 0;
                while ((l = content.read(data)) >= 0) {
                    tmp.write(data, 0, l);
                }
                buffer = tmp.toByteArray();
                content = null;
                contentLength = buffer.length;
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                buffer = null;
                content = null;
                contentLength = 0;
            }
        }
    }

    /**
     * Tests if this method is repeatable. Only <code>true</code> if the content
     * has been buffered.
     *
     * @see #getContentLength()
     */
    @Override
    public boolean isRepeatable() {
        return buffer != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.RequestEntity#writeRequest(java.io.
     * OutputStream )
     */
    @Override
    public void writeRequest(final OutputStream out) throws IOException {

        if (content != null) {
            final byte[] tmp = new byte[4096];
            int i = 0;
            while ((i = content.read(tmp)) >= 0) {
                out.write(tmp, 0, i);
            }
        } else if (buffer != null) {
            out.write(buffer);
        } else {
            throw new IllegalStateException("Content must be set before entity is written");
        }
    }

    /**
     * Gets the content length. If the content length has not been set, the
     * content will be buffered to determine the actual content length.
     */
    @Override
    public long getContentLength() {
        if (contentLength == CONTENT_LENGTH_AUTO && buffer == null) {
            bufferContent();
        }
        return contentLength;
    }

    /**
     * @return Returns the content.
     */
    public InputStream getContent() {
        return content;
    }

}
