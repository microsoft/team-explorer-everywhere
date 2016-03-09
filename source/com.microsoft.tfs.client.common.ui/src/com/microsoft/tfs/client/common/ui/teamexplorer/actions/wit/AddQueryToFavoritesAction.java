// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.favorites.QueryFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.FavoriteHelpers;
import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.core.clients.favorites.FavoritesStoreFactory;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.core.clients.favorites.exceptions.DuplicateFavoritesException;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.util.GUID;

public abstract class AddQueryToFavoritesAction extends TeamExplorerWITBaseAction {
    protected final boolean isPersonal;

    protected AddQueryToFavoritesAction(final boolean isPersonal) {
        this.isPersonal = isPersonal;
    }

    protected abstract void fireFavoritesChangedEvent(final TeamExplorerContext context);

    @Override
    protected void doRun(final IAction action) {
        @SuppressWarnings("rawtypes")
        Iterator i;

        final List<FavoriteItem> list = new ArrayList<FavoriteItem>();

        for (i = getStructuredSelection().iterator(); i.hasNext();) {
            final QueryDefinition definition = (QueryDefinition) i.next();
            list.add(createFavoriteItem(definition));
        }

        final IFavoritesStore store = getFavoritesStore();
        try {
            store.updateFavorites(list.toArray(new FavoriteItem[list.size()]));
            fireFavoritesChangedEvent(getContext());
        } catch (final DuplicateFavoritesException e) {
            FavoriteHelpers.showDuplicateError(getShell(), e, list.size());
        }
    }

    private FavoriteItem createFavoriteItem(final QueryDefinition definition) {
        final FavoriteItem favorite = new FavoriteItem();
        favorite.setID(GUID.newGUID());
        favorite.setParentID(GUID.EMPTY);
        favorite.setName(definition.getName());
        favorite.setType(QueryFavoriteItem.QUERY_DEFINITION_FAVORITE_TYPE);
        favorite.setData(definition.getID().toString());
        return favorite;
    }

    private IFavoritesStore getFavoritesStore() {
        final TeamExplorerContext context = getContext();

        return FavoritesStoreFactory.create(
            context.getServer().getConnection(),
            context.getCurrentProjectInfo(),
            context.getCurrentTeam(),
            QueryFavoriteItem.QUERY_DEFINITION_FEATURE_SCOPE,
            isPersonal);
    }
}
