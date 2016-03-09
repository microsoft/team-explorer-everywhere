// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.views;

import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.pages.TeamExplorerBuildPage;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerDockableView;

/**
 * The dockable team explorer build view
 */
public class TeamExplorerBuildView extends TeamExplorerDockableView {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPageID() {
        return TeamExplorerBuildPage.ID;
    }
}
