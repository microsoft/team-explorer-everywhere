// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.microsoft.tfs.client.common.commands.QueryLocalWorkspacesCommand;
import com.microsoft.tfs.client.common.framework.command.ExtensionPointAsyncObjectWaiter;
import com.microsoft.tfs.client.common.framework.command.JobCommandAdapter;
import com.microsoft.tfs.client.common.repository.RepositoryConflictException;
import com.microsoft.tfs.client.common.repository.RepositoryManager;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceKey;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.util.Check;

/**
 * ProjectConnectionManager is responsible for connecting an IProject to a
 * TFSRepository. It will reuse existing TFS {@link Workspace} or
 * {@link TFSRepository}s whenever possible, and multiple {@link IProject}s will
 * be connected to their given {@link Workspace} in parallel. Uses plugin
 * extension points to gain any necessary but unavailable information (for
 * example, profile completion, login failures, etc.)
 *
 * Clients should never access this class directly. Clients should only use
 * ProjectRepositoryManager.
 *
 * @threadsafety thread safe
 */
final class ProjectConnectionManager {
    private static final Log log = LogFactory.getLog(ProjectConnectionManager.class);

    private final ServerManager serverManager;
    private final RepositoryManager repositoryManager;

    private final Object cachedWorkspaceLock = new Object();
    private boolean emptyCachedWorkspaceFailures = false;
    private final Set<WorkspaceInfo> cachedWorkspaceFailures = new HashSet<WorkspaceInfo>();

    private final Object connectionLock = new Object();
    private final Map<WorkspaceKey, ProjectManagerRepositoryJob> workspaceJobMap =
        new HashMap<WorkspaceKey, ProjectManagerRepositoryJob>();
    private final Map<URI, ProjectManagerConnectionJob> serverConnectionJobMap =
        new HashMap<URI, ProjectManagerConnectionJob>();

    private final ProjectManagerRepositoryJobChangeListener repositoryJobChangeListener =
        new ProjectManagerRepositoryJobChangeListener();
    private final ProjectManagerConnectionJobChangeListener connectionJobChangeListener =
        new ProjectManagerConnectionJobChangeListener();

    // NLS Strings that are too long and wrap lines, thus breaking MessagesTests
    private final String projectWillNotBeConnectedExistingConnectionExistsFormat =
        Messages.getString("ProjectConnectionManager.ProjectWillNotBeConnectedExistingConnectionExistsFormat"); //$NON-NLS-1$
    private final String projectWillNotBeConnectedPreviousConnectionCancelledFormat =
        Messages.getString("ProjectConnectionManager.ProjectWillNotBeConnectedPreviousConnectionCancelledFormat"); //$NON-NLS-1$
    private final String projectDoesNotHaveWorkingFolderMappingFormat =
        Messages.getString("ProjectConnectionManager.ProjectDoesNotHaveWorkingFolderMappingFormat"); //$NON-NLS-1$

    ProjectConnectionManager(final ServerManager serverManager, final RepositoryManager repositoryManager) {
        this.serverManager = serverManager;
        this.repositoryManager = repositoryManager;
    }

    /**
     * Clears errors to allow for reconnection.
     *
     * Since we can be connected asynchronously and frequently, we need to block
     * future connections until they're manually cleared (usually when returning
     * online.)
     */
    void clearErrors() {
        synchronized (cachedWorkspaceLock) {
            emptyCachedWorkspaceFailures = false;
            cachedWorkspaceFailures.clear();
        }
    }

    /**
     * Attempts to build a TFSRepository for the given IProject. Will block
     * until connection is completed successfully, or has failed.
     *
     * @param project
     *        The IProject to connect
     * @param disconnectOtherServers
     *        true to allow disconnecting other TFS repositories that are
     *        running, false to fail if a different TFS repository is configured
     * @return A ProjectManagerResult containing a TFSRepository or an error
     *         status
     */
    ProjectConnectionManagerResult connect(final IProject project, final boolean disconnectOtherServers) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        final ProjectManagerDataProvider dataProvider = ProjectManagerDataProviderFactory.getDataProvider();

        if (project.isOpen() == false) {
            return new ProjectConnectionManagerResult(
                new Status(
                    Status.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    0,
                    MessageFormat.format(
                        Messages.getString("ProjectConnectionManager.ProjectNotOpenFormat"), //$NON-NLS-1$
                        project.getName()),
                    null));
        }

        if (project.getLocation() == null) {
            final String message = MessageFormat.format(
                Messages.getString("ProjectConnectionManager.ProjectDoesNotHaveLocalFileSystemMappingFormat"), //$NON-NLS-1$
                project.getName());

            return new ProjectConnectionManagerResult(
                new Status(Status.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, message, null));
        }

