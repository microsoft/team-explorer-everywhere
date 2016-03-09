// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder.entry;

import java.io.File;
import java.util.Date;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.jni.appleforked.fileformat.entry.AppleForkedDateEntry;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedDecoder;

/**
 * Decodes the "date" of an AppleSingle file. When the file has fully decoded
 * (ie, when the {@link AppleForkedDateDecoder#close()} method is called, it
 * will set the file's modification time to that from the date entry.
 */
public class AppleForkedDateDecoder extends ByteArrayChunkedDecoder implements AppleForkedEntryDecoder {
    private final File file;

    private Date modifiedTime = null;

    protected AppleForkedDateDecoder(final AppleForkedEntryDescriptor descriptor, final File file) {
        super((int) descriptor.getLength());

        Check.isTrue(
            descriptor.getType() == AppleForkedConstants.ID_DATEINFO,
            "descriptor.getType() == AppleForkedConstants.ID_DATEINFO"); //$NON-NLS-1$
        Check.isTrue(
            descriptor.getLength() == AppleForkedDateEntry.DATE_ENTRY_SIZE,
            "descriptor.getLength() == AppleForkedDateEntry.DATE_ENTRY_SIZE"); //$NON-NLS-1$
        Check.notNull(file, "file"); //$NON-NLS-1$

        this.file = file;
    }

    @Override
    protected void finish(final byte[] buffer) {
        final AppleForkedDateEntry dateEntry = new AppleForkedDateEntry(buffer);
        modifiedTime = dateEntry.getModificationDate();
    }

    /*
     * Set the last modified date here, as there will be no more writes to the
     * filesystem at this point, and we will not override our own last modified
     * date.
     *
     * (non-Javadoc)
     *
     * @see bytearrayutils.ByteArrayChunkedDecoder#close()
     */
    @Override
    public void close() {
        if (modifiedTime != null) {
            file.setLastModified(modifiedTime.getTime());
        }
    }
}
