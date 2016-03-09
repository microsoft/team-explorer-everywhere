// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.TeamBuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;

public class TeamExplorerBuildsTeamFavoritesSection extends TeamExplorerBuildsFavoritesSection {
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

    @Override
    protected void loadFavorites(final TeamExplorerContext context) {
        favoriteItems = new TeamBuildFavoriteItem[0];
        if (context == null || context.getBuildServer() == null) {
            return;
        }
        final IFavoritesStore store = getFavoritesStore(context, false);

        if (store != null) {
            loadDefinitions(context);
            favoriteItems =
                BuildFavoriteItem.fromFavoriteItems(context.getBuildServer(), store.getFavorites(), definitions, false);
        }
    }

    @Override
    protected void addFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().addListener(TeamExplorerEvents.TEAM_BUILD_FAVORITES_CHANGED, listener);
    }

    @Override
    protected void removeFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().removeListener(TeamExplorerEvents.TEAM_BUILD_FAVORITES_CHANGED, listener);
    }

    private class TeamFavoritesChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            loadFavorites(context);
            refresh();
        }
    }
}
