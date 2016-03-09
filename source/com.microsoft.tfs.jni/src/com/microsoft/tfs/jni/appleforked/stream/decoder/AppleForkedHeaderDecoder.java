// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedHeader;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedDecoder;

/**
 * A stream decoder for AppleSingle/AppleDouble headers. This is useful for
 * handling decoding by-part - that is, when the AppleSingle data are coming in
 * pieces, from an InputStream, for example.
 */
public class AppleForkedHeaderDecoder extends ByteArrayChunkedDecoder {
    private AppleForkedHeader header = null;

    public AppleForkedHeaderDecoder() {
        super(AppleForkedHeader.HEADER_SIZE);
    }

    @Override
    protected void finish(final byte[] buffer) {
        header = new AppleForkedHeader(buffer);
    }

    public AppleForkedHeader getHeader() {
        return header;
    }
}
