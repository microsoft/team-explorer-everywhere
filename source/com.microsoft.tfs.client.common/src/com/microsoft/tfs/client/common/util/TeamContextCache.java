// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.server.cache.project.ServerProjectCache;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.teamsettings.TeamConfiguration;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * Provides persistence for the following things using the Eclipse preference
 * API:
 * <ul>
 * <li>Active team projects (the ones in the collection the user is working
 * with)</li>
 * <li>Current team project (the single project the Team Explorer has selected)
 * </li>
 * <li>Current TFS 2012 team (the single team the Team Explorer has selected)
 * </li>
 * </ul>
 * <p>
 * Consider using {@link ServerProjectCache}, available from
 * {@link TFSServer#getProjectCache()}, instead of using this class directly.
 * {@link ServerProjectCache} handles retrieving the available projects and
 * teams from the server, which this class doesn't.
 */
public final class TeamContextCache {
    private static final Log log = LogFactory.getLog(TeamContextCache.class);

    private static final String PREFERENCE_CHARSET = "UTF-8"; //$NON-NLS-1$
    private static final String PREFERENCE_KEY_PREFIX = "com.microsoft.tfs.projectsAndTeams"; //$NON-NLS-1$

    private static final String MEMENTO_ROOT_KEY = "projectsAndTeams"; //$NON-NLS-1$

    // 0 or more of these can appear under the root
    private static final String MEMENTO_ACTIVE_PROJECT_KEY = "activeTeamProject"; //$NON-NLS-1$
    private static final String MEMENTO_ACTIVE_PROJECT_NAME_KEY = "name"; //$NON-NLS-1$
    private static final String MEMENTO_ACTIVE_PROJECT_GUID_KEY = "guid"; //$NON-NLS-1$

    // 0 or 1 of these can appear under the root
    private static final String MEMENTO_CURRENT_PROJECT_KEY = "currentTeamProject"; //$NON-NLS-1$
    private static final String MEMENTO_CURRENT_PROJECT_NAME_KEY = "name"; //$NON-NLS-1$
    private static final String MEMENTO_CURRENT_PROJECT_GUID_KEY = "guid"; //$NON-NLS-1$

    // 0 or 1 of these can appear under the root
    private static final String MEMENTO_CURRENT_TEAM_KEY = "currentTeam"; //$NON-NLS-1$
    private static final String MEMENTO_CURRENT_TEAM_NAME_KEY = "name"; //$NON-NLS-1$
    private static final String MEMENTO_CURRENT_TEAM_GUID_KEY = "guid"; //$NON-NLS-1$

    private static final TeamContextCache instance = new TeamContextCache();

    /**
     * The in-memory cache. Synchronized on itself.
     */
    private final Map<String, XMLMemento> cache = new HashMap<String, XMLMemento>();

    private TeamContextCache() {
    }

    public static TeamContextCache getInstance() {
        return instance;
    }

    /**
     * Gets the active team projects.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param allProjects
     *        the current set of all team projects in the collection (the
     *        returned values are taken from this array) (must not be
     *        <code>null</code>)
     * @return the active projects or an empty array if there are no active
     *         projects
     */
    public ProjectInfo[] getActiveTeamProjects(
        final TFSTeamProjectCollection connection,
        final ProjectInfo[] allProjects) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(allProjects, "allProjects"); //$NON-NLS-1$

        final String key = getCacheKey(connection);

        final List<ProjectInfo> activeProjectList = new ArrayList<ProjectInfo>();

        final Memento root = getMemento(key);

        // Ensure each saved project is still available
        final Memento[] activeProjects = root.getChildren(MEMENTO_ACTIVE_PROJECT_KEY);
        for (final Memento activeProject : activeProjects) {
            final GUID activeProjectGUID = new GUID(activeProject.getString(MEMENTO_ACTIVE_PROJECT_GUID_KEY));
            for (final ProjectInfo project : allProjects) {
                if (activeProjectGUID.equals(new GUID(project.getGUID()))) {
                    activeProjectList.add(project);
                    break;
                }
            }
        }

        return activeProjectList.toArray(new ProjectInfo[activeProjectList.size()]);
    }

    /**
     * Sets the active team projects.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param activeTeamProjects
     *        the projects to save (may be <code>null</code> or empty)
     */
    public void setActiveTeamProjects(
        final TFSTeamProjectCollection connection,
        final ProjectInfo[] activeTeamProjects) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final String key = getCacheKey(connection);

        final XMLMemento root = getMemento(key);

        root.removeChildren(MEMENTO_ACTIVE_PROJECT_KEY);

        if (activeTeamProjects != null) {
            for (final ProjectInfo project : activeTeamProjects) {
                final Memento child = root.createChild(MEMENTO_ACTIVE_PROJECT_KEY);
                child.putString(MEMENTO_ACTIVE_PROJECT_NAME_KEY, project.getName());
                child.putString(MEMENTO_ACTIVE_PROJECT_GUID_KEY, project.getGUID());
            }
        }

        setMemento(key, root);
    }

    /**
     * Gets the current team project.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param allProjects
     *        the current set of all team projects in the collection (the
     *        returned value is taken from this array) (must not be
     *        <code>null</code>)
     * @return the current team project or <code>null</code> if there is no
     *         current team project
     */
    public ProjectInfo getCurrentTeamProject(
        final TFSTeamProjectCollection connection,
        final ProjectInfo[] allProjects) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(allProjects, "allProjects"); //$NON-NLS-1$

        final String key = getCacheKey(connection);

        final Memento root = getMemento(key);

        final Memento currentProject = root.getChild(MEMENTO_CURRENT_PROJECT_KEY);
        if (currentProject != null) {
            // Ensure the saved project is still available
            final GUID savedProjectGUID = new GUID(currentProject.getString(MEMENTO_CURRENT_PROJECT_GUID_KEY));
            for (final ProjectInfo project : allProjects) {
                if (savedProjectGUID.equals(new GUID(project.getGUID()))) {
                    return project;
                }
            }
        }

        return null;
    }

    /**
     * Sets the current team projects.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param currentProject
     *        the current project to save (may be <code>null</code>)
     */
    public void setCurrentTeamProject(final TFSTeamProjectCollection connection, final ProjectInfo currentProject) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final String key = getCacheKey(connection);

        final XMLMemento root = getMemento(key);

        root.removeChildren(MEMENTO_CURRENT_PROJECT_KEY);

        if (currentProject != null) {
            final Memento child = root.createChild(MEMENTO_CURRENT_PROJECT_KEY);
            child.putString(MEMENTO_CURRENT_PROJECT_NAME_KEY, currentProject.getName());
            child.putString(MEMENTO_CURRENT_PROJECT_GUID_KEY, currentProject.getGUID());
        }

        setMemento(key, root);
    }

    /**
     * Gets the current TFS 2012 team.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param allTeams
     *        the current set of all teams in the collection (the returned value
     *        is taken from this array) (must not be <code>null</code>)
     * @return the current team or <code>null</code> if there is no current team
     */
    public TeamConfiguration getCurrentTeam(
        final TFSTeamProjectCollection connection,
        final TeamConfiguration[] allTeams) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(allTeams, "allTeams"); //$NON-NLS-1$

        final String key = getCacheKey(connection);

        final Memento root = getMemento(key);

        final Memento currentTeam = root.getChild(MEMENTO_CURRENT_TEAM_KEY);
        if (currentTeam != null) {
            // Ensure the saved project is still available
            final GUID savedTeamGUID = new GUID(currentTeam.getString(MEMENTO_CURRENT_TEAM_GUID_KEY));
            for (final TeamConfiguration team : allTeams) {
                if (savedTeamGUID.equals(team.getTeamID())) {
                    return team;
                }
            }
        }

        return null;
    }

    /**
     * Sets the current TFS 2012 team.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param currentTeam
     *        the current team to save (may be <code>null</code>)
     */
    public void setCurrentTeam(final TFSTeamProjectCollection connection, final TeamConfiguration currentTeam) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        final String key = getCacheKey(connection);

        final XMLMemento root = getMemento(key);

        root.removeChildren(MEMENTO_CURRENT_TEAM_KEY);

        if (currentTeam != null) {
            final Memento child = root.createChild(MEMENTO_CURRENT_TEAM_KEY);
            child.putString(MEMENTO_CURRENT_TEAM_NAME_KEY, currentTeam.getTeamName());
            child.putString(MEMENTO_CURRENT_TEAM_GUID_KEY, currentTeam.getTeamID().getGUIDString());
        }

        setMemento(key, root);
    }

    /**
     * Gets the memento for the specified key, trying the in-memory cache first,
     * then trying the Eclipse preference store. If no saved memento was found,
     * a new {@link XMLMemento} is returned.
     *
     * @param key
     *        the key (must not be <code>null</code>)
     * @return the {@link XMLMemento}, never <code>null</code>
     */
    private XMLMemento getMemento(final String key) {
        Check.notNull(key, "key"); //$NON-NLS-1$

        synchronized (cache) {
            XMLMemento memento = cache.get(key);

            if (memento == null) {
                try {
                    final Preferences preferences = TFSCommonClientPlugin.getDefault().getPluginPreferences();
                    final String preferenceName = getPreferenceName(key);

                    final String mementoString = preferences.getString(preferenceName);

                    if (mementoString != null && mementoString.length() > 0) {
                        memento = XMLMemento.read(
                            new ByteArrayInputStream(mementoString.getBytes(PREFERENCE_CHARSET)),
                            PREFERENCE_CHARSET);
                    } else {
                        memento = new XMLMemento(MEMENTO_ROOT_KEY);
                    }
                } catch (final Exception e) {
                    log.warn("Error loading active project and team information", e); //$NON-NLS-1$
                    memento = new XMLMemento(MEMENTO_ROOT_KEY);
                }

                cache.put(key, memento);
            }

            return memento;
        }
    }

    /**
     * Sets the memento for the specified key into the in-memory cache and the
     * Eclipse preference store.
     * <p>
     * Must hold {@link #cacheLock} while calling this method.
     *
     * @param key
     *        the key (must not be <code>null</code>)
     */
    private void setMemento(final String key, final XMLMemento memento) {
        Check.notNull(key, "key"); //$NON-NLS-1$
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        synchronized (cache) {
            cache.put(key, memento);
        }

        try {
            Check.notNull(memento, "memento"); //$NON-NLS-1$

            final Preferences preferences = TFSCommonClientPlugin.getDefault().getPluginPreferences();
            final String preferenceName = getPreferenceName(key);

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            memento.write(outputStream, PREFERENCE_CHARSET);

            preferences.setValue(preferenceName, outputStream.toString(PREFERENCE_CHARSET));
            TFSCommonClientPlugin.getDefault().savePluginPreferences();
        } catch (final Exception e) {
            log.warn("Error saving active project and team information", e); //$NON-NLS-1$
        }
    }

    private String getPreferenceName(final String key) {
        return PREFERENCE_KEY_PREFIX + "." + key; //$NON-NLS-1$
    }

    private String getCacheKey(final TFSTeamProjectCollection connection) {
        /*
         * So TeamContextCache is useful offline, we must use only attributes
         * from the connection that can be read offline. The connection's
         * instance ID plus the authorized user name would combine to make an
         * ideal key here, but those require a round-trip to the server. The URI
         * will do instead.
         */

        final URI uri = connection.getBaseURI();

        // Convert the URI parts to something more pref-key-like
        return String.format("%s/%s/%d/%s", uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath()).toLowerCase(); //$NON-NLS-1$
    }
}
