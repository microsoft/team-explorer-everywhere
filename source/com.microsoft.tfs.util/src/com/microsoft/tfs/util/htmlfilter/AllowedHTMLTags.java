// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.htmlfilter;

import java.util.Map;
import java.util.TreeMap;

/**
 * Contains static methods to query whether certain HTML tags and attributes are
 * allowed.
 *
 * Copied from Web Access.
 *
 * @threadsafety thread-compatible
 */
class AllowedHTMLTags {
    // structure is used inside Map
    private static class TagInfo {
        protected int m_eolBefore;
        protected int m_eolAfter;
        protected Map<String, Integer> m_attributes;

        public TagInfo() {
        }
    };

    // hashtable of allowed tags
    // key - allowed tag name
    // value - null or hashtable of allowed attributes
    private static Map<String, TagInfo> m_hashAllowed;

    // hashtable of special tags (where entire body must be ignored)
    private static Map<String, Integer> m_hashSpecial;

    // hashtable of common attributes
    private static Map<String, Integer> m_hashAttributes;

    // helper method to compare two tag names
    public static boolean areTagsEqual(final String t1, final String t2) {
        if (t1 == t2) {
            return true;
        }

        if (t1 != null) {
            return t1.equalsIgnoreCase(t2);
        }

        return false;
    }

    // check if tag is allowed
    public static boolean isAllowedTag(final String s) {
        init();

        return m_hashAllowed.containsKey(s);
    }

    // check if tag is special
    public static boolean isSpecialTag(final String s) {
        init();

        return m_hashSpecial.containsKey(s);
    }

    // check if tag attribute is allowed
    public static boolean isAllowedAttribute(final String t, final String a) {
        init();

        if (m_hashAttributes.containsKey(a)) {
            return true;
        }

        if (m_hashAllowed.containsKey(t)) {
            final TagInfo ti = m_hashAllowed.get(t);

            return ti.m_attributes != null && ti.m_attributes.containsKey(a);
        }

        return false;
    }

    public static int getEOLBefore(final String t) {
        init();

        if (m_hashAllowed.containsKey(t)) {
            final TagInfo ti = m_hashAllowed.get(t);

            return ti.m_eolBefore;
        }

        return 0;
    }

    public static int getEOLAfter(final String t) {
        init();

        if (m_hashAllowed.containsKey(t)) {
            final TagInfo ti = m_hashAllowed.get(t);

            return ti.m_eolAfter;
        }

        return 0;
    }

    // add tag to hashmap
    private static void addTag(final String tag, final int eolBefore, final int eolAfter, final String[] attributes) {
        if (attributes == null) {
            m_hashAllowed.put(tag, new TagInfo());
        } else {
            final TagInfo ti = new TagInfo();
            ti.m_eolBefore = eolBefore;
            ti.m_eolAfter = eolAfter;

            /*
             * Using Unicode sort order and comparison (instead of Collator) is
             * OK here because the attribute names are pre-defined and all
             * English/ASCII.
             */
            ti.m_attributes = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

            for (final String s : attributes) {
                ti.m_attributes.put(s, 0);
            }
            m_hashAllowed.put(tag, ti);
        }
    }

    // overload with defaults
    private static void addTag(final String tag, final String[] attributes) {
        addTag(tag, 0, 0, attributes);
    }

    // add tags to hashmap
    private static void addTags(final String[] tags) {
        for (final String t : tags) {
            m_hashAllowed.put(t, new TagInfo());
        }
    }

    // add tag to hashmap
    private static void addCommonAttributes(final String[] attributes) {
        for (final String s : attributes) {
            m_hashAttributes.put(s, 0);
        }
    }

    // add special tags to hashmap
    private static void addSpecialTags(final String[] tags) {
        for (final String s : tags) {
            m_hashSpecial.put(s, 0);
        }
    }

