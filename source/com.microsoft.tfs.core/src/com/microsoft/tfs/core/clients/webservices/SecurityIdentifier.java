// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.util.Check;

/**
 * Represents a Windows Security Identifier (SID). Can be used on all platforms.
 *
 * @threadsafety immutable
 * @see http://msdn.microsoft.com/en-us/library/ff632068%28v=prot.10%29.aspx
 *      (SID String Format Syntax)
 */
public class SecurityIdentifier {
    /**
     * The SID revision this class understands.
     */
    public static final int REVISION = 1;

    public static final byte MAX_SUB_AUTHORITIES = 15;

    // Values greater than 2^32 are encoded as hexadecimal
    private static final BigInteger MAX_DECIMAL_AUTHORITY_VALUE = new BigInteger("4294967296"); //$NON-NLS-1$
    // Subauthorities must be <= 2^32
    private static final long MAX_SUBAUTHORITY_VALUE = 4294967296L;

    private static final int MIN_BINARY_LENGTH = 1 + 1 + 6;
    private static final int MIN_SDDL_SEGMENTS = 1 + 1 + 1;
    private static final int MAX_SDDL_SEGMENTS = 1 + 1 + 1 + MAX_SUB_AUTHORITIES;

    private final byte[] binaryForm;
    private final String sddlForm;

    /**
     * The SID specification permits the primary authority value to exceed 2^32
     * (it's encoded as hexadecimal in this case). Use a BigInteger so printing
     * as a string is convenient, as the signed behavior of Java longs would get
     * in the way here.
     */
    private final BigInteger authority;

    /**
     * The SID specification says subauthorities are always <= 10 decimal digits
     * (never hexadecimal), which limits us to a maximum value of
     * "9,999,999,999", well within the non-negative domain of a Java long.
     */
    private final long[] subAuthorities;

    /**
     * Constructs a {@link SecurityIdentifier} from a .NET compatible byte array
     * form.
     */
    public SecurityIdentifier(final byte[] binaryForm, final int offset) {
        Check.notNull(binaryForm, "binaryForm"); //$NON-NLS-1$

        Check.isTrue(
            binaryForm.length - offset >= MIN_BINARY_LENGTH,
            "Binary form array after offset too small to hold a security identifier"); //$NON-NLS-1$

        /*
         * Parse and validate binary form.
         */

        final int revision = binaryForm[offset];
        checkRevision(revision);

        final int subAuthorityCount = binaryForm[offset + 1];
        checkSubAuthorityCount(subAuthorityCount);

        final int length = 1 + 1 + 6 + (subAuthorityCount * 4);

        // Copy to the field
        this.binaryForm = new byte[length];
        for (int i = 0; i < length; i++) {
            this.binaryForm[i] = binaryForm[offset + i];
        }

        // Primary authority is big-endian
        authority = new BigInteger(new byte[] {
            binaryForm[offset + 2],
            binaryForm[offset + 3],
            binaryForm[offset + 5],
            binaryForm[offset + 6],
            binaryForm[offset + 4],
            binaryForm[offset + 7],
        });

        // Subauthorities are little-endian (the .NET comments appear to be
        // incorrect in specifying big-endian)
        subAuthorities = new long[subAuthorityCount];
        for (byte i = 0; i < subAuthorityCount; i++) {
            // Java note: masking promotes the byte to the integer domain,
            // unsigned
            subAuthorities[i] = ((long) (binaryForm[offset + 8 + 4 * i + 0] & 0xFF) << 0)
                + ((long) (binaryForm[offset + 8 + 4 * i + 1] & 0xFF) << 8)
                + ((long) (binaryForm[offset + 8 + 4 * i + 2] & 0xFF) << 16)
                + ((long) (binaryForm[offset + 8 + 4 * i + 3] & 0xFF) << 24);
        }

        /*
         * Convert to string form.
         */

        this.sddlForm = toSDDLForm(authority, subAuthorities);
    }

    public SecurityIdentifier(final String sddlForm) {
        Check.notNullOrEmpty(sddlForm, "sddlForm"); //$NON-NLS-1$

        /*
         * Parse and validate string form.
         */

        this.sddlForm = sddlForm;

        final String[] segments = sddlForm.split("-"); //$NON-NLS-1$

        Check.isTrue(
            segments.length >= MIN_SDDL_SEGMENTS,
            MessageFormat.format(
                "The security identifier contains less than the minimum {0} segments", //$NON-NLS-1$
                Integer.toString(MIN_SDDL_SEGMENTS)));

        Check.isTrue(
            segments.length <= MAX_SDDL_SEGMENTS,
            MessageFormat.format(
                "The security identifier string contains more than the maximum {0} segments", //$NON-NLS-1$
                Integer.toString(MAX_SDDL_SEGMENTS)));

        Check.isTrue("S".equals(segments[0]), "First security identifier segment must be 'S'"); //$NON-NLS-1$//$NON-NLS-2$

        final int revision = Integer.parseInt(segments[1]);
        checkRevision(revision);

        authority = parseAuthority(segments[2]);

        // The count of subauthorities
        final List<Long> subList = new ArrayList<Long>(MAX_SUB_AUTHORITIES);
        for (int i = 3; i < segments.length; i++) {
            subList.add(parseSubAuthority(segments[i]));
        }

        subAuthorities = new long[subList.size()];
        for (int i = 0; i < subList.size(); i++) {
            subAuthorities[i] = subList.get(i);
        }

        /*
         * Convert to binary form.
         */

        this.binaryForm = toBinaryForm(authority, subAuthorities);
    }

