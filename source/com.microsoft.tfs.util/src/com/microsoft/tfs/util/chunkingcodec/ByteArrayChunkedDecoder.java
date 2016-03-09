// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;

/**
 * Will hold an array of bytes that can be written to at the caller's leisure,
 * and when the requisite number of bytes have been written, it can process
 * these bytes.
 *
 * This is useful, for example, for decoding objects from an InputStream, or
 * otherwise when data appears piecewise.
 *
 * Subclasses should override the finish() method to do some work when the
 * entire buffer has been received.
 */
public abstract class ByteArrayChunkedDecoder implements ChunkedDecoder {
    /* Temporary deserialization buffer */
    private final byte[] buffer;
    private int bufferLen = 0;

    private boolean complete = false;

    protected ByteArrayChunkedDecoder(final int bufferSize) {
        buffer = new byte[bufferSize];
    }

    /**
     * Writes the specified number of len bytes from buf, starting at offset off
     * and decodes them. len may be larger (or smaller) than the expected buffer
     * length, callers should call isComplete() to know if their decoding has
     * finished.
     *
     *
     * @param buf
     *        The buffer to write
     * @param off
     *        The beginning of the buffer to write
     * @param len
     *        The length to write
     * @return The number of bytes actually processed
     */
    @Override
    public final int decode(final byte[] buf, final int off, final int len) {
        final int writeLen = Math.min((buffer.length - bufferLen), len);

        System.arraycopy(buf, off, buffer, bufferLen, writeLen);
        bufferLen += writeLen;

        if (bufferLen == buffer.length) {
            complete = true;
            finish(buffer);
        }

        return writeLen;
    }

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedDecoder#isComplete()
     */
    @Override
    public final boolean isComplete() {
        return complete;
    }

    /**
     * Subclasses should override to perform a function when the entire byte
     * array is available.
     *
     * @param buffer
     *        The buffer that was read for deserialization
     */
    protected abstract void finish(byte[] buffer);

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedDecoder#close()
     */
    public void close() throws IOException {
    }
}
