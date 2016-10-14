// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.alm.client.TeeClientHandler;
import com.microsoft.alm.teamfoundation.build.webapi.BuildDefinition;
import com.microsoft.alm.teamfoundation.build.webapi.BuildHttpClient;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.alm.teamfoundation.build.webapi.DefinitionType;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.core.clients.favorites.exceptions.FavoritesException;
import com.microsoft.tfs.util.Check;

public abstract class BuildFavoriteItem {
    public static final String BUILD_DEFINITION_FEATURE_SCOPE = "Build.Definitions"; //$NON-NLS-1$
    public static final String BUILD_DEFINITION_FAVORITE_TYPE = "Microsoft.TeamFoundation.Build.Definition"; //$NON-NLS-1$

    private final IBuildServer server;
    private final FavoriteItem favoriteItem;
    private DefinitionReference definition;
    private IBuildDefinition buildDefinition;

    protected BuildFavoriteItem(
        final IBuildServer server,
        final FavoriteItem favoriteItem,
        final DefinitionReference definition) {
        this.server = server;
        this.favoriteItem = favoriteItem;
        this.definition = definition;
        if (getBuildDefinitionType() == DefinitionType.XAML) {
            this.buildDefinition = getXamlDefinitionFromFavorite(server, favoriteItem);
        } else {
            this.buildDefinition = null;
        }
    }

    public FavoriteItem getFavoriteItem() {
        return favoriteItem;
    }

    public Object getBuildDefinition() {
        if (getBuildDefinitionType() == DefinitionType.XAML) {
            return buildDefinition;
        } else {
            return definition;
        }
    }

    public String getBuildDefinitionUri() {
        if (definition == null) {
            return buildDefinition.getURI().toString();
        } else {
            return definition.getUri().toString();
        }
    }

    public String getBuildDefinitionName() {
        if (definition == null) {
            return buildDefinition.getName();
        } else {
            return definition.getName();
        }
    }

    public DefinitionType getBuildDefinitionType() {
        if (definition == null) {
            return DefinitionType.XAML;
        } else {
            return definition.getType();
        }
    }

    public void refresh() {
        if (getBuildDefinitionType() == DefinitionType.XAML) {
            this.buildDefinition = getXamlDefinitionFromFavorite(server, favoriteItem);
        } else {
            this.definition = getBuildDefinitionFromFavorite(server, (BuildDefinition) definition);
        }
    }

    private static IBuildDefinition getXamlDefinitionFromFavorite(
        final IBuildServer server,
        final FavoriteItem favoriteItem) {
        final String uri = favoriteItem.getData();
        final IBuildDefinition definition = server.getBuildDefinition(uri);

        if (definition == null || definition.getURI() == null) {
            throw new FavoritesException("Build favorite " + uri + " does not exist."); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return definition;
    }

    private static BuildDefinition getBuildDefinitionFromFavorite(
        final IBuildServer server,
        final BuildDefinition oldDefinition) {

        final TFSTeamProjectCollection connection = server.getConnection();
        final BuildHttpClient buildClient =
            new BuildHttpClient(new TeeClientHandler(connection.getHTTPClient()), connection.getBaseURI());

        final DefinitionReference definition =
            buildClient.getDefinition(oldDefinition.getProject().getId(), oldDefinition.getId(), null, null);

        if (definition == null || !(definition instanceof BuildDefinition) || definition.getUri() == null) {
            throw new FavoritesException("Build favorite " + oldDefinition.getUri() + " does not exist."); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return (BuildDefinition) definition;
    }

    public static BuildFavoriteItem[] fromFavoriteItems(
        final IBuildServer server,
        final FavoriteItem[] favoriteItems,
        final Map<String, DefinitionReference> definitions,
        final boolean isPersonal) {
        Check.notNull(server, "server"); //$NON-NLS-1$

        if (favoriteItems == null || favoriteItems.length == 0) {
            return new PrivateBuildFavoriteItem[0];
        }

        final List<BuildFavoriteItem> list = new ArrayList<BuildFavoriteItem>();
        for (final FavoriteItem favoriteItem : favoriteItems) {
            if (favoriteItem.getType().equals(BUILD_DEFINITION_FAVORITE_TYPE)) {
                try {
                    if (isPersonal) {
                        list.add(
                            new PrivateBuildFavoriteItem(
                                server,
                                favoriteItem,
                                definitions.get(favoriteItem.getData())));
                    } else {
                        list.add(
                            new TeamBuildFavoriteItem(server, favoriteItem, definitions.get(favoriteItem.getData())));
                    }
                } catch (final FavoritesException e) {
                }
            }
        }
        return list.toArray(new BuildFavoriteItem[list.size()]);
    }
}
