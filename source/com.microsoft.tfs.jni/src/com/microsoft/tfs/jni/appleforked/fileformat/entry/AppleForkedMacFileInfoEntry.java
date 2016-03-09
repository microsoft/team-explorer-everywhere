// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat.entry;

import java.text.MessageFormat;

import com.microsoft.tfs.util.Check;

/**
 * The MacFileInfo entry in an AppleSingle / AppleDouble (version 2.0) document.
 */
public class AppleForkedMacFileInfoEntry {
    public static final int MAC_FILE_INFO_ENTRY_SIZE = 4;

    private boolean locked;
    private boolean protect;

    private static final int LOCKED_BIT = (1 << 0);
    private static final int PROTECTED_BIT = (1 << 1);

    public AppleForkedMacFileInfoEntry() {
    }

    public AppleForkedMacFileInfoEntry(final byte[] macFileInfo) {
        decode(macFileInfo);
    }

    public boolean getLocked() {
        return locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public boolean isProtected() {
        return protect;
    }

    public void setProtected(final boolean protect) {
        this.protect = protect;
    }

    public void decode(final byte[] macFileInfo) {
        Check.notNull(macFileInfo, "macFileInfo"); //$NON-NLS-1$
        Check.isTrue(macFileInfo.length >= MAC_FILE_INFO_ENTRY_SIZE, "macFileInfo.length >= MAC_FILE_INFO_ENTRY_SIZE"); //$NON-NLS-1$

        locked = (macFileInfo[3] & LOCKED_BIT) == LOCKED_BIT;
        protect = (macFileInfo[3] & PROTECTED_BIT) == PROTECTED_BIT;
    }

    public byte[] encode() {
        final byte[] macFileInfo = new byte[MAC_FILE_INFO_ENTRY_SIZE];

        macFileInfo[3] ^= locked ? LOCKED_BIT : 0;
        macFileInfo[3] ^= protect ? PROTECTED_BIT : 0;

        return macFileInfo;
    }

    @Override
    public String toString() {
        return MessageFormat.format("protected={0}, locked={1}", (protect ? "true" : "false"), (locked ? "true" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            : "false")); //$NON-NLS-1$
    }
}
