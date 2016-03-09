// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.io.OutputStream;

import com.microsoft.tfs.util.Check;

public class BinaryWriter {
    private final OutputStream os;
    private final String charsetName;

    public BinaryWriter(final OutputStream os, final String charsetName) {
        this.os = os;
        this.charsetName = charsetName;
    }

    public void write(final boolean value) throws IOException {
        write((byte) (value == true ? 1 : 0));
    }

    public void write(final byte value) throws IOException {
        final byte[] bytes = new byte[1];
        bytes[0] = value;
        os.write(bytes);
    }

    public void write(final byte[] value) throws IOException {
        Check.notNull(value, "value"); //$NON-NLS-1$
        os.write(value);
    }

    public void write(final short value) throws IOException {
        final byte[] bytes = new byte[2];
        bytes[0] = (byte) value;
        bytes[1] = (byte) (value >> 8);
        os.write(bytes);
    }

    public void write(final int value) throws IOException {
        final byte[] bytes = new byte[4];
        bytes[0] = (byte) value;
        bytes[1] = (byte) (value >> 8);
        bytes[2] = (byte) (value >> 16);
        bytes[3] = (byte) (value >> 24);
        os.write(bytes);
    }

    public void write(final long value) throws IOException {
        final byte[] bytes = new byte[8];
        bytes[0] = (byte) value;
        bytes[1] = (byte) (value >> 8);
        bytes[2] = (byte) (value >> 16);
        bytes[3] = (byte) (value >> 24);
        bytes[4] = (byte) (value >> 32);
        bytes[5] = (byte) (value >> 40);
        bytes[6] = (byte) (value >> 48);
        bytes[7] = (byte) (value >> 56);
        os.write(bytes);
    }

    public void write(final String value) throws IOException {
        Check.notNull(value, "value"); //$NON-NLS-1$

        final byte[] bytes = value.getBytes(charsetName);
        writeStringLength(bytes.length);
        os.write(bytes);
    }

    public void close() throws IOException {
        os.close();
    }

    private void writeStringLength(int value) throws IOException {
        while (value >= 0x80) {
            write((byte) (value | 0x80));
            value >>= 7;
        }
        // The high bit is not set which indicates the end of the length
        // encoding.
        write((byte) value);
    }
}
