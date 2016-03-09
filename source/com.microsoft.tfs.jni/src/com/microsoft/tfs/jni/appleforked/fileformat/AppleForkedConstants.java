// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat;

/**
 * AppleSingle / AppleDouble IDs and other constants
 */
public class AppleForkedConstants {
    /* Magic IDs for identifying AppleForked files */
    public static final int MAGIC_APPLESINGLE = 0x00051600;
    public static final int MAGIC_APPLEDOUBLE = 0x00051607;

    /* Version IDs */
    public static final int VERSION_1 = 0x00010000;
    public static final int VERSION_2 = 0x00020000;

    public static final byte[] FILESYSTEM = new byte[] {
        'M',
        'a',
        'c',
        'i',
        'n',
        't',
        'o',
        's',
        'h',
        ' ',
        ' ',
        ' ',
        ' ',
        ' ',
        ' ',
        ' '
    };

    public static final long MAX_OFFSET = 4294967295L;
    public static final long MAX_LENGTH = 4294967295L;

    public static final int ID_DATAFORK = 1;
    public static final int ID_RESOURCEFORK = 2;
    public static final int ID_FILENAME = 3;
    public static final int ID_COMMENT = 4;
    public static final int ID_ICON_BW = 5;
    public static final int ID_ICON_COLOR = 6;
    public static final int ID_FILEINFO = 7;
    public static final int ID_DATEINFO = 8;
    public static final int ID_FINDERINFO = 9;
    public static final int ID_MACFILEINFO = 10;
    public static final int ID_PRODOSFILEINFO = 11;
    public static final int ID_MSDOSFILEINFO = 12;
    public static final int ID_AFPSHORTNAME = 13;
    public static final int ID_AFPFILEINFO = 14;
    public static final int ID_AFPDIRECTORYID = 15;

    public static final String XATTR_FINDERINFO = "com.apple.FinderInfo"; //$NON-NLS-1$
    public static final String XATTR_COMMENT = "com.apple.metadata:kMDItemFinderComment"; //$NON-NLS-1$

    public static String getNameFromMagic(final int magic) {
        if (magic == MAGIC_APPLESINGLE) {
            return "AppleSingle"; //$NON-NLS-1$
        } else if (magic == MAGIC_APPLEDOUBLE) {
            return "AppleDouble"; //$NON-NLS-1$
        } else {
            return "Apple 0x" + Integer.toHexString(magic); //$NON-NLS-1$
        }
    }
}
