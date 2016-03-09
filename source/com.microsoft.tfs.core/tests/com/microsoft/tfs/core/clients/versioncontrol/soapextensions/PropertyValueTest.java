// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import junit.framework.TestCase;

public class PropertyValueTest extends TestCase {
    public void testEqualsNull() {
        final PropertyValue ab = new PropertyValue("a", "b"); //$NON-NLS-1$ //$NON-NLS-2$

        assertFalse(ab.equals(null));
        assertFalse(ab.equals(new Object()));
    }

    public void testEqualsString() {
        // Same name, different values
        final PropertyValue ab1 = new PropertyValue("a", "b"); //$NON-NLS-1$ //$NON-NLS-2$
        final PropertyValue ab2 = new PropertyValue("a", "b"); //$NON-NLS-1$ //$NON-NLS-2$
        final PropertyValue aB1 = new PropertyValue("a", "B"); //$NON-NLS-1$ //$NON-NLS-2$
        final PropertyValue ac = new PropertyValue("a", "c"); //$NON-NLS-1$ //$NON-NLS-2$
        final PropertyValue aNull = new PropertyValue("a", null); //$NON-NLS-1$

        assertTrue(ab1.equals(ab1));
        assertTrue(ab1.equals(ab2));
        assertFalse(ab1.equals(aB1));
        assertFalse(ab1.equals(ac));
        assertFalse(ab1.equals(aNull));

        // Different name
        final PropertyValue xb1 = new PropertyValue("x", "b"); //$NON-NLS-1$//$NON-NLS-2$
        assertFalse(ab1.equals(xb1));
    }

    public void testEqualsInteger() {
        // Same name, different values
        final PropertyValue aOne1 = new PropertyValue("a", Integer.valueOf(1)); //$NON-NLS-1$
        final PropertyValue aOne2 = new PropertyValue("a", Integer.valueOf(1)); //$NON-NLS-1$
        final PropertyValue aTwo1 = new PropertyValue("a", Integer.valueOf(2)); //$NON-NLS-1$
        final PropertyValue aLongOne1 = new PropertyValue("a", Long.valueOf(1)); //$NON-NLS-1$
        final PropertyValue aNull = new PropertyValue("a", null); //$NON-NLS-1$

        assertTrue(aOne1.equals(aOne1));
        assertTrue(aOne1.equals(aOne2));
        assertFalse(aOne1.equals(aTwo1));
        assertFalse(aOne1.equals(aLongOne1));
        assertFalse(aOne1.equals(aNull));
        assertFalse(aOne1.equals("Not an integer")); //$NON-NLS-1$
        assertFalse(aOne1.equals(aOne1.toString()));
    }

    public void testEqualsDouble() {
        // Same name, different values
        final PropertyValue aOne1 = new PropertyValue("a", Double.valueOf(1)); //$NON-NLS-1$
        final PropertyValue aOne2 = new PropertyValue("a", Double.valueOf(1)); //$NON-NLS-1$
        final PropertyValue aTwo1 = new PropertyValue("a", Double.valueOf(2)); //$NON-NLS-1$
        final PropertyValue aFloatOne1 = new PropertyValue("a", Float.valueOf(1)); //$NON-NLS-1$
        final PropertyValue aNull = new PropertyValue("a", null); //$NON-NLS-1$

        assertTrue(aOne1.equals(aOne1));
        assertTrue(aOne1.equals(aOne2));
        assertFalse(aOne1.equals(aTwo1));
        assertFalse(aOne1.equals(aFloatOne1));
        assertFalse(aOne1.equals(aNull));
        assertFalse(aOne1.equals("Not a double")); //$NON-NLS-1$
        assertFalse(aOne1.equals(aOne1.toString()));
    }

    public void testEqualsCalendar() throws InterruptedException {
        final Calendar c1 = Calendar.getInstance();
        Thread.sleep(100);
        final Calendar c2 = Calendar.getInstance();

        // Same name, different values
        final PropertyValue cOne1 = new PropertyValue("a", c1); //$NON-NLS-1$
        final PropertyValue cOne2 = new PropertyValue("a", c1); //$NON-NLS-1$
        final PropertyValue cTwo1 = new PropertyValue("a", c2); //$NON-NLS-1$
        final PropertyValue aNull = new PropertyValue("a", null); //$NON-NLS-1$

        assertTrue(cOne1.equals(cOne1));
        assertTrue(cOne1.equals(cOne2));
        assertFalse(cOne1.equals(cTwo1));
        assertFalse(cOne1.equals(aNull));
        assertFalse(cOne1.equals("Not a calendar")); //$NON-NLS-1$
        assertFalse(cOne1.equals(c1.toString()));
    }

    public void testEqualsByteArray() {
        final byte[] b1 = new byte[] {
            0,
            1,
            2
        };
        final byte[] b2 = new byte[] {
            7,
            8,
            9
        };

        // Same name, different values
        final PropertyValue bOne1 = new PropertyValue("a", b1); //$NON-NLS-1$
        final PropertyValue bOne2 = new PropertyValue("a", b1.clone()); //$NON-NLS-1$
        final PropertyValue cTwo1 = new PropertyValue("a", b2); //$NON-NLS-1$
        final PropertyValue aNull = new PropertyValue("a", null); //$NON-NLS-1$

        assertTrue(bOne1.equals(bOne1));
        assertTrue(bOne1.equals(bOne2));
        assertFalse(bOne1.equals(cTwo1));
        assertFalse(bOne1.equals(aNull));
        assertFalse(bOne1.equals("Not a calendar")); //$NON-NLS-1$
        assertFalse(bOne1.equals(b1.toString()));
    }
}
