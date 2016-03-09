// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * An order-preserving collection of strings which never exceeds a fixed size.
 * Newly added items go at the end of the set. If adding a new item would exceed
 * the max size, the first item in the set is discarded.
 *
 * @threadsafety unknown
 */
public class MRUSet extends LinkedHashSet<String> {
    private static final long serialVersionUID = -7846787297390071357L;

    private final int maxSize;

    /**
     * Constructs and {@link MRUSet} which can hold at most maxSize elements
     * without tossing old ones out.
     *
     * @param maxSize
     *        the maximum capacity of this set. adding more items causes the
     *        least-recently added items to be removed from the set. Must be > 0
     */
    public MRUSet(final int maxSize) {
        super();

        Check.isTrue(maxSize > 0, "maxSize > 0"); //$NON-NLS-1$
        this.maxSize = maxSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(final String element) {
        Check.notNull(element, "element"); //$NON-NLS-1$

        /*
         * If the item already existed, remove it and re-add to push it to the
         * end of the collection. The linear set walk isn't good for
         * scalability, but it lets us detect modification to order correctly
         * (testing remove()'s return value alone can't).
         */
        final int index = indexOf(element);
        if (index != -1) {
            remove(element);
            super.add(element);

            // True (modified) if the old index was not the last item (it moved)
            return index != size() - 1;
        }

        /*
         * Remove the first item if we need the space.
         */
        if (size() >= maxSize) {
            final Iterator<String> iterator = iterator();
            remove(iterator.next());
        }

        super.add(element);
        return true;
    }

    /**
     * Gets the index of the specified element.
     *
     * @param element
     *        the element to get the index of (must not be <code>null</code>)
     * @return the index of the specified element or -1 if it was not contained
     *         in this set
     */
    private int indexOf(final String element) {
        Check.notNull(element, "element"); //$NON-NLS-1$

        int pos = 0;
        final Iterator<String> i = iterator();
        while (i.hasNext()) {
            if (element.equals(i.next())) {
                return pos;
            }

            pos++;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(final Collection<? extends String> c) {
        Check.notNull(c, "c"); //$NON-NLS-1$

        /*
         * Not very efficient for computing return value, but a simple way to
         * test for order preservation across individual adds (which may reorder
         * individually but end up in the same order when finished).
         *
         * We cannot simply test for any add() which returns true because a
         * sequence of additions may individually reorder the set but
         * ultimiately leave it in its initial order.
         */
        final String[] oldValues = toArray(new String[size()]);

        final Iterator<? extends String> iterator = c.iterator();
        while (iterator.hasNext()) {
            add(iterator.next());
        }

        if (oldValues.length != size()) {
            return true;
        }

        return Arrays.equals(oldValues, toArray(new String[size()])) == false;
    }
}
