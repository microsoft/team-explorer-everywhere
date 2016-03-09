// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.core.clients.favorites.FavoritesStoreFactory;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.util.GUID;

public abstract class RemoveBuildFavoriteAction extends TeamExplorerBaseAction {
    private final boolean isPersonal;

    protected RemoveBuildFavoriteAction(final boolean isPersonal) {
        this.isPersonal = isPersonal;
    }

    protected abstract void fireFavoritesChangedEvent(final TeamExplorerContext context);

    @Override
    public void doRun(final IAction action) {
        final List<GUID> list = new ArrayList<GUID>();
        for (final Object section : selectionToArray()) {
            list.add(((BuildFavoriteItem) section).getFavoriteItem().getID());
        }

        final IFavoritesStore store = getFavoritesStore();
        if (store != null && list.size() > 0) {
            store.remove(list.toArray(new GUID[list.size()]));
            fireFavoritesChangedEvent(getContext());
        }
    }

    private IFavoritesStore getFavoritesStore() {
        final TeamExplorerContext context = getContext();

        return FavoritesStoreFactory.create(
            context.getServer().getConnection(),
            context.getCurrentProjectInfo(),
            context.getCurrentTeam(),
            BuildFavoriteItem.BUILD_DEFINITION_FEATURE_SCOPE,
            isPersonal);
    }
}
