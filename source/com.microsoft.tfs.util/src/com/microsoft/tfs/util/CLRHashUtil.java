// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 * The version control notifications in Orcas RTM and Orcas SP1 rely on .NET's
 * String.GetHashCode in order to determine which workspace the notification is
 * for. It turns out that String.GetHashCode is not guaranteed to return the
 * same result across different versions of the .NET Framework. It even returns
 * different values when run as 32bit vs 64bit.
 *
 * The purpose of this class is to provide point-in-time variants of the 32bit
 * and 64bit hash functions that can be used for backwards compatibility with
 * older clients.
 */
public class CLRHashUtil {
    /**
     * Guid.GetHashCode for Dev 10 RTM, presumably the same in previous .NET
     * releases.
     */
    public static int getGUIDHash(final GUID guid) {
        final byte[] bytes = guid.getGUIDBytes();

        /*
         * See $/DevDiv/Feature/CLR_Next/ndp/clr/src/BCL/System/Guid.cs. Field
         * composition and names are preserved here for readability.
         *
         * The byte array that getGUIDBytes() returns is "little-endian",
         * meaning the low-order bytes of the .NET Guid's integral type fields
         * appear first in the array.
         *
         * In Java we must do "& 0xff" to prevent sign extension when promoting
         * to larger types.
         */

        final int _a =
            (((bytes[3] & 0xff) << 24) | ((bytes[2] & 0xff) << 16) | ((bytes[1] & 0xff) << 8) | (bytes[0] & 0xff));
        final short _b = (short) (((bytes[5] & 0xff) << 8) | (bytes[4] & 0xff));
        final short _c = (short) (((bytes[7] & 0xff) << 8) | (bytes[6] & 0xff));
        // byte _d = bytes[8];
        // byte _e = bytes[9];
        final byte _f = bytes[10];
        // byte _g = bytes[11];
        // byte _h = bytes[12];
        // byte _i = bytes[13];
        // byte _j = bytes[14];
        final byte _k = bytes[15];

        return _a ^ ((_b & 0xffff) << 16 | (_c & 0xffff)) ^ (((_f & 0xff) << 24) | (_k & 0xff));
    }

    /**
     * String.GetHashCode for Orcas RTM 32bit (.NET 2.0).
     *
     * @param str
     *        string to hash
     * @return the hash code
     */
    public static int getStringHashOrcas32(final String str) {
        // This was preserved from
        // $/Orcas/PU/TSADT/ndp/clr/src/BCL/System/String.cs.

        int hash1 = (5381 << 16) + 5381;
        int hash2 = hash1;

        /*
         * The C# implementation uses an unsafe code block with pointers to
         * access the memory backing the string class.
         *
         * In the loop 32-bit (int-sized) words are read from the string, each
         * word containing two 16-bit code points or one 16-bit code point and a
         * 0 terminator. Since Java won't return a 0 for the terminator, we have
         * to detect it ourselves and substitute a 0.
         *
         * Each pass of the loop reads 1 or 2 of these 32-bit words, which
         * covers 1, 2, 3, or 4 code points (less than 4 if at the end of
         * string).
         *
         * The translation to Java is intended to be relatively easy to follow
         * when compared with the C# implementation. It was not optimized for
         * speed.
         */

        int cp1;
        int cp2;
        int word;

        int cpIndex = 0;
        int len = str.length();
        while (len > 0) {
            // Read 1 or 2 16-bit code points, hash little-endian 32-bit word
            cp1 = str.codePointAt(cpIndex);
            cp2 = (len >= 2) ? str.codePointAt(cpIndex + 1) : 0;
            word = (cp2 << 16) + cp1;
            hash1 = ((hash1 << 5) + hash1 + (hash1 >> 27)) ^ word;
            cpIndex += 2;

            if (len <= 2) {
                break;
            }

            // Same as above for 2 more code points
            cp1 = str.codePointAt(cpIndex);
            cp2 = (len >= 4) ? str.codePointAt(cpIndex + 1) : 0;
            word = (cp2 << 16) + cp1;
            hash2 = ((hash2 << 5) + hash2 + (hash2 >> 27)) ^ word;
            cpIndex += 2;

            len -= 4;
        }

        return hash1 + (hash2 * 1566083941);
    }

    /**
     * String.GetHashCode for Orcas RTM 64bit (.NET 2.0).
     *
     * @param str
     *        string to hash
     * @return the hash code
     */
    public static int getStringHashOrcas64(final String str) {
        // This was preserved from
        // $/Orcas/PU/TSADT/ndp/clr/src/BCL/System/String.cs.

        int hash1 = 5381;
        int hash2 = hash1;

        /*
         * The C# implementation uses an unsafe code block with pointers to
         * access the memory backing the string class.
         *
         * Each pass of the loop reads and hashes 1 or 2 16-bit code points.
         *
         * The translation to Java is intended to be relatively easy to follow
         * when compared with the C# implementation. It was not optimized for
         * speed.
         */

        int cp1;

        int cpIndex = 0;
        int len = str.length();
        while (len > 0) {
            cp1 = str.codePointAt(cpIndex);
            hash1 = ((hash1 << 5) + hash1) ^ cp1;
            cpIndex++;

            if (len <= 1) {
                break;
            }

            cp1 = str.codePointAt(cpIndex);
            hash2 = ((hash2 << 5) + hash2) ^ cp1;
            cpIndex++;

            len -= 2;
        }

        return hash1 + (hash2 * 1566083941);
    }
}
