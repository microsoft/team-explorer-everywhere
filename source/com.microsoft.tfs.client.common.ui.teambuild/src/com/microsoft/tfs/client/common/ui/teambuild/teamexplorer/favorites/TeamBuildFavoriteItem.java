// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites;

import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.favorites.FavoriteItem;

public class TeamBuildFavoriteItem extends BuildFavoriteItem {
    public TeamBuildFavoriteItem(
        final IBuildServer server,
        final FavoriteItem favoriteItem,
        final DefinitionReference definition) {
        super(server, favoriteItem, definition);
    }
}
