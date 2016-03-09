// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class WorkspaceData {
    private final WorkspaceDetails workspaceDetails;
    private final WorkingFolderDataCollection workingFolderDataCollection;

    public WorkspaceData(final TFSTeamProjectCollection connection, final String defaultWorkspaceName) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNullOrEmpty(defaultWorkspaceName, "defaultWorkspaceName"); //$NON-NLS-1$

        workspaceDetails = new WorkspaceDetails(connection, defaultWorkspaceName);
        workingFolderDataCollection = new WorkingFolderDataCollection();
    }

    public WorkspaceData(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        workspaceDetails = new WorkspaceDetails(workspace);
        workingFolderDataCollection = new WorkingFolderDataCollection(workspace);
    }

    public WorkspaceDetails getWorkspaceDetails() {
        return workspaceDetails;
    }

    public WorkingFolderDataCollection getWorkingFolderDataCollection() {
        return workingFolderDataCollection;
    }
}
