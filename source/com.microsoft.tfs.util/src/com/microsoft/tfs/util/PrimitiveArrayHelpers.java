// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A collection of static convenience methods for working with primitive Arrays.
 * These methods provide analogs to methods provided in the standard Java
 * library that only work with Object arrays.
 */
public class PrimitiveArrayHelpers {
    /**
     * <p>
     * Converts the given Collection to a primitive array of the specified type.
     * This method is analogous to Collection.toArray(Object[]), but that method
     * will only convert to an Object-based array, not a primitive array.
     * </p>
     * <p>
     * The items held in the collection must all be able to be coerced to the
     * specified primitive type. If any of them are not, an
     * IllegalArgumentException will be thrown.
     * </p>
     *
     * @param collection
     *        the Collection to convert - must not be null
     * @param componentType
     *        - the primitive type of the returned array - must not be null
     * @return a primitive array of the requested type
     */
    public static Object toArray(final Collection collection, final Class componentType) {
        Check.notNull(collection, "collection"); //$NON-NLS-1$
        Check.notNull(componentType, "componentType"); //$NON-NLS-1$

        if (!componentType.isPrimitive()) {
            throw new IllegalArgumentException("the specified component type [" //$NON-NLS-1$
                + componentType.getName()
                + "] is not primitive"); //$NON-NLS-1$
        }

        final Object array = Array.newInstance(componentType, collection.size());
        int ix = 0;
        for (final Iterator it = collection.iterator(); it.hasNext();) {
            final Object currentComponent = it.next();
            Array.set(array, ix++, currentComponent);
        }
        return array;
    }

    /**
     * <p>
     * An analog of Arrays.asList(Object[]) for primitive arrays. Unlike
     * Arrays.asList, the List returned from this method does not write through
     * to the array.
     * </p>
     * <p>
     * The returned List will contain non-primitive elements that correspond to
     * the primitive type of the array. For example, an input array of int[]
     * will produce a List containing Integers.
     * </p>
     *
     * @param primitiveArray
     *        the primitive array to convert to a list
     * @return a List as described above
     */
    public static List asList(final Object primitiveArray) {
        Check.notNull(primitiveArray, "primitiveArray"); //$NON-NLS-1$

        if (!primitiveArray.getClass().isArray() || !primitiveArray.getClass().getComponentType().isPrimitive()) {
            throw new IllegalArgumentException("the input argument is not a primitive array"); //$NON-NLS-1$
        }

        final int size = Array.getLength(primitiveArray);

        final List list = new ArrayList(size);

        for (int i = 0; i < size; i++) {
            final Object obj = Array.get(primitiveArray, i);
            list.add(obj);
        }

        return list;
    }
}
