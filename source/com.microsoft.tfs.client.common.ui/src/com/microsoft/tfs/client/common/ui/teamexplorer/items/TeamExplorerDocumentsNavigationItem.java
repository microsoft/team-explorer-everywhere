// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.items;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.clients.sharepoint.WSSUtils;

public class TeamExplorerDocumentsNavigationItem extends TeamExplorerBaseNavigationItem {
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context.isConnected()
            && WSSUtils.isWSSConfigured(context.getServer().getConnection(), context.getCurrentProjectInfo());

    }
}
