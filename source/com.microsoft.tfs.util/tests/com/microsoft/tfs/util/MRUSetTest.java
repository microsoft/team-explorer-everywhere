// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.Arrays;
import java.util.Iterator;

import junit.framework.TestCase;

public class MRUSetTest extends TestCase {
    public void testMRUSet() {
        assertTrue("new set of any size should be empty", new MRUSet(1).isEmpty()); //$NON-NLS-1$
        assertTrue("new set of any size should be empty", new MRUSet(Integer.MAX_VALUE).isEmpty()); //$NON-NLS-1$

        try {
            assertTrue(new MRUSet(0).isEmpty());
            assertTrue("should fail for < 1 max size", false); //$NON-NLS-1$
        } catch (final IllegalArgumentException e) {
        }
    }

    public void testAdd() {
        // Test stays at size 1
        MRUSet set = new MRUSet(1);
        set.add("a"); //$NON-NLS-1$
        set.add("b"); //$NON-NLS-1$
        assertEquals(1, set.size());
        Iterator<String> i = set.iterator();
        assertEquals("a should have been discarded", "b", i.next()); //$NON-NLS-1$ //$NON-NLS-2$

        // Test stays at size 2
        set = new MRUSet(2);
        set.add("a"); //$NON-NLS-1$
        set.add("b"); //$NON-NLS-1$
        assertEquals(2, set.size());
        i = set.iterator();
        assertEquals("a should be first", "a", i.next()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("b should be second", "b", i.next()); //$NON-NLS-1$ //$NON-NLS-2$

        set.add("c"); //$NON-NLS-1$
        assertEquals(2, set.size());
        i = set.iterator();
        assertEquals("b should be first", "b", i.next()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("c should be second", "c", i.next()); //$NON-NLS-1$ //$NON-NLS-2$

        // Test return value
        set = new MRUSet(2);
        assertTrue("adding a changes size", set.add("a")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse("adding same a should not change set", set.add("a")); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue("adding b changes size", set.add("b")); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse("adding same b should not change set", set.add("b")); //$NON-NLS-1$ //$NON-NLS-2$
        // another b at end won't modify
        assertFalse("adding another b at should not change set", set.add("b")); //$NON-NLS-1$ //$NON-NLS-2$
        // moving a to end will modify
        assertTrue("adding a changes its position", set.add("a")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testAddAll() {
        MRUSet set = new MRUSet(2);
        set.addAll(Arrays.asList(new String[] {
            "a", //$NON-NLS-1$
            "b" //$NON-NLS-1$
        }));
        assertEquals(2, set.size());
        Iterator<String> i = set.iterator();
        assertEquals("a should be first", "a", i.next()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("b should be first", "b", i.next()); //$NON-NLS-1$ //$NON-NLS-2$

        // Overflow
        set = new MRUSet(2);
        set.addAll(Arrays.asList(new String[] {
            "a", //$NON-NLS-1$
            "b", //$NON-NLS-1$
            "c" //$NON-NLS-1$
        }));
        assertEquals(2, set.size());
        i = set.iterator();
        assertEquals("b should be first", "b", i.next()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("c should be second", "c", i.next()); //$NON-NLS-1$ //$NON-NLS-2$

        // Underflow
        set = new MRUSet(2);
        set.addAll(Arrays.asList(new String[] {
            "a", //$NON-NLS-1$
        }));
        assertEquals(1, set.size());
        i = set.iterator();
        assertEquals("a should be first", "a", i.next()); //$NON-NLS-1$ //$NON-NLS-2$

        // Test return value
        set = new MRUSet(2);
        assertTrue("adding a should change set", set.addAll(Arrays.asList(new String[] //$NON-NLS-1$
        {
            "a", //$NON-NLS-1$
        })));
        assertFalse("adding another a should not change set", set.addAll(Arrays.asList(new String[] //$NON-NLS-1$
        {
            "a", //$NON-NLS-1$
        })));
        assertTrue("adding a,b should change set", set.addAll(Arrays.asList(new String[] //$NON-NLS-1$
        {
            "a", //$NON-NLS-1$
            "b", //$NON-NLS-1$
        })));
        assertFalse("adding another a,b should not change set", set.addAll(Arrays.asList(new String[] //$NON-NLS-1$
        {
            "a", //$NON-NLS-1$
            "b", //$NON-NLS-1$
        })));
        assertTrue("adding b,a (different order) should change set", set.addAll(Arrays.asList(new String[] //$NON-NLS-1$
        {
            "b", //$NON-NLS-1$
            "a", //$NON-NLS-1$
        })));
    }
}
