// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.email;

import com.microsoft.tfs.util.StringUtil;

import junit.framework.TestCase;

public class EmailAddressParserTest extends TestCase {
    final EmailAddressParser parser = new EmailAddressParser();

    public void testEmailAddressGoodSimple() {
        final boolean result = parser.parse("JohnDoe@live.test"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodWithHyphens() {
        final boolean result = parser.parse("git-tf-test1@ex-wife.test"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodWithDots() {
        final boolean result = parser.parse("Jane.Roe@live.test"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodSpaces() {
        final boolean result = parser.parse("\"Mark Twain\"@live.test"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodQuotedSpaces() {
        final boolean result = parser.parse("\"Mark\\ Twain\"@live.test"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressGoodAt() {
        final boolean result = parser.parse("\"William Shakespeare\\@home\"@live.test"); //$NON-NLS-1$

        assertEquals(true, result);
        assertNull(parser.getErrorMessage());
    }

    public void testEmailAddressBadTooLong() {
        final boolean result = parser.parse(new String(new char[250]).replace('\0', 'a') + "@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadSubdomainTooLong() {
        final boolean result = parser.parse(
            "a@" + new String(new char[64]).replace('\0', 'a') + "." + new String(new char[64]).replace('\0', 'b')); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadTooManySubdomains() {
        final boolean result = parser.parse("a@a" + new String(new char[127]).replace("\0", ".a")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBaDomainTooLong() {
        final boolean result = parser.parse("a@" + new String(new char[254]).replace('\0', 'a')); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadNoAt() {
        final boolean result = parser.parse("live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadEndsWithAt() {
        final boolean result = parser.parse("live.test@"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadStartsWithAt() {
        final boolean result = parser.parse("@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadMultipleAts() {
        final boolean result = parser.parse("John@Doe@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadStartsWithDot() {
        final boolean result = parser.parse(".John.Doe@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadEndsWithDot() {
        final boolean result = parser.parse("John.Doe.@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadDomainStartsWithDot() {
        final boolean result = parser.parse("John.Doe@.live.invalid"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadDomainEndsWithDot() {
        final boolean result = parser.parse("John.Doe@live.invalid."); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadDomainInvalidChars() {
        final boolean result = parser.parse("John.Doe@live.inva[lid"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadDomainWithMulipleDots() {
        final boolean result = parser.parse("John.Doe@live..invalid"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadSubdomainStartsWithHyphen() {
        final boolean result = parser.parse("John.Doe@live.-invalid"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadSubdomainEndsWithHyphen() {
        final boolean result = parser.parse("John.Doe@live-.invalid"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadLocalPartWithMulipleDots() {
        final boolean result = parser.parse("John..Doe@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadQuotePairInDotAtom() {
        final boolean result = parser.parse("John\\.Doe@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadQuotesAtInDotAtom() {
        final boolean result = parser.parse("John\\@Doe@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadMultipleQuoteStrings() {
        final boolean result = parser.parse("\"John\\@Doe\"and\"Jain\\@Roe\"@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }

    public void testEmailAddressBadUnquotedinQuoteAt() {
        final boolean result = parser.parse("\"Mark @ Twain\"@live.test"); //$NON-NLS-1$

        assertEquals(false, result);
        assertNotNull(parser.getErrorMessage());

        final String actualMessage = parser.getErrorMessage();
        assertFalse(StringUtil.isNullOrEmpty(actualMessage));
    }
}
