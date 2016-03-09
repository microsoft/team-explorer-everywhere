// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.connectionconflict;

import com.microsoft.tfs.client.common.connectionconflict.ConnectionConflictHandler;

/**
 * A connection conflict handler for the Eclipse Plug-in. Disallows connections
 * to new servers / workspaces.
 *
 * @threadsafety unknown
 */
public class EclipseConnectionConflictHandler implements ConnectionConflictHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean resolveServerConflict() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean resolveRepositoryConflict() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyServerConflict() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyRepositoryConflict() {
    }
}
