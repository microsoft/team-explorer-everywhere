// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsageMetadata;

/**
 * Roughly equivalent to "DatastoreItemFieldUsagesClass" in MS code.
 */
public class DatastoreItemFieldUsagesCollection {
    private final FieldReferenceBasedCache<DatastoreItemFieldUsage> cache =
        new FieldReferenceBasedCache<DatastoreItemFieldUsage>();

    private final Set<FieldDefinition> fieldDefinitions = new HashSet<FieldDefinition>();
    private final Set<DatastoreItemFieldUsage> fieldUsages = new HashSet<DatastoreItemFieldUsage>();

    public DatastoreItemFieldUsagesCollection(final int fieldId, final WITContext context) {
        final FieldUsageMetadata[] fieldUsagesMetadata =
            context.getMetadata().getFieldUsagesTable().getFieldUsagesForObjectID(fieldId);

        for (int i = 0; i < fieldUsagesMetadata.length; i++) {
            final DatastoreItemFieldUsage fieldUsage = new DatastoreItemFieldUsage(fieldUsagesMetadata[i], context);
            final FieldDefinitionImpl fieldDefinition = fieldUsage.getFieldDefinition();

            cache.put(
                fieldUsage,
                fieldDefinition.getName(),
                fieldDefinition.getReferenceName(),
                fieldDefinition.getID());
            fieldUsages.add(fieldUsage);
            fieldDefinitions.add(fieldDefinition);
        }
    }

    public FieldDefinitionImpl[] getFieldDefinitions() {
        return fieldDefinitions.toArray(new FieldDefinitionImpl[] {});
    }

    public DatastoreItemFieldUsage[] getFieldUsages() {
        return fieldUsages.toArray(new DatastoreItemFieldUsage[] {});
    }

    public DatastoreItemFieldUsage getUsageByFieldID(final int fieldId) {
        return cache.get(fieldId);
    }

    public FieldDefinitionImpl getFieldDefinitionByName(final String fieldName) {
        final DatastoreItemFieldUsage fieldUsage = cache.get(fieldName);
        if (fieldUsage == null) {
            return null;
        }
        return fieldUsage.getFieldDefinition();
    }

    public FieldDefinitionImpl getFieldDefinitionByID(final int id) {
        final DatastoreItemFieldUsage fieldUsage = cache.get(id);
        if (fieldUsage == null) {
            return null;
        }
        return fieldUsage.getFieldDefinition();
    }
}
