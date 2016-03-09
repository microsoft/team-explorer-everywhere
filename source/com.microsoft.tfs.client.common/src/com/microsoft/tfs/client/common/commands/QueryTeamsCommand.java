// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.clients.teamstore.TeamProjectCollectionTeamStore;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Gets the TFS 2012 teams available for the currently authorized user. If the
 * server does not support teams, an empty team list is returned.
 */
public class QueryTeamsCommand extends TFSConnectedCommand {
    private final TFSTeamProjectCollection connection;
    private final ProjectInfo[] projects;

    private TeamConfiguration[] teams;

    public QueryTeamsCommand(final TFSTeamProjectCollection connection, final ProjectInfo[] projects) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(projects, "projects"); //$NON-NLS-1$

        this.connection = connection;
        this.projects = projects;

        setConnection(connection);
    }

    @Override
    public String getName() {
        return (Messages.getString("QueryTeamsCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("QueryTeamsCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("QueryTeamsCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(
            Messages.getString("QueryTeamsCommand.ProgressMonitorTitle"), //$NON-NLS-1$
            IProgressMonitor.UNKNOWN);

        try {
            final TeamProjectCollectionTeamStore teamStore =
                (TeamProjectCollectionTeamStore) connection.getClient(TeamProjectCollectionTeamStore.class);

            if (teamStore.supportsTeam()) {
                /*
                 * Initialize the cache for efficiency. This lets the cache do
                 * just one server round-trip.
                 */
                teamStore.initializeTeamCache(projects);
                teams = teamStore.getTeamsForCurrentUser();
            } else {
                teams = new TeamConfiguration[0];
            }
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }

    /**
     * @return the teams for the specified projects that the current user is a
     *         member of, or an empty list if the server does not support teams
     */
    public TeamConfiguration[] getTeams() {
        return teams;
    }
}