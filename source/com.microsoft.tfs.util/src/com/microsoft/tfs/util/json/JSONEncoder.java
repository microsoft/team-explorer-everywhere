// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Messages;

/**
 * Encodes/decodes JSON types.
 *
 * @see http://json.org/
 * @see http://www.ietf.org/rfc/rfc4627.txt
 * @threadsafety thread-safe
 */
public class JSONEncoder {
    private final static String JSON_NULL = "null"; //$NON-NLS-1$

    private static enum StringParseState {
        /**
         * The initial state. No characters have been read.
         *
         * Valid transitions are to: {@link #TEXT}
         */
        START,

        /**
         * The starting double-quote character was read and the parser is not
         * handling an escape sequence.
         *
         * Valid transitions are to: {@link #ESCAPE}, {@link #DONE}
         */
        TEXT,

        /**
         * The most recently read character was a backslash that starts an
         * escape sequence.
         *
         * Valid transitions are to: {@link #ESCAPE_U}, {@link #TEXT}
         */
        ESCAPE,

        /**
         * A backslash followed by a "u" was recently read (possibly more than
         * one iteration ago) and we're decoding one of the four hex digits that
         * represent a Unicode code point.
         *
         * Valid transitions are to: {@link #TEXT}
         */
        ESCAPE_U,

        /**
         * The final double-quote character has been read.
         *
         * This is the terminal state.
         */
        DONE
    }

    private static enum ObjectParseState {
        /**
         * The initial state. No characters have been read.
         *
         * Valid transitions are to: {@link #MEMBERS}
         */
        START,

        /**
         * The starting curly brace character was read and the parser is reading
         * members (the only things that can be in an object).
         *
         * Valid transitions are to: {@link #MEMBER_NAME}, {@link #DONE}
         */
        MEMBERS,

        /**
         * Reading a member name.
         *
         * Valid transitions are to: {@link #MEMBER_NAME_SEPARATOR}
         */
        MEMBER_NAME,

        /**
         * Reading the colon and optional whitespace that separates a member
         * name from its value.
         *
         * Valid transitions are to: {@link #MEMBER_VALUE}
         */
        MEMBER_NAME_SEPARATOR,

        /**
         * The member value is being read.
         *
         * Valid transitions are to: {@link #MEMBERS}
         */
        MEMBER_VALUE,

        /**
         * The final curly brace character has been read.
         *
         * This is the terminal state.
         */
        DONE
    }

    private final static Map<Character, Character> ENCODE_CHAR_ESCAPES = new HashMap<Character, Character>();
    private final static Map<Character, Character> DECODE_CHAR_ESCAPES = new HashMap<Character, Character>();

    static {
        /*
         * Membership in either of these maps (they're inverses of each other)
         * triggers single character escaping during encode/decode.
         */
        ENCODE_CHAR_ESCAPES.put('"', '"');
        ENCODE_CHAR_ESCAPES.put('\\', '\\');
        ENCODE_CHAR_ESCAPES.put('/', '/');
        ENCODE_CHAR_ESCAPES.put('\b', 'b');
        ENCODE_CHAR_ESCAPES.put('\f', 'f');
        ENCODE_CHAR_ESCAPES.put('\n', 'n');
        ENCODE_CHAR_ESCAPES.put('\r', 'r');
        ENCODE_CHAR_ESCAPES.put('\t', 't');

        // Construct the inverse
        for (final Entry<Character, Character> e : ENCODE_CHAR_ESCAPES.entrySet()) {
            DECODE_CHAR_ESCAPES.put(e.getValue(), e.getKey());
        }
    }

    public static String encodeObject(final JSONObject object) {
        Check.notNull(object, "object"); //$NON-NLS-1$

        final StringBuilder encoded = new StringBuilder("{"); //$NON-NLS-1$

        int i = 0;
        for (final Entry<String, String> entry : object.entrySet()) {
            if (i++ > 0) {
                encoded.append(',');
            }

            // JSONObject ensures key is never null
            final String key = entry.getKey();
            final String value = entry.getValue();

            encoded.append(encodeString(key));
            encoded.append(':');
            if (value == null) {
                encoded.append(JSON_NULL);
            } else {
                encoded.append(encodeString(entry.getValue()));
            }
        }

        encoded.append("}"); //$NON-NLS-1$
        return encoded.toString();
    }

    public static JSONObject decodeObject(final String s) {
        return decodeObject(s, null);
    }

