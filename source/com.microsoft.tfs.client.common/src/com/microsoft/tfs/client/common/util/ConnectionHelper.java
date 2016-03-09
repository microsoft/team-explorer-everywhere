// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.util.Check;

public final class ConnectionHelper {
    private ConnectionHelper() {
    }

    public static boolean isConnected(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        return connection.hasAuthenticated() && !connection.getConnectivityFailureOnLastWebServiceCall();
    }
}
