// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.vc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.QueryMergesExtendedOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetSummary;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * Based on HistoryEnumerator in VS2010
 *
 */
public class HistoryManager {
    /**
     * This method is called only for TFS 2010 servers and therefore uses slot
     * mode to query branches and merges
     *
     */
    public static Changeset[] findChangesetChildren(final TFSRepository repository, final Changeset c) {
        // Slot mode is always true because this code is exercised only for
        // TFS2010
        final boolean slotMode = true;
        if (mayHaveChildren(repository, c)) {
            if (c.getChanges() == null || c.getChanges().length <= 0) {
                return new Changeset[0];
            }
            final ChangeType ctype = c.getChanges()[0].getChangeType();
            if (ctype.contains(ChangeType.BRANCH)) {
                return findBranchPaths(repository, c, slotMode);
            }
            if (ctype.contains(ChangeType.MERGE)) {
                return findMergePaths(repository, c, slotMode);
            }

            if (ctype.contains(ChangeType.RENAME)) {
                return findMergePaths(repository, c, slotMode);
            }
        }
        return null;
    }

    private static Changeset[] findBranchPaths(
        final TFSRepository repository,
        final Changeset c,
        final boolean slotMode) {
        final Change change = c.getChanges()[0];
        final String item = change.getItem().getServerItem();
        final BranchHistory history = repository.getWorkspace().getBranchHistory(
            new ItemSpec(item, RecursionType.NONE),
            new ChangesetVersionSpec(c.getChangesetID()));
        final Item branchFromItem = history.getRequestedItem().getFromItem();
        if (branchFromItem == null) {
            // The user may not have permissions on the item
            return new Changeset[0];
        }
        ArrayList changesets;
        changesets = new ArrayList();
        final ChangesetVersionSpec toVersion = new ChangesetVersionSpec(branchFromItem.getChangeSetID());
        final Iterator iter = repository.getWorkspace().queryHistoryIterator(
            branchFromItem.getServerItem(),
            toVersion,
            0,
            RecursionType.NONE,
            null,
            null,
            toVersion,
            Integer.MAX_VALUE,
            true,
            slotMode,
            false,
            false);

        while (iter.hasNext()) {
            changesets.add(iter.next());
        }
        return (Changeset[]) changesets.toArray(new Changeset[0]);
    }

    private static Changeset[] findMergePaths(
        final TFSRepository repository,
        final Changeset c,
        final boolean slotMode) {
        final Change change = c.getChanges()[0];
        final ItemSpec item = new ItemSpec(change.getItem().getServerItem(), RecursionType.NONE);
        final VersionSpec itemVersion = new ChangesetVersionSpec(c.getChangesetID());

        QueryMergesExtendedOptions options = QueryMergesExtendedOptions.NONE;
        final ChangeType changeType = change.getChangeType();
        if (changeType.contains(ChangeType.RENAME)
            && !changeType.contains(ChangeType.BRANCH)
            && !changeType.contains(ChangeType.MERGE)) {
            options = QueryMergesExtendedOptions.QUERY_RENAMES;
        }
        final ExtendedMerge[] merges = repository.getVersionControlClient().queryMergesExtended(
            item,
            itemVersion,
            itemVersion,
            itemVersion,
            options);

        final ArrayList csList = new ArrayList();
        for (int i = 0; i < merges.length; i++) {
            final ChangesetSummary cs = merges[i].getSourceChangeset();
            final Change[] srcChange = new Change[] {
                merges[i].getSourceItem()
            };
            final Changeset child = new Changeset(
                srcChange,
                cs.getComment(),
                null,
                null,
                cs.getCommitter(),
                cs.getCommitterDisplayName(),
                cs.getCreationDate(),
                cs.getChangesetID(),
                cs.getOwner(),
                cs.getOwnerDisplayName(),
                null);
            final boolean isRenameOrSourceRename =
                changeType.contains(ChangeType.RENAME) || changeType.contains(ChangeType.SOURCE_RENAME);
            if (!(isRenameOrSourceRename && caseChangingRenameSource(c, child))) {
                addChild(csList, child, slotMode);
            }

        }

        // Sort
        final Changeset[] changesets = (Changeset[]) csList.toArray(new Changeset[0]);
        for (int i = 0; i < changesets.length - 1; i++) {
            for (int j = i + 1; j < changesets.length; j++) {
                if (changesets[i].getChangesetID() < changesets[j].getChangesetID()) {
                    final Changeset temp = changesets[i];
                    changesets[i] = changesets[j];
                    changesets[j] = temp;
                }
            }
        }
        return changesets;
    }

    public static boolean mayHaveChildren(final TFSRepository repository, final Changeset c) {
        final Change[] changes = c.getChanges();
        if (changes == null || changes.length <= 0) {
            return false;
        }
        final boolean extSupport = extensionsSupported(repository);
        boolean flag = false;
        final ChangeType ch = changes[0].getChangeType();
        flag |= ch.contains(ChangeType.BRANCH);
        flag |= (ch.contains(ChangeType.MERGE) && extSupport);
        flag |= (ch.contains(ChangeType.RENAME) && extSupport);
        return flag;
    }

    public static void addChild(final List children, final Changeset child, final boolean slotMode) {
        children.add(child);
        final Change ch = child.getChanges()[0];
        final ChangeType changeType = ch.getChangeType();
        // Split merge renames
        if (slotMode && changeType.contains(ChangeType.RENAME) && changeType.contains(ChangeType.MERGE)) {
            ch.setChangeType(ch.getChangeType().remove(ChangeType.RENAME));
            final Change newChange = new Change(ch.getItem(), ChangeType.RENAME, ch.getMergeSources());
            final Changeset newChild = new Changeset(child, newChange);
            children.add(newChild);
        }
    }

    private static boolean caseChangingRenameSource(final Changeset parent, final Changeset child) {
        return (parent.getChangesetID() == child.getChangesetID())
            && (parent.getChanges()[0].getItem().getServerItem().equalsIgnoreCase(
                child.getChanges()[0].getItem().getServerItem()));
    }

    public static boolean extensionsSupported(final TFSRepository repository) {
        return repository.getVersionControlClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue();
    }
}
