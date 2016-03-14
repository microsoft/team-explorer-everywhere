// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.LocalPendingChangeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.InvalidPendingChangeTableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayerLocalWorkspaces;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumParentsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumSubTreeOptions;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.EnumeratedSparseTreeNode;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree.EnumNodeCallback;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTreeAdditionalData;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.datetime.DotNETDate;

public class LocalPendingChangesTable extends LocalMetadataTable {
    private static final short MAGIC = 0x7425;
    private static final byte[] EMPTY_HASH = GUID.EMPTY.getGUIDBytes();
    private static final byte[] ZERO_LENGTH_ARRAY_BYTES = new byte[0];

    private static final byte SCHEMA_VERSION_1 = 1;
    private static final byte SCHEMA_VERSION_2 = 2;

    private GUID baseSignature;
    private GUID clientSignature;
    private boolean hasRenames;
    private SparseTree<LocalPendingChange> pendingChangesTarget;
    private SparseTree<LocalPendingChange> pendingChangesCommitted;
    private SparseTree<LocalPendingChange> pendingChangesCandidateTarget;

    public LocalPendingChangesTable(final String fileName, final LocalPendingChangesTable cachedLoadSource)
        throws Exception {
        super(fileName, cachedLoadSource);
        /* Don't do anything here, Initialize() runs first */
    }

    @Override
    protected void initialize() {
        hasRenames = false;

        pendingChangesTarget =
            new SparseTree<LocalPendingChange>(ServerPath.PREFERRED_SEPARATOR_CHARACTER, String.CASE_INSENSITIVE_ORDER);

        pendingChangesCommitted =
            new SparseTree<LocalPendingChange>(ServerPath.PREFERRED_SEPARATOR_CHARACTER, String.CASE_INSENSITIVE_ORDER);

        pendingChangesCandidateTarget =
            new SparseTree<LocalPendingChange>(ServerPath.PREFERRED_SEPARATOR_CHARACTER, String.CASE_INSENSITIVE_ORDER);

        baseSignature = WebServiceLayerLocalWorkspaces.INITIAL_PENDING_CHANGES_SIGNATURE;
        clientSignature = WebServiceLayerLocalWorkspaces.INITIAL_PENDING_CHANGES_SIGNATURE;
    }

    @Override
    protected void load(final InputStream is) throws InvalidPendingChangeTableException, IOException {
        final BinaryReader br = new BinaryReader(is, "UTF-16LE"); //$NON-NLS-1$
        try {
            final short magic = br.readInt16();

            if (MAGIC != magic) {
                throw new InvalidPendingChangeTableException();
            }

            final byte schemaVersion = br.readByte();
            if (schemaVersion == SCHEMA_VERSION_1 || schemaVersion == SCHEMA_VERSION_2) {
                loadFromVersion(br, schemaVersion);
            } else {
                throw new InvalidPendingChangeTableException();
            }
        } catch (final Exception e) {
            if (e instanceof InvalidPendingChangeTableException) {
                throw (InvalidPendingChangeTableException) e;
            } else {
                // Wrap the exception
                throw new InvalidPendingChangeTableException(e);
            }
        } finally {
            br.close();
        }
    }

