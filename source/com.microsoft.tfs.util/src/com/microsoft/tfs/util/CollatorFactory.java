// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.text.Collator;
import java.util.Locale;

/**
 * Easy factory methods for creating case-sensitive or case-insensitive
 * {@link Collator}s, which use a confusing set of "strengths" to configure
 * behavior. The methods here are documented relative to their .NET
 * counterparts.
 *
 * @threadsafety thread-safe
 */
public abstract class CollatorFactory {
    /**
     * @equivalent getCaseSensitiveCollator(Locale.getDefault())
     */
    public static Collator getCaseSensitiveCollator() {
        return getCaseSensitiveCollator(Locale.getDefault());
    }

    /**
     * Creates and returns a new {@link Collator} for the specified
     * {@link Locale} with strength set to {@link Collator#IDENTICAL}. See
     * {@link Collator} for exactly what this means; it's similar to the .NET
     * <b>StringComparison.CurrentCulture</b> (if the given locale is the
     * current culture) in that the following types of things are generally true
     * (though locales differ):
     * </p>
     * <ul>
     * <li>"a" != "b" (letter DIFFERS)</li>
     * <li>"A" != "a" (case DIFFERS)</li>
     * <li>"a" != "\u00e0" [latin small letter a with grave] (accents DIFFER)
     * </li>
     * <li>"\\u0001" != "\\u0002" (control characters DIFFER)</li>
     * </ul>
     *
     * @param locale
     *        the {@link Locale} to get the {@link Collator} for (must not be
     *        <code>null</code>)
     * @return a {@link Collator} for the given locale that considers all
     *         character differences important
     */
    public static Collator getCaseSensitiveCollator(final Locale locale) {
        Check.notNull(locale, "locale"); //$NON-NLS-1$

        final Collator c = Collator.getInstance(locale);
        c.setStrength(Collator.IDENTICAL);

        return c;
    }

    /**
     * @equivalent getCaseInsensitiveCollator(Locale.getDefault())
     */
    public static Collator getCaseInsensitiveCollator() {
        return getCaseInsensitiveCollator(Locale.getDefault());
    }

    /**
     * <p>
     * reates and returns a new {@link Collator} for the specified
     * {@link Locale} with strength set to {@link Collator#SECONDARY}. See
     * {@link Collator} for exactly what this means; it's similar to the .NET
     * <b>StringComparison.CurrentCultureCaseInsensitive</b> (if the given
     * locale is the current culture) in that the following types of things are
     * generally true (though locales differ):
     * </p>
     * <ul>
     * <li>"a" != "b" (letters DIFFER)</li>
     * <li>"A" == "a" (case IGNORED)</li>
     * <li>"a" != "\u00e0" [latin small letter a with grave] (accents DIFFER)
     * </li>
     * </ul>
     * <p>
     * However, unlike <b>StringComparison.CurrentCultureCaseInsensitive</b> (in
     * which control characters differ):
     * </p>
     * <ul>
     * <li>"\\u0001" == "\\u0002" (control characters IGNORED)</li>
     * </ul>
     * <p>
     * Control characters rarely appear in user strings.
     * </p>
     *
     * @param locale
     *        the {@link Locale} to get the {@link Collator} for (must not be
     *        <code>null</code>)
     * @return a {@link Collator} for the given locale that considers character
     *         and accent differences important, but ignores case and unicode
     *         control character differences
     */
    public static Collator getCaseInsensitiveCollator(final Locale locale) {
        Check.notNull(locale, "locale"); //$NON-NLS-1$

        final Collator c = Collator.getInstance(locale);
        c.setStrength(Collator.SECONDARY);

        return c;
    }

}
