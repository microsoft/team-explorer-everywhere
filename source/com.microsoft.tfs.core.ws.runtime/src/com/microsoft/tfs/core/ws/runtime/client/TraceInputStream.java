// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.client;

import java.io.IOException;
import java.io.InputStream;

/**
 * A single threaded input stream that wraps another input stream but captures
 * information suitable for tracing
 */
public class TraceInputStream extends InputStream {
    private final InputStream delegate;
    private long totalBytes;
    private int bytesRead;
    private final int storeBytes;
    private byte[] byteStore;
    private int currentPosition;
    private final boolean compressed;

    private static final int BUFFER_INITIAL_SIZE = 8192;

    public TraceInputStream(final InputStream delegate, final int storeBytes, final boolean isCompressed) {
        this.delegate = delegate;
        this.storeBytes = storeBytes;
        compressed = isCompressed;
        byteStore = new byte[BUFFER_INITIAL_SIZE];
    }

    @Override
    public int read() throws IOException {
        bytesRead = delegate.read();
        ++totalBytes;
        if (totalBytes < storeBytes) {
            addBytes(new byte[] {
                (byte) bytesRead
            }, 1);
        }
        return bytesRead;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        bytesRead = delegate.read(b, off, len);
        totalBytes += bytesRead;
        addBytes(b, bytesRead);
        return bytesRead;
    }

    @Override
    public int read(final byte[] b) throws IOException {
        bytesRead = delegate.read(b);
        totalBytes += bytesRead;
        addBytes(b, bytesRead);
        return bytesRead;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public byte[] getBytes() {
        if (storeBytes <= 0 || currentPosition == 0) {
            return null;
        }

        final byte[] copy = new byte[currentPosition];
        System.arraycopy(byteStore, 0, copy, 0, currentPosition);
        return copy;
    }

    private void addBytes(final byte[] bytes, final int bytesRead) {
        if (totalBytes < storeBytes) {
            if (currentPosition + bytesRead > byteStore.length) {
                // We need more room, double our buffer.
                final int increase = Math.max(byteStore.length * 2, bytesRead);
                final byte[] temp = new byte[byteStore.length + increase];
                System.arraycopy(byteStore, 0, temp, 0, currentPosition);
                byteStore = temp;
            }
            System.arraycopy(bytes, 0, byteStore, currentPosition, bytesRead);
            currentPosition = currentPosition + bytesRead;
        }
    }

    /**
     * @return true is stream based on a compressed (gzip) stream.
     */
    public boolean isCompressed() {
        return compressed;
    }

}
