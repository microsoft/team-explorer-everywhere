// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream;

import java.io.File;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;

/**
 * Creates an on-disk representation of the filesystem attributes (FinderInfo,
 * Resource Fork) represented by the streamed-in AppleDouble file. Writes this
 * to the file specified in the constructor.
 */
public final class AppleDoubleDecoderStream extends AppleForkedDecoderStream {
    /**
     * Writes the resources specified in the AppleDouble file, written in the
     * stream, to the given File.
     *
     * @param file
     *        The file to write the AppleDouble resources to (must exist)
     */
    public AppleDoubleDecoderStream(final File file) {
        super(file, AppleForkedConstants.MAGIC_APPLEDOUBLE);
    }

    /**
     * Writes the resources specified in the AppleDouble file, written in the
     * stream, to the given File.
     *
     * @param filename
     *        The name of the file to write the AppleDouble resources to (must
     *        exist)
     */
    public AppleDoubleDecoderStream(final String filename) {
        super(filename, AppleForkedConstants.MAGIC_APPLEDOUBLE);
    }

    /*
     * AppleDouble stream decoding should ignore spurious data forks, filename
     * and date information. This data should ONLY be included in AppleSingle
     * files.
     *
     * (non-Javadoc)
     *
     * @see AppleForkedDecoderStream#isIgnored(long)
     */
    @Override
    protected boolean isIgnored(final long entryType) {
        if (entryType == AppleForkedConstants.ID_DATAFORK) {
            return true;
        } else if (entryType == AppleForkedConstants.ID_FILENAME) {
            return true;
        } else if (entryType == AppleForkedConstants.ID_DATEINFO) {
            return true;
        }

        return super.isIgnored(entryType);
    }
}
