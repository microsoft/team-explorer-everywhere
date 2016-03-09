// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.internal.db.ResultHandler;

public class NodeResultHandler implements ResultHandler {
    private final List nodes = new ArrayList();

    @Override
    public void handleRow(final ResultSet rset) throws SQLException {
        nodes.add(NodeMetadata.fromRow(rset));
    }

    public NodeMetadata[] toArray() {
        return (NodeMetadata[]) nodes.toArray(new NodeMetadata[nodes.size()]);
    }
}
