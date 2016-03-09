// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsageMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldUsagesTable;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class FieldUsagesTableImpl extends BaseMetadataDAO implements FieldUsagesTable {
    @Override
    public FieldUsageMetadata[] getFieldUsagesForObjectID(final int objectId) {
        final List<FieldUsageMetadata> usages = new ArrayList<FieldUsageMetadata>();
        final String selectString = FieldUsageMetadata.getSelectStatement(getConnection());

        getConnection().createStatement(selectString + " where ObjectID = ?").executeQuery( //$NON-NLS-1$
            new Integer(objectId),
            new ResultHandler() {
                @Override
                public void handleRow(final ResultSet rset) throws SQLException {
                    usages.add(FieldUsageMetadata.fromRow(rset));
                }
            });

        return usages.toArray(new FieldUsageMetadata[usages.size()]);
    }
}
