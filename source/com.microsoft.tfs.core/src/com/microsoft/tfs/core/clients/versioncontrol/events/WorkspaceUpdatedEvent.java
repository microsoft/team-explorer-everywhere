// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Event fired when a workspace is updated.
 *
 * @threadsafety unknown
 * @since TEE-SDK-10.1
 */
public class WorkspaceUpdatedEvent extends WorkspaceEvent {
    private final String originalName;
    private final WorkspaceLocation originalLocation;

    public WorkspaceUpdatedEvent(
        final EventSource source,
        final Workspace workspace,
        final String originalName,
        final WorkspaceLocation originalLocation,
        final WorkspaceEventSource workspaceEventSource) {
        super(source, workspace, workspaceEventSource);

        this.originalName = originalName;
        this.originalLocation = originalLocation;
    }

    /**
     * @return the name of this workspace before this update, or
     *         <code>null</code> if the original name is unknown (event came
     *         from another process)
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * @return the location of this workspace before this update, or
     *         <code>null</code> if the original location is unknown (event came
     *         from another process)
     */
    public WorkspaceLocation getOriginalLocation() {
        return originalLocation;
    }
}
