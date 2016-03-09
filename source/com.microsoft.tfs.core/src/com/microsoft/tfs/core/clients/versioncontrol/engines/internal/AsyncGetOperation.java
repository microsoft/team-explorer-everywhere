// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.ILocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.InitiallyDeletedLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.MoveUncommittedLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueue;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueueOptions;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Contains state information used by the {@link GetEngine} class to track its
 * progress and errors. Holds similar information as VS's AsyncGetOperation, but
 * is not actually a schedulable/callable unit.
 *
 * @threadsafety thread-safe
 */
public final class AsyncGetOperation extends AsyncOperation implements Closeable {
    private final ProcessType type;
    private final GetOptions options;
    private final GetStatus getStatus;
    private final Workspace workspace;
    private final WorkspaceLock workspaceLock;
    private final boolean deleteUndoneAdds;
    private final ChangePendedFlags flags;
    private final AccountingCompletionService<WorkerStatus> completionService;

    /**
     * A lookup table of all of the existing local paths we are affecting.
     */
    private final Map<String, GetOperation> existingLocalHash =
        Collections.synchronizedMap(new TreeMap<String, GetOperation>(LocalPath.TOP_DOWN_COMPARATOR));

    /**
     * This is the list of folders that should not be deleted at the end of the
     * get.
     */
    private final Map<String, GetOperation> dontDeleteFolderHash =
        Collections.synchronizedMap(new TreeMap<String, GetOperation>(LocalPath.TOP_DOWN_COMPARATOR));

    /**
     * A list of deletes. This list is sorted bottom up so children get deleted
     * before parents.
     */
    private final SortedMap<String, GetOperation> deletes =
        Collections.synchronizedSortedMap(new TreeMap<String, GetOperation>(LocalPath.BOTTOM_UP_COMPARATOR));

    private final UpdateLocalVersionQueue localUpdateQueue;
    private final UpdateLocalVersionQueueOptions localUpdateOptions;

    private final List<RetryEntry> retryList = Collections.synchronizedList(new ArrayList<RetryEntry>());

    // Operation stats for use in the events.
    public volatile int currentNumOperations;
    public volatile int totalNumOperations;