    /**
     * Decodes a JSON object from the Java string.
     *
     * @param s
     *        the string to decode from (may be <code>null</code>)
     * @param charsRead
     *        receives the number of characters (not code points) read from the
     *        input string (may be <code>null</code>)
     * @return the string decoded or <code>null</code> if the input was
     *         <code>null</code> or the contents specified a JSON null
     */
    public static JSONObject decodeObject(final String s, final AtomicInteger charsRead) {
        Check.notNull(s, "s"); //$NON-NLS-1$

        final JSONObject decoded = new JSONObject();
        ObjectParseState state = ObjectParseState.START;

        String memberName = null;

        // Handle surrogate pairs correctly

        final int length = s.length();
        int i = 0;
        while (i < length && state != ObjectParseState.DONE) {
            final int codePoint = s.codePointAt(i);
            final char c = (char) codePoint;
            boolean advance = true;

            switch (state) {
                case START:
                    if (Character.isWhitespace(codePoint)) {
                        // Ignore
                    } else if (c == '{') {
                        state = ObjectParseState.MEMBERS;
                    } else {
                        throw new JSONParseException(Messages.getString("JSONEncoder.StringMustStartWithCurlyBrace")); //$NON-NLS-1$
                    }
                    break;
                case MEMBERS:
                    if (Character.isWhitespace(codePoint) || c == ',') {
                        // Ignore
                    } else if (c == '}') {
                        state = ObjectParseState.DONE;
                    } else if (c == '"') {
                        final AtomicInteger nameCharsRead = new AtomicInteger();
                        memberName = decodeString(s.substring(i), nameCharsRead);
                        if (memberName == null) {
                            throw new JSONParseException(Messages.getString("JSONEncoder.MemberNameCannotBeNull")); //$NON-NLS-1$
                        }

                        // Advance by exactly the number of chars we read
                        advance = false;
                        i += nameCharsRead.get();

                        // Next we need a separator
                        state = ObjectParseState.MEMBER_NAME_SEPARATOR;
                    } else {
                        throw new JSONParseException(
                            MessageFormat.format(
                                Messages.getString("JSONEncoder.UnexpectedCharacterReadingMembersFormat"), //$NON-NLS-1$
                                c));
                    }
                    break;
                case MEMBER_NAME_SEPARATOR:
                    if (Character.isWhitespace(codePoint)) {
                        // Ignore
                    } else if (c == ':') {
                        state = ObjectParseState.MEMBER_VALUE;
                    } else {
                        throw new JSONParseException(
                            MessageFormat.format(
                                Messages.getString("JSONEncoder.UnexpectedCharacterReadingNameValueSeparatorFormat"), //$NON-NLS-1$
                                c));
                    }
                    break;
                case MEMBER_VALUE:
                    if (Character.isWhitespace(codePoint)) {
                        // Ignore
                    } else if (s.startsWith(JSON_NULL, i)) {
                        decoded.put(memberName, null);
                        memberName = null;

                        advance = false;
                        i += JSON_NULL.length();
                        state = ObjectParseState.MEMBERS;
                    } else if (c == '"') {
                        final AtomicInteger valueCharsRead = new AtomicInteger();

                        decoded.put(memberName, decodeString(s.substring(i), valueCharsRead));
                        memberName = null;

                        advance = false;
                        i += valueCharsRead.get();
                        state = ObjectParseState.MEMBERS;
                    } else {
                        throw new JSONParseException(
                            MessageFormat.format(
                                Messages.getString("JSONEncoder.UnexpectedCharacterReadingMemberValueFormat"), //$NON-NLS-1$
                                c));
                    }
                    break;
                default:
                    throw new JSONParseException(MessageFormat.format("Unknown parse state {0}", state)); //$NON-NLS-1$
            }

            if (advance) {
                i += Character.charCount(codePoint);
            }
        }

        // Check for premature end-of-string
        switch (state) {
            case START:
                throw new JSONParseException(Messages.getString("JSONEncoder.StringMustStartWithCurlyBrace")); //$NON-NLS-1$
            case MEMBERS:
                throw new JSONParseException(Messages.getString("JSONEncoder.StringMustEndWithCurlyBrace")); //$NON-NLS-1$
            case MEMBER_NAME_SEPARATOR:
                throw new JSONParseException(Messages.getString("JSONEncoder.StringEndedBeforeMemberValueCouldBeRead")); //$NON-NLS-1$
            case MEMBER_VALUE:
                throw new JSONParseException(Messages.getString("JSONEncoder.StringEndedBeforeMemberValueCouldBeRead")); //$NON-NLS-1$
        }

        if (charsRead != null) {
            charsRead.set(i);
        }

        return decoded;
    }

