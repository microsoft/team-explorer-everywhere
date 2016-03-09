// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.util.EventObject;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class WorkspaceEditEvent extends EventObject {
    private final WorkspaceData oldWorkspaceData;
    private final Workspace workspace;

    public WorkspaceEditEvent(
        final WorkspacesControl source,
        final WorkspaceData oldWorkspaceData,
        final Workspace workspace) {
        super(source);

        Check.notNull(oldWorkspaceData, "oldWorkspaceData"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.oldWorkspaceData = oldWorkspaceData;
        this.workspace = workspace;
    }

    public WorkspacesControl getWorkspacesControl() {
        return (WorkspacesControl) getSource();
    }

    public WorkspaceData getOldWorkspaceData() {
        return oldWorkspaceData;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
