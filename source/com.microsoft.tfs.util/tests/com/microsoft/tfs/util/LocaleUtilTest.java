// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.Locale;

import junit.framework.TestCase;

public class LocaleUtilTest extends TestCase {
    public void testlocaleToRFC5646LanguageTag() {
        // Bad arguments

        try {
            LocaleUtil.localeToRFC5646LanguageTag(null);
            assertFalse("can't take null Locale", true); //$NON-NLS-1$
        } catch (final NullPointerException e) {
        }

        try {
            LocaleUtil.localeToRFC5646LanguageTag(new Locale("")); //$NON-NLS-1$
            assertFalse("can't take empty language", true); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }

        // Normal stuff
        assertEquals("en", LocaleUtil.localeToRFC5646LanguageTag(Locale.ENGLISH)); //$NON-NLS-1$
        assertEquals("zh-CN", LocaleUtil.localeToRFC5646LanguageTag(Locale.SIMPLIFIED_CHINESE)); //$NON-NLS-1$
        assertEquals("zh-Hans-CN", LocaleUtil.localeToRFC5646LanguageTag(new Locale("zh", "CN", "Hans"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        // language
        assertEquals("a", LocaleUtil.localeToRFC5646LanguageTag(new Locale("a"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("ab", LocaleUtil.localeToRFC5646LanguageTag(new Locale("ab"))); //$NON-NLS-1$ //$NON-NLS-2$

        // Language and variant
        assertEquals("a-b", LocaleUtil.localeToRFC5646LanguageTag(new Locale("a", "", "b"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals("ab-cd", LocaleUtil.localeToRFC5646LanguageTag(new Locale("ab", "", "cd"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        // Language and region (Locale upper-cases regions internally)
        assertEquals("a-B", LocaleUtil.localeToRFC5646LanguageTag(new Locale("a", "b"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertEquals("ab-CD", LocaleUtil.localeToRFC5646LanguageTag(new Locale("ab", "cd"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        // Language and variant and region (Locale upper-cases regions
        // internally)
        assertEquals("a-c-B", LocaleUtil.localeToRFC5646LanguageTag(new Locale("a", "b", "c"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        assertEquals("ab-ef-CD", LocaleUtil.localeToRFC5646LanguageTag(new Locale("ab", "cd", "ef"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        // Multiple intermediate tags shoehorned in via variant (RFC allows an
        // unlimited number of these)
        assertEquals("en-x-Y-z-US", LocaleUtil.localeToRFC5646LanguageTag(new Locale("en", "US", "x-Y-z"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    }
}
