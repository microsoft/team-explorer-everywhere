// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat.entry;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.microsoft.tfs.util.Check;

/**
 * The filename entry in an AppleSingle document.
 */
public class AppleForkedFilenameEntry {
    private String filename = ""; //$NON-NLS-1$

    public AppleForkedFilenameEntry() {
    }

    public AppleForkedFilenameEntry(final String filename) {
        this.filename = filename;
    }

    public AppleForkedFilenameEntry(final byte[] filename) {
        decode(filename);
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.filename = filename;
    }

    public void decode(final byte[] filename) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.filename = Charset.forName("UTF-8").decode(ByteBuffer.wrap(filename)).toString(); //$NON-NLS-1$
    }

    public byte[] encode() {
        return Charset.forName("UTF-8").encode(filename).array(); //$NON-NLS-1$
    }

    @Override
    public String toString() {
        return filename;
    }
}
