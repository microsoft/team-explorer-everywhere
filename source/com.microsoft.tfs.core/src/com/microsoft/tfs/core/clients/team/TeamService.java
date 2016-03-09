// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.core.clients.commonstructure.ProjectProperty;
import com.microsoft.tfs.core.clients.security.FrameworkSecurity;
import com.microsoft.tfs.core.clients.security.SecurityNamespace;
import com.microsoft.tfs.core.clients.security.SecurityService;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService2;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.webservices.IdentityPermissions;
import com.microsoft.tfs.core.clients.webservices.IdentityPropertyScope;
import com.microsoft.tfs.core.clients.webservices.IdentitySearchFactor;
import com.microsoft.tfs.core.clients.webservices.MembershipQuery;
import com.microsoft.tfs.core.clients.webservices.ReadIdentityOptions;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * A client for the TFS 2012 "Team" services.
 *
 * @since TEE-SDK-11.0
 * @threadsafety thread-compatible
 */
public class TeamService {
    private final TFSConnection connection;

    public TeamService(final TFSConnection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    /**
     * Create a Team on server with optional properties
     *
     * @param projectId
     *        Project Uri (scope id)
     * @param name
     *        Team name
     * @param description
     *        Team description
     * @param properties
     *        properties, can be null or empty
     * @return TeamFoundationTeam
     */
    public TeamFoundationTeam createTeam(
        final String projectId,
        final String name,
        final String description,
        final Map<String, Object> properties) {
        // TODO when we have CssUtils

        /*
         * // Since Team name is part of web access Url, and we may // create
         * Area Path by same name in future, Team name will // be subject to
         * same restriction as project name. if
         * (!CssUtils.IsValidProjectName(name)) { throw new
         * ArgumentException(String.Format(CultureInfo.CurrentCulture,
         * CommonResources.BAD_GROUP_NAME(name))); }
         */

        final IIdentityManagementService2 identitySvc =
            (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);
        final IdentityDescriptor desc = identitySvc.createApplicationGroup(projectId, name, description);
        final TeamFoundationIdentity group =
            identitySvc.readIdentity(desc, MembershipQuery.NONE, ReadIdentityOptions.NONE);

        // Set Team property
        group.setProperty(IdentityPropertyScope.LOCAL, TeamConstants.TEAM_PROPERTY_NAME, true);

        // Set any other properties
        if (properties != null) {
            for (final Map.Entry<String, Object> property : properties.entrySet()) {
                group.setProperty(IdentityPropertyScope.LOCAL, property.getKey(), property.getValue());
            }
        }

        // Persist
        identitySvc.updateExtendedProperties(group);

        // grant current user explicit admin permission to team
        // For a TFS group, security token is scopeUri\groupId. the project or
        // host URI is stored
        // as the domain of the identity
        final SecurityService client = (SecurityService) connection.getClient(SecurityService.class);

        final SecurityNamespace security = client.getSecurityNamespace(FrameworkSecurity.IDENTITIES_NAMESPACE_ID);
        final String token = IdentityHelper.createSecurityToken(group);
        security.setPermissions(
            token,
            connection.getAuthorizedIdentity().getDescriptor(),
            IdentityPermissions.ALL_PERMISSIONS.toIntFlags(),
            0,
            false);

        return new TeamFoundationTeam(group);
    }

    /**
     * <p>
     * Query all Team groups in given project.
     * </p>
     *
     * @param projectId
     *        Project Uri (scope id)
     */
    public TeamFoundationTeam[] queryTeams(final String projectId) {
        // TODO - Until IMS provides such a query we will filter project
        // groups ourselves. Note that only Team property is requested,
        // not all properties.
        final IIdentityManagementService2 identitySvc =
            (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);
        final TeamFoundationIdentity[] projGroups =
            identitySvc.listApplicationGroups(projectId, ReadIdentityOptions.EXTENDED_PROPERTIES, new String[] {
                TeamConstants.TEAM_PROPERTY_NAME
        }, IdentityPropertyScope.LOCAL);

        final List<TeamFoundationTeam> teams = new ArrayList<TeamFoundationTeam>();
        for (final TeamFoundationIdentity projGroup : projGroups) {
            final AtomicReference<Object> object = new AtomicReference<Object>();
            if (projGroup.tryGetProperty(TeamConstants.TEAM_PROPERTY_NAME, object)) {
                teams.add(new TeamFoundationTeam(projGroup));
            }
        }

        return teams.toArray(new TeamFoundationTeam[teams.size()]);
    }

    /**
     * Query all Team groups that given user is a member of.
     */
    public TeamFoundationTeam[] queryTeams(final IdentityDescriptor descriptor) {
        return queryTeams(descriptor, new String[] {
            TeamConstants.TEAM_PROPERTY_NAME
        });
    }

    /**
     * Query all Team groups that given user is a member of.
     */
    public TeamFoundationTeam[] queryTeams(final IdentityDescriptor descriptor, String[] propertyNameFilters) {
        // TODO - Until IMS provides such a query we will get all parent groups
        // and filter Teams ourselves.
        final IIdentityManagementService2 identitySvc =
            (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);
        final TeamFoundationIdentity user = identitySvc.readIdentity(
            descriptor,
            MembershipQuery.EXPANDED,
            ReadIdentityOptions.NONE,
            null,
            IdentityPropertyScope.NONE);

        final List<TeamFoundationTeam> teams = new ArrayList<TeamFoundationTeam>();
        if (user != null) {
            propertyNameFilters = PropertyUtils.mergePropertyFilters(propertyNameFilters, new String[] {
                TeamConstants.TEAM_PROPERTY_NAME
            });

            final TeamFoundationIdentity[] parentGroups = identitySvc.readIdentities(
                user.getMemberOf(),
                MembershipQuery.NONE,
                ReadIdentityOptions.EXTENDED_PROPERTIES,
                propertyNameFilters,
                IdentityPropertyScope.LOCAL);

            for (final TeamFoundationIdentity group : parentGroups) {
                final AtomicReference<Object> object = new AtomicReference<Object>();
                if (group.tryGetProperty(TeamConstants.TEAM_PROPERTY_NAME, object)) {
                    teams.add(new TeamFoundationTeam(group));
                }
            }

        }
        return teams.toArray(new TeamFoundationTeam[teams.size()]);
    }

    /**
     * Defines a type that can retrieve a {@link TeamFoundationIdentity} for the
     * identifcation object type T.
     */
    private interface TeamRetriever<T> {
        TeamFoundationIdentity retrieve(T teamDescriptor, String[] propertyNameFilters);
    }

    private <T> TeamFoundationTeam readTeamInternal(
        final TeamRetriever<T> teamRetriever,
        final T teamDescriptor,
        String[] propertyNameFilters) {
        // Add Team property to filters
        propertyNameFilters = PropertyUtils.mergePropertyFilters(propertyNameFilters, new String[] {
            TeamConstants.TEAM_PROPERTY_NAME
        });

        final TeamFoundationIdentity team = teamRetriever.retrieve(teamDescriptor, propertyNameFilters);

        final AtomicReference<Object> object = new AtomicReference<Object>();
        if (team == null || !team.isActive() || !team.tryGetProperty(TeamConstants.TEAM_PROPERTY_NAME, object)) {
            return null;
        } else {
            return new TeamFoundationTeam(team);
        }
    }

    /**
     * <p>
     * Read Team group by descriptor
     * </p>
     *
     * @param propertyNameFilters
     *        specifies extended properties to read, refer Prpoperty Service
     *        API. Set to null to read all properties.
     * @return Team object if group exists AND is a team, else null
     */
    public TeamFoundationTeam readTeam(final IdentityDescriptor descriptor, final String[] propertyNameFilters) {
        final TeamRetriever<IdentityDescriptor> teamRetriever = new TeamRetriever<IdentityDescriptor>() {
            @Override
            public TeamFoundationIdentity retrieve(final IdentityDescriptor id, final String[] filters) {
                final IIdentityManagementService2 identitySvc =
                    (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);
                return identitySvc.readIdentity(
                    id,
                    MembershipQuery.NONE,
                    ReadIdentityOptions.EXTENDED_PROPERTIES,
                    filters,
                    IdentityPropertyScope.LOCAL);
            }
        };

        return this.readTeamInternal(teamRetriever, descriptor, propertyNameFilters);
    }

    /**
     * <p>
     * Read Team group by descriptor
     * </p>
     *
     * @param propertyNameFilters
     *        specifies extended properties to read, refer Prpoperty Service
     *        API. Set to null to read all properties.
     * @return Team object if group exists AND is a team, else null
     *         <p>
     *         Read Team group by id
     *         </p>
     * @param propertyNameFilters
     *        specifies extended properties to read, refer Prpoperty Service
     *        API. Set to null to read all properties.
     * @return Team object if group exists AND is a team, else null
     */
    public TeamFoundationTeam readTeam(final GUID teamId, final String[] propertyNameFilters) {
        final TeamRetriever<GUID> teamRetriever = new TeamRetriever<GUID>() {
            @Override
            public TeamFoundationIdentity retrieve(final GUID id, final String[] filters) {
                final IIdentityManagementService2 identitySvc =
                    (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);

                return identitySvc.readIdentities(
                    new GUID[] {
                        id
                },
                    MembershipQuery.NONE,
                    ReadIdentityOptions.EXTENDED_PROPERTIES,
                    filters,
                    IdentityPropertyScope.LOCAL)[0];
            }
        };

        return this.readTeamInternal(teamRetriever, teamId, propertyNameFilters);
    }

    /**
     * <p>
     * Read Team group by name in given project
     * </p>
     *
     * @param projectId
     *        Project Uri (scope id)
     * @param propertyNameFilters
     *        specifies extended properties to read, refer Prpoperty Service
     *        API. Set to null to read all properties.
     * @return Team object if group exists AND is a team, else null
     */
    public TeamFoundationTeam readTeam(
        final String projectId,
        final String teamName,
        final String[] propertyNameFilters) {
        final TeamRetriever<String> teamRetriever = new TeamRetriever<String>() {
            @Override
            public TeamFoundationIdentity retrieve(final String id, final String[] filters) {
                final IIdentityManagementService2 identitySvc =
                    (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);

                final String searchFactor = projectId + "\\" + id; //$NON-NLS-1$

                return identitySvc.readIdentity(
                    IdentitySearchFactor.ACCOUNT_NAME,
                    searchFactor,
                    MembershipQuery.NONE,
                    ReadIdentityOptions.EXTENDED_PROPERTIES,
                    filters,
                    IdentityPropertyScope.LOCAL);
            }
        };

        return this.readTeamInternal(teamRetriever, teamName, propertyNameFilters);
    }

    /**
     * <p>
     * Returns default team id for the project.
     * </p>
     *
     * @param projectUri
     * @return
     */
    public GUID getDefaultTeamID(final String projectUri) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$

        final CommonStructureClient cssSvc = (CommonStructureClient) connection.getClient(CommonStructureClient.class);
        final ProjectProperty defaultTeamProp =
            cssSvc.getProjectProperty(projectUri, TeamConstants.DEFAULT_TEAM_PROPERTY_NAME);

        if (defaultTeamProp != null && defaultTeamProp.getValue() != null && defaultTeamProp.getValue().length() > 0) {
            return new GUID(defaultTeamProp.getValue());
        }

        return GUID.EMPTY;
    }

