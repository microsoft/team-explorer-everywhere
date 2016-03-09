// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.microsoft.tfs.util.Check;

public class BinaryReader {
    private final InputStream is;
    private final String charsetName;

    public BinaryReader(final InputStream is, final String charsetName) {
        Check.isTrue(is.markSupported(), "is"); //$NON-NLS-1$

        this.is = is;
        this.charsetName = charsetName;
    }

    public boolean isEOF() throws IOException {
        is.mark(1);
        final int value = is.read();
        if (value == -1) {
            return true;
        } else {
            is.reset();
            return false;
        }
    }

    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public byte readByte() throws IOException {
        final int value = is.read();
        if (value == -1) {
            throw new EOFException();
        }

        return (byte) value;
    }

    public byte[] readBytes(final int count) throws IOException {
        final byte[] bytes = new byte[count];
        final int bytesRead = is.read(bytes);
        if (bytesRead == -1) {
            throw new EOFException();
        }

        if (bytesRead < count) {
            throw new IllegalStateException();
        }

        return bytes;
    }

    public short readInt16() throws IOException {
        final byte[] bytes = readBytes(2);

        short value = (short) (bytes[1] & 0xff);
        value <<= 8;
        value |= bytes[0] & 0xff;
        return value;
    }

    public int readInt32() throws IOException {
        final byte[] bytes = readBytes(4);

        int value = bytes[3] & 0xff;
        value <<= 8;
        value |= bytes[2] & 0xff;
        value <<= 8;
        value |= bytes[1] & 0xff;
        value <<= 8;
        value |= bytes[0] & 0xff;
        return value;
    }

    public int readUInt32() throws IOException {
        return readInt32();
    }

    public long readInt64() throws IOException {
        final byte[] bytes = readBytes(8);

        long value = bytes[7] & 0xff;
        value <<= 8;
        value |= bytes[6] & 0xff;
        value <<= 8;
        value |= bytes[5] & 0xff;
        value <<= 8;
        value |= bytes[4] & 0xff;
        value <<= 8;
        value |= bytes[3] & 0xff;
        value <<= 8;
        value |= bytes[2] & 0xff;
        value <<= 8;
        value |= bytes[1] & 0xff;
        value <<= 8;
        value |= bytes[0] & 0xff;
        return value;
    }

    public String readString() throws IOException {
        int multiplier = 1;
        int length = 0;
        do {
            final byte next = readByte();
            length += (next & 0x7f) * multiplier;
            if ((next & 0x80) == 0) {
                break;
            }
            multiplier *= 128;
        } while (true);

        final byte[] bytes = readBytes(length);
        final String s = new String(bytes, charsetName);

        return s;
    }

    public void close() throws IOException {
        is.close();
    }
}
