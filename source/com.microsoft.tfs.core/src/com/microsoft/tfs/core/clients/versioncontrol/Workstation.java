// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.events.ChangesetReconciledEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.FolderContentChangedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceCacheFileReloadedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkstationNonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalItemExclusionCache;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ServerSettings;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.KeyValuePair;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.InternalWorkspaceConflictInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkstationType;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalCache;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalCacheLoader;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.InternalServerInfo;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.persistence.FilesystemPersistenceStore;
import com.microsoft.tfs.core.util.SpecialFolders;
import com.microsoft.tfs.core.util.notifications.Notification;
import com.microsoft.tfs.core.util.notifications.NotificationListener;
import com.microsoft.tfs.core.util.notifications.NotificationManager;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.CLRHashUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.GUID.GUIDStringFormat;
import com.microsoft.tfs.util.Platform;

public class Workstation {
    private static final Log log = LogFactory.getLog(Workstation.class);

    /**
     * On non-Windows platforms, this child is created inside the configuration
     * persistence store to store local workspace metadata. On Windows a
     * location shared by Visual Studio is used.
     *
     * See {@link #getOfflineMetadataFileRoot(PersistenceStoreProvider)}.
     */
    private static final String NON_WINDOWS_OFFLINE_STORAGE_CHILD_NAME = "TFS-Offline"; //$NON-NLS-1$

    /**
     * Map of {@link PersistenceStoreProvider}s to {@link Workstation} instances
     * which use them for cache data. Synchronized on itself.
     */
    private static Map<PersistenceStoreProvider, Workstation> currentWorkstation =
        new HashMap<PersistenceStoreProvider, Workstation>();

    private final WorkstationType type;

    // Cache objects

    private final boolean cacheEnabled;
    private final File cacheDirectory;
    private final File workspaceCacheFile;

    private final Object cacheMutex = new Object();
    private InternalCache workspaceCache;
    private boolean cacheFileChanged;
    private final Map<String, Long> workspacesLoadedTable = new HashMap<String, Long>();

    // Configuration objects

    private final boolean configurationEnabled;
    private final File configurationDirectory;
    private final File localItemExclusionCacheFile;

    private final Object configurationMutex = new Object();
    private LocalItemExclusionCache localItemExclusionCache;

    // Notifications

    private NotificationManager notificationManager;
    private final Object notificationManagerLock = new Object();
    private final NotificationListener notificationListener = new NotificationListener() {
        @Override
        public void notificationReceived(final Notification notification, final long param1, final long param2) {
            handleNotification(notification, param1, param2);
        }
    };

    /**
     * Instead of handling lists of listeners for many types of events, we
     * dispatch notifications to the {@link VersionControlEventEngine} in these
     * interested {@link VersionControlClient}s.
     */
    private final List<VersionControlClient> workstationEventListeners = new ArrayList<VersionControlClient>();

    private Workstation(final WorkstationType type, final PersistenceStoreProvider persistenceProvider) {
        Check.notNull(type, "type"); //$NON-NLS-1$

        this.type = type;

        if (type == WorkstationType.CURRENT) {
            Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

            synchronized (cacheMutex) {
                // Detect if cache directory is available
                cacheDirectory = getCacheDirectory(persistenceProvider);

                cacheEnabled = cacheDirectory != null
                    ? ensureLocalPathIsUsable(cacheDirectory, InternalCacheLoader.FILE_NAME) : false;

                workspaceCacheFile = cacheEnabled ? new File(cacheDirectory, InternalCacheLoader.FILE_NAME) : null;

                /*
                 * Load the cache. After this, it will be reloaded by the Cache
                 * property when the FSW sets the flag indicating the cache file
                 * changed. Use the Cache property everywhere else. We are not
                 * performing any merging on mappings, so we can safely ignore
                 * the warnings
                 */
                final AtomicReference<InternalWorkspaceConflictInfo[]> conflictingWorkspaces =
                    new AtomicReference<InternalWorkspaceConflictInfo[]>();
                workspaceCache = InternalCacheLoader.loadConfig(
                    null,
                    cacheEnabled,
                    conflictingWorkspaces,
                    cacheMutex,
                    workspaceCacheFile);
                Check.isTrue(conflictingWorkspaces.get().length == 0, "conflictingWorkspaces.get().length == 0"); //$NON-NLS-1$
            }

            synchronized (configurationMutex) {
                // Detect if configuration directory is available
                configurationDirectory = getConfigurationDirectory(persistenceProvider);

                configurationEnabled = configurationDirectory != null
                    ? ensureLocalPathIsUsable(configurationDirectory, LocalItemExclusionCache.FILE_NAME) : false;

                localItemExclusionCacheFile =
                    configurationEnabled ? new File(configurationDirectory, LocalItemExclusionCache.FILE_NAME) : null;
            }
        } else {
            cacheDirectory = null;
            cacheEnabled = false;
            workspaceCacheFile = null;

            configurationDirectory = null;
            configurationEnabled = false;
            localItemExclusionCacheFile = null;
        }
    }

    /**
     * Gets the "current" (for this computer) {@link Workstation} that uses the
     * specified {@link PersistenceStoreProvider} to read and write its cache
     * and config files.
     *
     * @param persistenceProvider
     *        the {@link PersistenceStoreProvider} the {@link Workstation} will
     *        use (must not be <code>null</code>)
     * @return the {@link Workstation} (never <code>null</code>)
     */
    public static Workstation getCurrent(final PersistenceStoreProvider persistenceProvider) {
        Check.notNull(persistenceProvider, "persistenceProvider"); //$NON-NLS-1$

        synchronized (Workstation.currentWorkstation) {
            Workstation w = Workstation.currentWorkstation.get(persistenceProvider);

            if (w == null) {
                w = new Workstation(WorkstationType.CURRENT, persistenceProvider);
                Workstation.currentWorkstation.put(persistenceProvider, w);
            }

            return w;
        }
    }

    /**
     * Adds a {@link VersionControlClient} whose
     * {@link VersionControlEventEngine} will receive events from this
     * {@link Workstation} class. This special pattern is used because of the
     * automatic caching done for {@link Workstation} instances.
     *
     * @param client
     *        the client to add (must not be <code>null</code>)
     */
    public void addWorkstationEventListener(final VersionControlClient client) {
        workstationEventListeners.add(client);
    }

    /**
     * Removes a listener previously added with
     * {@link #addWorkstationEventListener(VersionControlClient)}.
     *
     * @param client
     *        the client to remove (must not be <code>null</code>)
     */
    public void removeWorkstationEventListener(final VersionControlClient client) {
        workstationEventListeners.remove(client);
    }

    /*
     * Notifications
     */

