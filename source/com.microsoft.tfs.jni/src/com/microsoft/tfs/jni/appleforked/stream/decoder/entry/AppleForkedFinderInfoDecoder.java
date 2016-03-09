// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder.entry;

import java.io.File;

import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedDecoder;

/**
 * A decoder for the FinderInfo field of an AppleForked file. Writes this data
 * to the FinderInfo extended attribute of a Mac OS X file.
 */
public class AppleForkedFinderInfoDecoder extends ByteArrayChunkedDecoder implements AppleForkedEntryDecoder {
    private String filename;

    private byte[] finderInfo;

    public AppleForkedFinderInfoDecoder(final AppleForkedEntryDescriptor descriptor, final File file) {
        super((int) descriptor.getLength());

        Check.isTrue(
            descriptor.getType() == AppleForkedConstants.ID_FINDERINFO,
            "descriptor.getType() == AppleForkedConstants.ID_FINDERINFO"); //$NON-NLS-1$
        Check.notNull(file, "file"); //$NON-NLS-1$

        try {
            filename = file.getCanonicalPath();
        } catch (final Exception e) {
            filename = file.getAbsolutePath();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedDecoder#finish(byte
     * [])
     */
    @Override
    protected void finish(final byte[] buffer) {
        finderInfo = buffer;
    }

    /*
     * Set the finder info here, as we can be sure that the data fork was
     * created.
     *
     * (non-Javadoc)
     *
     * @see bytearrayutils.ByteArrayChunkedDecoder#close()
     */
    @Override
    public void close() {
        if (filename != null && finderInfo != null && finderInfo.length > 0) {
            FileSystemUtils.getInstance().setMacExtendedAttribute(
                filename,
                AppleForkedConstants.XATTR_FINDERINFO,
                finderInfo);
        }
    }
}
