// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder;

import java.io.IOException;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedHeader;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes an AppleSingle / AppleDouble header into its byte array
 * representation.
 */
public class AppleForkedHeaderEncoder extends ByteArrayChunkedEncoder {
    private final AppleForkedHeader header;

    public AppleForkedHeaderEncoder(final AppleForkedHeader header) {
        Check.notNull(header, "header"); //$NON-NLS-1$

        this.header = header;
    }

    @Override
    protected byte[] start() throws IOException {
        return header.encode();
    }
}
