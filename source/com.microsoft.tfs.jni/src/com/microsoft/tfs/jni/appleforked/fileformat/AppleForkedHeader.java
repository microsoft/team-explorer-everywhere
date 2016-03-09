// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.microsoft.tfs.util.ByteArrayUtils;
import com.microsoft.tfs.util.Check;

/**
 * The header of an AppleSingle / AppleDouble file.
 */
public class AppleForkedHeader {
    public static final int HEADER_SIZE = 26;
    public static final int HEADER_FILESYSTEM_SIZE = 16;

    /* Header fields */
    private int magic;
    private int version;
    private int entryCount;

    private String filesystem = null;

    public AppleForkedHeader() {
    }

    public AppleForkedHeader(final int magic, final int version, final String filesystem, final int entryCount) {
        setMagic(magic);
        setVersion(version);
        setFilesystem(filesystem);
        setEntryCount(entryCount);
    }

    public AppleForkedHeader(final byte[] headerBytes) {
        decode(headerBytes);
    }

    public int getMagic() {
        return magic;
    }

    public void setMagic(final int magic) {
        this.magic = magic;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    public String getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(final String filesystem) {
        this.filesystem = filesystem;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public void setEntryCount(final int entryCount) {
        Check.isTrue(entryCount >= 0, "entryCount >= 0"); //$NON-NLS-1$

        this.entryCount = entryCount;
    }

    public byte[] encode() {
        final byte[] header = new byte[HEADER_SIZE];
        final byte[] filesystemBytes = new byte[HEADER_FILESYSTEM_SIZE];

        if (filesystem != null) {
            final byte[] filesystemTemp = Charset.forName("US-ASCII").encode(filesystem).array(); //$NON-NLS-1$

            for (int i = 0; i < HEADER_FILESYSTEM_SIZE; i++) {
                filesystemBytes[i] = (i < filesystemTemp.length) ? filesystemTemp[i] : 32;
            }
        }

        ByteArrayUtils.putInt32(header, 0, magic);
        ByteArrayUtils.putInt32(header, 4, version);
        ByteArrayUtils.putBytes(header, 8, filesystemBytes);
        ByteArrayUtils.putUnsignedInt16(header, 24, entryCount);

        return header;
    }

    public void decode(final byte[] header) {
        Check.notNull(header, "header"); //$NON-NLS-1$
        Check.isTrue(header.length == HEADER_SIZE, "header.length == HEADER_SIZE"); //$NON-NLS-1$

        magic = ByteArrayUtils.getInt32(header, 0);
        version = ByteArrayUtils.getInt32(header, 4);
        final byte[] filesystemTemp = ByteArrayUtils.getBytes(header, 8, HEADER_FILESYSTEM_SIZE);
        entryCount = ByteArrayUtils.getUnsignedInt16(header, 24);

        int filesystemLen = 0;

        for (int i = 0; i < HEADER_FILESYSTEM_SIZE; i++, filesystemLen++) {
            /* The end of the filesystem is specified by a null or a space */
            if (filesystemTemp[i] == 0 || filesystemTemp[i] == 32) {
                break;
            }
        }

        if (filesystemLen == 0) {
            filesystem = null;
        } else {
            filesystem =
                Charset.forName("US-ASCII").decode(ByteBuffer.wrap(filesystemTemp, 0, filesystemLen)).toString(); //$NON-NLS-1$
        }
    }

    @Override
    public String toString() {
        final StringBuffer value = new StringBuffer();

        if (magic == AppleForkedConstants.MAGIC_APPLESINGLE) {
            value.append("AppleSingle"); //$NON-NLS-1$
        } else if (magic == AppleForkedConstants.MAGIC_APPLEDOUBLE) {
            value.append("AppleDouble"); //$NON-NLS-1$
        } else {
            value.append("magic=0x" + Integer.toHexString(magic)); //$NON-NLS-1$
        }

        value.append(", "); //$NON-NLS-1$

        if (version == AppleForkedConstants.VERSION_1) {
            value.append("version=1"); //$NON-NLS-1$
        } else if (version == AppleForkedConstants.VERSION_2) {
            value.append("version=2"); //$NON-NLS-1$
        } else {
            value.append("version=0x" + Integer.toHexString(version)); //$NON-NLS-1$
        }

        value.append(", "); //$NON-NLS-1$

        value.append("filesystem=" + filesystem); //$NON-NLS-1$

        value.append(", "); //$NON-NLS-1$

        value.append("entryCount=" + entryCount); //$NON-NLS-1$

        return value.toString();
    }
}