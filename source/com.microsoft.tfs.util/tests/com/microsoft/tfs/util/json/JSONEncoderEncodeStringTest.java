// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.json;

import junit.framework.TestCase;

public class JSONEncoderEncodeStringTest extends TestCase {
    public void testEncodeNull() {
        assertEquals("null", JSONEncoder.encodeString(null)); //$NON-NLS-1$
    }

    public void testEncodeEmpty() {
        assertEquals("\"\"", JSONEncoder.encodeString("")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEncodeSimple() {
        assertEquals("\"abc def\"", JSONEncoder.encodeString("abc def")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\" abc def \"", JSONEncoder.encodeString(" abc def ")); //$NON-NLS-1$ //$NON-NLS-2$

    }

    public void testEncodeStandardEscapes() {
        // Some of these are ISO control chars, but they should always be
        // encoded with the digraph (not the 4 digit hex version).
        assertEquals("\"\\\" \\\\ \\/ \\b \\f \\n \\r \\t\"", JSONEncoder.encodeString("\" \\ / \b \f \n \r \t")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEncodeUnicodeEscapes() {
        // Encoded hex is always lower case

        // Unicode control chars always get escaped
        assertEquals("\"\\u0000\"", JSONEncoder.encodeString("\u0000")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\"\\u001f\"", JSONEncoder.encodeString("\u001F")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\"\\u007f\"", JSONEncoder.encodeString("\u007F")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\"\\u009f\"", JSONEncoder.encodeString("\u009F")); //$NON-NLS-1$ //$NON-NLS-2$

        // These are not control characters and should be left alone (note the
        // single backslash before the 'u' in the expected string).
        assertEquals("\"\u0020\"", JSONEncoder.encodeString("\u0020")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\"\u007e\"", JSONEncoder.encodeString("\u007E")); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("\"\u00a0\"", JSONEncoder.encodeString("\u00A0")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEncodeSurrogate() {
        // See http://en.wikipedia.org/wiki/UTF-16

        // String has two chars but just one code point: 0xD800 + 0xDC01 encodes
        // 0x10001 (65537)
        final String withSurrogates = "\uD800\uDC01"; //$NON-NLS-1$
        assertEquals(1, withSurrogates.codePointCount(0, 1));
        assertEquals(65537, withSurrogates.codePointAt(0));

        // Surrogates are preserved after encoding
        assertEquals("\"\ud800\udc01\"", JSONEncoder.encodeString(withSurrogates)); //$NON-NLS-1$
        assertEquals(
            "\"something \ud800\udc01 else\"", //$NON-NLS-1$
            JSONEncoder.encodeString("something " + withSurrogates + " else")); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
