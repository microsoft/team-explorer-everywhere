// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.favorites;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.core.clients.favorites.exceptions.FavoritesException;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class QueryFavoriteItem {
    public static final String QUERY_DEFINITION_FEATURE_SCOPE = "WorkItemTracking.Queries"; //$NON-NLS-1$
    public static final String QUERY_DEFINITION_FAVORITE_TYPE = "Microsoft.TeamFoundation.WorkItemTracking.QueryItem"; //$NON-NLS-1$

    private final FavoriteItem favoriteItem;
    private final QueryDefinition queryDefinition;

    protected QueryFavoriteItem(final Project project, final FavoriteItem favoriteItem) {
        this.favoriteItem = favoriteItem;
        this.queryDefinition = getQueryDefinitionFromFavorite(project, favoriteItem);
    }

    public FavoriteItem getFavoriteItem() {
        return favoriteItem;
    }

    public QueryDefinition getQueryDefinition() {
        return queryDefinition;
    }

    private static QueryDefinition getQueryDefinitionFromFavorite(
        final Project project,
        final FavoriteItem favoriteItem) {
        final GUID guid = new GUID(favoriteItem.getData());
        final QueryItem queryItem = project.getQueryHierarchy().find(guid);

        if (queryItem == null || !(queryItem instanceof QueryDefinition)) {
            throw new FavoritesException("Query favorite " + guid + " does not exist."); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return (QueryDefinition) queryItem;
    }

    public static QueryFavoriteItem[] fromFavoriteItems(
        final Project project,
        final FavoriteItem[] favoriteItems,
        final boolean isPersonal) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        if (favoriteItems == null || favoriteItems.length == 0) {
            return new QueryFavoriteItem[0];
        }

        final List<QueryFavoriteItem> list = new ArrayList<QueryFavoriteItem>();
        for (final FavoriteItem favoriteItem : favoriteItems) {
            try {
                if (isPersonal) {
                    list.add(new PrivateQueryFavoriteItem(project, favoriteItem));
                } else {
                    list.add(new TeamQueryFavoriteItem(project, favoriteItem));
                }
            } catch (final FavoritesException e) {
            }
        }
        return list.toArray(new QueryFavoriteItem[list.size()]);
    }
}
