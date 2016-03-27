// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.email;

import com.microsoft.tfs.util.StringUtil;

import junit.framework.TestCase;

public class EmailAddressParserTest extends TestCase {
    final EmailAddressParser parser = new EmailAddressParser();

    public void testEmailAddressGoodSimple() {
        final boolean result = parser.parse("JohnDoe@live.com"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodWithHyphens() {
        final boolean result = parser.parse("git-tf-test1@ex-wife.com"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodWithDots() {
        final boolean result = parser.parse("Jane.Roe@live.com"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodSpaces() {
        final boolean result = parser.parse("\"Mark Twain\"@live.com"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodQuotedSpaces() {
        final boolean result = parser.parse("\"Mark\\ Twain\"@live.com"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodAt() {
        final boolean result = parser.parse("\"William Shakespeare\\@home\"@live.com"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressBadNoAt() {
        final boolean result = parser.parse("live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadEndsWithAt() {
        final boolean result = parser.parse("live.com@"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadStartsWithAt() {
        final boolean result = parser.parse("@live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadMultipleAts() {
        final boolean result = parser.parse("John@Doe@live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadStartsWithDot() {
        final boolean result = parser.parse(".John.Doe@live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadEndsWithDot() {
        final boolean result = parser.parse("John.Doe.@live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadDomainStartsWithDot() {
        final boolean result = parser.parse("John.Doe@.live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadDomainEndsWithDot() {
        final boolean result = parser.parse("John.Doe@live.com."); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadQuotePairInDotAtom() {
        final boolean result = parser.parse("John\\.Doe@live.com."); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadQuotesAtInDotAtom() {
        final boolean result = parser.parse("John\\@Doe@live.com."); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadMultipleQuoteStrings() {
        final boolean result = parser.parse("\"John\\@Doe\"and\"Jain\\@Roe\"@live.com."); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadUnquotedinQuoteAt() {
        final boolean result = parser.parse("\"Mark @ Twain\"@live.com"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());
    }
}
