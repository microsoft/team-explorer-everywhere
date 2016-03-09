// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;

public class RemoveQueryFromTeamFavoritesAction extends RemoveQueryFavoriteAction {
    public RemoveQueryFromTeamFavoritesAction() {
        super(false);
    }

    @Override
    protected void fireFavoritesChangedEvent(final TeamExplorerContext context) {
        context.getEvents().notifyListener(TeamExplorerEvents.TEAM_WORK_ITEM_FAVORITES_CHANGED);
    }
}
