// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.Locale;

import junit.framework.TestCase;

/**
 *
 *
 * @threadsafety unknown
 */
public class LocaleInvariantStringHelpersTest extends TestCase {
    public void testCaseInsensitiveEqualsChars() {
        // ASCII
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('a', 'a'));
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('b', 'B'));
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('C', 'c'));
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('1', '1'));
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('\u905b', '\u905b'));

        assertFalse(LocaleInvariantStringHelpers.caseInsensitiveEquals('a', 'x'));
        assertFalse(LocaleInvariantStringHelpers.caseInsensitiveEquals('a', '1'));

        // Greek
        assertTrue(
            "Greek upper alpha vs. lower alpha", //$NON-NLS-1$
            LocaleInvariantStringHelpers.caseInsensitiveEquals('\u0391', '\u03b1'));

        /*
         * Turksish
         *
         * Dotted and dotless i are two separate characters, but Java's
         * collation and comparison rules squash them into one char.
         */

        // Dotless i
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('ı', 'I'));
        // Dotted i
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('i', 'İ'));
        // Dotless and dotted squashed
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('ı', 'İ'));
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEquals('i', 'I'));
    }

    public void testCaseInsensitiveStartsWith() {
        final Locale backupLocale = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR")); //$NON-NLS-1$ //$NON-NLS-2$

        /* Control: Some turkısh İ dilemmas. */
        assertFalse("WWW.MICROSOFT.*".toLowerCase().startsWith("www.microsoft.".toLowerCase())); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse("www.microsoft.*".toUpperCase().startsWith("WWW.MICROSOFT.".toUpperCase())); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveStartsWith("www.google.*", "www.google.")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveStartsWith("www.GOOGLE.*", "WWW.google.")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveStartsWith("WWW.MICROSOFT.*", "www.microsoft.")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(LocaleInvariantStringHelpers.caseInsensitiveStartsWith("tooshor", "tooshort")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(LocaleInvariantStringHelpers.caseInsensitiveStartsWith("toolong", "oolong")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveStartsWith("justright", "justright")); //$NON-NLS-1$ //$NON-NLS-2$

        Locale.setDefault(backupLocale);
    }

    public void testCaseInsensitiveEndsWith() {
        final Locale backupLocale = Locale.getDefault();
        Locale.setDefault(new Locale("tr", "TR")); //$NON-NLS-1$ //$NON-NLS-2$

        /* Control: Some turkısh İ dilemmas. */
        assertFalse("*.MICROSOFT.COM".toLowerCase().endsWith(".microsoft.com".toLowerCase())); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse("*.microsoft.com".toUpperCase().endsWith(".MICROSOFT.COM".toUpperCase())); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEndsWith("*.google.com", ".google.com")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEndsWith("*.GOOGLE.com", ".google.COM")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEndsWith("*.MICROSOFT.COM", ".microsoft.com")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(LocaleInvariantStringHelpers.caseInsensitiveEndsWith("tooshort", "tooshor")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(LocaleInvariantStringHelpers.caseInsensitiveEndsWith("oolong", "toolong")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(LocaleInvariantStringHelpers.caseInsensitiveEndsWith("justright", "justright")); //$NON-NLS-1$ //$NON-NLS-2$

        Locale.setDefault(backupLocale);
    }
}
