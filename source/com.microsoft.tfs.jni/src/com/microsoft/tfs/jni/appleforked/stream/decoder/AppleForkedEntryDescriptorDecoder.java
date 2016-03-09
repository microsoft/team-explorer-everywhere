// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedDecoder;

/**
 * A stream decoder for AppleSingleEntryDescriptors. This is useful for handling
 * decoding by-part - that is, when the AppleSingle / AppleDouble data is coming
 * in pieces, from an InputStream, for example.
 */
public class AppleForkedEntryDescriptorDecoder extends ByteArrayChunkedDecoder {
    private AppleForkedEntryDescriptor entryDescriptor = null;

    public AppleForkedEntryDescriptorDecoder() {
        super(AppleForkedEntryDescriptor.ENTRY_DESCRIPTOR_SIZE);
    }

    @Override
    protected void finish(final byte[] buffer) {
        entryDescriptor = new AppleForkedEntryDescriptor(buffer);
    }

    public AppleForkedEntryDescriptor getEntryDescriptor() {
        return entryDescriptor;
    }
}
