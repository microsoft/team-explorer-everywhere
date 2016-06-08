// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.InvalidPendingChangeTableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.WorkspaceVersionTableException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.ServerItemLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumSubTreeOptions;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree.ModifyInPlaceCallback;
import com.microsoft.tfs.util.Check;

public class WorkspaceVersionTable extends LocalMetadataTable {
    private static final Log log = LogFactory.getLog(WorkspaceVersionTable.class);

    private static final short MAGIC = (short) 0xa7cc;
    private static final int SCHEMA_VERSION_2 = 2;

    public SparseTree<WorkspaceLocalItemPair> server;
    public SparseTree<WorkspaceLocalItem> local;

    private List<WorkspaceLocalItem> removedItems;
    private int pendingReconcileCount;

    /**
     * Creates or opens a workspace metadata table with the specified base file
     * name and location.
     *
     *
     * @param tableLocation
     *        The fully qualified path that will be used to construct the actual
     *        metadata file name.
     * @param cachedLoadSource
     * @throws Exception
     */
    public WorkspaceVersionTable(final String tableLocation, final WorkspaceVersionTable cachedLoadSource)
        throws Exception {
        super(tableLocation, cachedLoadSource);
        /* Don't do anything here, Initialize() runs first */
    }

    /**
     * Called unconditionlly during the base class ctor to allow this class a
     * chance to initialize prior to Load being called.
     */
    @Override
    protected void initialize() {
        server = new SparseTree<WorkspaceLocalItemPair>(
            ServerPath.PREFERRED_SEPARATOR_CHARACTER,
            String.CASE_INSENSITIVE_ORDER);

        local = new SparseTree<WorkspaceLocalItem>(File.separatorChar, String.CASE_INSENSITIVE_ORDER);

        removedItems = new ArrayList<WorkspaceLocalItem>();
    }

    @Override
    protected void load(final InputStream is) throws InvalidPendingChangeTableException, IOException {
        final BinaryReader br = new BinaryReader(is, "UTF-16LE"); //$NON-NLS-1$

        try {
            final short magic = br.readInt16();

            if (MAGIC != magic) {
                throw new WorkspaceVersionTableException(
                    Messages.getString("WorkspaceVersionTable.InvalidVersionTable")); //$NON-NLS-1$
            }

            final int schemaVersion = br.readInt32();
            if (schemaVersion == SCHEMA_VERSION_2) {
                loadFromVersion2(br);
            } else {
                throw new WorkspaceVersionTableException(
                    Messages.getString("WorkspaceVersionTable.InvalidVersionTable")); //$NON-NLS-1$
            }
        } catch (final Exception e) {
            if (e instanceof WorkspaceVersionTableException) {
                throw (WorkspaceVersionTableException) e;
            } else {
                // Wrap the exception
                throw new WorkspaceVersionTableException(e);
            }
        } finally {
            br.close();
        }
    }

    private void loadFromVersion2(final BinaryReader br) throws IOException {
        // pending reconcile bit ignored.
        br.readBoolean();

        // Number of local version entries in the table
        final int rowCount = br.readInt32();

        for (int i = 0; i < rowCount; i++) {
            final WorkspaceLocalItem lvEntry = WorkspaceLocalItem.fromVersion2(br);

            // The removed list keeps track of removed local version rows that
            // must be reconciled to the server. These items have the following
            // flags:
            // 1. No local item (because the instruction for the server is to
            // remove the row).
            // 2. Pending reconcile bit set (because the instruction should be
            // sent to the server at
            // the next reconcile).
            // 3. Deleted bit *not* set (because otherwise the entry goes in the
            // tree with a null local item).
            if (null == lvEntry.getLocalItem() && lvEntry.isPendingReconcile() && !lvEntry.isDeleted()) {
                pendingReconcileCount++;
                removedItems.add(lvEntry);
            } else {
                add(lvEntry, true);
            }
        }

    }

    @Override
    protected boolean cachedLoad(final LocalMetadataTable source) {
        WorkspaceVersionTable lvCached = null;
        if (source instanceof WorkspaceVersionTable) {
            lvCached = (WorkspaceVersionTable) source;
        }

        if (null != lvCached) {
            pendingReconcileCount = lvCached.pendingReconcileCount;
            server = lvCached.server;
            local = lvCached.local;
            removedItems = lvCached.removedItems;

            // Re-mark the source as eligible for cached load. We took a deep
            // copy of what we need.
            source.setEligibleForCachedLoad(true);

            return true;
        }

        return false;
    }

