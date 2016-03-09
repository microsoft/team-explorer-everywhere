// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query.qe;

import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;

public class WIQLTranslatorFieldDefinitionCollectionAdatper implements WIQLTranslatorFieldService {
    private final FieldDefinitionCollection fieldDefinitions;

    public WIQLTranslatorFieldDefinitionCollectionAdatper(final FieldDefinitionCollection fieldDefinitions) {
        this.fieldDefinitions = fieldDefinitions;
    }

    @Override
    public String getLocalizedFieldName(final String fieldName) {
        return fieldDefinitions.get(fieldName).getName();
    }

    @Override
    public String getInvariantFieldName(final String fieldName) {
        return fieldDefinitions.get(fieldName).getReferenceName();
    }

    @Override
    public boolean isDateTimeField(final String fieldName) {
        final FieldDefinition fd = fieldDefinitions.get(fieldName);
        return fd.getFieldType() == FieldType.DATETIME;
    }

    @Override
    public boolean isDecimalField(final String fieldName) {
        final FieldDefinition fd = fieldDefinitions.get(fieldName);
        return fd.getFieldType() == FieldType.DOUBLE;
    }

    @Override
    public boolean isStringField(final String fieldName) {
        final FieldDefinition fd = fieldDefinitions.get(fieldName);
        if (fd.getID() == WorkItemFieldIDs.TEAM_PROJECT) {
            return true;
        }
        return fd.getFieldType() == FieldType.STRING
            || fd.getFieldType() == FieldType.PLAINTEXT
            || fd.getFieldType() == FieldType.TREEPATH
            || fd.getFieldType() == FieldType.HISTORY
            || fd.getFieldType() == FieldType.HTML
            || fd.getFieldType() == FieldType.GUID;
    }
}
