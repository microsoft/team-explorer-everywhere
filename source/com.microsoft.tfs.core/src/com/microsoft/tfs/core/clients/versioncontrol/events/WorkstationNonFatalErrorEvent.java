// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.InternalWorkspaceConflictInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when {@link Workstation} encounters non-fatal errors processing
 * workspace updates.
 *
 * @since TEE-SDK-11.0
 */
public class WorkstationNonFatalErrorEvent extends CoreClientEvent {
    private final WorkspaceInfo info;

    public WorkstationNonFatalErrorEvent(final EventSource source, final InternalWorkspaceConflictInfo info) {
        super(source);

        Check.notNull(info, "info"); //$NON-NLS-1$

        this.info = info.getPrimaryWorkspace();
    }

    public WorkstationNonFatalErrorEvent(
        final EventSource source,
        final Exception exception,
        final Workspace workspace) {
        super(source);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.info = Workstation.getCurrent(
            workspace.getClient().getConnection().getPersistenceStoreProvider()).getLocalWorkspaceInfo(
                workspace.getClient(),
                workspace.getName(),
                workspace.getOwnerName());
    }

    public WorkspaceInfo getWorkspaceInfo() {
        return this.info;
    }
}
