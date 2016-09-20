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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringUtil {

    public static final int MAX_COMMENT_DISPLAY_LENGTH = 120;
    public static final String ELLIPSIS = "..."; //$NON-NLS-1$
    public static final String EMPTY = ""; //$NON-NLS-1$
    public final static String UTF8_CHARSET = "UTF-8"; //$NON-NLS-1$

    private final static String PASSWORD_TOKEN = "Password="; //$NON-NLS-1$
    private final static String PWD_TOKEN = "Pwd="; //$NON-NLS-1$
    private final static String ACCOUNT_KEY_TOKEN = "AccountKey="; //$NON-NLS-1$
    private final static String PASSWORD_MASK = "******"; //$NON-NLS-1$

    private final static List<Character> VALID_PASSWORD_ENDING = Arrays.asList(';', '\'', '"');
    private static String[] TOKENS_TO_SCRUB = new String[] {
        PASSWORD_TOKEN,
        PWD_TOKEN,
        ACCOUNT_KEY_TOKEN
    };

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
        if (value == null || possiblePrefix == null || value.length() < possiblePrefix.length()) {
            return false;
        }

        return value.substring(0, possiblePrefix.length()).equalsIgnoreCase(possiblePrefix);
    }

    /**
     * Format a comment for one line display. Removes all new line characters
     * and tabs then trims any whitespace from the end of the comment and adds
     * an ellipsis if the comment is greater than
     * {@link MAX_COMMENT_DISPLAY_LENGTH}
     *
     * @param comment
     * @return
     */
    public static String formatCommentForOneLine(String comment) {
        if (isNullOrEmpty(comment)) {
            return EMPTY;
        }

        // Remove new lines
        comment = comment.replace('\n', ' ').replace('\r', ' ');
        // Replace tabs with 4 spaces
        comment = replace(comment, "\t", "    ").trim(); //$NON-NLS-1$ //$NON-NLS-2$

        // If the comment is greater than 120 characters trim it and add
        // ellipsis.
        if (comment.length() > MAX_COMMENT_DISPLAY_LENGTH) {
            comment = comment.substring(0, MAX_COMMENT_DISPLAY_LENGTH - 3);
            comment = comment + ELLIPSIS;
        }

        return comment;
    }

    /**
     * Join values to string
     *
     * @param delimiter
     * @param values
     * @return String
     */
    public static String join(final String delimiter, final List<?> values) {
        final StringBuilder sb = new StringBuilder();

        for (final Object v : values) {
            if (sb.length() > 0) {
                sb.append(delimiter);
            }

            sb.append(v);
        }

        return sb.toString();
    }

    /**
     * Scrub password
     *
     * @param message
     * @return String
     */
    public static String ScrubPassword(final String message) {
        return ScrubPassword(message, true);
    }

    /**
     * Scrub Password
     *
     * @param message
     * @param assertOnDetection
     * @return String
     */
    public static String ScrubPassword(final String message, final boolean assertOnDetection) {
        if (isNullOrEmpty(message)) {
            return message;
        }

        String msg = message;

        for (final String token : TOKENS_TO_SCRUB) {
            msg = ScrubSecret(msg, token, PASSWORD_MASK, assertOnDetection);
        }

        return msg;
    }

    /**
     * Scrub secret
     *
     * @param message
     * @param token
     * @param mask
     * @param assertOnDetection
     * @return String
     */
    private static String ScrubSecret(
        final String message,
        final String token,
        final String mask,
        final boolean assertOnDetection) {

        int startIndex = 0;
        String msg = message;

        do {
            startIndex = msg.toUpperCase().indexOf(token.toUpperCase(), startIndex);
            if (startIndex < 0) {
                // Common case, there is not a password.
                break;
            }

            if (msg.toUpperCase().indexOf(token.toUpperCase() + mask.toUpperCase()) == startIndex) {
                // The password is already masked, move past this string.
                startIndex += token.length() + mask.length();
                continue;
            }

            // At this point we detected a password that is not masked, remove
            // it!
            try {
                startIndex += token.length();

                // Find the end of the password.
                int endIndex = msg.length() - 1;

                if (msg.charAt(startIndex) == '"' || msg.charAt(startIndex) == '\'') {
                    // The password is wrapped in quotes. The end of the string
                    // will be the next unpaired quote.
                    // Unless the message itself wrapped the connection string
                    // in quotes, in which case we may mask out the rest of the
                    // message. Better to be safe than leak the connection
                    // string.
                    // Intentionally going to "i < message.Length - 1". If the
                    // quote isn't the second to last character, it is the last
                    // character, and we delete to the end of the string anyway.
                    for (int i = startIndex + 1; i < msg.length() - 1; i++) {
                        if (msg.charAt(startIndex) == msg.charAt(i)) {
                            if (msg.charAt(startIndex) == msg.charAt(i + 1)) {
                                // we found a pair of quotes. Skip over the pair
                                // and continue.
                                i++;
                                continue;
                            } else {
                                // this is a single quote, and the end of the
                                // password.
                                endIndex = i;
                                break;
                            }
                        }
                    }
                } else {
                    // The password is not wrapped in quotes.
                    // The end is any whitespace, semi-colon, single, or double
                    // quote character.
                    for (int i = startIndex + 1; i < msg.length(); i++) {
                        if (VALID_PASSWORD_ENDING.contains(msg.charAt(i))) {
                            endIndex = i - 1;
                            break;
                        }
                    }
                }

                msg = msg.substring(0, startIndex) + mask + msg.substring(endIndex + 1);

                // Bug 94478: We need to scrub the message before Assert,
                // otherwise we will fall into
                // a recursive assert where the TeamFoundationServerException
                // contains same message
                if (assertOnDetection) {
                    Check.isTrue(
                        false,
                        MessageFormat.format("Message contains an unmasked password. Message: {0}", msg)); //$NON-NLS-1$
                }

                // Trace raw that we have scrubbed a message.
                // TODO: We need a work item to add Tracing to the VSS Client
                // assembly.
                // TraceLevel traceLevel = assertOnDetection ? TraceLevel.Error
                // : TraceLevel.Info;
                // TeamFoundationTracingService.TraceRaw(99230, traceLevel,
                // s_area, s_layer,
                // "An unmasked password was detected in a message. MESSAGE:
                // {0}. STACK TRACE: {1}",
                // message, Environment.StackTrace);
            } catch (final Exception ex) {
                // With an exception here the message may still contain an
                // unmasked password.
                // We also do not want to interupt the current thread with this
                // exception, because it may be constucting a message
                // for a different exception. Trace this exception and continue
                // on using a generic exception message.
                // TeamFoundationTracingService.TraceExceptionRaw(99231, s_area,
                // s_layer, exception);
            } finally {
                // Iterate to the next password (if it exists)
                startIndex += mask.length();
            }
        } while (startIndex < msg.length());

        return msg;
    }

    /**
     * Create new string
     *
     * @param padChar
     * @param width
     * @return String
     */
    public static String newString(final char padChar, final int width) {
        final char[] buf = new char[width];

        for (int i = 0; i < width; i++) {
            buf[i] = padChar;
        }

        return new String(buf);
    }

    /**
     * Pad string
     *
     * @param s
     * @param width
     * @param leftJustified
     * @param padChar
     * @return String
     */
    public static String pad(final String s, final int width, final boolean leftJustified, final char padChar) {
        if (leftJustified) {
            return (s + newString(' ', width)).substring(0, width);
        } else {
            return (newString(' ', width) + s).substring(s.length());
        }
    }

    /**
     * Pad string
     *
     * @param s
     * @param width
     * @param leftJustified
     * @return String
     */
    public static String pad(final String s, final int width, final boolean leftJustified) {
        return pad(s, width, leftJustified, ' ');
    }

    /**
     * Pad string
     *
     * @param s
     * @param width
     * @return String
     */
    public static String pad(final String s, final int width) {
        return pad(s, width, true, ' ');
    }

    /**
     * Pad string
     *
     * @param n
     * @param width
     * @param padChar
     * @return String
     */
    public static String pad(final Number n, final int width, final char padChar) {
        return pad(n.toString(), width, false, padChar);
    }

    /**
     * Pad string
     *
     * @param n
     * @param width
     * @return String
     */
    public static String pad(final Number n, final int width) {
        return pad(n, width, ' ');
    }

    public static int toInt(final String s) {
        final int number;
        switch (s.charAt(s.length() - 1)) {
            case 'K':
            case 'k':
                number = Integer.parseInt(s.substring(0, s.length() - 1).trim()) * 1024;
                break;
            case 'M':
            case 'm':
                number = Integer.parseInt(s.substring(0, s.length() - 1).trim()) * 1024 * 1024;
                break;
            default:
                number = Integer.parseInt(s.trim());
        }

        return number;

    }

    public static String escapeXml(final String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