    /**
     * Sets the {@link NotificationManager} this workstation uses to send and
     * receive intra- and inter-process notifications about things like changes
     * to mappings, new pending changes, merge completion.
     * <p>
     * Does not call {@link NotificationManager#close()} on the old manager
     * being replaced.
     *
     * @param newManager
     *        the notification manager to use or <code>null</code> to stop
     *        sending and receiving notifications
     * @return the existing {@link NotificationManager} or <code>null</code> if
     *         there wasn't one
     */
    public NotificationManager setNotificationManager(final NotificationManager newManager) {
        synchronized (notificationManagerLock) {
            if (this.notificationManager == newManager) {
                return notificationManager;
            }

            final NotificationManager oldManager = this.notificationManager;
            if (oldManager != null) {
                oldManager.removeListener(notificationListener);
            }

            this.notificationManager = newManager;

            if (newManager != null) {
                this.notificationManager.addListener(notificationListener);
            }

            return oldManager;
        }
    }

    /**
     * @return the {@link NotificationManager} this {@link Workstation} is using
     *         or <code>null</code> if there is no active
     *         {@link NotificationManager}.
     */
    public NotificationManager getNotificationManager() {
        synchronized (notificationManagerLock) {
            return this.notificationManager;
        }
    }

    /**
     * Handles a notification received by the {@link #notificationManager}.
     *
     * @param notification
     *        the notification (must not be <code>null</code>)
     * @param param1
     *        the first parameter
     * @param param2
     *        the second paramter
     */
    private void handleNotification(final Notification notification, final long param1, final long param2) {
        Check.notNull(notification, "notification"); //$NON-NLS-1$

        // This handler is only interested in version control cross-process
        // notifications.
        if (notification.getValue() <= Notification.VERSION_CONTROL_NOTIFICATION_BEGIN.getValue()
            || notification.getValue() >= Notification.VERSION_CONTROL_NOTIFICATION_END.getValue()) {
            return;
        }

        log.trace(MessageFormat.format(
            "Received notification {0} ({1}, {2})", //$NON-NLS-1$
            notification,
            Long.toHexString(param1),
            Long.toHexString(param2)));

        if (Notification.VERSION_CONTROL_WORKSPACE_CREATED == notification
            || Notification.VERSION_CONTROL_WORKSPACE_CHANGED == notification) {
            // Force a reload of the cache file before raising the notification.
            reloadCache();
        }

        /*
         * Fire the correct events on the correct clients. The two-layered
         * conditionals aren't very pretty, but it keeps all the message
         * cracking from escaping further into VersionControlClient or
         * EventEngine.
         */

        if (notification == Notification.VERSION_CONTROL_WORKSPACE_CREATED
            || notification == Notification.VERSION_CONTROL_WORKSPACE_DELETED
            || notification == Notification.VERSION_CONTROL_WORKSPACE_CHANGED
            || notification == Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED
            || notification == Notification.VERSION_CONTROL_GET_COMPLETED
            || notification == Notification.VERSION_CONTROL_LOCAL_WORKSPACE_SCAN) {
            /*
             * Match these kinds of events by collection and workspace. We only
             * ever expect 32-bit int range data in these fields, so cast to
             * int.
             */
            final int collectionHashCode = (int) param1;
            final int workspaceHashCode = (int) param2;

            for (final WorkspaceInfo wsInfo : getAllLocalWorkspaceInfo()) {
                if (matchesNotification(wsInfo, collectionHashCode, workspaceHashCode)) {
                    for (final VersionControlClient client : workstationEventListeners) {
                        if (client.getServerGUID().equals(wsInfo.getServerGUID())) {
                            final WorkspaceEvent event = new WorkspaceEvent(
                                EventSource.newFromHere(),
                                client.getWorkspace(wsInfo),
                                WorkspaceEventSource.EXTERNAL);

                            if (notification == Notification.VERSION_CONTROL_WORKSPACE_CREATED) {
                                client.getEventEngine().fireWorkspaceCreated(event);
                            } else if (notification == Notification.VERSION_CONTROL_WORKSPACE_DELETED) {
                                client.getEventEngine().fireWorkspaceDeleted(event);
                            } else if (notification == Notification.VERSION_CONTROL_WORKSPACE_CHANGED) {
                                // We can't tell the original name or location
                                // from the notification
                                client.getEventEngine().fireWorkspaceUpdated(
                                    new WorkspaceUpdatedEvent(
                                        event.getEventSource(),
                                        event.getWorkspace(),
                                        null,
                                        null,
                                        event.getWorkspaceSource()));
                            } else if (notification == Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED) {
                                client.getEventEngine().firePendingChangesChangedEvent(event);
                            } else if (notification == Notification.VERSION_CONTROL_GET_COMPLETED) {
                                client.getEventEngine().fireGetCompletedEvent(event);
                            } else if (notification == Notification.VERSION_CONTROL_LOCAL_WORKSPACE_SCAN) {
                                client.getEventEngine().fireLocalWorkspaceScanEvent(event);
                            }
                        }
                    }
                }
            }
        } else if (notification == Notification.VERSION_CONTROL_CHANGESET_RECONCILED
            || notification == Notification.VERSION_CONTROL_FOLDER_CONTENT_CHANGED) {
            /*
             * Match these kinds of events by collection only. We only ever
             * expect 32-bit int range data in these fields, so cast to int.
             */
            final int collectionHashCode = (int) param1;
            final int changesetID = (int) param2;

            for (final VersionControlClient client : workstationEventListeners) {
                if (matchesNotification(client, collectionHashCode)) {
                    if (notification == Notification.VERSION_CONTROL_CHANGESET_RECONCILED) {
                        client.getEventEngine().fireChangesetReconciledEvent(
                            new ChangesetReconciledEvent(EventSource.newFromHere(), changesetID));
                    } else if (notification == Notification.VERSION_CONTROL_FOLDER_CONTENT_CHANGED) {
                        client.getEventEngine().fireFolderContentChangedEvent(
                            new FolderContentChangedEvent(EventSource.newFromHere(), client, changesetID));
                    }
                }
            }
        }

        if (Notification.VERSION_CONTROL_WORKSPACE_DELETED == notification) {
            // Force a reload of the cache file after raising the notification.
            reloadCache();
        }

    }

    /**
     * Send a cross-process notification through the current
     * {@link NotificationManager} for an event that happened in the specified
     * workspace.
     * <p>
     * This method may be called on any thread (does not have to be the UI
     * thread).
     *
     * @param workspace
     *        the workspace where the event happened (must not be
     *        <code>null</code>)
     * @param notification
     *        the notification to send (must not be <code>null</code>)
     */
    public void notifyForWorkspace(final Workspace workspace, final Notification notification) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(notification, "notification"); //$NON-NLS-1$

