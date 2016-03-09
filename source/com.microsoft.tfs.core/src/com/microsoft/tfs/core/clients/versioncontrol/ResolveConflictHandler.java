// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolveErrorHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolvedHandler;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictComparator;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class ResolveConflictHandler implements ConflictResolvedHandler, ConflictResolveErrorHandler {
    private final VersionControlClient client;
    private final Workspace workspace;
    private final Map<Integer, String> updatedSourceLocalItems;

    private final List<GetOperationGroup> getOpGroups = new ArrayList<GetOperationGroup>();
    private final List<Conflict> resolvedConflicts = new ArrayList<Conflict>();
    private final List<UpdateLocalVersionSpec> updateLocalVersionSpecs = new ArrayList<UpdateLocalVersionSpec>();
    private ChangePendedFlags flags = ChangePendedFlags.UNKNOWN;

    public ResolveConflictHandler(
        final VersionControlClient client,
        final Workspace workspace,
        final Map<Integer, String> updatedSourceLocalItems) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(updatedSourceLocalItems, "updatedSourceLocalItems"); //$NON-NLS-1$

        this.client = client;
        this.workspace = workspace;
        this.updatedSourceLocalItems = updatedSourceLocalItems;
    }

    public List<GetOperationGroup> getGetOpGroups() {
        return getOpGroups;
    }

    public List<Conflict> getResolvedConflicts() {
        return resolvedConflicts;
    }

    public List<UpdateLocalVersionSpec> getUpdateLocalVersionSpecs() {
        return updateLocalVersionSpecs;
    }

    public ChangePendedFlags getFlags() {
        return flags;
    }

    @Override
    public void conflictResolved(
        final Conflict conflict,
        final GetOperation[] getOps,
        final GetOperation[] undoOps,
        final Conflict[] otherResolvedConflicts,
        final ChangePendedFlags changePendedFlags) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        final String updatedSourceLocalItem = updatedSourceLocalItems.get(conflict.getConflictID());

        if (updatedSourceLocalItem != null) {
            workspace.getWorkspaceWatcher().removeSkippedItem(updatedSourceLocalItem);

            conflict.setSourceLocalItem(updatedSourceLocalItem);
        }

        // If we are deleting a conflict, we should get any operations.
        Check.isTrue(
            !Resolution.DELETE_CONFLICT.equals(conflict.getResolution()) || getOps.length == 0 && undoOps.length == 0,
            MessageFormat.format(
                "Didn't expect any get ops (got {0}) or undo ops (got {1})", //$NON-NLS-1$
                getOps.length,
                undoOps.length));

        conflict.setResolved(true);

        client.getEventEngine().fireConflictResolved(
            new ConflictResolvedEvent(EventSource.newFromHere(), workspace, conflict, changePendedFlags));

        /*
         * Sort the child conflicts before firing the events so the application
         * receives them in a sorted order.
         */
        Arrays.sort(otherResolvedConflicts, new ConflictComparator());

        /* If there were any other resolved conflicts. */
        for (final Conflict c : otherResolvedConflicts) {
            c.setResolved(true);
            c.setResolution(conflict.getResolution());

            client.getEventEngine().fireConflictResolved(
                new ConflictResolvedEvent(EventSource.newFromHere(), workspace, c, changePendedFlags));
        }

        /*
         * If you are resolving a conflict on an add / branch as acceptTheirs we
         * need to set the file to readonly so we don't cause a writable file
         * conflict
         */
        if (Resolution.ACCEPT_THEIRS.equals(conflict.getResolution()) && !conflict.isNamespaceConflict()) {
            for (final GetOperation getOp : undoOps) {
                if (getOp.getTargetServerItem() != null
                    && ServerPath.equals(getOp.getTargetServerItem(), conflict.getYourServerItem())
                    && getOp.getVersionLocal() == 0
                    && getOp.getSourceLocalItem() != null
                    && ItemType.FILE.equals(getOp.getItemType())) {
                    getOp.setOkayToOverwriteExistingLocal(true);
                }
            }
        }

        /*
         * If the user specified AcceptTheirs as the resolution to a conflict
         * generated by merge or rollback and the user had a pending edit prior
         * to the merge, we need to mark the target file read-only here so that
         * the get code will be able to replace the file (for non-merge
         * conflicts, AcceptTheirs is the same as undo). Otherwise, get will
         * think it is a local conflict because it lacks the necessary context
         * to know that it's not a local conflict and that it really should
         * replace the writable file.
         *
         * If the user resolved a three-way merge conflict as AcceptMerge and
         * created the output file, the client already has the content that it
         * needs. In that case, set VersionLocal = VersionServer so we don't try
         * to download it and get the writable file conflict incorrectly.
         */
        if (Resolution.ACCEPT_THEIRS.equals(conflict.getResolution())
            && conflict.getYourLocalChangeType().contains(ChangeType.EDIT)) {
            for (final GetOperation getOp : getOps) {
                final String getOpItem =
                    getOp.getSourceServerItem() != null ? getOp.getSourceServerItem() : getOp.getTargetServerItem();
                final String conflictItem = conflict.getYourServerItemSource() != null
                    ? conflict.getYourServerItemSource() : conflict.getYourServerItem();

                // server says replace it
                if (ServerPath.equals(getOpItem, conflictItem)
                    && getOp.getVersionLocal() == -1
                    && getOp.getSourceLocalItem() != null
                    && (getOp.getChangeType().contains(ChangeType.MERGE)
                        || getOp.getChangeType().contains(ChangeType.ROLLBACK)
                        || conflict.isShelvesetConflict())
                    && getOp.getChangeType().contains(ChangeType.EDIT)) {
                    getOp.setOkayToOverwriteExistingLocal(true);
                }
            }
        } else if ((Resolution.ACCEPT_MERGE.equals(conflict.getResolution())
            || Resolution.ACCEPT_YOURS.equals(conflict.getResolution()))) {
            for (final GetOperation getOp : getOps) {
                if ((Resolution.ACCEPT_MERGE.equals(conflict.getResolution())
                    && conflict.getMergedFileName() != null
                    && conflict.getMergedFileName().length() > 0
                    && ServerPath.equals(getOp.getTargetServerItem(), conflict.getYourServerItem())
                    && getOp.getItemID() == conflict.getYourItemID()

                /* Server says replace it, or we didn't have it. */
                    && (getOp.getVersionLocal() == -1 || getOp.getVersionLocal() == 0)
                    && getOp.getChangeType().contains(ChangeType.MERGE)
                    && getOp.getChangeType().contains(ChangeType.EDIT)) ||

                /*
                 * if it's a version conflict and we have an edit, we need to
                 * ensure we don't lose our changes.
                 */
                    ((ConflictType.CHECKIN.equals(conflict.getType()) || ConflictType.GET.equals(conflict.getType()))
                        && ItemType.FILE.equals(getOp.getItemType())
                        && getOp.getChangeType().contains(ChangeType.EDIT)
                        && LocalPath.equals(conflict.getSourceLocalItem(), getOp.getSourceLocalItem()))) {
                    final UpdateLocalVersionSpec ulvs = new UpdateLocalVersionSpec();
                    ulvs.setSourceServerItem(getOp.getSourceServerItem());
                    ulvs.setItemID(getOp.getItemID());
                    ulvs.setSourceLocalItem(conflict.getSourceLocalItem());
                    ulvs.setVersionServer(getOp.getVersionServer());
                    ulvs.setPropertyValues(getOp.getPropertyValues());

                    updateLocalVersionSpecs.add(ulvs);

                    /*
                     * In order for the get engine to process the get op, we
                     * have to tell it where the item is now, which the server
                     * also knows due to the ULV call, so that if the target is
                     * different, get can move it with all of its normal error
                     * checking, local conflict handling, etc.
                     */
                    getOp.setVersionLocal(getOp.getVersionServer());
                    getOp.setSourceLocalItem(conflict.getSourceLocalItem());
                }
            }
        }

        flags = flags.combine(changePendedFlags);

        /*
         * Figure out if we can add get ops to the current group or we need to
         * create a new one. We need to create a new one if one doesn't exist
         * yet or if we are adding a get op for a path that is already in that
         * get op group.
         */

        GetOperationGroup currentGroup = null;
        boolean needNewGroup = true;

        if (getOpGroups.size() > 0) {
            needNewGroup = false;
            currentGroup = getOpGroups.get(getOpGroups.size() - 1);

            for (final GetOperation getOp : getOps) {
                if (getOp.getTargetLocalItem() != null
                    && currentGroup.hasGetOpForLocalPath(getOp.getTargetLocalItem())) {
                    needNewGroup = true;
                    break;
                }
            }
            if (needNewGroup == false) {
                for (final GetOperation getOp : undoOps) {
                    if (getOp.getTargetLocalItem() != null
                        && currentGroup.hasGetOpForLocalPath(getOp.getTargetLocalItem())) {
                        needNewGroup = true;
                        break;
                    }
                }
            }
        }
        if (needNewGroup) {
            currentGroup = new GetOperationGroup();
            getOpGroups.add(currentGroup);
        }

        currentGroup.addOperations(ProcessType.GET, getOps);
        currentGroup.addOperations(ProcessType.UNDO, undoOps);

        for (final Conflict otherResolvedConflict : otherResolvedConflicts) {
            resolvedConflicts.add(otherResolvedConflict);
        }
    }

    @Override
    public void conflictResolveError(final Conflict conflict, final Exception exception) {
        final String updatedSourceLocalItem = updatedSourceLocalItems.get(conflict.getConflictID());

        if (updatedSourceLocalItem != null) {
            workspace.getWorkspaceWatcher().removeSkippedItem(updatedSourceLocalItem);

            conflict.setSourceLocalItem(updatedSourceLocalItem);
        }

        client.getEventEngine().fireNonFatalError(
            new NonFatalErrorEvent(EventSource.newFromHere(), workspace, exception));
    }

    public class UpdateLocalVersionSpec {
        private String sourceServerItem;
        private int itemID;
        private String sourceLocalItem;
        private int versionServer;
        private PropertyValue[] properties;

        public String getSourceServerItem() {
            return sourceServerItem;
        }

        public void setSourceServerItem(final String sourceServerItem) {
            this.sourceServerItem = sourceServerItem;
        }

        public int getItemID() {
            return itemID;
        }

        public void setItemID(final int itemID) {
            this.itemID = itemID;
        }

        public String getSourceLocalItem() {
            return sourceLocalItem;
        }

        public void setSourceLocalItem(final String sourceLocalItem) {
            this.sourceLocalItem = sourceLocalItem;
        }

        public int getVersionServer() {
            return versionServer;
        }

        public void setVersionServer(final int versionServer) {
            this.versionServer = versionServer;
        }

        public PropertyValue[] getProperyValues() {
            return properties;
        }

        public void setPropertyValues(final PropertyValue[] properties) {
            this.properties = properties;
        }
    }

    public class GetOperationGroup {
        private final Set<String> localItems = new TreeSet<String>(LocalPath.TOP_DOWN_COMPARATOR);

        private List<GetOperation> undoOps = new ArrayList<GetOperation>();
        private List<GetOperation> getOps = new ArrayList<GetOperation>();

        public Boolean hasGetOpForLocalPath(final String localPath) {
            return localItems.contains(localPath);
        }

        public void addOperations(final ProcessType processType, final GetOperation[] operations) {
            for (final GetOperation operation : operations) {
                if (operation.getTargetLocalItem() != null) {
                    localItems.add(operation.getTargetLocalItem());
                }

                if (processType == ProcessType.UNDO) {
                    undoOps.add(operation);
                } else {
                    getOps.add(operation);
                }
            }
        }

        public GetOperation[] getUndoOps() {
            return undoOps.toArray(new GetOperation[undoOps.size()]);
        }

        public void setUndoOps(final List<GetOperation> undoOps) {
            this.undoOps = undoOps;
        }

        public GetOperation[] getGetOps() {
            return getOps.toArray(new GetOperation[getOps.size()]);
        }

        public void setGetOps(final List<GetOperation> getOps) {
            this.getOps = getOps;
        }
    }

}