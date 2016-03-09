// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import java.util.Iterator;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * Represents a collection of {@link Field}s in a {@link WorkItem} object.
 *
 * @since TEE-SDK-10.1
 */
public interface FieldCollection extends Iterable<Field> {
    /**
     * @return an {@link Iterator} of {@link Field}s
     */
    @Override
    public Iterator<Field> iterator();

    /**
     * @return the number of {@link Field}s in this collection
     */
    public int size();

    /**
     * Gets the {@link Field} with the specified name.
     *
     * @param name
     *        the name to get (must not be <code>null</code>)
     * @return the {@link Field}
     * @throws IllegalArgumentException
     *         if the field name was not found in this collection
     */
    public Field getField(String name);

    /**
     * @return <code>true</code> if a {@link Field} with the specified name is
     *         in this collection, <code>false</code> if it is not
     */
    public boolean contains(String name);

    /**
     * Convenience method to get the value of a field in this collection.
     *
     * @return the integer value of the {@link CoreFieldReferenceNames#ID}
     *         {@link Field} in this collection
     */
    public int getID();

    /**
     * Convenience method to get the value of a field in this collection.
     *
     * @return the integer value of the {@link CoreFieldReferenceNames#REVISION}
     *         {@link Field} in this collection
     */
    public int getRevision();

    /**
     * Convenience method to get the value of a field in this collection.
     *
     * @return the integer value of the {@link CoreFieldReferenceNames#AREA_ID}
     *         {@link Field} in this collection
     */
    public int getAreaID();

    /**
     * Convenience method to get the value of a field in this collection.
     *
     * @return the value of the {@link CoreFieldReferenceNames#WORK_ITEM_TYPE}
     *         {@link Field} in this collection
     */
    public String getWorkItemType();
}
