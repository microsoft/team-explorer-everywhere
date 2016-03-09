// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalWorkspaceScanner;
import com.microsoft.tfs.util.listeners.ListenerCategory;
import com.microsoft.tfs.util.listeners.ListenerRunnable;
import com.microsoft.tfs.util.listeners.MultiListenerList;

/**
 * Coordinates listeners and fires events for {@link VersionControlClient}.
 * <p>
 * Events are processed synchronously: fire*() methods only return after each
 * registered event handler processes the event completely.
 * <p>
 * This class is thread-safe. fire*() methods do <b>not</b> invoke event
 * listener methods inside a synchronized block. This is done to prevent any
 * implementation of an event listener from calling back into this event engine
 * (through some other {@link VersionControlClient} method which would fire an
 * event). That might cause deadlock, and is avoided by not locking when
 * invoking listeners. An effect of this behavior is that an event listener
 * could be invoked a short time after it was removed by another thread.
 * <p>
 * Some core methods create new threads to perform work, and events may be fired
 * by these new threads. To help callers determine which call into core created
 * these events, each event is accompanied by an {@link EventSource} object
 * which identifies the original thread.
 * <p>
 * Event handlers that throw any {@link Throwable} will have the error logged by
 * the {@link VersionControlEventEngine} and the next handler will be invoked
 * normally. These errors will not be rethrown by the fire* methods.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class VersionControlEventEngine {
    private static final Log log = LogFactory.getLog(VersionControlEventEngine.class);

    private final MultiListenerList listeners = new MultiListenerList();

    /*
     * Categories for the multi-listener.
     */

    // Workspace events
    private static final ListenerCategory WORKSPACE_CREATED = new ListenerCategory(WorkspaceCreatedListener.class);
    private static final ListenerCategory WORKSPACE_UPDATED = new ListenerCategory(WorkspaceUpdatedListener.class);
    private static final ListenerCategory WORKSPACE_DELETED = new ListenerCategory(WorkspaceDeletedListener.class);

    // Error events
    private static final ListenerCategory NON_FATAL_ERROR = new ListenerCategory(NonFatalErrorListener.class);
    private static final ListenerCategory WORKSTATION_NON_FATAL_ERROR =
        new ListenerCategory(WorkstationNonFatalErrorListener.class);

    // Get events.
    private static final ListenerCategory GET = new ListenerCategory(GetListener.class);

    // Pending changes events
    private static final ListenerCategory NEW_PENDING_CHANGE = new ListenerCategory(NewPendingChangeListener.class);
    private static final ListenerCategory UNDONE_PENDING_CHANGE =
        new ListenerCategory(UndonePendingChangeListener.class);
    private static final ListenerCategory BEFORE_CHECKIN = new ListenerCategory(BeforeCheckinListener.class);
    private static final ListenerCategory CHECKIN = new ListenerCategory(CheckinListener.class);

    // Shelve changes events
    private static final ListenerCategory BEFORE_SHELVE = new ListenerCategory(BeforeShelveListener.class);
    private static final ListenerCategory SHELVE = new ListenerCategory(ShelveListener.class);

    // Conflict events
    private static final ListenerCategory CONFLICT = new ListenerCategory(ConflictListener.class);
    private static final ListenerCategory CONFLICT_RESOLVED = new ListenerCategory(ConflictResolvedListener.class);

    // Generic operation completion events
    private static final ListenerCategory OPERATION_STARTED = new ListenerCategory(OperationStartedListener.class);
    private static final ListenerCategory OPERATION_COMPLETED = new ListenerCategory(OperationCompletedListener.class);

    // Merge events
    private static final ListenerCategory MERGING = new ListenerCategory(MergingListener.class);

    // Destroy events
    private static final ListenerCategory DESTROY = new ListenerCategory(DestroyListener.class);

    // Branch objected created/updated events
    private static final ListenerCategory BRANCH_COMMITTED = new ListenerCategory(BranchCommittedListener.class);
    private static final ListenerCategory BRANCH_OBJECT_UPDATED =
        new ListenerCategory(BranchObjectUpdatedListener.class);

    // Local workspace scanner
    private static final ListenerCategory SCANNER_MODIFIED_FILES =
        new ListenerCategory(ScannerModifiedFilesListener.class);

    /*
     * Local versions of the cross-process notifications. These are redundant
     * with some of the events above, but they're used to signify a general
     * change to UI elements that don't care about details or from places which
     * cannot send conventional details (local workspace scanner).
     */
    private static final ListenerCategory PENDING_CHANGES_CHANGED =
        new ListenerCategory(PendingChangesChangedListener.class);
    private static final ListenerCategory PENDING_CHANGE_CANDIDATES_CHANGED =
        new ListenerCategory(PendingChangeCandidatesChangedListener.class);
    private static final ListenerCategory GET_COMPLETED = new ListenerCategory(GetCompletedListener.class);
    private static final ListenerCategory FOLDER_CONTENT_CHANGED =
        new ListenerCategory(FolderContentChangedListener.class);
    private static final ListenerCategory CHANGESET_RECONCILED =
        new ListenerCategory(ChangesetReconciledListener.class);
    private static final ListenerCategory WORKSPACE_CACHE_FILE_RELOADED =
        new ListenerCategory(WorkspaceCacheFileReloadedListener.class);
    private static final ListenerCategory LOCAL_WORKSPACE_SCAN = new ListenerCategory(LocalWorkspaceScanListener.class);

    public VersionControlEventEngine() {
        super();
    }

    /**
     * Fire an event when a workspace is created.
     */
    public void fireWorkspaceCreated(final WorkspaceEvent event) {
        listeners.getListenerList(WORKSPACE_CREATED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((WorkspaceCreatedListener) listener).onWorkspaceCreated(event);
                return true;
            }
        });
    }

    /**
     * Fire an event when a workspace is updated.
     */
    public void fireWorkspaceUpdated(final WorkspaceUpdatedEvent event) {
        listeners.getListenerList(WORKSPACE_UPDATED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((WorkspaceUpdatedListener) listener).onWorkspaceUpdated(event);
                return true;
            }
        });
    }

    /**
     * Fire an event when a workspace is deleted.
     */
    public void fireWorkspaceDeleted(final WorkspaceEvent event) {
        listeners.getListenerList(WORKSPACE_DELETED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((WorkspaceDeletedListener) listener).onWorkspaceDeleted(event);
                return true;
            }
        });
    }

    public void fireNonFatalError(final NonFatalErrorEvent event) {
        listeners.getListenerList(NON_FATAL_ERROR, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((NonFatalErrorListener) listener).onNonFatalError(event);
                return true;
            }
        });
    }

    public void fireWorkstationNonFatalError(final WorkstationNonFatalErrorEvent event) {
        listeners.getListenerList(WORKSTATION_NON_FATAL_ERROR, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((WorkstationNonFatalErrorListener) listener).onWorkstationNonFatalError(event);
                return true;
            }
        });
    }

    /**
     * Fire an event when a file is retrieved from the server.
     */
    public void fireGet(final GetEvent event) {
        listeners.getListenerList(GET, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((GetListener) listener).onGet(event);
                return true;
            }
        });
    }

    /**
     * Fire an event when a new change is pended.
     */
    public void fireNewPendingChange(final PendingChangeEvent event) {
        listeners.getListenerList(NEW_PENDING_CHANGE, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((NewPendingChangeListener) listener).onNewPendingChange(event);
                return true;
            }
        });
    }

    /**
     * Fire an event when a pending change is undone.
     */
    public void fireUndonePendingChange(final PendingChangeEvent event) {
        listeners.getListenerList(UNDONE_PENDING_CHANGE, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((UndonePendingChangeListener) listener).onUndonePendingChange(event);
                return true;
            }
        });
    }

    /**
     * Fire an event before a pending change is checked in.
     */
    public void fireBeforeCheckinPendingChange(final PendingChangeEvent event) {
        listeners.getListenerList(BEFORE_CHECKIN, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((BeforeCheckinListener) listener).onBeforeCheckin(event);
                return true;
            }
        });
    }

    /**
     * Fire an event before a pending change is shelved.
     */
    public void fireBeforeShelvePendingChange(final PendingChangeEvent event) {
        listeners.getListenerList(BEFORE_SHELVE, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((BeforeShelveListener) listener).onBeforeShelve(event);
                return true;
            }
        });
    }

    /**
     * Fire an event after a branch has been committed.
     */
    public void fireBranchCommitted(final BranchCommittedEvent event) {
        listeners.getListenerList(BRANCH_COMMITTED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((BranchCommittedListener) listener).onBranchCommitted(event);
                return true;
            }
        });
    }

    /**
     * Fire an event after a branch object has been created or modified.
     */
    public void fireBranchObjectUpdated(final BranchObjectUpdatedEvent event) {
        listeners.getListenerList(BRANCH_OBJECT_UPDATED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((BranchObjectUpdatedListener) listener).onBranchObjectUpdated(event);
                return true;
            }
        });
    }

    public void fireScannerModifiedFile(final ScannerModifiedFilesEvent event) {
        listeners.getListenerList(SCANNER_MODIFIED_FILES, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((ScannerModifiedFilesListener) listener).onScannerModifiedFiles(event);
                return true;
            }
        });
    }

    /**
     * Fire an event after a checkin has succeeded.
     */
    public void fireCheckin(final CheckinEvent event) {
        listeners.getListenerList(CHECKIN, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((CheckinListener) listener).onCheckin(event);
                return true;
            }
        });
    }

    /**
     * Fire an event after a shelve operation has succeeded.
     */
    public void fireShelve(final ShelveEvent event) {
        listeners.getListenerList(SHELVE, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((ShelveListener) listener).onShelve(event);
                return true;
            }
        });
    }

    /**
     * Fires an event that signals a conflict was detected.
     */
    public void fireConflict(final ConflictEvent event) {
        listeners.getListenerList(CONFLICT, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((ConflictListener) listener).onConflict(event);
                return true;
            }
        });
    }

    /**
     * Fires an event that signals a conflict has been resolved.
     */
    public void fireConflictResolved(final ConflictResolvedEvent event) {
        listeners.getListenerList(CONFLICT_RESOLVED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((ConflictResolvedListener) listener).onConflictResolved(event);
                return true;
            }
        });
    }

    /**
     * This "fire" method requires the caller construct its own event, because
     * OperationCompletedEvent is abstract and we must use derived classes.
     */
    public void fireOperationStarted(final OperationStartedEvent event) {
        log.debug("Firing event OPERATION_STARTED from " + event.getEventSource().getOriginatingThread().getName()); //$NON-NLS-1$
        listeners.getListenerList(OPERATION_STARTED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((OperationStartedListener) listener).onOperationStarted(event);
                return true;
            }
        });
    }

    /**
     * This "fire" method requires the caller construct its own event, because
     * OperationCompletedEvent is abstract and we must use derived classes.
     */
    public void fireOperationCompleted(final OperationCompletedEvent event) {
        log.debug("Firing event OPERATION_COMPLETED from " + event.getEventSource().getOriginatingThread().getName()); //$NON-NLS-1$

        listeners.getListenerList(OPERATION_COMPLETED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((OperationCompletedListener) listener).onOperationCompleted(event);
                return true;
            }
        });
    }

    /**
     * Fires an event that signals a file is being merged.
     */
    public void fireMerging(final MergingEvent event) {
        listeners.getListenerList(MERGING, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((MergingListener) listener).onMerging(event);
                return true;
            }
        });
    }

    public void fireDestroyEvent(final DestroyEvent event) {
        listeners.getListenerList(DESTROY, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((DestroyListener) listener).onDestroy(event);
                return true;
            }
        });
    }

    public void firePendingChangesChangedEvent(final WorkspaceEvent event) {
        listeners.getListenerList(PENDING_CHANGES_CHANGED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PendingChangesChangedListener) listener).onPendingChangesChanged(event);
                return true;
            }
        });
    }

    public void firePendingChangeCandidatesChangedEvent(final WorkspaceEvent event) {
        listeners.getListenerList(PENDING_CHANGE_CANDIDATES_CHANGED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((PendingChangeCandidatesChangedListener) listener).onPendingChangeCandidatesChanged(event);
                return true;
            }
        });
    }

    public void fireGetCompletedEvent(final WorkspaceEvent event) {
        listeners.getListenerList(GET_COMPLETED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((GetCompletedListener) listener).onGetCompleted(event);
                return true;
            }
        });
    }

    public void fireFolderContentChangedEvent(final FolderContentChangedEvent event) {
        listeners.getListenerList(FOLDER_CONTENT_CHANGED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((FolderContentChangedListener) listener).onFolderContentChanged(event);
                return true;
            }
        });
    }

    public void fireChangesetReconciledEvent(final ChangesetReconciledEvent event) {
        listeners.getListenerList(CHANGESET_RECONCILED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((ChangesetReconciledListener) listener).onChangesetReconciled(event);
                return true;
            }
        });
    }

    public void fireWorkspaceCacheFileReloaded(final WorkspaceCacheFileReloadedEvent event) {
        listeners.getListenerList(WORKSPACE_CACHE_FILE_RELOADED, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((WorkspaceCacheFileReloadedListener) listener).onWorkspaceCacheFileReloaded(event);
                return true;
            }
        });
    }

    public void fireLocalWorkspaceScanEvent(final WorkspaceEvent event) {
        listeners.getListenerList(LOCAL_WORKSPACE_SCAN, true).foreachListener(new ListenerRunnable() {
            @Override
            public boolean run(final Object listener) throws Exception {
                ((LocalWorkspaceScanListener) listener).onLocalWorkspaceScan(event);
                return true;
            }
        });
    }

    /*
     * Workspace events.
     */

    /**
     * Add a listener for the event fired when a workspace is created.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addWorkspaceCreatedListener(final WorkspaceCreatedListener listener) {
        listeners.addListener(listener, WORKSPACE_CREATED);
    }

    /**
     * Remove a listener for the event fired when a workspace is created.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeWorkspaceCreatedListener(final WorkspaceCreatedListener listener) {
        listeners.removeListener(listener, WORKSPACE_CREATED);
    }

    /**
     * Add a listener for the event fired when a workspace is updated (meaning
     * the workspace data or the working folder mappings are modified.)
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addWorkspaceUpdatedListener(final WorkspaceUpdatedListener listener) {
        listeners.addListener(listener, WORKSPACE_UPDATED);

    }

    /**
     * Remove a listener for the event fired when a workspace is updated
     * (meaning the workspace data or the working folder mappings are modified.)
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeWorkspaceUpdatedListener(final WorkspaceUpdatedListener listener) {
        listeners.removeListener(listener, WORKSPACE_UPDATED);
    }

    /**
     * Add a listener for the event fired when a workspace is deleted.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addWorkspaceDeletedListener(final WorkspaceDeletedListener listener) {
        listeners.addListener(listener, WORKSPACE_DELETED);
    }

    /**
     * Remove a listener for the event fired when a workspace is removed.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeWorkspaceDeletedListener(final WorkspaceDeletedListener listener) {
        listeners.removeListener(listener, WORKSPACE_DELETED);
    }

    /*
     * Error events.
     */

    /**
     * Add a listener for the event fired when a non-fatal error is encountered.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addNonFatalErrorListener(final NonFatalErrorListener listener) {
        listeners.addListener(listener, NON_FATAL_ERROR);
    }

    /**
     * Remove a listener for the event fired when a non-fatal error is
     * encountered.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeNonFatalErrorListener(final NonFatalErrorListener listener) {
        listeners.removeListener(listener, NON_FATAL_ERROR);
    }

    /**
     * Add a listener for the event fired when a non-fatal error is encountered
     * processing the workstation's cache files.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addWorkstationNonFatalErrorListener(final WorkstationNonFatalErrorListener listener) {
        listeners.addListener(listener, WORKSTATION_NON_FATAL_ERROR);
    }

    /**
     * Remove a listener for the event fired when a non-fatal error is
     * encountered processing the workstation's cache files.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeWorkstationNonFatalErrorListener(final WorkstationNonFatalErrorListener listener) {
        listeners.removeListener(listener, WORKSTATION_NON_FATAL_ERROR);
    }

    /*
     * Get events.
     */

    /**
     * Add a listener for the event fired when a file is retrieved.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addGetListener(final GetListener listener) {
        listeners.addListener(listener, GET);
    }

    /**
     * Remove a listener for the event fired when a file is retrieved.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeGetListener(final GetListener listener) {
        listeners.removeListener(listener, GET);
    }

    /*
     * Pending changes events.
     */

    /**
     * Add a listener for the event fired when a change is pended.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addNewPendingChangeListener(final NewPendingChangeListener listener) {
        listeners.addListener(listener, NEW_PENDING_CHANGE);
    }

    /**
     * Remove a listener for the event fired when a change is pended.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeNewPendingChangeListener(final NewPendingChangeListener listener) {
        listeners.removeListener(listener, NEW_PENDING_CHANGE);
    }

    /**
     * Add a listener for the event fired when a pending change is undone.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addUndonePendingChangeListener(final UndonePendingChangeListener listener) {
        listeners.addListener(listener, UNDONE_PENDING_CHANGE);
    }

    /**
     * Remove a listener for the event fired when a pending change is undone.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeUndonePendingChangeListener(final UndonePendingChangeListener listener) {
        listeners.removeListener(listener, UNDONE_PENDING_CHANGE);
    }

    /**
     * Add a listener for the event fired before a pending change is checked in.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addBeforeCheckinListener(final BeforeCheckinListener listener) {
        listeners.addListener(listener, BEFORE_CHECKIN);
    }

    /**
     * Remove a listener for the event fired before a pending change is checked
     * in.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeBeforeCheckinListener(final BeforeCheckinListener listener) {
        listeners.removeListener(listener, BEFORE_CHECKIN);
    }

    /**
     * Add a listener for the event fired before a pending change is shelved.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addBeforeShelveListener(final BeforeShelveListener listener) {
        listeners.addListener(listener, BEFORE_SHELVE);
    }

    /**
     * Remove a listener for the event fired before a pending change is shelved.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeBeforeShelveListener(final BeforeShelveListener listener) {
        listeners.removeListener(listener, BEFORE_SHELVE);
    }

    /**
     * Add a listener for the event fired when a checkin has completed
     * successfully.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addCheckinListener(final CheckinListener listener) {
        listeners.addListener(listener, CHECKIN);
    }

    /**
     * Remove a listener for the event when a checkin has completed
     * successfully.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeCheckinListener(final CheckinListener listener) {
        listeners.removeListener(listener, CHECKIN);
    }

    /**
     * Add a listener for the event fired when a shelve operation has completed.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addShelveListener(final ShelveListener listener) {
        listeners.addListener(listener, SHELVE);
    }

    /**
     * Remove a listener for the event when a cshelve operation has completed.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeShelveListener(final ShelveListener listener) {
        listeners.removeListener(listener, SHELVE);
    }

    /**
     * Add a listener for the event fired when a client operation is started.
     * These operations are like Get, Undo, and Pend. The event may not be fired
     * if the operation did not do any work (for example, the input array to
     * undo is empty and the function returns early).
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addOperationStartedListener(final OperationStartedListener listener) {
        listeners.addListener(listener, OPERATION_STARTED);
    }

    /**
     * Remove a listener for the event fired when a client operation is started.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeOperationStartedListener(final OperationStartedListener listener) {
        listeners.removeListener(listener, OPERATION_STARTED);
    }

    /**
     * Add a listener for the event fired when a client operation is completed.
     * These operations are like Get, Undo, and Pend. The event may not be fired
     * if the operation did not do any work (for example, the input array to
     * undo is empty and the function returns early).
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addOperationCompletedListener(final OperationCompletedListener listener) {
        listeners.addListener(listener, OPERATION_COMPLETED);
    }

    /**
     * Remove a listener for the event fired when a client operation is
     * completed.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeOperationCompletedListener(final OperationCompletedListener listener) {
        listeners.removeListener(listener, OPERATION_COMPLETED);
    }

    /*
     * Conflict events.
     */

    /**
     * Add a listener for the event fired when a conflict is discovered.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addConflictListener(final ConflictListener listener) {
        listeners.addListener(listener, CONFLICT);
    }

    /**
     * Remove a listener for the event fired when a conflict is discovered.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeConflictListener(final ConflictListener listener) {
        listeners.removeListener(listener, CONFLICT);
    }

    /**
     * Add a listener for the event fired when a conflict is resolved.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addConflictResolvedListener(final ConflictResolvedListener listener) {
        listeners.addListener(listener, CONFLICT_RESOLVED);
    }

    /**
     * Remove a listener for the event fired when a conflict is resolved.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeConflictResolvedListener(final ConflictResolvedListener listener) {
        listeners.removeListener(listener, CONFLICT_RESOLVED);
    }

    /*
     * Merge events.
     */

    /**
     * Add a listener for the event fired when a file is being merged.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addMergingListener(final MergingListener listener) {
        listeners.addListener(listener, MERGING);
    }

    /**
     * Remove a listener for the event fired when a file is being merged.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeMergingListener(final MergingListener listener) {
        listeners.removeListener(listener, MERGING);
    }

    /**
     * Adds a listener for the event fired when an item is destroyed.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addDestroyListener(final DestroyListener listener) {
        listeners.addListener(listener, DESTROY);
    }

    /**
     * Removes a listener for the event fired when an item is destroyed.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeDestroyListener(final DestroyListener listener) {
        listeners.removeListener(listener, DESTROY);
    }

    /*
     * Branch object created / updated events
     */

    /**
     * Adds a listener for the event fired when branches are committed.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addBranchCommittedListener(final BranchCommittedListener listener) {
        listeners.addListener(listener, BRANCH_COMMITTED);
    }

    /**
     * Adds a listener for the event fired when branches are committed.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeBranchCommittedListener(final BranchCommittedListener listener) {
        listeners.removeListener(listener, BRANCH_COMMITTED);
    }

    /**
     * Adds a listener for the event fired when a branch object is created or
     * modified.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addBranchObjectUpdatedListener(final BranchObjectUpdatedListener listener) {
        listeners.addListener(listener, BRANCH_OBJECT_UPDATED);
    }

    /**
     * Removes a listener for the event fired when a branch object is created or
     * modified.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeBranchObjectUpdatedListener(final BranchObjectUpdatedListener listener) {
        listeners.removeListener(listener, BRANCH_OBJECT_UPDATED);
    }

    /**
     * Adds a listener for the event fired when the
     * {@link LocalWorkspaceScanner} modifies on-disk file information during a
     * scan.
     *
     * @param listener
     *        athe listener to add (must not be <code>null</code>)
     */
    public void addScannerModifiedFilesListener(final ScannerModifiedFilesListener listener) {
        listeners.addListener(listener, SCANNER_MODIFIED_FILES);
    }

    /**
     * Removes a listener for the event fired when the
     * {@link LocalWorkspaceScanner} modifies on-disk file information during a
     * scan.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeScannerModifiedFilesListener(final ScannerModifiedFilesListener listener) {
        listeners.removeListener(listener, SCANNER_MODIFIED_FILES);
    }

    /*
     * Local verisons of the cross-process events
     */

    /**
     * Add a listener for the event fired when the pending changes list changes.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addPendingChangesChangedListener(final PendingChangesChangedListener listener) {
        listeners.addListener(listener, PENDING_CHANGES_CHANGED);
    }

    /**
     * Remove a listener for the event fired when a workspace is created.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removePendingChangesChangedListener(final PendingChangesChangedListener listener) {
        listeners.removeListener(listener, PENDING_CHANGES_CHANGED);
    }

    /**
     * Add a listener for the event fired when a pending change candidates list
     * changes.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addPendingChangeCandidatesChangedListener(final PendingChangeCandidatesChangedListener listener) {
        listeners.addListener(listener, PENDING_CHANGE_CANDIDATES_CHANGED);
    }

    /**
     * Remove a listener for the event fired when a pending change candidates
     * list changes.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removePendingChangeCandidatesChangedListener(final PendingChangeCandidatesChangedListener listener) {
        listeners.removeListener(listener, PENDING_CHANGE_CANDIDATES_CHANGED);
    }

    /**
     * Add a listener for the event fired when a get operation completes.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addGetCompletedListener(final GetCompletedListener listener) {
        listeners.addListener(listener, GET_COMPLETED);
    }

    /**
     * Remove a listener for the event fired when a get operation completes.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeGetCompletedListener(final GetCompletedListener listener) {
        listeners.removeListener(listener, GET_COMPLETED);
    }

    /**
     * Add a listener for the event fired when server folder content is changed
     * without pending changes. Examples are creating committed branch and
     * destroying item.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addFolderContentChangedListener(final FolderContentChangedListener listener) {
        listeners.addListener(listener, FOLDER_CONTENT_CHANGED);
    }

    /**
     * Remove a listener for the event fired when server folder content is
     * changed without pending changes.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeFolderContentChangedListener(final FolderContentChangedListener listener) {
        listeners.removeListener(listener, FOLDER_CONTENT_CHANGED);
    }

    /**
     * Add a listener for the event fired when pending changes are reconciled
     * with a checked-in changeset.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addChangesetReconciledListener(final ChangesetReconciledListener listener) {
        listeners.addListener(listener, CHANGESET_RECONCILED);
    }

    /**
     * Remove a listener for the event fired when pending changes are reconciled
     * with a checked-in changeset.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeChangesetReconciledListener(final ChangesetReconciledListener listener) {
        listeners.removeListener(listener, CHANGESET_RECONCILED);
    }

    /**
     * Add a listener for the event fired when the {@link Workstation}'s
     * workspace cache is reloaded.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addWorkspaceCacheFileReloadedListener(final WorkspaceCacheFileReloadedListener listener) {
        listeners.addListener(listener, WORKSPACE_CACHE_FILE_RELOADED);
    }

    /**
     * Remove a listener for the event fired when the {@link Workstation}'s
     * workspace cache is reloaded.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeWorkspaceCacheFileReloadedListener(final WorkspaceCacheFileReloadedListener listener) {
        listeners.removeListener(listener, WORKSPACE_CACHE_FILE_RELOADED);
    }

    /**
     * Add a listener for the event fired when a change was detected by a local
     * workspace scanner.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addLocalWorkspaceScanListener(final LocalWorkspaceScanListener listener) {
        listeners.addListener(listener, LOCAL_WORKSPACE_SCAN);
    }

    /**
     * Removes a listener for the event fired when a change was detected by a
     * local workspace scanner.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeLocalWorkspaceScanListener(final LocalWorkspaceScanListener listener) {
        listeners.removeListener(listener, LOCAL_WORKSPACE_SCAN);
    }

    /**
     * Removes all listeners.
     */
    public void clear() {
        listeners.clear();
    }
}
