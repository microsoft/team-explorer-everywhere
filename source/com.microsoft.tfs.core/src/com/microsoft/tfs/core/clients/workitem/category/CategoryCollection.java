// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.category;

import java.util.Iterator;

/**
 * A collection of {@link Category}s.
 *
 * @since TEE-SDK-10.1
 */
public interface CategoryCollection extends Iterable<Category> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<Category> iterator();

    /**
     * @return the number of {@link Category}s in the collection.
     */
    public int size();

    /**
     * Gets the {@link Category} object at the specified index in this
     * collection.
     *
     * @param index
     *        The index of the desired {@link Category}
     * @return The {@link Category} at the specified index in this collection.
     */
    public Category get(int index);

    /**
     * Gets the {@link Category} in this collection with the specified reference
     * name.
     *
     * @param categoryReferenceName
     *        The reference name of the {@link Category} to get.
     * @return The {@link Category} that has the specified name.
     */
    public Category get(String categoryReferenceName);

    /**
     * Checks whether this collection contains a {@link Category} that has the
     * specified name.
     *
     * @param categoryReferenceName
     *        The reference name of the {@link Category} of interest.
     * @return True if a {@link Category} that has the reference specified name
     *         exists in this collection; otherwise, false.
     */
    public boolean contains(String categoryReferenceName);
}