    public static String encodeString(final String s) {
        if (s == null) {
            return JSON_NULL;
        }

        final StringBuilder encoded = new StringBuilder("\""); //$NON-NLS-1$

        // Handle surrogate pairs correctly

        final int length = s.length();
        int i = 0;
        while (i < length) {
            final int codePoint = s.codePointAt(i);
            final char c = (char) codePoint;
            i += Character.charCount(codePoint);

            // Use our mappings in preference to the hex encoding for control
            // chars
            if (ENCODE_CHAR_ESCAPES.containsKey(c)) {
                encoded.append('\\');
                encoded.append(ENCODE_CHAR_ESCAPES.get(c));
            } else if (Character.isISOControl(codePoint)) {
                encoded.append("\\u"); //$NON-NLS-1$
                encoded.append(String.format("%04x", codePoint)); //$NON-NLS-1$
            } else {
                encoded.appendCodePoint(codePoint);
            }
        }

        encoded.append('"');
        return encoded.toString();
    }

    public static String decodeString(final String s) {
        return decodeString(s, null);
    }

    /**
     * Decodes a JSON string from the Java string.
     *
     * @param s
     *        the string to decode from (may be <code>null</code>)
     * @param charsRead
     *        receives the number of characters (not code points) read from the
     *        input string into the returned string (may be <code>null</code>)
     * @return the string decoded or <code>null</code> if the input was
     *         <code>null</code> or the contents specified a JSON null
     */
    public static String decodeString(final String s, final AtomicInteger charsRead) {
        if (s == null) {
            return null;
        }

        if (JSON_NULL.equals(s)) {
            return null;
        }

        final StringBuilder decoded = new StringBuilder();
        final StringBuilder hexString = new StringBuilder(4);
        StringParseState state = StringParseState.START;

        // Handle surrogate pairs correctly

        final int length = s.length();
        int i = 0;
        while (i < length && state != StringParseState.DONE) {
            final int codePoint = s.codePointAt(i);
            final char c = (char) codePoint;

            switch (state) {
                case START:
                    if (Character.isWhitespace(codePoint)) {
                        // Ignore
                    } else if (c == '"') {
                        state = StringParseState.TEXT;
                    } else {
                        throw new JSONParseException(Messages.getString("JSONConvert.StringMustStartWithDoubleQuote")); //$NON-NLS-1$
                    }
                    break;
                case TEXT:
                    if (c == '"') {
                        state = StringParseState.DONE;
                    } else if (c == '\\') {
                        state = StringParseState.ESCAPE;
                    } else {
                        decoded.appendCodePoint(codePoint);
                    }
                    break;
                case ESCAPE:
                    if (c == 'u') {
                        state = StringParseState.ESCAPE_U;
                    } else if (DECODE_CHAR_ESCAPES.containsKey(c)) {
                        decoded.append(DECODE_CHAR_ESCAPES.get(c));
                        state = StringParseState.TEXT;
                    } else {
                        throw new JSONParseException(
                            MessageFormat.format(Messages.getString("JSONConvert.UnknownEscapeSequenceFormat"), c)); //$NON-NLS-1$
                    }
                    break;
                case ESCAPE_U:
                    // Ensure it's something like a hexadecimal character
                    if (c == '"') {
                        throw new JSONParseException(
                            Messages.getString("JSONConvert.StringEndedBeforeAllFourHexadecimal")); //$NON-NLS-1$
                    } else if (!Character.isLetterOrDigit(c)) {
                        throw new JSONParseException(
                            MessageFormat.format(
                                Messages.getString("JSONConvert.CharacterNotValidHexadecimalaFormat"), //$NON-NLS-1$
                                c));
                    }

                    hexString.append(c);
                    if (hexString.length() == 4) {
                        decoded.appendCodePoint(Integer.parseInt(hexString.toString(), 16));
                        hexString.setLength(0);
                        state = StringParseState.TEXT;
                    }
                    break;
                default:
                    throw new JSONParseException(MessageFormat.format("Unknown parse state {0}", state)); //$NON-NLS-1$
            }

            i += Character.charCount(codePoint);
        }

        // Check for premature end-of-string
        switch (state) {
            case START:
                throw new JSONParseException(Messages.getString("JSONConvert.StringMustStartWithDoubleQuote")); //$NON-NLS-1$
            case TEXT:
                throw new JSONParseException(Messages.getString("JSONConvert.StringMustEndWithDoubleQuote")); //$NON-NLS-1$
            case ESCAPE:
                throw new JSONParseException(Messages.getString("JSONConvert.StringEndedBeforeEscapeIdentified")); //$NON-NLS-1$
            case ESCAPE_U:
                throw new JSONParseException(Messages.getString("JSONConvert.StringEndedBeforeAllFourHexadecimal")); //$NON-NLS-1$
        }

        if (charsRead != null) {
            charsRead.set(i);
        }

        return decoded.toString();
    }
}
