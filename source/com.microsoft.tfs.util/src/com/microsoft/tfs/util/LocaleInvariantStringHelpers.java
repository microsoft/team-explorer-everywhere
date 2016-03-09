// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.Locale;

/**
 * Utilities for dealing with strings in a locale-invariant manner.
 *
 * @threadsafety unknown
 */
public class LocaleInvariantStringHelpers {
    private LocaleInvariantStringHelpers() {
    }

    public static boolean caseInsensitiveEquals(final String a, final String b) {
        return CollatorFactory.getCaseInsensitiveCollator().equals(a, b);
    }

    public static boolean caseInsensitiveEquals(char a, char b) {
        if (a != b) {
            a = Character.toUpperCase(a);
            b = Character.toUpperCase(b);
            if (a != b) {
                a = Character.toLowerCase(a);
                b = Character.toLowerCase(b);
                if (a != b) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Computes a case-insensitive hash code for the string that will not vary
     * by locale. For use with {@link String#CASE_INSENSITIVE_ORDER}'s
     * compare(String, String) implementation.
     *
     * @param string
     *        the string to compute the hash code for (must not be
     *        <code>null</code>)
     * @return the hash code
     */
    public static int caseInsensitiveHashCode(final String string) {
        Check.notNull(string, "string"); //$NON-NLS-1$

        int hash = 0;
        final int length = string.length();
        for (int i = 0; i < length; i++) {
            hash = 31 * hash + Character.toUpperCase(string.charAt(i));
        }

        return hash;
    }

    public static boolean caseInsensitiveStartsWith(final String string, final String substring) {
        Check.notNull(string, "string"); //$NON-NLS-1$
        Check.notNull(substring, "substring"); //$NON-NLS-1$

        if (substring.length() > string.length()) {
            return false;
        }

        final String stringEndingSegment = string.substring(0, substring.length());

        return CollatorFactory.getCaseInsensitiveCollator(Locale.ENGLISH).equals(stringEndingSegment, substring);
    }

    public static boolean caseInsensitiveEndsWith(final String string, final String substring) {
        Check.notNull(string, "string"); //$NON-NLS-1$
        Check.notNull(substring, "substring"); //$NON-NLS-1$

        if (substring.length() > string.length()) {
            return false;
        }

        final String stringEndingSegment = string.substring(string.length() - substring.length(), string.length());

        return CollatorFactory.getCaseInsensitiveCollator(Locale.ENGLISH).equals(stringEndingSegment, substring);
    }
}
