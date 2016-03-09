// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.teamexplorer;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerBaseNavigationItem;

public class TeamExplorerGitExplorerNavigationItem extends TeamExplorerBaseNavigationItem {

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return TeamExplorerHelpers.supportsGit(context);
    }
}
