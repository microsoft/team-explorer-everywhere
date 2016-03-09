// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ChunkedEncoder;
import com.microsoft.tfs.util.chunkingcodec.ChunkedEncoderArray;

/**
 * Encodes an array of AppleForked entry descriptors. Uses
 * {@link AppleForkedEntryDescriptorEncoder} to do the work.
 */
public class AppleForkedEntryDescriptorArrayEncoder extends ChunkedEncoderArray {
    private final AppleForkedEntryDescriptor[] descriptors;

    public AppleForkedEntryDescriptorArrayEncoder(final AppleForkedEntryDescriptor[] descriptors) {
        Check.notNull(descriptors, "descriptors"); //$NON-NLS-1$

        for (int i = 0; i < descriptors.length; i++) {
            Check.notNull(descriptors[i], "descriptors[i]"); //$NON-NLS-1$
        }

        this.descriptors = descriptors;
    }

    @Override
    protected ChunkedEncoder start(final int idx) {
        if (idx >= descriptors.length) {
            return null;
        }

        return new AppleForkedEntryDescriptorEncoder(descriptors[idx]);
    }
}
