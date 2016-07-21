// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IAction;

import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.FavoriteHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.core.clients.favorites.FavoritesStoreFactory;
import com.microsoft.tfs.core.clients.favorites.IFavoritesStore;
import com.microsoft.tfs.core.clients.favorites.exceptions.DuplicateFavoritesException;
import com.microsoft.tfs.util.GUID;

public abstract class AddDefinitionToFavoritesAction extends TeamExplorerBaseAction {
    protected final boolean isPersonal;

    protected AddDefinitionToFavoritesAction(final boolean isPersonal) {
        this.isPersonal = isPersonal;
    }

    protected abstract void fireFavoritesChangedEvent(final TeamExplorerContext context);

    @Override
    public void doRun(final IAction action) {
        @SuppressWarnings("rawtypes")
        Iterator i;

        final List<FavoriteItem> list = new ArrayList<FavoriteItem>();
        for (i = getStructuredSelection().iterator(); i.hasNext();) {
            final Object o = i.next();
            if (o instanceof IBuildDefinition) {
                final IBuildDefinition definition = (IBuildDefinition) o;
                list.add(createFavoriteItem(definition));
            } else if (o instanceof DefinitionReference) {
                final DefinitionReference definition = (DefinitionReference) o;
                list.add(createFavoriteItem(definition));
            }
        }

        final IFavoritesStore store = getFavoritesStore();
        try {
            store.updateFavorites(list.toArray(new FavoriteItem[list.size()]));
            fireFavoritesChangedEvent(getContext());
        } catch (final DuplicateFavoritesException e) {
            FavoriteHelpers.showDuplicateError(getShell(), e, list.size());
        }
    }

    private FavoriteItem createFavoriteItem(final IBuildDefinition definition) {
        final FavoriteItem favorite = new FavoriteItem();
        favorite.setID(GUID.newGUID());
        favorite.setParentID(GUID.EMPTY);
        favorite.setName(definition.getName());
        favorite.setType(BuildFavoriteItem.BUILD_DEFINITION_FAVORITE_TYPE);
        favorite.setData(definition.getURI());
        return favorite;
    }

    private FavoriteItem createFavoriteItem(final DefinitionReference definition) {
        final FavoriteItem favorite = new FavoriteItem();
        favorite.setID(GUID.newGUID());
        favorite.setParentID(GUID.EMPTY);
        favorite.setName(definition.getName());
        favorite.setType(BuildFavoriteItem.BUILD_DEFINITION_FAVORITE_TYPE);
        favorite.setData(definition.getUri().toASCIIString());
        return favorite;
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
