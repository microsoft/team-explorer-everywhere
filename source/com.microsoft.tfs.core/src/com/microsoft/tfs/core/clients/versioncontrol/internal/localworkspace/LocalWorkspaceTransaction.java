// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.UnableToLoadLocalPropertiesTableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.UnableToLoadLocalVersionTableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.UnableToLoadPendingChangesTableException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;
import com.microsoft.tfs.util.Check;

public class LocalWorkspaceTransaction implements Closeable {
    private static final Log log = LogFactory.getLog(LocalWorkspaceTransaction.class);

    /**
     * The local workspace this transaction is for.
     */
    private final Workspace workspace;

    /**
     * The workspace lock protecting the workspace. Allocated on construction or
     * acquireTables. Closed and set to <code>null</code> on {@link #close()}.
     */
    private WorkspaceLock workspaceLock;

    /**
     * True if the lock should allow yields.
     */
    private final boolean requestYield;

    /**
     * The thread which created this transaction -- it must be the same one that
     * disposes it
     */
    private final long creationThreadID;

    /**
     * Set to <code>true</code> on construction if this instance owns
     * {@link #workspaceLock}, <code>false</code> if some other instance owns
     * {@link #workspaceLock}.
     */
    private final boolean ownsWorkspaceLock;

    // Lock order: WorkspaceProperties, LocalVersion, PendingChanges.
    private LocalWorkspaceProperties wp;
    private WorkspaceVersionTable lv;
    private LocalPendingChangesTable pc;

    // Lock order: LocalVersionHeader, PendingChangesHeader.
    private WorkspaceVersionTableHeader lvh;
    private LocalPendingChangesTableHeader pch;

    // Indicates whether or not autorecovery of the workspace properties table
    // is enabled for this transaction (defaults to true)
    private boolean autoRecover = true;

    // Indicates whether or not LocalWorkspaceTransaction will use the
    // OfflineCacheData of the Workspace object to locate a cached copy of the
    // metadata tables for the workspace and provide them to the
    // LocalMetadataTable subclasses at instantiation. This provides a
    // substantial performance benefit.
    private static final boolean ALLOW_CACHED_LOADS;
    private static final String ALLOW_CACHED_LOADS_PROPERTY_NAME =
        "com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.enablecachedmetadatatableloads"; //$NON-NLS-1$

    // Events
    private volatile boolean raisePendingChangesChanged;
    private volatile boolean raisePendingChangeCandidatesChanged;

    /**
     * The current {@link LocalWorkspaceTransaction} on this thread.
     */
    private final static ThreadLocal<LocalWorkspaceTransaction> current = new ThreadLocal<LocalWorkspaceTransaction>();

    static {
        final String propertyValue = System.getProperty(ALLOW_CACHED_LOADS_PROPERTY_NAME);
        ALLOW_CACHED_LOADS = propertyValue != null && propertyValue.equalsIgnoreCase("false") ? false : true; //$NON-NLS-1$
    }

    public LocalWorkspaceTransaction(final Workspace workspace) {
        this(workspace, null);
    }

    public LocalWorkspaceTransaction(final Workspace workspace, final boolean requestYield) {
        this(workspace, null, requestYield);
    }

    public LocalWorkspaceTransaction(final Workspace workspace, final WorkspaceLock workspaceLock) {
        this(workspace, workspaceLock, true);
    }

    /**
     * Constructor which takes a WorkspaceLock instance. If you already have the
     * workspace locked, and are on a different thread from the one where you
     * locked it (or don't know whether you are), then pass in that workspace
     * lock here, and LocalWorkspaceTransaction will skip acquisition of the
     * workspace lock (since you provided it yourself). In this case,
     * LocalWorkspaceTransaction will not dispose the lock when you dispose it.
     *
     * @param workspace
     *        Local workspace on which to perform a transaction
     * @param workspaceLock
     *        WorkspaceLock instance which locks the local workspace (or null)
     */
    public LocalWorkspaceTransaction(
        final Workspace workspace,
        final WorkspaceLock workspaceLock,
        final boolean requestYield) {
        Check.isTrue(WorkspaceLocation.LOCAL == workspace.getLocation(), "Workspace must be a local workspace"); //$NON-NLS-1$

        this.workspace = workspace;
        this.workspaceLock = workspaceLock != null ? workspaceLock : WorkspaceLock.getCurrent();
        this.ownsWorkspaceLock = (null == this.workspaceLock);
        this.requestYield = requestYield;

        if (!ownsWorkspaceLock) {
            // Ensure workspace is same instance as lock's workspace
            Check.isTrue(
                this.workspace.equals(this.workspaceLock.getWorkspace()),
                "this.workspace.equals(this.workspaceLock.getWorkspace())"); //$NON-NLS-1$

            this.workspaceLock.startTransaction();
        }

        Check.isTrue(
            LocalWorkspaceTransaction.current.get() == null,
            "A local workspace transaction is already running on this thread"); //$NON-NLS-1$

        LocalWorkspaceTransaction.current.set(this);
        this.creationThreadID = Thread.currentThread().getId();

    }

