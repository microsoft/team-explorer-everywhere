// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

/**
 * Defines an interface for listeners of changes in workspace metatdata (name,
 * comment, etc.).
 *
 * @since TEE-SDK-10.1
 */
public interface WorkspaceUpdatedListener extends EventListener {
    /**
     * Fired when a workspace is modified through its update() method.
     *
     * @param e
     *        the {@link WorkspaceUpdatedEvent} (must not be <code>null</code>)
     */
    public void onWorkspaceUpdated(WorkspaceUpdatedEvent e);
}
