// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedHeader;

/**
 * Utilities for working with AppleForked files.
 */
public class AppleForkedUtils {
    /**
     * Returns the AppleForkedHeader from an AppleForked file.
     *
     * @param file
     *        The file to read the header from
     * @return The AppleForkedHeader
     * @throws IOException
     *         If the file could not be read or is not an AppleForked file
     */
    public static AppleForkedHeader getHeader(final File file) throws IOException {
        final byte[] header = new byte[AppleForkedHeader.HEADER_SIZE];
        int headerLen = 0;

        InputStream headerInputStream = null;

        try {
            headerInputStream = new FileInputStream(file);

            while (headerLen < header.length) {
                final int readlen = headerInputStream.read(header, headerLen, (header.length - headerLen));

                if (readlen < 0) {
                    throw new IOException(Messages.getString("AppleForkedUtils.FileIsNotAppleSingleFile")); //$NON-NLS-1$
                }

                headerLen += readlen;
            }
        } finally {
            if (headerInputStream != null) {
                headerInputStream.close();
            }
        }

        return new AppleForkedHeader(header);
    }
}
