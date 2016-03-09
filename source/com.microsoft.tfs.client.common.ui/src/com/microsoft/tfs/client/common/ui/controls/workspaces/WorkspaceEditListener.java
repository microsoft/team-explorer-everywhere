// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import java.util.EventListener;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public interface WorkspaceEditListener extends EventListener {
    public void onWorkspaceEdited(WorkspaceEditEvent event);

    public void onWorkspaceAdded(Workspace workspace);

    public void onWorkspaceRemoved(Workspace workspace);
}
