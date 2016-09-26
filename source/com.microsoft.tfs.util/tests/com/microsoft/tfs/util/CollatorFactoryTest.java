// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.text.Collator;
import java.util.Locale;

import junit.framework.TestCase;

public class CollatorFactoryTest extends TestCase {
    public void testGetCaseSensitiveCollatorEnglish() {
        final Collator csCollator = CollatorFactory.getCaseSensitiveCollator(Locale.ENGLISH);

        // Identical
        assertTrue(csCollator.equals("a", "a")); //$NON-NLS-1$//$NON-NLS-2$

        // Accent different
        assertFalse(csCollator.equals("a", "à")); //$NON-NLS-1$//$NON-NLS-2$
        // Case different
        assertFalse(csCollator.equals("a", "A")); //$NON-NLS-1$//$NON-NLS-2$
    }

    public void testGetCaseInsensitiveCollatorEnglish() {
        final Collator ciCollator = CollatorFactory.getCaseInsensitiveCollator(Locale.ENGLISH);

        // Identical
        assertTrue(ciCollator.equals("a", "a")); //$NON-NLS-1$//$NON-NLS-2$
        // Case different
        assertTrue(ciCollator.equals("a", "A")); //$NON-NLS-1$//$NON-NLS-2$

        // Accent different
        assertFalse(ciCollator.equals("a", "à")); //$NON-NLS-1$//$NON-NLS-2$
    }

    public void testGetCaseSensitiveCollatorTurkish() {
        final Collator csCollator = CollatorFactory.getCaseSensitiveCollator(new Locale("tr", "TR")); //$NON-NLS-1$ //$NON-NLS-2$

        // Identical (dotless i)
        assertTrue(csCollator.equals("ı", "ı")); //$NON-NLS-1$//$NON-NLS-2$
        // Identical (dotted i)
        assertTrue(csCollator.equals("i", "i")); //$NON-NLS-1$//$NON-NLS-2$

        // Case different (dotless lower and cap)
        assertFalse(csCollator.equals("ı", "I")); //$NON-NLS-1$//$NON-NLS-2$
        // Case different (dotted lower and cap)
        assertFalse(csCollator.equals("i", "İ")); //$NON-NLS-1$//$NON-NLS-2$
        // Different letters (dotted lower and dotless lower)
        assertFalse(csCollator.equals("i", "ı")); //$NON-NLS-1$//$NON-NLS-2$
        // Different letters, case different (dotted lower and dotless cap)
        assertFalse(csCollator.equals("i", "I")); //$NON-NLS-1$//$NON-NLS-2$
        // Different letters, case different (dotted cap and dotless lower)
        assertFalse(csCollator.equals("İ", "ı")); //$NON-NLS-1$//$NON-NLS-2$
    }

    public void testGetCaseInsensitiveCollatorTurkish() {
        final Collator ciCollator = CollatorFactory.getCaseInsensitiveCollator(new Locale("tr", "TR")); //$NON-NLS-1$ //$NON-NLS-2$

        /*
         * A case insensitive collator in Turkish treats dotted i and dotless i
         * equal, even though they are different letters (like a and b in
         * English). This enables some compat cases with English text in Turkish
         * locales like having lower-case "title" (dotted i) match the
         * upper-case "TITLE" (dotless i).
         */

        // Identical (dotless i)
        assertTrue(ciCollator.equals("ı", "ı")); //$NON-NLS-1$//$NON-NLS-2$
        // Identical (dotted i)
        assertTrue(ciCollator.equals("i", "i")); //$NON-NLS-1$//$NON-NLS-2$
        // Case different (dotless lower and cap)
        assertTrue(ciCollator.equals("ı", "I")); //$NON-NLS-1$//$NON-NLS-2$
        // Case different (dotted lower and cap)
        assertTrue(ciCollator.equals("i", "İ")); //$NON-NLS-1$//$NON-NLS-2$

        // Doesn't test collator but confirms that Java's String considers
        // dotless i and dotted i equivalent when case is ignored (also works in
        // English locale).
        final Locale oldLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR")); //$NON-NLS-1$//$NON-NLS-2$

            assertFalse("i".equals("ı")); //$NON-NLS-1$//$NON-NLS-2$
            assertFalse("İ".equals("I")); //$NON-NLS-1$//$NON-NLS-2$
            assertFalse("i".equals("I")); //$NON-NLS-1$ //$NON-NLS-2$
            assertFalse("İ".equals("ı")); //$NON-NLS-1$ //$NON-NLS-2$

            assertTrue("i".equalsIgnoreCase("ı")); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue("İ".equalsIgnoreCase("I")); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue("i".equalsIgnoreCase("I")); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue("İ".equalsIgnoreCase("ı")); //$NON-NLS-1$ //$NON-NLS-2$
        } finally {
            Locale.setDefault(oldLocale);
        }
    }
}
