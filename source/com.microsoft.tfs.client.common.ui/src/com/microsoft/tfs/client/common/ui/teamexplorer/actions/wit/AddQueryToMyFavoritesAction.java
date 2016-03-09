// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;

public class AddQueryToMyFavoritesAction extends AddQueryToFavoritesAction {
    public AddQueryToMyFavoritesAction() {
        super(true);
    }

    @Override
    protected void fireFavoritesChangedEvent(final TeamExplorerContext context) {
        context.getEvents().notifyListener(TeamExplorerEvents.MY_WORK_ITEM_FAVORITES_CHANGED);
    }
}
