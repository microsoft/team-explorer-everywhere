// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat;

import com.microsoft.tfs.util.ByteArrayUtils;
import com.microsoft.tfs.util.Check;

/**
 * An entry descriptor in an AppleSingle / AppleDouble file.
 */
public class AppleForkedEntryDescriptor {
    public static final int ENTRY_DESCRIPTOR_SIZE = 12;

    private long type;
    private long offset;
    private long length;

    public AppleForkedEntryDescriptor() {
    }

    public AppleForkedEntryDescriptor(final long type, final long offset, final long length) {
        setType(type);
        setOffset(offset);
        setLength(length);
    }

    public AppleForkedEntryDescriptor(final byte[] buffer) {
        decode(buffer);
    }

    public long getType() {
        return type;
    }

    public void setType(final long type) {
        Check.isTrue(type >= 0, "type >= 0"); //$NON-NLS-1$

        this.type = type;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(final long offset) {
        Check.isTrue(offset >= 0, "offset >= 0"); //$NON-NLS-1$

        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(final long length) {
        Check.isTrue(length >= 0, "length >= 0"); //$NON-NLS-1$

        this.length = length;
    }

    public byte[] encode() {
        final byte[] descriptor = new byte[ENTRY_DESCRIPTOR_SIZE];

        ByteArrayUtils.putUnsignedInt32(descriptor, 0, type);
        ByteArrayUtils.putUnsignedInt32(descriptor, 4, offset);
        ByteArrayUtils.putUnsignedInt32(descriptor, 8, length);

        return descriptor;
    }

    public void decode(final byte[] descriptor) {
        Check.notNull(descriptor, "descriptor"); //$NON-NLS-1$
        Check.isTrue(descriptor.length == ENTRY_DESCRIPTOR_SIZE, "descriptor.length == ENTRY_DESCRIPTOR_SIZE"); //$NON-NLS-1$

        type = ByteArrayUtils.getUnsignedInt32(descriptor, 0);
        offset = ByteArrayUtils.getUnsignedInt32(descriptor, 4);
        length = ByteArrayUtils.getUnsignedInt32(descriptor, 8);
    }

    @Override
    public String toString() {
        final StringBuffer value = new StringBuffer();

        value.append("type=" + type + ", "); //$NON-NLS-1$ //$NON-NLS-2$
        value.append("offset=" + offset + ", "); //$NON-NLS-1$ //$NON-NLS-2$
        value.append("length=" + length); //$NON-NLS-1$

        return value.toString();
    }
}
