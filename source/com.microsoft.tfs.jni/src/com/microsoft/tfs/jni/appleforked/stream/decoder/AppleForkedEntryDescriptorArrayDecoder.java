// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ChunkedDecoder;
import com.microsoft.tfs.util.chunkingcodec.ChunkedDecoderArray;

/**
 * Capable of decoding an array of AppleForked entry descriptors. The entry
 * descriptor decoding itself is provided by
 * {@link AppleForkedEntryDescriptorDecoder}.
 */
public class AppleForkedEntryDescriptorArrayDecoder extends ChunkedDecoderArray {
    private final AppleForkedEntryDescriptorDecoder[] decoders;

    public AppleForkedEntryDescriptorArrayDecoder(final int count) {
        Check.isTrue(count >= 0, "count >= 0"); //$NON-NLS-1$

        decoders = new AppleForkedEntryDescriptorDecoder[count];

        for (int i = 0; i < decoders.length; i++) {
            decoders[i] = new AppleForkedEntryDescriptorDecoder();
        }
    }

    @Override
    protected ChunkedDecoder start(final int idx) {
        if (idx >= decoders.length) {
            return null;
        }

        return decoders[idx];
    }

    public AppleForkedEntryDescriptor[] getEntryDescriptors() {
        Check.isTrue(isComplete(), "isComplete()"); //$NON-NLS-1$

        final AppleForkedEntryDescriptor[] descriptor = new AppleForkedEntryDescriptor[decoders.length];

        for (int i = 0; i < decoders.length; i++) {
            descriptor[i] = decoders[i].getEntryDescriptor();
        }

        return descriptor;
    }
}