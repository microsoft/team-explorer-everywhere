// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;

public interface TeamExplorerNavigationListener {
    public void navigateToItem(TeamExplorerNavigationItemConfig item);

    public void navigationHistoryChanged();
}
