// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;

public class RemoveDefinitionFromMyFavoritesAction extends RemoveBuildFavoriteAction {
    public RemoveDefinitionFromMyFavoritesAction() {
        super(true);
    }

    @Override
    protected void fireFavoritesChangedEvent(final TeamExplorerContext context) {
        context.getEvents().notifyListener(TeamExplorerEvents.MY_BUILD_FAVORITES_CHANGED);
    }
}
