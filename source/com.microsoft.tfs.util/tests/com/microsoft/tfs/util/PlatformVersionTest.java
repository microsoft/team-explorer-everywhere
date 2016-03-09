// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import junit.framework.TestCase;

public class PlatformVersionTest extends TestCase {
    public void testParser() {
        assertEquals(PlatformVersion.parseVersionNumber("1"), new int[] //$NON-NLS-1$
        {
            1
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4"), new int[] //$NON-NLS-1$
        {
            10,
            4
        });

        assertEquals(PlatformVersion.parseVersionNumber("10_4-21"), new int[] //$NON-NLS-1$
        {
            10,
            4,
            21
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.21"), new int[] //$NON-NLS-1$
        {
            10,
            4,
            21
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.21Q"), new int[] //$NON-NLS-1$
        {
            10,
            4,
            21
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.Q"), new int[] //$NON-NLS-1$
        {
            10,
            4
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.Q.2"), new int[] //$NON-NLS-1$
        {
            10,
            4
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.Q21"), new int[] //$NON-NLS-1$
        {
            10,
            4
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.Q21.2"), new int[] //$NON-NLS-1$
        {
            10,
            4
        });

        assertEquals(PlatformVersion.parseVersionNumber("10.4.Q21.Z.2"), new int[] //$NON-NLS-1$
        {
            10,
            4
        });
    }

    public void testCompare() {
        final String originalVersion = System.getProperty("os.version"); //$NON-NLS-1$
        System.setProperty("os.version", "10.4.21"); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(PlatformVersion.compareTo("10.4.21") == 0); //$NON-NLS-1$
        assertTrue(PlatformVersion.compareTo("10.4.21Q") == 0); //$NON-NLS-1$
        assertTrue(PlatformVersion.compareTo("10.4.21.21") < 0); //$NON-NLS-1$
        assertTrue(PlatformVersion.compareTo("10.4") > 0); //$NON-NLS-1$
        assertTrue(PlatformVersion.compareTo("10.4.21.0") == 0); //$NON-NLS-1$
        assertTrue(PlatformVersion.compareTo("10.4.21.0.0.0.0.Q") == 0); //$NON-NLS-1$
        assertTrue(PlatformVersion.compareTo("010.4.21") == 0); //$NON-NLS-1$

        System.setProperty("os.version", originalVersion); //$NON-NLS-1$
    }

    public void testHelperMethods() {
        final String originalVersion = System.getProperty("os.version"); //$NON-NLS-1$
        System.setProperty("os.version", "10.4.21"); //$NON-NLS-1$ //$NON-NLS-2$

        assertTrue(PlatformVersion.isCurrentVersion("10.4.21.0")); //$NON-NLS-1$
        assertTrue(PlatformVersion.isGreaterThanVersion("10.4.20.1441")); //$NON-NLS-1$
        assertTrue(PlatformVersion.isGreaterThanOrEqualToVersion("10.4.21")); //$NON-NLS-1$
        assertTrue(PlatformVersion.isGreaterThanOrEqualToVersion("10.4.20.21")); //$NON-NLS-1$
        assertTrue(PlatformVersion.isLessThanVersion("10.5")); //$NON-NLS-1$
        assertTrue(PlatformVersion.isLessThanOrEqualToVersion("10.4.21")); //$NON-NLS-1$

        System.setProperty("os.version", originalVersion); //$NON-NLS-1$
    }

    private static final void assertEquals(final int[] one, final int[] two) {
        assertNotNull(one);
        assertNotNull(two);

        assertEquals(one.length, two.length);

        for (int i = 0; i < one.length; i++) {
            assertEquals(one[i], two[i]);
        }
    }
}
