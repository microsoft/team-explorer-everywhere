/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/ChunkedOutputStream.java,v 1.16
 * 2004/05/13 04:03:25 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
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

package com.microsoft.tfs.core.httpclient;

import java.io.IOException;
import java.io.OutputStream;

import com.microsoft.tfs.core.httpclient.util.EncodingUtil;

/**
 * Implements HTTP chunking support. Writes are buffered to an internal buffer
 * (2048 default size). Chunks are guaranteed to be at least as large as the
 * buffer size (except for the last chunk).
 *
 * @author Mohammad Rezaei, Goldman, Sachs & Co.
 */
public class ChunkedOutputStream extends OutputStream {

    // ------------------------------------------------------- Static Variables
    private static final byte CRLF[] = new byte[] {
        (byte) 13,
        (byte) 10
    };

    /** End chunk */
    private static final byte ENDCHUNK[] = CRLF;

    /** 0 */
    private static final byte ZERO[] = new byte[] {
        (byte) '0'
    };

    // ----------------------------------------------------- Instance Variables
    private OutputStream stream = null;

    private final byte[] cache;

    private int cachePosition = 0;

    private boolean wroteLastChunk = false;

    // ----------------------------------------------------------- Constructors
    /**
     * Wraps a stream and chunks the output.
     *
     * @param stream
     *        to wrap
     * @param bufferSize
     *        minimum chunk size (excluding last chunk)
     * @throws IOException
     *
     * @since 3.0
     */
    public ChunkedOutputStream(final OutputStream stream, final int bufferSize) throws IOException {
        cache = new byte[bufferSize];
        this.stream = stream;
    }

    /**
     * Wraps a stream and chunks the output. The default buffer size of 2048 was
     * chosen because the chunk overhead is less than 0.5%
     *
     * @param stream
     * @throws IOException
     */
    public ChunkedOutputStream(final OutputStream stream) throws IOException {
        this(stream, 2048);
    }

    // ----------------------------------------------------------- Internal
    // methods
    /**
     * Writes the cache out onto the underlying stream
     *
     * @throws IOException
     *
     * @since 3.0
     */
    protected void flushCache() throws IOException {
        if (cachePosition > 0) {
            final byte chunkHeader[] = EncodingUtil.getAsciiBytes(Integer.toHexString(cachePosition) + "\r\n");
            stream.write(chunkHeader, 0, chunkHeader.length);
            stream.write(cache, 0, cachePosition);
            stream.write(ENDCHUNK, 0, ENDCHUNK.length);
            cachePosition = 0;
        }
    }

    /**
     * Writes the cache and bufferToAppend to the underlying stream as one large
     * chunk
     *
     * @param bufferToAppend
     * @param off
     * @param len
     * @throws IOException
     *
     * @since 3.0
     */
    protected void flushCacheWithAppend(final byte bufferToAppend[], final int off, final int len) throws IOException {
        final byte chunkHeader[] = EncodingUtil.getAsciiBytes(Integer.toHexString(cachePosition + len) + "\r\n");
        stream.write(chunkHeader, 0, chunkHeader.length);
        stream.write(cache, 0, cachePosition);
        stream.write(bufferToAppend, off, len);
        stream.write(ENDCHUNK, 0, ENDCHUNK.length);
        cachePosition = 0;
    }

    protected void writeClosingChunk() throws IOException {
        // Write the final chunk.

        stream.write(ZERO, 0, ZERO.length);
        stream.write(CRLF, 0, CRLF.length);
        stream.write(ENDCHUNK, 0, ENDCHUNK.length);
    }

    // ----------------------------------------------------------- Public
    // Methods
    /**
     * Must be called to ensure the internal cache is flushed and the closing
     * chunk is written.
     *
     * @throws IOException
     *
     * @since 3.0
     */
    public void finish() throws IOException {
        if (!wroteLastChunk) {
            flushCache();
            writeClosingChunk();
            wroteLastChunk = true;
        }
    }

    // -------------------------------------------- OutputStream Methods
    /**
     * Write the specified byte to our output stream.
     *
     * Note: Avoid this method as it will cause an inefficient single byte
     * chunk. Use write (byte[], int, int) instead.
     *
     * @param b
     *        The byte to be written
     * @throws IOException
     *         if an input/output error occurs
     */
    @Override
    public void write(final int b) throws IOException {
        cache[cachePosition] = (byte) b;
        cachePosition++;
        if (cachePosition == cache.length) {
            flushCache();
        }
    }

    /**
     * Writes the array. If the array does not fit within the buffer, it is not
     * split, but rather written out as one large chunk.
     *
     * @param b
     * @throws IOException
     *
     * @since 3.0
     */
    @Override
    public void write(final byte b[]) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(final byte src[], final int off, final int len) throws IOException {
        if (len >= cache.length - cachePosition) {
            flushCacheWithAppend(src, off, len);
        } else {
            System.arraycopy(src, off, cache, cachePosition, len);
            cachePosition += len;
        }
    }

    /**
     * Flushes the underlying stream, but leaves the internal buffer alone.
     *
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    /**
     * Finishes writing to the underlying stream, but does NOT close the
     * underlying stream.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        finish();
        super.close();
    }
}
