// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.team;

/**
 * Constants associated with 'Teams' in TFS.
 *
 * @threadsafety immutable
 */
public abstract class TeamConstants {
    /**
     * Property on a group which indicates that it is a team.
     */
    public static final String TEAM_PROPERTY_NAME = "Microsoft.TeamFoundation.Team"; //$NON-NLS-1$

    /**
     * Property on a project which indicates the team foundation id of the
     * default team.
     */
    public static final String DEFAULT_TEAM_PROPERTY_NAME = "Microsoft.TeamFoundation.Team.Default"; //$NON-NLS-1$

    /**
     * Property on a group which indicates that it is a team.
     */
    public static final String TEAM_SETTINGS_PROPERTY_NAME = TEAM_PROPERTY_NAME + ".Settings"; //$NON-NLS-1$

    /**
     * Property name for the property that specifies the default value to use in
     * the area path field since there can be more than one
     */
    public static final String DEFAULT_VALUE_INDEX_PROPERTY_NAME = TEAM_SETTINGS_PROPERTY_NAME + ".DefaultValueIndex"; //$NON-NLS-1$

    /**
     * Property name for the property which team field settings.
     */
    public static final String TEAM_FIELD_SETTINGS_PROPERTY_NAME = TEAM_SETTINGS_PROPERTY_NAME + ".TeamFieldSettings"; //$NON-NLS-1$

    /**
     * Property name for the property which contains the 'team field' default
     * values.
     */
    public static final String TEAM_FIELD_DEFAULT_VALUE_INDEX_PROPERTY_NAME =
        TEAM_FIELD_SETTINGS_PROPERTY_NAME + ".DefaultValueIndex"; //$NON-NLS-1$

    /**
     * Property name for the property which contains the 'team field' default
     * values.
     */
    public static final String TEAM_FIELD_VALUES_PROPERTY_NAME = TEAM_FIELD_SETTINGS_PROPERTY_NAME + ".TeamFieldValues"; //$NON-NLS-1$

    /**
     * Format string for setting the default team value for a team.
     */
    public static final String TEAM_FIELD_VALUE_FORMAT =
        "<ArrayOfTeamFieldValue xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><TeamFieldValue><Value>{0}</Value><IncludeChildren>true</IncludeChildren></TeamFieldValue></ArrayOfTeamFieldValue>"; //$NON-NLS-1$

    /**
     * Property name for the Backlog iteration (set to the Guid of the iteration
     * that holds the backlog)
     */
    public static final String BACKLOG_ITERATION_PROPERTY_NAME = TEAM_SETTINGS_PROPERTY_NAME + ".BacklogIterationId"; //$NON-NLS-1$

    /**
     * Property name for the collection of iteration paths that are associated
     * with a team
     */
    public static final String TEAM_ITERATION_PROPERTY_NAME = TEAM_SETTINGS_PROPERTY_NAME + ".TeamIteration"; //$NON-NLS-1$

    /**
     * Property name format for the collection of iteration paths that are
     * associated with a team
     */
    public static final String TEAM_ITERATION_ID_PROPERTY_NAME_FORMAT =
        TEAM_ITERATION_PROPERTY_NAME + ".{0}.iterationId"; //$NON-NLS-1$
}
