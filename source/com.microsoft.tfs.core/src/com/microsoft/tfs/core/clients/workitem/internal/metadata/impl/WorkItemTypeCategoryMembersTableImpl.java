// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMemberMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMembersTable;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class WorkItemTypeCategoryMembersTableImpl extends BaseMetadataDAO implements WorkItemTypeCategoryMembersTable {
    @Override
    public WorkItemTypeCategoryMemberMetadata[] getCategoryMembers() {
        final List<WorkItemTypeCategoryMemberMetadata> list = new ArrayList<WorkItemTypeCategoryMemberMetadata>();

        final String sql =
            "select WorkItemTypeCategoryMemberID, WorkItemTypeCategoryID, WorkItemTypeID from WorkItemTypeCategoryMembers where fDeleted = 0"; //$NON-NLS-1$

        getConnection().createStatement(sql).executeQuery(new Object[] {}, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                final int categoryMemberID = rset.getInt(1);
                final int categoryID = rset.getInt(2);
                final int workItemTypeID = rset.getInt(3);

                list.add(new WorkItemTypeCategoryMemberMetadata(categoryMemberID, categoryID, workItemTypeID));
            }
        });

        return list.toArray(new WorkItemTypeCategoryMemberMetadata[list.size()]);
    }
}
