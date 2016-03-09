// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.teamsettings;

import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.tfs.services.teamconfiguration._01._TeamConfigurationServiceSoap;

/**
 * Public interface for the TeamConfigurationService web service. Allows for
 * team configuration items (such as product backlog path, team fields) to be
 * set and retrieved.
 *
 * @since TEE-SDK-11.0
 */
public class TeamSettingsConfigurationService {
    private final _TeamConfigurationServiceSoap internalService;

    public TeamSettingsConfigurationService(final _TeamConfigurationServiceSoap webService) {
        Check.notNull(webService, "webService"); //$NON-NLS-1$

        this.internalService = webService;
    }

    /**
     * Get the team settings for teams that the current user/identity is a
     * member of (scoped to a set of team projects)
     *
     * @param projectUris
     *        The project uris that will be used to scope down the teams that
     *        are returned. If the parameter is <code>null</code> or an empty
     *        enumeration then there is no scoping of the results and the method
     *        will return all teams in the team project collection that the user
     *        is a member of.
     * @return A collection of team settings for each team that the user is a
     *         member of within the given projects
     */
    public TeamConfiguration[] getTeamConfigurationsForUser(String[] projectUris) {
        if (projectUris == null) {
            projectUris = new String[0];
        }

        return (TeamConfiguration[]) WrapperUtils.wrap(
            TeamConfiguration.class,
            internalService.getTeamConfigurationsForUser(projectUris));
    }

    /**
     * Set the team settings for a given team
     *
     * @param teamId
     *        The identifier for the team group
     * @param settings
     *        The settings for the team
     */
    public void setTeamSettings(final GUID teamId, final TeamSettings settings) {
        Check.isTrue(!GUID.EMPTY.equals(teamId), "teamId must not be GUID.EMPTY"); //$NON-NLS-1$
        Check.notNull(settings, "settings"); //$NON-NLS-1$
        Check.notNullOrEmpty(settings.getBacklogIterationPath(), "settings.getBacklogIterationPath()"); //$NON-NLS-1$
        Check.notNull(settings.getTeamFieldValues(), "settings.getTeamFieldValues()"); //$NON-NLS-1$

        if (settings.getIterationPaths() == null) {
            settings.setIterationPaths(new String[0]);
        }

        internalService.setTeamSettings(teamId.getGUIDString(), settings.getWebServiceObject());
    }
}
