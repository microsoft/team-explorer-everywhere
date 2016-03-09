// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.listeners;

import java.util.Comparator;

/**
 * {@link Comparators} is a utility class that contains {@link Comparator}
 * implementations that are commonly used with {@link ListenerList}s.
 * {@link ListenerList} implementations (like {@link StandardListenerList})
 * often take a {@link Comparator} instance that is used to compare listeners.
 *
 * @see Comparator
 * @see ListenerList
 * @see StandardListenerList
 */
public class Comparators {
    /**
     * A {@link Comparator} implementation that compares objects using the
     * {@link Object#equals(Object)} method. This is an "equality" comparison.
     */
    public static final Comparator EQUALITY = new EqualityComparator();

    /**
     * A {@link Comparator} implementation that considers objects equal only if
     * they are the exact same object. This is an "identity" comparison.
     */
    public static final Comparator IDENTITY = new IdentityComparator();

    private static class EqualityComparator implements Comparator {
        @Override
        public int compare(final Object o1, final Object o2) {
            if (o1.equals(o2)) {
                return 0;
            }
            return o1.hashCode() - o2.hashCode();
        }
    }

    private static class IdentityComparator implements Comparator {
        @Override
        public int compare(final Object o1, final Object o2) {
            if (o1 == o2) {
                return 0;
            }
            return System.identityHashCode(o1) - System.identityHashCode(o2);
        }
    }
}
