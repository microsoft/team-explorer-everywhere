// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import static org.junit.Assert.assertArrayEquals;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import junit.framework.TestCase;

public class GUIDTest extends TestCase {
    /*
     * The strinsg here were manually formatted with .NET's Guid class from the
     * byte arrays below. This ensures our implementation converts between the
     * two forms the same (.NET uses a different ordering).
     */

    // .NET Guid "N"o-dash format
    private static String guidLowerNoDash = "00000000000000000000000000000000"; //$NON-NLS-1$
    private static String guidRandomNoDash = "daa177085b9b2c4ca3e5511f5c658e24"; //$NON-NLS-1$
    private static String guidUpperNoDash = "ffffffffffffffffffffffffffffffff"; //$NON-NLS-1$

    // .NET Guid "D"ashed format
    private static String guidLowerDash = "00000000-0000-0000-0000-000000000000"; //$NON-NLS-1$
    private static String guidRandomDash = "daa17708-5b9b-2c4c-a3e5-511f5c658e24"; //$NON-NLS-1$
    private static String guidUpperDash = "ffffffff-ffff-ffff-ffff-ffffffffffff"; //$NON-NLS-1$

    // .NET Guid "B"races format
    private static String guidLowerBracket = "{00000000-0000-0000-0000-000000000000}"; //$NON-NLS-1$
    private static String guidRandomBracket = "{daa17708-5b9b-2c4c-a3e5-511f5c658e24}"; //$NON-NLS-1$
    private static String guidUpperBracket = "{ffffffff-ffff-ffff-ffff-ffffffffffff}"; //$NON-NLS-1$

    // .NET Guid "P"arenthesis format
    private static String guidLowerParen = "(00000000-0000-0000-0000-000000000000)"; //$NON-NLS-1$
    private static String guidRandomParen = "(daa17708-5b9b-2c4c-a3e5-511f5c658e24)"; //$NON-NLS-1$
    private static String guidUpperParen = "(ffffffff-ffff-ffff-ffff-ffffffffffff)"; //$NON-NLS-1$

    private static byte[] guidLowerBytes = new byte[] {
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
        (byte) 0x00,
    };

    private static byte[] guidRandomBytes = new byte[] {
        (byte) 0x08,
        (byte) 0x77,
        (byte) 0xa1,
        (byte) 0xda,
        (byte) 0x9b,
        (byte) 0x5b,
        (byte) 0x4c,
        (byte) 0x2c,
        (byte) 0xa3,
        (byte) 0xe5,
        (byte) 0x51,
        (byte) 0x1f,
        (byte) 0x5c,
        (byte) 0x65,
        (byte) 0x8e,
        (byte) 0x24,
    };

    private static byte[] guidUpperBytes = new byte[] {
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
        (byte) 0xff,
    };

    public void testCaseSensitivity() {
        assertEquals(
            new GUID("ABCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc"), //$NON-NLS-1$
            new GUID("abcdefab-0ffa-bcda-a055-abcdefffaffc")); //$NON-NLS-1$
    }

    public void testInvalidInput() {
        verifyIllegalArgumentExceptionForGUID(""); //$NON-NLS-1$
        verifyIllegalArgumentExceptionForGUID("a"); //$NON-NLS-1$
        verifyIllegalArgumentExceptionForGUID("ZBCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFz"); //$NON-NLS-1$
        verifyIllegalArgumentExceptionForGUID("<aBCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc>"); //$NON-NLS-1$
    }

    public void testNoDashInputFormat() {
        final GUID g1 = new GUID(guidRandomNoDash);
        assertArrayEquals(g1.getGUIDBytes(), guidRandomBytes);
        assertEquals(g1.getGUIDString(), guidRandomDash);

        final GUID g2 = new GUID(guidLowerNoDash);
        assertArrayEquals(g2.getGUIDBytes(), guidLowerBytes);
        assertEquals(g2.getGUIDString(), guidLowerDash);

        final GUID g3 = new GUID(guidUpperNoDash);
        assertArrayEquals(g3.getGUIDBytes(), guidUpperBytes);
        assertEquals(g3.getGUIDString(), guidUpperDash);
    }

    public void testDashInputFormat() {
        final GUID g1 = new GUID(guidRandomDash);
        assertArrayEquals(g1.getGUIDBytes(), guidRandomBytes);
        assertEquals(g1.getGUIDString(), guidRandomDash);

        final GUID g2 = new GUID(guidLowerDash);
        assertArrayEquals(g2.getGUIDBytes(), guidLowerBytes);
        assertEquals(g2.getGUIDString(), guidLowerDash);

        final GUID g3 = new GUID(guidUpperDash);
        assertArrayEquals(g3.getGUIDBytes(), guidUpperBytes);
        assertEquals(g3.getGUIDString(), guidUpperDash);
    }

    public void testBracketInputFormat() {
        final GUID g1 = new GUID(guidRandomBracket);
        assertArrayEquals(g1.getGUIDBytes(), guidRandomBytes);
        assertEquals(g1.getGUIDString(), guidRandomDash);

        final GUID g2 = new GUID(guidLowerBracket);
        assertArrayEquals(g2.getGUIDBytes(), guidLowerBytes);
        assertEquals(g2.getGUIDString(), guidLowerDash);

        final GUID g3 = new GUID(guidUpperBracket);
        assertArrayEquals(g3.getGUIDBytes(), guidUpperBytes);
        assertEquals(g3.getGUIDString(), guidUpperDash);
    }

    public void testParenInputFormat() {
        final GUID g1 = new GUID(guidRandomParen);
        assertArrayEquals(g1.getGUIDBytes(), guidRandomBytes);
        assertEquals(g1.getGUIDString(), guidRandomDash);

        final GUID g2 = new GUID(guidLowerParen);
        assertArrayEquals(g2.getGUIDBytes(), guidLowerBytes);
        assertEquals(g2.getGUIDString(), guidLowerDash);

        final GUID g3 = new GUID(guidUpperParen);
        assertArrayEquals(g3.getGUIDBytes(), guidUpperBytes);
        assertEquals(g3.getGUIDString(), guidUpperDash);
    }

    public void testRandomEncodeDecode() throws Exception {
        /* Test conversion from string -> byte array and back */
        for (int i = 0; i < 5000; i++) {
            final GUID guid1 = new GUID(GUID.newGUIDString());
            final GUID guid2 = new GUID(guid1.getGUIDBytes());

            assertEquals(guid1, guid2);
        }

        /* Test conversion from byte array -> string and back */
        for (int i = 0; i < 5000; i++) {
            final byte[] guidBytes = new byte[16];
            getSecureRandom().nextBytes(guidBytes);

            final GUID guid1 = new GUID(guidBytes);
            final GUID guid2 = new GUID(guid1.getGUIDString());

            assertEquals(guid1, guid2);
        }
    }

    private SecureRandom getSecureRandom() throws Exception {
        try {
            return SecureRandom.getInstance("IBMSecureRandom"); //$NON-NLS-1$
        } catch (final NoSuchAlgorithmException e) {

        }

        return SecureRandom.getInstance("SHA1PRNG"); //$NON-NLS-1$
    }

    private void verifyIllegalArgumentExceptionForGUID(final String guid) {
        try {
            new GUID(guid);
        } catch (final IllegalArgumentException e) {
            return;
        }

        fail("Expected an IllegalArgumentException for guid input: " + ((guid == null) ? "null" : guid)); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