    private void loadFromVersion(final BinaryReader br, final byte schemaVersion)
        throws InvalidPendingChangeTableException,
            IOException {
        final byte[] clientSignatureBytes = br.readBytes(16);

        clientSignature = new GUID(clientSignatureBytes);
        baseSignature = clientSignature;

        final int pendingChangeCount = br.readInt32();

        while (!br.isEOF()) {
            final LocalPendingChange pc = new LocalPendingChange();

            pc.setTargetServerItem(br.readString());
            pc.setCommittedServerItem(br.readString());

            if (0 == pc.getCommittedServerItem().length()) {
                pc.setCommittedServerItem(null);
            }

            pc.setBranchFromItem(br.readString());

            if (0 == pc.getBranchFromItem().length()) {
                pc.setBranchFromItem(null);
            }

            pc.setVersion(br.readInt32());
            pc.setBranchFromVersion(br.readInt32());
            pc.setChangeType(ChangeType.fromIntFlags(br.readUInt32()));
            pc.setItemType(ItemType.fromByteValue(br.readByte()));
            pc.setEncoding(br.readInt32());
            pc.setLockStatus(br.readByte());
            pc.setItemID(br.readInt32());
            pc.setCreationDate(DotNETDate.fromBinary(br.readInt64()));
            pc.setDeletionID(br.readInt32());

            if (ItemType.FILE == pc.getItemType()) {
                pc.setHashValue(br.readBytes(16));

                if (Arrays.equals(pc.getHashValue(), EMPTY_HASH)) {
                    pc.setHashValue(ZERO_LENGTH_ARRAY_BYTES);
                }
            }

            // v1 did not support this flag.
            if (schemaVersion != SCHEMA_VERSION_1) {
                pc.setFlags(new LocalPendingChangeFlags(br.readByte()));
            }

            if (!pc.isCandidate()) {
                pendingChangesTarget.add(pc.getTargetServerItem(), pc, true);

                final String committedItem = pc.getCommittedServerItem();
                if (committedItem != null && committedItem.length() > 0 && pc.isCommitted()) {
                    pendingChangesCommitted.add(committedItem, pc, true);
                }

                if (pc.isRename()) {
                    hasRenames = true;
                }
            } else {
                pendingChangesCandidateTarget.add(pc.getTargetServerItem(), pc, true);
            }
        }

        if (pendingChangesTarget.getCount() != pendingChangeCount) {
            throw new InvalidPendingChangeTableException();
        }
    }

    @Override
    protected boolean cachedLoad(final LocalMetadataTable source) {
        if (source instanceof LocalPendingChangesTable) {
            final LocalPendingChangesTable pcCached = (LocalPendingChangesTable) source;

            this.clientSignature = pcCached.clientSignature;
            this.baseSignature = pcCached.clientSignature;

            // TODO: This is not going to work well
            this.hasRenames = pcCached.hasRenames;

            this.pendingChangesCommitted = pcCached.pendingChangesCommitted;
            this.pendingChangesTarget = pcCached.pendingChangesTarget;
            this.pendingChangesCandidateTarget = pcCached.pendingChangesCandidateTarget;

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
            writeToVersion1(bw);
        } finally {
            bw.close();
        }

        return true;
    }

    private void writeToVersion1(final BinaryWriter bw) throws IOException {
        bw.write(SCHEMA_VERSION_2);

        updateClientSignatureIfNecessary();

        // Unique signature for this set of pending changes
        bw.write(clientSignature.getGUIDBytes());

        // Number of pending changes stored in the table
        bw.write(pendingChangesTarget.getCount());

        pendingChangesTarget.EnumSubTree(
            ServerPath.ROOT,
            new SaveCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE,
            null,
            bw);

        pendingChangesCandidateTarget.EnumSubTree(
            ServerPath.ROOT,
            new SaveCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE,
            null,
            bw);
    }

    /**
     * Returns true if the local version entry provided has a pending change on
     * it, or if a pending change exists on a child item.
     *
     *
     * @param lvEntry
     *        Local version entry to check
     * @return
     */
    public boolean hasSubItemOfLocalVersion(final WorkspaceLocalItem lvEntry) {
        String targetServerItem;

        if (lvEntry.isCommitted()) {
            targetServerItem = getTargetServerItemForCommittedServerItem(lvEntry.getServerItem());
        } else {
            targetServerItem = lvEntry.getServerItem();
        }

        return hasSubItemOfTargetServerItem(targetServerItem);
    }

    /**
     * Returns true if the target server item provided has a pending change on
     * it, or if a pending change exists on a child item.
     *
     *
     * @param targetServerItem
     *        Target server item to check
     * @return
     */
    public boolean hasSubItemOfTargetServerItem(final String targetServerItem) {
        final EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);

        // With depth 0, enumerate the item in question from the tree. By
        // requesting the tree to enumerate sparse nodes, we will get a callback
        // for the token if we have a pending change OR if we have a pending
        // change on any subitem. The callback function will return true (halt
        // the enumeration) if it is called. This will cause this function to
        // return true. If the callback is not called, the enumeration will
        // complete (it enumerated nothing) and we will return false.

