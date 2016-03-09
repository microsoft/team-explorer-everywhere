// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream.decoder.entry;

import java.io.File;
import java.io.IOException;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.util.Check;

/**
 * Given an AppleForkedEntryDescriptor (read from an AppleSingle or AppleDouble
 * file), returns a suitable Decoder that is capable of understanding this type
 * of file.
 */
public class AppleForkedEntryDecoderFactory {
    public static AppleForkedEntryDecoder getDecoder(final AppleForkedEntryDescriptor descriptor, final File file)
        throws IOException {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$

        if (descriptor.getType() == AppleForkedConstants.ID_DATAFORK) {
            return new AppleForkedDataForkDecoder(descriptor, file);
        } else if (descriptor.getType() == AppleForkedConstants.ID_RESOURCEFORK) {
            return new AppleForkedResourceForkDecoder(descriptor, file);
        } else if (descriptor.getType() == AppleForkedConstants.ID_DATEINFO) {
            return new AppleForkedDateDecoder(descriptor, file);
        } else if (descriptor.getType() == AppleForkedConstants.ID_FINDERINFO) {
            return new AppleForkedFinderInfoDecoder(descriptor, file);
        } else if (descriptor.getType() == AppleForkedConstants.ID_COMMENT) {
            return new AppleForkedCommentDecoder(descriptor, file);
        }

        /*
         * We ignore (or don't understand) this type of entry, simply let it go
         * into the bit bucket
         */
        return new AppleForkedNullEntryDecoder(descriptor);
    }
}
