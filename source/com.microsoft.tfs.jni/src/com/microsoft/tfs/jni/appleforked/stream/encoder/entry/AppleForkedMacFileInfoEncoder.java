// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.entry.AppleForkedMacFileInfoEntry;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes a MacFileInfo entry from an on-disk resource.
 *
 * At this moment, we do not actually set the MacFileInfo bits, we simply set
 * them to zero.
 */
public class AppleForkedMacFileInfoEncoder extends ByteArrayChunkedEncoder implements AppleForkedEntryEncoder {
    public AppleForkedMacFileInfoEncoder(final File file) {
    }

    @Override
    public long getType() {
        return AppleForkedConstants.ID_MACFILEINFO;
    }

    @Override
    public long getLength() {
        return AppleForkedMacFileInfoEntry.MAC_FILE_INFO_ENTRY_SIZE;
    }

    @Override
    protected byte[] start() throws IOException {
        return new AppleForkedMacFileInfoEntry().encode();
    }

    @Override
    public void close() {
    }
}
