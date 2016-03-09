// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

public class UploadedBaselinesCollection {
    private final Map<String, UploadedBaseline> uploadedBaselines =
        new TreeMap<String, UploadedBaseline>(String.CASE_INSENSITIVE_ORDER);

    public void addUploadedBaseline(
        final String targetLocalItem,
        final byte[] baselineFileGuid,
        final long uncompressedLength) {
        synchronized (uploadedBaselines) {
            uploadedBaselines.put(targetLocalItem, new UploadedBaseline(baselineFileGuid, uncompressedLength));
        }
    }

    public byte[] getUploadedBaseline(final String targetLocalItem, final AtomicLong outUncompressedLength) {
        outUncompressedLength.set(-1);

        final UploadedBaseline toReturn = uploadedBaselines.get(targetLocalItem);
        if (toReturn != null) {
            outUncompressedLength.set(toReturn.uncompressedLength);
            return toReturn.baselineFileGUID;
        }

        return null;
    }

    public void removeUploadedBaseline(final String targetLocalItem) {
        uploadedBaselines.remove(targetLocalItem);
    }

    public int getCount() {
        return uploadedBaselines.size();
    }

    private class UploadedBaseline {
        final public byte[] baselineFileGUID;
        final public long uncompressedLength;

        public UploadedBaseline(final byte[] baselineFileGUID, final long uncompressedLength) {
            this.baselineFileGUID = baselineFileGUID;
            this.uncompressedLength = uncompressedLength;
        }
    }
}
