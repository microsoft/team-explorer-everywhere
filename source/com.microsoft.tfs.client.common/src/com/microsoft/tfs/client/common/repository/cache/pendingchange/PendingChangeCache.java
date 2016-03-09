// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.pendingchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.FolderContentChangedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.FolderContentChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.LocalWorkspaceScanListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * {@link PendingChangeCache} caches pending changes ({@link PendingChange}) for
 * a single TFS workspace.
 * </p>
 *
 * <p>
 * Cached pending changes can be retrieved by pending change ID (
 * {@link #getPendingChange(int)}), local path (
 * {@link #getPendingChangeByLocalPath(String)}), and server path (
 * {@link #getPendingChangesByServerPath(String)}). All of the pending changes
 * can be retrieved by calling {@link #getPendingChanges()}. Clients of this
 * cache are required to not tamper with the pending change objects returned
 * from the cache.
 * </p>
 *
 * <p>
 * To receive notification when the cached data changes, call
 * {@link #addListener(PendingChangeCacheListener)}.
 * </p>
 *
 * <p>
 * This class attaches listeners to the Core objects that it uses. The
 * {@link #dispose()} method must be called when an instance of
 * {@link PendingChangeCache} is no longer needed. The {@link #dispose()} method
 * removes the Core listeners and allows the cache to be garbage collected.
 * </p>
 */
public class PendingChangeCache {
    private static final Log log = LogFactory.getLog(PendingChangeCache.class);

    /**
     * The workspace this cache is caching pending changes for (never
     * <code>null</code>).
     */
    private final Workspace workspace;

    /**
     * The data structure that holds the cached pending change data (never
     * <code>null</code>).
     */
    private final PendingChangeCollection changes;

    /**
     * The event listener we attach to Core's event engine (never
     * <code>null</code>).
     */
    private final CoreEventListener coreEventListener;

    /**
     * Manages the {@link PendingChangeCacheListener}s attached to this cache
     * (never <code>null</code>).
     */
    private final SingleListenerFacade listeners;

    /**
     * <p>
     * A lock that is acquired to give atomic semantics to some operations. For
     * instance, a refresh operation can't happen concurrently with another
     * refresh operation - some interleavings of the threads would add duplicate
     * items, among other problems.
     * </p>
     *
     * <p>
     * This lock is acquired only by entry points. Entry points are either
     * public methods or core event handling methods. Internal helper methods
     * that are not entry points should not acquire this lock.
     * </p>
     *
     * <p>
     * This lock provides only atomic semantics to operations in this class that
     * modify cached data. Visibility of the cached data is provided by a
     * separate lock that's encapsulated in the PendingChangeCollection class.
     * The two locks are not related (they serve different purposes).
     * </p>
     */
    private final Object atomicOperationLock = new Object();

    /**
     * Keeps track of the current operation depth to ensure that we send out the
     * right sequence of events to {@link PendingChangeCacheListener}s.
     */
    private int operationDepth;

    /**
     * Tracks whether {@link #changes} is actually modified between core's begin
     * operation and end operation events. {@link #beginUpdatePendingChanges()}
     * resets this variable (to <code>false</code>) if the
     * {@link #operationDepth} field is 0, and methods which modify
     * {@link #changes} set the field to true. When
     * {@link #endUpdatePendingChanges()} runs, the event includes whether any
     * pending changes were actually modified.
     */
    private boolean changesModifiedInOperation;

    /**
     * A lock that must be acquired to read/write the {@link #operationDepth}
     * and {@link #changesModifiedInOperation} fields.
     */
    private final Object operationDepthAndModificationLock = new Object();

    /**
     * Creates a new {@link PendingChangeCache} for the specified workspace. The
     * constructor attaches listeners to Core objects and immediately queries
     * the server to initially populate the cache with pending changes.
     *
     * @param workspace
     *        the {@link Workspace} to cached pending changes for (must not be
     *        <code>null</code>)
     */
    public PendingChangeCache(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
        changes = new PendingChangeCollection(workspace);
        coreEventListener = new CoreEventListener();
        listeners = new SingleListenerFacade(PendingChangeCacheListener.class);

        synchronized (operationDepthAndModificationLock) {
            operationDepth = 0;
        }

        attachCoreEvents();
    }

    /**
     * Refreshes the pending changes contained in this cache by querying the
     * server for the latest pending changes.
     */
    public void refresh() {
        PendingChange[] newPendingChanges = null;
        PendingSet pendingSet;

        try {
            pendingSet = workspace.getPendingChanges();
        } catch (final Exception e) {
            log.error("Error refreshing pending change cache", e); //$NON-NLS-1$
            return;
        }

        if (pendingSet != null) {
            newPendingChanges = pendingSet.getPendingChanges();
        }
        if (newPendingChanges == null) {
            newPendingChanges = new PendingChange[0];
        }

        synchronized (atomicOperationLock) {
            beginUpdatePendingChanges();
            try {
                clearPendingChanges();
                for (int i = 0; i < newPendingChanges.length; i++) {
                    addPendingChange(newPendingChanges[i], true);
                }
            } finally {
                endUpdatePendingChanges();
            }
        }
    }

    /**
     * Gets all of the pending changes currently held by this cache.
     *
     * @return the cached pending changes (never <code>null</code>)
     */
    public PendingChange[] getPendingChanges() {
        return changes.getValues();
    }

    /**
     * Gets the cached pending change that corresponds to the specified server
     * path, if any.
     *
     * @param serverPath
     *        the local path to identify a cached pending change with (must not
     *        be <code>null</code>)
     * @return the associated pending change, or <code>null</code> if no cached
     *         pending changes had the specified path
     */
    public PendingChange getPendingChangeByServerPath(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        return changes.getValueByServerPath(serverPath);
    }

    /**
     * Gets the cached rename pending change that corresponds to the specified
     * server path, if any.
     *
     * @param serverPath
     *        the original server path before the rename to identify a cached
     *        rename pending change with (must not be <code>null</code>)
     * @return the associated rename pending change, or <code>null</code> if no
     *         cached rename pending changes had the specified path
     */
    public PendingChange getRenamePendingChangeByServerPath(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$
        if (changes.getValues() != null) {
            for (final PendingChange change : changes.getValues()) {
                if (change.getSourceServerItem() != null && change.getSourceServerItem().equalsIgnoreCase(serverPath)) {
                    return change;
                }
            }
        }

        return null;
    }

    /**
     * Gets all of the cached pending changes that are at the specified server
     * path or have the specified server path as a parent path. Parent path here
     * means parent at any level up ($/tmp is considered a parent of
     * $/tmp/a/b/c).
     *
     * @param serverPath
     *        the parent path to identify cached pending changes with (must not
     *        be <code>null</code>)
     * @return all cached pending changes with the specified parent path (never
     *         <code>null</code>)
     */
    public PendingChange[] getPendingChangesByServerPathRecursive(final String serverPath) {
        Check.notNull(serverPath, "serverPath"); //$NON-NLS-1$

        return changes.getValuesByServerPathRecursive(serverPath);
    }

    /**
     * Gets the cached pending change that corresponds to the specified local
     * path, if any.
     *
     * @param localPath
     *        the local path to identify a cached pending change with (must not
     *        be <code>null</code>)
     * @return the associated pending change, or <code>null</code> if no cached
     *         pending changes had the specified path
     */
    public PendingChange getPendingChangeByLocalPath(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        return changes.getValueByLocalPath(localPath);
    }

    /**
     * Gets all of the cached pending changes that are at the specified local
     * path or have the specified local path as a parent path. Parent path here
     * means parent at any level up (/tmp is considered a parent of /tmp/a/b/c).
     *
     * @param localPath
     *        the parent path to identify cached pending changes with (must not
     *        be <code>null</code>)
     * @return all cached pending changes with the specified parent path (never
     *         <code>null</code>)
     */
    public PendingChange[] getPendingChangesByLocalPathRecursive(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        return changes.getValuesByLocalPathRecursive(localPath);
    }

    /**
     * Tests whether there are any cached pending changes that are at the
     * specified local path or have the specified local path as a parent path.
     * Parent path here means parent at any level up (/tmp is considered a
     * parent of /tmp/a/b/c).
     * <p>
     * This method is faster than
     * {@link #getPendingChangesByLocalPathRecursive(String)} when there are
     * many matching changes, because it does not have to convert and return an
     * array.
     *
     * @param localPath
     *        the parent path to identify cached pending changes with (must not
     *        be <code>null</code>)
     * @return <code>true</code> if the given path has pending changes on it or
     *         on its children, false if there are no pending changes on it or
     *         its children
     */
    public boolean hasPendingChangesByLocalPathRecursive(final String localPath) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$

        return changes.hasValuesByLocalPathRecursive(localPath);
    }

    /**
     * Adds a {@link PendingChangeCacheListener} to this cache.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addListener(final PendingChangeCacheListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Removes a previously added {@link PendingChangeCacheListener} from this
     * cache.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeListener(final PendingChangeCacheListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Must be called when this {@link PendingChangeCache} is no longer needed.
     * Calling this method removes internal listeners that this cache attaches
     * to Core objects. After calling this method, no further use of this
     * {@link PendingChangeCache} should be attempted.
     */
    public void dispose() {
        detachCoreEvents();
    }

    /**
     * Adds a new pending change to the cache. If <code>forRefill</code> is
     * <code>true</code>, no attempt is made to remove any old pending change
     * with the same item item path. Either a added or modified event is fired,
     * depending on whether there was an old pending change.
     *
     * @param newPendingChange
     *        the new change to add
     * @param forRefill
     *        <code>true</code> to skip checks for old pending changes with the
     *        same item ID
     */
    private void addPendingChange(final PendingChange newPendingChange, final boolean forRefill) {
        final PendingChange oldPendingChange = changes.add(newPendingChange, forRefill);

        final PendingChangeCacheEvent event = new PendingChangeCacheEvent(this, oldPendingChange, newPendingChange);

        synchronized (operationDepthAndModificationLock) {
            /*
             * Mark the change collection modified for the add and modify
             * scenarios.
             */
            changesModifiedInOperation = true;
        }

        if (oldPendingChange == null) {
            getListener().onPendingChangeAdded(event);
        } else {
            getListener().onPendingChangeModified(event);
        }
    }

    /**
     * Removes a pending change from the cache. This will fire a removed event
     * if there was an existing pending change with the same item ID as the
     * argument.
     *
     * @param changeToRemove
     *        the pendingn change to remove (by item ID)
     */
    private void removePendingChange(final PendingChange changeToRemove) {
        final PendingChange removedChange = changes.remove(changeToRemove);

        if (removedChange == null) {
            return;
        }

        synchronized (operationDepthAndModificationLock) {
            changesModifiedInOperation = true;
        }

        final PendingChangeCacheEvent event = new PendingChangeCacheEvent(this, removedChange);
        getListener().onPendingChangeRemoved(event);
    }

    /**
     * Clears all pending changes from this cache. A clear event is fired.
     */
    private void clearPendingChanges() {
        final int oldSize = changes.size();

        changes.clear();

        synchronized (operationDepthAndModificationLock) {
            if (oldSize > 0) {
                changesModifiedInOperation = true;
            }
        }

        final PendingChangeCacheEvent event = new PendingChangeCacheEvent(this);
        getListener().onPendingChangesCleared(event);
    }

    /**
     * Called to begin an update operation. The operation depth is incremented.
     * If the operation depth was 0 before calling this method, a before update
     * event is fired, and the change collection modification field is reset to
     * <code>false</code>.
     */
    private void beginUpdatePendingChanges() {
        boolean sendEvent;

        synchronized (operationDepthAndModificationLock) {
            sendEvent = (operationDepth == 0);

            /*
             * Only reset if we're the first event level.
             */
            if (operationDepth == 0) {
                changesModifiedInOperation = false;
            }

            ++operationDepth;
        }

        if (sendEvent) {
            final PendingChangeCacheEvent event = new PendingChangeCacheEvent(this);
            getListener().onBeforeUpdatePendingChanges(event);
        }
    }

    /**
     * Called to end an update operation. The operation depth is decremented. If
     * the operation depth is 0 after decrementing an after update event is
     * fired.
     */
    private void endUpdatePendingChanges() {
        boolean sendEvent;
        boolean modified;

        synchronized (operationDepthAndModificationLock) {
            --operationDepth;
            sendEvent = (operationDepth == 0);
            modified = changesModifiedInOperation;
        }

        if (sendEvent) {
            final PendingChangeCacheEvent event = new PendingChangeCacheEvent(this);
            getListener().onAfterUpdatePendingChanges(event, modified);
        }
    }

    /**
     * @return the "single listener" facade for all attached
     *         {@link PendingChangeCacheListener}s
     */
    private PendingChangeCacheListener getListener() {
        return (PendingChangeCacheListener) listeners.getListener();
    }

    /**
     * Attaches listeners to Core's event engine.
     */
    private void attachCoreEvents() {
        final VersionControlEventEngine eventEngine = workspace.getClient().getEventEngine();

        eventEngine.addNewPendingChangeListener(coreEventListener);
        eventEngine.addUndonePendingChangeListener(coreEventListener);
        eventEngine.addMergingListener(coreEventListener);
        eventEngine.addCheckinListener(coreEventListener);
        eventEngine.addOperationStartedListener(coreEventListener);
        eventEngine.addOperationCompletedListener(coreEventListener);
        eventEngine.addPendingChangesChangedListener(coreEventListener);
        eventEngine.addFolderContentChangedListener(coreEventListener);
        eventEngine.addGetCompletedListener(coreEventListener);
        eventEngine.addLocalWorkspaceScanListener(coreEventListener);
    }

    /**
     * Detaches listeners from Core's event engine.
     */
    private void detachCoreEvents() {
        final VersionControlEventEngine eventEngine = workspace.getClient().getEventEngine();

        eventEngine.removeNewPendingChangeListener(coreEventListener);
        eventEngine.removeUndonePendingChangeListener(coreEventListener);
        eventEngine.removeMergingListener(coreEventListener);
        eventEngine.removeCheckinListener(coreEventListener);
        eventEngine.removeOperationStartedListener(coreEventListener);
        eventEngine.removeOperationCompletedListener(coreEventListener);
        eventEngine.removePendingChangesChangedListener(coreEventListener);
        eventEngine.removeFolderContentChangedListener(coreEventListener);
        eventEngine.removeGetCompletedListener(coreEventListener);
        eventEngine.removeLocalWorkspaceScanListener(coreEventListener);
    }

    /**
     * Tests whether the argument is not <code>null</code> and is the same as
     * the workspace associated with this cache.
     *
     * @param eventWorkspace
     *        the workspace to test
     * @return <code>true</code> if the workspace matches and should be
     *         processed
     */
    private boolean shouldProcessWorkspace(final Workspace eventWorkspace) {
        return eventWorkspace != null && workspace.equals(eventWorkspace);
    }

    /**
     * Handles the new pending change event from Core. If the event has a
     * different workspace than this cache, or if the pending change's ID is 0,
     * the event is ignored. Otherwise, either
     * {@link #removePendingChange(PendingChange)} or
     * {@link #addPendingChange(PendingChange, boolean)} is called (depending on
     * the change type of the pending change).
     */
    private void coreEventOnNewPendingChange(final PendingChangeEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        final PendingChange newPendingChange = e.getPendingChange();

        if (newPendingChange.getPendingChangeID() == 0) {
            return;
        }

        synchronized (atomicOperationLock) {
            if (ChangeType.NONE.equals(newPendingChange.getChangeType())) {
                removePendingChange(newPendingChange);
            } else {
                addPendingChange(newPendingChange, false);
            }
        }
    }

    /**
     * Handles the undone pending change event from Core. If the event has a
     * different workspace than this cache, or if the pending change's ID is 0,
     * the event is ignored. Otherwise, the
     * {@link #removePendingChange(PendingChange)} method is called.
     */
    private void coreEventOnUndonePendingChange(final PendingChangeEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        final PendingChange undonePendingChange = e.getPendingChange();

        if (undonePendingChange.getPendingChangeID() == 0) {
            return;
        }

        synchronized (atomicOperationLock) {
            removePendingChange(undonePendingChange);
        }
    }

    /**
     * Handles the merging event from Core. If the event has a different
     * workspace than this cache, or if the pending change is null or has ID 0,
     * the event is ignored. Otherwise, the
     * {@link #addPendingChange(PendingChange, boolean)} method is called.
     */
    private void coreEventOnMerging(final MergingEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        final PendingChange newPendingChange = e.getPendingChange();

        if (newPendingChange == null) {
            return;
        }

        if (newPendingChange.getPendingChangeID() == 0) {
            return;
        }

        synchronized (atomicOperationLock) {
            addPendingChange(newPendingChange, false);
        }
    }

    /**
     * Handles the checkin event from Core. If the event has a different
     * workspace than this cache it is ignored. Otherwise, all of the pending
     * changes contained in the event are removed from the cache. This happens
     * one of two ways. If the number of pending changes in the event is more
     * than half of the number of changes in the cache, we call
     * {@link #clearPendingChanges()} to empty the cache and then
     * {@link #addPendingChange(PendingChange, boolean)} for each previous
     * change that was not in the event. Otherwise, we call
     * {@link #removePendingChange(PendingChange)} for each change in the event.
     */
    private void coreEventOnCheckin(final CheckinEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        /*
         * Combine committed and pending changes into a single list.
         */
        final List<PendingChange> changesToRemove = new ArrayList<PendingChange>();
        changesToRemove.addAll(Arrays.asList(e.getUndoneChanges()));
        changesToRemove.addAll(Arrays.asList(e.getCommittedChanges()));

        if (changesToRemove.size() == 0) {
            return;
        }

        synchronized (atomicOperationLock) {
            beginUpdatePendingChanges();
            try {
                /*
                 * There was a brilliant premature optimization here that
                 * decided whether or not we should optimize this by clearing
                 * the cache and then adding new ones, rather than removing the
                 * ones that were simply checked in. Of course, this causes
                 * event stupidity, and listeners will never actually know what
                 * resources were affected.
                 */
                for (int i = 0; i < changesToRemove.size(); i++) {
                    removePendingChange(changesToRemove.get(i));
                }
            } finally {
                endUpdatePendingChanges();
            }
        }
    }

    /**
     * Handles the operation started event from Core. If the event has a
     * different workspace than this cache it is ignored. Otherwise,
     * {@link #beginUpdatePendingChanges()} is called.
     */
    private void coreEventOnOperationStarted(final OperationStartedEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        beginUpdatePendingChanges();
    }

    /**
     * Handles the operation completed event from Core. If the event has a
     * different workspace than this cache it is ignored. Otherwise,
     * {@link #endUpdatePendingChanges()} is called.
     */
    private void coreEventOnOperationCompleted(final OperationCompletedEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        try {
            /*
             * If the client has pended an encoding change, then we've gotten a
             * new pending change event from core. The core event includes a
             * PendingChange object that is synthesized from the GetOperation.
             * GetOperations do not include encodings, thus we've lost that
             * information. However, we do get an operation completed event that
             * contains the original request, so we can update that information.
             */
            if (e instanceof PendOperationCompletedEvent) {
                final ChangeRequest[] requests = ((PendOperationCompletedEvent) e).getRequests();

                for (int i = 0; i < requests.length; i++) {
                    if (requests[i].getRequestType() == RequestType.EDIT
                        && requests[i].getEncoding() != VersionControlConstants.ENCODING_UNCHANGED) {
                        PendingChange pendingChange = null;
                        final String queryPath = requests[i].getTargetItem() != null ? requests[i].getTargetItem()
                            : requests[i].getItemSpec().getItem();

                        if (ServerPath.isServerPath(queryPath)) {
                            pendingChange = changes.getValueByServerPath(queryPath);
                        } else {
                            pendingChange = changes.getValueByLocalPath(queryPath);
                        }

                        if (pendingChange != null
                            && pendingChange.getChangeType().contains(ChangeType.ENCODING)
                            && pendingChange.getEncoding() == VersionControlConstants.ENCODING_UNCHANGED) {
                            pendingChange.setEncoding(requests[i].getEncoding());
                        }
                    }
                }
            }
        } catch (final Exception exception) {
            log.error("Could not handle operation completed", exception); //$NON-NLS-1$
        }

        endUpdatePendingChanges();
    }

    public void coreEventOnPendingChangesChanged(final WorkspaceEvent e) {
        if (!shouldProcessWorkspace(e.getWorkspace())) {
            return;
        }

        /*
         * The onPendingChangesChanged event only notifies us that the table has
         * changed, it does not tell us what changed, we need to do a refresh.
         * Note that this event is only fired for local workspaces, so the
         * pending changes table is already up to date (and since we use a file
         * system watcher, no refresh is necessary when querying pending
         * changes.)
         */
        refresh();
    }

    /**
     * Handles a cross-process notification.
     */
    public void coreEventOnFolderContentChanged(final FolderContentChangedEvent e) {
        /*
         * The event is mainly to make SCE and pending changes refresh after a
         * "destroy" or "branch" which didn't create/undo pending changes.
         */
        refresh();
    }

    /**
     * Handles a cross-process notification.
     */
    public void coreEventOnGetCompleted(final WorkspaceEvent e) {
        /*
         * We're only interested in external events (IPC from other clients).
         */
        if (e.getWorkspaceSource() == WorkspaceEventSource.EXTERNAL) {
            refresh();
        }
    }

    /**
     * Handles a cross-process notification.
     */
    public void coreEventOnLocalWorkspaceScan(final WorkspaceEvent e) {
        /*
         * We're only interested in external events (IPC from other clients).
         */
        if (e.getWorkspaceSource() == WorkspaceEventSource.EXTERNAL) {
            refresh();
        }
    }

    /**
     * Implements all of the event interfaces from Core that we're interested
     * in. Each method is trivially implemented by delegating to one of the
     * <code>coreEvent*</code> methods in the {@link PendingChangeCache} class.
     */
    private class CoreEventListener
        implements NewPendingChangeListener, UndonePendingChangeListener, MergingListener, CheckinListener,
        OperationStartedListener, OperationCompletedListener, PendingChangesChangedListener,
        FolderContentChangedListener, GetCompletedListener, LocalWorkspaceScanListener {
        @Override
        public void onNewPendingChange(final PendingChangeEvent e) {
            coreEventOnNewPendingChange(e);
        }

        @Override
        public void onUndonePendingChange(final PendingChangeEvent e) {
            coreEventOnUndonePendingChange(e);
        }

        @Override
        public void onMerging(final MergingEvent e) {
            coreEventOnMerging(e);
        }

        @Override
        public void onCheckin(final CheckinEvent e) {
            coreEventOnCheckin(e);
        }

        @Override
        public void onOperationStarted(final OperationStartedEvent e) {
            coreEventOnOperationStarted(e);
        }

        @Override
        public void onOperationCompleted(final OperationCompletedEvent e) {
            coreEventOnOperationCompleted(e);
        }

        @Override
        public void onPendingChangesChanged(final WorkspaceEvent e) {
            coreEventOnPendingChangesChanged(e);
        }

        @Override
        public void onFolderContentChanged(final FolderContentChangedEvent e) {
            coreEventOnFolderContentChanged(e);
        }

        @Override
        public void onGetCompleted(final WorkspaceEvent e) {
            coreEventOnGetCompleted(e);
        }

        @Override
        public void onLocalWorkspaceScan(final WorkspaceEvent e) {
            coreEventOnLocalWorkspaceScan(e);
        }
    }
}