    public void setAllowTxF(final boolean allow) {
    }

    public boolean ownsWorkspaceLock() {
        return ownsWorkspaceLock;
    }

    public boolean isRaisePendingChangesChanged() {
        return this.raisePendingChangesChanged;
    }

    public void setRaisePendingChangesChanged(final boolean value) {
        this.raisePendingChangesChanged = value;
    }

    public boolean isRaisePendingChangeCandidatesChanged() {
        return this.raisePendingChangeCandidatesChanged;
    }

    public void setRaisePendingChangeCandidatesChanged(final boolean value) {
        this.raisePendingChangeCandidatesChanged = value;
    }

    /**
     * @return the local workspace on which this transaction is being performed.
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    /**
     * @return the local workspace transaction currently running on this thread
     */
    public static LocalWorkspaceTransaction getCurrent() {
        return current.get();
    }

    /**
     * @return the tables opened by this transaction
     */
    public Tables getOpenedTables() {
        Tables toReturn = Tables.NONE;

        if (null != wp) {
            toReturn = toReturn.combine(Tables.WORKSPACE_PROPERTIES);
        }

        if (null != lv || null != lvh) {
            toReturn = toReturn.combine(Tables.LOCAL_VERSION).combine(Tables.LOCAL_VERSION_HEADER);
        }

        if (null != pc || null != pch) {
            toReturn = toReturn.combine(Tables.PENDING_CHANGES).combine(Tables.PENDING_CHANGES_HEADER);
        }

        return toReturn;
    }

    /**
     * The workspace lock protecting this transaction
     */
    public WorkspaceLock getWorkspaceLock() {
        return workspaceLock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // TODO: Transaction file system not implemented. See VS
        // LocalWorkspaceTransaction.Dispose().

        Check.isTrue(
            Thread.currentThread().getId() == creationThreadID,
            "A different thread is disposing a LocalWorkspaceTransaction than the one that created it"); //$NON-NLS-1$

        boolean raisePendingChangesChanged = false;
        boolean raisePendingChangeCandidatesChanged = false;

        // Release the tables in reverse lock order:
        // Pending changes, then local version, then workspace properties

        // Make sure to dispose of all tables and the workspace lock, and to
        // reset the value of ts_current, even in the face of exceptions. The
        // last exception wins.

        try {
            // Pending changes

            if (null != pc) {
                Check.isTrue(null == pch, "null == pch"); //$NON-NLS-1$

                if (this.raisePendingChangesChanged && !pc.isAborted()) {
                    raisePendingChangesChanged = true;
                } else if (this.raisePendingChangeCandidatesChanged && !pc.isAborted()) {
                    // We consider the PendingChangesChanged event to be a
                    // superset of the PendingChangeCandidatesChanged event.
                    raisePendingChangeCandidatesChanged = true;
                }

                pc.close();
                workspace.getOfflineCacheData().cacheMetadataTable(pc);
                pc = null;
            } else if (null != pch) {
                pch.close();
                workspace.getOfflineCacheData().cacheMetadataTable(pch);
                pch = null;
            }
        } finally {
            // Local version

            try {
                if (null != lv) {
                    Check.isTrue(null == lvh, "null == lvh"); //$NON-NLS-1$

                    lv.close();
                    workspace.getOfflineCacheData().cacheMetadataTable(lv);
                    lv = null;
                } else if (null != lvh) {
                    lvh.close();
                    workspace.getOfflineCacheData().cacheMetadataTable(lvh);
                    lvh = null;
                }
            } finally {
                // Workspace properties

                try {
                    if (null != wp) {
                        wp.close();
                        workspace.getOfflineCacheData().cacheMetadataTable(wp);
                        wp = null;
                    }
                } finally {
                    // Workspace lock

                    try {
                        if (null != workspaceLock) {
                            if (ownsWorkspaceLock) {
                                workspaceLock.close();
                                workspaceLock = null;
                            } else {
                                workspaceLock.endTransaction();
                            }
                        }
                    } finally {
                        // Thread-static local workspace transaction reference

                        current.set(null);
                    }
                }
            }
        }

        // Raise an event indicating that the pending changes changed as a
        // result of work done by the scanner.
        if (raisePendingChangesChanged) {
            workspace.getClient().getEventEngine().firePendingChangesChangedEvent(
                new WorkspaceEvent(EventSource.newFromHere(), workspace, WorkspaceEventSource.EXTERNAL_SCANNED));
        }

        // Raise an event indicating that the pending change candidates changed
        // as a result of work done by the scanner.
        if (raisePendingChangeCandidatesChanged) {
            workspace.getClient().getEventEngine().firePendingChangeCandidatesChangedEvent(
                new WorkspaceEvent(EventSource.newFromHere(), workspace, WorkspaceEventSource.EXTERNAL_SCANNED));
        }
    }