    @Override
    protected boolean save(final OutputStream os) throws IOException {
        final BinaryWriter bw = new BinaryWriter(os, "UTF-16LE"); //$NON-NLS-1$

        try {
            bw.write(MAGIC);
            writeToVersion2(bw);
        } finally {
            bw.close();
        }

        return true;
    }

    private void writeToVersion2(final BinaryWriter bw) throws IOException {
        bw.write(SCHEMA_VERSION_2);
        bw.write(getPendingReconcile());

        // Number of items in the local version table. We cannot use
        // m_server.Count + m_removedItems.Count here
        // because each entry in m_server may have one or two rows in it. Count
        // the rows.

        int count = 0;
        int pendingReconcileCount = 0;

        final List<WorkspaceLocalItemPair> workspaceLocalItemPairs = new ArrayList<WorkspaceLocalItemPair>();
        for (final WorkspaceLocalItemPair pair : server.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE)) {
            workspaceLocalItemPairs.add(pair);
            if (null != pair.getCommitted()) {
                if (pair.getCommitted().isPendingReconcile()) {
                    pendingReconcileCount++;
                }
                count++;
            }
            if (null != pair.getUncommitted()) {
                if (pair.getUncommitted().isPendingReconcile()) {
                    pendingReconcileCount++;
                }
                count++;
            }
        }

        pendingReconcileCount += removedItems.size();
        if (pendingReconcileCount != this.pendingReconcileCount) {
            log.error(new Exception("Unexpected pendingReconcileCount != this.pendingReconcileCount")); //$NON-NLS-1$
        }

        // Write the number of rows.
        bw.write(count + removedItems.size());

        // Save the main tree.
        for (final WorkspaceLocalItemPair pair : workspaceLocalItemPairs) {
            if (null != pair.getCommitted()) {
                pair.getCommitted().saveToVersion2(bw);
            }

            if (null != pair.getUncommitted()) {
                pair.getUncommitted().saveToVersion2(bw);
            }
        }

