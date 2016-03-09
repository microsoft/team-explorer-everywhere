// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.link;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;

public class TeamExplorerNewQueryNavigationLink extends TeamExplorerBaseNavigationLink {
    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        return context.isConnected();
    }

    @Override
    public void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        WorkItemHelpers.openNewQuery(context);
    }
}
