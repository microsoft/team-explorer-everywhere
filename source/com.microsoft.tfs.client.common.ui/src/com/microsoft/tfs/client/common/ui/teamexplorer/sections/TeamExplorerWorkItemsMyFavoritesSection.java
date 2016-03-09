// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEventListener;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.PrivateQueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.QueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class TeamExplorerWorkItemsMyFavoritesSection extends TeamExplorerWorkItemsFavoritesSection {
    private TeamExplorerContext context;
    private final TeamExplorerEventListener listener = new MyFavoritesChangedListener();

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
        loadFavorites(context);
    }

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return TeamExplorerHelpers.supportsMyFavorites(context.getServer().getConnection());
    }

    protected void loadFavorites(final TeamExplorerContext context) {
        final Project project = context.getCurrentProject();
        final IFavoritesStore store = getFavoritesStore(context, true);

        if (store != null) {
            favoriteItems = QueryFavoriteItem.fromFavoriteItems(project, store.getFavorites(), true);
        } else {
            favoriteItems = new PrivateQueryFavoriteItem[0];
        }
    }

    @Override
    protected void addFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().addListener(TeamExplorerEvents.MY_WORK_ITEM_FAVORITES_CHANGED, listener);
    }

    @Override
    protected void removeFavoritesChangedListener(final TeamExplorerContext context) {
        context.getEvents().removeListener(TeamExplorerEvents.MY_WORK_ITEM_FAVORITES_CHANGED, listener);
    }

    private class MyFavoritesChangedListener implements TeamExplorerEventListener {
        @Override
        public void onEvent(final TeamExplorerEventArg arg) {
            loadFavorites(context);
            refresh();
        }
    }
}
