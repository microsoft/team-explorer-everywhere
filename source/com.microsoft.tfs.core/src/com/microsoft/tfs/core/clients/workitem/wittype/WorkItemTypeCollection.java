// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.wittype;

import java.util.Iterator;

/**
 * Represents a collection of {@link WorkItemType} objects.
 *
 * @since TEE-SDK-10.1
 */
public interface WorkItemTypeCollection extends Iterable<WorkItemType> {
    /**
     * @return an iterator for this collection
     */
    @Override
    public Iterator<WorkItemType> iterator();

    /**
     * @return the number of {@link WorkItemType}s in the collection.
     */
    public int size();

    /**
     * Gets a {@link WorkItemType} by name.
     *
     * @param workItemTypeName
     *        the name of the {@link WorkItemType} to get (must not be
     *        <code>null</code>)
     * @return the {@link WorkItemType} for the specified name
     */
    public WorkItemType get(String workItemTypeName);

    /**
     * Gets a {@link WorkItemType} by ID.
     *
     *
     * @param workItemTypeID
     *        the ID of the {@link WorkItemType} to get
     * @return the {@link WorkItemType} for the specified ID
     */
    public WorkItemType get(int workItemTypeID);

    /**
     * @return an array of all {@link WorkItemType}s
     */
    public WorkItemType[] getTypes();
}
