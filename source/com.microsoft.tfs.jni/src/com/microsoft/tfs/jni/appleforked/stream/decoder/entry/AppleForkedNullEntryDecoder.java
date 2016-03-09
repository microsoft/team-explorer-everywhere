// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder.entry;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;

/**
 * A decoder for AppleSingle/AppleDouble file portions that we want to ignore.
 * Any decoding into this decoder simply goes to the bit bucket.
 */
public class AppleForkedNullEntryDecoder implements AppleForkedEntryDecoder {
    private final long entrySize;
    private long entryLen;

    public AppleForkedNullEntryDecoder(final AppleForkedEntryDescriptor descriptor) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        entrySize = descriptor.getLength();
    }

    @Override
    public int decode(final byte[] buf, final int off, final int len) {
        final int writeLen = (int) Math.min((entrySize - entryLen), len);
        entryLen += writeLen;

        return writeLen;
    }

    @Override
    public boolean isComplete() {
        return (entryLen == entrySize);
    }

    @Override
    public void close() {
    }
}
