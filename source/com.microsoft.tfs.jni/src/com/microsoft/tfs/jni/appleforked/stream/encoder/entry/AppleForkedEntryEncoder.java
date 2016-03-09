// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.IOException;

import com.microsoft.tfs.util.chunkingcodec.ChunkedEncoder;

/**
 * Interface for encoding the entries in AppleSingle and AppleDouble files. Adds
 * a getType() method and getLength() method.
 */
public interface AppleForkedEntryEncoder extends ChunkedEncoder {
    /**
     * Returns the type of the AppleSingle/AppleDouble entry. See
     * AppleForkedConstants for details.
     *
     * @return The type of the entry for the entry descriptor
     */
    public long getType();

    /**
     * Returns the length of the entry, in bytes.
     *
     * @return The length of the entry
     */
    public long getLength();

    /**
     * Closes the entry encoder.
     *
     * @throws IOException
     */
    public void close() throws IOException;
}
