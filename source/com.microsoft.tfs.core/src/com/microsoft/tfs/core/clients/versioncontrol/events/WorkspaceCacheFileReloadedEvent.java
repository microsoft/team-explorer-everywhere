// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when {@link Workstation} reloads its workspace cache file.
 *
 * @since TEE-SDK-11.0
 */
public class WorkspaceCacheFileReloadedEvent extends CoreClientEvent {
    private final Workstation workstation;

    public WorkspaceCacheFileReloadedEvent(final EventSource source, final Workstation workstation) {
        super(source);

        Check.notNull(workstation, "workstation"); //$NON-NLS-1$
        this.workstation = workstation;
    }

    public Workstation getWorkstation() {
        return this.workstation;
    }
}
