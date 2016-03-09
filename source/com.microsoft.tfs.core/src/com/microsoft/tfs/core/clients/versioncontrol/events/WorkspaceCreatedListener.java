// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

/**
 * Defines an interface for listeners of the {@link WorkspaceEvent}.
 *
 * @since TEE-SDK-10.1
 */
public interface WorkspaceCreatedListener extends EventListener {
    public void onWorkspaceCreated(WorkspaceEvent e);
}
