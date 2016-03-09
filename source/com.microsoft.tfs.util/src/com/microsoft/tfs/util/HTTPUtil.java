// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

public class HTTPUtil {
    /*
     * code originally from http://www.rgagnon.com/javadetails/java-0306.html,
     * and then modified a bit
     */
    public static String escapeHTMLCharacters(final String string) {
        final StringBuffer sb = new StringBuffer(string.length());
        // true if last char was blank
        boolean lastWasBlankChar = false;
        final int len = string.length();
        char c;

        for (int i = 0; i < len; i++) {
            c = string.charAt(i);
            if (c == ' ') {
                // blank gets extra work,
                // this solves the problem you get if you replace all
                // blanks with &nbsp;, if you do that you loss
                // word breaking
                if (lastWasBlankChar) {
                    lastWasBlankChar = false;
                    sb.append("&nbsp;"); //$NON-NLS-1$
                } else {
                    lastWasBlankChar = true;
                    sb.append(' ');
                }
            } else {
                lastWasBlankChar = false;
                //
                // HTML Special Chars
                if (c == '"') {
                    sb.append("&quot;"); //$NON-NLS-1$
                } else if (c == '&') {
                    sb.append("&amp;"); //$NON-NLS-1$
                } else if (c == '<') {
                    sb.append("&lt;"); //$NON-NLS-1$
                } else if (c == '>') {
                    sb.append("&gt;"); //$NON-NLS-1$
                } else if (c == '\n') {
                    // Handle Newline
                    sb.append("<br/>"); //$NON-NLS-1$
                } else {
                    final int ci = 0xffff & c;
                    if (ci < 160) {
                        // nothing special only 7 Bit
                        sb.append(c);
                    } else {
                        // Not 7 Bit use the unicode system
                        sb.append("&#"); //$NON-NLS-1$
                        sb.append(new Integer(ci).toString());
                        sb.append(';');
                    }
                }
            }
        }
        return sb.toString();
    }
}
