// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.text.MessageFormat;

/**
 * A collection of utilities for working with byte arrays, particularly those
 * for low-level file types or network communication.
 */
public class ByteArrayUtils {
    public static void putBytes(final byte[] dest, final int off, final byte[] source) {
        putBytes(dest, off, source, 0, source.length);
    }

    public static void putBytes(
        final byte[] dest,
        final int destOff,
        final byte[] source,
        final int sourceOff,
        final int len) {
        testCapacity(dest, destOff, len);

        System.arraycopy(source, sourceOff, dest, destOff, len);
    }

    public static byte[] getBytes(final byte[] buf, final int off, final int len) {
        testCapacity(buf, off, len);

        final byte[] value = new byte[len];
        System.arraycopy(buf, off, value, 0, len);

        return value;
    }

    public static void putInt32(final byte[] buf, final int off, final int value) {
        testCapacity(buf, off, 4);

        buf[off + 0] = (byte) ((value & 0xFF000000) >> 24);
        buf[off + 1] = (byte) ((value & 0x00FF0000) >> 16);
        buf[off + 2] = (byte) ((value & 0x0000FF00) >> 8);
        buf[off + 3] = (byte) ((value & 0x000000FF) >> 0);
    }

    public static int getInt32(final byte[] buf, final int off) {
        testCapacity(buf, off, 4);
        return ((buf[off + 0] & 0xFF) << 24)
            | ((buf[off + 1] & 0xFF) << 16)
            | ((buf[off + 2] & 0xFF) << 8)
            | ((buf[off + 3] & 0xFF) << 0);
    }

    public static void putUnsignedInt32(final byte[] buf, final int off, final long value) {
        testCapacity(buf, off, 4);

        if (value < 0 || value > 4294967295L) {
            throw new RuntimeException("Can't add unsigned int32 to buffer, out of range"); //$NON-NLS-1$
        }

        buf[off + 0] = (byte) ((value & 0xFF000000) >> 24);
        buf[off + 1] = (byte) ((value & 0x00FF0000) >> 16);
        buf[off + 2] = (byte) ((value & 0x0000FF00) >> 8);
        buf[off + 3] = (byte) ((value & 0x000000FF) >> 0);
    }

    public static long getUnsignedInt32(final byte[] buf, final int off) {
        testCapacity(buf, off, 4);

        return ((long) (buf[off + 0] & 0xFF) << 24)
            | ((long) (buf[off + 1] & 0xFF) << 16)
            | ((long) (buf[off + 2] & 0xFF) << 8)
            | ((long) (buf[off + 3] & 0xFF) << 0);
    }

    public static void putInt16(final byte[] buf, final int off, final short value) {
        testCapacity(buf, off, 2);

        buf[off + 0] = (byte) ((value & 0xFF00) >> 8);
        buf[off + 1] = (byte) ((value & 0x00FF) >> 0);
    }

    public static short getInt16(final byte[] buf, final int off) {
        testCapacity(buf, off, 2);
        return (short) (((buf[off + 0] & 0xFF) << 8) | ((buf[off + 1] & 0xFF) << 0));
    }

    public static void putUnsignedInt16(final byte[] buf, final int off, final int value) {
        testCapacity(buf, off, 2);

        if (value < 0 || value > 65535) {
            throw new RuntimeException("Can't add unsigned int16 to buffer, out of range"); //$NON-NLS-1$
        }

        buf[off + 0] = (byte) ((value & 0xFF00) >> 8);
        buf[off + 1] = (byte) ((value & 0x00FF) >> 0);
    }

    public static int getUnsignedInt16(final byte[] buf, final int off) {
        testCapacity(buf, off, 2);
        return ((buf[off + 0] & 0xFF) << 8) | ((buf[off + 1] & 0xFF) << 0);
    }

    private static void testCapacity(final byte[] buf, final int off, final int size) {
        if (off + size > buf.length) {
            final String messageFormat = "Can't read {0} bytes from buffer, length: {1}, offset: {2}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, size, buf.length, off);
            throw new ArrayIndexOutOfBoundsException(message);
        }
    }
}
