// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.utils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.core.Messages;

public abstract class StringUtil {

    public final static String EMPTY = ""; //$NON-NLS-1$
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
     * Check if input string is null or empty
     *
     * @param s
     * @return boolean
     */
    public static boolean isNullOrEmpty(final String s) {
        return s == null || s.length() == 0;
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
                    assert false : MessageFormat.format(Messages.getString("StringUtil.UnmaskedPasswordFormat"), msg); //$NON-NLS-1$
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
}