        // Save the removed items list.
        for (final WorkspaceLocalItem item : removedItems) {
            item.saveToVersion2(bw);
        }
    }

    /**
     * Adds an item to the workspace local item table.
     *
     *
     * @param lvEntry
     *        The item to add to the table.
     */
    public void add(final WorkspaceLocalItem lvEntry) {
        add(lvEntry, false);
    }

    private void add(final WorkspaceLocalItem lvEntry, final boolean fromLoad) {
        final AtomicReference<WorkspaceLocalItem> atomicMatch = new AtomicReference<WorkspaceLocalItem>();

        // Create a callback.
        final ModifyInPlaceCallback<WorkspaceLocalItemPair> callback =
            new ModifyInPlaceCallback<WorkspaceLocalItemPair>() {
                @Override
                public WorkspaceLocalItemPair invoke(
                    final String token,
                    WorkspaceLocalItemPair pair,
                    final Object param) {
                    if (pair == null) {
                        pair = new WorkspaceLocalItemPair();
                    }

                    if (lvEntry.isCommitted()) {
                        atomicMatch.set(pair.getCommitted());
                        pair.setCommitted(lvEntry);
                    } else {
                        atomicMatch.set(pair.getUncommitted());
                        pair.setUncommitted(lvEntry);
                    }

                    return pair;
                }
            };

        // Insert by the primary key (ServerItem, IsCommitted).
        server.modifyInPlace(lvEntry.getServerItem(), callback, null);
        final WorkspaceLocalItem matchingEntry = atomicMatch.get();

        setDirty(!fromLoad);

        // Remove the replaced entry from other indexes.
        if (null != matchingEntry) {
            final String localItem = matchingEntry.getLocalItem();
            if (localItem != null && localItem.length() > 0) {
                local.remove(localItem, false);
            }

            if (matchingEntry.isPendingReconcile()) {
                pendingReconcileCount--;
            }
        }

        if (lvEntry.isPendingReconcile()) {
            pendingReconcileCount++;
        }

        // Add the entry to the LocalItem tree if necessary.
        final String localItem = lvEntry.getLocalItem();
        if (localItem != null && localItem.length() > 0) {
            local.add(localItem, lvEntry, true);
        }
    }

    public void markAsReconciled(final LocalWorkspaceProperties wp, final boolean removeMissingFromDiskRows) {
        final List<WorkspaceLocalItem> missingRowsRemoved = new ArrayList<WorkspaceLocalItem>();

        final Iterable<WorkspaceLocalItemPair> pairs = server.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE);

        for (final WorkspaceLocalItemPair pair : pairs) {
            if (null != pair.getCommitted()) {
                if (pair.getCommitted().isPendingReconcile()) {
                    pendingReconcileCount--;
                    pair.getCommitted().setPendingReconcile(false);
                }

                if (removeMissingFromDiskRows && pair.getCommitted().isMissingOnDisk()) {
                    missingRowsRemoved.add(pair.getCommitted());
                }
            }

            if (null != pair.getUncommitted()) {
                if (pair.getUncommitted().isPendingReconcile()) {
                    pendingReconcileCount--;
                    pair.getUncommitted().setPendingReconcile(false);
                }

                if (removeMissingFromDiskRows && pair.getUncommitted().isMissingOnDisk()) {
                    missingRowsRemoved.add(pair.getUncommitted());
                }
            }
        }

        for (final WorkspaceLocalItem lvEntry : missingRowsRemoved) {
            if (lvEntry.hasBaselineFileGUID()) {
                wp.deleteBaseline(lvEntry.getBaselineFileGUID());
            }

            removeByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted(), false);
        }

        pendingReconcileCount -= removedItems.size();
        removedItems.clear();

        Check.isTrue(0 == pendingReconcileCount, "0 == pendingReconcileCount"); //$NON-NLS-1$
        setDirty(true);
    }

    /**
     * Given a local item, returns the associated local version entry.
     */
    public WorkspaceLocalItem getByLocalItem(final String localItem) {
        Check.notNullOrEmpty(localItem, "localItem"); //$NON-NLS-1$
        return local.get(localItem);
    }

    /**
     * Given a (ServerItem, IsCommitted) tuple, returns the associated local
     * version entry.
     *
     *
     * @param serverItem
     *        Server item part
     * @param isCommitted
     *        IsCommitted part
     * @return Local version entry for the given tuple
     */
    public WorkspaceLocalItem getByServerItem(final String serverItem, final boolean isCommitted) {
        Check.notNullOrEmpty(serverItem, "serverItem"); //$NON-NLS-1$

        final WorkspaceLocalItemPair pair = server.get(serverItem);
        if (pair == null) {
            return null;
        }

        if (isCommitted) {
            return pair.getCommitted();
        } else {
            return pair.getUncommitted();
        }
    }

    /**
     * Given a pending change, returns the associated local version entry.
     *
     *
     * @param pcEntry
     *        Pending change to look up
     * @return Local version entry for the given pending change
     */
    public WorkspaceLocalItem getByPendingChange(final LocalPendingChange pcEntry) {
        Check.notNull(pcEntry, "pcEntry"); //$NON-NLS-1$

        if (pcEntry.isCommitted()) {
            return getByServerItem(pcEntry.getCommittedServerItem(), true);
        } else {
            return getByServerItem(pcEntry.getTargetServerItem(), false);
        }
    }

    /**
     * Given a pending change, returns the associated local version entry.
     *
     * @param pc
     *        Pending change to look up
     * @return Local version entry for the given pending change
     */
    public WorkspaceLocalItem getByPendingChange(final PendingChange pc) {
        Check.notNull(pc, "pc"); //$NON-NLS-1$

        if (pc.isAdd() || pc.isBranch()) {
            return getByServerItem(pc.getServerItem(), false);
        } else {
            final String source = pc.getSourceServerItem();
            final String serverItem = (source == null || source.length() == 0) ? pc.getServerItem() : source;

            return getByServerItem(serverItem, true);
        }
    }

    /**
     * Given a GetOperation, returns the associated local version entry.
     *
     * @param getOp
     * @return Local version entry for the given GetOperation
     */
    public WorkspaceLocalItem getByGetOperation(final GetOperation getOp) {
        Check.notNull(getOp, "getOp"); //$NON-NLS-1$

        final ChangeType change = getOp.getChangeType();
        if (change.contains(ChangeType.ADD) || change.contains(ChangeType.BRANCH)) {
            return getByServerItem(getOp.getTargetServerItem(), false);
        } else {
            return getByServerItem(getOp.getSourceServerItem(), true);
        }
    }

    /**
     * Returns those items in the local version table which have a local item
     * but have no parent in the local item tree.
     *
     *
     * @return The set of matching local version entries
     */
    public Iterable<WorkspaceLocalItem> queryLocalItemRoots() {
        return local.EnumRootsReferencedObjects();
    }

    /**
     * Given a local item, a recursion type, and a pattern (can be null), return
     * the set of matching local version entries. All deleted items are removed
     * from the result.
     *
     *
     * @param localItem
     *        Local item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @return The set of matching local version entries
     */
    public Iterable<WorkspaceLocalItem> queryByLocalItem(
        final String localItem,
        final RecursionType recursion,
        final String pattern) {
        return queryByLocalItem(localItem, recursion, pattern, false);
    }

    /**
     * Given a local item, a recursion type, and a pattern (can be null), return
     * the set of matching local version entries.
     *
     *
     * @param localItem
     *        Local item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @param includeDeleted
     *        If true, result will include items removed from disk dues to
     *        pending change operation
     * @return The set of matching local version entries
     */
    public Iterable<WorkspaceLocalItem> queryByLocalItem(
        final String localItem,
        final RecursionType recursion,
        final String pattern,
        final boolean includeDeleted) {
        return new WorkspaceLocalItemEnumerable(this, recursion, localItem, pattern, includeDeleted);
    }

    /**
     * Given a server item, a recursion type, and a pattern (can be null),
     * return the set of matching local version entries. The local version
     * entries returned include both committed and uncommitted items. All
     * deleted items are removed from the result.
     *
     *
     * @param serverItem
     *        Server item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @return The set of matching local version entries
     */
    public Iterable<WorkspaceLocalItem> queryByServerItem(
        final String serverItem,
        final RecursionType recursion,
        final String pattern) {
        return queryByServerItem(serverItem, recursion, pattern, CommittedState.BOTH, false);
    }

    /**
     * Given a server item, a recursion type, and a pattern (can be null),
     * return the set of matching local version entries. The local version
     * entries returned include both committed and uncommitted items.
     *
     *
     * @param serverItem
     *        Server item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @param includeDeleted
     *        If true, result will include items removed from disk dues to
     *        pending change operation
     * @return The set of matching local version entries
     */
    public Iterable<WorkspaceLocalItem> queryByServerItem(
        final String serverItem,
        final RecursionType recursion,
        final String pattern,
        final boolean includeDeleted) {
        return queryByServerItem(serverItem, recursion, pattern, CommittedState.BOTH, includeDeleted);
    }

    /**
     * Given a server item, a recursion type, and a pattern (can be null),
     * return the set of matching local version entries. You specify whether to
     * return uncommitted items only,committed items only, or both.
     *
     *
     * @param serverItem
     *        Server item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @param committedState
     * @param includeDeleted
     *        If true, result will include items removed from disk dues to
     *        pending change operation
     * @return The set of matching local version entries
     */
    public Iterable<WorkspaceLocalItem> queryByServerItem(
        final String serverItem,
        final RecursionType recursion,
        final String pattern,
        final CommittedState committedState,
        final boolean includeDeleted) {
        Check.notNullOrEmpty(serverItem, "serverItem"); //$NON-NLS-1$
        return new WorkspaceLocalItemEnumerable(this, recursion, serverItem, committedState, pattern, includeDeleted);
    }

    /**
     * Removes the item with the specified local item from the table.
     *
     *
     * @param localItem
     *        The local item to remove from the table.
     * @param queueForReconcile
     */
    public void removeByLocalItem(final String localItem, final boolean queueForReconcile) {
        if (localItem != null && localItem.length() > 0) {
            final WorkspaceLocalItem lvEntry = local.get(localItem);
            if (lvEntry != null) {
                setDirty(true);

                // Remove from the index we used to look up the data
                local.remove(localItem, false);

                // Remove from the other indexes
                removeFromServerIndex(lvEntry.getServerItem(), lvEntry.isCommitted());

                if (queueForReconcile) {
                    queueForReconcile(lvEntry);
                }
            }
        }
    }

    /**
     * Removes the item with the specified (ServerItem, IsCommitted) pair from
     * the table.
     *
     *
     * @param serverItem
     *        Server item to remove from the table
     * @param isCommitted
     *        IsCommitted bit for the server item to remove from the table
     * @param queueForReconcile
     */
    public void removeByServerItem(
        final String serverItem,
        final boolean isCommitted,
        final boolean queueForReconcile) {
        final WorkspaceLocalItem lvEntry = getByServerItem(serverItem, isCommitted);

        if (null != lvEntry) {
            setDirty(true);

            // Remove from the index we used to look up the data
            removeFromServerIndex(serverItem, isCommitted);

            // Remove from the other indexes
            if (lvEntry.getLocalItem() != null && lvEntry.getLocalItem().length() > 0) {
                local.remove(lvEntry.getLocalItem(), false);
            }

            if (queueForReconcile) {
                queueForReconcile(lvEntry);
            }
        }
    }

    private void queueForReconcile(final WorkspaceLocalItem lvEntry) {
        // we need to clone this entry and then add it to our removed list
        final WorkspaceLocalItem removed = lvEntry.clone();

        removed.setPendingReconcile(true);
        removed.setDeleted(false);
        removed.setLocalItem(null);

        pendingReconcileCount++;
        removedItems.add(removed);
    }

    /**
     * Given a (ServerItem, IsCommitted) pair, removes the entry from the
     * primary key only.
     *
     *
     * @param serverItem
     * @param isCommitted
     */
    private void removeFromServerIndex(final String serverItem, final boolean isCommitted) {
        final WorkspaceLocalItemPair pair = server.get(serverItem);

        if (pair != null) {
            if (isCommitted) {
                if (null != pair.getCommitted() && pair.getCommitted().isPendingReconcile()) {
                    pendingReconcileCount--;
                }

                pair.setCommitted(null);
            } else {
                if (null != pair.getUncommitted() && pair.getUncommitted().isPendingReconcile()) {
                    pendingReconcileCount--;
                }

                pair.setUncommitted(null);
            }

            if (null == pair.getCommitted() && null == pair.getUncommitted()) {
                server.remove(serverItem, false);
            } else {
                server.add(serverItem, pair, true);
            }
        }
    }

    /**
     * Locates a WorkspaceLocalItem by (ServerItem, IsCommitted) and marks it as
     * deleted. Deleted items have no local item.
     *
     *
     * @param serverItem
     * @param isCommitted
     * @param pendingReconcile
     */
    public void markAsDeleted(final String serverItem, final boolean isCommitted, final boolean pendingReconcile) {
        final WorkspaceLocalItem lvEntry = getByServerItem(serverItem, isCommitted);

        if (null != lvEntry) {
            setDirty(true);

            lvEntry.setDeleted(true);
            lvEntry.setMissingOnDisk(false);

            if (pendingReconcile && !lvEntry.isPendingReconcile()) {
                pendingReconcileCount++;
            }

            lvEntry.setPendingReconcile(lvEntry.isPendingReconcile() || pendingReconcile);

            if (lvEntry.getLocalItem() != null && lvEntry.getLocalItem().length() > 0) {
                local.remove(lvEntry.getLocalItem(), false);
            }

            lvEntry.setLocalItem(null);
        }
    }

    public ServerItemLocalVersionUpdate[] getUpdatesForReconcile(
        final LocalPendingChange[] pendingChanges,
        final boolean reconcileMissingOnDisk,
        final AtomicBoolean outClearLocalVersionTable) {
        // Start out by presuming we are going to clear the local version table
        outClearLocalVersionTable.set(true);

        final Set<ServerItemLocalVersionUpdate> updates = new HashSet<ServerItemLocalVersionUpdate>();

        // Add an update for every row in the table which is marked
        // PendingReconcile.
        final Iterable<WorkspaceLocalItemPair> pairs = server.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE);

        for (final WorkspaceLocalItemPair pair : pairs) {
            if (pair.getCommitted() != null) {
                if (outClearLocalVersionTable.get() && !pair.getCommitted().isPendingReconcile()) {
                    // There's a row in our local table which has been
                    // reconciled. So this is not a recovery situation where the
                    // local version table has been lost locally. Don't clear
                    // the local version table as a part of this reconcile.
                    outClearLocalVersionTable.set(false);
                }

                final ServerItemLocalVersionUpdate update =
                    pair.getCommitted().getLocalVersionUpdate(reconcileMissingOnDisk);

                if (null != update) {
                    updates.add(update);
                }
            }

            if (pair.getUncommitted() != null) {
                if (outClearLocalVersionTable.get() && !pair.getUncommitted().isPendingReconcile()) {
                    // There's a row in our local table which has been
                    // reconciled. So this is not a recovery situation where the
                    // local version table has been lost locally. Don't clear
                    // the local version table as a part of this reconcile.
                    outClearLocalVersionTable.set(false);
                }

                final ServerItemLocalVersionUpdate update =
                    pair.getUncommitted().getLocalVersionUpdate(reconcileMissingOnDisk);

                if (null != update) {
                    updates.add(update);
                }
            }
        }

        // Next, add an update for every item which is in the removed items
        // list.
        for (final WorkspaceLocalItem lvEntry : removedItems) {
            // Make sure that the item has not been resurrected in the local
            // version table.
            if (null == getByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted())) {
                updates.add(lvEntry.getLocalVersionUpdate());
            }
        }

        // For safety, always enqueue local version updates for pending changes.
        for (final LocalPendingChange pc : pendingChanges) {
            final WorkspaceLocalItem lvEntry = getByServerItem(
                pc.isCommitted() ? pc.getCommittedServerItem() : pc.getTargetServerItem(),
                pc.isCommitted());

            if (lvEntry != null) {
                if (!lvEntry.isPendingReconcile()) {
                    lvEntry.setPendingReconcile(true);
                    pendingReconcileCount++;
                }

                updates.add(lvEntry.getLocalVersionUpdate(reconcileMissingOnDisk));
            }
        }

        return updates.toArray(new ServerItemLocalVersionUpdate[updates.size()]);

    }

    /**
     * Returns every server item held by this WorkspaceVersionTable. The set may
     * contain duplicates.
     */
    public List<String> getKnownServerItems() {
        final List<String> knownServerItems = new ArrayList<String>();

        for (final WorkspaceLocalItemPair pair : server.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE)) {
            knownServerItems.add(pair.getServerItem());
        }

        for (final WorkspaceLocalItem lvEntry : removedItems) {
            knownServerItems.add(lvEntry.getServerItem());
        }

        return knownServerItems;
    }

    /**
     * Performs a team project rename on this WorkspaceVersionTable, using the
     * provided server item mapping function.
     *
     * @param serverItemMapper
     *        A class instance that implements a function which maps old server
     *        paths to new server paths
     */
    public void renameTeamProjects(final ServerItemMapper serverItemMapper) {
        int newPendingReconcileCount = 0;

        setDirty(true);

        local.clear();

        final List<WorkspaceLocalItemPair> pairs = new ArrayList<WorkspaceLocalItemPair>(server.getCount());

        for (final WorkspaceLocalItemPair pair : server.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE)) {
            WorkspaceLocalItem committed = null;
            WorkspaceLocalItem uncommitted = null;

            if (pair.getCommitted() != null) {
                committed = pair.getCommitted().clone();
                committed.setServerItem(serverItemMapper.map(committed.getServerItem()));
                committed.setPendingReconcile(true);

                if (committed.getLocalItem() != null) {
                    local.add(committed.getLocalItem(), committed);
                }

                newPendingReconcileCount++;
            }

            if (pair.getUncommitted() != null) {
                uncommitted = pair.getUncommitted().clone();
                uncommitted.setServerItem(serverItemMapper.map(uncommitted.getServerItem()));
                uncommitted.setPendingReconcile(true);

                if (uncommitted.getLocalItem() != null) {
                    local.add(uncommitted.getLocalItem(), uncommitted);
                }

                newPendingReconcileCount++;
            }

            final WorkspaceLocalItemPair newPair = new WorkspaceLocalItemPair();

            newPair.setCommitted(committed);
            newPair.setUncommitted(uncommitted);

            pairs.add(newPair);
        }

        server.clear();

        for (final WorkspaceLocalItemPair pair : pairs) {
            server.add(pair.getServerItem(), pair);
        }

        final List<WorkspaceLocalItem> newRemovedItems = new ArrayList<WorkspaceLocalItem>();

        for (final WorkspaceLocalItem lvEntry : newRemovedItems) {
            final WorkspaceLocalItem renamedEntry = lvEntry.clone();
            renamedEntry.setServerItem(serverItemMapper.map(renamedEntry.getServerItem()));
            newRemovedItems.add(renamedEntry);

            newPendingReconcileCount++;
        }

        removedItems = newRemovedItems;
        pendingReconcileCount = newPendingReconcileCount;
    }

    public int getLocalItemsCount() {
        return local.getCount();
    }

    public boolean getPendingReconcile() {
        return pendingReconcileCount > 0;
    }
}
