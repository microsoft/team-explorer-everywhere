// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import static org.junit.Assert.assertArrayEquals;

import junit.framework.TestCase;

public class SecurityIdentifierTest extends TestCase {
    public void testConstructParts() {
        SecurityIdentifier sid;

        sid = new SecurityIdentifier(IdentifierAuthority.NULL, new long[0]);
        assertEquals("S-1-0", sid.getSDDLForm()); //$NON-NLS-1$

        sid = new SecurityIdentifier(IdentifierAuthority.RESOURCE_MANAGER, new long[0]);
        assertEquals("S-1-9", sid.getSDDLForm()); //$NON-NLS-1$

        sid = new SecurityIdentifier(IdentifierAuthority.RESOURCE_MANAGER, new long[] {
            0x01
        });
        assertEquals("S-1-9-1", sid.getSDDLForm()); //$NON-NLS-1$

        sid = new SecurityIdentifier(IdentifierAuthority.RESOURCE_MANAGER, new long[] {
            0xFFFFFFFFL,
            0x00000000L
        });
        assertEquals("S-1-9-4294967295-0", sid.getSDDLForm()); //$NON-NLS-1$
    }

    public void testConstructStringForm() {
        String s;

        // Smallest SID (no sub authority)
        s = "S-1-1"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        s = "S-1-1-1234"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        // Largest decimal SID (10 digits) with 15 subauthorities
        s = "S-1-9999999999-0-1-2-3-4-5-6-7-8-9-10-11-12-13-14"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        // Largest hexadecimal SID (12 digits) with 15 subauthorities
        s = "S-1-0xFFFFFFFFFF-0-1-2-3-4-5-6-7-8-9-10-11-12-13-14"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        try {
            // Decimal authorities may have 1-10 digits, no more (this one has
            // 11)
            s = "S-1-11111111112-0-1-2-3-4-5-6-7-8-9-10-11-12-13-14"; //$NON-NLS-1$
            new SecurityIdentifier(s);
            fail("Should have thrown for exceeding the maximum decimal authority size (10)"); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            // Hexadecimal authorities may have 1-12 digits, no more (this one
            // has
            // 13)
            s = "S-1-0x0011223344556-0-1-2-3-4-5-6-7-8-9-10-11-12-13-14"; //$NON-NLS-1$
            new SecurityIdentifier(s);
            fail("Should have thrown for exceeding the maximum hexadecimal authority size (12)"); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        try {
            // Too many subauthorities
            s = "S-1-11111111112-0-1-2-3-4-5-6-7-8-9-10-11-12-13-14-15"; //$NON-NLS-1$
            new SecurityIdentifier(s);
            fail("Should have thrown for exceeding the subauthority limit (15)"); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // Real SIDs used in binary form test below

        s = "S-1-5-21-3623811015-3361044348-30300820-1013"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        s = "S-1-5-21-541067253-791394461-2640115302"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        s = "S-1-5-21-124525095-708259637-1543119021-941687"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());

        s = "S-1-5-21-3623811015-3361044348-30300820-1013"; //$NON-NLS-1$
        assertEquals(s, new SecurityIdentifier(s).toString());
    }

    public void testConstructBinaryForm() {
        /*
         * This test compares values computed with the .NET SecurityIdentifier
         * class with values computed by the Java class.
         */

        byte[] bytes;

        bytes = new byte[] {
            0x01,
            0x02,
            0x00,
            0x00,
            0x00,
            (byte) 0xD7,
            (byte) 0xA1,
            0x72,
            0x00,
            (byte) 0xB8,
            (byte) 0x12,
            0x2B,
            (byte) 0xAA,
            0x01,
            (byte) 0xF5,
            0x22
        };
        assertArrayEquals(bytes, new SecurityIdentifier(bytes, 0).getBinaryForm());
        assertEquals("S-1-3617652850-722647040-586482090", new SecurityIdentifier(bytes, 0).toString()); //$NON-NLS-1$

        bytes = new byte[] {
            0x01,
            0x02,
            0x00,
            0x04,
            0x00,
            0x06,
            0x34,
            0x12,
            (byte) 0xC5,
            0x07,
            (byte) 0x5A,
            (byte) 0xC1,
            (byte) 0x2B,
            0x2E,
            0x01,
            (byte) 0xF6
        };
        assertArrayEquals(bytes, new SecurityIdentifier(bytes, 0).getBinaryForm());
        assertEquals("S-1-0x406340012-3243902917-4127272491", new SecurityIdentifier(bytes, 0).toString()); //$NON-NLS-1$
    }
}
