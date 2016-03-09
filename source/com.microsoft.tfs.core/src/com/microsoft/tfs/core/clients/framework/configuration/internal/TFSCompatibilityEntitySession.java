// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntitySession;
import com.microsoft.tfs.core.clients.framework.configuration.compatibility.OrganizationalRootCompatibilityEntity;
import com.microsoft.tfs.core.clients.framework.configuration.entities.OrganizationalRootEntity;
import com.microsoft.tfs.util.Check;

public class TFSCompatibilityEntitySession implements TFSEntitySession {
    private final OrganizationalRootCompatibilityEntity organizationalRoot;

    TFSCompatibilityEntitySession(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        organizationalRoot = new OrganizationalRootCompatibilityEntity(connection);
    }

    @Override
    public OrganizationalRootEntity getOrganizationalRoot() {
        return organizationalRoot;
    }
}
