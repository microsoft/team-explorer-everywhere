// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

public class WIStringTypeConverter implements WITypeConverter {
    private final boolean isLargeText;
    private final boolean trimOnTranslate;

    /**
     * @param isLargeText
     *        pass <code>true</code> if the field type is for "large" text
     *        (conversion should permit line breaks and extra whitespace),
     *        <code>false</code> if this is for a non-large text (no line breaks
     *        permitted)
     * @param trimOnTranslate
     *        if <code>true</code> the input value is trimmed via
     *        {@link String#trim()} during
     *        {@link #translate(Object, WIValueSource)}, if <code>false</code>
     *        the value is not trimmed.
     */
    public WIStringTypeConverter(final boolean isLargeText, final boolean trimOnTranslate) {
        this.isLargeText = isLargeText;
        this.trimOnTranslate = trimOnTranslate;
    }

    @Override
    public Object translate(final Object data, final WIValueSource valueSource) {
        String translatedValue = null;
        if (data != null) {
            if (data instanceof String) {
                translatedValue = ((String) data);

                if (trimOnTranslate) {
                    translatedValue = translatedValue.trim();
                }

                if (translatedValue.length() == 0) {
                    translatedValue = null;
                }

            } else {
                translatedValue = String.valueOf(data);
            }
        }

        if (translatedValue != null && isValidFieldValueString(translatedValue, isLargeText) == false) {
            throw new WITypeConverterException(
                "translated value string contains invalid characters (likely control, whitespace, or invalid surrogate pairs)", //$NON-NLS-1$
                FieldStatus.INVALID_CHARACTERS,
                translatedValue);
        }

        return translatedValue;
    }

    @Override
    public String toString(final Object data) {
        return (String) data;
    }

    /**
     * Tests whether the string contains invalid values for a work item string
     * field. Logic is copied from Visual Studio Dev10 OMutils class.
     */
    private static boolean isValidFieldValueString(final String s, final boolean isLongText) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);

            if (Character.isISOControl(c) && (!isLongText || c != '\r' && c != '\n' && c != '\t')) {
                return false; // unexpected control char
            }
            if (Character.isLowSurrogate(c)) {
                return false; // unexpected low surrogate
            } else if (Character.isHighSurrogate(c)) {
                if (++i == s.length()) {
                    return false; // unexpected EOS
                }
                if (!Character.isLowSurrogate(s.charAt(i))) {
                    return false; // low surrogate should follow high surrogate
                }
            }
        }

        return true;
    }
}
