// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.AllTablesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFileGUIDComparer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineRequest;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalDataAccessLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalPendingChangesTable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalVersionTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspacePropertiesLocalVersionTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspacePropertiesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Closable;

/**
 * <p>
 * Class for handling deferred batch updates of the (server's) LocalVersion
 * table. Holds up to N requests for a maximum of M seconds. We use time rather
 * than byte counts because it more closely models the user's maximum threshold
 * of pain, if the operation were to be cancelled. In effect, it is the byte
 * count adjusted for bandwidth.
 * </p>
 * <p>
 * You must call {@link #close()} when finished with an instance.
 * </p>
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-11.0
 */
public class UpdateLocalVersionQueue implements Closable {
    private static final Log log = LogFactory.getLog(UpdateLocalVersionQueue.class);

    /**
     * Maximum number of pending updates that are allowed. Whenever this number
     * is exceeded, updates are immediately flushed to the server
     */
    private static final int COUNT_THRESHOLD = 200;

    /**
     * If not signaled, threads waiting on m_pendingUpdates should wake up at
     * this interval. They may need to take action (such as if the queue has
     * been disposed).
     */
    private static final int SLEEP_TIME_MILLISECONDS = 2 * 1000;

    private static final int DEFAULT_FLUSH_TRIGGER_LEVEL = 400;
    private static final int DEFAULT_MAXIMUM_LEVEL = 1600;
    private static final int DEFAULT_TIME_TRIGGER_MILLISECONDS = 15 * 1000;

    private final Workspace workspace;
    private final UpdateLocalVersionQueueOptions options;
    private final WorkspaceLock wLock;

    /**
     * Measures the time since the local version update with index 0 was added
     * to m_pendingUpdates.
     */
    private long timerMillis;

    /**
     * Used to ensure only one flush runs at a time.
     */
    private final Object flushLock = new Object();

    /**
     * Set when a thread is currently flushing.
     */
    private volatile boolean flushing;

    /**
     * Set to true when the object has been disposed
     */
    private boolean closed;

    /**
     * Collects local workspace baseline files that were displaced while
     * updating local versions for processing during {@link #close()}.
     *
     * Synchronized on this.
     */
    private Set<byte[]> persistedDisplacedBaselines;

    /**
     * The list of local version updates which have been written to the local
     * local version table with PendingReconcile = true and have also been
     * successfully flushed to the server. These need to be marked
     * PendingReconcile = false in the local local version table to complete the
     * 2-phase commit.
     */
    private List<ILocalVersionUpdate> pendingAcks;

    /**
     * Where pending updates are stored before being sent to the server.
     *
     * Synchronized on this.
     */
    private List<ILocalVersionUpdate> pendingUpdates = new ArrayList<ILocalVersionUpdate>(COUNT_THRESHOLD);

    /**
     * When m_pendingUpdates reaches this count, the thread which called
     * QueueUpdate will be selected to perform a flush. Other threads can
     * continue queueing updates until the count reaches m_maximumLevel.
     */
    private final int flushTriggerLevel;

    /**
     * The maximum number of local version updates to hold in m_pendingUpdates.
     * If the count in the List reaches this level, threads calling QueueUpdate
     * will block until the count drops. This only needs to be very slightly
     * higher than the value for m_flushTriggerLevel.
     */
    private final int maximumLevel;

    /**
     * If when queueing an update, it is discovered that the amount of time
     * which has elapsed since m_pendingUpdates[0] was queued has exceeded this
     * value, then the thread which called QueueUpdate will be selected to
     * perform a flush.
     */
    private final int timeTriggerInMilliseconds;

    /**
     * Constructs a queue for updating both the local workspace and server's
     * local version tables.
     *
     * @param workspace
     *        the workspace that will be updated (must not be <code>null</code>)
     */
    public UpdateLocalVersionQueue(final Workspace workspace) {
        this(workspace, UpdateLocalVersionQueueOptions.UPDATE_BOTH, null);
    }

    /**
     * Constructs a queue for updating one or both (according to options) of the
     * local workspace and server's local version tables.
     *
     * @param workspace
     *        the workspace that will be updated (must not be <code>null</code>)
     * @param options
     *        options that control which version tables get updated (must not be
     *        <code>null</code>)
     */
    public UpdateLocalVersionQueue(final Workspace workspace, final UpdateLocalVersionQueueOptions options) {
        this(workspace, options, null);
    }

