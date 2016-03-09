// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Event fired when a workspace is created, deleted, etc.
 *
 * @since TEE-SDK-10.1
 */
public class WorkspaceEvent extends CoreClientEvent {
    static final long serialVersionUID = -2014395938222685364L;
    private final Workspace workspace;
    private final WorkspaceEventSource workspaceEventSource;

    public WorkspaceEvent(
        final EventSource source,
        final Workspace workspace,
        final WorkspaceEventSource workspaceEventSource) {
        super(source);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(workspaceEventSource, "workspaceEventSource"); //$NON-NLS-1$

        this.workspace = workspace;
        this.workspaceEventSource = workspaceEventSource;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public WorkspaceEventSource getWorkspaceSource() {
        return workspaceEventSource;
    }

    public static class WorkspaceEventSource extends TypesafeEnum {
        /**
         * This event was raised by an action which occurred on this
         * VersionControlServer object.
         */
        public static final WorkspaceEventSource INTERNAL = new WorkspaceEventSource(0);

        /**
         * This event was raised by an action which occurred externally (such as
         * through a cross- process notification from the NotificationManager).
         * External events can only be received if the notification manager has
         * been started with NotificationManager.Initialize().
         */
        public static final WorkspaceEventSource EXTERNAL = new WorkspaceEventSource(1);

        /**
         * This event was raised by an action which occurred externally, but not
         * through a cross-process notification -- instead, it was discovered by
         * the local workspace scanner. Events with this source are raised only
         * for local workspaces (Workspace.Location == WorkspaceLocation.Local).
         */
        public static final WorkspaceEventSource EXTERNAL_SCANNED = new WorkspaceEventSource(2);

        private WorkspaceEventSource(final int value) {
            super(value);
        }
    }

}
