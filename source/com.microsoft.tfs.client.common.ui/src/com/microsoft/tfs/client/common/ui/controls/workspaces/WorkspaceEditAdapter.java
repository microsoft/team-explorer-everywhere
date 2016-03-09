// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.workspaces;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

/**
 *
 *
 * @threadsafety unknown
 */
public class WorkspaceEditAdapter implements WorkspaceEditListener {
    @Override
    public void onWorkspaceEdited(final WorkspaceEditEvent event) {
    }

    @Override
    public void onWorkspaceAdded(final Workspace workspace) {
    }

    @Override
    public void onWorkspaceRemoved(final Workspace workspace) {
    }
}
