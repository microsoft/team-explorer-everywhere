// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import junit.framework.TestCase;

public class StandardListenerListTest extends TestCase {
    private StandardListenerList list;

    @Override
    public void setName(final String name) {
        super.setName(name);
        list = new StandardListenerList();
    }

    public void testAdd() {
        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        assertEquals(0, list.size());
        assertTrue(list.addListener(l1));
        assertEquals(1, list.size());

        assertFalse(list.addListener(l1));
        assertEquals(1, list.size());

        assertTrue(list.addListener(l2));
        assertEquals(2, list.size());

        final Object[] listeners = list.getListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.length);
    }

    public void testRemove() {
        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        list.addListener(l1);

        assertFalse(list.removeListener(l2));
        assertEquals(1, list.size());
        assertTrue(list.removeListener(l1));
        assertEquals(0, list.size());

        list.addListener(l1);
        list.addListener(l2);

        assertTrue(list.removeListener(l2));
        assertTrue(list.removeListener(l1));
        assertEquals(0, list.size());
    }

    public void testContains() {
        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        assertFalse(list.containsListener(l1));

        list.addListener(l1);

        assertTrue(list.containsListener(l1));
        assertFalse(list.containsListener(l2));

        list.addListener(l2);

        assertTrue(list.containsListener(l1));
        assertTrue(list.containsListener(l2));

        list.removeListener(l1);
        list.removeListener(l2);

        assertFalse(list.containsListener(l1));
        assertFalse(list.containsListener(l2));
    }

    public void testClear() {
        assertFalse(list.clear());

        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        list.addListener(l1);
        list.addListener(l2);

        assertTrue(list.clear());
        assertEquals(0, list.size());
    }

    public void testGetListeners() {
        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        list.addListener(l1);
        list.addListener(l2);

        final Object[] o1 = list.getListeners();
        final Object[] o2 = list.getListeners(new Listener[list.size()]);

        assertEquals(2, o1.length);
        assertEquals(2, o2.length);

        assertTrue(o2 instanceof Listener[]);

        assertTrue(o1[0] == l1 || o1[1] == l1);
        assertTrue(o1[0] == l2 || o1[1] == l2);
        assertTrue(o2[0] == l1 || o2[1] == l1);
        assertTrue(o2[0] == l2 || o2[1] == l2);
    }

    public void testForeach() {
        final Listener l1 = new Listener();
        final Listener l2 = new Listener();

        list.addListener(l1);
        list.addListener(l2);

        list.foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((Listener) listener).onEvent();
                return true;
            }
        });

        assertEquals(1, l1.notifyCount);
        assertEquals(1, l2.notifyCount);
    }

    private static class Listener {
        public int notifyCount = 0;

        public void onEvent() {
            ++notifyCount;
        }
    }
}
