// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.htmlfilter;

import com.microsoft.tfs.util.Check;

/**
 * Static methods to filter HTML, removing unsafe tags, attributes, and link
 * types.
 *
 * Copied from Web Access.
 *
 * @threadsafety thread-compatible
 */
public class HTMLFilter {
    private static class ScanResult {
        public final int offset;
        public final String tag;

        public ScanResult(final int offset, final String tag) {
            this.offset = offset;
            this.tag = tag;
        }
    }

    /**
     * Strips disallowed tags and attributes and link types.
     *
     * @param html
     *        the HTML to process (must not be <code>null</code>)
     * @return the HTML stripped of unsafe tags, attributes, and link types
     */
    public static String strip(final String html) {
        Check.notNull(html, "html"); //$NON-NLS-1$

        final HTMLSimpleWriter writer = new HTMLSimpleWriter();
        parse(html, writer);
        return writer.toString();
    }

    // skip whitespaces,
    // returns new value of String offset i
    private static int skipWhiteSpaces(final String html, int offset) {
        final int n = html.length(); // end index

        // skip whitespaces if they are
        while (offset < n
            && (Character.isWhitespace(html.charAt(offset)) || Character.isISOControl(html.charAt(offset)))) {
            offset++;
        }

        Check.isTrue(
            offset == n || !Character.isWhitespace(html.charAt(offset)) && !Character.isISOControl(html.charAt(offset)),
            "offset == n || !Character.isWhitespace(html.charAt(offset)) && !Character.isISOControl(html.charAt(offset))"); //$NON-NLS-1$
        return offset;
    }

    // skip until terminator c1
    // returns new value of String offset i
    private static int skipUntil(final String html, int offset, final char terminatorChar) {
        final int n = html.length(); // end index

        while (offset < n && html.charAt(offset++) != terminatorChar) {
        }

        Check.isTrue(
            offset == n || html.charAt(offset - 1) == terminatorChar,
            "offset == n || html.charAt(offset - 1) == terminatorChar"); //$NON-NLS-1$
        return offset;
    }

    // skip until three character terminator c1,c2,c3
    // returns new value of String offset i
    private static int skipUntil(
        final String html,
        int offset,
        final char terminatorChar1,
        final char terminatorChar2,
        final char terminatorChar3) {
        final int n = html.length(); // end index

        while (offset + 2 < n
            && !(html.charAt(offset) == terminatorChar1
                && html.charAt(offset + 1) == terminatorChar2
                && html.charAt(offset + 2) == terminatorChar3)) {
            offset++;
        }

        if (offset + 2 < n) {
            offset += 3; // skip s[i], s[i+1], s[i+2] characters
        } else {
            offset = n; // stop at the end of string
        }

        Check.isTrue(
            offset == n
                || html.charAt(offset - 3) == terminatorChar1
                    && html.charAt(offset - 2) == terminatorChar2
                    && html.charAt(offset - 1) == terminatorChar3,
            "offset == n || html.charAt(offset - 3) == terminatorChar1 && html.charAt(offset - 2) == terminatorChar2 && html.charAt(offset - 1) == terminatorChar3"); //$NON-NLS-1$
        return offset;
    }

    // scan name into output string,
    // returns new value of String offset i
    private static ScanResult scanName(final String html, int offset) {
        final int n = html.length(); // end index

        // scan tag name
        final int i0 = offset;
        while (offset < n
            && (Character.isLetterOrDigit(html.charAt(offset))
                || html.charAt(offset) == ':'
                || html.charAt(offset) == '_')) {
            offset++;
        }

        return new ScanResult(offset, html.substring(i0, offset));
    }

    // -----------------------------------------------------------------
    // helper method to parse HTML string,
    // s - Html string,
    // writer - writer for output
    // -----------------------------------------------------------------

    // fxcop may warn about complexity of this function,
    // but it is much more efficient to leave entire parsing in the same
    // function, instead of splitting it. I recommend to leave as is.

