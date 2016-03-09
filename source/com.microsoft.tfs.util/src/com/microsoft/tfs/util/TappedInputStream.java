// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * An {@link InputStream} where the read methods send a copy of the data to a
 * {@link ReadHandler}. Good for chaining underneath another
 * {@link FilterInputStream} like {@link GZIPInputStream} to grab a copy of the
 * compressed (gzipped) bytes before they are decoded.
 *
 * @threadsafety thread-safe
 */
public class TappedInputStream extends FilterInputStream {
    private final ReadHandler handler;

    /**
     * Constructs a {@link TappedInputStream} that reads from the given stream
     * and sends bytes to the given {@link ReadHandler}.
     *
     * @param stream
     *        the stream to read from (must not be <code>null</code>)
     * @param handler
     *        the handler to send copies of bytes to (must not be
     *        <code>null</code> )
     */
    public TappedInputStream(final InputStream stream, final ReadHandler handler) {
        super(stream);

        Check.notNull(handler, "handler"); //$NON-NLS-1$

        this.handler = handler;
    }

    @Override
    public int read() throws IOException {
        final int val = super.read();

        if (val != -1) {
            // Checked for -1; safe to cast
            handler.handleRead((byte) val);
        }

        return val;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        final int readCount = super.read(b);

        if (readCount != -1) {
            handler.handleRead(b, readCount);
        }

        return readCount;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int readCount = super.read(b, off, len);

        if (readCount != -1) {
            handler.handleRead(b, off, len, readCount);
        }

        return readCount;
    }

    /**
     * Handles data read by {@link TappedInputStream}.
     *
     * @threadsafety thread-compatible
     */
    public interface ReadHandler {
        /**
         * Called when one byte was read from the stream.
         *
         * @param the
         *        byte read
         */
        public void handleRead(byte b) throws IOException;

        /**
         * Called when multiple bytes were read from the stream.
         * <p>
         * An implementation must make a copy of the array or its contents to
         * use it after it returns.
         *
         * @param b
         *        the array the bytes were read into (array only valid during
         *        the execution of the method)
         * @param readCount
         *        the count of bytes actually read from the stream and written
         *        into b
         */
        public void handleRead(byte[] b, int readCount) throws IOException;

        /**
         * Called when multiple bytes were read from the stream.
         * <p>
         * An implementation must make a copy of the array or its contents to
         * use it after it returns.
         *
         * @param b
         *        the array the bytes were read into (array only valid during
         *        the execution of the method)
         * @param off
         *        offset into b the bytes were read into
         * @param len
         *        the maximum number of bytes that could be read into b
         * @param readCount
         *        the count of bytes actually read from the stream and written
         *        into b
         */
        public void handleRead(byte[] b, int off, int len, int readCount) throws IOException;
    }
}
