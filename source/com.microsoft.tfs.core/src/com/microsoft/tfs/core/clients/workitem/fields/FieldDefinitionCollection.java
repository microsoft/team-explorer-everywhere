// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import java.util.Iterator;

import com.microsoft.tfs.core.clients.workitem.exceptions.FieldDefinitionNotExistException;

/**
 * Describes a collection of {@link FieldDefinition} objects.
 *
 * @since TEE-SDK-10.1
 */
public interface FieldDefinitionCollection extends Iterable<FieldDefinition> {
    /**
     * @return an {@link Iterator} of {@link FieldDefinition}s, sorted by name.
     */
    @Override
    public Iterator<FieldDefinition> iterator();

    /**
     * @return the number of objects in this collection.
     */
    public int size();

    /**
     * Gets a {@link FieldDefinition} by name.
     *
     * @param fieldName
     *        the name of the field to get (must not be <code>null</code>)
     * @return the {@link FieldDefinition} for the specified name
     * @throws FieldDefinitionNotExistException
     *         if this collection does not contain the named field
     */
    public FieldDefinition get(String fieldName);

    /**
     * Tests whether this collection contains the specified item.
     *
     * @param fieldName
     *        the item to find in this collection
     * @return <code>true</code> if the collection contains the specified item,
     *         <code>false</code> if it does not
     */
    public boolean contains(String fieldName);

    /**
     * @return an array of the {@link FieldDefinition}s in this collection.
     */
    public FieldDefinition[] getFieldDefinitions();
}
