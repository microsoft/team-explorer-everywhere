// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.HierarchyTable;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.NodeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.NodeResultHandler;
import com.microsoft.tfs.core.internal.db.ResultHandler;

public class HierarchyTableImpl extends BaseMetadataDAO implements HierarchyTable {
    @Override
    public NodeMetadata[] getNodesWithParentID(final int parentId) {
        /*
         * This select clause filters out the root node, which has itself as a
         * parent. In other words, getNodesWithParentId() never returns the root
         * node.
         */
        final String SQL = NodeMetadata.SELECT_STRING + " where ParentID = ? and AreaId <> 0"; //$NON-NLS-1$

        final NodeResultHandler resultHandler = new NodeResultHandler();

        getConnection().createStatement(SQL).executeQuery(new Object[] {
            new Integer(parentId)
        }, resultHandler);

        return resultHandler.toArray();
    }

    @Override
    public NodeMetadata getRootNode() {
        final String SQL = NodeMetadata.SELECT_STRING + " where AreaID = 0"; //$NON-NLS-1$

        final NodeMetadata[] holder = new NodeMetadata[1];

        getConnection().createStatement(SQL).executeQuery(new ResultHandler() {
            @Override
            public void handleRow(final ResultSet rset) throws SQLException {
                holder[0] = NodeMetadata.fromRow(rset);
            }
        });

        return holder[0];
    }

    @Override
    public int getParentID(final int childId) {
        return getConnection().createStatement("select ParentID from Hierarchy where AreaID = ?").executeIntQuery( //$NON-NLS-1$
            new Object[] {
                new Integer(childId)
        }).intValue();
    }

    @Override
    public NodeMetadata[] getNodesWithTypeID(final int typeId) {
        final NodeResultHandler resultHandler = new NodeResultHandler();

        getConnection().createStatement(NodeMetadata.SELECT_STRING + " where TypeId = ?").executeQuery(new Object[] //$NON-NLS-1$
        {
            new Integer(typeId)
        }, resultHandler);

        return resultHandler.toArray();
    }

    @Override
    public NodeMetadata[] getAllNodes() {
        final NodeResultHandler resultHandler = new NodeResultHandler();

        getConnection().createStatement(NodeMetadata.SELECT_STRING).executeQuery(resultHandler);

        return resultHandler.toArray();
    }
}
