/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/ByteArrayRequestEntity.java,v
 * 1.3 2004/05/13 02:26:08 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
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
 *
 * [Additional notices, if required by prior licensing conditions]
 */
package com.microsoft.tfs.core.httpclient.methods;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A RequestEntity that contains an array of bytes.
 *
 * @since 3.0
 */
public class ByteArrayRequestEntity implements RequestEntity {

    /** The content */
    private final byte[] content;

    /** The content type */
    private final String contentType;

    /**
     * Creates a new entity with the given content.
     *
     * @param content
     *        The content to set.
     */
    public ByteArrayRequestEntity(final byte[] content) {
        this(content, null);
    }

    /**
     * Creates a new entity with the given content and content type.
     *
     * @param content
     *        The content to set.
     * @param contentType
     *        The content type to set or <code>null</code>.
     */
    public ByteArrayRequestEntity(final byte[] content, final String contentType) {
        super();
        if (content == null) {
            throw new IllegalArgumentException("The content cannot be null");
        }
        this.content = content;
        this.contentType = contentType;
    }

    /**
     * @return <code>true</code>
     */
    @Override
    public boolean isRepeatable() {
        return true;
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

    /*
     * (non-Javadoc)
     *
     * @see org.apache.commons.httpclient.RequestEntity#writeRequest(java.io.
     * OutputStream )
     */
    @Override
    public void writeRequest(final OutputStream out) throws IOException {
        out.write(content);
    }

    /**
     * @return The length of the content.
     */
    @Override
    public long getContentLength() {
        return content.length;
    }

    /**
     * @return Returns the content.
     */
    public byte[] getContent() {
        return content;
    }

}
