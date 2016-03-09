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
 * Encodes a resource fork from an on-disk file.
 */
public class AppleForkedResourceForkEncoder extends StreamChunkedEncoder implements AppleForkedEntryEncoder {
    private final long length;

    public AppleForkedResourceForkEncoder(final File dataFile) {
        Check.notNull(dataFile, "dataFile"); //$NON-NLS-1$

        final File resourceFile = getResourceFile(dataFile);

        long resourceLen = resourceFile.length();

        if (resourceLen > 0) {
            try {
                final InputStream resourceStream = new FileInputStream(resourceFile);

                setBufferSize(resourceLen);
                setStream(resourceStream);
            } catch (final Exception e) {
                // TODO: log
                resourceLen = 0;
            }
        }

        length = resourceLen;
    }

    private final File getResourceFile(final File dataFile) {
        String filename;

        try {
            filename = dataFile.getCanonicalPath();
        } catch (final Exception e) {
            filename = dataFile.getAbsolutePath();
        }

        return new File(filename + "/..namedfork/rsrc"); //$NON-NLS-1$
    }

    @Override
    public long getType() {
        return AppleForkedConstants.ID_RESOURCEFORK;
    }

    @Override
    public long getLength() {
        return length;
    }
}