    public AsyncGetOperation(
        final Workspace workspace,
        final ProcessType type,
        final RequestType requestType,
        final GetOptions options,
        final boolean deleteUndoneAdds,
        final WorkspaceLock wLock,
        final UpdateLocalVersionQueueOptions localUpdateOptions,
        final ChangePendedFlags flags,
        final AccountingCompletionService<WorkerStatus> completionService) {
        super();

        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNull(requestType, "requestType"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNull(localUpdateOptions, "localUpdateOptions"); //$NON-NLS-1$
        Check.notNull(flags, "flags"); //$NON-NLS-1$
        // wLock may be null
        Check.notNull(completionService, "completionService"); //$NON-NLS-1$

        this.type = type;
        this.getStatus = new GetStatus();
        this.workspace = workspace;
        this.options = options;
        this.localUpdateOptions = localUpdateOptions;

        // Create object to handle deferred QueueUpdate() calls
        this.localUpdateQueue = new UpdateLocalVersionQueue(workspace, localUpdateOptions, wLock);

        // Workspace lock which was taken by ProcessGetOperations for the entire
        // Get
        this.workspaceLock = wLock;

        this.deleteUndoneAdds = deleteUndoneAdds;
        this.flags = flags;
        this.completionService = completionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (localUpdateQueue != null) {
            localUpdateQueue.close();
        }
    }

    public ProcessType getType() {
        return type;
    }

    public GetOptions getOptions() {
        return options;
    }

    public GetStatus getStatus() {
        return getStatus;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public UpdateLocalVersionQueueOptions getLocalUpdateOptions() {
        return localUpdateOptions;
    }

    public Map<String, GetOperation> getExistingLocalHash() {
        return existingLocalHash;
    }

    public Map<String, GetOperation> getDontDeleteFolderHash() {
        return dontDeleteFolderHash;
    }

    public SortedMap<String, GetOperation> getDeletes() {
        return deletes;
    }

    public List<RetryEntry> getRetryList() {
        return retryList;
    }

    /**
     * Intelligently queue an update to tell the server the local and version of
     * the local file.
     *
     * @param operation
     *        the {@link GetOperation} so that the new path and version can be
     *        compared to the original path and version
     * @param targetLocalPath
     *        The local path where the item now resides; null if deleted
     * @param version
     *        the version of the local file
     * @throws IllegalStateException
     *         if this engine was constructed with a null workspace.
     **/
    public void queueLocalVersionUpdate(final GetOperation operation, final String targetLocalPath, final int version)
        throws IllegalStateException {
        queueLocalVersionUpdate(operation, targetLocalPath, version, false);
    }

    /**
     * Intelligently queue an update to tell the server the local and version of
     * the local file.
     *
     * @param operation
     *        the {@link GetOperation} so that the new path and version can be
     *        compared to the original path and version
     * @param targetLocalItem
     *        The local path where the item now resides; null if deleted
     * @param version
     *        the version of the local file
     * @param force
     *        if <code>true</code> the source and target item path comparison is
     *        skipped and an update it always sent to the server
     * @throws IllegalStateException
     *         if this engine was constructed with a null workspace.
     **/
    public void queueLocalVersionUpdate(
        final GetOperation operation,
        final String targetLocalItem,
        final int version,
        final boolean force) throws IllegalStateException {
        /*
         * Do a case-sensitive, exact match here to handle case-changing moves.
         * If all of the information that we would send to the server matches
         * what it already has, there's no need to queue an update. This
         * optimization removes the calls for edits, undoing edits where the
         * revert-to is the current version, etc. assuming the files didn't get
         * re-mapped.
         *
         * Note that we use SourceLocalItem instead of CurrentLocalItem to
         * prevent a race condition where CurrentLocalItem could be cleared,
         * leaving a (null, null) comparsion that would evaluate to true.
         *
         * If force is set to true, an update is queued no matter what.
         * Currently this is set whenever content is actually downloaded and
         * placed on disk. (get /all, for example)
         */

        final String sourceLocalPath = operation.getSourceLocalItem();
        if (force
            || version != operation.getVersionLocal()
            || (targetLocalItem == null && sourceLocalPath != null)
            || (targetLocalItem != null && targetLocalItem.equals(operation.getSourceLocalItem()) == false)) {
            final boolean keepLocalVersionRowOnDelete =
                !operation.isUndo() && !operation.getChangeType().equals(ChangeType.NONE);

            if (keepLocalVersionRowOnDelete
                && null == operation.getSourceLocalItem()
                && null == operation.getTargetLocalItem()) {
                // Queue an update which puts the item into the workspace in the
                // deleted state
                // and downloads the baseline for the item. An example of an
                // update taking this path
                // would be:
                // 1. Add file.txt; checkin
                // 2. Get the version before file.txt was committed (removing it
                // from your workspace)
                // 3. Rollback the changeset from step 1; this pends a
                // "delete, rollback"
                // We want the LV entry for x to be present so the pending
                // rollback can be undone offline.
                localUpdateQueue.queueUpdate(
                    new InitiallyDeletedLocalVersionUpdate(
                        operation.getSourceServerItem(),
                        operation.getItemID(),
                        operation.getVersionServer(),
                        operation.getVersionServerDate(),
                        operation.getEncoding(),
                        operation.getTargetServerItem()));
            } else {
                if (0 == version
                    && 0 == operation.getVersionLocal()
                    && null != operation.getSourceLocalItem()
                    && !LocalPath.equals(operation.getSourceLocalItem(), operation.getTargetLocalItem())) {
                    // An uncommitted item is being moved in the local version
                    // table, which means that its source server item may have
                    // changed. The main update queued with QueueUpdate below
                    // won't be able to find the existing local version entry
                    // since (ServerItem, IsCommitted) isn't the same. This
                    // 'pre-update' is issued to look up the existing local
                    // version entry by source local item and re-file it at the
                    // new location in the (ServerItem, IsCommitted) index.
                    localUpdateQueue.queueUpdate(
                        new MoveUncommittedLocalVersionUpdate(
                            operation.getSourceServerItem(),
                            operation.getSourceLocalItem()));
                }

                localUpdateQueue.queueUpdate(
                    new ClientLocalVersionUpdate(
                        operation.getSourceServerItem(),
                        operation.getItemID(),
                        targetLocalItem,
                        version,
                        operation.getEncoding(),
                        keepLocalVersionRowOnDelete,
                        operation.getPropertyValues()));
            }

            // We have apparently completed a successful operation.
            getStatus.incrementNumUpdated();
        }
    }

    public void queueLocalVersionUpdate(final ILocalVersionUpdate update) {
        Check.notNull(update, "update"); //$NON-NLS-1$

        localUpdateQueue.queueUpdate(update);

        // We have apparently completed a successful operation.
        getStatus.incrementNumUpdated();
    }

    /**
     * Resets the state that must be cleared between retries.
     */
    public void resetForRetry() {
        retryList.clear();
    }

    /**
     * Record a get conflict.
     */
    public void addConflict(final GetOperation action) {
        getStatus.incrementNumConflicts();
    }

    public void addWarning(final OperationStatus status, final GetOperation op) {
        addWarning(status, op, null);
    }

    /**
     * Record a get warning for use with resolve except when the user has
     * specified Preview.
     */
    public void addWarning(final OperationStatus status, final GetOperation op, final GetOperation targetAction) {
        retryList.add(new RetryEntry(status, op, targetAction));
    }

    public boolean isPreview() {
        return options.contains(GetOptions.PREVIEW);
    }

    public boolean isGetAll() {
        return options.contains(GetOptions.GET_ALL);
    }

    public boolean isOverwrite() {
        return options.contains(GetOptions.OVERWRITE);
    }

    public boolean isNoDiskUpdate() {
        return options.contains(GetOptions.NO_DISK_UPDATE);
    }

    public boolean getDeleteUndoneAdds() {
        return deleteUndoneAdds;
    }

    public ChangePendedFlags getFlags() {
        return flags;
    }

    public AccountingCompletionService<WorkerStatus> getCompletionService() {
        return completionService;
    }

    public WorkspaceLock getWorkspaceLock() {
        return workspaceLock;
    }

    public BaselineFolderCollection getBaselineFolders() {
        if (null == workspaceLock) {
            return null;
        }

        return workspaceLock.getBaselineFolders();
    }

    /**
     * Exposed so {@link GetEngine} can flush and close the update queue when
     * its done running operations.
     */
    public UpdateLocalVersionQueue getLocalUpdateQueue() {
        return localUpdateQueue;
    }
}
