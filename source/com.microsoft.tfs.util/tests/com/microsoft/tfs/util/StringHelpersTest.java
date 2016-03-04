// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import junit.framework.TestCase;

public class StringHelpersTest extends TestCase {
    public void testSingle() {
        final String[] actual = StringHelpers.split(";,", "John"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John" //$NON-NLS-1$
        }, actual);
    }

    public void testBasic() {
        final String[] actual = StringHelpers.split(";,", "John,Jane,Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testBasicWithIrishName() {
        final String[] actual = StringHelpers.split(";,", "John,Jane O'Roe,Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane O'Roe", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testBasicWithSpaces() {
        final String[] actual = StringHelpers.split(";,", "John, Jane, Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testBasicWithSpacesBeforeAndAfter() {
        final String[] actual = StringHelpers.split(";,", "John , Jane , Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testQuotedStringWithOne() {
        final String[] actual = StringHelpers.split(";,", "\"Doe, John\""); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John" //$NON-NLS-1$
        }, actual);
    }

    public void testQuotedStringWithMany() {
        final String[] actual = StringHelpers.split(";,", "\"Doe, John\",\"O'Roe, Jane\""); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John", //$NON-NLS-1$
            "O'Roe, Jane" //$NON-NLS-1$
        }, actual);
    }

    public void testSingleQuotedStringWithOne() {
        final String[] actual = StringHelpers.split(";,", "'Doe, John'"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John" //$NON-NLS-1$
        }, actual);
    }

    public void testSingleQuotedStringWithMany() {
        final String[] actual = StringHelpers.split(";,", "'Doe, John','Roe, Jane'"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "Doe, John", //$NON-NLS-1$
            "Roe, Jane" //$NON-NLS-1$
        }, actual);
    }

    public void testNullTextPassed() {
        final String[] actual = StringHelpers.split(";,", null); //$NON-NLS-1$

        assertArraysEquals(new String[0], actual);
    }

    public void testNullDelimterThrowsException() {
        try {
            StringHelpers.split(null, null);
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testQuoteDelimterThrowsException() {
        try {
            StringHelpers.split("\"", "blah blah"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testSingleQuoteDelimterThrowsException() {
        try {
            StringHelpers.split("\'", "blah blah"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testPipeDelimter() {
        final String[] actual = StringHelpers.split("|", "John|Jane|Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testAsteriskDelimter() {
        final String[] actual = StringHelpers.split("*", "John*Jane*Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testSquareBrackerDelimter() {
        final String[] actual = StringHelpers.split("]", "John]Jane]Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testNotSymbolDelimter() {
        final String[] actual = StringHelpers.split("^;", "John^Jane;Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testHyphenDelimter() {
        final String[] actual = StringHelpers.split("-", "John-Jane-Robinson"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testRegexArgumentsThrowsException() {
        try {
            StringHelpers.split("a-d[m-p]", "blah blah"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final Exception e) {
            assertTrue("IllegalArgumentException expected", e instanceof IllegalArgumentException); //$NON-NLS-1$
            return;
        }
        fail("IllegalArgumentException expected"); //$NON-NLS-1$
    }

    public void testSplitRemoveWithNoEmpties() {
        final String[] actual = StringHelpers.splitRemoveEmpties("John\\Jane\\Robinson", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
    }

    public void testSplitRemoveWithEmpties() {
        final String[] actual = StringHelpers.splitRemoveEmpties("\\John\\\\Jane\\\\Robinson\\", "\\\\"); //$NON-NLS-1$ //$NON-NLS-2$

        assertArraysEquals(new String[] {
            "John", //$NON-NLS-1$
            "Jane", //$NON-NLS-1$
            "Robinson" //$NON-NLS-1$
        }, actual);
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
