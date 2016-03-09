// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;

/**
 * Represents a collection of {@link FieldDefinition}s that will be paged from
 * the server when the WorkItems in a {@link WorkItemCollection} are accessed.
 *
 * @since TEE-SDK-10.1
 */
public interface DisplayFieldList {
    public void add(FieldDefinition field);

    public void add(String fieldName);

    public int getSize();

    public FieldDefinition getField(int index);

    public int indexOf(FieldDefinition fieldDefinition);

    public void clear();
}
