// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;

/**
 * Represents a collection of {@link SortField}s that can be sorted by a query.
 *
 * @since TEE-SDK-10.1
 */
public interface SortFieldList {
    public boolean add(FieldDefinition fieldDefinition, SortType sortType);

    public boolean add(String fieldName, SortType sortType);

    public int getSize();

    public void insert(int ix, FieldDefinition fieldDefinition, SortType sortType);

    public boolean remove(FieldDefinition fieldDefinition);

    public int indexOf(FieldDefinition fieldDefinition);

    public SortField get(int index);

    public void clear();
}
