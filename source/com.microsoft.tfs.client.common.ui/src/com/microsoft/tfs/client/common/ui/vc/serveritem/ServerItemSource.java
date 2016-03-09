// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.serveritem;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;

public abstract class ServerItemSource {
    private final TFSTeamProjectCollection connection;
    private final Map<TypedServerItem, TypedServerItem[]> childCache =
        new HashMap<TypedServerItem, TypedServerItem[]>();

    protected ServerItemSource(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    public TFSTeamProjectCollection getConnection() {
        return connection;
    }

    public String getServerName() {
        /* 2010: display hostname and collection name */
        if (connection.getCatalogNode() != null && connection.getCatalogNode().getResource() != null) {
            return connection.getBaseURI().getHost()
                + "\\" //$NON-NLS-1$
                + connection.getCatalogNode().getResource().getDisplayName();
        }

        return connection.getBaseURI().getHost();
    }

    public void clearChildCache(final TypedServerItem parent) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        childCache.remove(parent);
    }

    public TypedServerItem[] getChildren(final TypedServerItem parent) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        TypedServerItem[] children = childCache.get(parent);

        if (children == null) {
            children = computeChildren(parent);
            if (children == null) {
                children = new TypedServerItem[0];
            }

            for (int i = 0; i < children.length; i++) {
                childCache.remove(children[i]);
            }

            childCache.put(parent, children);
        }

        return children;
    }

    protected abstract TypedServerItem[] computeChildren(TypedServerItem parent);
}
