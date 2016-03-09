// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemLinkTypesTable;
import com.microsoft.tfs.core.internal.db.ResultHandler;

/**
 * Models the link types table from the WIT metadata. This table first appeared
 * in WIT version 3.
 *
 *
 * @threadsafety thread-safe
 */
public class WorkItemLinkTypesTableImpl extends BaseMetadataDAO implements WorkItemLinkTypesTable {
    @Override
    public WorkItemLinkTypeMetadata[] getLinkTypes() {
        final ArrayList linkTypeData = new ArrayList();
        final String sql =
            "select ReferenceName, ForwardName, ForwardId, ReverseName, ReverseId, Rules from LinkTypes where fDeleted = 0"; //$NON-NLS-1$

        getConnection().createStatement(sql).executeQuery(new Object[] {}, new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                final String referenceName = rset.getString(1);
                final String forwardName = rset.getString(2);
                final int forwardId = rset.getInt(3);
                final String reverseName = rset.getString(4);
                final int reverseId = rset.getInt(5);
                final int rules = rset.getInt(6);
                linkTypeData.add(
                    new WorkItemLinkTypeMetadata(referenceName, forwardName, forwardId, reverseName, reverseId, rules));
            }
        });

        return (WorkItemLinkTypeMetadata[]) linkTypeData.toArray(new WorkItemLinkTypeMetadata[linkTypeData.size()]);
    }
}
