// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;

/**
 * Will hold an array of bytes that can be read to at the caller's leisure, and
 * when the requisite number of bytes have been written, it can process these
 * bytes.
 *
 * This is useful, for example, for encoding objects to an OutputStream, or
 * otherwise when data appears piecewise.
 *
 * Subclasses must override the start() method to provide a byte array.
 */
public abstract class ByteArrayChunkedEncoder implements ChunkedEncoder {
    /* Temporary deserialization buffer */
    private byte[] buffer;
    private int bufferLen = 0;

    private boolean complete = false;

    protected ByteArrayChunkedEncoder() {
    }

    /**
     * Gets the buffer to be written.
     *
     * @return
     */
    protected abstract byte[] start() throws IOException;

    /**
     * Writes the specified number of len bytes into buf, starting at offset off
     * and encodes them. len may be larger (or smaller) than the expected buffer
     * length, callers should call isComplete() to know if their encoding has
     * finished.
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
    public final int encode(final byte[] buf, final int off, final int len) throws IOException {
        if (buffer == null) {
            buffer = start();

            if (buffer == null) {
                throw new IOException("Could not get byte buffer"); //$NON-NLS-1$
            }
        }

        final int writeLen = Math.min((buffer.length - bufferLen), len);

        System.arraycopy(buffer, bufferLen, buf, off, writeLen);
        bufferLen += writeLen;

        if (bufferLen == buffer.length) {
            complete = true;
        }

        return writeLen;
    }

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedEncoder#isComplete()
     */
    @Override
    public final boolean isComplete() {
        return complete;
    }
}
