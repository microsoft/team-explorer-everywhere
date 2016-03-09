// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.encoder;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.chunkingcodec.ByteArrayChunkedEncoder;

/**
 * Encodes an AppleForked entry descriptor, which describes the entries an
 * AppleSingle/AppleDouble file contains.
 */
public class AppleForkedEntryDescriptorEncoder extends ByteArrayChunkedEncoder {
    private final AppleForkedEntryDescriptor descriptor;

    public AppleForkedEntryDescriptorEncoder(final AppleForkedEntryDescriptor descriptor) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        this.descriptor = descriptor;
    }

    @Override
    protected byte[] start() {
        return descriptor.encode();
    }
}
