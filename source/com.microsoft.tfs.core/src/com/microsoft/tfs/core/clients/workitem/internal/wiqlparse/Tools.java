// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime.UncheckedParseException;
import com.microsoft.tfs.util.GUID;

public class Tools {
    public static void ensureSyntax(final boolean condition, final SyntaxError syntaxError, final Node node) {
        if (!condition) {
            throw new SyntaxException(node, syntaxError);
        }
    }

    public static void AppendName(final StringBuffer b, String s) {
        boolean flag = (s.length() == 0) || !Character.isLetter(s.charAt(0));
        final int length = s.length();
        for (int i = 0; (i < length) && !flag; i++) {
            if (!Character.isLetterOrDigit(s.charAt(i))) {
                flag = true;
            }
        }
        if (flag) {
            s = "[" + s + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        b.append(s);
    }

    public static void AppendString(final StringBuffer sb, final String s) {
        final String[] parts = s.split("\\'"); //$NON-NLS-1$
        sb.append("'"); //$NON-NLS-1$
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append("''"); //$NON-NLS-1$
            }
        }
        sb.append("'"); //$NON-NLS-1$
    }

    public static String formatString(final String s) {
        /*
         * The idea here is to return the input with single-quotes around it.
         * Any single-quotes contained in the input need to be replaced by
         * repeated single-quotes.
         */

        final String[] parts = s.split("\\'"); //$NON-NLS-1$
        final StringBuffer sb = new StringBuffer("'"); //$NON-NLS-1$
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append("''"); //$NON-NLS-1$
            }
        }
        sb.append("'"); //$NON-NLS-1$
        return sb.toString();
    }

    public static String formatName(String name) {
        /*
         * If the input is the empty string, does not start with a letter, or
         * contains characters that are not digits or letters, we return it
         * surrounded by square brackets. Otherwise we return it as-is.
         */

        boolean needToEscapeWithBrackets = (name.length() == 0 || !Character.isLetter(name.charAt(0)));

        for (int i = 0; i < name.length() && !needToEscapeWithBrackets; i++) {
            if (!Character.isLetterOrDigit(name.charAt(i))) {
                needToEscapeWithBrackets = true;
            }
        }
        if (needToEscapeWithBrackets) {
            name = "[" + name + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return name;
    }

    public static boolean IsGUIDString(final String s) {
        try {
            new GUID(s);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isNumericString(final String s) {
        final List<Node> list = Parser.parseLexems(s);
        if (list.size() == 1) {
            return ((NodeItem) list.get(0)).getNodeType() == NodeType.NUMBER;
        }
        return false;
    }

    public static boolean isDateString(final String s, final Locale locale) {
        try {
            DateTime.parse(s, locale, TimeZone.getDefault());
            return true;
        } catch (final UncheckedParseException ex) {
            return false;
        }
    }

    public static Boolean TranslateBoolToken(String val) {
        val = val.toLowerCase();
        if (val.equals("true")) //$NON-NLS-1$
        {
            return Boolean.TRUE;
        }
        if (val.equals("false")) //$NON-NLS-1$
        {
            return Boolean.FALSE;
        }
        return null;
    }

}
