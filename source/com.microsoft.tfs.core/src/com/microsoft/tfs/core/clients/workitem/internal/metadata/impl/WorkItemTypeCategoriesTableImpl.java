// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoriesTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMetadata;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class WorkItemTypeCategoriesTableImpl extends BaseMetadataDAO implements WorkItemTypeCategoriesTable {

    @Override
    public WorkItemTypeCategoryMetadata[] getCategories() {
        final ArrayList categoryData = new ArrayList();
        final String sql =
            "select WorkItemTypeCategoryID, ProjectID, Name, ReferenceName, DefaultWorkItemTypeID from WorkItemTypeCategories where fDeleted = 0"; //$NON-NLS-1$

        getConnection().createStatement(sql).executeQuery(new Object[] {}, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                final int workItemTypeCategoryId = rset.getInt(1);
                final int projectId = rset.getInt(2);
                final String name = rset.getString(3);
                final String referenceName = rset.getString(4);
                final int defaultWorkItemTypeId = rset.getInt(5);

                categoryData.add(
                    new WorkItemTypeCategoryMetadata(
                        workItemTypeCategoryId,
                        projectId,
                        name,
                        referenceName,
                        defaultWorkItemTypeId));
            }
        });

        return (WorkItemTypeCategoryMetadata[]) categoryData.toArray(
            new WorkItemTypeCategoryMetadata[categoryData.size()]);
    }

}
