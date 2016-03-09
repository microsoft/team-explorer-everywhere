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
 * A decoder for the Comment field of an AppleForked file. Writes this data to
 * the Finder Comment extended attribute of a Mac OS X file.
 */
public class AppleForkedCommentDecoder extends ByteArrayChunkedDecoder implements AppleForkedEntryDecoder {
    private String filename;

    private byte[] comment;

    public AppleForkedCommentDecoder(final AppleForkedEntryDescriptor descriptor, final File file) {
        super((int) descriptor.getLength());

        Check.isTrue(
            descriptor.getType() == AppleForkedConstants.ID_COMMENT,
            "descriptor.getType() == AppleForkedConstants.ID_COMMENT"); //$NON-NLS-1$
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
        comment = buffer;
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
        if (filename != null && comment != null && comment.length > 0) {
            FileSystemUtils.getInstance().setMacExtendedAttribute(
                filename,
                AppleForkedConstants.XATTR_COMMENT,
                comment);
        }
    }
}