        synchronized (notificationManagerLock) {
            if (notificationManager != null) {
                notificationManager.sendNotification(
                    notification,
                    getNotificationHashCode(workspace.getClient().getServerGUID()),
                    getNotificationHashCode(workspace));
            }
        }
    }

    /**
     * Send a cross-process notification through the current
     * {@link NotificationManager} that a changeset was reconciled after a gated
     * check-in.
     * <p>
     * This method may be called on any thread (does not have to be the UI
     * thread).
     *
     * @param client
     *        the client where the reconcile happened (must not be
     *        <code>null</code>)
     * @param changesetID
     *        of changeset reconciled
     */
    public void notifyForChangesetReconciled(final VersionControlClient client, final int changesetID) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        synchronized (notificationManagerLock) {
            if (notificationManager != null) {
                notificationManager.sendNotification(
                    Notification.VERSION_CONTROL_CHANGESET_RECONCILED,
                    getNotificationHashCode(client.getServerGUID()),
                    changesetID);
            }
        }
    }

    /**
     * Send a cross-process notification through the current
     * {@link NotificationManager} that server data changed without a new
     * changeset being created (from destroy, branch objects, etc.).
     * <p>
     * This method may be called on any thread (does not have to be the UI
     * thread).
     *
     * @param client
     *        the client where the change happened (must not be
     *        <code>null</code>)
     */
    public void notifyForFolderContentChanged(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        synchronized (notificationManagerLock) {
            if (notificationManager != null) {
                notificationManager.sendNotification(
                    Notification.VERSION_CONTROL_FOLDER_CONTENT_CHANGED,
                    getNotificationHashCode(client.getServerGUID()),
                    -1);
            }
        }
    }

    /**
     * Send a cross-process notification through the current
     * {@link NotificationManager} that server data changed with a new changeset
     * created.
     * <p>
     * This method may be called on any thread (does not have to be the UI
     * thread).
     *
     * @param client
     *        the client where the change happened (must not be
     *        <code>null</code>)
     * @param changesetID
     *        the ID of the committed changeset
     */
    public void notifyForFolderContentChanged(final VersionControlClient client, final int changesetID) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        synchronized (notificationManagerLock) {
            if (notificationManager != null) {
                notificationManager.sendNotification(
                    Notification.VERSION_CONTROL_FOLDER_CONTENT_CHANGED,
                    getNotificationHashCode(client.getServerGUID()),
                    changesetID);
            }
        }
    }

    private boolean matchesNotification(
        final WorkspaceInfo workspaceInfo,
        final int collectionHashCode,
        final int workspaceHashCode) {
        return notificationMatchesCollectionAndWorkspace(
            collectionHashCode,
            workspaceHashCode,
            workspaceInfo.getServerGUID(),
            getNotificationWorkspaceSpec(workspaceInfo));
    }

    private boolean matchesNotification(final VersionControlClient client, final int collectionHashCode) {
        return notificationMatchesCollection(collectionHashCode, client.getServerGUID());
    }

    /**
     * Tests whether one hash code from a cross-process notification matches the
     * GUID of the specified project collection.
     */
    private boolean notificationMatchesCollection(final int collectionHashCode, final GUID collectionGUID) {
        return getNotificationHashCode(collectionGUID) == collectionHashCode;
    }

    /**
     * Tests whether the hash codes from a cross-process notification match the
     * specified project collection and workspace.
     */
    private boolean notificationMatchesCollectionAndWorkspace(
        final int collectionHashCode,
        final int workspaceHashCode,
        final GUID collectionGUID,
        final String workspaceSpecString) {
        if (!notificationMatchesCollection(collectionHashCode, collectionGUID)) {
            return false;
        }

        return (getNotificationHashCode(workspaceSpecString) == workspaceHashCode);
    }

    /*
     * Hash code computation for notifications
     */

    private String getNotificationWorkspaceSpec(final WorkspaceInfo workspaceInfo) {
        return getNotificationWorkspaceSpec(workspaceInfo.getName(), workspaceInfo.getOwnerName());
    }

    private String getNotificationWorkspaceSpec(final Workspace workspace) {
        return getNotificationWorkspaceSpec(workspace.getName(), workspace.getOwnerName());
    }

    private String getNotificationWorkspaceSpec(final String workspaceName, final String workspaceOwner) {
        return workspaceName + ";" + workspaceOwner; //$NON-NLS-1$
    }

    /**
     * Computes the hash code for the specified {@link Workspace}. The hash
     * algorithm used is consistent across Java and .NET clients running 32-bit
     * or 64-bit architectures.
     */
    private int getNotificationHashCode(final Workspace workspace) {
        return getNotificationHashCode(getNotificationWorkspaceSpec(workspace));
    }

    /**
     * Computes the hash code for the specified project collection GUID. The
     * hash algorithm used is consistent across Java and .NET clients running
     * 32-bit or 64-bit architectures.
     */
    private int getNotificationHashCode(final String workspaceNotificationParam) {
        // Just using one of the Orcas String hash codes for now
        return CLRHashUtil.getStringHashOrcas64(workspaceNotificationParam);
    }

    /**
     * Computes the hash code for the specified project collection GUID. The
     * hash algorithm used is consistent across Java and .NET clients running
     * 32-bit or 64-bit architectures.
     */
    private int getNotificationHashCode(final GUID collectionGUID) {
        // Use the GUID String representation "D" which is 32 digits separated
        // by hyphens:
        // xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        return CLRHashUtil.getStringHashOrcas64(
            collectionGUID.getGUIDString(GUIDStringFormat.DASHED).toUpperCase(Locale.ENGLISH));
    }

    /*
     * Private event dispatch/fire methods
     */

    private void onCacheFileReloaded() {
        // Chain up to interested clients
        for (final VersionControlClient client : workstationEventListeners) {
            // Each Workspace in the cache needs to have its uncacheable
            // properties invalidated.
            client.getRuntimeWorkspaceCache().invalidateAllWorkspaces();

            client.getEventEngine().fireWorkspaceCacheFileReloaded(
                new WorkspaceCacheFileReloadedEvent(EventSource.newFromHere(), this));
        }
    }

    /**
     * The NonFatalError event is sent when some error occurs in the client but
     * does not cause the processing of the command to terminate.
     */
    private void onNonFatalError(final InternalWorkspaceConflictInfo[] conflictingWorkspaces) {
        for (final InternalWorkspaceConflictInfo iwci : conflictingWorkspaces) {
            onNonFatalError(new WorkstationNonFatalErrorEvent(EventSource.newFromHere(), iwci));
        }
    }

    private void onNonFatalError(final WorkstationNonFatalErrorEvent event) {
        // Chain up to interested clients
        for (final VersionControlClient client : workstationEventListeners) {
            client.getEventEngine().fireWorkstationNonFatalError(event);
        }
    }

    /*
     * Version control workspace cache
     */

    /**
     * Get a list of all cached local workspace info objects.
     *
     * @return workspace info objects
     */
    public WorkspaceInfo[] getAllLocalWorkspaceInfo() {
        synchronized (cacheMutex) {
            return getCache().getAllWorkspaces();
        }
    }

    /**
     * Get the cached local workspace info for the specified workspace.
     *
     * @param sourceControl
     *        the server associated with the specified workspace
     * @param workspaceName
     *        the name of the workspace
     * @param workspaceOwner
     *        the workspace owner
     * @return info on the workspace or null
     */
    public WorkspaceInfo getLocalWorkspaceInfo(
        final VersionControlClient client,
        final String workspaceName,
        final String workspaceOwner) {
        return getLocalWorkspaceInfo(client.getServerGUID(), workspaceName, workspaceOwner);
    }

    /**
     * Get the cached local workspace info for the specified workspace.
     *
     * @param repositoryGuid
     *        GUID for the associated server
     * @param workspaceName
     *        the name of the workspace (must not be <code>null</code>)
     * @param workspaceOwner
     *        the workspace owner (must not be <code>null</code>)
     * @return info on the workspace or null
     */
    public WorkspaceInfo getLocalWorkspaceInfo(
        final GUID repositoryGuid,
        final String workspaceName,
        final String workspaceOwner) {
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$
        Check.notNull(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        requireFullyQualifiedUserName(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$

        return getCache().getWorkspace(repositoryGuid, workspaceName, workspaceOwner);
    }

    /**
     * Get the cached local workspace info for the workspace that contains the
     * specified path.
     *
     * @param path
     *        the local path (must not be <code>null</code> or empty)
     * @return info on the containing workspace or null if not in a workspace
     */
    public WorkspaceInfo getLocalWorkspaceInfo(String path) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$

        path = LocalPath.canonicalize(path);

        return getCache().getWorkspace(path);
    }

    /**
     * Gets the workspace at or workspaces below the path.
     *
     * @param path
     *        the path (must not be <code>null</code> or empty)
     * @return the workspaces for the path and its children
     */
    public WorkspaceInfo[] getLocalWorkspaceInfoRecursively(String path) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$

        path = LocalPath.canonicalize(path);

        final List<WorkspaceInfo> infoList = new ArrayList<WorkspaceInfo>();
        for (final WorkspaceInfo ws : getCache().getAllWorkspaces()) {
            for (final String mappedPath : ws.getMappedPaths()) {
                if (LocalPath.isChild(path, mappedPath)) {
                    infoList.add(ws);
                    break;
                }
            }
        }

        return infoList.toArray(new WorkspaceInfo[infoList.size()]);
    }

    /**
     * Get a list of cached local workspace info. If workspaceName is null, all
     * workspaces in the cache file for the specified owner are returned. If
     * workspaceName is not null, then only those workspaces with a matching
     * name are returned.
     *
     * @param sourceControl
     *        the source control repository (if null, any repository)
     * @param workspaceName
     *        the name of the workspace (if null, all workspaces for this
     *        computer)
     * @param workspaceOwner
     *        the workspace owner (if null, any workspace owner)
     * @return the matching workspace info instances
     */
    public WorkspaceInfo[] queryLocalWorkspaceInfo(
        final VersionControlClient client,
        final String workspaceName,
        String workspaceOwner) {
        // If the caller specified the constant, we need to resolve it.
        if (workspaceOwner == VersionControlConstants.AUTHENTICATED_USER) {
            if (client != null) {
                workspaceOwner = client.getConnection().getAuthorizedAccountName();
            } else {
                // We can't handle the constant when there is no
                // VersionControlServer instance (partial
                // names are fine).
                requireFullyQualifiedUserName(workspaceOwner, "workspaceOwner"); //$NON-NLS-1$
            }
        }

        final List<WorkspaceInfo> list = new ArrayList<WorkspaceInfo>();
        for (final WorkspaceInfo workspaceInfo : getCache().getAllWorkspaces()) {
            if ((client == null || client.getServerGUID().equals(workspaceInfo.getServerGUID()))
                && (workspaceName == null || Workspace.matchName(workspaceInfo.getName(), workspaceName))
                && (workspaceOwner == null || workspaceInfo.ownerNameMatches(workspaceOwner))) {
                list.add(workspaceInfo);
            }
        }

        return list.toArray(new WorkspaceInfo[list.size()]);
    }

    /**
     * Returns true if the specified local path is mapped in a workspace.
     *
     * @param the
     *        local path
     * @return true if the path is mapped in a workspace; false otherwise
     */
    public boolean isMapped(String path) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$

        path = LocalPath.canonicalize(path);

        return getCache().getMapping(path) != null;
    }

    /**
     * Returns true if the specified local path is explicitly mapped in a
     * workspace (i.e., it is a root mapping).
     *
     * @param the
     *        local path
     * @return true if the path is explicitly mapped in a workspace; false
     *         otherwise
     */
    public boolean isExplicitlyMapped(String path) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$

        path = LocalPath.canonicalize(path);

        final String mappedPath = getCache().getMapping(path);
        return mappedPath != null && LocalPath.equals(mappedPath, path);
    }

    /**
     * If updateWorkspaceInfoCache() has not yet been called for the specified
     * server and owner, this method will call it. Otherwise, it returns without
     * contacting the server.
     */
    public void ensureUpdateWorkspaceInfoCache(final VersionControlClient client, final String ownerName) {
        ensureUpdateWorkspaceInfoCache(client, ownerName, Long.MAX_VALUE);
    }

    /**
     * If updateWorkspaceInfoCache() has not yet been called for the specified
     * server and owner within the specified time, this method will call it.
     * Otherwise, it returns without contacting the server.
     *
     * @param client
     *        the client (must not be <code>null</code>)
     * @param ownerName
     *        the owner of the workspaces (must not be <code>null</code> or
     *        empty)
     * @param maxAgeMillis
     *        the maximum age (not time) of the last request in milliseconds
     */
    public void ensureUpdateWorkspaceInfoCache(
        final VersionControlClient client,
        final String ownerName,
        final long maxAgeMillis) {
        boolean needRefresh;
        String key;

        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNullOrEmpty(ownerName, "ownerName"); //$NON-NLS-1$

        synchronized (cacheMutex) {
            // It is okay to use the passed in ownerName here to construct the
            // key because all valid forms of ownerName are in the table.
            key = createLoadWorkspacesTableKey(client, ownerName);
            needRefresh = (!workspacesLoadedTable.containsKey(key)
                || (System.currentTimeMillis() - workspacesLoadedTable.get(key)) > maxAgeMillis);
        }

        if (needRefresh) {
            updateWorkspaceInfoCache(key, client, ownerName);
        }
    }

    /**
     * Updates the cache to reference the specified source control repository
     * using the specified URI, if different.
     *
     * @param client
     *        the version control client (must not be <code>null</code>)
     * @param uri
     *        the URI for the server to use in the cache (must not be
     *        <code>null</code>)
     */
    public void updateServerURIReferences(final VersionControlClient client, final URI uri) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        getCache().updateServerURI(client.getServerGUID(), uri);
        saveConfigIfDirty();
    }

    /**
     * Loads into the cache the workspaces residing in the specified source
     * control repository and having the specified owner.
     *
     * @param client
     *        the client (must not be <code>null</code>)
     * @param ownerName
     *        the owner of the workspaces (must not be <code>null</code> or
     *        empty)
     */
    public void updateWorkspaceInfoCache(final VersionControlClient client, final String ownerName) {
        updateWorkspaceInfoCache(client, ownerName, new AtomicReference<Workspace[]>());
    }

    /**
     * Loads into the cache the workspaces residing in the specified source
     * control repository and having the specified owner. Returns the workspaces
     * from the QueryWorkspaces call made by this method.
     *
     * @param client
     *        the client (must not be <code>null</code>)
     * @param ownerName
     *        the owner of the workspaces (must not be <code>null</code> or
     *        empty)
     * @param workspaces
     *        the holder for the workspaces (must not be <code>null</code>)
     */
    public void updateWorkspaceInfoCache(
        final VersionControlClient client,
        final String ownerName,
        final AtomicReference<Workspace[]> workspaces) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNullOrEmpty(ownerName, "ownerName"); //$NON-NLS-1$
        Check.notNull(workspaces, "workspaces"); //$NON-NLS-1$

        // It is okay to use the passed in ownerName here to construct the key
        // because all valid forms of ownerName are in the table.
        final String key = createLoadWorkspacesTableKey(client, ownerName);
        workspaces.set(updateWorkspaceInfoCache(key, client, ownerName));
    }

    private Workspace[] updateWorkspaceInfoCache(
        final String key,
        final VersionControlClient client,
        String ownerName) {
        // Do not allow null for owner name. We do not want to put the
        // workspaces for every owner
        // into the cache. The machine may be a server with many different users
        // having workspaces
        // on it.
        Check.notNullOrEmpty(ownerName, "ownerName"); //$NON-NLS-1$

        // Make sure we have the fully qualified owner name.
        ownerName = client.resolveUserUniqueName(ownerName);

        // We do this *before* removing the workspaces from cache in case it
        // throws.
        Workspace[] workspaces = null;

        if (client.isAuthorizedUser(ownerName)) {
            // Only add the permissions filter when ownerName is the
            // AuthenticatedUser.
            workspaces = client.queryWorkspaces(
                null,
                ownerName,
                getName(),
                WorkspacePermissions.READ.combine(WorkspacePermissions.USE));
        } else {
            workspaces = client.queryWorkspaces(null, ownerName, getName());
        }

        // Refresh the server GUID in case it changed somehow
        client.refreshServerGUID();

        final List<InternalWorkspaceConflictInfo> warningList = new ArrayList<InternalWorkspaceConflictInfo>();
        final List<KeyValuePair<Exception, Workspace>> errorList = new ArrayList<KeyValuePair<Exception, Workspace>>();

        final Set<String> keysUpdated = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        // Make sure to add the key passed in. This way if no workspaces are
        // found at least we can record that nothing was found for this key.
        keysUpdated.add(key);

        synchronized (cacheMutex) {
            // Remove all workspaces for the specified repository.
            final List<WorkspaceInfo> infoRemovedList = removeCachedWorkspaceInfo(client, workspaces, false);
            infoRemovedList.addAll(removeCachedWorkspaceInfo(client, ownerName, false));

            // Add all workspaces that are local and are owned by the specified
            // owner.
            for (final Workspace workspace : workspaces) {
                try {
                    final AtomicReference<InternalWorkspaceConflictInfo[]> cws1 =
                        new AtomicReference<InternalWorkspaceConflictInfo[]>();

                    final WorkspaceInfo info = getCache().insertWorkspace(workspace, cws1);

                    for (final InternalWorkspaceConflictInfo iwci : cws1.get()) {
                        warningList.add(iwci);
                    }

                    // For each workspace info that was removed, we want to copy
                    // its local metadata (e.g., LastSavedCheckin) to the new
                    // object.
                    copyLocalMetadata(info, infoRemovedList);

                    keysUpdated.addAll(createLoadWorkspacesTableKeys(client, workspace));
                } catch (final VersionControlException exception) {
                    // Skip the workspace if there's a mapping conflict, etc.
                    errorList.add(new KeyValuePair<Exception, Workspace>(exception, workspace));
                }
            }

            final AtomicReference<InternalWorkspaceConflictInfo[]> cws2 =
                new AtomicReference<InternalWorkspaceConflictInfo[]>();

            InternalCacheLoader.saveConfigIfDirty(getCache(), cws2, cacheMutex, workspaceCacheFile);

            for (final InternalWorkspaceConflictInfo iwci : cws2.get()) {
                warningList.add(iwci);
            }

            // Record for EnsureUpdateWorkspaceInfoCache() the fact that we have
            // already queried the server for the user's workspaces.
            final Long now = System.currentTimeMillis();
            for (final String updatedKey : keysUpdated) {
                workspacesLoadedTable.put(updatedKey, now);
            }
        }

        // Raise all of the error events after releasing the lock.
        for (final InternalWorkspaceConflictInfo iwci : warningList) {
            onNonFatalError(new WorkstationNonFatalErrorEvent(EventSource.newFromHere(), iwci));
        }
        for (final KeyValuePair<Exception, Workspace> error : errorList) {
            onNonFatalError(
                new WorkstationNonFatalErrorEvent(EventSource.newFromHere(), error.getKey(), error.getValue()));
        }

        return workspaces;
    }

    /**
     * If the new workspace info object matches one that was removed, add back
     * the local metadata (e.g., LastSavedCheckin).
     */
    private void copyLocalMetadata(final WorkspaceInfo info, final List<WorkspaceInfo> infoRemovedList) {
        for (final WorkspaceInfo removedInfo : infoRemovedList) {
            if (removedInfo.equals(info)) {
                info.copyLocalMetadata(removedInfo);
                break;
            }
        }
    }

    /**
     * Returns the keys used to index the private load workspaces table for the
     * provided workspace.
     */
    private List<String> createLoadWorkspacesTableKeys(final VersionControlClient client, final Workspace workspace) {
        final List<String> keys = new ArrayList<String>();

        keys.add(createLoadWorkspacesTableKey(client, workspace.getOwnerName()));
        keys.add(createLoadWorkspacesTableKey(client, workspace.getOwnerDisplayName()));

        for (final String alias : workspace.getOwnerAliases()) {
            keys.add(createLoadWorkspacesTableKey(client, alias));
        }

        return keys;
    }

    /**
     * Returns a key used to index the private load workspaces table.
     */
    private String createLoadWorkspacesTableKey(final VersionControlClient client, final String ownerName) {
        return ownerName + '@' + client.getConnection().getBaseURI().toString();
    }

    /**
     * This helper method inserts a workspace into the Workstation cache. The
     * caller is responsible for writing out the Workstation cache.
     */
    public WorkspaceInfo insertWorkspaceIntoCache(final Workspace localWorkspace) {
        final AtomicReference<InternalWorkspaceConflictInfo[]> conflictingWorkspaces =
            new AtomicReference<InternalWorkspaceConflictInfo[]>();
        final WorkspaceInfo workspaceInfo = getCache().insertWorkspace(localWorkspace, conflictingWorkspaces);
        onNonFatalError(conflictingWorkspaces.get());
        return workspaceInfo;
    }

    /**
     * Removes from the cache the workspaces for the specified source control
     * repository.
     *
     * @param client
     *        the source control repository
     * @return the {@link WorkspaceInfo} objects that are removed from the cache
     */
    public WorkspaceInfo[] removeCachedWorkspaceInfo(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        final List<WorkspaceInfo> ret =
            removeCachedWorkspaceInfo(client, client.getConnection().getAuthorizedAccountName(), true);
        return ret.toArray(new WorkspaceInfo[ret.size()]);
    }

    /**
     * Removes from the cache the workspaces for the specified source control
     * repository.
     *
     * @param client
     *        the source control repository (must not be <code>null</code>)
     * @param ownerName
     *        the owner of the workspaces (must not be <code>null</code> or
     *        empty) <param name="ownerName">the owner of the workspaces</param>
     *
     * @return the {@link WorkspaceInfo} objects that are removed from the cache
     */
    public WorkspaceInfo[] removeCachedWorkspaceInfo(final VersionControlClient client, String ownerName) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNullOrEmpty(ownerName, "ownerName"); //$NON-NLS-1$

        // Make sure we have the fully qualified owner name.
        ownerName = client.resolveUserUniqueName(ownerName);

        final List<WorkspaceInfo> ret = removeCachedWorkspaceInfo(client, ownerName, true);
        return ret.toArray(new WorkspaceInfo[ret.size()]);
    }

    /**
     * Private method to remove from the cache workspaces associated with the
     * specified repository. It returns the WorkspaceInfo objects that are
     * removed from the cache.
     */
    private List<WorkspaceInfo> removeCachedWorkspaceInfo(
        final VersionControlClient client,
        String ownerName,
        final boolean saveConfigFile) {
        // If the caller specified the constant, we need to resolve it.
        if (ownerName.equals(VersionControlConstants.AUTHENTICATED_USER)) {
            ownerName = client.getConnection().getAuthorizedAccountName();
        }

        final List<WorkspaceInfo> infoRemovedList = new ArrayList<WorkspaceInfo>();

        synchronized (cacheMutex) {
            final WorkspaceInfo[] allWorkspaceInfo = getAllLocalWorkspaceInfo();
            for (final WorkspaceInfo workspaceInfo : allWorkspaceInfo) {
                // The server match is by name or guid since the guid match
                // fails if the database is re-installed.
                if ((ownerName == null || workspaceInfo.ownerNameMatches(ownerName))
                    && (Workspace.matchServerURI(workspaceInfo.getServerURI(), client.getConnection().getBaseURI())
                        || workspaceInfo.getServerGUID().equals(client.getServerGUID()))) {
                    getCache().removeWorkspace(workspaceInfo);
                    infoRemovedList.add(workspaceInfo);
                }
            }

            if (saveConfigFile) {
                saveConfigIfDirty();
            }
        }

        return infoRemovedList;
    }

    /**
     * Private method to remove from the cache workspaces associated with the
     * specified repository. It returns the WorkspaceInfo objects that are
     * removed from the cache.
     */
    private List<WorkspaceInfo> removeCachedWorkspaceInfo(
        final VersionControlClient client,
        final Workspace[] workspaces,
        final boolean saveConfigFile) {
        final Map<String, Workspace> workspacesDict = new TreeMap<String, Workspace>(String.CASE_INSENSITIVE_ORDER);

        for (final Workspace toBeRemoved : workspaces) {
            // We will look up in our hash table using
            // workspacename;workspaceowner.
            workspacesDict.put(toBeRemoved.getQualifiedName(), toBeRemoved);
        }

        final List<WorkspaceInfo> infoRemovedList = new ArrayList<WorkspaceInfo>();

        synchronized (cacheMutex) {
            final WorkspaceInfo[] allWorkspaceInfo = getAllLocalWorkspaceInfo();
            for (final WorkspaceInfo workspaceInfo : allWorkspaceInfo) {
                // The server match is by name or guid since the guid match
                // fails if the database is re-installed.
                if (Workspace.matchServerURI(workspaceInfo.getServerURI(), client.getConnection().getBaseURI())
                    || workspaceInfo.getServerGUID().equals(client.getServerGUID())) {
                    // If the workspace was specified for removal, or if we
                    // happen to find a workspace
                    // whose URI matches ours but has a different server GUID,
                    // remove it.
                    if (workspacesDict.containsKey(workspaceInfo.getQualifiedName())
                        || !workspaceInfo.getServerGUID().equals(client.getServerGUID())) {
                        getCache().removeWorkspace(workspaceInfo);
                        infoRemovedList.add(workspaceInfo);
                    }
                }
            }

            if (saveConfigFile) {
                saveConfigIfDirty();
            }
        }

        return infoRemovedList;
    }

    /**
     * Removes from the cache the workspace specified. Since the server may no
     * longer exist, the server is not contacted and the match is by name.
     *
     * @param serverUri
     *        the URI of the server containing the workspaces to remove from the
     *        cache; passing null matches all servers
     * @param workspaceName
     *        the name of the workspace to remove from the cache; passing null
     *        matches all workspaces
     * @param workspaceOwner
     *        the owner of the workspace to remove from the cache; passing null
     *        matches all owners
     * @return the WorkspaceInfo objects that were removed from the cache
     */
    public WorkspaceInfo[] removeCachedWorkspaceInfo(
        final URI serverUri,
        final String workspaceName,
        final String workspaceOwner) {
        final List<WorkspaceInfo> infoRemovedList = new ArrayList<WorkspaceInfo>();

        synchronized (cacheMutex) {
            for (final WorkspaceInfo workspaceInfo : getAllLocalWorkspaceInfo()) {
                if ((serverUri == null || Workspace.matchServerURI(workspaceInfo.getServerURI(), serverUri))
                    && (workspaceName == null
                        || workspaceName.length() == 0
                        || Workspace.matchName(workspaceInfo.getName(), workspaceName))
                    && (workspaceOwner == null
                        || workspaceOwner.length() == 0
                        || workspaceInfo.ownerNameMatches(workspaceOwner))) {
                    getCache().removeWorkspace(workspaceInfo);
                    infoRemovedList.add(workspaceInfo);
                }
            }

            if (isCacheEnabled()) {
                saveConfigIfDirty();
            }
        }

        return infoRemovedList.toArray(new WorkspaceInfo[infoRemovedList.size()]);
    }

    /**
     * If the user name is not fully qualified, we'll throw an exception.
     */
    private void requireFullyQualifiedUserName(final String userName, final String paramName) {
        Check.notNull(userName, "userName"); //$NON-NLS-1$

        if (VersionControlConstants.AUTHENTICATED_USER.equals(userName)) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("Workstation.FullyQualifiedUserNameRequiredFormat"), //$NON-NLS-1$
                    userName));
        }
    }

    /**
     * Internal method to save the cache if it has been modified.
     */
    public void saveConfigIfDirty() {
        if (isCacheEnabled()) {
            final AtomicReference<InternalWorkspaceConflictInfo[]> conflictingWorkspaces =
                new AtomicReference<InternalWorkspaceConflictInfo[]>();

            InternalCacheLoader.saveConfigIfDirty(getCache(), conflictingWorkspaces, cacheMutex, workspaceCacheFile);

            onNonFatalError(conflictingWorkspaces.get());
        }
    }

    /**
     * Internal method to get the {@link InternalCache}.
     */
    public InternalCache getCache() {
        boolean cacheReloaded = false;
        InternalCache ret;

        final AtomicReference<InternalWorkspaceConflictInfo[]> outConflictingWorkspaces =
            new AtomicReference<InternalWorkspaceConflictInfo[]>();

        synchronized (cacheMutex) {
            cacheReloaded = ensureCacheLoaded(outConflictingWorkspaces);
            ret = workspaceCache;
        }

        if (cacheReloaded) {
            onNonFatalError(outConflictingWorkspaces.get());

            // Let the listeners (Client objects) know.
            onCacheFileReloaded();
        }

        return ret;
    }

    /**
     * Call Workstation.Current.ReloadCache() to force a reload of the cache
     * file from disk.
     */
    public void reloadCache() {
        boolean cacheReloaded = false;
        final AtomicReference<InternalWorkspaceConflictInfo[]> outConflictingWorkspaces =
            new AtomicReference<InternalWorkspaceConflictInfo[]>();

        synchronized (cacheMutex) {
            if (cacheEnabled) {
                // Force a reload of the cache file before raising the
                // notification.
                cacheFileChanged = true;
                cacheReloaded = ensureCacheLoaded(outConflictingWorkspaces);
            }
        }

        if (cacheReloaded) {
            onNonFatalError(outConflictingWorkspaces.get());

            // Let the listeners (Client objects) know.
            onCacheFileReloaded();
        }
    }

    /**
     * Ensure the cache is loaded from disk if it has changed. Return a boolean
     * indicating if the cache was loaded.
     *
     * @param outConflictingWorkspaces
     *        returns value for conflicting workspace. Can only have a value if
     *        the cache was loaded from disk.
     *
     * @return true if the cache was loaded from disk, false otherwise.
     */
    private boolean ensureCacheLoaded(final AtomicReference<InternalWorkspaceConflictInfo[]> outConflictingWorkspaces) {
        synchronized (cacheMutex) {
            // If the cache file changed, reload it.
            if (cacheFileChanged) {
                cacheFileChanged = false;

                workspaceCache = InternalCacheLoader.loadConfig(
                    workspaceCache,
                    cacheEnabled,
                    outConflictingWorkspaces,
                    cacheMutex,
                    workspaceCacheFile);

                return true;
            }

            return false;
        }
    }

    /*
     * Exclusions
     */

    /**
     * Removes an exclusion from the local item exclusion list.
     *
     * @param The
     *        exclusion to remove (must not be <code>null</code> or empty)
     */
    public void removeLocalItemExclusion(final VersionControlClient client, final String exclusion) {
        Check.notNullOrEmpty(exclusion, "exclusion"); //$NON-NLS-1$

        final InternalServerInfo serverInfo =
            getCache().getServerInfoByGUID(client.getServerGUID(), client.getConnection().getBaseURI());

        getLocalItemExclusionCache().removeExclusion(serverInfo, exclusion);
    }

    /**
     * Returns the set of local item exclusions for this user on this machine.
     */
    public String[] getLocalItemExclusions(final VersionControlClient client) {
        final InternalServerInfo serverInfo =
            getCache().getServerInfoByGUID(client.getServerGUID(), client.getConnection().getBaseURI());

        return getLocalItemExclusionCache().getExclusions(serverInfo);
    }

    /**
     * Overwrites the list of local item exclusions with the list passed in.
     *
     * @param exclusions
     *        The new exclusions
     */
    public void setLocalItemExclusions(final VersionControlClient client, final String[] exclusions) {
        final InternalServerInfo serverInfo =
            getCache().getServerInfoByGUID(client.getServerGUID(), client.getConnection().getBaseURI());

        getLocalItemExclusionCache().setDefaultExclusions(serverInfo, exclusions);
    }

    /**
     * Checks to see if the default local item exclusions have changed on the
     * server. If force is false, this will only happen once a week.
     *
     * @param force
     *        if true the server is contacted every time, otherwise if enough
     *        time has elapsed since the last check
     */
    public void checkForLocalItemExclusionUpdates(final VersionControlClient client, final boolean force) {
        final InternalServerInfo serverInfo =
            getCache().getServerInfoByGUID(client.getServerGUID(), client.getConnection().getBaseURI());

        synchronized (configurationMutex) {
            if (!force) {
                final Calendar lastUpdatePlusSevenDays =
                    getLocalItemExclusionCache().getLastDefaultExclusionUpdate(serverInfo);
                lastUpdatePlusSevenDays.add(Calendar.DAY_OF_MONTH, 7);
                if (lastUpdatePlusSevenDays.after(Calendar.getInstance())) {
                    // nothing to do
                    return;
                }
            }

            // Call the server to get the latest default exclusions
            final AtomicBoolean fallbackUsed = new AtomicBoolean();
            final ServerSettings settings = client.getServerSettingsWithFallback(fallbackUsed);

            if (settings.getDefaultLocalItemExclusionSet() != null) {
                getLocalItemExclusionCache().setDefaultExclusions(
                    serverInfo,
                    settings.getDefaultLocalItemExclusionSet());
            }
        }
    }

    private LocalItemExclusionCache getLocalItemExclusionCache() {
        synchronized (configurationMutex) {
            if (localItemExclusionCache == null) {
                // Unlike VS we pass in the file; it's easier this way
                localItemExclusionCache =
                    new LocalItemExclusionCache(localItemExclusionCacheFile, configurationEnabled);
            }

            return localItemExclusionCache;
        }
    }

    /*
     * General properties
     */

    /**
     * @return the name of this {@link Workstation} (aka machine name or
     *         computer name).
     */
    public String getName() {
        if (type == WorkstationType.CURRENT) {
            return LocalHost.getShortName();
        } else {
            // We don't have remote workstation instances in version 1.
            throw new RuntimeException("Getting the name of a non-CURRENT Workstations is not supported"); //$NON-NLS-1$
        }
    }

    /**
     * When true, the cache directory exists and will be used. When false,
     * either we don't have access to the directory, it doesn't exist, or it has
     * been set to false by the application, and we'll need to run without the
     * cache.
     */
    public boolean isCacheEnabled() {
        synchronized (cacheMutex) {
            return cacheEnabled;
        }
    }

    /**
     * When true, the configuration directory exists and will be used. When
     * false, either we don't have access to the directory, it doesn't exist, or
     * it has been set to false by the application, and we'll need to run
     * without the configuration cache.
     */
    public boolean isConfigurationEnabled() {
        synchronized (configurationMutex) {
            return configurationEnabled;
        }
    }

    /*
     * Initialization
     */

    /**
     * Gets the directory where {@link Workstation} stores its cache files. This
     * may not be the given {@link PersistenceStoreProvider}'s value if certain
     * environment variables or application config settings are specified.
     *
     * @param provider
     *        the {@link PersistenceStoreProvider} to get the default location
     *        from (must not be <code>null</code>)
     * @return the directory where {@link Workstation} will store its cache
     *         files or <code>null</code> if the cache directory is not
     *         available
     */
    private static File getCacheDirectory(final PersistenceStoreProvider provider) {
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        String defaultDirectory = null;
        if (provider.getCachePersistenceStore() != null) {
            defaultDirectory = provider.getCachePersistenceStore().getStoreFile().getAbsolutePath();
        }

        return getProperSettingsDirectory(defaultDirectory, EnvironmentVariables.WORKSTATION_CACHE_DIRECTORY);
    }

    /**
     * Gets the directory where {@link Workstation} stores its non-cache
     * configuration files. This may not be the given
     * {@link PersistenceStoreProvider}'s value if certain environment variables
     * or application config settings are specified.
     *
     * @param provider
     *        the {@link PersistenceStoreProvider} to get the default location
     *        from (must not be <code>null</code>)
     * @return the directory where {@link Workstation} will store its
     *         configuration files or <code>null</code> if the configuration
     *         directory is not available
     */
    public static File getConfigurationDirectory(final PersistenceStoreProvider provider) {
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        String defaultDirectory = null;
        if (provider.getConfigurationPersistenceStore() != null) {
            defaultDirectory = ((FilesystemPersistenceStore) provider.getConfigurationPersistenceStore().getChildStore(
                "VersionControl")).getStoreFile().getAbsolutePath(); //$NON-NLS-1$
        }

        return getProperSettingsDirectory(defaultDirectory, EnvironmentVariables.WORKSTATION_CONFIGURATION_DIRECTORY);
    }

    /**
     * Returns the directory location where workspace local version metadata
     * files are stored.
     *
     * @param provider
     *        the provider to find configuration storage with (must not be
     *        <code>null</code>)
     */
    public static File getOfflineMetadataFileRoot(final PersistenceStoreProvider provider) {
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        File location = null;

        // Try the environment variable first

        final String envVar = PlatformMiscUtils.getInstance().getEnvironmentVariable(
            EnvironmentVariables.OFFLINE_METADATA_ROOT_DIRECTORY);
        if (envVar != null && envVar.length() > 0) {
            location = new File(PlatformMiscUtils.getInstance().expandEnvironmentString(envVar));
        }

        // On Windows use the common app data folder

        if (location == null && Platform.isCurrentPlatform(Platform.WINDOWS)) {
            // This folder must always be available
            final String appData = SpecialFolders.getCommonApplicationDataPath();
            Check.notNull(appData, "appData"); //$NON-NLS-1$

            // This must match the VS logic
            location = new File(appData);
            location = new File(location, "Microsoft Team Foundation Local Workspaces"); //$NON-NLS-1$
        }

        // Non-Windows platforms use the "TEE-Offline" child in the
        // configuration settings store

        if (location == null) {
            location = ((FilesystemPersistenceStore) provider.getConfigurationPersistenceStore().getChildStore(
                NON_WINDOWS_OFFLINE_STORAGE_CHILD_NAME)).getStoreFile();
        }

        return location;
    }

    /**
     * Evaluates a default path, environment variable, and application config
     * setting to determine the correct settings directory to use.
     *
     * @param frameworkPath
     *        the path that comes from the {@link PersistenceStoreProvider} (may
     *        be <code>null</code>)
     * @param environmentVarOverride
     *        the name of the environment variable that, if set, overrides all
     *        other paths (must not be <code>null</code>)
     * @return the directory to use or <code>null</code> if none of the inputs
     *         resulted in a directory being found
     */
    private static File getProperSettingsDirectory(final String frameworkPath, final String environmentVarOverride) {
        Check.notNull(environmentVarOverride, "environmentVarOverride"); //$NON-NLS-1$

        /*
         * The cache file path is either in the .config file, the env var, or
         * the local settings directory (via tfs object). The environment
         * variable support is required for trun tests to be able to use a
         * separate cache file.
         */
        String settingsDir = PlatformMiscUtils.getInstance().getEnvironmentVariable(environmentVarOverride);
        if (settingsDir == null || settingsDir.length() == 0) {
            settingsDir = frameworkPath;
        } else {
            settingsDir = PlatformMiscUtils.getInstance().expandEnvironmentString(settingsDir);
        }

        if (settingsDir == null || settingsDir.length() == 0) {
            // If we have to run without a cache file, make sure the path is
            // null.
            settingsDir = null;
        } else {
            // Make sure it's a legal path.
            settingsDir = LocalPath.canonicalize(settingsDir);
        }

        return new File(settingsDir);
    }

    private static boolean ensureLocalPathIsUsable(final File directory, final String shortFileName) {
        if (directory == null || shortFileName == null || shortFileName.length() == 0) {
            return false;
        }

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                log.debug(MessageFormat.format("Could not create directory {0}", directory)); //$NON-NLS-1$
                return false;
            }
        }

        final File file = new File(directory, shortFileName);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    log.debug(MessageFormat.format("Could not create file {0}", shortFileName)); //$NON-NLS-1$
                    return false;
                }

                file.delete();
            } catch (final IOException e) {
                log.debug(MessageFormat.format("Exception creating file {0}", shortFileName), e); //$NON-NLS-1$
                return false;
            }
        }

        return true;
    }
}
