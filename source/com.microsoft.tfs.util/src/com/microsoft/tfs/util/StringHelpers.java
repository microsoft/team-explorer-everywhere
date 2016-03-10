/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This file contains code derived from StringUtils.java in Apache Commons Lang.
 *
 * The remainder of the code is Copyright (c) Microsoft Corporation. All rights
 * reserved.
 */
package com.microsoft.tfs.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringHelpers {
    /**
     * Returns the given string without leading whitespace.
     *
     * @param s
     *        the string to strip leading whitespace from (not null).
     * @return the given string with all leading whitespace removed.
     */
    public static String trimBegin(final String s) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final int length = s.length();
        int index = 0;

        while (index < length && s.charAt(index) <= ' ') {
            index++;
        }

        return (index > 0) ? s.substring(index) : s;
    }

    /**
     * Return the given string without the specified leading characters.
     *
     * @param s
     *        the string to strip leading characters from (not null).
     * @param ch
     *        the leading character to remove.
     * @return the given string with all leading characters removed.
     */
    public static String trimBegin(final String s, final char ch) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final int length = s.length();
        int index = 0;

        while (index < length && s.charAt(index) == ch) {
            index++;
        }

        return (index > 0) ? s.substring(index) : s;
    }

    /**
     * Return the given string without the specified leading characters.
     *
     * @param s
     *        the string to strip leading characters from (not null).
     * @param trimChars
     *        the leading characters to remove.
     * @return the given string with all leading characters removed.
     */
    public static String trimBegin(final String s, final char[] trimChars) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final int length = s.length();
        int index = 0;

        while (index < length && isTrimChar(s.charAt(index), trimChars)) {
            index++;
        }

        return (index > 0) ? s.substring(index) : s;
    }

    /**
     * Returns the given string without trailing whitespace.
     *
     * @param s
     *        the string to strip trailing whitespace from (not null).
     * @return the given string with all trailing whitespace removed.
     */
    public static String trimEnd(final String s) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final int length = s.length();
        int index = s.length() - 1;

        while (index >= 0 && s.charAt(index) <= ' ') {
            index--;
        }

        return (index < length - 1) ? s.substring(0, index + 1) : s;
    }

    /**
     * Return the given string without specified trailing character.
     *
     * @param s
     *        the string to strip trailing characters from (not null).
     * @param ch
     *        the trailing character to eliminate.
     * @return the given string with all trailing characters removed.
     */
    public static String trimEnd(final String s, final char ch) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final int length = s.length();
        int index = s.length() - 1;

        while (index >= 0 && s.charAt(index) == ch) {
            index--;
        }

        return (index < length - 1) ? s.substring(0, index + 1) : s;
    }

    /**
     * Return the given string without specified trailing character.
     *
     * @param s
     *        the string to strip trailing characters from (not null).
     * @param trimChars
     *        the trailing characters to eliminate.
     * @return the given string with all trailing characters removed.
     */
    public static String trimEnd(final String s, final char[] trimChars) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final int length = s.length();
        int index = s.length() - 1;

        while (index >= 0 && isTrimChar(s.charAt(index), trimChars)) {
            index--;
        }

        return (index < length - 1) ? s.substring(0, index + 1) : s;

    }

    /**
     * Return true if the specified character appears in the character array.
     *
     * @param ch
     *        The character to test.
     * @param trimChars
     *        The set of trim characters.
     * @return true if the character is in the trim characters array.
     */
    private static boolean isTrimChar(final char ch, final char[] trimChars) {
        for (int i = 0; i < trimChars.length; i++) {
            if (trimChars[i] == ch) {
                return true;
            }
        }

        return false;
    }

    /**
     * Parse the pass string text into a resulting array of strings using the
     * passed delimeters. Note that the strings can be quoted using a pair of
     * single or double qutoes.
     *
     * @param delimiters
     *        String containing list of delimters that can be used, invalid
     *        delimters are &quot;,' or one of the special regex classes
     *        in-conjunction with another seperator i.e. a-z
     * @param text
     *        String to split using the passed delimters.
     * @return
     */
    public static String[] split(final String delimiters, final String text) {
        if (delimiters == null || delimiters.length() == 0) {
            throw new IllegalArgumentException("Must pass a delimiter."); //$NON-NLS-1$
        }
        if (delimiters.indexOf('\'') >= 0 || delimiters.indexOf('"') >= 0) {
            throw new IllegalArgumentException("' and \" are not allowed as delimiters"); //$NON-NLS-1$
        }
        if ((delimiters.indexOf('-') >= 0
            || delimiters.indexOf('[') >= 0
            || delimiters.indexOf(']') >= 0
            || delimiters.indexOf('&') >= 0) && delimiters.length() > 1) {
            throw new IllegalArgumentException("-,[,] or & are not allowed in-conjunction with other delimiters "); //$NON-NLS-1$
        }

        if (text == null) {
            return new String[0];
        }

        // Find text in-between " or ' or not a delimter.
        final String delimRegex = "\"[^\"]*\"|'[^']*'|[^" + delimiters + "]+"; //$NON-NLS-1$ //$NON-NLS-2$

        final Pattern pattern = Pattern.compile(delimRegex);
        final Matcher matcher = pattern.matcher(text);
        final ArrayList matches = new ArrayList();
        if (matcher.find()) {
            do {
                String matchText = matcher.group().trim();
                if ((matchText.startsWith("\"") && matchText.endsWith("\"")) //$NON-NLS-1$ //$NON-NLS-2$
                    || (matchText.startsWith("\'") && matchText.endsWith("\'"))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    // Remove quotes if used.
                    matchText = matchText.substring(1, matchText.length() - 1);
                }
                matches.add(matchText);
            } while (matcher.find());
        }

        return (String[]) matches.toArray(new String[matches.size()]);
    }

    /**
     * Call the java.lang.String.split(regx) with the supplied regex. Remove
     * empties from the resulting arrary.
     *
     * @param stringToSplit
     *        The string to split.
     * @param regex
     *        The regex to pass to java.lang.String.split
     * @return An array of split string with empties removed.
     */
    public static String[] splitRemoveEmpties(final String stringToSplit, final String regex) {
        Check.notNull(stringToSplit, "stringToSplit"); //$NON-NLS-1$
        Check.notNull(regex, "regex"); //$NON-NLS-1$

        final String[] strings = stringToSplit.split(regex);
        boolean hasEmpties = false;

        for (final String string : strings) {
            if (isNullOrEmpty(string)) {
                hasEmpties = true;
                break;
            }
        }

        if (!hasEmpties) {
            return strings;
        }

        final List<String> ret = new ArrayList<String>();

        for (final String string : strings) {
            if (!isNullOrEmpty(string)) {
                ret.add(string);
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Tests whether one of the strings in the given array of candidates equals
     * the given search string, ignoring character case.
     *
     * @param candidates
     *        the candidates to search (not null).
     * @param searchFor
     *        the string to search for in the candidates (not null).
     * @return true if the search string was found in the candidates ignoring
     *         character case, false if it was not found.
     */
    public static boolean containsStringInsensitive(final String[] candidates, final String searchString) {
        Check.notNull(candidates, "candidates"); //$NON-NLS-1$
        Check.notNull(searchString, "searchString"); //$NON-NLS-1$

        for (int i = 0; i < candidates.length; i++) {
            if (searchString.equalsIgnoreCase(candidates[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether one of the strings in the given array of candidates equals
     * the given search string, ignoring character case.
     *
     * @param s
     *        the string to search (not null).
     * @param searchFor
     *        the string to search for in the string (not null).
     * @return true if the search string was found in s ignoring character case,
     *         false if it was not found.
     */
    public static boolean containsIgnoreCase(final String s, final String searchFor) {
        Check.notNull(s, "s"); //$NON-NLS-1$
        Check.notNull(searchFor, "searchFor"); //$NON-NLS-1$

        return s.toLowerCase().contains(searchFor.toLowerCase());
    }

    /**
     * Test if the passed {@link String} is <code>null</code> or has a length of
     * zero.
     *
     * @param s
     *        {@link String} to be tested
     * @return true if null or empty.
     */
    public static boolean isNullOrEmpty(final String s) {
        return s == null || s.length() == 0;
    }

    // Replacing
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Replaces a String with another String inside a larger String, once.
     * </p>
     *
     * <p>
     * A <code>null</code> reference passed to this method is a no-op.
     * </p>
     *
     * <pre>
     * StringUtils.replaceOnce(null, *, *)        = null
     * StringUtils.replaceOnce(&quot;&quot;, *, *)          = &quot;&quot;
     * StringUtils.replaceOnce(&quot;any&quot;, null, *)    = &quot;any&quot;
     * StringUtils.replaceOnce(&quot;any&quot;, *, null)    = &quot;any&quot;
     * StringUtils.replaceOnce(&quot;any&quot;, &quot;&quot;, *)      = &quot;any&quot;
     * StringUtils.replaceOnce(&quot;aba&quot;, &quot;a&quot;, null)  = &quot;aba&quot;
     * StringUtils.replaceOnce(&quot;aba&quot;, &quot;a&quot;, &quot;&quot;)    = &quot;ba&quot;
     * StringUtils.replaceOnce(&quot;aba&quot;, &quot;a&quot;, &quot;z&quot;)   = &quot;zba&quot;
     * </pre>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text
     *        text to search and replace in, may be null
     * @param repl
     *        the String to search for, may be null
     * @param with
     *        the String to replace with, may be null
     * @return the text with any replacements processed, <code>null</code> if
     *         null String input
     */
    public static String replaceOnce(final String text, final String repl, final String with) {
        return replace(text, repl, with, 1);
    }

    /**
     * <p>
     * Replaces all occurrences of a String within another String.
     * </p>
     *
     * <p>
     * A <code>null</code> reference passed to this method is a no-op.
     * </p>
     *
     * <pre>
     * StringUtils.replace(null, *, *)        = null
     * StringUtils.replace(&quot;&quot;, *, *)          = &quot;&quot;
     * StringUtils.replace(&quot;any&quot;, null, *)    = &quot;any&quot;
     * StringUtils.replace(&quot;any&quot;, *, null)    = &quot;any&quot;
     * StringUtils.replace(&quot;any&quot;, &quot;&quot;, *)      = &quot;any&quot;
     * StringUtils.replace(&quot;aba&quot;, &quot;a&quot;, null)  = &quot;aba&quot;
     * StringUtils.replace(&quot;aba&quot;, &quot;a&quot;, &quot;&quot;)    = &quot;b&quot;
     * StringUtils.replace(&quot;aba&quot;, &quot;a&quot;, &quot;z&quot;)   = &quot;zbz&quot;
     * </pre>
     *
     * @see #replace(String text, String repl, String with, int max)
     * @param text
     *        text to search and replace in, may be null
     * @param repl
     *        the String to search for, may be null
     * @param with
     *        the String to replace with, may be null
     * @return the text with any replacements processed, <code>null</code> if
     *         null String input
     */
    public static String replace(final String text, final String repl, final String with) {
        return replace(text, repl, with, -1);
    }

    /**
     * <p>
     * Replaces a String with another String inside a larger String, for the
     * first <code>max</code> values of the search String.
     * </p>
     *
     * <p>
     * A <code>null</code> reference passed to this method is a no-op.
     * </p>
     *
     * <pre>
     * StringUtils.replace(null, *, *, *)         = null
     * StringUtils.replace(&quot;&quot;, *, *, *)           = &quot;&quot;
     * StringUtils.replace(&quot;any&quot;, null, *, *)     = &quot;any&quot;
     * StringUtils.replace(&quot;any&quot;, *, null, *)     = &quot;any&quot;
     * StringUtils.replace(&quot;any&quot;, &quot;&quot;, *, *)       = &quot;any&quot;
     * StringUtils.replace(&quot;any&quot;, *, *, 0)        = &quot;any&quot;
     * StringUtils.replace(&quot;abaa&quot;, &quot;a&quot;, null, -1) = &quot;abaa&quot;
     * StringUtils.replace(&quot;abaa&quot;, &quot;a&quot;, &quot;&quot;, -1)   = &quot;b&quot;
     * StringUtils.replace(&quot;abaa&quot;, &quot;a&quot;, &quot;z&quot;, 0)   = &quot;abaa&quot;
     * StringUtils.replace(&quot;abaa&quot;, &quot;a&quot;, &quot;z&quot;, 1)   = &quot;zbaa&quot;
     * StringUtils.replace(&quot;abaa&quot;, &quot;a&quot;, &quot;z&quot;, 2)   = &quot;zbza&quot;
     * StringUtils.replace(&quot;abaa&quot;, &quot;a&quot;, &quot;z&quot;, -1)  = &quot;zbzz&quot;
     * </pre>
     *
     * @param text
     *        text to search and replace in, may be null
     * @param repl
     *        the String to search for, may be null
     * @param with
     *        the String to replace with, may be null
     * @param max
     *        maximum number of values to replace, or <code>-1</code> if no
     *        maximum
     * @return the text with any replacements processed, <code>null</code> if
     *         null String input
     */
    public static String replace(final String text, final String repl, final String with, int max) {
        if (isNullOrEmpty(text) || isNullOrEmpty(repl) || with == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(repl, start);
        if (end == -1) {
            return text;
        }
        final int replLength = repl.length();
        int increase = with.length() - replLength;
        increase = (increase < 0 ? 0 : increase);
        increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
        final StringBuffer buf = new StringBuffer(text.length() + increase);
        while (end != -1) {
            buf.append(text.substring(start, end)).append(with);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(repl, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static String join(final String[] strings, final String joinString) {
        final StringBuffer buffer = new StringBuffer();
        if (strings != null && strings.length > 0) {
            buffer.append(strings[0]);

            for (int i = 1; i < strings.length; i++) {
                buffer.append(joinString);
                buffer.append(strings[i]);
            }
        }
        return buffer.toString();
    }

    public static boolean startsWithIgnoreCase(final String value, final String possiblePrefix) {
        if (value.length() < possiblePrefix.length()) {
            return false;
        }

        return value.substring(0, possiblePrefix.length()).equalsIgnoreCase(possiblePrefix);
    }
}
