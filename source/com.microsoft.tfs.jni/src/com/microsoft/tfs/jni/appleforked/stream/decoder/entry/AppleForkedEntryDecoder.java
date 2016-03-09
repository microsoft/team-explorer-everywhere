// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder.entry;

import java.io.IOException;

import com.microsoft.tfs.util.chunkingcodec.ChunkedDecoder;

/**
 * A simple interface for AppleForkedDecoders, adding a close() method to allow
 * decoders to do work at the end.
 */
public interface AppleForkedEntryDecoder extends ChunkedDecoder {
    /**
     * Closes the entry decoder. This allows any last-minute work to be done.
     *
     * @throws IOException
     */
    public void close() throws IOException;
}
