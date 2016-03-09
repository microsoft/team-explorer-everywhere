// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Information for conflict resolution.
 *
 * @since TEE-SDK-10.1
 */
public class ConflictResolvedEvent extends CoreClientEvent {
    private final Workspace workspace;
    private final Conflict conflict;
    private final ChangePendedFlags changePendedFlags;

    public ConflictResolvedEvent(
        final EventSource source,
        final Workspace workspace,
        final Conflict conflict,
        final ChangePendedFlags changePendedFlags) {
        super(source);

        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(changePendedFlags, "changePendedFlags"); //$NON-NLS-1$

        this.workspace = workspace;
        this.conflict = conflict;
        this.changePendedFlags = changePendedFlags;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Conflict getConflict() {
        return conflict;
    }

    public ChangePendedFlags getChangePendedFlags() {
        return changePendedFlags;
    }
}
