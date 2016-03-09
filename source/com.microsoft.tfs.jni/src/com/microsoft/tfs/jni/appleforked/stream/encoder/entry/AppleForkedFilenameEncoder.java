// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.entry.AppleForkedFilenameEntry;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes the filename entry for an AppleForked file from an on-disk file.
 */
public class AppleForkedFilenameEncoder extends ByteArrayChunkedEncoder implements AppleForkedEntryEncoder {
    private final AppleForkedFilenameEntry filenameEntry;

    public AppleForkedFilenameEncoder(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        filenameEntry = new AppleForkedFilenameEntry(file.getName());
    }

    public AppleForkedFilenameEncoder(final String filename) {
        filenameEntry = new AppleForkedFilenameEntry(getFilename(filename));
    }

    private static String getFilename(final String filename) {
        if (filename == null) {
            return ""; //$NON-NLS-1$
        }

        final int lastSlash = filename.lastIndexOf(File.separator);

        if (lastSlash == -1) {
            return filename;
        } else if (lastSlash + 1 < filename.length()) {
            return filename.substring(lastSlash + 1);
        }

        return ""; //$NON-NLS-1$
    }

    @Override
    public long getType() {
        return AppleForkedConstants.ID_FILENAME;
    }

    @Override
    public long getLength() {
        return filenameEntry.encode().length;
    }

    /*
     * Gets the filename of the on-disk file.
     *
     * (non-Javadoc)
     *
     * @see bytearrayutils.ByteArrayChunkedEncoder#start()
     */
    @Override
    protected byte[] start() throws IOException {
        return filenameEntry.encode();
    }

    @Override
    public void close() {
    }
}
