// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;

/**
 * Provides an interface for object decoding "in chunks". That is, decoded data
 * can be written at the caller's leisure. This avoids having to read an entire
 * object in order to decode it, and is useful for decoding data from a network
 * or off an InputStream without using significant memory.
 *
 * It is used significantly by AppleForkedDecoder, which decodes AppleSingle /
 * AppleDouble data on the fly.
 */
public interface ChunkedDecoder {
    /**
     * Decodes the bytes in buffer, beginning at offset off for length len.
     * Callers should check to see if decoding has completed by calling
     * isComplete.
     *
     * Len may be smaller than the number of bytes required for decoding, in
     * this case, isComplete should not return true. Len may be greater than the
     * number of bytes required for decoding, in this case, <code>decode</code>
     * should return only the number of bytes that were required.
     *
     * @param buf
     *        The bytes to decode
     * @param off
     *        The offset to begin reading at
     * @param len
     *        The length of bytes to decode
     * @return The number of bytes consumed
     * @throws IOException
     */
    public int decode(byte[] buf, int off, int len) throws IOException;

    /**
     * Queries if enough data has been read that decoding has finished.
     *
     * @return true if decoding has completed, false otherwise
     */
    public boolean isComplete();
}
