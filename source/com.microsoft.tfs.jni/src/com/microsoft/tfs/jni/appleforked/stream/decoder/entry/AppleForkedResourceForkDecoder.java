// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder.entry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.StreamChunkedDecoder;

/**
 * A decoder for the resource fork of an AppleForked file. Writes of this data
 * will go to the resource fork of the provided file on Mac OS X filesystems.
 */
public class AppleForkedResourceForkDecoder extends StreamChunkedDecoder implements AppleForkedEntryDecoder {
    public AppleForkedResourceForkDecoder(final AppleForkedEntryDescriptor descriptor, final File dataFile)
        throws FileNotFoundException {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$
        Check.isTrue(
            descriptor.getType() == AppleForkedConstants.ID_RESOURCEFORK,
            "descriptor.getType() == AppleForkedConstants.ID_RESOURCEFORK"); //$NON-NLS-1$

        setBufferSize(descriptor.getLength());
        setStream(new FileOutputStream(getResourceFile(dataFile)));
    }

    private final File getResourceFile(final File dataFile) {
        String filename;

        try {
            filename = dataFile.getCanonicalPath();
        } catch (final Exception e) {
            filename = dataFile.getAbsolutePath();
        }

        /*
         * Try to make sure the data fork exists on disk -- we can't update the
         * resource fork without it. Don't fail if we can't open it (we'll fail
         * on the resource fork writing.)
         */
        if (!dataFile.exists()) {
            try {
                final FileOutputStream dataTemp = new FileOutputStream(dataFile);
                dataTemp.close();
            } catch (final Exception e) {
            }
        }

        return new File(filename + "/..namedfork/rsrc"); //$NON-NLS-1$
    }
}