        WorkspaceInfo cachedWorkspace;
        TFSRepository repository;

        /*
         * Lock the cachedWorkspaceLock when we fetch a cached workspace for
         * this Project. We do this because if we cannot immediately locate a
         * cached workspace, we may prompt the user to connect to their TFS
         * Server in order to query workspaces (and thus refresh the workspace
         * cache.) We want to let the workspace cache refresh before we query it
         * again in a different thread, since its data will likely be complete.
         */
        synchronized (cachedWorkspaceLock) {
            cachedWorkspace = getCachedWorkspace(project);

            if (cachedWorkspace == null) {
                if (emptyCachedWorkspaceFailures) {
                    log.error(MessageFormat.format("Project {0} was not found in workspace cache", project.getName())); //$NON-NLS-1$

                    return new ProjectConnectionManagerResult(
                        new Status(
                            Status.ERROR,
                            TFSEclipseClientPlugin.PLUGIN_ID,
                            0,
                            MessageFormat.format(projectDoesNotHaveWorkingFolderMappingFormat, project.getName()),
                            null));
                }

                log.warn(MessageFormat.format("Project {0} was not found in workspace cache", project.getName())); //$NON-NLS-1$

                /*
                 * If we cannot find a cached workspace, prompt the user to
                 * connect. This will allow us to query workspaces on the server
                 * and rebuild the workspace cache. Hold a lock on the
                 * cachedWorkspaceLock to prevent other threads from prompting
                 * for a damaged workspace cache.
                 */
                repository = connectRepository(project);

                if (repository == null) {
                    /*
                     * Abort further connections to empty cached workspaces.
                     * Prevents us from prompting forever.
                     */
                    emptyCachedWorkspaceFailures = true;

                    return new ProjectConnectionManagerResult(
                        new Status(
                            Status.ERROR,
                            TFSEclipseClientPlugin.PLUGIN_ID,
                            0,
                            MessageFormat.format(projectDoesNotHaveWorkingFolderMappingFormat, project.getName()),
                            null));
                }

                return new ProjectConnectionManagerResult(repository);
            }
        }

        ProjectManagerRepositoryJob repositoryJob;
        final WorkspaceKey workspaceKey = new WorkspaceKey(cachedWorkspace);

