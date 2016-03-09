// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;

public class RefreshBuildExplorerAction extends Action {

    private BuildExplorer buildExplorer;

    public RefreshBuildExplorerAction() {
        super();
        setText(Messages.getString("RefreshBuildExplorerAction.RefreshBuildExplorerAction")); //$NON-NLS-1$
        setImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSTeamBuildPlugin.PLUGIN_ID, "/icons/Refresh.gif")); //$NON-NLS-1$
    }

    public void setActiveEditor(final BuildExplorer editor) {
        buildExplorer = editor;
    }

    @Override
    public void run() {
        if (buildExplorer != null) {
            buildExplorer.refresh();
        }
    }

}
