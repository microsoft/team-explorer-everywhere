// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat.entry;

/**
 * The FinderInfo entry in an AppleSingle / AppleDouble document.
 */
public class AppleForkedFinderInfoEntry {
    private byte[] finderInfo;

    public AppleForkedFinderInfoEntry() {
    }

    public byte[] getFinderInfo() {
        return finderInfo;
    }

    public void setFinderInfo(final byte[] finderInfo) {
        this.finderInfo = finderInfo;
    }

    public void decode(final byte[] finderInfoBytes) {
        if (finderInfoBytes == null) {
            finderInfo = null;
        } else {
            finderInfo = new byte[finderInfoBytes.length];
            System.arraycopy(finderInfoBytes, 0, finderInfo, 0, finderInfoBytes.length);
        }
    }

    public byte[] encode() {
        if (finderInfo == null) {
            return new byte[0];
        } else {
            final byte[] finderInfoBytes = new byte[finderInfo.length];
            System.arraycopy(finderInfo, 0, finderInfoBytes, 0, finderInfo.length);

            return finderInfoBytes;
        }
    }
}