        synchronized (connectionLock) {
            /* We've already got a TFS Repository for this workspace */
            if ((repository =
                TFSEclipseClientPlugin.getDefault().getRepositoryManager().getRepository(cachedWorkspace)) != null) {
                log.debug(MessageFormat.format(
                    "Project {0} can be connected to repository {1}", //$NON-NLS-1$
                    project.getName(),
                    repository));

                return new ProjectConnectionManagerResult(repository);
            }

            /*
             * See if we're already running a connection job for this workspace
             */
            if ((repositoryJob = workspaceJobMap.get(workspaceKey)) == null) {
                /* See if there already exists a connection for this profile. */
                final URI connectionURI = cachedWorkspace.getServerURI();
                TFSServer server;

                if ((server = serverManager.getServer(connectionURI)) != null) {
                    log.debug(
                        MessageFormat.format("Project {0} will be connected to server {1}", project.getName(), server)); //$NON-NLS-1$

                    /*
                     * Do not obliterate the current repository if it's
                     * configured.
                     */
                    if (repositoryManager.getDefaultRepository() != null && disconnectOtherServers == false) {

                        return new ProjectConnectionManagerResult(
                            new Status(
                                Status.ERROR,
                                TFSEclipseClientPlugin.PLUGIN_ID,
                                0,
                                MessageFormat.format(
                                    projectWillNotBeConnectedExistingConnectionExistsFormat,
                                    project.getName()),
                                null));
                    }

                    /*
                     * Hook up our workspace to this TFS Connection instead of
                     * building a new one.
                     */
                    repositoryJob =
                        new ProjectManagerRepositoryJob(this, dataProvider, cachedWorkspace, server.getConnection());
                    repositoryJob.addJobChangeListener(repositoryJobChangeListener);
                    repositoryJob.schedule();
                    workspaceJobMap.put(workspaceKey, repositoryJob);
                } else {
                    ProjectManagerConnectionJob connectionJob;

                    /*
                     * See if another thread is trying to connect this profile.
                     */
                    if ((connectionJob = serverConnectionJobMap.get(connectionURI)) == null) {
                        /*
                         * Make sure the user hasn't cancelled connections for
                         * this cached workspace.
                         */
                        synchronized (cachedWorkspaceLock) {
                            if (cachedWorkspaceFailures.contains(cachedWorkspace)) {
                                return new ProjectConnectionManagerResult(
                                    new Status(
                                        Status.ERROR,
                                        TFSEclipseClientPlugin.PLUGIN_ID,
                                        0,
                                        MessageFormat.format(
                                            projectWillNotBeConnectedPreviousConnectionCancelledFormat,
                                            project.getName()),
                                        null));
                            }
                        }

                        /*
                         * There is no connection going on to this server:
                         * before we build a server connection, ensure that
                         * there are no other server connections in the server
                         * manager.
                         */
                        if (serverManager.getDefaultServer() != null) {
                            log.debug(
                                MessageFormat.format(
                                    "Another connection already exists while connecting project {0}", //$NON-NLS-1$
                                    project.getName()));

                            /*
                             * Do not prompt to disconnect from the other server
                             */
                            if (disconnectOtherServers == false) {
                                return new ProjectConnectionManagerResult(
                                    new Status(
                                        Status.ERROR,
                                        TFSEclipseClientPlugin.PLUGIN_ID,
                                        0,
                                        MessageFormat.format(
                                            projectWillNotBeConnectedExistingConnectionExistsFormat,
                                            project.getName()),
                                        null));
                            }

                            /* Allow users to close the existing connection. */
                            final boolean retry = dataProvider.getConnectionConflictHandler().resolveServerConflict();

                            /*
                             * If the user cancelled the retry, or we still have
                             * a default server, fail.
                             */
                            if (retry == false || serverManager.getDefaultServer() != null) {
                                log.debug(
                                    MessageFormat.format(
                                        "Cancelled connection for project {0}, another connection already exists", //$NON-NLS-1$
                                        project.getName()));

                                return new ProjectConnectionManagerResult(
                                    new Status(
                                        Status.ERROR,
                                        TFSEclipseClientPlugin.PLUGIN_ID,
                                        0,
                                        MessageFormat.format(
                                            projectWillNotBeConnectedExistingConnectionExistsFormat,
                                            project.getName()),
                                        null));
                            }
                        }

                        connectionJob = new ProjectManagerCachedWorkspaceConnectionJob(
                            this,
                            dataProvider,
                            connectionURI,
                            cachedWorkspace);
                        connectionJob.addJobChangeListener(connectionJobChangeListener);
                        connectionJob.setSystem(true);
                        connectionJob.schedule();
                        serverConnectionJobMap.put(connectionURI, connectionJob);
                    }

                    log.debug(MessageFormat.format(
                        "Project {0} will be connected through connection job {1}", //$NON-NLS-1$
                        project.getName(),
                        connectionJob));

                    /*
                     * Now build a job that will realize the Workspace when this
                     * profile is connected.
                     */

                    repositoryJob = new ProjectManagerRepositoryJob(this, dataProvider, cachedWorkspace, connectionJob);
                    repositoryJob.addJobChangeListener(repositoryJobChangeListener);
                    repositoryJob.schedule();

                    log.debug(
                        MessageFormat.format(
                            "Project {0} will be connected through repository realization job {1}", //$NON-NLS-1$
                            project.getName(),
                            repositoryJob));

                    workspaceJobMap.put(workspaceKey, repositoryJob);
                }
            }
        }

        /* Wait for the workspace connector job to finish. */
        IStatus result;
        try {
            log.debug("Waiting for repository job to finish"); //$NON-NLS-1$

            new ExtensionPointAsyncObjectWaiter().joinJob(repositoryJob);
            result = repositoryJob.getResult();
        } catch (final InterruptedException e) {
            result =
                new Status(
                    Status.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    0,
                    MessageFormat.format(
                        Messages.getString("ProjectConnectionManager.CouldNotConnectToConnectionFormat"), //$NON-NLS-1$
                        getConnectionName(cachedWorkspace)),
                    e);
        }

