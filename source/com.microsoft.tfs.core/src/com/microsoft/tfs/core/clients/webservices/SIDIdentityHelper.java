// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.text.MessageFormat;

import com.microsoft.tfs.util.GUID;

/**
 * Well known SID constants and methods for parsing SIDs.
 *
 * @since TEE-SDK-10.1
 */
public abstract class SIDIdentityHelper {
    /*
     * In C# these are uints, promoted here to longs for ease of use in Java.
     */
    private static final long TFS_SUB_AUTHORITY_0 = 1551374245L;
    private static final long TFS_SUB_AUTHORITY_1 = 1204400969L;
    private static final long TFS_SUB_AUTHORITY_2 = 2402986413L;
    private static final long TFS_SUB_AUTHORITY_3 = 2179408616L;

    public static final String TEAM_FOUNDATION_SID_PREFIX = "S-1-9-" + Long.toString(TFS_SUB_AUTHORITY_0); //$NON-NLS-1$
    public static final String WELL_KNOWN_DOMAIN_SID = MessageFormat.format(
        "{0}-{1}-{2}-{3}", //$NON-NLS-1$
        TEAM_FOUNDATION_SID_PREFIX,
        Long.toString(TFS_SUB_AUTHORITY_1),
        Long.toString(TFS_SUB_AUTHORITY_2),
        Long.toString(TFS_SUB_AUTHORITY_3));
    public static final GUID WELL_KNOWN_DOMAIN_ID = new GUID("A517785C-C947-49B3-8F3A-A9AD81E722E8"); //$NON-NLS-1$
    public static final String WELL_KNOWN_SID_TYPE = "-0-"; //$NON-NLS-1$
    public static final String PHANTOM_KNOWN_SID_TYPE = "-2-"; //$NON-NLS-1$
    public static final int PHANTOM_SID_TYPE = 2;
    public static final String WELL_KNOWN_SID_PREFIX = WELL_KNOWN_DOMAIN_SID + WELL_KNOWN_SID_TYPE;
    public static final String PHANTOM_SID_PREFIX = WELL_KNOWN_DOMAIN_SID + PHANTOM_SID_TYPE;

    /**
     * Construct the "domain Sid" for a TFS identity domain
     */
    public static SecurityIdentifier getDomainSID(final GUID domainId) {
        final long[] subAuthorities = new long[5];

        fillDomainSID(subAuthorities, domainId);

        return new SecurityIdentifier(IdentifierAuthority.RESOURCE_MANAGER, subAuthorities);
    }

    /**
     * Puts the given {@link GUID} and the {@link #TFS_SUB_AUTHORITIES} into the
     * given array.
     */
    private static void fillDomainSID(final long[] subAuthorities, final GUID domainId) {
        // set the constant
        subAuthorities[0] = TFS_SUB_AUTHORITY_0;

        // set the domain id
        final byte[] binaryGuid = domainId.getGUIDBytes();
        for (int i = 0; i < 4; i++) {
            subAuthorities[i + 1] = guidBytesToLong(binaryGuid, i * 4);
        }
    }

    /**
     * Decodes the 4 bytes starting at the index as a little-endian long.
     */
    private static long guidBytesToLong(final byte[] array, final int index) {
        // Masking with 0xFF promotes the byte to the unsigned integer domain
        long ret = 0;
        ret |= array[index + 3] & 0xFF;
        ret |= ((long) (array[index + 2] & 0xFF) << 8);
        ret |= ((long) (array[index + 1] & 0xFF) << 16);
        ret |= ((long) (array[index + 0] & 0xFF) << 24);
        return ret;
    }
}
