// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;

/**
 * The interface all Team Explorer View should implement
 */
public interface ITeamExplorerView {
    public TeamExplorerContext getContext();

    public void refresh();
}
