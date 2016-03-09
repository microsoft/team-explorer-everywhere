// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.teamstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.team.TeamService;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.clients.teamsettings.TeamSettingsConfigurationService;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * An in-memory cache of team information for a {@link TFSTeamProjectCollection}
 * . This service is more high-level and user-centric than {@link TeamService}.
 * <p>
 * Do not construct a {@link TeamProjectCollectionTeamStore} directly, use
 * {@link TFSTeamProjectCollection#getClient(Class)} to get one.
 *
 * @since TEE-SDK-11.0
 */
public class TeamProjectCollectionTeamStore {
    private final Object lock = new Object();
    private final TFSTeamProjectCollection teamProjectCollection;
    private final Set<String> projectUris = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private boolean knowSupportsTeam;
    private boolean supportsTeam;
    private List<TeamConfiguration> cache;

    public TeamProjectCollectionTeamStore(final TFSTeamProjectCollection teamProjectCollection) {
        Check.notNull(teamProjectCollection, "teamProjectCollection"); //$NON-NLS-1$
        this.teamProjectCollection = teamProjectCollection;
    }

    /**
     * Property used to determine if the server we are talking to supports the
     * required services.
     *
     * @return true if the server supports Teams, false otherwise
     */
    public boolean supportsTeam() {
        if (!knowSupportsTeam) {
            if (null != teamProjectCollection.getServerDataProvider().locationForCurrentConnection(
                ServiceInterfaceNames.TEAM_CONFIGURATION,
                ServiceInterfaceIdentifiers.TEAM_CONFIGURATION)) {
                // We are talking to TFS Dev11+
                supportsTeam = true;
            } else {
                supportsTeam = false;
            }

            knowSupportsTeam = true;
        }

        return supportsTeam;
    }

    /**
     * Initializes the cache with the {@link TeamConfiguration} for the
     * specified set of projects.
     */
    public void initializeTeamCache(final ProjectInfo[] infos) {
        Check.isTrue(
            supportsTeam(),
            "Teams are not supported for this server, ensure SupportsTeam is true before using this method"); //$NON-NLS-1$

        synchronized (lock) {
            this.projectUris.clear();

            for (final ProjectInfo info : infos) {
                this.projectUris.add(info.getURI());
            }

            updateCache();
        }
    }

    /**
     * Initializes the cache with the {@link TeamConfiguration} for the
     * specified set of projects.
     */
    public void initializeTeamCache(final String[] projectUris) {
        Check.isTrue(
            supportsTeam(),
            "Teams are not supported for this server, ensure SupportsTeam is true before using this method"); //$NON-NLS-1$

        synchronized (lock) {
            this.projectUris.clear();

            for (final String projectUri : projectUris) {
                this.projectUris.add(projectUri);
            }

            updateCache();
        }
    }

    /**
     * Gets all the Teams the active user is a member of for the selected
     * project Uris (selected in the 'Initialize' or via the 'GetTeam' methods).
     */
    public TeamConfiguration[] getTeamsForCurrentUser() {
        Check.isTrue(
            supportsTeam(),
            "Teams are not supported for this server, ensure SupportsTeam is true before using this method"); //$NON-NLS-1$

        TeamConfiguration[] teams;
        synchronized (lock) {
            if (cache == null) {
                updateCache();
            }

            teams = cache.toArray(new TeamConfiguration[cache.size()]);
        }
        return teams;
    }

    /**
     * Gets the corresponding {@link TeamConfiguration} object for the project.
     *
     * @param projectUri
     *        The team project Uri
     * @param teamName
     *        The team name
     */
    public TeamConfiguration getTeam(final String projectUri, final String teamName) {
        Check.isTrue(
            supportsTeam(),
            "Teams are not supported for this server, ensure SupportsTeam is true before using this method"); //$NON-NLS-1$

        synchronized (lock) {
            ensureCacheContainsProjectTeams(projectUri);

            for (final TeamConfiguration conf : cache) {
                if (conf.getProjectURI().equalsIgnoreCase(projectUri)
                    && conf.getTeamName().equalsIgnoreCase(teamName)) {
                    return conf;
                }
            }
        }
        return null;
    }

    /**
     * Gets the corresponding {@link TeamConfiguration} object for the project.
     *
     * @param projectUri
     *        The team project Uri
     * @param id
     *        The TeamFoundationIdentity id for the team
     */
    public TeamConfiguration getTeam(final String projectUri, final GUID id) {
        Check.isTrue(
            supportsTeam(),
            "Teams are not supported for this server, ensure SupportsTeam is true before using this method"); //$NON-NLS-1$

        synchronized (lock) {
            ensureCacheContainsProjectTeams(projectUri);

            for (final TeamConfiguration conf : cache) {
                if (conf.getProjectURI().equalsIgnoreCase(projectUri) && conf.getTeamID().equals(id)) {
                    return conf;
                }
            }
        }
        return null;
    }

    /**
     * Gets all the corresponding {@link TeamConfiguration} objects for the
     * project that the current user belongs to.
     *
     * @param projectUri
     *        The team project Uri
     */
    public TeamConfiguration[] getTeams(final String projectUri) {
        Check.isTrue(
            supportsTeam(),
            "Teams are not supported for this server, ensure SupportsTeam is true before using this method"); //$NON-NLS-1$

        final List<TeamConfiguration> teams = new ArrayList<TeamConfiguration>();
        synchronized (lock) {
            ensureCacheContainsProjectTeams(projectUri);

            for (final TeamConfiguration conf : cache) {
                if (conf.getProjectURI().equalsIgnoreCase(projectUri)) {
                    teams.add(conf);
                }
            }
        }
        return teams.toArray(new TeamConfiguration[teams.size()]);
    }

    private void ensureCacheContainsProjectTeams(final String projectUri) {
        synchronized (lock) {
            if (cache == null || !projectUris.contains(projectUri)) {
                projectUris.add(projectUri);

                updateCache();
            }
        }
    }

    private void updateCache() {
        if (cache == null) {
            cache = new ArrayList<TeamConfiguration>();
        }

        if (projectUris.size() == 0) {
            // Nothing to do.
            cache.clear();
            return;
        }

        final TeamSettingsConfigurationService teamService =
            (TeamSettingsConfigurationService) teamProjectCollection.getClient(TeamSettingsConfigurationService.class);

        final TeamConfiguration[] teams =
            teamService.getTeamConfigurationsForUser(projectUris.toArray(new String[projectUris.size()]));

        cache.clear();

        for (final TeamConfiguration team : teams) {
            cache.add(team);
        }
    }
}