    /**
     * Constructs a queue for updating the workspace's local version table.
     *
     *
     * @param workspace
     *        Workspace to queue local version updates for
     * @param options
     * @param wLock
     *        Which table locations to update (local, server, or both)
     */
    public UpdateLocalVersionQueue(
        final Workspace workspace,
        final UpdateLocalVersionQueueOptions options,
        final WorkspaceLock wLock) {
        this(
            workspace,
            options,
            wLock,
            DEFAULT_FLUSH_TRIGGER_LEVEL,
            DEFAULT_MAXIMUM_LEVEL,
            DEFAULT_TIME_TRIGGER_MILLISECONDS);
    }

    /**
     * Constructs a queue for updating the workspace's local version table. This
     * constructor is called from {@link GetEngine} to provide the workspace
     * lock which was opened for processGetOperations. (It may have been taken
     * on a different thread.)
     *
     * @param workspace
     *        the workspace that will be updated (must not be <code>null</code>)
     * @param options
     *        options that control which version tables get updated (must not be
     *        <code>null</code>)
     * @param wLock
     *        a {@link WorkspaceLock} that this class should do its work inside
     *        (if <code>null</code> this class creates its own lock on demand)
     */
    public UpdateLocalVersionQueue(
        final Workspace workspace,
        final UpdateLocalVersionQueueOptions options,
        final WorkspaceLock wLock,
        final int flushTriggerLevel,
        final int maximumLevel,
        final int timeTriggerInMilliseconds) {
        Check.isTrue(flushTriggerLevel < maximumLevel, "flushTriggerLevel < maximumLevel"); //$NON-NLS-1$
        Check.isTrue(timeTriggerInMilliseconds > 0, "timeTriggerInMilliseconds > 0"); //$NON-NLS-1$

        this.flushTriggerLevel = flushTriggerLevel;
        this.maximumLevel = maximumLevel;
        this.timeTriggerInMilliseconds = timeTriggerInMilliseconds;
        this.workspace = workspace;
        this.wLock = wLock;
        this.pendingUpdates = new ArrayList<ILocalVersionUpdate>(maximumLevel);
        this.options = options;

        if (options.contains(UpdateLocalVersionQueueOptions.UPDATE_LOCAL)
            && WorkspaceLocation.LOCAL == workspace.getLocation()) {
            if (options.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER)) {
                pendingAcks = new ArrayList<ILocalVersionUpdate>(maximumLevel);
            }

            persistedDisplacedBaselines = new TreeSet<byte[]>(new BaselineFileGUIDComparer());
        }
    }

    /**
     * Queue a request to tell the server the local disk location of an item in
     * the workspace.
     *
     *
     * @param sourceServerItem
     *        The committed server path of the item in the workspace, or the
     *        target server item if the item is uncommitted (pending add or
     *        branch)
     * @param itemId
     *        Item ID of the item in the workspace (optional; used for backwards
     *        compatibility with TFS 2010 and earlier servers)
     * @param targetLocalItem
     *        New local path of the item, or null to remove it from the
     *        workspace
     * @param localVersion
     *        The version of the item in the workspace. If zero, the request
     *        refers to the uncommitted slot for the item ID. If non-zero, the
     *        request refers to the committed slot for the item ID.
     */
    public void queueUpdate(
        final String sourceServerItem,
        final int itemId,
        final String targetLocalItem,
        final int localVersion,
        final PropertyValue[] properties) {
        queueUpdate(new ClientLocalVersionUpdate(sourceServerItem, itemId, targetLocalItem, localVersion, properties));
    }

    /**
     * Queue a request to tell the server the local disk location of an item in
     * the workspace.
     *
     *
     * @param sourceServerItem
     *        The committed server path of the item in the workspace, or the
     *        target server item if the item is uncommitted (pending add or
     *        branch)
     * @param itemId
     *        Item ID of the item in the workspace (optional; used for backwards
     *        compatibility with TFS 2010 and earlier servers)
     * @param targetLocalItem
     *        New local path of the item, or null to remove it from the
     *        workspace
     * @param localVersion
     *        The version of the item in the workspace. If zero, the request
     *        refers to the uncommitted slot for the item ID. If non-zero, the
     *        request refers to the committed slot for the item ID.
     */
    public void queueUpdate(
        final String sourceServerItem,
        final int itemId,
        final String targetLocalItem,
        final int localVersion,
        final Calendar localVersionCheckinDate,
        final int encoding,
        final byte[] baselineHashValue,
        final long baselineFileLength,
        final PropertyValue[] properties) {
        queueUpdate(
            new ClientLocalVersionUpdate(
                sourceServerItem,
                itemId,
                targetLocalItem,
                localVersion,
                localVersionCheckinDate,
                encoding,
                baselineHashValue,
                baselineFileLength,
                null,
                null,
                properties));
    }

    /**
     * Add the update to the queue, flushing if thresholds have been exceeded.
     *
     *
     * @param update
     *        the update
     */
    public void queueUpdate(final ILocalVersionUpdate update) {
        Check.notNull(update, "update"); //$NON-NLS-1$

        if (WorkspaceLocation.SERVER == workspace.getLocation() && !update.isSendToServer()) {
            // If this is a server workspace, but this update object is only
            // intended to be used for updating local workspaces, then
            // ignore the update.
            return;
        }

        // Only allow a zero item ID when using a local workspace.
        Check.isTrue(
            WorkspaceLocation.LOCAL == workspace.getLocation() || 0 != update.getItemID(),
            "Local version updates queued for server workspaces must have an item ID to communicate with downlevel servers"); //$NON-NLS-1$

        boolean flush = false;

        synchronized (pendingUpdates) {
            // Used to indicate object.wait timeout.
            boolean timeout = false;

            // Wait until we have room in the queue.
            while (pendingUpdates.size() >= maximumLevel) {
                if (timeout && !flushing) {
                    // We woke up without being signaled, we have a full queue,
                    // and no one appears to be flushing. We'll just go ahead.
                    break;
                }

                final long beforeWaitMillis = System.currentTimeMillis();

                try {
                    pendingUpdates.wait(SLEEP_TIME_MILLISECONDS);
                } catch (final InterruptedException e) {
                }

                final long elapsedMillis = System.currentTimeMillis() - beforeWaitMillis;
                timeout = elapsedMillis >= SLEEP_TIME_MILLISECONDS;
            }

            // If we're disposed, then dispose this update and ignore it.
            if (closed) {
                return;
            }

            if (0 == pendingUpdates.size()) {
                timerMillis = System.currentTimeMillis();
            }

            pendingUpdates.add(update);

            if (!flushing
                && (pendingUpdates.size() >= flushTriggerLevel
                    || System.currentTimeMillis() - timerMillis >= timeTriggerInMilliseconds)) {
                flushing = true;
                flush = true;

                timerMillis = System.currentTimeMillis();
            }
        }

        if (flush) {
            try {
                flush();
            } finally {
                flushing = false;
            }
        }
    }

    /**
     * Send any pending requests to the server and close resources this instance
     * is using.
     */
    @Override
    public void close() {
        synchronized (pendingUpdates) {
            closed = true;
        }

        flush();
        flushAcks();

        // Delete any remaining displaced baselines that were not re-used
        if (null != persistedDisplacedBaselines && persistedDisplacedBaselines.size() > 0) {
            if (null != wLock && null != wLock.getBaselineFolders()) {
                // It's faster to use a cached copy of the baseline folders if
                // we have one.
                final BaselineFolderCollection baselineFolders = wLock.getBaselineFolders();

                for (final byte[] baselineFileGuid : persistedDisplacedBaselines) {
                    baselineFolders.deleteBaseline(baselineFileGuid);
                }
            } else {
                final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace, wLock);
                try {
                    transaction.execute(new WorkspacePropertiesTransaction() {
                        @Override
                        public void invoke(final LocalWorkspaceProperties wp) {
                            for (final byte[] baselineFileGuid : persistedDisplacedBaselines) {
                                wp.deleteBaseline(baselineFileGuid);
                            }
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
            }
        }
    }

    /**
     * Send any pending requests to the server
     */
    public void flush() {
        synchronized (flushLock) {
            ILocalVersionUpdate[] updates = null;

            synchronized (pendingUpdates) {
                updates = pendingUpdates.toArray(new ILocalVersionUpdate[pendingUpdates.size()]);
            }

            if (options.contains(UpdateLocalVersionQueueOptions.UPDATE_LOCAL)
                && WorkspaceLocation.LOCAL == workspace.getLocation()) {
                ensureUpdatesFullyPopulated(updates);
            }

            try {
                downloadMissingBaselines(sendToServer(updates));
            } finally {
                synchronized (pendingUpdates) {
                    for (int i = updates.length - 1; i >= 0; i--) {
                        pendingUpdates.remove(0);
                    }

                    // Wake up any threads blocked on a full queue.
                    pendingUpdates.notifyAll();
                }
            }
        }
    }

    private void ensureUpdatesFullyPopulated(final ILocalVersionUpdate[] updates) {
        final boolean setFileTimeToCheckin = workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN);

        // For the logic for fetching from QueryPendingChanges
        final List<ItemSpec> toFetchFromPendingChanges = new ArrayList<ItemSpec>();
        final Map<String, IPopulatableLocalVersionUpdate> targetServerItemMap =
            new TreeMap<String, IPopulatableLocalVersionUpdate>(String.CASE_INSENSITIVE_ORDER);

        // For the logic for fetching from QueryItems
        final List<IPopulatableLocalVersionUpdate> toFetchFromQueryItems =
            new ArrayList<IPopulatableLocalVersionUpdate>();

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace, wLock);
        try {
            transaction.execute(new WorkspacePropertiesLocalVersionTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
                    for (final ILocalVersionUpdate update : updates) {
                        // We only care about unpopulated
                        // IPopulatableLocalVersionUpdate objects.
                        if (!(update instanceof IPopulatableLocalVersionUpdate)) {
                            continue;
                        }

                        final IPopulatableLocalVersionUpdate cUpdate = (IPopulatableLocalVersionUpdate) update;
                        final WorkspaceLocalItem lvExisting =
                            lv.getByServerItem(cUpdate.getSourceServerItem(), cUpdate.isCommitted());

                        if (null != lvExisting && lvExisting.getVersion() == cUpdate.getVersionLocal()) {
                            cUpdate.updateFrom(lvExisting);
                        }

                        if (cUpdate.isFullyPopulated(setFileTimeToCheckin)) {
                            continue;
                        }

                        if (null != cUpdate.getPendingChangeTargetServerItem()) {
                            toFetchFromPendingChanges.add(
                                new ItemSpec(cUpdate.getPendingChangeTargetServerItem(), RecursionType.NONE));
                            targetServerItemMap.put(cUpdate.getPendingChangeTargetServerItem(), cUpdate);
                        } else {
                            toFetchFromQueryItems.add(cUpdate);
                        }
                    }
                }

            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }
        }

        if (toFetchFromPendingChanges.size() > 0) {
            Check.isTrue(
                options.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER),
                "Making a server call to fetch missing local version data during an offline operation"); //$NON-NLS-1$

            // We can't call Workspace.GetPendingChanges here, since that's an
            // offline operation. We want *real* PendingChange objects here,
            // from the server.

            final WebServiceLayer webServiceLayer = workspace.getClient().getWebServiceLayer();
            final PendingChange[] pendingChanges = webServiceLayer.queryServerPendingChanges(
                workspace,
                toFetchFromPendingChanges.toArray(new ItemSpec[toFetchFromPendingChanges.size()]),
                true,
                workspace.getClient().mergeWithDefaultItemPropertyFilters(null));

            for (final PendingChange pendingChange : pendingChanges) {
                final IPopulatableLocalVersionUpdate cUpdate = targetServerItemMap.get(pendingChange.getServerItem());

                if (cUpdate == null) {
                    log.warn("EnsureUpdatesFullyPopulated: Query did not return a PendingChange"); //$NON-NLS-1$
                    continue;
                }

                cUpdate.updateFrom(pendingChange);

                Check.isTrue(
                    cUpdate.isFullyPopulated(setFileTimeToCheckin),
                    "cUpdate.isFullyPopulated(setFileTimeToCheckin)"); //$NON-NLS-1$

                // Save off the download URL in case we need it after calling
                // UpdateLocalVersion.
                cUpdate.setDownloadURL(pendingChange.getDownloadURL());
            }
        }

        for (final IPopulatableLocalVersionUpdate update : toFetchFromQueryItems) {
            final GetItemsOptions options =
                GetItemsOptions.INCLUDE_SOURCE_RENAMES.combine(GetItemsOptions.UNSORTED).combine(
                    GetItemsOptions.DOWNLOAD);

            final ItemSet[] items = workspace.getClient().getItems(new ItemSpec[] {
                new ItemSpec(update.getSourceServerItem(), RecursionType.NONE)
            }, new ChangesetVersionSpec(update.getVersionLocal()), DeletedState.ANY, ItemType.ANY, options);

            if (items[0].getItems().length != 1) {
                log.warn("EnsureUpdatesFullyPopulated: Result missing"); //$NON-NLS-1$
            }

            for (final Item item : items[0].getItems()) {
                update.updateFrom(item);

                Check.isTrue(
                    update.isFullyPopulated(setFileTimeToCheckin),
                    "update.isFullyPopulated(setFileTimeToCheckin)"); //$NON-NLS-1$

                // Save off the download URL in case we need it after calling
                // UpdateLocalVersion.
                update.setDownloadURL(item.getDownloadURL());
                break;
            }
        }
    }

    private void downloadMissingBaselines(final IPopulatableLocalVersionUpdate[] updates) {
        if (null == updates || 0 == updates.length) {
            return;
        }

        Check.isTrue(
            options.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER),
            "Making a server call to fetch a missing baseline during an offline operation"); //$NON-NLS-1$

        final List<BaselineRequest> baselineRequests = new ArrayList<BaselineRequest>(updates.length);

        for (final IPopulatableLocalVersionUpdate update : updates) {
            if (null != update.getDownloadURL()) {
                // We already fetched this download URL in
                // EnsureUpdatesFullyPopulated.
                final BaselineRequest request = BaselineRequest.fromDownloadUrl(
                    update.getBaselineFileGUID(),
                    update.getTargetLocalItem(),
                    update.getDownloadURL(),
                    update.getBaselineHashValue());

                baselineRequests.add(request);
            } else {
                // We need to fetch this download URL from the server with a
                // QueryItems call.
                final GetItemsOptions options =
                    GetItemsOptions.INCLUDE_SOURCE_RENAMES.combine(GetItemsOptions.UNSORTED).combine(
                        GetItemsOptions.DOWNLOAD);
                final ItemSet[] items = workspace.getClient().getItems(new ItemSpec[] {
                    new ItemSpec(update.getSourceServerItem(), RecursionType.NONE)
                }, new ChangesetVersionSpec(update.getVersionLocal()), DeletedState.ANY, ItemType.ANY, options);

                if (items[0].getItems().length != 1) {
                    log.warn("DownloadMissingBaselines: Result missing"); //$NON-NLS-1$
                }

                for (final Item item : items[0].getItems()) {
                    final BaselineRequest request = BaselineRequest.fromDownloadUrl(
                        update.getBaselineFileGUID(),
                        update.getTargetLocalItem(),
                        item.getDownloadURL(),
                        item.getContentHashValue());

                    baselineRequests.add(request);
                    break;
                }
            }
        }

        // Now we're going to go fetch the new baselines. If we need to create
        // our own master lock, do so.
        final WorkspaceLock workspaceLock = wLock == null ? workspace.lock() : wLock;

        try {
            // If we created our own master lock, then put a
            // BaselineFolderCollection on it.
            if (null == wLock) {
                final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
                try {
                    transaction.execute(new WorkspacePropertiesTransaction() {
                        @Override
                        public void invoke(final LocalWorkspaceProperties wp) {
                            workspaceLock.setBaselineFolders(
                                new BaselineFolderCollection(workspace, wp.getBaselineFolders()));
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
            }

            workspaceLock.getBaselineFolders().processBaselineRequests(workspace, baselineRequests);
        } finally {
            // If we created our own master lock, release it.
            if (null == wLock) {
                workspaceLock.close();
            }
        }
    }

    /**
     * Sends the given updates to the server.
     *
     * @param updates
     *        the updates to send (must not be <code>null</code>)
     */
    private IPopulatableLocalVersionUpdate[] sendToServer(final ILocalVersionUpdate[] updates) {
        Check.notNull(updates, "updates"); //$NON-NLS-1$

        log.debug(MessageFormat.format(
            "Sending {0} updates to the server (options={1})", //$NON-NLS-1$
            Integer.toString(updates.length),
            options));

        if (updates.length == 0) {
            return null;
        }

        final AtomicReference<IPopulatableLocalVersionUpdate[]> updatesMissingBaselines =
            new AtomicReference<IPopulatableLocalVersionUpdate[]>();

        if (workspace.getLocation() == WorkspaceLocation.LOCAL
            && options.contains(UpdateLocalVersionQueueOptions.UPDATE_LOCAL)) {
            /*
             * We cannot perform baseline folder maintenance with the LV or PC
             * tables open. The baseline folder maintenance could cause them to
             * move.
             */
            final LocalWorkspaceTransaction baselineMaintTransaction = new LocalWorkspaceTransaction(workspace, wLock);
            try {
                baselineMaintTransaction.execute(new WorkspacePropertiesTransaction() {
                    @Override
                    public void invoke(final LocalWorkspaceProperties wp) {
                        wp.doBaselineFolderMaintenance();
                    }
                });
            } finally {
                try {
                    baselineMaintTransaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }

            /*
             * Must run while synchronized(this) because
             * persistedDisplacedBaselines is accessed.
             */
            synchronized (this) {
                final LocalWorkspaceTransaction updateTransaction = new LocalWorkspaceTransaction(workspace, wLock);
                try {
                    updateTransaction.execute(new AllTablesTransaction() {
                        @Override
                        public void invoke(
                            final LocalWorkspaceProperties wp,
                            final WorkspaceVersionTable lv,
                            final LocalPendingChangesTable pc) {
                            if (options.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER)) {
                                ILocalVersionUpdate[] acks;

                                synchronized (pendingAcks) {
                                    acks = pendingAcks.toArray(new ILocalVersionUpdate[pendingAcks.size()]);
                                    pendingAcks.clear();
                                }

                                if (acks.length > 0) {
                                    LocalDataAccessLayer.acknowledgeUpdateLocalVersion(lv, acks);
                                }
                            }

                            LocalDataAccessLayer.updateLocalVersion(
                                workspace,
                                wp,
                                lv,
                                pc,
                                updates,
                                persistedDisplacedBaselines,
                                updatesMissingBaselines);
                        }
                    });
                } finally {
                    try {
                        updateTransaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
            }
        }

        if (options.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER)) {
            workspace.getClient().getWebServiceLayer().updateLocalVersion(
                workspace.getName(),
                workspace.getOwnerName(),
                updates);

            if (options.contains(UpdateLocalVersionQueueOptions.UPDATE_LOCAL)
                && WorkspaceLocation.LOCAL == workspace.getLocation()) {
                synchronized (pendingAcks) {
                    for (final ILocalVersionUpdate update : updates) {
                        pendingAcks.add(update);
                    }
                }
            }
        }

        return updatesMissingBaselines.get();
    }

    /**
     * When the UpdateLocalVersionQueue is being disposed, if the queue is
     * flushing to both local and server, take the last batch of
     * ILocalVersionUpdate objects that need to have the PendingReconcile bit
     * cleared, and do that work.
     */
    private void flushAcks() {
        if (options.equals(UpdateLocalVersionQueueOptions.UPDATE_BOTH)
            && WorkspaceLocation.LOCAL == workspace.getLocation()) {
            final ILocalVersionUpdate[] acks;

            synchronized (pendingAcks) {
                acks = pendingAcks.toArray(new ILocalVersionUpdate[pendingAcks.size()]);
                pendingAcks.clear();
            }

            if (acks.length > 0) {
                final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace, wLock);
                try {
                    transaction.execute(new LocalVersionTransaction() {

                        @Override
                        public void invoke(final WorkspaceVersionTable lv) {
                            LocalDataAccessLayer.acknowledgeUpdateLocalVersion(lv, acks);
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
            }
        }
    }
}
