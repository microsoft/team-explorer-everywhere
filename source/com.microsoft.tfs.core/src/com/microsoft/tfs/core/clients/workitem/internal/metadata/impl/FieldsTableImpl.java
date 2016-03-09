// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldDefinitionMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldsTable;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class FieldsTableImpl extends BaseMetadataDAO implements FieldsTable {
    @Override
    public FieldDefinitionMetadata[] getAllFieldDefinitions() {
        final List<FieldDefinitionMetadata> fieldDefinitions = new ArrayList<FieldDefinitionMetadata>();
        final String selectString = FieldDefinitionMetadata.getSelectStatement(getConnection());

        getConnection().createStatement(selectString).executeQuery(new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                fieldDefinitions.add(FieldDefinitionMetadata.fromRow(rset));
            }
        });

        return fieldDefinitions.toArray(new FieldDefinitionMetadata[fieldDefinitions.size()]);
    }
}
