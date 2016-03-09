// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes the FinderInfo field of an on-disk file.
 */
public class AppleForkedFinderInfoEncoder extends ByteArrayChunkedEncoder implements AppleForkedEntryEncoder {
    private final byte[] finderInfo;

    public AppleForkedFinderInfoEncoder(final File file) {
        final byte[] finderInfo = FileSystemUtils.getInstance().getMacExtendedAttribute(
            getFilename(file),
            AppleForkedConstants.XATTR_FINDERINFO);

        this.finderInfo = (finderInfo != null) ? finderInfo : new byte[0];
    }

    private static String getFilename(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (final Exception e) {
            return file.getAbsolutePath();
        }
    }

    @Override
    public long getType() {
        return AppleForkedConstants.ID_FINDERINFO;
    }

    @Override
    public long getLength() {
        return finderInfo.length;
    }

    @Override
    protected byte[] start() throws IOException {
        return finderInfo;
    }

    @Override
    public void close() throws IOException {
    }
}
