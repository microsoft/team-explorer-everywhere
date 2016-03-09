// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import junit.framework.TestCase;

public class PagedCollectionTest extends TestCase {
    private MockPageCallback callback;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        callback = new MockPageCallback();
    }

    private PagedCollection newCollection(final int totalSize, final int pageSize) {
        return new PagedCollection(totalSize, pageSize, callback);
    }

    public void testEdgeOfPage() {
        final PagedCollection collection = newCollection(10, 5);

        assertNotNull(collection.getItem(5));
        assertTrue(callback.isPageCalled());
        assertEquals(5, callback.getLastStartingIx());
        assertEquals(5, callback.getLastLength());
    }

    public void testSmallerFinalPage() {
        final PagedCollection collection = newCollection(10, 3);

        assertNotNull(collection.getItem(9));
        assertTrue(callback.isPageCalled());
        assertEquals(9, callback.getLastStartingIx());
        assertEquals(1, callback.getLastLength());

        callback.reset();

        assertNotNull(collection.getItem(8));
        assertTrue(callback.isPageCalled());
        assertEquals(6, callback.getLastStartingIx());
        assertEquals(3, callback.getLastLength());
    }

    public void testNormalOperation() {
        final PagedCollection collection = newCollection(10, 5);

        assertNotNull(collection.getItem(2));
        assertTrue(callback.isPageCalled());
        assertEquals(0, callback.getLastStartingIx());
        assertEquals(5, callback.getLastLength());

        callback.reset();

        assertNotNull(collection.getItem(3));
        assertFalse(callback.isPageCalled());

        callback.reset();

        assertNotNull(collection.getItem(7));
        assertTrue(callback.isPageCalled());
        assertEquals(5, callback.getLastStartingIx());
        assertEquals(5, callback.getLastLength());
    }

    public void testEmptyCollection() {
        final PagedCollection collection = newCollection(0, 0);

        assertEquals(0, collection.getSize());
        try {
            collection.getItem(0);
            fail();
        } catch (final IllegalArgumentException ex) {

        }

        assertFalse(callback.isPageCalled());
    }

    private static class MockPageCallback implements PageCallback {
        private boolean pageCalled = false;
        private int lastStartingIx = -1;
        private int lastLength = -1;

        public void reset() {
            pageCalled = false;
            lastStartingIx = -1;
            lastLength = -1;
        }

        @Override
        public Object[] pageInItems(final int startingIx, final int length) {
            pageCalled = true;
            lastStartingIx = startingIx;
            lastLength = length;

            final Object[] results = new Object[length];
            for (int i = 0; i < length; i++) {
                results[i] = new Object();
            }
            return results;
        }

        public int getLastLength() {
            return lastLength;
        }

        public int getLastStartingIx() {
            return lastStartingIx;
        }

        public boolean isPageCalled() {
            return pageCalled;
        }
    }
}
