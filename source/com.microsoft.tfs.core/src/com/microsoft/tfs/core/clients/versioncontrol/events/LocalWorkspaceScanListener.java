// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.EventListener;

/**
 * @since TEE-SDK-11.0
 */
public interface LocalWorkspaceScanListener extends EventListener {
    public void onLocalWorkspaceScan(WorkspaceEvent e);
}
