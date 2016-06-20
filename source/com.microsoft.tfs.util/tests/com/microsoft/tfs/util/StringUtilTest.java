// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import junit.framework.TestCase;

public class StringUtilTest extends TestCase {
    public void testSingle() {
        final String[] actual = StringUtil.split(";,", "John"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John" //$NON-NLS-1$
        }, actual);
    }

    public void testBasic() {
        final String[] actual = StringUtil.split(";,", "John,Jane,Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testBasicWithIrishName() {
        final String[] actual = StringUtil.split(";,", "John,Jane O'Roe,Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane O'Roe", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testBasicWithSpaces() {
        final String[] actual = StringUtil.split(";,", "John, Jane, Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testBasicWithSpacesBeforeAndAfter() {
        final String[] actual = StringUtil.split(";,", "John , Jane , Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testQuotedStringWithOne() {
        final String[] actual = StringUtil.split(";,", "\"Doe, John\""); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John" //$NON-NLS-1$
        }, actual);
    }

    public void testQuotedStringWithMany() {
        final String[] actual = StringUtil.split(";,", "\"Doe, John\",\"O'Roe, Jane\""); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John", //$NON-NLS-1$
            "O'Roe, Jane" //$NON-NLS-1$
        }, actual);
    }

    public void testSingleQuotedStringWithOne() {
        final String[] actual = StringUtil.split(";,", "'Doe, John'"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John" //$NON-NLS-1$
        }, actual);
    }

    public void testSingleQuotedStringWithMany() {
        final String[] actual = StringUtil.split(";,", "'Doe, John','Roe, Jane'"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John", //$NON-NLS-1$
            "Roe, Jane" //$NON-NLS-1$
        }, actual);
    }

    public void testNullTextPassed() {
        final String[] actual = StringUtil.split(";,", null); //$NON-NLS-1$

        assertArraysEquals(new String[0], actual);
    }

    public void testNullDelimterThrowsException() {
        try {
            StringUtil.split(null, null);
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testQuoteDelimterThrowsException() {
        try {
            StringUtil.split("\"", "blah blah"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testSingleQuoteDelimterThrowsException() {
        try {
            StringUtil.split("\'", "blah blah"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testPipeDelimter() {
        final String[] actual = StringUtil.split("|", "John|Jane|Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testAsteriskDelimter() {
        final String[] actual = StringUtil.split("*", "John*Jane*Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testSquareBrackerDelimter() {
        final String[] actual = StringUtil.split("]", "John]Jane]Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testNotSymbolDelimter() {
        final String[] actual = StringUtil.split("^;", "John^Jane;Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testHyphenDelimter() {
        final String[] actual = StringUtil.split("-", "John-Jane-Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testRegexArgumentsThrowsException() {
        try {
            StringUtil.split("a-d[m-p]", "blah blah"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testSplitRemoveWithNoEmpties() {
        final String[] actual = StringUtil.splitRemoveEmpties("John\\Jane\\Robinson", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testSplitRemoveWithEmpties() {
        final String[] actual = StringUtil.splitRemoveEmpties("\\John\\\\Jane\\\\Robinson\\", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testToIntConversion() {
        int actual;

        actual = StringUtil.toInt("1"); //$NON-NLS-1$
        assertEquals(actual, 1);

        actual = StringUtil.toInt("-2"); //$NON-NLS-1$
        assertEquals(actual, -2);

        actual = StringUtil.toInt("3K"); //$NON-NLS-1$
        assertEquals(actual, 3 * 1024);

        actual = StringUtil.toInt("4 K"); //$NON-NLS-1$
        assertEquals(actual, 4 * 1024);

        actual = StringUtil.toInt("5k"); //$NON-NLS-1$
        assertEquals(actual, 5 * 1024);

        actual = StringUtil.toInt(" 6  k"); //$NON-NLS-1$
        assertEquals(actual, 6 * 1024);

        actual = StringUtil.toInt("7M"); //$NON-NLS-1$
        assertEquals(actual, 7 * 1024 * 1024);

        actual = StringUtil.toInt("8 M"); //$NON-NLS-1$
        assertEquals(actual, 8 * 1024 * 1024);
    }

    public void testToIntConversionThrowsException() {
        try {
            int actual = StringUtil.toInt("1H"); //$NON-NLS-1$
        } catch (final Exception e) {
            assertTrue("NumberFormatException expected", e instanceof NumberFormatException); //$NON-NLS-1$
            return;
        }

        fail("NumberFormatException expected"); //$NON-NLS-1$
    }

    // --- Private Test Helpers --- //

    /**
     * Helper method for testing array equality.
     */
    private static void assertArraysEquals(final Object[] expected, final Object[] actual) {
        assertEquals("array lengths mismatch!", expected.length, actual.length); //$NON-NLS-1$
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

}
