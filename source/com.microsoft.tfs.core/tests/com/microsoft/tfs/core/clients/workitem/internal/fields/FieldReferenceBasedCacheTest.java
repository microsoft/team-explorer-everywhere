// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import junit.framework.TestCase;

public class FieldReferenceBasedCacheTest extends TestCase {
    private FieldReferenceBasedCache<Object> cache;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = new FieldReferenceBasedCache<Object>();
    }

    public void testBasicOperation() {
        assertEquals(0, cache.size());
        assertEquals(0, cache.values().size());

        final Object o1 = new Object();

        cache.put(o1, "DisplayName", "Reference.Name", 1); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(1, cache.size());

        assertEquals(o1, cache.get("DisplayName")); //$NON-NLS-1$
        assertEquals(o1, cache.get("Reference.Name")); //$NON-NLS-1$
        assertEquals(o1, cache.get(1));

        assertNull(cache.get("dummy value")); //$NON-NLS-1$
        assertNull(cache.get(2));

        assertEquals(1, cache.values().size());
        assertTrue(cache.values().contains(o1));

        cache.clear();

        assertEquals(0, cache.size());
        assertEquals(0, cache.values().size());

        assertNull(cache.get("DisplayName")); //$NON-NLS-1$
        assertNull(cache.get("Reference.Name")); //$NON-NLS-1$
        assertNull(cache.get(1));
    }

    public void testValuesUnmodifiable() {
        final Object o1 = new Object();

        cache.put(o1, "DisplayName", "Reference.Name", 1); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            cache.values().remove(o1);
            fail();
        } catch (final UnsupportedOperationException ex) {
        }
    }

    public void testBadValues() {
        try {
            cache.put(new Object(), "", "reference.name", 1); //$NON-NLS-1$ //$NON-NLS-2$
            fail();
        } catch (final IllegalArgumentException ex) {
        }

        try {
            cache.put(new Object(), "displayname", "badreferencename", 1); //$NON-NLS-1$ //$NON-NLS-2$
            fail();
        } catch (final IllegalArgumentException ex) {
        }
    }

    public void testCaseSensitivity() {
        final Object o1 = new Object();

        cache.put(o1, "DisplayName", "Reference.Name", 1); //$NON-NLS-1$ //$NON-NLS-2$

        assertEquals(o1, cache.get("DisplayName", true, true, true)); //$NON-NLS-1$
        assertEquals(o1, cache.get("DisplayName", true, true, false)); //$NON-NLS-1$
        assertEquals(null, cache.get("displayname", true, true, true)); //$NON-NLS-1$
        assertEquals(o1, cache.get("displayname", true, true, false)); //$NON-NLS-1$
    }

    public void testReferenceNameVsDisplayName() {
        final Object o1 = new Object();

        cache.put(o1, "DisplayName", "Reference.Name", 1); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            cache.get("DisplayName", true, false, true); //$NON-NLS-1$
            fail();
        } catch (final IllegalArgumentException ex) {
        }

        assertEquals(o1, cache.get("DisplayName", false, true, true)); //$NON-NLS-1$
        assertEquals(o1, cache.get("DisplayName", true, true, true)); //$NON-NLS-1$

        assertEquals(null, cache.get("Reference.Name", false, true, true)); //$NON-NLS-1$
        assertEquals(o1, cache.get("Reference.Name", true, false, true)); //$NON-NLS-1$
        assertEquals(o1, cache.get("Reference.Name", true, true, true)); //$NON-NLS-1$
    }
}
