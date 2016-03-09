// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Globally unique identifier object. Can parse multiple GUID string formats,
 * can output in multiple string formats, and can output raw bytes.
 * <p>
 * A {@link GUID}'s external representation as string or byte array matches the
 * .NET Guid class's external representations of the same object.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class GUID implements Serializable, Comparable<GUID> {
    public static final GUID EMPTY = new GUID("00000000-0000-0000-0000-000000000000"); //$NON-NLS-1$
    static final long serialVersionUID = -108693329452431991L;

    /*
     * It's necessary to document the order in which bytes are stored in this
     * array so the methods in this class make sense.
     *
     * The .NET Guid class differs in the order it presents bytes when asked for
     * a string representation (ToString()) versus a byte array representation
     * (ToByteArray()). Internally the Guid class does not store an array of
     * bytes like this class, it uses integral type fields (int, short, byte)
     * instead.
     *
     * Let's call .NET's hex string representation can "big-endian" because the
     * high-order bytes of the internal fields are send as hex digits before the
     * low-order bytes.
     *
     * .NET's byte array representation is therefore "little-endian" because the
     * low-order bytes of the internal fields are put in the array before the
     * high-order bytes.
     *
     * This class uses the little-endian format in the internal array because we
     * had to choose one, it makes getGUIDBytes() a simple clone, and having it
     * match .NET's ToByteArray() representation makes debugging and testing
     * eaiser.
     *
     * Externally, this class matches .NET Guid: hex strings are "big-endian",
     * byte arrays are "little-endian".
     */
    private final byte[] guidBytes;

    // A lazy initialized string representation of the GUID bytes. This is a map
    // of string format (key) to guid string value in that format.
    private final Map<GUIDStringFormat, String> guidStrings = new HashMap<GUIDStringFormat, String>();

    /**
     * Creates a new instance of the GUID class identical to the specified GUID.
     *
     * @param guid
     *        The GUID to clone
     */
    public GUID(final GUID guid) {
        Check.notNull(guid, "guid"); //$NON-NLS-1$

        // Returns a clone
        guidBytes = guid.getGUIDBytes();
    }

    /**
     * Creates a new instance of the GUID class using the value represented by
     * the specified string.
     *
     * @param guidString
     *        A string that contains a GUID in one of the following formats ("d"
     *        represents a hexadecimal digit whose case is ignored): 32
     *        contiguous digits: dddddddddddddddddddddddddddddddd -or- Groups of
     *        8, 4, 4, 4, and 12 digits with hyphens between the groups. The
     *        entire GUID can optionally be enclosed in matching braces or
     *        parentheses: dddddddd-dddd-dddd-dddd-dddddddddddd -or-
     *        {dddddddd-dddd-dddd-dddd-dddddddddddd} -or-
     *        (dddddddd-dddd-dddd-dddd-dddddddddddd)
     * @throws IllegalArgumentException
     */
    public GUID(String guidString) {
        Check.notNull(guidString, "guidString"); //$NON-NLS-1$

        if (guidString.indexOf('-') != -1) {
            // String brackets or parenthesis if needed.
            if (guidString.startsWith("{") && guidString.endsWith("}")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                guidString = guidString.substring(1, guidString.length() - 1);
            } else if (guidString.startsWith("(") && guidString.endsWith(")")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                guidString = guidString.substring(1, guidString.length() - 1);
            }

            // Split the GUID at dashes and verify groups of 8, 4, 4, 4, and 12
            // characters.
            final String[] parts = guidString.split("-"); //$NON-NLS-1$
            if (parts.length != 5
                || parts[0].length() != 8
                || parts[1].length() != 4
                || parts[2].length() != 4
                || parts[3].length() != 4
                || parts[4].length() != 12) {
                throw new IllegalArgumentException(Messages.getString("GUID.SpecifiedStringIsNotValidGUID")); //$NON-NLS-1$
            }
        } else if (guidString.length() != 32) {
            throw new IllegalArgumentException(Messages.getString("GUID.SpecifiedStringIsNotValidGUID")); //$NON-NLS-1$
        }

        // The input hex string is always in "big-endian" order, this method
        // will flip.
        guidBytes = getGUIDBytes(guidString);
    }

    /**
     * Returns a GUID as represented by the given byte array. The byte array
     * will be parsed according to the MS GUID spec for compatibility with the
     * .NET GUID serialization/deserialization schemes.
     *
     * @param guidBytes
     *        A 16 byte array of the GUID byte value
     */
    public GUID(final byte[] guidBytes) {
        Check.notNull(guidBytes, "guidBytes"); //$NON-NLS-1$
        Check.isTrue(guidBytes.length == 16, "guidBytes.length == 16"); //$NON-NLS-1$
        this.guidBytes = guidBytes.clone();
    }

    /**
     * Create a new random GUID.
     *
     *
     * @return An instance of a new random GUID.
     */
    public static GUID newGUID() {
        return new GUID(newGUIDString());
    }

    /**
     * Create a new random GUID.
     *
     *
     * @return A string representation of a new random GUID.
     */
    public static String newGUIDString() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the GUID bytes.
     */
    public byte[] getGUIDBytes() {
        // Bytes are always "little-endian" internally and externally
        return guidBytes.clone();
    }

    /**
     * @return the dddddddd-dddd-dddd-dddd-dddddddddddd string representation
     *         {@link GUID} type wraps
     */
    public final String getGUIDString() {
        // Returns a "big-endian" hex string
        return getGUIDString(GUIDStringFormat.DASHED);
    }

    /**
     * Returns a string representation of this GUID in the format described.
     *
     * @param format
     *        The string format to use when constructing the string.
     * @return A string representation of this GUID in the given format.
     */
    public final String getGUIDString(GUIDStringFormat format) {
        if (format == null) {
            format = GUIDStringFormat.DASHED;
        }

        if (guidStrings.containsKey(format)) {
            return guidStrings.get(format);
        }

        // Returns a "big-endian" hex string
        final String guidString = createGUIDString(format);

        guidStrings.put(format, guidString);

        return guidString;
    }

    private String createGUIDString(final GUIDStringFormat format) {
        final StringBuffer sb = new StringBuffer();

        if (format.equals(GUIDStringFormat.BRACES)) {
            sb.append('{');
        } else if (format.equals(GUIDStringFormat.PARENTHESES)) {
            sb.append('(');
        }

        /*
         * Swap some bytes to produce a "big-endian" hex string.
         */
        final byte[] bytes = guidBytes.clone();

        // 32-bit word
        swapBytes(bytes, 0, 3);
        swapBytes(bytes, 1, 2);

        // 16-bit word
        swapBytes(bytes, 4, 5);

        // 16 bit word
        swapBytes(bytes, 6, 7);

        for (int i = 0; i < bytes.length; i++) {
            if ((!format.equals(GUIDStringFormat.NONE)) && (i == 4 || i == 6 || i == 8 || i == 10)) {
                sb.append('-');
            }

            final short unsignedByte = (short) (0x00ff & (bytes[i]));
            final String hex = Integer.toHexString(unsignedByte);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }

        if (format.equals(GUIDStringFormat.BRACES)) {
            sb.append('}');
        } else if (format.equals(GUIDStringFormat.PARENTHESES)) {
            sb.append(')');
        }

        return sb.toString().toLowerCase(Locale.US);
    }

    /**
     * Returns a byte array format of this GUID in the .NET scheme, suitable for
     * interoperability with the .NET Guid object serialization/deserialization
     * mechanisms.
     * <p>
     * Since hex strings are always "big-endian" order, this method converts to
     * a "little-endian" byte array.
     *
     * @return A 16 byte array of the bytes in the GUID
     */
    private static byte[] getGUIDBytes(String guidString) {
        guidString = guidString.replaceAll("-", ""); //$NON-NLS-1$ //$NON-NLS-2$

        if (guidString.length() != 32) {
            throw new IllegalArgumentException(Messages.getString("GUID.SpecifiedStringIsNotValidGUID")); //$NON-NLS-1$
        }

        final byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            final int offset = i * 2;
            bytes[i] = (byte) Integer.parseInt(guidString.substring(offset, offset + 2), 16);
        }

        // Must reorder some bytes so array is little-endian

        // 32-bit word
        swapBytes(bytes, 0, 3);
        swapBytes(bytes, 1, 2);

        // 16-bit word
        swapBytes(bytes, 4, 5);

        // 16 bit word
        swapBytes(bytes, 6, 7);

        return bytes;
    }

    /**
     * Swaps the two bytes at the specified offsets within the specified byte
     * array.
     */
    private static void swapBytes(final byte[] bytes, final int offset1, final int offset2) {
        byte temp;
        temp = bytes[offset1];
        bytes[offset1] = bytes[offset2];
        bytes[offset2] = temp;
    }

    /**
     * Tests the given GUID for equality by comparing GUID bytes.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (object == null) {
            return false;
        }
        if (object instanceof GUID == false) {
            return false;
        }
        if (object == this) {
            return true;
        }

        final GUID other = (GUID) object;
        return Arrays.equals(other.guidBytes, guidBytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // Hash code from Joshua Bloch's "Effective Java"
        int hashCode = 17;
        for (int i = 0; i < guidBytes.length; i++) {
            hashCode = 31 * hashCode + guidBytes[i];
        }
        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getGUIDString();
    }

    /**
     * Returns an array of Strings corresponding to the string values of the
     * specified GUIDs.
     */
    public static String[] toStringArray(final GUID[] guids) {
        if (guids == null) {
            return null;
        }

        final String[] strings = new String[guids.length];
        for (int i = 0; i < guids.length; i++) {
            strings[i] = guids[i].getGUIDString();
        }
        return strings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final GUID other) {
        return String.CASE_INSENSITIVE_ORDER.compare(
            getGUIDString(GUIDStringFormat.NONE),
            other.getGUIDString(GUIDStringFormat.NONE));
    }

    /**
     * Specifies the return code for GUID string formatting.
     */
    public static final class GUIDStringFormat extends TypesafeEnum {
        /**
         * 32 digits, for example: 00000000000000000000000000000000
         */
        public static final GUIDStringFormat NONE = new GUIDStringFormat(0);

        /**
         * 32 digits separated by hyphens, for example:
         * 00000000-0000-0000-0000-000000000000.
         */
        public static final GUIDStringFormat DASHED = new GUIDStringFormat(1);

        /**
         * 32 digits separated by hyphens and enclosed in braces, for example:
         * {00000000-0000-0000-0000-000000000000}.
         */
        public static final GUIDStringFormat BRACES = new GUIDStringFormat(2);

        /**
         * 32 digits separated by hyphens and enclosed in parentheses, for
         * example: (00000000-0000-0000-0000-000000000000)
         */
        public static final GUIDStringFormat PARENTHESES = new GUIDStringFormat(3);

        private GUIDStringFormat(final int value) {
            super(value);
        }
    }
}
