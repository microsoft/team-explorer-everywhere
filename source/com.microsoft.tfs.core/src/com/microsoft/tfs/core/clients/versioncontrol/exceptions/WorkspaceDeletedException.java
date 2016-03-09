// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 * Thrown when a workspace is accessed after it has been deleted.
 *
 * @since TEE-SDK-11.0
 */
public class WorkspaceDeletedException extends VersionControlException {
    private final Workspace workspace;

    public WorkspaceDeletedException(final Workspace workspace) {
        super();
        this.workspace = workspace;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
