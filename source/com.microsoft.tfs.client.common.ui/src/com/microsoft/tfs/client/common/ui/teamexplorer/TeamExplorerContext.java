// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.part.WorkbenchPart;

import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.teamexplorer.TeamExplorerControl;
import com.microsoft.tfs.client.common.ui.framework.telemetry.ClientTelemetryHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.telemetry.TfsTelemetryConstants;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class TeamExplorerContext {
    private ProjectInfo currentProject;
    private TeamConfiguration currentTeam;
    private final ViewPart viewPart;

    private final Map<String, Object> mapData = new HashMap<String, Object>();
    private final TeamExplorerEvents events;

    public TeamExplorerContext(final ViewPart viewPart) {
        this.viewPart = viewPart;
        this.events = new TeamExplorerEvents();
        ConnectHelpers.showHideViews(getSourceControlCapability());
    }

    public TeamExplorerEvents getEvents() {
        return events;
    }

    public TFSRepository getDefaultRepository() {
        final RepositoryManager repositoryManager =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager();
        return repositoryManager.getDefaultRepository();
    }

    public void refresh() {
        currentProject = null;
        currentTeam = null;
        // Preserve the map data for third parties
    }

    public WorkbenchPart getWorkbenchPart() {
        return viewPart;
    }

    public void add(final String key, final Object value) {
        Check.notNull(key, "key"); //$NON-NLS-1$
        mapData.put(key, value);
    }

    public Object get(final String key) {
        Check.notNull(key, "key"); //$NON-NLS-1$
        return mapData.get(key);
    }

    public boolean containsKey(final String key) {
        Check.notNull(key, "key"); //$NON-NLS-1$
        return mapData.containsKey(key);
    }

    public synchronized TFSServer getServer() {
        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();
    }

    /**
     * Gets the current team project that was most recently selected in the
     * {@link TeamExplorerControl} or was set with
     * {@link #setCurrentProject(String)}.
     *
     * @return the current team project or <code>null</code> if not connected or
     *         if there is no current team project
     */
    public synchronized Project getCurrentProject() {
        final TFSServer server = getServer();
        if (server == null || server.getConnection() == null || getCurrentProjectInfo() == null) {
            return null;
        }

        final String projectName = getCurrentProjectInfo().getName();
        if (projectName == null) {
            return null;
        }

        return server.getConnection().getWorkItemClient().getProjects().get(projectName);
    }

    /**
     * Gets the current team project that was most recently selected in the
     * {@link TeamExplorerControl} or was set with
     * {@link #setCurrentProject(String)}.
     *
     * @return the current team project or <code>null</code> if not connected or
     *         if there is no current team project
     */
    public synchronized ProjectInfo getCurrentProjectInfo() {
        if (currentProject == null) {
            final TFSServer server = getServer();
            if (server != null) {
                ProjectInfo currentProjectCandidate = server.getProjectCache().getCurrentTeamProject();

                final ProjectInfo[] activeProjects = server.getProjectCache().getActiveTeamProjects();

                if (currentProjectCandidate != null) {
                    // Ensure the current project is still active
                    boolean found = false;
                    for (final ProjectInfo project : activeProjects) {
                        if (project.equals(currentProjectCandidate)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        currentProjectCandidate = null;
                    }
                }

                if (currentProjectCandidate == null) {
                    // No saved project or saved project no longer active, use
                    // the default
                    if (activeProjects.length > 0) {
                        currentProjectCandidate = activeProjects[0];
                    }
                }

                setCurrentProject(currentProjectCandidate);
            }
        }

        return currentProject;
    }

    /**
     * Sets the current team project. The project must be an "active" team
     * project (one the user has selected to work with inside this team project
     * collection).
     *
     * @param projectGUID
     *        the {@link GUID} of the project (may be <code>null</code>)
     */
    public synchronized void setCurrentProject(final String projectGUID) {
        if (projectGUID == null) {
            currentProject = null;
            getServer().getProjectCache().setCurrentTeamProject(null);
            return;
        }

        final TFSServer server = getServer();
        Check.notNull(server, "server"); //$NON-NLS-1$

        final ProjectInfo[] activeProjects = server.getProjectCache().getActiveTeamProjects();
        Check.notNull(activeProjects, "activeProjects"); //$NON-NLS-1$

        for (final ProjectInfo activeProject : activeProjects) {
            if (activeProject.getGUID().equals(projectGUID)) {
                if (currentProject == null || !currentProject.getGUID().equalsIgnoreCase(activeProject.getGUID())) {
                    final Map<String, String> properties = new HashMap<String, String>();
                    final boolean tfvc =
                        activeProject.getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.TFS);
                    final String vc = tfvc ? "tfvc" : "git"; //$NON-NLS-1$ //$NON-NLS-2$
                    properties.put(TfsTelemetryConstants.PLUGIN_COMMAND_EVENT_PROPERTY_VERSION_CONTROL, vc);

                    ClientTelemetryHelper.sendRunActionEvent("ProjectSelect", properties); //$NON-NLS-1$
                }

                currentProject = activeProject;
                server.getProjectCache().setCurrentTeamProject(currentProject);

                return;
            }
        }

        final String format = Messages.getString("TeamExplorerContext.ProjectNotActiveExceptionFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(format, projectGUID);
        throw new IllegalArgumentException(message);
    }

    /**
     * @equivalence setCurrentProject(projectInfo.getGUID());
     */
    public synchronized void setCurrentProject(final ProjectInfo projectInfo) {
        setCurrentProject(projectInfo != null ? projectInfo.getGUID() : null);
    }

    /**
     * @equivalence setCurrentProject(project.getGUID().getGUIDString());
     */
    public synchronized void setCurrentProject(final Project project) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        setCurrentProject(project != null ? project.getGUID().getGUIDString() : null);
    }

    /**
     * Gets the current TFS 2012 team that was most recently selected in the
     * {@link TeamExplorerControl} or was set with
     * {@link #setCurrentTeam(TeamConfiguration)}.
     *
     * @return the current team or <code>null</code> if not connected, or if no
     *         team project has been selected, or if the server does not support
     *         teams
     */
    public synchronized TeamConfiguration getCurrentTeam() {
        if (currentTeam == null) {
            final TFSServer server = getServer();
            if (server != null) {
                currentTeam = server.getProjectCache().getCurrentTeam();

                final ProjectInfo currentProject = getCurrentProjectInfo();
                if (currentProject == null) {
                    return null;
                }

                final TeamConfiguration[] currentProjectTeams = server.getProjectCache().getTeams(currentProject);

                if (currentTeam != null) {
                    // Ensure the current team is still available in this
                    // project
                    boolean found = false;
                    for (final TeamConfiguration team : currentProjectTeams) {
                        if (team.equals(currentTeam)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        currentTeam = null;
                    }
                }

                // No saved team or saved team no longer active, use the default
                if (currentTeam == null) {
                    for (final TeamConfiguration team : currentProjectTeams) {
                        if (team.isDefaultTeam()) {
                            currentTeam = team;
                            break;
                        }
                    }

                    // currentTeam remains null here for pre-TFS 2012 servers
                }
            }
        }
        return currentTeam;
    }

    /**
     * Sets the current TFS 2012 team.
     *
     * @param currentTeam
     *        the team to set as the current team (may be <code>null</code>)
     */
    public synchronized void setCurrentTeam(final TeamConfiguration currentTeam) {
        if (currentTeam == null) {
            this.currentTeam = null;
            getServer().getProjectCache().setCurrentTeam(null);
            return;
        }

        final TFSServer server = getServer();
        Check.notNull(server, "server"); //$NON-NLS-1$

        final TeamConfiguration[] teams = server.getProjectCache().getTeams();
        Check.notNull(teams, "teams"); //$NON-NLS-1$

        for (final TeamConfiguration team : teams) {
            if (currentTeam.getTeamID().equals(team.getTeamID())) {
                this.currentTeam = team;
                server.getProjectCache().setCurrentTeam(team);
                return;
            }
        }

        throw new IllegalArgumentException(
            MessageFormat.format(
                Messages.getString("TeamExplorerContext.TeamDoesNotExistFormat"), //$NON-NLS-1$
                currentTeam.getTeamName()));
    }

    public WorkItemClient getWorkItemClient() {
        if (getServer() == null) {
            return null;
        }

        if (getServer().getConnection() == null) {
            return null;
        }

        return getServer().getConnection().getWorkItemClient();
    }

    public IBuildServer getBuildServer() {
        if (getServer() == null) {
            return null;
        }

        if (getServer().getConnection() == null) {
            return null;
        }

        return getServer().getConnection().getBuildServer();
    }

    public synchronized boolean isConnected() {
        return isConnectedToCollection() && getServer().getProjectCache().getActiveTeamProjects().length > 0;
    }

    public synchronized boolean isConnectedToCollection() {
        final TFSRepository repository = getDefaultRepository();

        return repository != null
            && repository.getConnection() != null
            && !repository.getConnection().getConnectivityFailureOnLastWebServiceCall();
    }

    public synchronized SourceControlCapabilityFlags getSourceControlCapability() {
        if (!isConnected()) {
            return SourceControlCapabilityFlags.NONE;
        } else {
            final ProjectInfo currentProject = getCurrentProjectInfo();
            if (currentProject != null) {
                return currentProject.getSourceControlCapabilityFlags();
            } else {
                return SourceControlCapabilityFlags.NONE;
            }
        }
    }
}
