// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.items;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

public class TeamExplorerHomeNavigationItem extends TeamExplorerBaseNavigationItem {
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return true;
    }
}
