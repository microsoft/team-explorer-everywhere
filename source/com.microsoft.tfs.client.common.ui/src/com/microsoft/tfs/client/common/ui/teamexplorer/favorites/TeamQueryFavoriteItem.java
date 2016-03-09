// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.favorites;

import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class TeamQueryFavoriteItem extends QueryFavoriteItem {
    public TeamQueryFavoriteItem(final Project project, final FavoriteItem favoriteItem) {
        super(project, favoriteItem);
    }
}
