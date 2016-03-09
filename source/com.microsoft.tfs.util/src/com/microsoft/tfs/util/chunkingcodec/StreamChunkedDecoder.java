// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;
import java.io.OutputStream;

import com.microsoft.tfs.util.Check;

/**
 * Will hold a reference to an OutputStream that can be written to at the
 * caller's leisure.
 *
 * This is useful, for example, for decoding objects from an InputStream, or
 * otherwise when data appears piecewise.
 *
 * Subclasses may override the finish() method to do some work when the entire
 * buffer has been received.
 */
public class StreamChunkedDecoder implements ChunkedDecoder {
    private OutputStream stream;

    private long bufferSize = 0;
    private long bufferLen = 0;

    private boolean complete = false;

    protected StreamChunkedDecoder() {
    }

    protected StreamChunkedDecoder(final OutputStream stream, final long bufferSize) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$
        Check.isTrue(bufferSize >= 0, "bufferSize >= 0"); //$NON-NLS-1$

        this.stream = stream;
        this.bufferSize = bufferSize;
    }

    protected void setBufferSize(final long bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected void setStream(final OutputStream stream) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        this.stream = stream;
    }

    /**
     * Writes the specified number of len bytes from buf, starting at offset off
     * and decodes them. len may be larger (or smaller) than the expected buffer
     * length, callers should call isComplete() to know if their decoding has
     * finished.
     *
     * @param buf
     *        The buffer to write
     * @param off
     *        The beginning of the buffer to write
     * @param len
     *        The length to write
     * @return The number of bytes actually processed
     * @throws IOException
     */
    @Override
    public final int decode(final byte[] buf, final int off, final int len) throws IOException {
        if (stream == null) {
            throw new IOException("Decoder not initialized"); //$NON-NLS-1$
        }

        final int writeLen = Math.min((int) (bufferSize - bufferLen), len);

        stream.write(buf, off, writeLen);
        bufferLen += writeLen;

        if (bufferLen == bufferSize) {
            complete = true;
            finish();

            stream.close();
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
     * Subclasses may override to perform a function when the entire stream has
     * been written.
     */
    protected void finish() {
    }

    public void close() throws IOException {
        finish();

        if (stream != null) {
            stream.close();
        }
    }
}
