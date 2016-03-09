// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.annotations;

import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.BooleanFileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeNames;

import junit.framework.TestCase;

public final class BooleanFileAttributeTest extends TestCase {
    public void testBooleanFileAttribute() {
        final BooleanFileAttribute a = new BooleanFileAttribute(FileAttributeNames.EXECUTABLE);
        assertEquals(FileAttributeNames.EXECUTABLE, a.getName());

        final BooleanFileAttribute b = BooleanFileAttribute.parse(FileAttributeNames.EXECUTABLE);
        assertEquals(FileAttributeNames.EXECUTABLE, b.getName());
        assertEquals(FileAttributeNames.EXECUTABLE, b.toString());

        assertTrue(a.equals(b));
    }
}
