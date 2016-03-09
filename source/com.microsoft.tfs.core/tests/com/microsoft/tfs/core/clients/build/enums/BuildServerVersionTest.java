// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.enums;

import com.microsoft.tfs.core.clients.build.flags.BuildServerVersion;

import junit.framework.TestCase;

public class BuildServerVersionTest extends TestCase {

    public void testCompareTo() {
        assertTrue(BuildServerVersion.V1.compareTo(BuildServerVersion.V2) < 0);
        assertTrue(BuildServerVersion.V1.compareTo(BuildServerVersion.V1) == 0);
        assertTrue(BuildServerVersion.V2.compareTo(BuildServerVersion.V1) > 0);
    }

    public void testEqualsObject() {
        assertFalse(BuildServerVersion.V1.equals(BuildServerVersion.V2));
        assertFalse(BuildServerVersion.V2.equals(BuildServerVersion.V1));
        assertTrue(BuildServerVersion.V1.equals(BuildServerVersion.V1));
        assertTrue(BuildServerVersion.V2.equals(BuildServerVersion.V2));
    }

    public void testGetVersion() {
        assertEquals(1, BuildServerVersion.V1.getVersion());
        assertEquals(2, BuildServerVersion.V2.getVersion());

        BuildServerVersion.V2.getVersion();
        assertEquals(2, BuildServerVersion.V2.getVersion());

    }

    public void testIsV1() {
        assertTrue(BuildServerVersion.V1.isV1());
        assertFalse(BuildServerVersion.V2.isV1());
    }

    public void testIsV2() {
        assertFalse(BuildServerVersion.V1.isV2());
        assertTrue(BuildServerVersion.V2.isV2());
    }

}
