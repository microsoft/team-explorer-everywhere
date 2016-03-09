// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for interacting with OM / Work Item Query Language
 */
public abstract class WIQLHelpers {
    private static final String SINGLE_QUOTE = "'"; //$NON-NLS-1$
    private static final String TWO_SINGLE_QUOTES = "''"; //$NON-NLS-1$

    // Take care to escape these strings for MessageFormat (like doubling up
    // single quotes).
    private static final String SINGLE_QUOTED_STRING_FORMAT = "''{0}''"; //$NON-NLS-1$
    private static final String DOUBLE_QUOTED_STRING_FORMAT = "\"{0}\""; //$NON-NLS-1$
    private static final String BRACKETED_STRING_FORMAT = "[{0}]"; //$NON-NLS-1$

    public static Map<String, String> getParameterDictionary(final String projectName) {
        final Map<String, String> dict = new HashMap<String, String>();
        dict.put(WIQLOperators.PROJECT_CONTEXT_KEY, projectName);
        return dict;
    }

    /**
     * Put the specified String in quotes, escaping internal single-quotes.
     */
    public static String getEscapedSingleQuotedValue(final String value) {
        return getSingleQuotedValue(value.replace(SINGLE_QUOTE, TWO_SINGLE_QUOTES));
    }

    /**
     * Encloses the value in single quotations.
     */
    public static String getSingleQuotedValue(final String value) {
        return MessageFormat.format(SINGLE_QUOTED_STRING_FORMAT, value);
    }

    /**
     * Encloses the value in double quotations.
     */
    public static String getDoubleQuotedValue(final String value) {
        return MessageFormat.format(DOUBLE_QUOTED_STRING_FORMAT, value);
    }

    /**
     * Encloses the value in square brackets.
     */
    public static String getEnclosedField(final String value) {
        return MessageFormat.format(BRACKETED_STRING_FORMAT, value);
    }
}