        return pendingChangesTarget.EnumSubTree(targetServerItem, new HasSubItemCallback(), options, 0, null, null);
    }

    class HasSubItemCallback implements EnumNodeCallback<LocalPendingChange> {
        @Override
        public boolean invoke(
            final String token,
            final LocalPendingChange referencedObject,
            final SparseTreeAdditionalData additionalData,
            final java.lang.Object param) {
            // The callback function will return true (halt the enumeration) if
            // it is called. This will cause this function to return true. If
            // the callback is not called, the enumeration will complete (it
            // enumerated nothing) and we will return false.
            return true;
        }
    }

    /**
     * Given a target server item, returns the pending change associated with
     * the item. If the item does not have a pending change, null is returned.
     *
     *
     * @param targetServerItem
     *        The target server item to look up
     * @return The pending change associated with the target server item
     *         provided
     */
    public LocalPendingChange getByTargetServerItem(final String targetServerItem) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$
        return pendingChangesTarget.get(targetServerItem);
    }

    /**
     * Given a committed server item, returns the pending change associated with
     * the item. If the item does not have a pending change, null is returned.
     *
     *
     * @param committedServerItem
     *        The committed server item to look up
     * @return The pending change associated with the committed server item
     *         provided
     */
    public LocalPendingChange getByCommittedServerItem(final String committedServerItem) {
        Check.notNullOrEmpty(committedServerItem, "committedServerItem"); //$NON-NLS-1$
        return pendingChangesCommitted.get(committedServerItem);
    }

    /**
     * Given a local version entry, returns the pending change associated with
     * the item. If the item does not have a pending change, null is returned.
     *
     *
     * @param lvEntry
     *        The local version entry to look up
     * @return The pending change associated with the local version entry
     *         provided
     */
    public LocalPendingChange getByLocalVersion(final WorkspaceLocalItem lvEntry) {
        Check.notNull(lvEntry, "lvEntry"); //$NON-NLS-1$

        if (lvEntry.isCommitted()) {
            return getByCommittedServerItem(lvEntry.getServerItem());
        } else {
            return getByTargetServerItem(lvEntry.getServerItem());
        }
    }

    /**
     * Given a target server item, reverses the server item back through the
     * workspace's pending renames to give the committed server item.
     *
     *
     * @param targetServerItem
     *        Target server item to translate
     * @return Corresponding committed server item
     */
    public String getCommittedServerItemForTargetServerItem(final String targetServerItem) {
        if (hasRenames) {
            final LocalPendingChange rename = getFirstRenameForTargetItem(targetServerItem);
            if (rename == null) {
                return targetServerItem;
            } else {
                return rename.getCommittedServerItem()
                    + targetServerItem.substring(rename.getTargetServerItem().length());
            }
        } else {
            return targetServerItem;
        }
    }

    /**
     * Given a committed server item, forwards the server item through the
     * workspace's pending renames to give the target server item.
     *
     *
     * @param committedServerItem
     *        Committed server item to translate
     * @return Corresponding target server item
     */
    public String getTargetServerItemForCommittedServerItem(final String committedServerItem) {
        if (hasRenames) {
            final LocalPendingChange rename = getFirstRenameForCommitedItem(committedServerItem);
            if (rename == null) {
                return committedServerItem;
            } else {
                return rename.getTargetServerItem()
                    + committedServerItem.substring(rename.getCommittedServerItem().length());
            }
        } else {
            return committedServerItem;
        }
    }

    /**
     * Given a local version, returns the target server item.
     *
     *
     * @param lvEntry
     *        Local version
     * @return Target server item for the local version
     */
    public String getTargetServerItemForLocalVersion(final WorkspaceLocalItem lvEntry) {
        if (lvEntry.isCommitted()) {
            return getTargetServerItemForCommittedServerItem(lvEntry.getServerItem());
        } else {
            return lvEntry.getServerItem();
        }
    }

    private LocalPendingChange getFirstRenameForCommitedItem(final String committedServerItem) {
        final GetFirstRenameState state = new GetFirstRenameState();

        pendingChangesCommitted.EnumParents(
            committedServerItem,
            new GetFirstRenameCallback(),
            EnumParentsOptions.NONE,
            null,
            state);

        return state.renamedParent;
    }

    private LocalPendingChange getFirstRenameForTargetItem(final String targetServerItem) {
        final GetFirstRenameState state = new GetFirstRenameState();

        pendingChangesTarget.EnumParents(
            targetServerItem,
            new GetFirstRenameCallback(),
            EnumParentsOptions.NONE,
            null,
            state);

        return state.renamedParent;
    }

    /**
     * Class to pass parameters to GetFirstRenameCallback
     */
    private class GetFirstRenameState {
        public LocalPendingChange renamedParent;
    }

    /**
     * SparseTree callback for GetFirstRenamedParent family of methods.
     */
    private class GetFirstRenameCallback implements EnumNodeCallback<LocalPendingChange> {
        @Override
        public boolean invoke(
            final String token,
            final LocalPendingChange pc,
            final SparseTreeAdditionalData additionalData,
            final Object param) {
            final GetFirstRenameState state = (GetFirstRenameState) param;
            if (pc.isRename()) {
                state.renamedParent = pc;
                return true;
            }
            return false;
        }
    }

    /**
     * Given a local version entry, return the union of: 1. The full ChangeType
     * on the item specified. 2. The union of all the recursive ChangeTypes on
     * parents of the item specified, up to $/. Recursive changetypes are rename
     * and delete.
     *
     * @param lvEntry
     *        Local version entry
     * @return The recursive ChangeType for the local version entry specified
     */
    public ChangeType getRecursiveChangeTypeForLocalVersion(final WorkspaceLocalItem lvEntry) {
        Check.notNull(lvEntry, "lvEntry"); //$NON-NLS-1$

        String targetServerItem;

        if (lvEntry.isCommitted()) {
            targetServerItem = getTargetServerItemForCommittedServerItem(lvEntry.getServerItem());
        } else {
            targetServerItem = lvEntry.getServerItem();
        }

        return getRecursiveChangeTypeForTargetServerItem(targetServerItem);
    }

    /**
     * Given a target server item, return the union of: 1. The full ChangeType
     * on the item specified. 2. The union of all the recursive ChangeTypes on
     * parents of the item specified, up to $/. Recursive changetypes are rename
     * and delete.
     *
     * @param targetServerItem
     *        Target server item
     * @return The recursive ChangeType for the target server item // specified
     */
    public ChangeType getRecursiveChangeTypeForTargetServerItem(final String targetServerItem) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        final ChangeType inheritedChangeType = getInheritedChangeTypeForTargetServerItem(targetServerItem);

        final LocalPendingChange pcEntry = getByTargetServerItem(targetServerItem);

        if (null == pcEntry) {
            return inheritedChangeType;
        } else if (ChangeType.NONE == inheritedChangeType) {
            return pcEntry.getChangeType();
        } else {
            return pcEntry.getChangeType().combine(inheritedChangeType);
        }
    }

    /**
     * Given a target server item, return the union of all the recursive
     * ChangeTypes on parents of the item specified, up to $/. Recursive
     * changetypes are rename and delete. The changetype of the target server
     * item specified is not returned.
     *
     *
     * @param targetServerItem
     *        Target server item
     * @return The recursive ChangeType for the target server item specified
     */
    public ChangeType getInheritedChangeTypeForTargetServerItem(final String targetServerItem) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        ChangeType changeType = ChangeType.NONE;

        final Iterable<LocalPendingChange> pcEntries =
            pendingChangesTarget.EnumParentsReferencedObjects(targetServerItem, EnumParentsOptions.NONE);

        for (final LocalPendingChange pcEntry : pcEntries) {
            if (!pcEntry.getChangeType().equals(ChangeType.NONE)) {
                changeType = changeType.combine(pcEntry.getChangeType().retain(ChangeType.RENAME_OR_DELETE));
            }
        }

        return changeType;
    }

    /**
     * Given a target server item, returns the parents of that target server
     * item which have pending changes. If the target server item itself has a
     * pending change, that pending change is not returned as part of the result
     * set. The result set is ordered, with the closest pending change to the
     * target server item being returned first.
     *
     *
     * @param targetServerItem
     *        Target server item for which parent pending changes should be
     *        queried
     * @return The set of pending changes on items which parent the target
     *         server item.
     */
    public Iterable<LocalPendingChange> queryParentsOfTargetServerItem(final String targetServerItem) {
        return pendingChangesTarget.EnumParentsReferencedObjects(targetServerItem, EnumParentsOptions.NONE);
    }

    /**
     * Given a target server item, a recursion type, and a pattern (can be
     * null), return the set of matching pending changes.
     *
     * @param targetServerItem
     *        Target server item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @return The set of matching pending changes
     */
    public LocalPendingChange[] queryByTargetServerItem(
        final String targetServerItem,
        final RecursionType recursion,
        final String pattern) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        final QueryState qpcState = new QueryState();
        qpcState.matchingItems = new ArrayList<LocalPendingChange>();
        qpcState.pattern = pattern;

        // When matching a pattern, do not enumerate the root of the query.
        EnumSubTreeOptions options = EnumSubTreeOptions.NONE;
        if (pattern == null) {
            options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT;
        }

        pendingChangesTarget.EnumSubTree(
            targetServerItem,
            new QueryCallback(),
            options,
            depthFromRecursionType(recursion),
            null,
            qpcState);

        return qpcState.matchingItems.toArray(new LocalPendingChange[qpcState.matchingItems.size()]);
    }

    /**
     * Given a committed server item, a recursion type, and a pattern (can be
     * null), return the set of matching pending changes.
     *
     *
     * @param committedServerItem
     *        Committed server item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @return The set of matching pending changes
     */
    public Iterable<LocalPendingChange> queryByCommittedServerItem(
        final String committedServerItem,
        final RecursionType recursion,
        final String pattern) {
        Check.notNullOrEmpty(committedServerItem, "committedServerItem"); //$NON-NLS-1$

        final QueryState qpcState = new QueryState();
        qpcState.matchingItems = new ArrayList<LocalPendingChange>();
        qpcState.pattern = pattern;

        // When matching a pattern, do not enumerate the root of the query.
        EnumSubTreeOptions options = EnumSubTreeOptions.NONE;
        if (pattern == null) {
            options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT;
        }

        pendingChangesCommitted.EnumSubTree(
            committedServerItem,
            new QueryCallback(),
            options,
            depthFromRecursionType(recursion),
            null,
            qpcState);

        return qpcState.matchingItems;
    }

    /**
     * Given a RecursionType, return the depth to use for a SparseTree
     * EnumSubTree call.
     */
    private static int depthFromRecursionType(final RecursionType recursion) {
        if (recursion == RecursionType.NONE) {
            return 0;
        } else if (recursion == RecursionType.ONE_LEVEL) {
            return 1;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * State passed to QueryCallback method.
     */
    private class QueryState {
        public List<LocalPendingChange> matchingItems;
        public String pattern;
    }

    /**
     * SparseTree callback for QueryBy family of methods.
     */
    private class QueryCallback implements EnumNodeCallback<LocalPendingChange> {
        @Override
        public boolean invoke(
            final String token,
            final LocalPendingChange pcEntry,
            final SparseTreeAdditionalData additionalData,
            final Object param) {
            final QueryState state = (QueryState) param;

            if (null == state.pattern || ItemPath.matchesWildcardFile(ServerPath.getFileName(token), state.pattern)) {
                state.matchingItems.add(pcEntry);
            }

            return false;
        }

    }

    /**
     * Adds pending change to the table. Does not modify any other existing
     * pending changes (e.g. sub items)
     *
     *
     * @param change
     */
    public void pendChange(final LocalPendingChange change) {
        Check.notNull(change, "change"); //$NON-NLS-1$

        setDirty(true);
        pendingChangesTarget.set(change.getTargetServerItem(), change);

        final String committed = change.getCommittedServerItem();
        if (committed != null && committed.length() > 0 && change.isCommitted()) {
            pendingChangesCommitted.set(committed, change);
        }

        if (change.isRename()) {
            hasRenames = true;
        }
    }

    /**
     * Removes pending change from the table. Does not modify any other existing
     * pending changes (e.g. sub items)
     *
     *
     * @param change
     */
    public void remove(final LocalPendingChange change) {
        Check.notNull(change, "change"); //$NON-NLS-1$

        setDirty(true);
        pendingChangesTarget.remove(change.getTargetServerItem(), false);

        final String committed = change.getCommittedServerItem();
        if (committed != null && committed.length() > 0) {
            pendingChangesCommitted.remove(committed, false);
        }
    }

    /**
     * Given a target server item, removes the pending change on the item, if
     * one exists.
     *
     *
     * @param targetServerItem
     *        Target server item to remove
     * @return True if a pending change was removed
     */
    public boolean removeByTargetServerItem(final String targetServerItem) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        final LocalPendingChange pcEntry = pendingChangesTarget.get(targetServerItem);
        if (pcEntry != null) {
            setDirty(true);
            pendingChangesTarget.remove(pcEntry.getTargetServerItem(), false);

            final String committed = pcEntry.getCommittedServerItem();
            if (committed != null && committed.length() > 0) {
                pendingChangesCommitted.remove(committed, false);
            }

            return true;
        }

        return false;
    }

    public void replacePendingChanges(final PendingChange[] newPendingChanges) {
        setDirty(true);

        pendingChangesCommitted.clear();
        pendingChangesTarget.clear();
        hasRenames = false;

        for (final PendingChange newPc : newPendingChanges) {
            pendChange(LocalPendingChange.fromPendingChange(newPc));
        }
    }

    /**
     * Special ReplacePendingChanges call for
     * LocalDataAccessLayer.SyncPendingChanges()
     */
    public void replacePendingChanges(final SparseTree<LocalPendingChange> pendingChangesTarget) {
        setDirty(true);

        this.pendingChangesTarget = pendingChangesTarget;
        this.pendingChangesCommitted.clear();

        hasRenames = false;

        pendingChangesTarget.EnumSubTree(null, new ReplaceCallback());
    }

    class ReplaceCallback implements EnumNodeCallback<LocalPendingChange> {
        @Override
        public boolean invoke(
            final String token,
            final LocalPendingChange pcEntry,
            final SparseTreeAdditionalData additionalData,
            final Object param) {
            pendChange(pcEntry);
            return false;
        }
    }

    /**
     * Given a target server item, a recursion type, and a pattern (can be
     * null), return the set of candidate matching pending changes.
     *
     *
     * @param targetServerItem
     *        Target server item at which to start the query
     * @param recursion
     *        Recursion level to use for the query
     * @param pattern
     *        A pattern to require for matching (optional)
     * @return The set of matching pending changes
     */
    public Iterable<LocalPendingChange> queryCandidatesByTargetServerItem(
        final String targetServerItem,
        final RecursionType recursion,
        final String pattern) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        final QueryState qpcState = new QueryState();
        qpcState.matchingItems = new ArrayList<LocalPendingChange>();
        qpcState.pattern = pattern;

        // When matching a pattern, do not enumerate the root of the query.
        EnumSubTreeOptions options = EnumSubTreeOptions.NONE;
        if (pattern == null) {
            options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT;
        }

        pendingChangesCandidateTarget.EnumSubTree(
            targetServerItem,
            new QueryCallback(),
            options,
            depthFromRecursionType(recursion),
            null,
            qpcState);

        return qpcState.matchingItems;
    }

    public void addCandidate(final LocalPendingChange pendingChange) {
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$
        Check.isTrue(pendingChange.isCandidate(), "cannot call add with non-candidate pending change"); //$NON-NLS-1$

        if (pendingChange.isDelete()) {
            // if we have a delete on a parent skip adding it
            for (final EnumeratedSparseTreeNode<LocalPendingChange> pc : pendingChangesCandidateTarget.EnumParents(
                pendingChange.getTargetServerItem(),
                EnumParentsOptions.NONE)) {
                if (pc.referencedObject.getChangeType().contains(ChangeType.DELETE)) {
                    return;
                }
            }

            if (pendingChange.getItemType() == ItemType.FOLDER) {
                pendingChangesCandidateTarget.remove(pendingChange.getTargetServerItem(), true);
            }
        }

        pendingChangesCandidateTarget.add(pendingChange.getTargetServerItem(), pendingChange, true);
        setDirty(true);
    }

    /**
     * Given a target server item, removes the candidate pending change on the
     * item, if one exists.
     *
     * @param targetServerItem
     *        Target server item to remove
     * @return True if a pending change was removed
     */
    public boolean removeCandidateByTargetServerItem(final String targetServerItem) {
        return removeCandidateByTargetServerItem(targetServerItem, false);
    }

    /**
     * Given a target server item, removes the candidate pending change on the
     * item, if one exists.
     *
     * @param targetServerItem
     *        Target server item to remove
     * @param recursive
     *        True to remove all child candidate pending changes, too
     * @return True if a pending change was removed
     */
    public boolean removeCandidateByTargetServerItem(final String targetServerItem, final boolean recursive) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        if (pendingChangesCandidateTarget.remove(targetServerItem, recursive)) {
            setDirty(true);
            return true;
        }

        return false;
    }

    public LocalPendingChange getCandidateByTargetServerItem(final String targetServerItem) {
        Check.notNullOrEmpty(targetServerItem, "targetServerItem"); //$NON-NLS-1$
        return pendingChangesCandidateTarget.get(targetServerItem);
    }

    /**
     * Returns every server item held by this LocalPendingChangesTable. The set
     * may contain duplicates.
     */
    public List<String> getKnownServerItems() {
        final List<String> knownServerItems = new ArrayList<String>();

        for (final LocalPendingChange pcEntry : pendingChangesTarget.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE)) {
            if (!StringUtil.isNullOrEmpty(pcEntry.getBranchFromItem())) {
                knownServerItems.add(pcEntry.getBranchFromItem());
            }

            if (!StringUtil.isNullOrEmpty(pcEntry.getCommittedServerItem())) {
                knownServerItems.add(pcEntry.getCommittedServerItem());
            }
        }

        for (final LocalPendingChange pcEntry : pendingChangesCandidateTarget.EnumSubTreeReferencedObjects(
            ServerPath.ROOT,
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE)) {
            if (!StringUtil.isNullOrEmpty(pcEntry.getBranchFromItem())) {
                knownServerItems.add(pcEntry.getBranchFromItem());
            }

            if (!StringUtil.isNullOrEmpty(pcEntry.getCommittedServerItem())) {
                knownServerItems.add(pcEntry.getCommittedServerItem());
            }
        }

        return knownServerItems;
    }

    /**
     * Performs a team project rename on this WorkspaceVersionTable, using the
     * provided server item mapping function
     *
     * @param serverItemMapper
     *        A class instance that implements a function which maps old server
     *        paths to new server paths
     */
    public void renameTeamProjects(final ServerItemMapper serverItemMapper) {
        setDirty(true);
        clientSignature = GUID.newGUID();

        // hasRenames should have no change when this method is executed

        /*
         * Process pendingChangesTarget and pendingChangesCommitted
         */
        {
            pendingChangesCommitted.clear();

            final List<LocalPendingChange> pcEntries =
                new ArrayList<LocalPendingChange>(pendingChangesTarget.getCount());

            for (final LocalPendingChange pcEntry : pendingChangesTarget.EnumSubTreeReferencedObjects(
                ServerPath.ROOT,
                EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
                Integer.MAX_VALUE)) {
                final LocalPendingChange renamedEntry = pcEntry.clone();

                renamedEntry.setTargetServerItem(serverItemMapper.map(renamedEntry.getTargetServerItem()));

                if (!StringUtil.isNullOrEmpty(renamedEntry.getBranchFromItem())) {
                    renamedEntry.setBranchFromItem(serverItemMapper.map(renamedEntry.getBranchFromItem()));
                }

                if (!StringUtil.isNullOrEmpty(renamedEntry.getCommittedServerItem())) {
                    renamedEntry.setCommittedServerItem(serverItemMapper.map(renamedEntry.getCommittedServerItem()));

                    if (renamedEntry.isCommitted()) {
                        pendingChangesCommitted.add(renamedEntry.getCommittedServerItem(), renamedEntry);
                    }
                }

                pcEntries.add(renamedEntry);
            }

            pendingChangesTarget.clear();

            for (final LocalPendingChange pcEntry : pcEntries) {
                pendingChangesTarget.add(pcEntry.getTargetServerItem(), pcEntry);
            }
        }

        /*
         * Process pendingChangesCandidateTarget
         */
        {
            final List<LocalPendingChange> pcEntries =
                new ArrayList<LocalPendingChange>(pendingChangesCandidateTarget.getCount());

            for (final LocalPendingChange pcEntry : pendingChangesCandidateTarget.EnumSubTreeReferencedObjects(
                ServerPath.ROOT,
                EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
                Integer.MAX_VALUE)) {
                final LocalPendingChange renamedEntry = pcEntry.clone();

                renamedEntry.setTargetServerItem(serverItemMapper.map(renamedEntry.getTargetServerItem()));

                if (!StringUtil.isNullOrEmpty(renamedEntry.getBranchFromItem())) {
                    renamedEntry.setBranchFromItem(serverItemMapper.map(renamedEntry.getBranchFromItem()));
                }

                if (!StringUtil.isNullOrEmpty(renamedEntry.getCommittedServerItem())) {
                    renamedEntry.setCommittedServerItem(serverItemMapper.map(renamedEntry.getCommittedServerItem()));
                }

                pcEntries.add(renamedEntry);
            }

            pendingChangesCandidateTarget.clear();

            for (final LocalPendingChange pcEntry : pcEntries) {
                pendingChangesCandidateTarget.add(pcEntry.getTargetServerItem(), pcEntry);
            }
        }
    }

    /**
     * Returns true if the pending changes include a rename.
     */
    public boolean hasRenames() {
        return hasRenames;
    }

    /**
     * Returns the number of pending changes.
     */
    public int getCount() {
        return pendingChangesTarget.getCount();
    }

    /**
     * Returns the GUID signature of the pending changes that the client has.
     *
     *
     * @return
     */
    public GUID getClientSignature() {
        updateClientSignatureIfNecessary();
        return clientSignature;
    }

    public void setClientSignature(final GUID value) {
        if (!value.equals(clientSignature)) {
            setDirty(true);
        }

        clientSignature = value;
    }

    /**
     * Generates a new client signature if the table is dirty.
     */
    private void updateClientSignatureIfNecessary() {
        if (isDirty() && baseSignature.equals(clientSignature)) {
            if (pendingChangesTarget.getCount() == 0) {
                clientSignature = WebServiceLayerLocalWorkspaces.INITIAL_PENDING_CHANGES_SIGNATURE;
            } else {
                clientSignature = GUID.newGUID();
            }
        }
    }

    class SaveCallback implements EnumNodeCallback<LocalPendingChange> {
        @Override
        public boolean invoke(
            final String token,
            final LocalPendingChange pc,
            final SparseTreeAdditionalData additionalData,
            final Object param) {
            final BinaryWriter bw = (BinaryWriter) param;

            try {
                bw.write(pc.getTargetServerItem());
                bw.write(pc.getCommittedServerItem() == null ? "" : pc.getCommittedServerItem()); //$NON-NLS-1$
                bw.write(pc.getBranchFromItem() == null ? "" : pc.getBranchFromItem()); //$NON-NLS-1$
                bw.write(pc.getVersion());
                bw.write(pc.getBranchFromVersion());
                bw.write(pc.getChangeType().toIntFlags());
                bw.write(pc.getItemType().getValue());
                bw.write(pc.getEncoding());
                bw.write(pc.getLockStatus());
                bw.write(pc.getItemID());
                bw.write(DotNETDate.toBinary(pc.getCreationDate()));
                bw.write(pc.getDeletionID());

                if (ItemType.FILE == pc.getItemType()) {
                    if (null == pc.getHashValue() || pc.getHashValue().length != 16) {
                        bw.write(EMPTY_HASH);
                    } else {
                        bw.write(pc.getHashValue());
                    }
                }
                bw.write((byte) pc.getFlags().toIntFlags());
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }

            return false;
        }
    }
}
