// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * This is a Junit 4 test because assertArrayEquals is useful.
 */
public class ArrayUtilsTest {
    private final static String[] empty = new String[0];
    private final static String[] one = new String[] {
        "one" //$NON-NLS-1$
    };
    private final static String[] two = new String[] {
        "one", //$NON-NLS-1$
        "two" //$NON-NLS-1$
    };
    private final static String[] three = new String[] {
        "one", //$NON-NLS-1$
        "two", //$NON-NLS-1$
        "baz" //$NON-NLS-1$
    };

    private final static String[] oneNull = new String[] {
        null
    };

    @Test
    public void testConcatSingle() {
        // use null a
        assertArrayEquals(null, ArrayUtils.concat(null, (String) null));
        assertArrayEquals(one, ArrayUtils.concat(null, "one")); //$NON-NLS-1$

        // add null b
        assertArrayEquals(empty, ArrayUtils.concat(empty, (String) null));
        assertArrayEquals(one, ArrayUtils.concat(one, (String) null));

        // add one b
        assertArrayEquals(one, ArrayUtils.concat(empty, "one")); //$NON-NLS-1$
        assertArrayEquals(two, ArrayUtils.concat(one, "two")); //$NON-NLS-1$
        assertArrayEquals(three, ArrayUtils.concat(two, "baz")); //$NON-NLS-1$
    }

    public void testConcatArray() {
        // use null a
        assertArrayEquals(null, ArrayUtils.concat(null, (String[]) null));
        assertArrayEquals(empty, ArrayUtils.concat(null, empty));
        assertArrayEquals(empty, ArrayUtils.concat(null, one));
        assertArrayEquals(two, ArrayUtils.concat(null, two));

        // use empty a
        assertArrayEquals(one, ArrayUtils.concat(empty, one));
        assertArrayEquals(two, ArrayUtils.concat(empty, two));
        assertArrayEquals(three, ArrayUtils.concat(empty, three));

        // add null b
        assertArrayEquals(empty, ArrayUtils.concat(empty, (String[]) null));
        assertArrayEquals(one, ArrayUtils.concat(one, (String[]) null));

        // add empty b
        assertArrayEquals(empty, ArrayUtils.concat(empty, empty));
        assertArrayEquals(one, ArrayUtils.concat(one, empty));
        assertArrayEquals(two, ArrayUtils.concat(two, empty));

        // preserve null elements
        assertArrayEquals(oneNull, ArrayUtils.concat(null, oneNull));
        assertArrayEquals(oneNull, ArrayUtils.concat(oneNull, null));

        // free form
        assertArrayEquals(new String[] {
            "one", //$NON-NLS-1$
            "one" //$NON-NLS-1$
        }, ArrayUtils.concat(one, one));

        assertArrayEquals(new String[] {
            "one", //$NON-NLS-1$
            "one", //$NON-NLS-1$
            "two" //$NON-NLS-1$
        }, ArrayUtils.concat(one, two));

        assertArrayEquals(new String[] {
            "one", //$NON-NLS-1$
            null
        }, ArrayUtils.concat(one, oneNull));
    }
}
