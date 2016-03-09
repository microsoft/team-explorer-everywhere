// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.StringReader;

import junit.framework.TestCase;

public class NewlineUtilsTest extends TestCase {
    public void testDetectNewlineConvention() {
        /*
         * Normal files.
         */
        assertEquals("\n", NewlineUtils.detectNewlineConvention(new StringReader("first\nsecond\n"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r\n", NewlineUtils.detectNewlineConvention(new StringReader("first\r\nsecond\r\n"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r", NewlineUtils.detectNewlineConvention(new StringReader("first\rsecond\r"))); //$NON-NLS-1$//$NON-NLS-2$

        /*
         * No final newline.
         */
        assertEquals("\n", NewlineUtils.detectNewlineConvention(new StringReader("first\nsecond"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r\n", NewlineUtils.detectNewlineConvention(new StringReader("first\r\nsecond"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r", NewlineUtils.detectNewlineConvention(new StringReader("first\rsecond"))); //$NON-NLS-1$//$NON-NLS-2$

        /*
         * Only a newline.
         */
        assertEquals("\n", NewlineUtils.detectNewlineConvention(new StringReader("\n"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r\n", NewlineUtils.detectNewlineConvention(new StringReader("\r\n"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r", NewlineUtils.detectNewlineConvention(new StringReader("\r"))); //$NON-NLS-1$//$NON-NLS-2$

        /*
         * Mixed newlines. Should match first.
         */
        assertEquals("\n", NewlineUtils.detectNewlineConvention(new StringReader("first\nsecond\r\nthird"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r\n", NewlineUtils.detectNewlineConvention(new StringReader("first\r\nsecond\nthird"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals("\r", NewlineUtils.detectNewlineConvention(new StringReader("first\rsecond\r\nthird"))); //$NON-NLS-1$//$NON-NLS-2$

        /*
         * Unconventional Unicode newlines.
         */
        assertEquals(
            "" + NewlineUtils.NEXT_LINE, //$NON-NLS-1$
            NewlineUtils.detectNewlineConvention(new StringReader("first" + NewlineUtils.NEXT_LINE + "second"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(
            "" + NewlineUtils.LINE_TABULATION, //$NON-NLS-1$
            NewlineUtils.detectNewlineConvention(new StringReader("first" + NewlineUtils.LINE_TABULATION + "second"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(
            "" + NewlineUtils.FORM_FEED, //$NON-NLS-1$
            NewlineUtils.detectNewlineConvention(new StringReader("first" + NewlineUtils.FORM_FEED + "second"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(
            "" + NewlineUtils.LINE_SEPARATOR, //$NON-NLS-1$
            NewlineUtils.detectNewlineConvention(new StringReader("first" + NewlineUtils.LINE_SEPARATOR + "second"))); //$NON-NLS-1$//$NON-NLS-2$
        assertEquals(
            "" + NewlineUtils.PARAGRAPH_SEPARATOR, //$NON-NLS-1$
            NewlineUtils.detectNewlineConvention(
                new StringReader("first" + NewlineUtils.PARAGRAPH_SEPARATOR + "second"))); //$NON-NLS-1$//$NON-NLS-2$

        /*
         * No newlines or unusable input.
         */
        assertNull(NewlineUtils.detectNewlineConvention(new StringReader("first and second"))); //$NON-NLS-1$
        assertNull(NewlineUtils.detectNewlineConvention(new StringReader("\t"))); //$NON-NLS-1$
        assertNull(NewlineUtils.detectNewlineConvention(null));
    }
}