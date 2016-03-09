/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/RequestEntity.java,v 1.4
 * 2004/05/17 21:46:03 olegk Exp $ $Revision: 480424 $ $Date: 2006-11-29
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
import java.io.OutputStream;

/**
 * @since 3.0
 */
public interface RequestEntity {

    /**
     * Tests if {@link #writeRequest(OutputStream)} can be called more than
     * once.
     *
     * @return <tt>true</tt> if the entity can be written to
     *         {@link OutputStream} more than once, <tt>false</tt> otherwise.
     */
    boolean isRepeatable();

    /**
     * Writes the request entity to the given stream.
     *
     * @param out
     * @throws IOException
     */
    void writeRequest(OutputStream out) throws IOException;

    /**
     * Gets the request entity's length. This method should return a
     * non-negative value if the content length is known or a negative value if
     * it is not. In the latter case the
     * {@link com.microsoft.tfs.core.httpclient.methods.EntityEnclosingMethod}
     * will use chunk encoding to transmit the request entity.
     *
     * @return a non-negative value when content length is known or a negative
     *         value when content length is not known
     */
    long getContentLength();

    /**
     * Gets the entity's content type. This content type will be used as the
     * value for the "Content-Type" header.
     *
     * @return the entity's content type
     * @see com.microsoft.tfs.core.httpclient.HttpMethod#setRequestHeader(String,
     *      String)
     */
    String getContentType();

}
