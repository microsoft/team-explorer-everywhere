// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream;

import java.io.File;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;

/**
 * Creates an on-disk representation of the file (data fork) and its attributes
 * (FinderInfo, Resource Fork) represented by the streamed-in AppleSingle file.
 * Writes this to the file specified in the constructor.
 */
public final class AppleSingleDecoderStream extends AppleForkedDecoderStream {
    /**
     * Creates a file from the provided AppleSingle data. Will overwrite any
     * existing file.
     *
     * @param file
     *        The File to write data and resource forks to
     */
    public AppleSingleDecoderStream(final File file) {
        super(file, AppleForkedConstants.MAGIC_APPLESINGLE);
    }

    /**
     * Creates a file from the provided AppleSingle data. Will overwrite any
     * existing file.
     *
     * @param filename
     *        The name of the file to write data and resource forks to
     */
    public AppleSingleDecoderStream(final String filename) {
        super(filename, AppleForkedConstants.MAGIC_APPLESINGLE);
    }
}