    /**
     * <p>
     * Gets default team for the project
     * </p>
     *
     * @param projectUri
     * @param propertyNameFilters
     * @return
     */
    public TeamFoundationTeam getDefaultTeam(final String projectUri, final String[] propertyNameFilters) {
        final GUID teamId = getDefaultTeamID(projectUri);

        TeamFoundationTeam team = null;

        if (teamId != GUID.EMPTY) {
            team = readTeam(teamId, propertyNameFilters);
        }

        return team;
    }

    /**
     * <p>
     * Sets default team for the project
     * </p>
     *
     * @param team
     */
    public void setDefaultTeam(final TeamFoundationTeam team) {
        Check.notNull(team, "team"); //$NON-NLS-1$

        setDefaultTeamID(team.getProject(), team.getIdentity().getTeamFoundationID());
    }

    /**
     * <p>
     * Sets default team for the project
     * </p>
     *
     * @param projectUri
     * @param teamId
     */
    public void setDefaultTeamID(final String projectUri, final GUID teamId) {
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$

        final CommonStructureClient cssSvc = (CommonStructureClient) connection.getClient(CommonStructureClient.class);
        cssSvc.setProjectProperty(projectUri, TeamConstants.DEFAULT_TEAM_PROPERTY_NAME, teamId.getGUIDString());
    }

    /**
     * <p>
     * Persist Team updates.
     * </p>
     */
    public void updateTeam(final TeamFoundationTeam team) {
        final IIdentityManagementService2 identitySvc =
            (IIdentityManagementService2) connection.getClient(IIdentityManagementService2.class);

        identitySvc.updateExtendedProperties(team.getIdentity());
    }
}
