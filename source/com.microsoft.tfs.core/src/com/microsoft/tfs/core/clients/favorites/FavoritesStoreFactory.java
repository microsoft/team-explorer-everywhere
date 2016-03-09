// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * Exposes IFavoritesStore implementations to consumers.
 * <p>
 * Only supports {@link IdentityFavoritesStore} implementations from TFS 2010
 * servers and newer. No local store is availble.
 *
 * @since TFS-SDK-11.0
 */
public abstract class FavoritesStoreFactory {
    /**
     * Creates a Favorites store using Identity Service.
     *
     * @param connection
     *        a {@link TFSTeamProjectCollection} (must not be <code>null</code>)
     * @param currentTeam
     *        the current TFS 2012 team or <code>null</code> if there is none or
     *        the results should not be scoped to a team
     * @param currentProject
     *        the current team project or <code>null</code> if there is none
     *        (the method will return <code>null</code> when this is
     *        <code>null</code>)
     * @param featureScope
     *        the feature scope (must not be <code>null</code> or empty)
     * @return an {@link IFavoritesStore} if currentProject was not
     *         <code>null</code> and the favorites store is available from the
     *         server, <code>null</code> otherwise
     */
    public static IFavoritesStore create(
        final TFSTeamProjectCollection connection,
        final ProjectInfo currentProject,
        final TeamConfiguration currentTeam,
        final String featureScope,
        final boolean isPersonal) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNullOrEmpty(featureScope, "featureScope"); //$NON-NLS-1$

        // Must be scoped to a project (currentTeam may be null)
        if (currentProject == null) {
            return null;
        }

        // currentTeam may be null here, and the method may return GUID.EMPTY
        final GUID identity = getIdentity(connection, currentTeam, isPersonal);

        final String filterNamespace = generateQueryNamespace(currentProject, currentTeam, featureScope, isPersonal);

        final IFavoritesStore store = new IdentityFavoritesStore();
        store.connect(connection, filterNamespace, identity);

        // Check that the server is TFS 2010 or later
        if (!store.isConnected()) {
            return null;
        }

        return store;
    }

    /**
     * Get a user or team identity for indexing the favorites which is unique
     * within TPC scope.
     */
    private static GUID getIdentity(
        final TFSTeamProjectCollection connection,
        final TeamConfiguration currentTeam,
        final boolean isPersonal) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        if (isPersonal) {
            return connection.getAuthorizedIdentity().getTeamFoundationID();
        } else {
            if (currentTeam != null) {
                return currentTeam.getTeamID();
            } else {
                return GUID.EMPTY;
            }
        }
    }

    /**
     * Generates Scoping string for querying
     * <p>
     * Note: This convention is currently on a per project basis. Favorites are
     * not retained across projects in web access arrangement.
     */
    private static String generateQueryNamespace(
        final ProjectInfo project,
        final TeamConfiguration team,
        final String featureScope,
        final boolean isPersonal) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        // All this does is ensure that the project data for this team
        // project is loaded.
        final StringBuilder scopeParts = new StringBuilder();

        scopeParts.append(getFavoritesTypeNamespace());

        scopeParts.append('.');
        scopeParts.append(project.getGUID());

        if (!isPersonal && team != null && !GUID.EMPTY.equals(team.getTeamID())) {
            scopeParts.append('.');
            scopeParts.append(team.getTeamID().getGUIDString());
        }

        if (featureScope != null && featureScope.trim().length() > 0) {
            scopeParts.append('.');
            scopeParts.append(featureScope);
        }

        return scopeParts.toString();
    }

    /**
     * Property namespace for the view properties. By default the full name of
     * view type will be the prefix for property names.
     */
    private static String getFavoritesTypeNamespace() {
        // Corresponds to the IdentityFavorties type name defined in the server
        // object model.
        // ~\alm\tfs_core\Framework\Server\IdentityProperties\IdentityPropertiesView.cs
        // ~\alm\tfs_core\Framework\Server\IdentityProperties\IdentityFavorites.cs
        return "Microsoft.TeamFoundation.Framework.Server.IdentityFavorites" + "."; //$NON-NLS-1$ //$NON-NLS-2$
    }
}