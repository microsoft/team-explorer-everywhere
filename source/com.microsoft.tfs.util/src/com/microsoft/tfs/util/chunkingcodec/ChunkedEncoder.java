// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;

/**
 * Provides an interface for object encoding "in chunks". That is, encoded data
 * can be read at the caller's leisure. This avoids having to write an entire
 * object in order to encode it, and is useful for serializing data from a
 * network or off an InputStream without using significant memory.
 */
public interface ChunkedEncoder {
    /**
     * Encodes the bytes in buffer, beginning at offset off for length len.
     * Callers should check to see if encoding has completed by calling
     * isComplete.
     *
     * Len may be smaller than the number of bytes required for encoding, in
     * this case, isComplete should not return true. Len may be greater than the
     * number of bytes required for encoding, in this case, <code>encode</code>
     * should return only the number of bytes that were required.
     *
     * @param buf
     *        The bytes to encode data into
     * @param off
     *        The offset to begin writing at
     * @param len
     *        The length of bytes to encode
     * @return The number of bytes written
     * @throws IOException
     */
    public int encode(byte[] buf, int off, int len) throws IOException;

    /**
     * Queries if enough data has been written that encoding has finished.
     *
     * @return true if encoding has completed, false otherwise
     */
    public boolean isComplete();
}
