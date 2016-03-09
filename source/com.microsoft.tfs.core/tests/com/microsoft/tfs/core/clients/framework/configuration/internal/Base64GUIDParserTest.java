// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import static org.junit.Assert.assertArrayEquals;

import java.text.MessageFormat;

import com.microsoft.tfs.util.GUID;

import junit.framework.TestCase;

public class Base64GUIDParserTest extends TestCase {
    public void testBasicGUID() {
        assertEquals(
            new GUID("93857a5f-c339-433b-9c4b-8bf83482bd69"), //$NON-NLS-1$
            Base64GUIDParser.getGUIDFromBase64("X3qFkznDO0OcS4v4NIK9aQ==")); //$NON-NLS-1$
        assertEquals(
            new GUID("b5c1da35-1b61-4ada-942b-4369ea1f9f1c"), //$NON-NLS-1$
            Base64GUIDParser.getGUIDFromBase64("NdrBtWEb2kqUK0Np6h+fHA==")); //$NON-NLS-1$
        assertEquals(
            new GUID("fc3b564f-86a1-4e69-ba2b-1780c19e4639"), //$NON-NLS-1$
            Base64GUIDParser.getGUIDFromBase64("T1Y7/KGGaU66KxeAwZ5GOQ==")); //$NON-NLS-1$
        assertEquals(
            new GUID("8f650e65-bf7a-418b-8e22-7c9f2c8cdd15"), //$NON-NLS-1$
            Base64GUIDParser.getGUIDFromBase64("ZQ5lj3q/i0GOInyfLIzdFQ==")); //$NON-NLS-1$
    }

    public void testBasicGUIDPaths() {
        assertArrayEquals(new GUID[0], Base64GUIDParser.getGUIDPathFromBase64("")); //$NON-NLS-1$

        assertArrayEquals(new GUID[] {
            new GUID("93857a5f-c339-433b-9c4b-8bf83482bd69") //$NON-NLS-1$
        }, Base64GUIDParser.getGUIDPathFromBase64("X3qFkznDO0OcS4v4NIK9aQ==")); //$NON-NLS-1$

        assertArrayEquals(
            new GUID[] {
                new GUID("eea096fb-93c8-4e67-998d-a36f51d252c2"), //$NON-NLS-1$
                new GUID("e96c0462-69bb-43d9-b12e-04d072049be5"), //$NON-NLS-1$
                new GUID("905e60df-c9fd-434e-b450-a3fc6bc9d0b2"), //$NON-NLS-1$
                new GUID("2d373bae-e45c-4a03-8ec5-3d5a370cff2f"), //$NON-NLS-1$
        },
            Base64GUIDParser.getGUIDPathFromBase64(
                "+5ag7siTZ06ZjaNvUdJSwg==YgRs6btp2UOxLgTQcgSb5Q==32BekP3JTkO0UKP8a8nQsg==rjs3LVzkA0qOxT1aNwz/Lw==")); //$NON-NLS-1$
    }

    public void testTruncation() throws Exception {
        testGUIDFailure(""); //$NON-NLS-1$
        testGUIDFailure("+5ag7siTZ06Z"); //$NON-NLS-1$

        testGUIDPathFailure("+5ag7siTZ06ZjaNvUdJSwg==YgRs6btp2UOx"); //$NON-NLS-1$
        testGUIDPathFailure("+5ag7siTZ06ZjaNvUdJSwg==YgRs6btp2UOxLgTQcgSb5Q==32BekP3JTkO0UKP8a8nQsg="); //$NON-NLS-1$
    }

    private void testGUIDFailure(final String damagedInput) throws Exception {
        boolean parseFailure = false;

        try {
            Base64GUIDParser.getGUIDFromBase64(damagedInput);
        } catch (final IllegalArgumentException e) {
            parseFailure = true;
        }

        if (!parseFailure) {
            throw new Exception(
                MessageFormat.format("The input {0} was parsed - should have been invalid input", damagedInput)); //$NON-NLS-1$
        }
    }

    private void testGUIDPathFailure(final String damagedInput) throws Exception {
        boolean parseFailure = false;

        try {
            Base64GUIDParser.getGUIDPathFromBase64(damagedInput);
        } catch (final IllegalArgumentException e) {
            parseFailure = true;
        }

        if (!parseFailure) {
            throw new Exception(
                MessageFormat.format("The input {0} was parsed - should have been invalid input", damagedInput)); //$NON-NLS-1$
        }
    }
}