    // initialize database,
    // the tags are taken from http://www.w3.org/TR/REC-html40/references.html
    // unsafe of breaking formatting tags/attributes are removed
    private static void init() {
        if (m_hashAllowed == null) {
            m_hashAllowed = new TreeMap<String, TagInfo>(String.CASE_INSENSITIVE_ORDER);
            m_hashSpecial = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);
            m_hashAttributes = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

            // add special tags (to ignore entire body)
            addSpecialTags(new String[] {
                "script", //$NON-NLS-1$
                "style", //$NON-NLS-1$
                "option", //$NON-NLS-1$
                "select", //$NON-NLS-1$
                "textarea" //$NON-NLS-1$
            });

            // add tags with specific attributes
            addTag(
                "a", //$NON-NLS-1$
                new String[] {
                    "charset", //$NON-NLS-1$
                    "href", //$NON-NLS-1$
                    "hreflang", //$NON-NLS-1$
                    "name", //$NON-NLS-1$
                    "rel", //$NON-NLS-1$
                    "rev", //$NON-NLS-1$
                    "shape", //$NON-NLS-1$
                    "tabindex", //$NON-NLS-1$
                    "type" //$NON-NLS-1$
            });
            addTag(
                "blockquote", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "cite" //$NON-NLS-1$
            });
            addTag(
                "br", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "clear" //$NON-NLS-1$
            });
            addTag(
                "caption", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "col", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "align", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "span", //$NON-NLS-1$
                    "valign", //$NON-NLS-1$
                    "width" //$NON-NLS-1$
            });
            addTag(
                "colgroup", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "align", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "span", //$NON-NLS-1$
                    "valign", //$NON-NLS-1$
                    "width" //$NON-NLS-1$
            });
            addTag(
                "del", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "cite", //$NON-NLS-1$
                    "datetime" //$NON-NLS-1$
            });
            addTag(
                "dir", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "compact" //$NON-NLS-1$
            });
            addTag(
                "div", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "dl", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "compact" //$NON-NLS-1$
            });
            addTag(
                "font", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "color", //$NON-NLS-1$
                    "face", //$NON-NLS-1$
                    "size" //$NON-NLS-1$
            });
            addTag(
                "h1", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "h2", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "h3", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "h4", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "h5", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "h6", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "hr", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align", //$NON-NLS-1$
                    "size", //$NON-NLS-1$
                    "width" //$NON-NLS-1$
            });
            addTag(
                "img", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "align", //$NON-NLS-1$
                    "alt", //$NON-NLS-1$
                    "border", //$NON-NLS-1$
                    "height", //$NON-NLS-1$
                    "hspace", //$NON-NLS-1$
                    "ismap", //$NON-NLS-1$
                    "longdesc", //$NON-NLS-1$
                    "name", //$NON-NLS-1$
                    "src", //$NON-NLS-1$
                    "usemap", //$NON-NLS-1$
                    "vspace", //$NON-NLS-1$
                    "width", //$NON-NLS-1$
                    "alt2", //$NON-NLS-1$
                    "src2" //$NON-NLS-1$
            });
            addTag(
                "ins", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "cite", //$NON-NLS-1$
                    "datetime" //$NON-NLS-1$
            });
            addTag(
                "li", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "type", //$NON-NLS-1$
                    "value" //$NON-NLS-1$
            });
            addTag(
                "map", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "name" //$NON-NLS-1$
            });
            addTag(
                "menu", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "compact" //$NON-NLS-1$
            });
            addTag(
                "ol", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "compact", //$NON-NLS-1$
                    "start", //$NON-NLS-1$
                    "type" //$NON-NLS-1$
            });
            addTag(
                "p", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "align" //$NON-NLS-1$
            });
            addTag(
                "pre", //$NON-NLS-1$
                1,
                1,
                new String[] {
                    "width" //$NON-NLS-1$
            });
            addTag(
                "q", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "cite" //$NON-NLS-1$
            });
            addTag(
                "table", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align", //$NON-NLS-1$
                    "border", //$NON-NLS-1$
                    "cellpadding", //$NON-NLS-1$
                    "cellspacing", //$NON-NLS-1$
                    "frame", //$NON-NLS-1$
                    "rules", //$NON-NLS-1$
                    "summary", //$NON-NLS-1$
                    "width", //$NON-NLS-1$
                    "caption" //$NON-NLS-1$
            });
            addTag(
                "tbody", //$NON-NLS-1$
                0,
                0,
                new String[] {
                    "align", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "valign" //$NON-NLS-1$
            });
            addTag(
                "td", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "abbr", //$NON-NLS-1$
                    "align", //$NON-NLS-1$
                    "axis", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "colspan", //$NON-NLS-1$
                    "headers", //$NON-NLS-1$
                    "height", //$NON-NLS-1$
                    "nowrap", //$NON-NLS-1$
                    "rowspan", //$NON-NLS-1$
                    "scope", //$NON-NLS-1$
                    "valign", //$NON-NLS-1$
                    "width" //$NON-NLS-1$
            });
            addTag(
                "tfoot", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "valign" //$NON-NLS-1$
            });
            addTag(
                "th", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "abbr", //$NON-NLS-1$
                    "align", //$NON-NLS-1$
                    "axis", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "colspan", //$NON-NLS-1$
                    "headers", //$NON-NLS-1$
                    "height", //$NON-NLS-1$
                    "nowrap", //$NON-NLS-1$
                    "rowspan", //$NON-NLS-1$
                    "scope", //$NON-NLS-1$
                    "valign", //$NON-NLS-1$
                    "width" //$NON-NLS-1$
            });
            addTag(
                "thead", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "valign" //$NON-NLS-1$
            });
            addTag(
                "tr", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "align", //$NON-NLS-1$
                    "char", //$NON-NLS-1$
                    "charoff", //$NON-NLS-1$
                    "valign" //$NON-NLS-1$
            });
            addTag(
                "ul", //$NON-NLS-1$
                2,
                2,
                new String[] {
                    "compact", //$NON-NLS-1$
                    "type" //$NON-NLS-1$
            });
            addTag("dd", 1, 2, new String[] {}); //$NON-NLS-1$
            addTag("dt", 2, 1, new String[] {}); //$NON-NLS-1$

            // add tags without attributes
            addTags(new String[] {
                "abbr", //$NON-NLS-1$
                "acronym", //$NON-NLS-1$
                "address", //$NON-NLS-1$
                "b", //$NON-NLS-1$
                "bdo", //$NON-NLS-1$
                "big", //$NON-NLS-1$
                "center", //$NON-NLS-1$
                "cite", //$NON-NLS-1$
                "code", //$NON-NLS-1$
                "dfn", //$NON-NLS-1$
                "em", //$NON-NLS-1$
                "i", //$NON-NLS-1$
                "kbd", //$NON-NLS-1$
                "s", //$NON-NLS-1$
                "samp", //$NON-NLS-1$
                "small", //$NON-NLS-1$
                "span", //$NON-NLS-1$
                "strike", //$NON-NLS-1$
                "strong", //$NON-NLS-1$
                "sub", //$NON-NLS-1$
                "sup", //$NON-NLS-1$
                "tt", //$NON-NLS-1$
                "u", //$NON-NLS-1$
                "var" //$NON-NLS-1$
            });

            // add attributes for all tags
            addCommonAttributes(new String[] {
                "dir", //$NON-NLS-1$
                "lang", //$NON-NLS-1$
                "title", //$NON-NLS-1$
                "style", //$NON-NLS-1$
                "id", //$NON-NLS-1$
                "class", //$NON-NLS-1$
                "contenteditable" //$NON-NLS-1$
            });
        }
    }
}
