// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.util.Set;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when the local workspace scanner modifies on-disk files during a
 * scan.
 *
 * @since TEE-SDK-11.0
 */
public class ScannerModifiedFilesEvent extends CoreClientEvent {
    private final Workspace workspace;
    private final Set<String> paths;

    public ScannerModifiedFilesEvent(final EventSource source, final Workspace workspace, final Set<String> paths) {
        super(source);

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(paths, "paths"); //$NON-NLS-1$

        this.workspace = workspace;
        this.paths = paths;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Set<String> getPaths() {
        return paths;
    }
}
