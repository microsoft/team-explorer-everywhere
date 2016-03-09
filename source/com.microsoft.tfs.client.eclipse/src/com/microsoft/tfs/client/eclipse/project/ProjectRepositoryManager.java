// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.framework.background.BackgroundTask;
import com.microsoft.tfs.client.common.framework.background.IBackgroundTask;
import com.microsoft.tfs.client.common.framework.command.ExtensionPointAsyncObjectWaiter;
import com.microsoft.tfs.client.common.license.LicenseListener;
import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.RepositoryManagerAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.RepositoryManagerListener;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.license.TFSEclipseClientLicenseManager;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * ProjectRepositoryManager manages the Eclipse Plugins {@link IProject}s, and
 * handles the connection and disconnection of them.
 *
 * ProjectRepositoryManager is configured by the {@link TFSEclipseClientPlugin}
 * at startup. At this point, it queries all projects in the workspace for
 * TFS-managed projects and will reconnect them (if they were previously
 * connected to the TFS server. Offline projects are left offline.)
 *
 * New projects (ie, configured through the Import or Share Wizards) must be
 * added to this manager manually, by
 * {@link #addProject(IProject, TFSRepository)}.
 *
 * @threadsafety unknown
 */
public class ProjectRepositoryManager {
    private static final Log log = LogFactory.getLog(ProjectRepositoryManager.class);

    public static final CodeMarker FINISH_PROJECT_ADDITION =
        new CodeMarker("com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager#finishProjectAddition"); //$NON-NLS-1$

    public static final CodeMarker FINISH_CONNECTION =
        new CodeMarker("com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager#finishConnection"); //$NON-NLS-1$

    private static final QualifiedName REPOSITORY_STATUS_KEY =
        new QualifiedName(TFSEclipseClientPlugin.PLUGIN_ID, "TFSRepositoryStatus"); //$NON-NLS-1$
    private static final String REPOSITORY_STATUS_ONLINE_VALUE = "online"; //$NON-NLS-1$
    private static final String REPOSITORY_STATUS_OFFLINE_VALUE = "offline"; //$NON-NLS-1$

    private final RepositoryManager repositoryManager;
    private final ServerManager serverManager;

    private final ProjectConnectionManager connectionManager;

    /**
     * License checking is done synchronously, and only once to avoid multiple
     * popups at plugin start.
     */
    private final Object licenseLock = new Object();
    private boolean licenseTested = false;
    private boolean licenseValid = false;

    /**
     * A lock to arbitrate access to the major status (projectToStatusMap,
     * repositoryToProjectMap) fields.
     */
    private final Object projectDataLock = new Object();

    private boolean started = false;

    /* Maps IProject -> ProjectRepositoryData */
    private final Map<IProject, ProjectRepositoryData> projectDataMap = new HashMap<IProject, ProjectRepositoryData>();

    /* Maps TFSRepository -> Set of IProjects */
    private final Map<TFSRepository, Set<IProject>> repositoryToProjectMap =
        new HashMap<TFSRepository, Set<IProject>>();

    /* Maps TFSServer -> Set of IProjects */
    private final Map<TFSServer, Set<IProject>> serverToProjectMap = new HashMap<TFSServer, Set<IProject>>();

    /*
     * A set of closed projects that we managed. Note that this should be used
     * only for determining whether this manager has ever connected these closed
     * projects. This cannot be accurately used to identify closed projects that
     * are managed by us but have never been opened.
     */
    private final Set<IProject> projectClosedSet = new HashSet<IProject>();

    /**
     * An IResourceChangeListener that listens for {@link IProject} close
     * events.
     */
    private final ProjectCloseListener projectCloseListener;

    /**
     * A RepositoryManagerListener that listens for new {@link TFSRepository}s
     * being brought online. We will automatically reconnect {@link IProject}s
     * for this repository.
     */
    private final RepositoryManagerListener reconnectListener = new RepositoryManagerReconnectListener();

    /**
     * Listeners
     */
    private final SingleListenerFacade listeners = new SingleListenerFacade(ProjectRepositoryManagerListener.class);

    public ProjectRepositoryManager(final ServerManager serverManager, final RepositoryManager repositoryManager) {
        Check.notNull(serverManager, "serverManager"); //$NON-NLS-1$
        Check.notNull(repositoryManager, "repositoryManager"); //$NON-NLS-1$

        this.repositoryManager = repositoryManager;
        this.serverManager = serverManager;

        connectionManager = new ProjectConnectionManager(serverManager, repositoryManager);
        projectCloseListener = new ProjectCloseListener(this);

        repositoryManager.addListener(reconnectListener);
    }

    /**
     * Starts the project repository manager, and connects any previously
     * connected TFS-managed projects. This will block while connections are
     * ongoing.
     *
     * This must be called after the workbench is completely started (ie, the UI
     * has been fully formed.)
     */
    public void start() {
        final Map<IProject, ProjectRepositoryData> projectsToConnect = new HashMap<IProject, ProjectRepositoryData>();

        log.info("Starting TFS repository manager"); //$NON-NLS-1$

        /*
         * Take a very brief lock on projectDataLock to determine project and
         * reconnection status. We do not hold this while connecting or doing
         * time-intensive operations so that calls to #isStarted will not block.
         */
        synchronized (projectDataLock) {
            Check.isTrue(started == false, "started == false"); //$NON-NLS-1$

            /* Hook up our project closed listener */
            ResourcesPlugin.getWorkspace().addResourceChangeListener(
                projectCloseListener,
                IResourceChangeEvent.PRE_CLOSE);

            /* Determine which projects to connect. */
            final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

            for (int i = 0; i < projects.length; i++) {
                final Boolean shouldConnect = shouldConnect(projects[i]);

                if (shouldConnect == null) {
                    log.debug(
                        MessageFormat.format("Project {0} is not managed by TFS, ignoring", projects[i].getName())); //$NON-NLS-1$
                    continue;
                }

                final ProjectRepositoryData projectData = new ProjectRepositoryData();
                projectDataMap.put(projects[i], projectData);

                if (shouldConnect == Boolean.TRUE) {
                    projectData.setStatus(ProjectRepositoryStatus.CONNECTING);
                    projectsToConnect.put(projects[i], projectData);
                    log.info(MessageFormat.format("Connecting project {0} to TFS server", projects[i].getName())); //$NON-NLS-1$
                } else {
                    projectData.setStatus(ProjectRepositoryStatus.OFFLINE);
                    log.info(MessageFormat.format(
                        "Project {0} is offline from TFS server, will not be connected", //$NON-NLS-1$
                        projects[i].getName()));
                }
            }

            started = true;
        }

        /*
         * Get out of the big lock to connect. This allows other threads to
         * query status, etc w/o blocking waiting for connections to finish.
         */

        /* Connect up our projects (if any) */
        for (final Iterator<Entry<IProject, ProjectRepositoryData>> i =
            projectsToConnect.entrySet().iterator(); i.hasNext();) {
            final Entry<IProject, ProjectRepositoryData> projectEntry = i.next();

            final IProject project = projectEntry.getKey();
            final ProjectRepositoryData projectData = projectEntry.getValue();

            connectInternal(project, true, projectData);
        }
    }

    private void waitForManagerStartup() {
        while (true) {
            synchronized (projectDataLock) {
                if (started == true) {
                    return;
                }
            }

            try {
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                throw new RuntimeException(
                    Messages.getString("ProjectRepositoryManager.InterruptedWhileWaitingForProjectManagerStartup"), //$NON-NLS-1$
                    e);
            }
        }
    }

    /**
     * Adds this project to the project manager. This should only be done when
     * new projects are added to the repository system - ie, through the import
     * or share wizard.
     *
     * @param project
     *        The project to add
     * @param repository
     *        The repository
     */
    public void addProject(final IProject project, final TFSRepository repository) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        log.debug(
            MessageFormat.format("Adding project {0} for repository {1}", project.getName(), repository.getName())); //$NON-NLS-1$

        waitForManagerStartup();

        synchronized (projectDataLock) {
            ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData != null) {
                /* Sanity check */
                log.error(MessageFormat.format(
                    "Project Manager already contains project {0} (when adding)", //$NON-NLS-1$
                    project.getName()));
                CodeMarkerDispatch.dispatch(FINISH_PROJECT_ADDITION);
                return;
            }

            projectData = new ProjectRepositoryData();
            projectData.setRepository(repository);

            projectDataMap.put(project, projectData);

            /* Update repository map */
            connectRepository(project, repository);
        }

        log.info(MessageFormat.format("Project {0} is now managed by TFS", project.getName())); //$NON-NLS-1$

        TFSEclipseClientPlugin.getDefault().getResourceDataManager().addProject(repository, project);

        CodeMarkerDispatch.dispatch(FINISH_PROJECT_ADDITION);
    }

    /**
     * Adds this project to the project manager, but keeps the project offline.
     * This should only be done when new projects are added to the repository
     * system - ie, through automatic project detection and connection.
     *
     * @param project
     *        The project to add
     */
    public void addOfflineProject(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        log.debug(MessageFormat.format("Adding project {0} for offline repository", project.getName())); //$NON-NLS-1$

        waitForManagerStartup();

        /* Set this project as being offline. */
        try {
            project.setPersistentProperty(REPOSITORY_STATUS_KEY, REPOSITORY_STATUS_OFFLINE_VALUE);
        } catch (final CoreException e) {
            log.error(MessageFormat.format("Could not set offline status for project {0}", project.getName()), e); //$NON-NLS-1$
        }

        synchronized (projectDataLock) {
            ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData != null) {
                /* Sanity check */
                log.error(MessageFormat.format(
                    "Project Manager already contains project {0} (when adding)", //$NON-NLS-1$
                    project.getName()));
                CodeMarkerDispatch.dispatch(FINISH_PROJECT_ADDITION);
                return;
            }

            projectData = new ProjectRepositoryData();
            projectData.setStatus(ProjectRepositoryStatus.OFFLINE);

            projectDataMap.put(project, projectData);
        }

        log.info(MessageFormat.format("Project {0} is now managed by TFS (but offline)", project.getName())); //$NON-NLS-1$

        CodeMarkerDispatch.dispatch(FINISH_PROJECT_ADDITION);
    }

    public void addAndConnectProject(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        log.debug(MessageFormat.format("Adding project {0} for offline repository", project.getName())); //$NON-NLS-1$

        waitForManagerStartup();

        synchronized (projectDataLock) {
            ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData != null) {
                /* Sanity check */
                log.error(MessageFormat.format(
                    "Project Manager already contains project {0} (when adding)", //$NON-NLS-1$
                    project.getName()));
                CodeMarkerDispatch.dispatch(FINISH_PROJECT_ADDITION);
                return;
            }

            projectData = new ProjectRepositoryData();
            projectData.setStatus(ProjectRepositoryStatus.CONNECTING);

            projectDataMap.put(project, projectData);
        }

        connect(project);
        CodeMarkerDispatch.dispatch(FINISH_PROJECT_ADDITION);
    }

    /**
     * Removes a project from the project manager. This should only be done when
     * we are unmapping the project from TFS.
     *
     * @param project
     *        the {@link IProject} to remove
     */
    public void removeProject(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        log.debug(MessageFormat.format("Removing project {0}", project.getName())); //$NON-NLS-1$

        waitForManagerStartup();

        TFSRepository repository;
        synchronized (projectDataLock) {
            final ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData == null) {
                /* Sanity check */
                log.error(MessageFormat.format(
                    "Project Manager does not contain project {0} (when removing)", //$NON-NLS-1$
                    project.getName()));
                return;
            }

            synchronized (projectData) {
                repository = projectData.getRepository();
            }

            projectDataMap.remove(project);

            /* Update repository map */
            if (repository != null) {
                disconnectRepository(project, repository, true, false);
            }
        }

        /* Unhook from resource data manager */
        if (repository != null) {
            TFSEclipseClientPlugin.getDefault().getResourceDataManager().removeProject(repository, project);
        }

        /* Notify listeners */
        ((ProjectRepositoryManagerListener) listeners.getListener()).onProjectRemoved(project);
    }

    /**
     * Clears any connection errors.
     *
     * We hold on to connection errors and abort any new connections - because
     * Eclipse is multithreaded and the entry point to our
     * {@link TFSRepositoryProvider} is varied, we need to abort connections
     * after the first failure. They should be cleared when the user requests
     * it, for example, when returning online.
     */
    public void clearErrors() {
        synchronized (licenseLock) {
            licenseTested = false;
        }

        connectionManager.clearErrors();
    }

    /**
     * Connects the given IProject to its Team Foundation Server if and only if
     * it is not marked offline. This method will not reconnect offline
     * projects. This manager need not have any previous knowledge of this
     * project. This is most useful when a project was closed and is now opened.
     *
     * @param project
     *        The IProject to attempt to connect
     * @return The connected repository, or null if it could not be connected.
     */
    public TFSRepository connectIfNecessary(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        waitForManagerStartup();

        ProjectRepositoryData projectData;

        synchronized (projectDataLock) {
            projectData = projectDataMap.get(project);

            if (projectData == null) {
                /*
                 * We don't know about this project, it was likely just opened.
                 */
                Boolean shouldConnect = shouldConnect(project);

                /*
                 * null response means that we should ignore this project
                 * entirely
                 */
                if (shouldConnect == null) {
                    return null;
                }

                projectData = new ProjectRepositoryData();
                projectDataMap.put(project, projectData);
                projectClosedSet.remove(project);

                /*
                 * override the online/offline state iff we have TFS-managed
                 * projects in another state. this prevents us from reconnecting
                 * a previously closed project when other projects are offline.
                 */
                if (shouldConnect.equals(Boolean.FALSE) && isAnyProjectOfStatus(ProjectRepositoryStatus.ONLINE)) {
                    shouldConnect = Boolean.TRUE;
                } else if (shouldConnect.equals(Boolean.TRUE)
                    && isAnyProjectOfStatus(ProjectRepositoryStatus.OFFLINE)) {
                    shouldConnect = Boolean.FALSE;
                }

                if (shouldConnect == Boolean.FALSE) {
                    projectData.setStatus(ProjectRepositoryStatus.OFFLINE);
                    return null;
                }

                projectData.setStatus(ProjectRepositoryStatus.CONNECTING);
            } else {
                /*
                 * This project already exists in our map. If it's already
                 * connected (or offline), return the repository for the
                 * project.
                 */
                synchronized (projectData) {
                    if (projectData.getStatus() == ProjectRepositoryStatus.INITIALIZING) {
                        /* Sanity check. */
                        return null;
                    } else if (projectData.getStatus() != ProjectRepositoryStatus.CONNECTING) {
                        return projectData.getRepository();
                    }

                    /*
                     * The project is being connected, fall through to join to
                     * that job.
                     */
                }
            }
        }

        return connectInternal(project, true, projectData);
    }

    /**
     * Determines whether we should connect this project. Returns true if we
     * should connect, false if the project should be offline, or null if we
     * should ignore this project altogether.
     *
     * @param project
     *        The project to connect
     * @return {@link Boolean#TRUE} if the project should be connected,
     *         {@link Boolean#FALSE} if the project should stay offline, and
     *         <code>null</code> if the project should not be managed by us at
     *         all.
     */
    private Boolean shouldConnect(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        if (!project.isOpen()) {
            log.debug(MessageFormat.format("Ignoring closed project {0}", project.getName())); //$NON-NLS-1$
            return null;
        }

        String providerName;

        try {
            providerName = project.getPersistentProperty(TeamUtils.PROVIDER_PROP_KEY);
        } catch (final CoreException e) {
            log.warn(
                MessageFormat.format(
                    "Could not query repository manager for project {0} (when determining connection viability)", //$NON-NLS-1$
                    project.getName()),
                e);
            return null;
        }

        if (providerName == null || !providerName.equals(TFSRepositoryProvider.PROVIDER_ID)) {
            return null;
        }

        /* Load the persisted project status: "online" or "offline". */
        String repositoryStatus = null;

        try {
            repositoryStatus = project.getPersistentProperty(REPOSITORY_STATUS_KEY);
        } catch (final CoreException e) {
            log.warn(
                MessageFormat.format(
                    "Repository status could not be determined for project {0} (when determining connection viability)", //$NON-NLS-1$
                    project.getName()),
                e);
        }

        /*
         * If we were offline, then we should not reconnect this project.
         */
        if (REPOSITORY_STATUS_OFFLINE_VALUE.equals(repositoryStatus)) {
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    /**
     * Connects the given IProject to its Team Foundation Server. If the project
     * was offline, it will be brought online.
     *
     * @param project
     *        The IProject to connect
     * @return the connected repository, or null if it could not be connected.
     */
    public TFSRepository connect(final IProject project) {
        return connect(project, true);
    }

    /**
     * Connects the given IProject to its Team Foundation Server. If the project
     * was offline, it will be brought online.
     *
     * @param project
     *        The IProject to connect
     * @param disconnectOtherServers
     *        true to allow the user to (optionally) disconnect from the other
     *        servers, false if this should not be offered and we should fail if
     *        another connection exists
     * @return the connected repository, or null if it could not be connected.
     */
    public TFSRepository connect(final IProject project, final boolean disconnectOtherServers) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        log.info(MessageFormat.format("Connecting project {0} to TFS server", project.getName())); //$NON-NLS-1$

        waitForManagerStartup();

        ProjectRepositoryData projectData;

        synchronized (projectDataLock) {
            projectData = projectDataMap.get(project);

            if (projectData == null) {
                /* Sanity check. */
                log.error(MessageFormat.format(
                    "Project Manager does not contain project {0} (when returning online)", //$NON-NLS-1$
                    project.getName()));
                return null;
            }

            synchronized (projectData) {
                if (projectData.getStatus() == ProjectRepositoryStatus.INITIALIZING) {
                    /* Sanity check. */
                    log.error(
                        MessageFormat.format(
                            "Project manager does not contain a status for project {0} (when returning online)", //$NON-NLS-1$
                            project.getName()));
                    return null;
                } else if (projectData.getStatus() == ProjectRepositoryStatus.OFFLINE) {
                    projectData.setStatus(ProjectRepositoryStatus.CONNECTING);
                }
            }
        }

        return connectInternal(project, disconnectOtherServers, projectData);
    }

    /**
     * Connects the given IProject with the given ProjectRepositoryData.
     * Designed to be run outside of the projectDataLock to avoid taking a lock
     * on the entire connection mechanism.
     *
     * @param project
     * @param projectData
     * @return
     */
    private TFSRepository connectInternal(
        final IProject project,
        final boolean disconnectOtherServers,
        final ProjectRepositoryData projectData) {
        ProjectRepositoryConnectionJob connectionJob;

        /*
         * Test the license. If it is not valid, put this project offline.
         */
        if (testLicense() == false) {
            synchronized (projectData) {
                if (projectData.getStatus() == ProjectRepositoryStatus.CONNECTING) {
                    projectData.setStatus(ProjectRepositoryStatus.OFFLINE);
                    return null;
                }

                /*
                 * Another thread could have connected us before we acquired the
                 * lock. Should not happen.
                 */
                return projectData.getRepository();
            }
        }

        synchronized (projectData) {
            /*
             * Another thread could have connected before we acquired this lock.
             */
            if (projectData.getStatus() != ProjectRepositoryStatus.CONNECTING) {
                return projectData.getRepository();
            }

            /* See if another thread is already connecting this project. */
            connectionJob = projectData.getConnectionJob();

            if (connectionJob == null) {
                /* Start the connection job */

                connectionJob = new ProjectRepositoryConnectionJob(project, disconnectOtherServers, projectData);

                connectionJob.schedule();
                projectData.setConnectionJob(connectionJob);
            }
        }

        try {
            new ExtensionPointAsyncObjectWaiter().joinJob(connectionJob);
        } catch (final InterruptedException e) {
            log.error(MessageFormat.format("Interrupted while waiting to connect to project {0}", project.getName())); //$NON-NLS-1$
            return null;
        }

        synchronized (projectData) {
            return projectData.getRepository();
        }
    }

    /**
     * Checks the license (product id installation) status.
     *
     * @return true if the licensing for this product is valid (and connection
     *         should continue), false otherwise
     */
    private boolean testLicense() {
        /*
         * Check the license status.
         */
        synchronized (licenseLock) {
            if (licenseTested == false) {
                licenseValid = TFSEclipseClientLicenseManager.isLicensed();
                licenseTested = true;
            }

            if (!licenseValid) {
                /*
                 * If we're not valid, hook up a license manager listener to
                 * invalidate our test status.
                 */
                LicenseManager.getInstance().addListener(new ProjectManagerLicenseListener());

                return false;
            }
        }

        return true;
    }

    private void connectionFinished(
        final IProject project,
        final ProjectRepositoryData projectData,
        final TFSRepository repository) {
        synchronized (projectDataLock) {
            synchronized (projectData) {
                if (repository == null) {
                    projectData.setStatus(ProjectRepositoryStatus.OFFLINE);
                } else {
                    projectData.setRepository(repository);
                }

                projectData.setConnectionJob(null);

                if (repository != null) {
                    connectRepository(project, repository);
                }
            }
        }

        final String repositoryStatusValue =
            (repository != null) ? REPOSITORY_STATUS_ONLINE_VALUE : REPOSITORY_STATUS_OFFLINE_VALUE;

        try {
            project.setPersistentProperty(REPOSITORY_STATUS_KEY, repositoryStatusValue);
        } catch (final CoreException e) {
            log.error(
                MessageFormat.format(
                    "Could not set repository online/offline persistent property for project {0}", //$NON-NLS-1$
                    project.getName()),
                e);
        }

        if (repository != null) {
            log.info(MessageFormat.format("Connection to TFS server successful for project {0}", project.getName())); //$NON-NLS-1$

            TFSEclipseClientPlugin.getDefault().getResourceDataManager().addProject(repository, project);

            ((ProjectRepositoryManagerListener) listeners.getListener()).onProjectConnected(project, repository);
        } else {
            log.info(MessageFormat.format("Connection to TFS server failed for project {0}", project.getName())); //$NON-NLS-1$

            ((ProjectRepositoryManagerListener) listeners.getListener()).onProjectDisconnected(project);
        }

        CodeMarkerDispatch.dispatch(FINISH_CONNECTION);
    }

    private void connectionCancelled(
        final IProject project,
        final ProjectRepositoryData projectData,
        final TFSRepository repository) {
        synchronized (projectDataLock) {
            projectDataMap.remove(project);
            projectClosedSet.add(project);

            /*
             * When we cancel a connection, it will happen as the result of a
             * project being closed while it was being connected. If the
             * connection succeeded, then we need to discard the resultant
             * TFSRepository. This may mean removing it from the
             * RepositoryManager.
             */

            if (repository != null) {
                disconnectRepository(project, repository, false, false);
            }
        }

        log.info(MessageFormat.format("Connection to TFS server cancelled for project {0}", project.getName())); //$NON-NLS-1$
    }

    public boolean isStarted() {
        synchronized (projectDataLock) {
            return started;
        }
    }

    public boolean isConnecting() {
        waitForManagerStartup();

        synchronized (projectDataLock) {
            /*
             * Otherwise, we may be hooking up projects to connections.
             */
            for (final Iterator<ProjectRepositoryData> i = projectDataMap.values().iterator(); i.hasNext();) {
                final ProjectRepositoryData projectData = i.next();

                synchronized (projectData) {
                    if (projectData.getStatus() == ProjectRepositoryStatus.INITIALIZING) {
                        /* Sanity check */
                        log.error("Project data contains no status (during connection check)"); //$NON-NLS-1$
                        return true;
                    }

                    if (projectData.getStatus() == ProjectRepositoryStatus.CONNECTING) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public IProject[] getProjects() {
        waitForManagerStartup();

        synchronized (projectDataLock) {
            final Set<IProject> projectSet = projectDataMap.keySet();

            return projectSet.toArray(new IProject[projectSet.size()]);
        }
    }

    public boolean isAnyProjectOfStatus(final ProjectRepositoryStatus status) {
        return (getProjectsOfStatus(status).length > 0);
    }

    /**
     * Returns a list of all {@link IProject}s in any of the given statuses.
     *
     * @param statuses
     *        The statuses to query for (not <code>null</code>)
     * @return An array of {@link IProject}s currently in any of the given
     *         statuses (never <code>null</code>)
     */
    public IProject[] getProjectsOfStatus(final ProjectRepositoryStatus status) {
        Check.notNull(status, "status"); //$NON-NLS-1$

        waitForManagerStartup();

        final List<IProject> projectList = new ArrayList<IProject>();

        synchronized (projectDataLock) {
            for (final Iterator<Entry<IProject, ProjectRepositoryData>> i =
                projectDataMap.entrySet().iterator(); i.hasNext();) {
                final Entry<IProject, ProjectRepositoryData> projectEntry = i.next();

                final ProjectRepositoryData projectData = projectEntry.getValue();

                synchronized (projectData) {
                    if (status.contains(projectData.getStatus())) {
                        projectList.add(projectEntry.getKey());
                    }
                }
            }
        }

        return projectList.toArray(new IProject[projectList.size()]);
    }

    /**
     * Returns any projects that were previously managed by this manager but are
     * now closed. Note that this will not identify projects which are managed
     * by us but have never been opened.
     *
     * @return An array of {@link IProject}s that have been closed (never
     *         <code>null</code>)
     */
    public IProject[] getClosedProjects() {
        waitForManagerStartup();

        synchronized (projectDataLock) {
            return projectClosedSet.toArray(new IProject[projectClosedSet.size()]);
        }
    }

    /**
     * Should currently always be equivalent to getOnlineProjects().
     *
     * @param repository
     * @return
     */
    public IProject[] getProjectsForRepository(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        waitForManagerStartup();

        synchronized (projectDataLock) {
            final Set<IProject> projectsForRepository = repositoryToProjectMap.get(repository);

            if (projectsForRepository == null) {
                return new IProject[0];
            }

            return projectsForRepository.toArray(new IProject[projectsForRepository.size()]);
        }
    }

    public void disconnect(final TFSRepository repository) {
        disconnect(repository, true);
    }

    public void disconnect(final TFSRepository repository, final boolean disconnectServer) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        log.debug(MessageFormat.format(
            "Disconnecting all projects for repository {0}", //$NON-NLS-1$
            repository.getWorkspace().getDisplayName()));

        waitForManagerStartup();

        synchronized (projectDataLock) {
            final Set<IProject> projectsForRepository = repositoryToProjectMap.get(repository);

            if (projectsForRepository == null) {
                return;
            }

            final IProject[] projects = projectsForRepository.toArray(new IProject[projectsForRepository.size()]);

            for (int i = 0; i < projects.length; i++) {
                disconnect(projects[i], disconnectServer);
            }
        }
    }

    /**
     * Disconnect multiple projects from a repository, bringing them offline. If
     * all projects for a given server are disconnected, the server will be
     * disconnected as well.
     *
     * @param projects
     *        The {@link IProject}s to bring offline (not <code>null</code>)
     */
    public void disconnect(final IProject[] projects) {
        disconnect(projects, true);
    }

    /**
     * Disconnect multiple projects from a repository, bringing them offline.
     *
     * @param projects
     *        The {@link IProject}s to bring offline (not <code>null</code>)
     * @param disconnectServer
     *        true to disconnect the server from the server manager, if all
     *        repositories for that server are disconnected. false otherwise.
     */
    public void disconnect(final IProject[] projects, final boolean disconnectServer) {
        Check.notNull(projects, "projects"); //$NON-NLS-1$

        waitForManagerStartup();

        ((ProjectRepositoryManagerListener) listeners.getListener()).onOperationStarted();

        for (int i = 0; i < projects.length; i++) {
            disconnect(projects[i], disconnectServer);
        }

        ((ProjectRepositoryManagerListener) listeners.getListener()).onOperationFinished();
    }

    /**
     * Called to disconnect a project from a repository, bringing it offline.
     *
     * @param project
     *        The {@link IProject} to bring offline (not <code>null</code>).
     * @param disconnectServer
     *        true to disconnect the server from the server manager, if all
     *        repositories for that server are disconnected. false otherwise.
     */
    public void disconnect(final IProject project, final boolean disconnectServer) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        log.info(MessageFormat.format("Moving project {0} offline from TFS server", project.getName())); //$NON-NLS-1$

        waitForManagerStartup();

        /* Set this project as being offline. */
        try {
            project.setPersistentProperty(REPOSITORY_STATUS_KEY, REPOSITORY_STATUS_OFFLINE_VALUE);
        } catch (final CoreException e) {
            log.error(MessageFormat.format("Could not set offline status for project {0}", project.getName()), e); //$NON-NLS-1$
        }

        synchronized (projectDataLock) {
            final ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData == null) {
                log.error(MessageFormat.format(
                    "Project Manager does not contain project {0} (while disconnecting)", //$NON-NLS-1$
                    project.getName()));
                return;
            }

            TFSRepository projectRepository;

            synchronized (projectData) {
                if (projectData.getStatus() != ProjectRepositoryStatus.ONLINE) {
                    /* Sanity check */
                    log.error(MessageFormat.format(
                        "Project Manager not online for project {0} (while disconnecting)", //$NON-NLS-1$
                        project.getName()));
                    return;
                }

                projectRepository = projectData.getRepository();

                if (projectRepository == null) {
                    /* Sanity check */
                    log.error(
                        MessageFormat.format(
                            "Project Manager does not contain repository for project {0} (while disconnecting)", //$NON-NLS-1$
                            project.getName()));
                    return;
                }

                /* Turn this project offline */
                projectData.setStatus(ProjectRepositoryStatus.OFFLINE);

                /* Update repository map */
                disconnectRepository(project, projectRepository, true, disconnectServer);
            }

            /* Notify resource data manager */
            TFSEclipseClientPlugin.getDefault().getResourceDataManager().removeProject(projectRepository, project);

            ((ProjectRepositoryManagerListener) listeners.getListener()).onProjectDisconnected(project);
        }
    }

    void close(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        waitForManagerStartup();

        TFSRepository projectRepository = null;

        synchronized (projectDataLock) {
            projectClosedSet.add(project);

            final ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData == null) {
                log.error(MessageFormat.format(
                    "Project Manager does not contain project {0} (while closing project)", //$NON-NLS-1$
                    project.getName()));
                return;
            }

            synchronized (projectData) {
                projectDataMap.remove(project);

                if (projectData.getStatus() == ProjectRepositoryStatus.CONNECTING) {
                    if (projectData.getConnectionJob() == null) {
                        /*
                         * The project has not yet started connecting. Simply
                         * change the status to none.
                         */
                        projectData.setStatus(ProjectRepositoryStatus.INITIALIZING);
                    } else {
                        /*
                         * The project is connecting. Notify the connection job
                         * to cancel.
                         */
                        projectData.getConnectionJob().cancelConnection();
                    }
                } else if (projectData.getStatus() == ProjectRepositoryStatus.ONLINE) {
                    projectRepository = projectData.getRepository();

                    if (projectRepository == null) {
                        /* Sanity check */
                        log.error(
                            MessageFormat.format(
                                "Project Manager does not contain repository for project {0} (while closing)", //$NON-NLS-1$
                                project.getName()));
                        return;
                    }

                    /* Update repository map */
                    disconnectRepository(project, projectRepository, true, false);
                }
            }
        }

        if (projectRepository != null) {
            /* Notify resource data manager */
            TFSEclipseClientPlugin.getDefault().getResourceDataManager().removeProject(projectRepository, project);

            ((ProjectRepositoryManagerListener) listeners.getListener()).onProjectDisconnected(project);
        }
    }

    /**
     * Updates the repositoryToProjectMap when a project is added for a
     * repository.
     *
     * @param project
     *        The {@link IProject} that is being added (not null)
     * @param repository
     *        The corresponding {@link TFSRepository} (not null)
     */
    private void connectRepository(final IProject project, final TFSRepository repository) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        Set<IProject> projectsForRepository = repositoryToProjectMap.get(repository);

        if (projectsForRepository == null) {
            projectsForRepository = new HashSet<IProject>();
            repositoryToProjectMap.put(repository, projectsForRepository);
        }

        if (projectsForRepository.contains(project)) {
            /* Sanity check */
            log.error(MessageFormat.format(
                "Repository {0} already contains reference for project {1}", //$NON-NLS-1$
                repository.getName(),
                project.getName()));
        } else {
            projectsForRepository.add(project);
        }

        /* Now update the server map */
        final TFSServer server = serverManager.getOrCreateServer(repository.getVersionControlClient().getConnection());

        Set<IProject> projectsForServer = serverToProjectMap.get(server);

        if (projectsForServer == null) {
            projectsForServer = new HashSet<IProject>();
            serverToProjectMap.put(server, projectsForServer);
        }

        if (projectsForServer.contains(project)) {
            /* Sanity check */
            log.error(MessageFormat.format(
                "Server {0} already contains reference for project {1}", //$NON-NLS-1$
                server.getName(),
                project.getName()));
        } else {
            projectsForServer.add(project);
        }
    }

    /**
     * Updates the repositoryToProjectMap when a project is removed from a
     * repository.
     *
     * @param project
     *        The {@link IProject} that is being removed (not null)
     * @param repository
     *        The corresponding {@link TFSRepository} (not null)
     * @param whether
     *        we believe this project is connected (ie, we're bringing an
     *        offline project online)
     * @param disconnectServer
     *        true to disconnect servers from the server manager when all
     *        repositories are removed, false otherwise
     */
    private void disconnectRepository(
        final IProject project,
        final TFSRepository repository,
        final boolean isConnected,
        final boolean disconnectServer) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        final Set<IProject> projectsForRepository = repositoryToProjectMap.get(repository);

        if (projectsForRepository == null) {
            log.error(MessageFormat.format("Project Manager out of sync for repository {0}", repository.getName())); //$NON-NLS-1$
            return;
        }

        if (!projectsForRepository.contains(project)) {
            if (isConnected) {
                log.error(MessageFormat.format(
                    "Project Manager out of sync for repository {0} for project {1}", //$NON-NLS-1$
                    repository.getName(),
                    project.getName()));
            }
        } else {
            projectsForRepository.remove(project);
        }

        /*
         * If this was the last project for this repository, remove it from the
         * list.
         */
        if (projectsForRepository.size() == 0) {
            repositoryToProjectMap.remove(repository);

            /*
             * We must call RepositoryManager#removeRepository(TFSRepository)
             * here. All repositories come from the RepositoryManager, so it
             * will update itself when repositories are created. This, however,
             * is the only exit point for repositories.
             */
            if (disconnectServer) {
                repository.close();
                repositoryManager.removeRepository(repository);
            }
        }

        /* Now update the server map */
        final TFSServer server = serverManager.getServer(repository.getVersionControlClient().getConnection());

        if (server == null) {
            /* Sanity check */
            log.error(MessageFormat.format(
                "Project Manager cannot locate server for project {0} (repository = {1}", //$NON-NLS-1$
                project.getName(),
                repository.getName()));
            return;
        }

        final Set<IProject> projectsForServer = serverToProjectMap.get(server);

        if (projectsForServer == null) {
            log.error(MessageFormat.format("Project Manager out of sync for server {0}", server.getName())); //$NON-NLS-1$
            return;
        }

        if (!projectsForServer.contains(project)) {
            log.error(MessageFormat.format(
                "Project Manager out of sync for server {0} for project {1}", //$NON-NLS-1$
                server.getName(),
                project.getName()));
        } else {
            projectsForServer.remove(project);

            /*
             * If this was the last project for this repository, remove it from
             * the list.
             */
            if (projectsForServer.size() == 0) {
                serverToProjectMap.remove(server);

                /*
                 * Disconnect the server from the server manager, this will
                 * complete us going completely "offline" for the non-VC
                 * components (WIT, build, etc.) Do not do this if we are merely
                 * switching workspaces within the same server (see
                 * setDisconnectServerWithRepository), however.
                 */
                if (disconnectServer) {
                    server.close();
                    serverManager.removeServer(server);
                }
            }
        }
    }

    public ProjectRepositoryStatus getProjectStatus(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        waitForManagerStartup();

        synchronized (projectDataLock) {
            final ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData == null) {
                log.error(MessageFormat.format(
                    "Project Manager does not contain project {0} (during status query)", //$NON-NLS-1$
                    project.getName()));
                return ProjectRepositoryStatus.INITIALIZING;
            }

            synchronized (projectData) {
                return projectData.getStatus();
            }
        }
    }

    public Map<TFSRepository, Set<IProject>> getRepositoryToProjectMap() {
        waitForManagerStartup();

        final Map<TFSRepository, Set<IProject>> cloneMap = new HashMap<TFSRepository, Set<IProject>>();

        synchronized (projectDataLock) {
            for (final Entry<TFSRepository, Set<IProject>> entry : repositoryToProjectMap.entrySet()) {
                cloneMap.put(entry.getKey(), new HashSet<IProject>(entry.getValue()));
            }
        }

        return cloneMap;
    }

    public TFSRepository[] getRepositories() {
        waitForManagerStartup();

        final List<TFSRepository> repositories = new ArrayList<TFSRepository>();

        synchronized (projectDataLock) {
            for (final Entry<IProject, ProjectRepositoryData> entry : projectDataMap.entrySet()) {
                final IProject project = entry.getKey();
                final ProjectRepositoryData projectData = entry.getValue();

                if (projectData == null) {
                    log.error(MessageFormat.format(
                        "Project Manager does not contain project {0} (during query)", //$NON-NLS-1$
                        project.getName()));
                } else {
                    synchronized (projectData) {
                        if (projectData.getRepository() != null) {
                            repositories.add(projectData.getRepository());
                        }
                    }
                }
            }
        }

        return repositories.toArray(new TFSRepository[repositories.size()]);
    }

    public TFSRepository getRepository(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        waitForManagerStartup();

        synchronized (projectDataLock) {
            final ProjectRepositoryData projectData = projectDataMap.get(project);

            if (projectData == null) {
                log.error(MessageFormat.format(
                    "Project Manager does not contain project {0} (during query)", //$NON-NLS-1$
                    project.getName()));
                return null;
            }

            synchronized (projectData) {
                return projectData.getRepository();
            }
        }
    }

    public void addListener(final ProjectRepositoryManagerListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final ProjectRepositoryManagerListener listener) {
        listeners.removeListener(listener);
    }

    private static class ProjectRepositoryData {
        private TFSRepository repository;
        private ProjectRepositoryStatus status;
        private ProjectRepositoryConnectionJob connectionJob;

        public ProjectRepositoryData() {
            repository = null;
            status = ProjectRepositoryStatus.INITIALIZING;
            connectionJob = null;
        }

        public void setRepository(final TFSRepository repository) {
            Check.notNull(repository, "repository"); //$NON-NLS-1$

            this.repository = repository;
            status = ProjectRepositoryStatus.ONLINE;
            connectionJob = null;
        }

        public TFSRepository getRepository() {
            return repository;
        }

        public void setStatus(final ProjectRepositoryStatus status) {
            Check.notNull(status, "status"); //$NON-NLS-1$
            Check.isTrue(status != ProjectRepositoryStatus.ONLINE, "status != ONLINE"); //$NON-NLS-1$

            this.status = status;
            repository = null;
        }

        public ProjectRepositoryStatus getStatus() {
            return status;
        }

        public ProjectRepositoryConnectionJob getConnectionJob() {
            return connectionJob;
        }

        public void setConnectionJob(final ProjectRepositoryConnectionJob connectionJob) {
            if (connectionJob != null) {
                Check.isTrue(status == ProjectRepositoryStatus.CONNECTING, "status == CONNECTING"); //$NON-NLS-1$
                Check.isTrue(this.connectionJob == null, "this.connectionJob == null"); //$NON-NLS-1$
            }

            this.connectionJob = connectionJob;
        }
    }

    private class ProjectManagerLicenseListener implements LicenseListener {
        @Override
        public void eulaAcceptanceChanged(final boolean eulaAccepted) {
            synchronized (licenseLock) {
                licenseTested = false;
            }

            LicenseManager.getInstance().removeListener(this);
        }
    }

    private final class ProjectRepositoryConnectionJob extends Job {
        private final Object lock = new Object();
        private final IProject project;
        private final boolean disconnectOtherServers;
        private final ProjectRepositoryData projectData;

        private boolean cancelled = false;

        public ProjectRepositoryConnectionJob(
            final IProject project,
            final boolean disconnectOtherServers,
            final ProjectRepositoryData projectData) {
            super(
                MessageFormat.format(Messages.getString("ProjectRepositoryManager.JobNameFormat"), project.getName())); //$NON-NLS-1$

            this.project = project;
            this.disconnectOtherServers = disconnectOtherServers;
            this.projectData = projectData;
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            /* Notify the server manager so that it can display UI for this */
            final IBackgroundTask connectTask = new BackgroundTask(getName());

            TFSEclipseClientPlugin.getDefault().getServerManager().backgroundConnectionTaskStarted(connectTask);

            try {
                final ProjectConnectionManagerResult result =
                    connectionManager.connect(project, disconnectOtherServers);

                synchronized (lock) {
                    if (cancelled) {
                        connectionCancelled(project, projectData, result.getRepository());
                    } else {
                        connectionFinished(project, projectData, result.getRepository());
                    }
                }
            } catch (final Throwable e) {
                log.error(MessageFormat.format("Failed to build connection for project {0}", project.getName()), e); //$NON-NLS-1$
                connectionFinished(project, projectData, null);
            } finally {
                TFSEclipseClientPlugin.getDefault().getServerManager().backgroundConnectionTaskFinished(connectTask);
            }

            return Status.OK_STATUS;
        }

        public void cancelConnection() {
            synchronized (lock) {
                cancelled = true;
            }

            super.cancel();
        }
    }

    private class RepositoryManagerReconnectListener extends RepositoryManagerAdapter {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepositoryAdded(final RepositoryManagerEvent event) {
            final ProjectManagerDataProvider dataProvider = ProjectManagerDataProviderFactory.getDataProvider();

            if (dataProvider.shouldReconnectProjects() == false) {
                return;
            }

            final IProject[] offlineProjects;
            final List<IProject> onlineProjects = new ArrayList<IProject>();

            synchronized (projectDataLock) {
                /*
                 * Do not reconnect if we're doing any other work in a different
                 * thread (ie, there exists a project in not offline status.)
                 */
                if (getProjectsOfStatus(
                    ProjectRepositoryStatus.INITIALIZING.combine(
                        ProjectRepositoryStatus.ONLINE.combine(ProjectRepositoryStatus.CONNECTING))).length > 0) {
                    return;
                }

                offlineProjects = getProjectsOfStatus(ProjectRepositoryStatus.OFFLINE);
            }

            /*
             * We are on an unknown thread - get off this thread to avoid UI
             * deadlocks.
             */
            final Job reconnectJob = new Job(Messages.getString("ProjectRepositoryManager.ReconnectJobName")) //$NON-NLS-1$
            {
                @Override
                public IStatus run(final IProgressMonitor progressMonitor) {
                    if (offlineProjects.length == 0) {
                        return Status.OK_STATUS;
                    }

                    TFSRepository repository = null;

                    /*
                     * Try to connect all offline projects to the current
                     * repository (use the false flag to ensure that we do not
                     * try to connect to a different repository.)
                     */
                    for (int i = 0; i < offlineProjects.length; i++) {
                        final TFSRepository projectRepository = connect(offlineProjects[i], false);

                        if (projectRepository != null) {
                            /* Sanity check */
                            if (repository != null && repository != projectRepository) {
                                log.warn(
                                    "Multiple repositories were returned when trying to reconnect projects to existing repository."); //$NON-NLS-1$

                                return new Status(
                                    IStatus.ERROR,
                                    TFSEclipseClientPlugin.PLUGIN_ID,
                                    0,
                                    //@formatter:off
                                    Messages.getString("ProjectRepositoryManager.ReconnectToNewRepositoryFailureMessage"), //$NON-NLS-1$
                                    //@formatter:on
                                    null);
                            }

                            repository = projectRepository;
                            onlineProjects.add(offlineProjects[i]);
                        }
                    }

                    /*
                     * Notify the data provider that we've brought projects
                     * online.
                     */
                    if (onlineProjects.size() > 0) {
                        dataProvider.notifyProjectsReconnected(
                            repository,
                            onlineProjects.toArray(new IProject[onlineProjects.size()]));
                    }

                    return Status.OK_STATUS;
                }
            };

            reconnectJob.schedule();
        }
    }
}
