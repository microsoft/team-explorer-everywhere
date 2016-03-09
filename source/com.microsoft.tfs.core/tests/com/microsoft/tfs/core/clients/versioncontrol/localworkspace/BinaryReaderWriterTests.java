// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BinaryReader;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BinaryWriter;

import junit.framework.Assert;
import junit.framework.TestCase;

public class BinaryReaderWriterTests extends TestCase {
    private static final String CHARSET = "UTF-16LE"; //$NON-NLS-1$

    private static final byte[] bytes1 = new byte[] {
        0x00,
        0x01,
        0x02,
        0x03,
        (byte) 0xfe,
        (byte) 0xff,
        (byte) 0x7f,
        (byte) 0x80
    };

    private static final byte[] bytesHelloUnicode = new byte[] {
        0x04,
        0x48,
        0x00,
        0x69,
        0x00
    };

    // @formatter:off
    private static final byte[] bytesString150Chars = new byte[]
    {
        (byte) 0xac, 0x02,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
        0x30, 0x00, 0x31, 0x00, 0x32, 0x00, 0x33, 0x00, 0x34, 0x00, 0x35, 0x00, 0x36, 0x00, 0x37, 0x00, 0x38, 0x00, 0x39, 0x00,
    };
    // @formatter:on

    public void testIsEOF() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytesHelloUnicode);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertFalse(br.isEOF());
        Assert.assertEquals(bytesHelloUnicode[0], br.readByte());
        Assert.assertFalse(br.isEOF());
        Assert.assertEquals(bytesHelloUnicode[1], br.readByte());
        Assert.assertFalse(br.isEOF());
        Assert.assertEquals(bytesHelloUnicode[2], br.readByte());
        Assert.assertFalse(br.isEOF());
        Assert.assertEquals(bytesHelloUnicode[3], br.readByte());
        Assert.assertFalse(br.isEOF());
        Assert.assertEquals(bytesHelloUnicode[4], br.readByte());
        Assert.assertTrue(br.isEOF());
        br.close();
    }

    public void testReadBoolean() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytes1);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertFalse(br.readBoolean());
        Assert.assertTrue(br.readBoolean());
        Assert.assertTrue(br.readBoolean());
        br.close();
    }

    public void testReadBytes() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytes1);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals((byte) 0x00, br.readByte());
        Assert.assertEquals((byte) 0x01, br.readByte());
        Assert.assertEquals((byte) 0x02, br.readByte());
        Assert.assertEquals((byte) 0x03, br.readByte());

        Assert.assertEquals((byte) 0xfe, br.readByte());
        Assert.assertEquals((byte) 0xff, br.readByte());
        Assert.assertEquals((byte) 0x7f, br.readByte());
        Assert.assertEquals((byte) 0x80, br.readByte());
        br.close();
    }

    public void testReadByteArray() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytes1);
        final BinaryReader br = new BinaryReader(is, CHARSET);
        Assert.assertTrue(Arrays.equals(bytes1, br.readBytes(bytes1.length)));
        br.close();
    }

    public void testReadInt16() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytes1);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals((short) 0x0100, br.readInt16());
        Assert.assertEquals((short) 0x0302, br.readInt16());
        Assert.assertEquals((short) 0xfffe, br.readInt16());
        Assert.assertEquals((short) 0x807f, br.readInt16());
        br.close();
    }

    public void testReadInt32() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytes1);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals(0x03020100, br.readInt32());
        Assert.assertEquals(0x807ffffe, br.readInt32());
        br.close();
    }

    public void testReadInt64() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytes1);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals(0x807ffffe03020100L, br.readInt64());
        br.close();
    }

    public void testReadSmallString() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytesHelloUnicode);
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals("Hi", br.readString()); //$NON-NLS-1$
        br.close();
    }

    public void testReadString150Chars() throws IOException {
        final InputStream is = new ByteArrayInputStream(bytesString150Chars);
        final BinaryReader br = new BinaryReader(is, CHARSET);
        Assert.assertEquals(createCharString(150), br.readString());
        br.close();
    }

    public void testWriteBytes() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(bytes1[0]);
        bw.write(bytes1[1]);
        bw.write(bytes1[2]);
        bw.write(bytes1[3]);
        bw.write(bytes1[4]);
        bw.write(bytes1[5]);
        bw.write(bytes1[6]);
        bw.write(bytes1[7]);
        bw.close();

        Assert.assertTrue(Arrays.equals(bytes1, os.toByteArray()));
    }

    public void testWriteEmptyByteArray() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        final byte[] emptyByteArray = new byte[0];
        bw.write(emptyByteArray);
        bw.close();

        Assert.assertTrue(Arrays.equals(emptyByteArray, os.toByteArray()));
    }

    public void testWriteByteArray() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(bytes1);
        bw.close();

        Assert.assertTrue(Arrays.equals(bytes1, os.toByteArray()));
    }

    public void testWriteInt16() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write((short) 0x0100);
        bw.write((short) 0x0302);
        bw.write((short) 0xfffe);
        bw.write((short) 0x807f);
        bw.close();

        Assert.assertTrue(Arrays.equals(bytes1, os.toByteArray()));
    }

    public void testWriteInt32() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(0x03020100);
        bw.write(0x807ffffe);
        bw.close();

        Assert.assertTrue(Arrays.equals(bytes1, os.toByteArray()));
    }

    public void testWriteInt64() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(0x807ffffe03020100L);
        bw.close();

        Assert.assertTrue(Arrays.equals(bytes1, os.toByteArray()));
    }

    public void testWriteSmallString() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write("Hi"); //$NON-NLS-1$
        bw.close();

        Assert.assertTrue(Arrays.equals(bytesHelloUnicode, os.toByteArray()));
    }

    public void testWrite150CharString() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(createCharString(150));
        bw.close();

        Assert.assertTrue(Arrays.equals(bytesString150Chars, os.toByteArray()));
    }

    public void testReadWriteBytes() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(bytes1[0]);
        bw.write(bytes1[1]);
        bw.write(bytes1[2]);
        bw.write(bytes1[3]);
        bw.write(bytes1[4]);
        bw.write(bytes1[5]);
        bw.write(bytes1[6]);
        bw.write(bytes1[7]);
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals((byte) 0x00, br.readByte());
        Assert.assertEquals((byte) 0x01, br.readByte());
        Assert.assertEquals((byte) 0x02, br.readByte());
        Assert.assertEquals((byte) 0x03, br.readByte());

        Assert.assertEquals((byte) 0xfe, br.readByte());
        Assert.assertEquals((byte) 0xff, br.readByte());
        Assert.assertEquals((byte) 0x7f, br.readByte());
        Assert.assertEquals((byte) 0x80, br.readByte());
        br.close();
    }

    public void testReadWriteByteArray() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(bytes1);
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);
        Assert.assertTrue(Arrays.equals(bytes1, br.readBytes(bytes1.length)));
        br.close();
    }

    public void testReadWriteInt16() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write((short) 0x0100);
        bw.write((short) 0x0302);
        bw.write((short) 0xfffe);
        bw.write((short) 0x807f);
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals((short) 0x0100, br.readInt16());
        Assert.assertEquals((short) 0x0302, br.readInt16());
        Assert.assertEquals((short) 0xfffe, br.readInt16());
        Assert.assertEquals((short) 0x807f, br.readInt16());
        br.close();
    }

    public void testReadWriteInt32() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(0x03020100);
        bw.write(0x807ffffe);
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals(0x03020100, br.readInt32());
        Assert.assertEquals(0x807ffffe, br.readInt32());
        br.close();
    }

    public void testReadWriteInt64() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(0x807ffffe03020100L);
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals(0x807ffffe03020100L, br.readInt64());
        br.close();
    }

    public void testReadWriteSmallString() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write("Hi"); //$NON-NLS-1$
        bw.close();

        Assert.assertTrue(Arrays.equals(bytesHelloUnicode, os.toByteArray()));

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals("Hi", br.readString()); //$NON-NLS-1$
        br.close();
    }

    public void testReadWrite150CharString() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write(createCharString(150));
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals(createCharString(150), br.readString());
        br.close();
    }

    public void testReadWrite150000CharString() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        final String big = createCharString(150000);
        bw.write(big);
        bw.close();

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals(big, br.readString());
        br.close();
    }

    public void testReadWriteMixedValues() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BinaryWriter bw = new BinaryWriter(os, CHARSET);

        bw.write((byte) 0xff);
        bw.write((short) 0xffee);
        bw.write(0xffeeddcc);
        bw.write(0xffeeddccbbaa9988L);
        bw.write(""); //$NON-NLS-1$
        bw.write("aaa"); //$NON-NLS-1$
        bw.write(createCharString(150));
        bw.write(bytesHelloUnicode);
        bw.write(0x1122334455667788L);
        bw.write(0x11223344);
        bw.write((short) 0x1122);
        bw.write((byte) 0x11);

        final InputStream is = new ByteArrayInputStream(os.toByteArray());
        final BinaryReader br = new BinaryReader(is, CHARSET);

        Assert.assertEquals((byte) 0xff, br.readByte());
        Assert.assertEquals((short) 0xffee, br.readInt16());
        Assert.assertEquals(0xffeeddcc, br.readInt32());
        Assert.assertEquals(0xffeeddccbbaa9988L, br.readInt64());
        Assert.assertEquals("", br.readString()); //$NON-NLS-1$
        Assert.assertEquals("aaa", br.readString()); //$NON-NLS-1$
        Assert.assertEquals(createCharString(150), br.readString());
        Assert.assertTrue(Arrays.equals(bytesHelloUnicode, br.readBytes(bytesHelloUnicode.length)));
        Assert.assertEquals(0x1122334455667788L, br.readInt64());
        Assert.assertEquals(0x11223344, br.readInt32());
        Assert.assertEquals((short) 0x1122, br.readInt16());
        Assert.assertEquals((byte) 0x11, br.readByte());
    }

    private String createCharString(final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length / 10; i++) {
            sb.append("0123456789"); //$NON-NLS-1$
        }
        return sb.toString();
    }
}
