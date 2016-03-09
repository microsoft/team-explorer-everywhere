// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.revision;

import java.util.Iterator;

/**
 * Represents the revision history of a work item.
 *
 * @since TEE-SDK-10.1
 */
public interface RevisionCollection extends Iterable<Revision> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<Revision> iterator();

    /**
     * @return the number of {@link Revision}s in the collection.
     */
    public int size();

    /**
     * Gets a work item {@link Revision} at the specified index.
     *
     * @param index
     *        The index of the desired {@link Revision}
     * @return the work item {@link Revision} at the specified index.
     */
    public Revision get(int index);
}
