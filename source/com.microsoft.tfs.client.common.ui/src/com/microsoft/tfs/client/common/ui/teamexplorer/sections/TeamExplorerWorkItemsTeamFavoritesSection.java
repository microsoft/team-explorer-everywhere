// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.QueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.TeamQueryFavoriteItem;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class TeamExplorerWorkItemsTeamFavoritesSection extends TeamExplorerWorkItemsFavoritesSection {
    private TeamExplorerContext context;
    private final TeamExplorerEventListener listener = new TeamFavoritesChangedListener();

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context != null && context.getCurrentTeam() != null;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
        loadFavorites(context);
    }

    protected void loadFavorites(final TeamExplorerContext context) {
        final Project project = context.getCurrentProject();
        final IFavoritesStore store = getFavoritesStore(context, false);

        if (store != null) {
            favoriteItems = QueryFavoriteItem.fromFavoriteItems(project, store.getFavorites(), false);
        } else {
            favoriteItems = new TeamQueryFavoriteItem[0];
        }
    }

    @Override
    protected void addFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().addListener(TeamExplorerEvents.TEAM_WORK_ITEM_FAVORITES_CHANGED, listener);
    }

    @Override
    protected void removeFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().removeListener(TeamExplorerEvents.TEAM_WORK_ITEM_FAVORITES_CHANGED, listener);
    }

    private class TeamFavoritesChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            loadFavorites(context);
            refresh();
        }
    }
}
