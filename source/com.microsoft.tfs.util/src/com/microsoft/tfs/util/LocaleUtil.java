// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.Locale;

public class LocaleUtil {
    /**
     * Useful constant for the root locale. The root locale is the locale whose
     * language, country, and variant are empty ("") strings. This is regarded
     * as the base locale of all locales, and is used as the language/country
     * neutral locale for the locale sensitive operations.
     *
     * This is akin to {@link Locale#ROOT}, which exists only in Java 1.6 and
     * newer.
     */
    public static final Locale ROOT = new Locale("", "", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    /**
     * <p>
     * Converts a {@link Locale} to an RFC 5646 language tag. These generally
     * look like language[-variant][-region]: "en", "en-US", "zh-Hans" (Chinese,
     * Simplified, no region) , "zh-yue-HK", (Chinese, Cantonese, as used in
     * Hong Kong SAR).
     * </p>
     * <p>
     * The first part is usually an ISO 639 language code. If there is a
     * language variant it goes next (the RFC specifies lots of rules for this
     * part). If there is a region specification, a ISO 3166-1 region code goes
     * next. The RFC permits an unlimited number of other tags, but this
     * implementation is simple and creates tags with at most three.
     * </p>
     * <p>
     * This method does very little validation of the {@link Locale}'s fields
     * when building the language tag. It does not verify the lengths of the
     * parts (except that the language part is not empty), whether the
     * characters are permitted (alphabetic, alpha-numeric), or that parts are
     * valid ISO codes. All pre-defined Java {@link Locale}s will make valid
     * language tags beacuse they use ISO language and region codes. Watch out
     * for invalid characters in variants in custom {@link Locale}s!
     * </p>
     *
     * @param locale
     *        a {@link Locale} whose parts are valid language, region, and
     *        variant tags (must not be <code>null</code>)
     * @return an RFC 5646 language tag, never <code>null</code>
     * @throws IllegalArgumentException
     *         if the given {@link Locale}'s language is empty
     *
     * @see http://en.wikipedia.org/wiki/IETF_language_tag
     * @see http://tools.ietf.org/rfc/rfc5646.txt (Tags for the Identification
     *      of Languages)
     * @see http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.10 (HTTP
     *      1.1, Language Tags)
     */
    public static final String localeToRFC5646LanguageTag(final Locale locale) throws IllegalArgumentException {
        Check.notNull(locale, "locale"); //$NON-NLS-1$
        Check.notNullOrEmpty(locale.getLanguage(), "locale.getLanguage()"); //$NON-NLS-1$

        // language[-variant][-region]

        String result = locale.getLanguage();

        if (locale.getVariant().length() > 0) {
            result = result + "-" + locale.getVariant(); //$NON-NLS-1$
        }

        if (locale.getCountry().length() > 0) {
            result = result + "-" + locale.getCountry(); //$NON-NLS-1$
        }

        return result;
    }
}
