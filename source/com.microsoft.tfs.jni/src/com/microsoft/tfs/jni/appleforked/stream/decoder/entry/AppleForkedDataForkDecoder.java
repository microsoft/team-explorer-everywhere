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
 * Decodes a data fork out of a stream -- that is, writes the data fork from an
 * AppleSingle file directly to an on-disk file.
 */
public class AppleForkedDataForkDecoder extends StreamChunkedDecoder implements AppleForkedEntryDecoder {
    public AppleForkedDataForkDecoder(final AppleForkedEntryDescriptor descriptor, final File file)
        throws FileNotFoundException {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$
        Check.isTrue(
            descriptor.getType() == AppleForkedConstants.ID_DATAFORK,
            "descriptor.getType() == AppleForkedConstants.ID_DATAFORK"); //$NON-NLS-1$
        Check.notNull(file, "file"); //$NON-NLS-1$

        setBufferSize(descriptor.getLength());
        setStream(new FileOutputStream(file));
    }
}
