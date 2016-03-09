// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeTable;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class WorkItemTypeTableImpl extends BaseMetadataDAO implements WorkItemTypeTable {
    @Override
    public WorkItemTypeMetadata[] getWorkItemTypes(final int projectId) {
        final List workItemTypeData = new ArrayList();

        final String sql = "select WorkItemTypeID, NameConstantID from WorkItemTypes where " //$NON-NLS-1$
            + "ProjectID = ? and fDeleted = 0"; //$NON-NLS-1$

        getConnection().createStatement(sql).executeQuery(new Object[] {
            new Integer(projectId)
        }, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                final int workItemTypeID = rset.getInt(1);
                final int nameConstID = rset.getInt(2);
                final String name = getMetadata().getConstantsTable().getConstantByID(nameConstID);
                workItemTypeData.add(new WorkItemTypeMetadata(workItemTypeID, name));
            }
        });

        return (WorkItemTypeMetadata[]) workItemTypeData.toArray(new WorkItemTypeMetadata[] {});
    }
}
