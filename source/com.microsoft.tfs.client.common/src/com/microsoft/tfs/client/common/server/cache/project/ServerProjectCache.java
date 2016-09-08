// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.server.cache.project;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.QueryTeamProjectsCommand;
import com.microsoft.tfs.client.common.commands.QueryTeamsCommand;
import com.microsoft.tfs.client.common.framework.command.FutureStatus;
import com.microsoft.tfs.client.common.framework.command.JobCommandExecutor;
import com.microsoft.tfs.client.common.util.TeamContextCache;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.clients.teamstore.TeamProjectCollectionTeamStore;
import com.microsoft.tfs.util.Check;

/**
 * For a project collection, tracks the following things in memory (
 * <strong>*</strong> denotes items automatically persisted with
 * {@link TeamContextCache}):
 * <ul>
 * <li>Available team projects</li>
 * <li>Active team projects (the subset the user is working with)
 * <strong>*</strong></li>
 * <li>Current team project (the single project the Team Explorer has selected)
 * <strong>*</strong></li>
 * <li>Available TFS 2012 teams</li>
 * <li>Current TFS 2012 team (the single team the Team Explorer has selected)
 * <strong>*</strong></li>
 * </ul>
 */
public class ServerProjectCache {
    private static final Log log = LogFactory.getLog(ServerProjectCache.class);

    private final TFSTeamProjectCollection connection;

    private volatile boolean loaded = false;

    /*
     * ProjectAndTeamCache provides efficient access to Active Projects, Current
     * Project, Current Team. Fields here handle All Projects and All Teams and
     * we read/write through to the cache for the others.
     */

    private final Object lock = new Object();
    private ProjectInfo[] allProjects = new ProjectInfo[0];
    private TeamConfiguration[] allTeams = new TeamConfiguration[0];

    public ServerProjectCache(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.connection = connection;
    }

    public void refresh() {
        /*
         * Update team project information
         */
        final QueryTeamProjectsCommand projectCommand = new QueryTeamProjectsCommand(connection);
        final IStatus projectStatus = new JobCommandExecutor().execute(projectCommand);

        if (projectStatus instanceof FutureStatus) {
            ((FutureStatus) projectStatus).join();
        }

        if (!projectStatus.isOK()) {
            log.error(
                MessageFormat.format("Could not update team project list: {0}", projectStatus.getMessage()), //$NON-NLS-1$
                projectStatus.getException());
            return;
        }

        final ProjectInfo[] newAllProjects = projectCommand.getProjects();
        final ProjectInfo[] newActiveProjects =
            TeamContextCache.getInstance().getActiveTeamProjects(connection, newAllProjects);

        /*
         * Update team membership information for the active projects
         */
        final QueryTeamsCommand teamCommand = new QueryTeamsCommand(connection, newActiveProjects);
        final IStatus teamStatus = new JobCommandExecutor().execute(teamCommand);

        if (teamStatus instanceof FutureStatus) {
            ((FutureStatus) teamStatus).join();
        }

        if (!teamStatus.isOK()) {
            log.error(
                MessageFormat.format("Could not update team membership list: {0}", teamStatus.getMessage()), //$NON-NLS-1$
                teamStatus.getException());
            return;
        }

        final TeamConfiguration[] newAllTeams = teamCommand.getTeams();

        synchronized (lock) {
            allProjects = newAllProjects;
            allTeams = newAllTeams;
        }

        loaded = true;
    }

    /**
     * Gets all the team projects that are available to this user in the project
     * collection.
     */
    public ProjectInfo[] getTeamProjects() {
        if (!loaded) {
            refresh();
        }

        synchronized (lock) {
            return allProjects;
        }
    }

    /**
     * Sets which team projects the user chose to work with for this project
     * collection.
     *
     * @param activeTeamProjects
     *        the active projects (may be <code>null</code> or empty)
     */
    public void setActiveTeamProjects(final ProjectInfo[] activeTeamProjects) {
        TeamContextCache.getInstance().setActiveTeamProjects(connection, activeTeamProjects);
    }

    /**
     * Gets the team projects the user chose to work with for this project
     * collection (previously set with
     * {@link #setActiveTeamProjects(ProjectInfo[])});
     *
     * @return the active projects, possibly an empty array but never
     *         <code>null</code>
     */
    public ProjectInfo[] getActiveTeamProjects() {
        if (!loaded) {
            refresh();
        }

        return TeamContextCache.getInstance().getActiveTeamProjects(connection, allProjects);
    }