    private static void parse(final String html, final IHTMLFilterWriter writer) {
        // iterate characters in the string
        final int n = html.length(); // end index
        int i = 0; // current index
        while (i < n) {
            // scan text until the tag
            int i0 = i;
            while (i < n && html.charAt(i) != '<') {
                i++;
            }

            // copy text to output
            if (i > i0) {
                writer.writeText(html, i0, i - i0);
                continue; // next item
            }

            Check.isTrue(i < n && html.charAt(i) == '<', "i < n && html.charAt(i) == '<'"); //$NON-NLS-1$

            // scan the tag
            i0 = i++; // mark and skip '>'

            // check for '<!' section
            if (i < n && html.charAt(i) == '!') {
                i++; // skip '!'

                // check for HTML comment syntax: '<!-- ... -->'
                if (i + 1 < n && html.charAt(i) == '-' && html.charAt(i + 1) == '-') {
                    i += 2; // skip '--'

                    i = skipUntil(html, i, '-', '-', '>');
                    continue; // next syntax element
                }

                // check for CDATA section: '<![CDATA[ ... ]]>'
                if (i + 6 < n
                    && html.charAt(i) == '['
                    && html.charAt(i + 1) == 'C'
                    && html.charAt(i + 2) == 'D'
                    && html.charAt(i + 3) == 'A'
                    && html.charAt(i + 4) == 'T'
                    && html.charAt(i + 5) == 'A'
                    && html.charAt(i + 6) == '[') {
                    i += 7; // skip '[CDATA['

                    i = skipUntil(html, i, ']', ']', '>');
                    continue; // next syntax element
                }

                // skip other DTD sections: '<! ... >'
                i = skipUntil(html, i, '>');
                continue; // next syntax element
            }

            // check for '<?' section
            if (i < n && html.charAt(i) == '?') {
                i++; // skip '?'

                i = skipUntil(html, i, '>');
                continue; // next syntax element
            }

            // check for '/' character
            boolean endTag = false;
            if (i < n && html.charAt(i) == '/') {
                endTag = true;
                i++; // skip '/'
            }

            i = skipWhiteSpaces(html, i);

            // scan tag name

            final ScanResult scanResult = scanName(html, i);
            final String tag = scanResult.tag;
            i = scanResult.offset;

            // special handling of script tag: script until </script>
            if (AllowedHTMLTags.isSpecialTag(tag)) {
                // skip until end of tag
                i = skipUntil(html, i, '>');

                while (i < n) {
                    // wait for tag start
                    i = skipUntil(html, i, '<');

                    // check for comment
                    if (i + 2 < n && html.charAt(i) == '!' && html.charAt(i + 1) == '-' && html.charAt(i + 2) == '-') {
                        i = skipUntil(html, i, '-', '-', '>');
                    }
                    // check for end tag
                    else if (i < n && html.charAt(i) == '/') {
                        i++; // skip '/'
                        i = skipWhiteSpaces(html, i);
                        String etag;

                        final ScanResult innerScanResult = scanName(html, i);
                        i = innerScanResult.offset;
                        etag = innerScanResult.tag;

                        if (AllowedHTMLTags.areTagsEqual(tag, etag)) {
                            i = skipUntil(html, i, '>');
                            break;
                        }
                    }
                    // something, skip it
                    else if (i < n) {
                        i++;
                    }
                }

                continue; // next syntax element
            }

            // skip entire tag
            if (!AllowedHTMLTags.isAllowedTag(tag)) {
                i = skipUntil(html, i, '>');
                continue;
            }

            // allowed tag: write down proceeded part
            writer.writeTag(html, i0, i - i0, tag, endTag);

            // loop attributes
            while (i < n) {
                i0 = i; // new starting point

                i = skipWhiteSpaces(html, i);

                // end of tag?
                if (i < n && html.charAt(i) == '/') {
                    i++; // skip '/'
                }

                // end tag?
                if (i < n && html.charAt(i) == '>') {
                    i++; // skip '>'
                    writer.writeEndOfTag(html, i0, i - i0, tag);
                    break;
                }

                // attribute?
                if (i < n && Character.isLetterOrDigit(html.charAt(i))) {
                    // scan tag name
                    String attr;

                    final ScanResult innerScanResult = scanName(html, i);
                    i = innerScanResult.offset;
                    attr = innerScanResult.tag;

                    i = skipWhiteSpaces(html, i);

                    int i1 = 0; // start of attribute value
                    int i2 = 0; // end of attribute value

                    // check value part
                    if (i < n && html.charAt(i) == '=') {
                        i++; // skip '='

                        i = skipWhiteSpaces(html, i);

                        i1 = i; // attribute value starts here

                        if (i < n && (html.charAt(i) == '\'' || html.charAt(i) == '"')) {
                            final char term = html.charAt(i++); // save and skip
                            // terminator
                            i1 = i; // the attribute starts just here

                            // skip until end terminator or end tag
                            while (i < n && html.charAt(i) != '>' && html.charAt(i) != term) {
                                i++;
                            }

                            i2 = i; // attrubute ends here

                            // skip terminator
                            if (i < n && html.charAt(i) == term) {
                                i++;
                            }
                        } else {
                            // skip while not whitespace or end
                            while (i < n
                                && html.charAt(i) != '>'
                                && !Character.isWhitespace(html.charAt(i))
                                && !Character.isISOControl(html.charAt(i))) {
                                i++;
                            }

                            i2 = i; // attribute ends here
                        }

                        // ignore "javascript:..." type of attributes

                        if (i >= i1 + 11 && AllowedHTMLTags.areTagsEqual(html.substring(i1, i1 + 11), "javascript:")) //$NON-NLS-1$
                        {
                            attr = ""; // prohibited //$NON-NLS-1$
                        }
                    }

                    // and of attribute: check it
                    if (AllowedHTMLTags.isAllowedAttribute(tag, attr)) {
                        writer.writeAttribute(html, i0, i - i0, tag, attr, i1, i2);
                    }

                    continue; // next attribute
                }

                // unknown character - skip it
                if (i < n && html.charAt(i) != '>') {
                    i++;
                }
            }
        }

        // end of loop: i==n
        Check.isTrue(i == n, "i == n"); //$NON-NLS-1$
    }
}
