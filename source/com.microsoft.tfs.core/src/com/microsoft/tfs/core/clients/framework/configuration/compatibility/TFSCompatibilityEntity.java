// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.compatibility;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntity;
import com.microsoft.tfs.util.Check;

/**
 * @since TEE-SDK-10.1
 */
public abstract class TFSCompatibilityEntity implements TFSEntity {
    private final TFSTeamProjectCollection connection;
    private final TFSCompatibilityEntity parent;

    protected TFSCompatibilityEntity(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
        parent = null;
    }

    protected TFSCompatibilityEntity(final TFSCompatibilityEntity parent) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.notNull(parent.connection, "connection"); //$NON-NLS-1$

        connection = parent.connection;
        this.parent = parent;
    }

    protected TFSTeamProjectCollection getConnection() {
        return connection;
    }

    @Override
    public final TFSEntity getParent() {
        return parent;
    }

    @Override
    public String getDescription() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getDisplayPath() {
        final StringBuffer displayPath = new StringBuffer();

        if (getParent() != null) {
            displayPath.append(getParent().getDisplayPath());
        }

        displayPath.append("\\"); //$NON-NLS-1$

        if (getDisplayName() != null) {
            displayPath.append(getDisplayName());
        }

        return displayPath.toString();
    }

    protected <T extends TFSEntity> T getAncestorOfType(final Class<T> type) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        for (TFSEntity ancestor = getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (type.isInstance(ancestor)) {
                return (T) ancestor;
            }
        }

        return null;
    }
}