    public void abort() {
        // Abort in reverse lock order:
        // Pending changes, then local version, then workspace properties

        // No need to abort the headers; they are immutable

        if (null != pc) {
            pc.setAborted(true);
        }

        if (null != lv) {
            lv.setAborted(true);
        }

        if (null != wp) {
            wp.setAborted(true);
        }
    }

    public void execute(final WorkspacePropertiesTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES);

        if (null == wp || wp.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(wp);
            success = true;
        } finally {
            if (success == false) {
                if (null != wp) {
                    wp.setAborted(true);
                }
            }
        }
    }

    public void execute(final LocalVersionTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION));

        if (null == lv || lv.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(lv);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final LocalVersionHeaderTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION_HEADER));

        if (null == lvh || lvh.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(lvh);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final PendingChangesTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION_HEADER).combine(Tables.PENDING_CHANGES));

        if (null == pc || pc.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(pc);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final PendingChangesHeaderTransaction toExecute) {
        acquireTables(
            Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION_HEADER).combine(Tables.PENDING_CHANGES_HEADER));

        if (null == pch || pch.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(pch);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final WorkspacePropertiesLocalVersionTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION));

        if (null == wp || null == lv || wp.isAborted() || lv.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(wp, lv);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final LocalVersionPendingChangesTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION).combine(Tables.PENDING_CHANGES));

        if (null == lv || null == pc || lv.isAborted() || pc.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(lv, pc);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final LocalVersionPendingChangesHeadersTransaction toExecute) {
        acquireTables(
            Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION_HEADER).combine(Tables.PENDING_CHANGES_HEADER));

        if (null == lvh || null == pch || lvh.isAborted() || pch.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(lvh, pch);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    public void execute(final AllTablesTransaction toExecute) {
        acquireTables(Tables.WORKSPACE_PROPERTIES.combine(Tables.LOCAL_VERSION).combine(Tables.PENDING_CHANGES));

        if (null == wp || null == lv || null == pc || wp.isAborted() || lv.isAborted() || pc.isAborted()) {
            throw new IllegalStateException();
        }

        boolean success = false;
        try {
            toExecute.invoke(wp, lv, pc);
            success = true;
        } finally {
            if (success == false) {
                abort();
            }
        }
    }

    private void acquireTables(final Tables toAcquire) {
        // Lock order: Workspace lock, workspace properties, local version,
        // pending changes.
        if (ownsWorkspaceLock) {
            Check.isTrue(
                null == workspaceLock,
                "Execute() may only be called once per instance of LocalWorkspaceTransaction"); //$NON-NLS-1$

            workspaceLock = new WorkspaceLock(workspace, requestYield);
        }

        if (null == wp && toAcquire.contains(Tables.WORKSPACE_PROPERTIES)) {
            try {
                final String wpFullPathWithoutExtension =
                    LocalPath.combine(workspace.getLocalMetadataDirectory(), "properties"); //$NON-NLS-1$
                boolean regenerateWorkspaceProperties = false;

                if (autoRecover) {
                    regenerateWorkspaceProperties = !doAutoRecover(wpFullPathWithoutExtension);
                }

                LocalWorkspaceProperties cachedTable = null;

                if (!regenerateWorkspaceProperties && ALLOW_CACHED_LOADS) {
                    cachedTable = (LocalWorkspaceProperties) workspace.getOfflineCacheData().getCachedMetadataTable(
                        LocalWorkspaceProperties.class);
                }

                wp = new LocalWorkspaceProperties(wpFullPathWithoutExtension, cachedTable);

                if (regenerateWorkspaceProperties) {
                    final Workspace serverWorkspace = workspace.getClient().getWebServiceLayer().queryServerWorkspace(
                        workspace.getName(),
                        workspace.getOwnerName());

                    if (null != serverWorkspace) {
                        wp.setWorkingFolders(serverWorkspace.getFolders());
                    }
                }
            } catch (final Exception e) {
                throw new UnableToLoadLocalPropertiesTableException(workspace.getDisplayName(), e);
            }
        }

        String localVersionTableLocation = null;
        String pendingChangesTableLocation = null;

        if (toAcquire.contains(Tables.LOCAL_VERSION) || toAcquire.contains(Tables.LOCAL_VERSION_HEADER)) {
            localVersionTableLocation = wp.getMetadataTableLocation("localversion"); //$NON-NLS-1$
        }

        if (toAcquire.contains(Tables.PENDING_CHANGES) || toAcquire.contains(Tables.PENDING_CHANGES_HEADER)) {
            pendingChangesTableLocation = wp.getMetadataTableLocation("pendingchanges"); //$NON-NLS-1$
        }

        if (null == lv && toAcquire.contains(Tables.LOCAL_VERSION)) {
            try {
                WorkspaceVersionTable cachedTable = null;

                if (ALLOW_CACHED_LOADS) {
                    cachedTable = (WorkspaceVersionTable) workspace.getOfflineCacheData().getCachedMetadataTable(
                        WorkspaceVersionTable.class);
                }

                lv = new WorkspaceVersionTable(localVersionTableLocation, cachedTable);
            } catch (final Exception e) {
                throw new UnableToLoadLocalVersionTableException(workspace.getDisplayName(), e);
            }
        }

        if (null == lvh && toAcquire.contains(Tables.LOCAL_VERSION_HEADER)) {
            try {
                LocalMetadataTable cachedTable = null;

                if (ALLOW_CACHED_LOADS) {
                    // The local version table header can cached-load from
                    // either the header or the full table.
                    cachedTable = getBestCachedLoadSource(
                        WorkspaceVersionTable.class,
                        WorkspaceVersionTableHeader.class,
                        workspace);
                }

                lvh = new WorkspaceVersionTableHeader(localVersionTableLocation, cachedTable);
            } catch (final Exception e) {
                throw new UnableToLoadLocalVersionTableException(workspace.getDisplayName(), e);
            }
        }

        if (null == pc && toAcquire.contains(Tables.PENDING_CHANGES)) {
            try {
                LocalPendingChangesTable cachedTable = null;

                if (ALLOW_CACHED_LOADS) {
                    cachedTable = (LocalPendingChangesTable) workspace.getOfflineCacheData().getCachedMetadataTable(
                        LocalPendingChangesTable.class);
                }

                pc = new LocalPendingChangesTable(pendingChangesTableLocation, cachedTable);
            } catch (final Exception e) {
                throw new UnableToLoadPendingChangesTableException(workspace.getDisplayName(), e);
            }
        }

        if (null == pch && toAcquire.contains(Tables.PENDING_CHANGES_HEADER)) {
            try {
                LocalMetadataTable cachedTable = null;

                if (ALLOW_CACHED_LOADS) {
                    // The pending changes table header can cached-load from
                    // either the header or the full table.

                    cachedTable = getBestCachedLoadSource(
                        LocalPendingChangesTable.class,
                        LocalPendingChangesTableHeader.class,
                        workspace);
                }

                pch = new LocalPendingChangesTableHeader(pendingChangesTableLocation, cachedTable);
            } catch (final Exception e) {
                throw new UnableToLoadPendingChangesTableException(workspace.getDisplayName(), e);
            }
        }
    }

    private static LocalMetadataTable getBestCachedLoadSource(
        final Class<? extends LocalMetadataTable> t,
        final Class<? extends LocalMetadataTable> u,
        final Workspace workspace) {
        final LocalMetadataTable cachedLoadSourceT = workspace.getOfflineCacheData().getCachedMetadataTable(t);
        final LocalMetadataTable cachedLoadSourceU = workspace.getOfflineCacheData().getCachedMetadataTable(u);

        if (null == cachedLoadSourceT && null == cachedLoadSourceU) {
            return null;
        }

        if (null != cachedLoadSourceT && null == cachedLoadSourceU) {
            return cachedLoadSourceT;
        }

        if (null == cachedLoadSourceT && null != cachedLoadSourceU) {
            return cachedLoadSourceU;
        }

        // Complex case: Compare the last-modified times of the
        // SavedAttributes of the two tables.
        final FileSystemTime writeTimeT = cachedLoadSourceT.getSavedAttributes().getModificationTime();
        final FileSystemTime writeTimeU = cachedLoadSourceU.getSavedAttributes().getModificationTime();

        /*
         * Note: Visual Studio uses a 64 bit int to represent modification time
         * and if writeTimeU is strictly greater than writeTimeT, then
         * cachedLoadSourceU is definitive. Emulate this behavior but include
         * null checks.
         */
        if (writeTimeU != null && writeTimeT == null) {
            return cachedLoadSourceU;
        }

        if (writeTimeU != null && writeTimeT != null && writeTimeT.compareTo(writeTimeU) < 0) {
            return cachedLoadSourceU;
        } else {
            return cachedLoadSourceT;
        }
    }

    public boolean getAutoRecover() {
        return autoRecover;
    }

    public void setAutoRecover(final boolean value) {
        autoRecover = value;
    }

    /**
     * Given the presumed path for the local workspace properties table (WP),
     * ensures that either the slot one or slot two (.tf1 or .tf2) path exists.
     * If not, attempts to use the MappedPaths data from the WorkspaceInfo for
     * this Workspace to locate a backup copy of the properties.tf1 file and
     * restore it to the LocalMetadataDirectory for this workspace.
     *
     *
     * @param wpPathWithoutExtension
     *        The presumed path for the local workspace properties table (WP)
     * @return
     */
    private boolean doAutoRecover(final String wpPathWithoutExtension) {
        final String slotOnePath = LocalMetadataTable.getSlotOnePath(wpPathWithoutExtension);

        if (new File(slotOnePath).exists()) {
            return true;
        }

        final String slotTwoPath = LocalMetadataTable.getSlotTwoPath(wpPathWithoutExtension);

        if (new File(slotTwoPath).exists()) {
            return true;
        }

        // At this point, auto-recovery is necessary.
        final String wpTableName = LocalPath.getFileName(wpPathWithoutExtension);

        try {
            final PersistenceStoreProvider provider =
                workspace.getClient().getConnection().getPersistenceStoreProvider();

            final WorkspaceInfo wsInfo = Workstation.getCurrent(provider).getLocalWorkspaceInfo(
                workspace.getClient(),
                workspace.getName(),
                workspace.getOwnerName());

            if (null != wsInfo) {
                // Loop through $tf, $tf1, $tf2, etc. "PartitioningFolderCount"
                // is the number of partitioning folders in each baseilne
                // folder, as well as the maximum number of retries we take to
                // create a $tf folder on disk.
                for (int i = 0; i < BaselineFolder.getPartitioningFolderCount(); i++) {
                    String tfFolderName = BaselineFolder.getBaselineFolderName();

                    if (0 != i) {
                        tfFolderName = tfFolderName + Integer.toString(i);
                    }

                    for (final String mappedPath : wsInfo.getMappedPaths()) {
                        final String potentialBaselineFolderName = LocalPath.combine(mappedPath, tfFolderName);
                        final String potentialWpPathWithoutExtension =
                            LocalPath.combine(potentialBaselineFolderName, wpTableName);
                        final String potentialSlotOnePath =
                            LocalMetadataTable.getSlotOnePath(potentialWpPathWithoutExtension);

                        if (new File(potentialSlotOnePath).exists()) {
                            BaselineFolder.ensureLocalMetadataDirectoryExists(workspace);
                            FileCopyHelper.copy(potentialSlotOnePath, slotOnePath);
                            return true;
                        }
                    }
                }
            }
        } catch (final Exception ex) {
            log.trace("Auto recover failed", ex); //$NON-NLS-1$
        }

        return false;
    }
}
