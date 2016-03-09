// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes the comment (in OS X: the "Spotlight Comment") of an on-disk file.
 */
public class AppleForkedCommentEncoder extends ByteArrayChunkedEncoder implements AppleForkedEntryEncoder {
    private final byte[] comment;

    public AppleForkedCommentEncoder(final File file) {
        final byte[] comment = FileSystemUtils.getInstance().getMacExtendedAttribute(
            getFilename(file),
            AppleForkedConstants.XATTR_COMMENT);

        this.comment = (comment != null) ? comment : new byte[0];
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
        return AppleForkedConstants.ID_COMMENT;
    }

    @Override
    public long getLength() {
        return comment.length;
    }

    @Override
    protected byte[] start() throws IOException {
        return comment;
    }

    @Override
    public void close() throws IOException {
    }
}
