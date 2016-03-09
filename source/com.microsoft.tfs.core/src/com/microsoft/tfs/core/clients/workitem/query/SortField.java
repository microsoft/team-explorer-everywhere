// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;

/**
 * Represents a {@link WorkItemFieldIDs} that can be used to sort the results of
 * a query.
 *
 * @since TEE-SDK-10.1
 */
public class SortField {
    private final FieldDefinition fieldDefinition;
    private SortType sortType;

    public SortField(final FieldDefinition fieldDefinition, final SortType sortType) {
        this.fieldDefinition = fieldDefinition;
        this.sortType = sortType;
    }

    public FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public SortType getSortType() {
        return sortType;
    }

    public void setSortType(final SortType sortType) {
        this.sortType = sortType;
    }
}
