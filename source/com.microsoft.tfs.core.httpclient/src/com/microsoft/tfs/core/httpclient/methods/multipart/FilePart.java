/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/methods/multipart/FilePart.java,v
 * 1.19 2004/04/18 23:51:37 jsdever Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient.methods.multipart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.httpclient.util.EncodingUtil;

/**
 * This class implements a part of a Multipart post object that consists of a
 * file.
 *
 * @author <a href="mailto:mattalbright@yahoo.com">Matthew Albright</a>
 * @author <a href="mailto:jsdever@apache.org">Jeff Dever</a>
 * @author <a href="mailto:adrian@ephox.com">Adrian Sutton</a>
 * @author <a href="mailto:becke@u.washington.edu">Michael Becke</a>
 * @author <a href="mailto:mdiggory@latte.harvard.edu">Mark Diggory</a>
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 2.0
 *
 */
public class FilePart extends PartBase {

    /** Default content encoding of file attachments. */
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /** Default charset of file attachments. */
    public static final String DEFAULT_CHARSET = "ISO-8859-1";

    /** Default transfer encoding of file attachments. */
    public static final String DEFAULT_TRANSFER_ENCODING = "binary";

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(FilePart.class);

    /** Attachment's file name */
    protected static final String FILE_NAME = "; filename=";

    /** Attachment's file name as a byte array */
    private static final byte[] FILE_NAME_BYTES = EncodingUtil.getAsciiBytes(FILE_NAME);

    /** Source of the file part. */
    private final PartSource source;

    /**
     * FilePart Constructor.
     *
     * @param name
     *        the name for this part
     * @param partSource
     *        the source for this part
     * @param contentType
     *        the content type for this part, if <code>null</code> the
     *        {@link #DEFAULT_CONTENT_TYPE default} is used
     * @param charset
     *        the charset encoding for this part, if <code>null</code> the
     *        {@link #DEFAULT_CHARSET default} is used
     */
    public FilePart(final String name, final PartSource partSource, final String contentType, final String charset) {

        super(
            name,
            contentType == null ? DEFAULT_CONTENT_TYPE : contentType,
            charset == null ? "ISO-8859-1" : charset,
            DEFAULT_TRANSFER_ENCODING);

        if (partSource == null) {
            throw new IllegalArgumentException("Source may not be null");
        }
        source = partSource;
    }

    /**
     * FilePart Constructor.
     *
     * @param name
     *        the name for this part
     * @param partSource
     *        the source for this part
     */
    public FilePart(final String name, final PartSource partSource) {
        this(name, partSource, null, null);
    }

    /**
     * FilePart Constructor.
     *
     * @param name
     *        the name of the file part
     * @param file
     *        the file to post
     *
     * @throws FileNotFoundException
     *         if the <i>file</i> is not a normal file or if it is not readable.
     */
    public FilePart(final String name, final File file) throws FileNotFoundException {
        this(name, new FilePartSource(file), null, null);
    }

    /**
     * FilePart Constructor.
     *
     * @param name
     *        the name of the file part
     * @param file
     *        the file to post
     * @param contentType
     *        the content type for this part, if <code>null</code> the
     *        {@link #DEFAULT_CONTENT_TYPE default} is used
     * @param charset
     *        the charset encoding for this part, if <code>null</code> the
     *        {@link #DEFAULT_CHARSET default} is used
     *
     * @throws FileNotFoundException
     *         if the <i>file</i> is not a normal file or if it is not readable.
     */
    public FilePart(final String name, final File file, final String contentType, final String charset)
        throws FileNotFoundException {
        this(name, new FilePartSource(file), contentType, charset);
    }

    /**
     * FilePart Constructor.
     *
     * @param name
     *        the name of the file part
     * @param fileName
     *        the file name
     * @param file
     *        the file to post
     *
     * @throws FileNotFoundException
     *         if the <i>file</i> is not a normal file or if it is not readable.
     */
    public FilePart(final String name, final String fileName, final File file) throws FileNotFoundException {
        this(name, new FilePartSource(fileName, file), null, null);
    }

    /**
     * FilePart Constructor.
     *
     * @param name
     *        the name of the file part
     * @param fileName
     *        the file name
     * @param file
     *        the file to post
     * @param contentType
     *        the content type for this part, if <code>null</code> the
     *        {@link #DEFAULT_CONTENT_TYPE default} is used
     * @param charset
     *        the charset encoding for this part, if <code>null</code> the
     *        {@link #DEFAULT_CHARSET default} is used
     *
     * @throws FileNotFoundException
     *         if the <i>file</i> is not a normal file or if it is not readable.
     */
    public FilePart(
        final String name,
        final String fileName,
        final File file,
        final String contentType,
        final String charset) throws FileNotFoundException {
        this(name, new FilePartSource(fileName, file), contentType, charset);
    }

    /**
     * Write the disposition header to the output stream
     *
     * @param out
     *        The output stream
     * @throws IOException
     *         If an IO problem occurs
     * @see Part#sendDispositionHeader(OutputStream)
     */
    @Override
    protected void sendDispositionHeader(final OutputStream out) throws IOException {
        LOG.trace("enter sendDispositionHeader(OutputStream out)");
        super.sendDispositionHeader(out);
        final String filename = source.getFileName();
        if (filename != null) {
            out.write(FILE_NAME_BYTES);
            out.write(QUOTE_BYTES);
            out.write(EncodingUtil.getAsciiBytes(filename));
            out.write(QUOTE_BYTES);
        }
    }

    /**
     * Write the data in "source" to the specified stream.
     *
     * @param out
     *        The output stream.
     * @throws IOException
     *         if an IO problem occurs.
     * @see com.microsoft.tfs.core.httpclient.methods.multipart.Part#sendData(OutputStream)
     */
    @Override
    protected void sendData(final OutputStream out) throws IOException {
        LOG.trace("enter sendData(OutputStream out)");
        if (lengthOfData() == 0) {

            // this file contains no data, so there is nothing to send.
            // we don't want to create a zero length buffer as this will
            // cause an infinite loop when reading.
            LOG.debug("No data to send.");
            return;
        }

        final byte[] tmp = new byte[4096];
        final InputStream instream = source.createInputStream();
        try {
            int len;
            while ((len = instream.read(tmp)) >= 0) {
                out.write(tmp, 0, len);
            }
        } finally {
            // we're done with the stream, close it
            instream.close();
        }
    }

    /**
     * Returns the source of the file part.
     *
     * @return The source.
     */
    protected PartSource getSource() {
        LOG.trace("enter getSource()");
        return source;
    }

    /**
     * Return the length of the data.
     *
     * @return The length.
     * @throws IOException
     *         if an IO problem occurs
     * @see com.microsoft.tfs.core.httpclient.methods.multipart.Part#lengthOfData()
     */
    @Override
    protected long lengthOfData() throws IOException {
        LOG.trace("enter lengthOfData()");
        return source.getLength();
    }

}