    /**
     * Sets which team project is currently being used (for instance, by the
     * Team Explorer).
     *
     * @param currentTeamProject
     *        the current project (may be <code>null</code>)
     */
    public void setCurrentTeamProject(final ProjectInfo currentTeamProject) {
        TeamContextCache.getInstance().setCurrentTeamProject(connection, currentTeamProject);
    }

    /**
     * Gets the team project by its name.
     *
     * @return the team project (may be <code>null</code>)
     */
    public ProjectInfo getTeamProject(final String projectName) {
        if (!loaded) {
            refresh();
        }

        for (final ProjectInfo project : allProjects) {
            if (project.getName().equalsIgnoreCase(projectName)) {
                return project;
            }
        }

        return null;
    }

    /**
     * Gets the team project that is currently being used (for instance, by the
     * Team Explorer).
     *
     * @return the team project (may be <code>null</code>)
     */
    public ProjectInfo getCurrentTeamProject() {
        if (!loaded) {
            refresh();
        }

        return TeamContextCache.getInstance().getCurrentTeamProject(connection, allProjects);
    }

    /**
     * Tests whether the server supports the TFS 2012 team services. A team is a
     * logical grouping of users and groups under a team project.
     * <p>
     * If teams are not supported, {@link #getTeams()} will always return
     * <code>null</code>.
     *
     * @return <code>true</code> if the connected server supports the TFS 2012
     *         team services <code>false</code> if it does not
     */
    public boolean supportsTeam() {
        return ((TeamProjectCollectionTeamStore) connection.getClient(
            TeamProjectCollectionTeamStore.class)).supportsTeam();
    }

    /**
     * Gets the TFS 2012 teams available for the authorized user for all team
     * projects in the project collection.
     *
     * @return the teams, may be an empty array but never <code>null</code>
     */
    public TeamConfiguration[] getTeams() {
        if (!supportsTeam()) {
            return new TeamConfiguration[0];
        }

        if (!loaded) {
            refresh();
        }

        synchronized (lock) {
            return allTeams;
        }
    }

    /**
     * Gets the TFS 2012 teams available for the authorized user for the
     * specified project in the project collection.
     *
     * @param project
     *        the project to get the teams for (must not be <code>null</code>)
     * @return the teams, may be an empty array but never <code>null</code>
     */
    public TeamConfiguration[] getTeams(final ProjectInfo project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        return getTeams(new ProjectInfo[] {
            project
        });
    }

    /**
     * Gets the TFS 2012 teams available for the authorized user for the
     * specified projects in the project collection.
     *
     * @param projects
     *        the projects to get the teams for (must not be <code>null</code>)
     * @return the teams, may be an empty array but never <code>null</code>
     */
    public TeamConfiguration[] getTeams(final ProjectInfo[] projects) {
        Check.notNull(projects, "projects"); //$NON-NLS-1$

        if (!supportsTeam()) {
            return new TeamConfiguration[0];
        }

        if (!loaded) {
            refresh();
        }

        final List<TeamConfiguration> matches = new ArrayList<TeamConfiguration>();

        synchronized (lock) {
            for (final ProjectInfo project : projects) {
                for (final TeamConfiguration team : allTeams) {
                    if (project.getURI().equalsIgnoreCase(team.getProjectURI())) {
                        matches.add(team);
                    }
                }
            }
        }

        return matches.toArray(new TeamConfiguration[matches.size()]);
    }

    /**
     * Sets which TFS 2012 team is currently being used (for instance, by the
     * Team Explorer).
     *
     * @param currentTeam
     *        the current team (may be <code>null</code> or empty)
     */
    public void setCurrentTeam(final TeamConfiguration currentTeam) {
        if (!supportsTeam()) {
            return;
        }

        TeamContextCache.getInstance().setCurrentTeam(connection, currentTeam);
    }

    /**
     * Gets the TFS 2012 team that is currently being used (for instance, by the
     * Team Explorer).
     *
     * @return the active team, <code>null</code> if there is no active team or
     *         the server does not support teams
     */
    public TeamConfiguration getCurrentTeam() {
        if (!supportsTeam()) {
            return null;
        }

        if (!loaded) {
            refresh();
        }

        return TeamContextCache.getInstance().getCurrentTeam(connection, allTeams);
    }
}
