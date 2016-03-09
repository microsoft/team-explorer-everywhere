// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsageMetadata;

/**
 * Roughly equivalent to "DatastoreItemFieldUsage" class in MS code.
 */
public class DatastoreItemFieldUsage {
    private final FieldUsageMetadata fieldUsageMetadata;
    private final WITContext context;

    private FieldDefinitionImpl fieldDefinition;

    public DatastoreItemFieldUsage(final FieldUsageMetadata fieldUsageMetadata, final WITContext context) {
        this.fieldUsageMetadata = fieldUsageMetadata;
        this.context = context;
    }

    public boolean isCore() {
        return fieldUsageMetadata.isCore();
    }

    public int getFieldID() {
        return fieldUsageMetadata.getFieldID();
    }

    public boolean isOftenQueried() {
        return fieldUsageMetadata.isOftenQueried();
    }

    public boolean supportsTextQuery() {
        return fieldUsageMetadata.supportsTextQuery();
    }

    public synchronized FieldDefinitionImpl getFieldDefinition() {
        if (fieldDefinition == null) {
            fieldDefinition = context.getFieldDefinitions().getFieldDefinitionInternal(fieldUsageMetadata.getFieldID());
        }
        return fieldDefinition;
    }

    public FieldUsageMetadata getFieldUsageMetadata() {
        return fieldUsageMetadata;
    }

}
