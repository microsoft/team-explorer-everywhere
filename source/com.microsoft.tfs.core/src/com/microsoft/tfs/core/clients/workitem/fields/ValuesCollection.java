// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import java.util.Iterator;

/**
 * Represents a list of valid values for a {@link Field}.
 *
 * @since TEE-SDK-10.1
 */
public interface ValuesCollection extends Iterable<String> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<String> iterator();

    /**
     * @return the number of objects in this collection.
     */
    public int size();

    /**
     * Gets the item at the specified index.
     *
     * @param index
     *        the index of the item to get
     * @return the item at the specified index
     */
    public String get(int index);

    /**
     * Gets the index of the specified item.
     *
     * @param value
     *        the value of the item to get the index of
     * @return the index of the item in the collection, or -1 if the collection
     *         does not contain the specified item
     */
    public int indexOf(String value);

    /**
     * Tests whether this collection contains the specified item.
     *
     * @param value
     *        the item to find in this collection
     * @return <code>true</code> if the collection contains the specified item,
     *         <code>false</code> if it does not
     */
    public boolean contains(String value);

    /**
     * @return an array of the values in this collection.
     */
    public String[] getValues();
}