        if (result == null) {
            result = new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("ProjectConnectionManager.CouldNotExecuteProjectConnectionJobFormat"), //$NON-NLS-1$
                null);
        }

        if (!result.isOK()) {
            return new ProjectConnectionManagerResult(null, result);
        }

        repository = repositoryJob.getRepository();

        return new ProjectConnectionManagerResult(repositoryJob.getRepository(), result);
    }

    public static String getConnectionName(final WorkspaceInfo cachedWorkspace) {
        if (cachedWorkspace != null && cachedWorkspace.getServerURI() != null) {
            return cachedWorkspace.getServerURI().toString();
        }

        return Messages.getString("ProjectConnectionManager.TeamFoundationServerProductName"); //$NON-NLS-1$
    }

    void connectionFailed(final WorkspaceInfo cachedWorkspace) {
        synchronized (cachedWorkspaceLock) {
            cachedWorkspaceFailures.add(cachedWorkspace);
        }
    }

    /**
     * Locates a cached workspace for this IProject.
     *
     * @param project
     *        The IProject to locate
     * @return A CachedWorkspace or null if no cached workspaces contain a
     *         working folder mapping for this IProject.
     */
    private WorkspaceInfo getCachedWorkspace(final IProject project) {
        final String projectPath = LocalPath.canonicalize(project.getLocation().toOSString());

        return Workstation.getCurrent(DefaultPersistenceStoreProvider.INSTANCE).getLocalWorkspaceInfo(projectPath);
    }

    /**
     * Connects to a {@link TFSRepository} for an {@link IProject} that does not
     * have a cached workspace mapping. This is done by prompting for the TFS
     * connection information, connecting, querying workspaces and locating the
     * proper workspace mapping. If mappings are not located on the server, then
     * null is returned.
     *
     * @param project
     *        The {@link IProject} we are connecting
     * @return The {@link TFSRepository} for this {@link IProject} or null if
     *         none was located or the user canceled
     */
    private TFSRepository connectRepository(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(project.getLocation(), "project.getLocation()"); //$NON-NLS-1$
        Check.notNull(project.getLocation().toOSString(), "project.getLocation().toOSString()"); //$NON-NLS-1$

        final ProjectManagerDataProvider dataProvider = ProjectManagerDataProviderFactory.getDataProvider();

        /* Prompt user for server information */
        final TFSTeamProjectCollection connection = dataProvider.promptForConnection(project);

        if (connection == null) {
            return null;
        }

        /*
         * Ensure our local workspace cache contains information about all
         * workspaces on this computer
         */
        final QueryLocalWorkspacesCommand updateCacheCommand = new QueryLocalWorkspacesCommand(connection);
        final JobCommandAdapter updateCacheJob = new JobCommandAdapter(updateCacheCommand);
        updateCacheJob.schedule();

        try {
            new ExtensionPointAsyncObjectWaiter().joinJob(updateCacheJob);
        } catch (final InterruptedException e) {
            return null;
        }

        if (!updateCacheJob.getResult().isOK()) {
            return null;
        }

        /* Now hit the workspace cache. */
        final Workspace workspace =
            connection.getVersionControlClient().getWorkspace(project.getLocation().toOSString());

        if (workspace == null) {
            return null;
        }

        TFSRepository repository = null;

        /* Configure this workspace with the repository manager. */
        try {
            repository = repositoryManager.getOrCreateRepository(workspace);
        } catch (final RepositoryConflictException e) {
            /* Allow the data provider (UI) to fix this issue. */
            if (dataProvider.getConnectionConflictHandler().resolveRepositoryConflict()) {
                try {
                    /* Retry the workspace configuration. */
                    repository = repositoryManager.getOrCreateRepository(workspace);
                } catch (final RepositoryConflictException f) {
                    /* Notify the user of the failure. */
                    dataProvider.getConnectionConflictHandler().notifyRepositoryConflict();
                }
            }
        }

        if (repository != null) {
            serverManager.getOrCreateServer(connection);
        }

        return repository;
    }

    private class ProjectManagerRepositoryJobChangeListener extends JobChangeAdapter {
        @Override
        public void done(final IJobChangeEvent event) {
            final IStatus result = event.getResult();
            ProjectManagerRepositoryJob testJob;
            final ProjectManagerRepositoryJob job = (ProjectManagerRepositoryJob) event.getJob();
            final WorkspaceKey workspaceKey = new WorkspaceKey(job.getCachedWorkspace());

            synchronized (connectionLock) {
                testJob = workspaceJobMap.remove(workspaceKey);

                if (testJob == null) {
                    log.warn("Could not locate running repository connection job in connection manager."); //$NON-NLS-1$
                } else if (!testJob.equals(job)) {
                    log.warn("Multiple repository connection jobs running for same connection key."); //$NON-NLS-1$
                } else if (result.isOK()) {
                    log.debug("Project Manager connected project with repository"); //$NON-NLS-1$
                }
            }
        }
    }

    private class ProjectManagerConnectionJobChangeListener extends JobChangeAdapter {
        @Override
        public void done(final IJobChangeEvent event) {
            final IStatus result = event.getResult();
            final ProjectManagerConnectionJob job = (ProjectManagerConnectionJob) event.getJob();
            final URI connectionURI = job.getServerURI();

            synchronized (connectionLock) {
                /*
                 * Sanity check: make sure our connection job exists. If not,
                 * connections may have been invalidated underneath us.
                 */
                final ProjectManagerConnectionJob testJob = serverConnectionJobMap.remove(connectionURI);

                if (testJob == null) {
                    log.warn("Could not locate running server connection job in connection manager."); //$NON-NLS-1$
                } else if (!testJob.equals(job)) {
                    log.warn("Multiple server connection jobs running for same connection key."); //$NON-NLS-1$
                } else if (result.isOK()) {
                    serverManager.getOrCreateServer(job.getConnection());
                }
            }
        }
    }
}
