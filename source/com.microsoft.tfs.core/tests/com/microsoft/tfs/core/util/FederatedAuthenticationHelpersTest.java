// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import junit.framework.TestCase;

public class FederatedAuthenticationHelpersTest extends TestCase {
    public void testGetDetailMessage() {
        // A real one:
        assertEquals(
            "ACS50012: Authentication failed. ", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage(
                "Error:Code:403:SubCode:T0:Detail:ACS50012: Authentication failed. :TraceID:556a1fc6-3f3f-46f4-88ec-f8e865e6eca4:TimeStamp:2011-04-13 21:17:28Z")); //$NON-NLS-1$

        // Detail at the end, one message part
        assertEquals(
            "a", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail:a")); //$NON-NLS-1$

        // Detail at the end, one message part (but whitespace)
        assertEquals(
            " ", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail: ")); //$NON-NLS-1$

        // Detail at the end, two message parts
        assertEquals(
            "ACS00001:b", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail:ACS00001:b")); //$NON-NLS-1$

        // Detail in middle, two message parts
        assertEquals(
            "ACS00001:b", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail:ACS00001:b:x:y")); //$NON-NLS-1$

        // Detail in middle, two message parts (but whitespace)
        assertEquals(
            "ACS00001: ", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail:ACS00001: :x:y")); //$NON-NLS-1$

        // Detail at the beginning, two message parts
        assertEquals(
            "ACS00001:b", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("Detail:ACS00001:b:x:y")); //$NON-NLS-1$

        // Detail at the beginning and end, one message part
        assertEquals(
            "ACS00001", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("Detail:ACS00001")); //$NON-NLS-1$

        // Detail at the beginning and end, two message parts
        assertEquals(
            "ACS00001:b", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("Detail:ACS00001:b")); //$NON-NLS-1$

        // Different case
        assertEquals(
            "ACS00001:b", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:ACS00001:b:x:y")); //$NON-NLS-1$

        // Not quite a match for ACSnnnnn (won't get "b" part)

        // Not enough digits
        assertEquals(
            "ACS0000", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:ACS0000:b:x:y")); //$NON-NLS-1$

        // Not all digits
        assertEquals(
            "ACS0000X", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:ACS0000X:b:x:y")); //$NON-NLS-1$

        // Wrong case
        assertEquals(
            "Acs00000", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:Acs00000:b:x:y")); //$NON-NLS-1$

        // Empty message parts

        // Newlines in parts. So long as colons surround the "detail" key, it
        // should work.
        assertEquals(
            "a\n", //$NON-NLS-1$
            FederatedAuthenticationHelpers.getDetailMessage("x\n\n\n:y:Detail:a\n:b:x:y\n\nblah")); //$NON-NLS-1$

        // No detail
        assertNull(FederatedAuthenticationHelpers.getDetailMessage("x:y:Monkeys:a:b:x:y")); //$NON-NLS-1$
        assertNull(FederatedAuthenticationHelpers.getDetailMessage("")); //$NON-NLS-1$
        assertNull(FederatedAuthenticationHelpers.getDetailMessage(":x:y:\nDetail:a:b:x:y")); //$NON-NLS-1$
        assertNull(FederatedAuthenticationHelpers.getDetailMessage(":x:y:Detail\n:a:b:x:y")); //$NON-NLS-1$
        assertNull(FederatedAuthenticationHelpers.getDetailMessage("   abc  xyz   ")); //$NON-NLS-1$
        assertNull(FederatedAuthenticationHelpers.getDetailMessage(":::::::")); //$NON-NLS-1$

        // Missing messages after detail

        final String missing = "(Detail field present but message missing)"; //$NON-NLS-1$

        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:Detail:")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("Detail:")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("Detail")); //$NON-NLS-1$

        // Has separators for message, but Java tokenizer eats empty tokens, so
        // should see fail message instead

        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL::")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:::")); //$NON-NLS-1$
        assertEquals(missing, FederatedAuthenticationHelpers.getDetailMessage("x:y:DETAIL:::::")); //$NON-NLS-1$
    }
}