    public SecurityIdentifier(final long authority, final long[] subAuthorities) {
        this.authority = new BigInteger(Long.toString(authority));
        this.subAuthorities = subAuthorities.clone();
        this.binaryForm = toBinaryForm(this.authority, subAuthorities);
        this.sddlForm = toSDDLForm(this.authority, subAuthorities);
    }

    /**
     * Parses the primary authority from a decimal or hexadecimal string.
     */
    private static BigInteger parseAuthority(final String authority) {
        if (authority.startsWith("0x")) //$NON-NLS-1$
        {
            Check.isTrue(authority.length() <= 14, "A hexadecimal authority must be at most 12 digits long"); //$NON-NLS-1$
            return new BigInteger(authority.substring(2), 16);
        } else {
            Check.isTrue(authority.length() <= 10, "A decimal authority must be at most 10 digits long"); //$NON-NLS-1$
            return new BigInteger(authority, 10);
        }
    }

    /**
     * Parses a subauthority from a decimal string.
     */
    private static long parseSubAuthority(final String subAuthority) {
        final long ret = Long.parseLong(subAuthority);

        Check.isTrue(
            ret <= MAX_SUBAUTHORITY_VALUE,
            MessageFormat.format(
                "Subauthority value exceeds the maximum value of {0}", //$NON-NLS-1$
                Long.toString(MAX_SUBAUTHORITY_VALUE)));

        return ret;
    }

    public byte[] getBinaryForm() {
        return binaryForm;
    }

    public String getSDDLForm() {
        return sddlForm;
    }

    @Override
    public String toString() {
        return sddlForm;
    }

    public String getValue() {
        return toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SecurityIdentifier == false) {
            return false;
        }

        return Arrays.equals(binaryForm, ((SecurityIdentifier) obj).binaryForm);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(binaryForm);
    }

    private static String toSDDLForm(final BigInteger authority, final long[] subAuthorities) {
        final StringBuilder sb = new StringBuilder();
        sb.append("S-"); //$NON-NLS-1$
        sb.append(REVISION);
        sb.append("-"); //$NON-NLS-1$

        int radix = 10;
        if (authority.compareTo(MAX_DECIMAL_AUTHORITY_VALUE) > 0) {
            radix = 16;
            sb.append("0x"); //$NON-NLS-1$
        }
        sb.append(authority.toString(radix));

        for (int i = 0; i < subAuthorities.length; i++) {
            sb.append("-"); //$NON-NLS-1$
            // A subauthority won't exceed the maximum value of a 32-bit
            // unsigned integer
            sb.append(Long.toString(subAuthorities[i]));
        }

        return sb.toString();
    }

    private static byte[] toBinaryForm(final BigInteger authority, final long[] subAuthorities) {
        final byte[] bytes = new byte[1 + 1 + 6 + 4 * subAuthorities.length];

        bytes[0] = REVISION;
        bytes[1] = (byte) subAuthorities.length;

        // Primary authority is big-endian
        final byte[] authorityBytes = authority.toByteArray();
        Check.isTrue(authorityBytes.length <= 6, "authorityBytes.length <= 6"); //$NON-NLS-1$
        System.arraycopy(authorityBytes, 0, bytes, 2, authorityBytes.length);

        // Subauthorities are little-endian (the .NET comments appear to be
        // incorrect in specifying big-endian)
        for (int i = 0; i < subAuthorities.length; i++) {
            for (int b = 0; b < 4; b += 1) {
                bytes[8 + 4 * i + b] = (byte) (subAuthorities[i] >>> (b * 8));
            }
        }

        return bytes;
    }

    private static void checkRevision(final int revision) {
        Check.isTrue(
            revision == REVISION,
            MessageFormat.format("Unsupported revision {0}", Integer.toString(revision))); //$NON-NLS-1$
    }

    private static void checkSubAuthorityCount(final int subAuthorityCount) {
        Check.isTrue(
            subAuthorityCount <= MAX_SUB_AUTHORITIES,
            MessageFormat.format(
                "The subauthority count {0} exceeds the maximum {1}", //$NON-NLS-1$
                Integer.toString(subAuthorityCount),
                Integer.toString(MAX_SUB_AUTHORITIES)));
    }
}
