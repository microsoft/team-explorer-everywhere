// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import java.text.MessageFormat;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.cache.annotation.AnnotationCache;
import com.microsoft.tfs.client.common.repository.cache.conflict.ConflictCache;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCache;
import com.microsoft.tfs.client.common.repository.localworkspace.TFSRepositoryPathWatcherManager;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.FolderContentChangedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.FolderContentChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.LocalWorkspaceScanListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ScannerModifiedFilesEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ScannerModifiedFilesListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class TFSRepository {
    private static final Log log = LogFactory.getLog(TFSRepository.class);

    private final Workspace workspace;
    private final PendingChangeCache pendingChangeCache;
    private final ConflictCache conflictCache;
    private final AnnotationCache annotationCache;

    private final TFSRepositoryPathWatcherManager pathWatcherManager;

    /**
     * Adapts core events to {@link TFSRepositoryUpdatedListener} events to send
     * to {@link #repositoryUpdatedListeners}.
     */
    private final TFSRepositoryUpdateListenerAdapter listenerAdapter = new TFSRepositoryUpdateListenerAdapter();

    private final SingleListenerFacade repositoryUpdatedListeners =
        new SingleListenerFacade(TFSRepositoryUpdatedListener.class);

    /**
     * Creates a new {@link TFSRepository} for the given {@link Workspace}. If
     * this is a server workspace or is associated with an established server
     * connection then the server data will be refreshed, otherwise only local
     * data will be refreshed.
     *
     * @param workspace
     *        the TFS {@link Workspace} (not <code>null</code>)
     */
    public TFSRepository(final Workspace workspace) {
        this(
            workspace,
            (WorkspaceLocation.SERVER.equals(workspace.getLocation())
                || ConnectionHelper.isConnected(workspace.getClient().getConnection())));
    }

    /**
     * Creates a new {@link TFSRepository} for the given {@link Workspace}.
     *
     * @param workspace
     *        the TFS {@link Workspace} (not <code>null</code>)
     * @param refreshServer
     *        <code>true</code> to refresh the server data, <code>false</code>
     *        to only refresh local data
     */
    public TFSRepository(final Workspace workspace, final boolean refreshServer) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;

        final String messageFormat = "Created a new TFSRepository, workspace={0}, sessionId={1}"; //$NON-NLS-1$
        final String message = MessageFormat.format(
            messageFormat,
            workspace.getName(),
            workspace.getClient().getConnection().getSessionID());
        log.debug(message);

        pendingChangeCache = new PendingChangeCache(workspace);
        conflictCache = new ConflictCache(workspace);
        annotationCache = new AnnotationCache(workspace);

        pathWatcherManager = new TFSRepositoryPathWatcherManager(this);
        workspace.getClient().setPathWatcherFactory(pathWatcherManager);

        // Clear out any of the WorkspaceWatcher's path watchers before using
        // it. There might be watchers from previous uses of the workspace.
        workspace.getWorkspaceWatcher().stopWatching();
        workspace.getWorkspaceWatcher().setAsynchronous(true);

        workspace.getClient().getEventEngine().addWorkspaceUpdatedListener(listenerAdapter);
        workspace.getClient().getEventEngine().addFolderContentChangedListener(listenerAdapter);
        workspace.getClient().getEventEngine().addGetCompletedListener(listenerAdapter);
        workspace.getClient().getEventEngine().addScannerModifiedFilesListener(listenerAdapter);
        workspace.getClient().getEventEngine().addLocalWorkspaceScanListener(listenerAdapter);

        if (refreshServer) {
            refresh(true);
        } else {
            refreshLocal(true);
        }
    }

    public String getName() {
        return workspace.getDisplayName();
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public VersionControlClient getVersionControlClient() {
        return workspace.getClient();
    }

    public TFSConnection getConnection() {
        return workspace.getClient().getConnection();
    }

    public PendingChangeCache getPendingChangeCache() {
        return pendingChangeCache;
    }

    public ConflictCache getConflictManager() {
        return conflictCache;
    }

    public AnnotationCache getAnnotationCache() {
        return annotationCache;
    }

    public TFSRepositoryPathWatcherManager getPathWatcherManager() {
        return pathWatcherManager;
    }

    /**
     * Refreshes the server data for this TFS Repository. This will contact the
     * server.
     *
     * @param async
     *        <code>true</code> to refresh asynchronously, <code>false</code>
     *        otherwise
     */
    public void refresh(final boolean async) {
        if (!async) {
            refresh(new NullProgressMonitor());
        } else {
            final String messageFormat = Messages.getString("TFSRepository.ProgressTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, workspace.getServerName());
            final Job refreshJob = new Job(message) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    refresh(monitor);
                    return Status.OK_STATUS;
                }
            };
            refreshJob.schedule();
        }
    }

    /**
     * Refreshes the local data for this TFS Repository. This will NOT contact
     * the server.
     *
     * @param async
     *        <code>true</code> to refresh asynchronously, <code>false</code>
     *        otherwise
     */
    public void refreshLocal(final boolean async) {
        if (!async) {
            refreshLocal(new NullProgressMonitor());
        } else {
            final String messageFormat = Messages.getString("TFSRepository.ProgressTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, workspace.getServerName());
            final Job refreshJob = new Job(message) {
                @Override
                protected IStatus run(final IProgressMonitor monitor) {
                    refreshLocal(monitor);
                    return Status.OK_STATUS;
                }
            };
            refreshJob.schedule();
        }
    }

    private void refresh(final IProgressMonitor progressMonitor) {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        String messageFormat = Messages.getString("TFSRepository.ProgressTextFormat"); //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, workspace.getServerName());
        progressMonitor.beginTask(message, 3);

        messageFormat = Messages.getString("TFSRepository.ProgressStepRefreshChangesFormat"); //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, workspace.getServerName());
        progressMonitor.subTask(message);
        pendingChangeCache.refresh();
        progressMonitor.worked(1);

        messageFormat = Messages.getString("TFSRepository.ProgressStepRefreshConflictFormat"); //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, workspace.getServerName());
        progressMonitor.subTask(message);
        conflictCache.refresh();
        progressMonitor.worked(1);

        messageFormat = Messages.getString("TFSRepository.ProgressStepRefreshAnnotationsFormat"); //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, workspace.getServerName());
        progressMonitor.subTask(message);
        annotationCache.refresh();
        progressMonitor.worked(1);

        progressMonitor.done();
    }

    private void refreshLocal(final IProgressMonitor progressMonitor) {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        String messageFormat = Messages.getString("TFSRepository.ProgressTextFormat"); //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, workspace.getServerName());
        progressMonitor.beginTask(message, 1);

        if (WorkspaceLocation.LOCAL.equals(workspace.getLocation())) {
            messageFormat = Messages.getString("TFSRepository.ProgressStepRefreshChangesFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, workspace.getServerName());
            progressMonitor.subTask(message);
            pendingChangeCache.refresh();
            progressMonitor.worked(1);
        }

        progressMonitor.done();
    }

    /**
     * Closes this TFSRepository connection. Should be called whenever
     * disconnecting from a repository.
     */
    public void close() {
        /*
         * Important: if we don't stop the watchers, when we switch back to a
         * workspace after switching away from it, the old watchers will still
         * be configured for the old TFSRepository and polling them won't give
         * us changes from the new TFSRepository.
         */
        workspace.getWorkspaceWatcher().stopWatching();

        workspace.getClient().getEventEngine().removeWorkspaceUpdatedListener(listenerAdapter);
        workspace.getClient().getEventEngine().removeFolderContentChangedListener(listenerAdapter);
        workspace.getClient().getEventEngine().removeGetCompletedListener(listenerAdapter);
        workspace.getClient().getEventEngine().removeScannerModifiedFilesListener(listenerAdapter);
        workspace.getClient().getEventEngine().removeLocalWorkspaceScanListener(listenerAdapter);
    }

    public void addRepositoryUpdatedListener(final TFSRepositoryUpdatedListener repositoryUpdatedListener) {
        repositoryUpdatedListeners.addListener(repositoryUpdatedListener);
    }

    public void removeRepositoryUpdatedListener(final TFSRepositoryUpdatedListener repositoryUpdatedListener) {
        repositoryUpdatedListeners.removeListener(repositoryUpdatedListener);
    }

    private final class TFSRepositoryUpdateListenerAdapter
        implements WorkspaceUpdatedListener, FolderContentChangedListener, GetCompletedListener,
        ScannerModifiedFilesListener, LocalWorkspaceScanListener {
        @Override
        public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
            if (workspace.equals(e.getWorkspace())) {
                workspace.refresh();

                ((TFSRepositoryUpdatedListener) repositoryUpdatedListeners.getListener()).onRepositoryUpdated();
            }
        }

        @Override
        public void onFolderContentChanged(final FolderContentChangedEvent e) {
            // The event's client may be null
            if (workspace.getClient().equals(e.getClient())) {
                // Workspace refresh not needed
                ((TFSRepositoryUpdatedListener) repositoryUpdatedListeners.getListener()).onFolderContentChanged(
                    e.getChangesetID());
            }
        }

        @Override
        public void onGetCompleted(final WorkspaceEvent e) {
            if (workspace.equals(e.getWorkspace())) {
                // Workspace refresh not needed
                ((TFSRepositoryUpdatedListener) repositoryUpdatedListeners.getListener()).onGetCompletedEvent(
                    e.getWorkspaceSource());
            }
        }

        @Override
        public void onLocalWorkspaceScan(final WorkspaceEvent e) {
            if (workspace.equals(e.getWorkspace())) {
                // Workspace refresh not needed
                ((TFSRepositoryUpdatedListener) repositoryUpdatedListeners.getListener()).onLocalWorkspaceScan(
                    e.getWorkspaceSource());
            }
        }

        @Override
        public void onScannerModifiedFiles(final ScannerModifiedFilesEvent event) {
            final Set<String> paths = event.getPaths();

            if (paths == null || paths.size() == 0) {
                return;
            }

            for (final String path : paths) {
                if (path == null || path.length() == 0) {
                    continue;
                }

                try {
                    final IResource resource = Resources.getResourceForLocation(path);
                    if (resource != null) {
                        // These are always files, so DEPTH_ONE
                        // should be good
                        resource.refreshLocal(IResource.DEPTH_ONE, null);
                    }
                } catch (final CoreException e) {
                    log.warn("Error refreshing after scan", e); //$NON-NLS-1$
                }
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof TFSRepository == false) {
            return false;
        }

        return this.workspace.equals(((TFSRepository) obj).workspace);
    }

    @Override
    public int hashCode() {
        return workspace.hashCode();
    }
}
