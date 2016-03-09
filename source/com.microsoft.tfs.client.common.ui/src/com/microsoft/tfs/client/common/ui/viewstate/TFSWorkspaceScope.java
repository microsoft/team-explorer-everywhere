// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.viewstate;

import org.osgi.service.prefs.Preferences;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class TFSWorkspaceScope extends TFSServerScope {
    private final String workspaceName;

    public TFSWorkspaceScope(final Workspace workspace, final String rootNodeName) {
        super(rootNodeName, workspace.getClient().getConnection());
        workspaceName = workspace.getName();
    }

    @Override
    public Preferences getNestedPreferences(final Preferences startingNode) {
        final Preferences parentNode = super.getNestedPreferences(startingNode);
        return parentNode.node(workspaceName);
    }
}
