// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerPendingChangesPage;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

/**
 * The dockable pending change view
 */
public class TeamExplorerPendingChangesView extends TeamExplorerDockableView {

    public static final String ID = "com.microsoft.tfs.client.common.ui.views.PendingChangesView"; //$NON-NLS-1$

    @Override
    protected String getPageID() {
        return TeamExplorerPendingChangesPage.ID;
    }

    @Override
    protected void initialize() {
        TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel().initialize();
    }

    @Override
    public void refresh() {
        if (isVisible()) {
            TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel().initialize();
            control.refreshView();
        } else {
            getViewSite().getPage().hideView(this); // close view
        }
    }

    public boolean isVisible() {
        if (!context.isConnected()) {
            return true;
        } else {
            final SourceControlCapabilityFlags flags = context.getSourceControlCapability();
            return flags.contains(SourceControlCapabilityFlags.TFS);
        }
    }
}
