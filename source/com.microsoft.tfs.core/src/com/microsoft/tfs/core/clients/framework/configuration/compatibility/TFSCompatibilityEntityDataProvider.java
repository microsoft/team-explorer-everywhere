// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntityDataProvider;
import com.microsoft.tfs.util.Check;

/**
 * @since TEE-SDK-10.1
 */
public abstract class TFSCompatibilityEntityDataProvider implements TFSEntityDataProvider {
    private final TFSTeamProjectCollection connection;

    public TFSCompatibilityEntityDataProvider(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    protected TFSTeamProjectCollection getConnection() {
        return connection;
    }
}
