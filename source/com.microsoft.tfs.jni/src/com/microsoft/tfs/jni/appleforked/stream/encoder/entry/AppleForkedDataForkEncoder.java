// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.StreamChunkedEncoder;

/**
 * Encodes a data fork from an on-disk file.
 */
public class AppleForkedDataForkEncoder extends StreamChunkedEncoder implements AppleForkedEntryEncoder {
    private final long length;

    public AppleForkedDataForkEncoder(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        long fileLen = file.length();

        if (fileLen > 0) {
            try {
                final InputStream stream = new FileInputStream(file);

                setBufferSize(fileLen);
                setStream(stream);
            } catch (final Exception e) {
                // TODO: log
                fileLen = 0;
            }
        }

        length = fileLen;
    }

    @Override
    public long getType() {
        return AppleForkedConstants.ID_DATAFORK;
    }

    @Override
    public long getLength() {
        return length;
    }
}
