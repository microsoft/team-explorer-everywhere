// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

public class JSONEncoderDecodeStringTest extends TestCase {
    public void testDecodeNull() {
        assertEquals(null, JSONEncoder.decodeString(null));
        assertEquals(null, JSONEncoder.decodeString("null")); //$NON-NLS-1$
    }

    public void testDecodeEmpty() {
        assertEquals("", JSONEncoder.decodeString("\"\"")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testDecodeSimple() {
        assertEquals(" abc def ", JSONEncoder.decodeString("\" abc def \"")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testDecodeIgnoreWhitespaceAndTrailingChars() {
        assertEquals(" abc def ", JSONEncoder.decodeString(" \r\n\t \" abc def \" ignore this trailing stuff")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testDecodeLength() {
        final AtomicInteger charsRead = new AtomicInteger(-123);

        assertEquals("abc", JSONEncoder.decodeString("\"abc\" extra characters", charsRead)); //$NON-NLS-1$ //$NON-NLS-2$
        // Should have read 5 chars (start quote, "abc", end quote)
        assertEquals(5, charsRead.get());
    }

    public void testDecodeStandardEscapes() {
        assertEquals("\" \\ / \b \f \n \r \t", JSONEncoder.decodeString("\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t\"")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testDecodeUnicodeEscapes() {
        // Unicode control chars
        assertEquals("\u0000", JSONEncoder.decodeString("\"\\u0000\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\u001f", JSONEncoder.decodeString("\"\\u001F\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\u007f", JSONEncoder.decodeString("\"\\u007F\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\u009f", JSONEncoder.decodeString("\"\\u009F\"")); //$NON-NLS-1$ //$NON-NLS-2$

        // Non-control chars
        assertEquals("\u0020", JSONEncoder.decodeString("\"\\u0020\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\u007e", JSONEncoder.decodeString("\"\\u007E\"")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\u00a0", JSONEncoder.decodeString("\"\\u00A0\"")); //$NON-NLS-1$ //$NON-NLS-2$

        // Decode accepts upper and lower case hex
        assertEquals("\uffff", JSONEncoder.decodeString("\"\\uFfFf\"")); //$NON-NLS-1$ //$NON-NLS-2$

        // Some of the input code points are escaped, some are not
        assertEquals(
            "\u0000 \u1234 \uffff \u1234\u5678", //$NON-NLS-1$
            JSONEncoder.decodeString("\"\\u0000 \\u1234 \uffff \\u1234\\u5678\"")); //$NON-NLS-1$
    }

    public void testDecodeSurrogate() {
        // See http://en.wikipedia.org/wiki/UTF-16

        // String has two chars but just one code point: 0xD800 + 0xDC01 Decodes
        // 0x10001 (65537)
        final String withSurrogates = "\uD800\uDC01"; //$NON-NLS-1$
        assertEquals(1, withSurrogates.codePointCount(0, 1));
        assertEquals(65537, withSurrogates.codePointAt(0));

        // Surrogates are preserved after decoding
        assertEquals(withSurrogates, JSONEncoder.decodeString("\"\ud800\udc01\"")); //$NON-NLS-1$
        assertEquals(
            "something " + withSurrogates + " else", //$NON-NLS-1$//$NON-NLS-2$
            JSONEncoder.decodeString("\"something \ud800\udc01 else\"")); //$NON-NLS-1$
    }

    public void testDecodeSurrogateLength() {
        final AtomicInteger charsRead = new AtomicInteger(-123);

        // String has two chars but just one code point: 0xD800 + 0xDC01 Decodes
        // 0x10001 (65537)
        final String withSurrogates = "\uD800\uDC01"; //$NON-NLS-1$
        assertEquals(1, withSurrogates.codePointCount(0, 1));
        assertEquals(65537, withSurrogates.codePointAt(0));

        assertEquals(
            withSurrogates,
            JSONEncoder.decodeString("\"" + withSurrogates + "\" extra characters", charsRead)); //$NON-NLS-1$ //$NON-NLS-2$

        // Should have read 4 chars (start quote, 2 chars that make
        // 1 code point, end quote)
        assertEquals(4, charsRead.get());
    }

    public void testDecodeFailAtBeginning() {
        try {
            // No opening double quote
            JSONEncoder.decodeString(""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // No opening double quote
            JSONEncoder.decodeString("fun"); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // No opening double quote
            JSONEncoder.decodeString("'"); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }
    }

    public void testDecodeFailAtEnd() {
        try {
            // No closing double quote
            JSONEncoder.decodeString("\"hello"); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // No closing double quote (one is escaped in the middle)
            JSONEncoder.decodeString("\"hello\\\" people"); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            //
            JSONEncoder.decodeString("\"hello\\\" people"); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }
    }

    public void testDecodeFailEscape() {
        try {
            // \m isn't defined by JSON
            JSONEncoder.decodeString("\" this escape \\m is unknown \""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // Has an ending double quote but not a complete escape sequence
            JSONEncoder.decodeString("\" only half an escape \\\""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }
    }

    public void testDecodeFailUnicodeEscape() {
        try {
            JSONEncoder.decodeString("\" this escape \\u contains fewer than 4 hex digits \""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            JSONEncoder.decodeString("\" this escape \\u0 contains fewer than 4 hex digits \""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            JSONEncoder.decodeString("\" this escape \\u01 contains fewer than 4 hex digits \""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            JSONEncoder.decodeString("\" this escape \\u012 contains fewer than 4 hex digits \""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            JSONEncoder.decodeString("\" this escape \\u012m contains a non-hex digit \""); //$NON-NLS-1$
            fail();
        } catch (final NumberFormatException e) {
        }

        try {
            // Ends with incomplete escape
            JSONEncoder.decodeString("\" only half an escape \\u\""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // Ends with incomplete escape
            JSONEncoder.decodeString("\" only half an escape \\u0\""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // Ends with incomplete escape
            JSONEncoder.decodeString("\" only half an escape \\u01\""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // Ends with incomplete escape
            JSONEncoder.decodeString("\" only half an escape \\u012\""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }

        try {
            // Ends with incomplete escape
            JSONEncoder.decodeString("\" only half an escape \\u013\""); //$NON-NLS-1$
            fail();
        } catch (final JSONParseException e) {
        }
    }
}
