// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.link;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;

public abstract class TeamExplorerBaseNavigationLink implements ITeamExplorerNavigationLink {
    @Override
    public boolean isEnabled(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public final void clicked(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {

        onClick(shell, context, navigator, parentNavigationItem);
    }

    protected abstract void onClick(
        final Shell shell,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem);
}
