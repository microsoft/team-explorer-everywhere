// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;

/**
 * Describes the type of object on which a {@link FieldDefinition} applies.
 *
 * @since TEE-SDK-10.1
 */
public class FieldUsages {
    public static final FieldUsages WORK_ITEM = new FieldUsages("WorkItem", WorkItemFieldIDs.WORK_ITEM); //$NON-NLS-1$
    public static final FieldUsages WORK_ITEM_LINK = new FieldUsages("WorkItemLink", WorkItemFieldIDs.WORK_ITEM_LINK); //$NON-NLS-1$

    private final String name;
    private final int value;

    private FieldUsages(final String name, final int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

}
