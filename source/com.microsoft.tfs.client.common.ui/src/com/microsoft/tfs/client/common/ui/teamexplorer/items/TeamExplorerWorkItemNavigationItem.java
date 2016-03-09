// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.items;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;

public class TeamExplorerWorkItemNavigationItem extends TeamExplorerBaseNavigationItem {
    @Override
    public boolean canOpenInWeb() {
        return true;
    }

    @Override
    public void openInWeb(final TeamExplorerContext context) {
        WebAccessHelper.openWorkItems(context);
    }
}
