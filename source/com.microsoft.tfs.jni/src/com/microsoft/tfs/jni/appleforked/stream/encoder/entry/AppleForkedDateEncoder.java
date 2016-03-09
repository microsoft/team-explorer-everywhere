// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder.entry;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.entry.AppleForkedDateEntry;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes the "date" of a filesystem resource in AppleSingle format. When
 * encoding begins, gets the last modified date of the file and creates an
 * AppleForkedDateEntry, which is encoded into AppleSingle format.
 */
public class AppleForkedDateEncoder extends ByteArrayChunkedEncoder implements AppleForkedEntryEncoder {
    private File file;
    private Date date;

    public AppleForkedDateEncoder(final File file) {
        this.file = file;
    }

    public AppleForkedDateEncoder(final Date date) {
        this.date = date;
    }

    @Override
    public long getType() {
        return AppleForkedConstants.ID_DATEINFO;
    }

    @Override
    public long getLength() {
        return AppleForkedDateEntry.DATE_ENTRY_SIZE;
    }

    /*
     * Gets the date of the on-disk file.
     *
     * (non-Javadoc)
     *
     * @see bytearrayutils.ByteArrayChunkedEncoder#start()
     */
    @Override
    protected byte[] start() throws IOException {
        Date modifiedDate;

        if (date != null) {
            modifiedDate = date;
        } else if (file != null) {
            if (!file.exists()) {
                throw new IOException("File does not exist"); //$NON-NLS-1$
            }

            modifiedDate = new Date(file.lastModified());
        } else {
            modifiedDate = new Date();
        }

        final AppleForkedDateEntry dateEntry = new AppleForkedDateEntry(modifiedDate, modifiedDate, null, null);
        final byte[] dateData = dateEntry.encode();

        Check.isTrue(
            dateData.length == AppleForkedDateEntry.DATE_ENTRY_SIZE,
            "dateData.length == AppleForkedDateEntry.DATE_ENTRY_SIZE"); //$NON-NLS-1$

        return dateData;
    }

    @Override
    public void close() {
    }
}
