// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.enums;

import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;

import junit.framework.TestCase;

public class BuildServerVersionTest extends TestCase {

    public void testCompareTo() {
        assertTrue(BuildServerVersion.V3.compareTo(BuildServerVersion.V4) < 0);
        assertTrue(BuildServerVersion.V3.compareTo(BuildServerVersion.V3) == 0);
        assertTrue(BuildServerVersion.V4.compareTo(BuildServerVersion.V3) > 0);
    }

    public void testEqualsObject() {
        assertFalse(BuildServerVersion.V3.equals(BuildServerVersion.V4));
        assertFalse(BuildServerVersion.V4.equals(BuildServerVersion.V3));
        assertTrue(BuildServerVersion.V3.equals(BuildServerVersion.V3));
        assertTrue(BuildServerVersion.V4.equals(BuildServerVersion.V4));
    }

    public void testGetVersion() {
        assertEquals(1, BuildServerVersion.V3.getVersion());
        assertEquals(2, BuildServerVersion.V4.getVersion());

        BuildServerVersion.V4.getVersion();
        assertEquals(2, BuildServerVersion.V4.getVersion());

    }

    public void testIsV3() {
        assertTrue(BuildServerVersion.V3.isV3());
        assertFalse(BuildServerVersion.V4.isV3());
    }

    public void testIsV4() {
        assertFalse(BuildServerVersion.V3.isV4());
        assertTrue(BuildServerVersion.V4.isV4());
    }

}
