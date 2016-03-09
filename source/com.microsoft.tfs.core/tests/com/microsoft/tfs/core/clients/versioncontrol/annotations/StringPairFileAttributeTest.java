// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.annotations;

import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.StringPairFileAttribute;

import junit.framework.TestCase;

public final class StringPairFileAttributeTest extends TestCase {
    public void testStringPairFileAttribute() {
        assertEquals('=', StringPairFileAttribute.SEPARATOR);

        StringPairFileAttribute a = new StringPairFileAttribute("name", "value"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("name", a.getName()); //$NON-NLS-1$
        assertEquals("value", a.getValue()); //$NON-NLS-1$
        assertEquals("name=value", a.toString()); //$NON-NLS-1$

        final StringPairFileAttribute b = StringPairFileAttribute.parse("name=value"); //$NON-NLS-1$
        assertEquals("name", a.getName()); //$NON-NLS-1$
        assertEquals("value", a.getValue()); //$NON-NLS-1$
        assertEquals("name=value", a.toString()); //$NON-NLS-1$

        assertEquals(a, b);

        a = StringPairFileAttribute.parse("name="); //$NON-NLS-1$
        assertEquals("name", a.getName()); //$NON-NLS-1$
        assertEquals("", a.getValue()); //$NON-NLS-1$
        assertEquals("name=", a.toString()); //$NON-NLS-1$

        a = StringPairFileAttribute.parse("name"); //$NON-NLS-1$
        assertEquals("name", a.getName()); //$NON-NLS-1$
        assertEquals("", a.getValue()); //$NON-NLS-1$
        assertEquals("name=", a.toString()); //$NON-NLS-1$

        a = StringPairFileAttribute.parse("name=something with an = sign in it"); //$NON-NLS-1$
        assertEquals("name", a.getName()); //$NON-NLS-1$
        assertEquals("something with an = sign in it", a.getValue()); //$NON-NLS-1$
        assertEquals("name=something with an = sign in it", a.toString()); //$NON-NLS-1$

        a = StringPairFileAttribute.parse("=value"); //$NON-NLS-1$
        assertNull(a);

        a = StringPairFileAttribute.parse("="); //$NON-NLS-1$
        assertNull(a);

        a = StringPairFileAttribute.parse(""); //$NON-NLS-1$
        assertNull(a);
    }
}