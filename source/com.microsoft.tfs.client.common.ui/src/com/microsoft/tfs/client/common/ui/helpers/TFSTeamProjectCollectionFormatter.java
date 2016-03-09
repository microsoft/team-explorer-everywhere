// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;

public class TFSTeamProjectCollectionFormatter {
    public static String getLabel(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        /* 2010 (offline or local workspaces) */
        if (!ConnectionHelper.isConnected(connection)) {
            if (!connection.getBaseURI().getPath().equals("/")) //$NON-NLS-1$
            {
                return connection.getBaseURI().getHost();
            }

            return connection.getBaseURI().getHost() + "\\" + connection.getBaseURI().getPath(); //$NON-NLS-1$
        }
        /* 2010: display hostname and collection name */
        else if (connection.getCatalogNode() != null && connection.getCatalogNode().getResource() != null) {
            return connection.getBaseURI().getHost()
                + "\\" //$NON-NLS-1$
                + connection.getCatalogNode().getResource().getDisplayName();
        } else {
            return connection.getBaseURI().getHost();
        }
    }
}
