// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.PrivateBuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;

public class TeamExplorerBuildsMyFavoritesSection extends TeamExplorerBuildsFavoritesSection {
    private final TeamExplorerEventListener listener = new MyFavoritesChangedListener();

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
        loadFavorites(context);
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        final TFSServer server = context.getServer();
        // show this section even if offline
        return server == null || TeamExplorerHelpers.supportsMyFavorites(server.getConnection());
    }

    @Override
    protected void loadFavorites(final TeamExplorerContext context) {
        if (context == null || context.getBuildServer() == null) {
            favoriteItems = new PrivateBuildFavoriteItem[0];
        }

        final IBuildServer server = context.getBuildServer();
        final IFavoritesStore store = getFavoritesStore(context, true);

        if (store != null) {
            loadDefinitions(context);
            favoriteItems = BuildFavoriteItem.fromFavoriteItems(server, store.getFavorites(), definitions, true);
        } else {
            favoriteItems = new PrivateBuildFavoriteItem[0];
        }
    }

    @Override
    protected void addFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().addListener(TeamExplorerEvents.MY_BUILD_FAVORITES_CHANGED, listener);
    }

    @Override
    protected void removeFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().removeListener(TeamExplorerEvents.MY_BUILD_FAVORITES_CHANGED, listener);
    }

    private class MyFavoritesChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            loadFavorites(context);
            refresh();
        }
    }
}
