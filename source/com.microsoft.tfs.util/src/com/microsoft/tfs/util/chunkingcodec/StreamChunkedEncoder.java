// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Messages;

/**
 * Will hold a reference to an InputStream that can be read at the caller's
 * leisure.
 *
 * This is useful, for example, for encoding objects to an OutputStream, or
 * otherwise when writing is available piecewise.
 *
 * Subclasses may override the finish() method to do some work when the entire
 * buffer has been written.
 */
public class StreamChunkedEncoder implements ChunkedEncoder {
    private InputStream stream;

    private long bufferSize = -1;
    private long bufferLen;

    private boolean complete = false;

    protected StreamChunkedEncoder() {
    }

    protected StreamChunkedEncoder(final InputStream stream) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        this.stream = stream;
    }

    protected StreamChunkedEncoder(final InputStream stream, final long bufferSize) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$
        Check.isTrue(bufferSize >= 0, "bufferSize >= 0"); //$NON-NLS-1$

        this.stream = stream;
        this.bufferSize = bufferSize;
    }

    protected void setBufferSize(final long bufferSize) {
        Check.isTrue(bufferSize >= 0, "bufferSize >= 0"); //$NON-NLS-1$

        this.bufferSize = bufferSize;
    }

    protected void setStream(final InputStream stream) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        this.stream = stream;
    }

    @Override
    public int encode(final byte[] buf, final int off, final int len) throws IOException {

        if (bufferSize == 0) {
            complete = true;
            return 0;
        } else if (stream == null) {
            throw new IOException("Stream not initialized"); //$NON-NLS-1$
        }

        /*
         * If the client hasn't specified a maximum length, write all the stream
         * we can
         */
        final int chunklen = (bufferSize >= 0) ? (int) Math.min((bufferSize - bufferLen), len) : len;

        /* We don't have a specified length, write all the stream we can */
        final int readlen = stream.read(buf, off, chunklen);

        /* EOF when we didn't know the size (not an error) */
        if (readlen < 0 && bufferSize < 0) {
            complete = true;
            stream.close();

            return 0;
        }

        /* EOF when we did know the size (ie, short read, error) */
        else if (readlen < 0) {
            final String messageFormat = Messages.getString("StreamChunkedEncoder.BufferUnderrunFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, bufferSize, bufferLen);
            throw new IOException(message);
        }

        bufferLen += readlen;

        if (bufferSize >= 0 && bufferSize == bufferLen) {
            complete = true;
            stream.close();
        }

        return readlen;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    public void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
    }
}
