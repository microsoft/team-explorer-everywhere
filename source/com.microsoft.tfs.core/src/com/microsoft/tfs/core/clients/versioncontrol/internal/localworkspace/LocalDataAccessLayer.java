// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.FailureCodes;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.ILocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.IPopulatableLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.InitiallyDeletedLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.LocalPendingChangeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.MoveUncommittedLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.OwnershipState;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.TeamProject;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueue;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueueOptions;
import com.microsoft.tfs.core.clients.versioncontrol.UploadedBaselinesCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CannotRenameDueToChildConflictException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PartialRenameConflictException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ReconcileBlockedByProjectRenameException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ReconcileFailedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.RepositoryPathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayerLocalWorkspaces;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalVersion;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PathTranslation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSetType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ReconcileResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.SeverityType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.ServerItemLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.KeyValuePair;
import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.datetime.DotNETDate;
import com.microsoft.tfs.util.tasks.TaskMonitor;

import ms.tfs.versioncontrol.clientservices._03._WorkingFolder;

public class LocalDataAccessLayer {
    private static final Log log = LogFactory.getLog(LocalDataAccessLayer.class);

    private static final ChangeType PEND_DELETE_CONFLICTING_CHANGE_TYPES =
        ChangeType.ALL.remove(ChangeType.ENCODING).remove(ChangeType.DELETE).remove(ChangeType.BRANCH).remove(
            ChangeType.MERGE).remove(ChangeType.LOCK);

    private static final String characterCorpus = "abcdefghjklmnopqrstuvwxyz0123456789-"; //$NON-NLS-1$

    /**
     *
     *
     *
     * @param workspace
     * @param lv
     * @param pc
     * @param changeRequests
     * @param silent
     * @param failures
     * @return
     */
    public static GetOperation[] pendAdd(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ChangeRequest[] changeRequests,
        final boolean silent,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(changeRequests, "changeRequests"); //$NON-NLS-1$

        final List<Failure> failureList = new ArrayList<Failure>();
        final List<GetOperation> getOps = new ArrayList<GetOperation>();

        // Most duplicates filters are by WorkspaceLocalItem instance; but since
        // we are creating local version entries ourselves, we have to filter by
        // target server item.
        final Set<String> duplicatesFilter = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        for (final ChangeRequest changeRequest : changeRequests) {
            if (null == changeRequest) {
                continue;
            }

            // It is not legal to modify the state of a lock through a local
            // call.
            Check.isTrue(
                changeRequest.getLockLevel() == LockLevel.UNCHANGED || changeRequest.getLockLevel() == LockLevel.NONE,
                "changeRequest.getLockLevel() == LockLevel.UNCHANGED || changeRequest.getLockLevel() == LockLevel.NONE"); //$NON-NLS-1$

            // The paths for PendAdd must be local items with no recursion.
            if (ServerPath.isServerPath(changeRequest.getItemSpec().getItem())
                || RecursionType.NONE != changeRequest.getItemSpec().getRecursionType()) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("LocalDataAccessLayer.ChangeRequestMustBeLocalNonRecursiveFormat"), //$NON-NLS-1$
                        changeRequest.getItemSpec()));
            }

            LocalPath.checkLocalItem(changeRequest.getItemSpec().getItem(), "item", false, false, false, true); //$NON-NLS-1$

            // The local item includes all pending renames of parents so we
            // treat it as a target server item.
            final String targetServerItem =
                WorkingFolder.getServerItemForLocalItem(changeRequest.getItemSpec().getItem(), wp.getWorkingFolders());

            if (null == targetServerItem) {
                // We should have already validated that the path is mapped; so
                // if the server item is null, the item must be cloaked.
                failureList.add(createItemCloakedFailure(changeRequest.getItemSpec().getItem()));
                continue;
            }

            // Verify the length of the resultant target server item meets our
            // constraints.
            if (targetServerItem.length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
                failureList.add(createPathTooLongFailure(targetServerItem));
                continue;
            }

            WorkspaceLocalItem lvEntry = lv.getByLocalItem(changeRequest.getItemSpec().getItem());
            LocalPendingChange pcEntry = pc.getByTargetServerItem(targetServerItem);

            final Failure teamProjectValidationFailure =
                TeamProject.validateChange(targetServerItem, changeRequest.getItemType());

            // if we have a committed local version row, fall back to the error
            // below.
            if (teamProjectValidationFailure != null && (lvEntry == null || lvEntry.getVersion() == 0)) {
                failureList.add(teamProjectValidationFailure);
                continue;
            } else if (pcEntry != null && pcEntry.isAdd()) {
                // Existing pending add. Skip other validators and re-use the
                // existing pending change.

                // No change will be made to the existing pending change -- for
                // example the encoding will *not* be changed to respect the
                // encoding supplied on this ChangeRequest.
            } else if (pcEntry != null) {
                failureList.add(
                    new Failure(
                        MessageFormat.format(
                            Messages.getString("LocalDataAccessLayer.ChangeAlreadyPendingExceptionFormat"), //$NON-NLS-1$
                            targetServerItem),
                        FailureCodes.CHANGE_ALREADY_PENDING_EXCEPTION,
                        SeverityType.ERROR,
                        changeRequest.getItemSpec().getItem()));

                continue;
            } else if (lvEntry != null && lvEntry.getVersion() != 0) {
                failureList.add(
                    new Failure(
                        MessageFormat.format(
                            Messages.getString("LocalDataAccessLayer.ItemExistsExceptionFormat"), //$NON-NLS-1$
                            targetServerItem),
                        FailureCodes.ITEM_EXISTS_EXCEPTION,
                        SeverityType.ERROR,
                        changeRequest.getItemSpec().getItem()));

                continue;
            } else if (pc.getRecursiveChangeTypeForTargetServerItem(targetServerItem).contains(ChangeType.DELETE)) {
                final String changedItem = changeRequest.getItemSpec().getItem();
                failureList.add(createPendingParentDeleteFailure(targetServerItem, changedItem));
                continue;
            }

            if (null == lvEntry) {
                lvEntry = new WorkspaceLocalItem();
                lvEntry.setServerItem(targetServerItem);
                lvEntry.setVersion(0);
                lvEntry.setLocalItem(changeRequest.getItemSpec().getItem());
                lvEntry.setEncoding(
                    (ItemType.FILE == changeRequest.getItemType()) ? changeRequest.getEncoding()
                        : VersionControlConstants.ENCODING_FOLDER);
                lvEntry.setPendingReconcile(true);

                lv.add(lvEntry);
            }

            lvEntry.setPropertyValues(changeRequest.getProperties());
            if (null == pcEntry) {
                pcEntry = new LocalPendingChange(lvEntry, targetServerItem, ChangeType.ADD_ENCODING);
                pcEntry.setEncoding(lvEntry.getEncoding());
                pcEntry.setTargetServerItem(targetServerItem);
                pcEntry.setCommittedServerItem(null);

                if (ItemType.FILE == changeRequest.getItemType()) {
                    pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.EDIT));
                }

                if (changeRequest.getProperties() != null && changeRequest.getProperties().length > 0) {
                    pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.PROPERTY));
                    pcEntry.setPropertyValues(changeRequest.getProperties());
                }

                pc.pendChange(pcEntry);
                pc.removeCandidateByTargetServerItem(targetServerItem);
            }

            // Create the GetOperation for this pending change.
            if (!silent) {
                if (!duplicatesFilter.add(lvEntry.getServerItem())) {
                    continue;
                }

                getOps.add(lvEntry.toGetOperation(pcEntry, itemPropertyFilters));
            }
        }

        for (final Failure failure : failureList) {
            failure.setRequestType(RequestType.ADD);
        }

        failures.set(failureList.toArray(new Failure[failureList.size()]));

        return getOps.toArray(new GetOperation[getOps.size()]);
    }

    /**
     *
     *
     *
     * @param workspace
     * @param lv
     * @param pc
     * @param changeRequests
     * @param silent
     * @param failures
     * @return
     */
    public static GetOperation[] pendDelete(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ChangeRequest[] changeRequests,
        final boolean silent,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(changeRequests, "changeRequests"); //$NON-NLS-1$

        workspace.getWorkspaceWatcher().scan(wp, lv, pc);

        final List<Failure> failureList = new ArrayList<Failure>();
        final List<GetOperation> getOps = new ArrayList<GetOperation>();
        final Set<WorkspaceLocalItem> duplicatesFilter = new HashSet<WorkspaceLocalItem>();

        for (final ChangeRequest changeRequest : changeRequests) {
            if (null == changeRequest) {
                continue;
            }

            // It is not legal to modify the state of a lock through a local
            // call.
            Check.isTrue(
                changeRequest.getLockLevel() == LockLevel.UNCHANGED,
                "changeRequest.getLockLevel() == LockLevel.UNCHANGED"); //$NON-NLS-1$

            if (ServerPath.isRootFolder(changeRequest.getItemSpec().getItem())) {
                failureList.add(new Failure(
                    Messages.getString("TeamProject.CanNotChangeRootFolderException"), //$NON-NLS-1$
                    FailureCodes.CANNOT_CHANGE_ROOT_FOLDER_EXCEPTION,
                    SeverityType.ERROR,
                    changeRequest.getItemSpec().getItem()));

                continue;
            }

            final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromItemSpec(
                changeRequest.getItemSpec(),
                wp,
                lv,
                pc,
                ParsedItemSpecOptions.INCLUDE_DELETED,
                failureList);

            if (null == parsedItemSpec) {
                continue;
            }

            // Check up front to see if the item provided is deleted. Emit the
            // appropriate errors if so.
            final String rootTargetServerItem;
            if (parsedItemSpec.isServerItem()) {
                rootTargetServerItem = parsedItemSpec.getTargetItem();
            } else {
                rootTargetServerItem =
                    WorkingFolder.getServerItemForLocalItem(parsedItemSpec.getTargetItem(), wp.getWorkingFolders());
            }

            if (null != rootTargetServerItem) {
                if (rootTargetServerItem.length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
                    failureList.add(createPathTooLongFailure(rootTargetServerItem));
                    continue;
                }

                final LocalPendingChange pcEntry = pc.getByTargetServerItem(rootTargetServerItem);

                if (null != pcEntry && pcEntry.isDelete()) {
                    failureList.add(createPendingDeleteConflictChangeFailure(rootTargetServerItem));
                    continue;
                } else if (pc.getRecursiveChangeTypeForTargetServerItem(rootTargetServerItem).contains(
                    ChangeType.DELETE)) {
                    failureList.add(createPendingParentDeleteFailure(rootTargetServerItem));
                    continue;
                }
            }

            for (final WorkspaceLocalItem lvEntry : parsedItemSpec.expandRootsFrom(lv, pc, failureList)) {
                if (!duplicatesFilter.add(lvEntry)) {
                    continue;
                }

                final String targetServerItem = pc.getTargetServerItemForLocalVersion(lvEntry);
                final Failure failure = TeamProject.validateChange(targetServerItem, changeRequest.getItemType());

                if (failure != null) {
                    failureList.add(failure);
                    continue;
                }

                LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                // Permit the lock, property and branch bits. If the branch bit
                // is specified, permit the encoding and merge bits, too.
                // Additionally permit PendDelete to stomp on pending edits if
                // the item is missing from disk.
                if (null != pcEntry) {
                    ChangeType remainingChangeType =
                        pcEntry.getChangeType().remove(ChangeType.LOCK).remove(ChangeType.BRANCH).remove(
                            ChangeType.PROPERTY);

                    if (pcEntry.isBranch()) {
                        remainingChangeType = remainingChangeType.remove(ChangeType.ENCODING).remove(ChangeType.MERGE);
                    }

                    if (pcEntry.isEdit() && lvEntry.isMissingOnDisk()) {
                        remainingChangeType = remainingChangeType.remove(ChangeType.EDIT);
                    }

                    if (!remainingChangeType.equals(ChangeType.NONE)) {
                        failureList.add(createPendingDeleteConflictChangeFailure(targetServerItem));
                        continue;
                    }
                }

                if (lvEntry.isDirectory()) {
                    boolean okayToDelete = true;

                    for (final LocalPendingChange pcChildEntry : pc.queryByTargetServerItem(
                        targetServerItem,
                        RecursionType.FULL,
                        null)) {
                        if (pcChildEntry.getChangeType().containsAny(PEND_DELETE_CONFLICTING_CHANGE_TYPES)) {
                            failureList.add(createPendingDeleteConflictChangeFailure(targetServerItem));
                            okayToDelete = false;
                            break;
                        }
                    }

                    if (!okayToDelete) {
                        continue;
                    }

                    for (final LocalPendingChange pcRenamedOut : pc.queryByCommittedServerItem(
                        lvEntry.getServerItem(),
                        RecursionType.FULL,
                        null)) {
                        if (pcRenamedOut.isRename()) {
                            final String format =
                                Messages.getString("LocalDataAccessLayer.PendingChildExceptionFormat"); //$NON-NLS-1$
                            failureList.add(
                                new Failure(
                                    MessageFormat.format(format, targetServerItem),
                                    FailureCodes.PENDING_CHILD_EXCEPTION,
                                    SeverityType.ERROR,
                                    targetServerItem));

                            okayToDelete = false;
                            break;
                        }
                    }

                    if (!okayToDelete) {
                        continue;
                    }
                }

                if (pc.getRecursiveChangeTypeForTargetServerItem(targetServerItem).contains(ChangeType.DELETE)) {
                    failureList.add(createPendingParentDeleteFailure(targetServerItem));
                    continue;
                }

                if (null == pcEntry) {
                    pcEntry = new LocalPendingChange(lvEntry, targetServerItem, ChangeType.DELETE);
                } else {
                    pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.DELETE));

                    // It's possible to pend a delete on an item whose local
                    // version entry is marked as MissingFromDisk. In this case,
                    // we want to strip the edit bit -- it's not a pending edit
                    // any longer.
                    pcEntry.setChangeType(pcEntry.getChangeType().remove(ChangeType.EDIT));
                }

                pc.pendChange(pcEntry);
                pc.removeCandidateByTargetServerItem(targetServerItem, true);

                if (lvEntry.isDirectory()) {
                    final List<String> childDeletesToRemove = new ArrayList<String>();

                    for (final LocalPendingChange pcChildEntry : pc.queryByTargetServerItem(
                        targetServerItem,
                        RecursionType.FULL,
                        "*")) //$NON-NLS-1$
                    {
                        if (pcChildEntry.getChangeType().equals(ChangeType.DELETE)) {
                            childDeletesToRemove.add(pcChildEntry.getTargetServerItem());
                        } else {
                            pcChildEntry.setChangeType(pcChildEntry.getChangeType().combine(ChangeType.DELETE));
                        }
                    }

                    for (final String toRemove : childDeletesToRemove) {
                        pc.removeByTargetServerItem(toRemove);
                    }
                }

                // Generate GetOperations for this pending change.
                if (!silent) {
                    final GetOperation rootGetOp = lvEntry.toGetOperation(pcEntry, itemPropertyFilters);
                    rootGetOp.setTargetLocalItem(null);
                    getOps.add(rootGetOp);

                    if (lvEntry.isDirectory()) {
                        final Iterable<WorkspaceLocalItem> lvChildEntries =
                            lv.queryByServerItem(lvEntry.getServerItem(), RecursionType.FULL, "*"); //$NON-NLS-1$

                        for (final WorkspaceLocalItem lvChildEntry : lvChildEntries) {
                            if (!duplicatesFilter.add(lvChildEntry)) {
                                continue;
                            }

                            final GetOperation childGetOp = lvChildEntry.toGetOperation(itemPropertyFilters);
                            childGetOp.setBaselineFileGUID(null);
                            childGetOp.setTargetLocalItem(null);
                            childGetOp.setPendingChangeID(0);
                            childGetOp.setChangeType(pc.getRecursiveChangeTypeForLocalVersion(lvChildEntry));
                            childGetOp.setProcessType(ProcessType.PEND);

                            getOps.add(childGetOp);
                        }
                    }
                }
            }
        }

        for (final Failure failure : failureList) {
            failure.setRequestType(RequestType.DELETE);
        }

        failures.set(failureList.toArray(new Failure[failureList.size()]));
        return getOps.toArray(new GetOperation[getOps.size()]);
    }

    /**
     *
     *
     *
     * @param workspace
     * @param lv
     * @param pc
     * @param changeRequests
     * @param silent
     * @param failures
     * @return
     */
    public static GetOperation[] pendEdit(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ChangeRequest[] changeRequests,
        final boolean silent,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(changeRequests, "changeRequests"); //$NON-NLS-1$

        final List<Failure> failureList = new ArrayList<Failure>();
        final List<GetOperation> getOps = new ArrayList<GetOperation>();
        final Set<WorkspaceLocalItem> duplicatesFilter = new HashSet<WorkspaceLocalItem>();

        for (final ChangeRequest changeRequest : changeRequests) {
            if (null == changeRequest) {
                continue;
            }

            // It is not legal to modify the state of a lock through a local
            // call.
            Check.isTrue(
                changeRequest.getLockLevel() == LockLevel.UNCHANGED,
                "changeRequest.getLockLevel() == LockLevel.UNCHANGED"); //$NON-NLS-1$

            final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromItemSpec(
                changeRequest.getItemSpec(),
                wp,
                lv,
                pc,
                ParsedItemSpecOptions.NONE,
                failureList);

            if (null == parsedItemSpec) {
                continue;
            }

            for (final WorkspaceLocalItem lvEntry : parsedItemSpec.expandFrom(lv, pc, failureList)) {
                final String targetServerItem = pc.getTargetServerItemForLocalVersion(lvEntry);

                if (!duplicatesFilter.add(lvEntry)) {
                    continue;
                }

                if (lvEntry.isDirectory()) {
                    if (RecursionType.NONE == parsedItemSpec.getRecursionType()) {
                        final String format = Messages.getString("LocalDataAccessLayer.FolderEditExceptionFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(format, targetServerItem);
                        failureList.add(
                            new Failure(
                                message,
                                FailureCodes.NOT_ALLOWED_ON_FOLDER_EXCEPTION,
                                SeverityType.ERROR,
                                changeRequest.getItemSpec().getItem()));
                    }

                    continue;
                }

                LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                if (null != pcEntry && pcEntry.isDelete()) {
                    final String format = Messages.getString("LocalDataAccessLayer.IncompatibleChangeExceptionFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(format, targetServerItem);
                    failureList.add(
                        new Failure(
                            message,
                            FailureCodes.INCOMPATIBLE_CHANGE_EXCEPTION,
                            SeverityType.ERROR,
                            changeRequest.getItemSpec().getItem()));
                    continue;
                }

                if (pc.getRecursiveChangeTypeForLocalVersion(lvEntry).contains(ChangeType.DELETE)) {
                    final String changedItem = changeRequest.getItemSpec().getItem();
                    failureList.add(createPendingParentDeleteFailure(targetServerItem, changedItem));
                    continue;
                }

                if (null != pcEntry
                    && pcEntry.isAdd()
                    && VersionControlConstants.ENCODING_UNCHANGED == changeRequest.getEncoding()) {
                    final String format = Messages.getString("LocalDataAccessLayer.IncompatibleChangeExceptionFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(format, targetServerItem);
                    failureList.add(
                        new Failure(
                            message,
                            FailureCodes.INCOMPATIBLE_CHANGE_EXCEPTION,
                            SeverityType.ERROR,
                            changeRequest.getItemSpec().getItem()));
                    continue;
                }

                // OK, we need to modify or create the pending change for this
                // item.
                if (null == pcEntry) {
                    Check.isTrue(lvEntry.isCommitted(), "Local version is uncommitted but has no pending change."); //$NON-NLS-1$

                    pcEntry = new LocalPendingChange(lvEntry, targetServerItem, ChangeType.EDIT);
                } else {
                    pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.EDIT));
                }

                if (changeRequest.getProperties() != null) {
                    for (final PropertyValue value : changeRequest.getProperties()) {
                        if (PropertyConstants.SYMBOLIC_KEY.equalsIgnoreCase(value.getPropertyName())) {
                            pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.PROPERTY));
                            // Merge with existing properties for save
                            pcEntry.setPropertyValues(
                                PropertyUtils.mergePendingValues(
                                    pcEntry.getPropertyValues(),
                                    changeRequest.getProperties()));
                        }
                    }
                }

                if (VersionControlConstants.ENCODING_UNCHANGED != changeRequest.getEncoding()) {
                    pcEntry.setEncoding(changeRequest.getEncoding());
                    pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.ENCODING));
                }

                pc.pendChange(pcEntry);

                // Create the GetOperation for this pending change.
                if (!silent) {
                    final GetOperation getop = lvEntry.toGetOperation(pcEntry, itemPropertyFilters);
                    getop.setChangeType(pc.getRecursiveChangeTypeForLocalVersion(lvEntry));
                    getOps.add(getop);
                }
            }
        }

        for (final Failure failure : failureList) {
            failure.setRequestType(RequestType.EDIT);
        }

        failures.set(failureList.toArray(new Failure[failureList.size()]));
        return getOps.toArray(new GetOperation[getOps.size()]);
    }

    /**
     * Pending local property changes is extremely limited. Only the types of
     * values which can be stored in {@link LocalPendingChangeFlags} are
     * supported (executable bit changes). Trying to set other named properties
     * will result in a failure. The caller should do an online operation for
     * other types.
     */
    public static GetOperation[] pendPropertyChange(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ChangeRequest[] changeRequests,
        final boolean silent,
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean onlineOperationRequired,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(changeRequests, "changeRequests"); //$NON-NLS-1$

        final List<Failure> failureList = new ArrayList<Failure>();
        final List<GetOperation> getOps = new ArrayList<GetOperation>();
        final Set<WorkspaceLocalItem> duplicatesFilter = new HashSet<WorkspaceLocalItem>();

        for (final ChangeRequest changeRequest : changeRequests) {
            if (null == changeRequest) {
                continue;
            }

            // We can only handle the executable or symbolic link property
            // offline.
            if (changeRequest.getProperties() != null) {
                for (final PropertyValue value : changeRequest.getProperties()) {
                    if (!(PropertyConstants.EXECUTABLE_KEY.equalsIgnoreCase(value.getPropertyName())
                        || PropertyConstants.SYMBOLIC_KEY.equalsIgnoreCase(value.getPropertyName()))) {
                        return sendToServer(failures, onlineOperationRequired);
                    }
                }
            }

            // It is not legal to modify the state of a lock through a local
            // call.
            Check.isTrue(
                changeRequest.getLockLevel() == LockLevel.UNCHANGED,
                "changeRequest.getLockLevel() == LockLevel.UNCHANGED"); //$NON-NLS-1$

            final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromItemSpec(
                changeRequest.getItemSpec(),
                wp,
                lv,
                pc,
                ParsedItemSpecOptions.NONE,
                failureList);

            if (null == parsedItemSpec) {
                continue;
            }

            for (final WorkspaceLocalItem lvEntry : parsedItemSpec.expandFrom(lv, pc, failureList)) {
                final String targetServerItem = pc.getTargetServerItemForLocalVersion(lvEntry);

                if (!duplicatesFilter.add(lvEntry)) {
                    continue;
                }

                LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                if (null != pcEntry && pcEntry.isDelete()) {
                    final String format = Messages.getString("LocalDataAccessLayer.IncompatibleChangeExceptionFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(format, targetServerItem);
                    failureList.add(
                        new Failure(
                            message,
                            FailureCodes.INCOMPATIBLE_CHANGE_EXCEPTION,
                            SeverityType.ERROR,
                            changeRequest.getItemSpec().getItem()));
                    continue;
                }

                if (pc.getRecursiveChangeTypeForLocalVersion(lvEntry).contains(ChangeType.DELETE)) {
                    final String changedItem = changeRequest.getItemSpec().getItem();
                    failureList.add(createPendingParentDeleteFailure(targetServerItem, changedItem));
                    continue;
                }

                // OK, we need to modify or create the pending change for this
                // item.
                if (null == pcEntry) {
                    Check.isTrue(lvEntry.isCommitted(), "Local version is uncommitted but has no pending change."); //$NON-NLS-1$
                    pcEntry = new LocalPendingChange(lvEntry, targetServerItem, ChangeType.PROPERTY);
                } else {
                    pcEntry.setChangeType(pcEntry.getChangeType().combine(ChangeType.PROPERTY));
                }

                // Merge with existing properties for save
                pcEntry.setPropertyValues(
                    PropertyUtils.mergePendingValues(pcEntry.getPropertyValues(), changeRequest.getProperties()));

                pc.pendChange(pcEntry);

                // Create the GetOperation for this pending change.
                if (!silent) {
                    // The get operation only has properties that match the
                    // filters
                    final GetOperation getop = lvEntry.toGetOperation(pcEntry, itemPropertyFilters);
                    getop.setChangeType(pc.getRecursiveChangeTypeForLocalVersion(lvEntry));
                    getOps.add(getop);
                }
            }
        }

        for (final Failure failure : failureList) {
            failure.setRequestType(RequestType.PROPERTY);
        }

        failures.set(failureList.toArray(new Failure[failureList.size()]));
        return getOps.toArray(new GetOperation[getOps.size()]);
    }

    /**
     *
     *
     *
     * @param workspace
     * @param lv
     * @param pc
     * @param changeRequests
     * @param silent
     * @param failures
     * @param onlineOperationRequired
     * @param invalidateWorkspaceAfterServerCall
     * @return
     */
    public static GetOperation[] pendRename(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ChangeRequest[] changeRequests,
        final boolean silent,
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean onlineOperationRequired,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(changeRequests, "changeRequests"); //$NON-NLS-1$

        workspace.getWorkspaceWatcher().scan(wp, lv, pc);

        final List<Failure> failureList = new ArrayList<Failure>();
        final List<GetOperation> getOps = new ArrayList<GetOperation>();

        failures.set(null);
        final WorkingFolder[] workspaceMappings = wp.getWorkingFolders();

        for (int i = 0; i < changeRequests.length; i++) {
            if (null == changeRequests[i]) {
                continue;
            }

            final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromItemSpec(
                changeRequests[i].getItemSpec(),
                wp,
                lv,
                pc,
                ParsedItemSpecOptions.NONE,
                failureList);

            if (null == parsedItemSpec) {
                continue;
            }

            final String sourceRoot = parsedItemSpec.getTargetItem();
            String targetRoot = changeRequests[i].getTargetItem();
            final String sourceServerRoot = tryGetServerItem(sourceRoot, wp, lv, pc);

            String targetServerRoot = null;
            boolean isCloaked = false;

            if (ServerPath.isServerPath(targetRoot)) {
                targetServerRoot = targetRoot;
            } else {
                final PathTranslation translation =
                    WorkingFolder.translateLocalItemToServerItem(targetRoot, workspaceMappings);

                if (translation != null) {
                    targetServerRoot = translation.getTranslatedPath();
                    isCloaked = translation.isCloaked();
                }
            }

            if (isCloaked) {
                failureList.add(createItemCloakedFailure(targetRoot));
                continue;
            } else if (targetServerRoot == null) {
                // Error is consistent with server workspaces when target path
                // is non-server and unmapped, but not cloaked
                failureList.add(createItemNotMappedFailure(targetRoot));
                continue;
            }

            final String targetLocalRoot = ServerPath.isServerPath(targetRoot)
                ? WorkingFolder.getLocalItemForServerItem(targetRoot, workspaceMappings) : targetRoot;

            final int maxServerPathLength = workspace.getClient().getMaxServerPathLength();
            // For consistency in error messages with server workspaces, check
            // the provided itemspec first, then the translated path.
            final AtomicReference<String> outTargetRoot = new AtomicReference<String>(targetRoot);
            ItemPath.checkItem(
                outTargetRoot,
                "changeRequest.ItemSpec.Item", //$NON-NLS-1$
                false,
                false,
                false,
                true,
                maxServerPathLength);
            targetRoot = outTargetRoot.get();

            if (ServerPath.isServerPath(targetRoot)) {
                if (targetLocalRoot != null && targetLocalRoot.length() > 0) {
                    LocalPath.checkLocalItem(targetLocalRoot, "TargetItem", false, false, false, true); //$NON-NLS-1$
                }
            } else {
                final AtomicReference<String> outTargetServerRoot = new AtomicReference<String>(targetServerRoot);
                ServerPath.checkServerItem(
                    outTargetServerRoot,
                    "TargetItem", //$NON-NLS-1$
                    false,
                    false,
                    false,
                    true,
                    maxServerPathLength);
                targetServerRoot = outTargetServerRoot.get();
            }

            // server path minus all renames that exists on its parents
            final String targetUnrenamedServerRoot = pc.getCommittedServerItemForTargetServerItem(targetServerRoot);
            if (ItemPath.isWildcard(targetRoot)) {
                failureList.add(
                    new Failure(
                        Messages.getString("LocalDataAccessLayer.WildcardNotAllowedForRenameTarget"), //$NON-NLS-1$
                        FailureCodes.WILDCARD_NOT_ALLOWED_EXCEPTION,
                        SeverityType.ERROR,
                        targetRoot));
                continue;
            }

            final ItemType targetItemType = changeRequests[i].getTargetItemType();
            final boolean isMove = isMove(targetUnrenamedServerRoot, targetItemType, lv);
            final boolean targetIsFolder = targetItemType == ItemType.ANY ? isDirectory(lv, pc, targetServerRoot)
                : targetItemType == ItemType.FOLDER;
            // if we move dir\*.cs, root (dir will not be included), if we move
            // dir, root is included
            final boolean rootIncludedInRename = parsedItemSpec.getPattern() == null;

            if (parsedItemSpec.getPattern() != null && ItemPath.isWildcard(parsedItemSpec.getPattern()) && !isMove) {
                failureList.add(
                    new Failure(
                        Messages.getString("LocalDataAccessLayer.WildcardNotAllowedForRenameSource"), //$NON-NLS-1$
                        FailureCodes.WILDCARD_NOT_ALLOWED_EXCEPTION,
                        SeverityType.ERROR,
                        sourceRoot));
                continue;
            }

            if (ServerPath.isRootFolder(targetServerRoot)) {
                failureList.add(new Failure(
                    Messages.getString("LocalDataAccessLayer.CanNotChangeRootFolderException"), //$NON-NLS-1$
                    FailureCodes.CANNOT_CHANGE_ROOT_FOLDER_EXCEPTION,
                    SeverityType.ERROR,
                    parsedItemSpec.getTargetItem()));
                continue;
            }

            if (!targetIsFolder) {
                // we validate if target is a team project only if it doesn't
                // exist in local workspace - if it does, we will move files
                // under it
                final Failure teamProjectValidationFailure =
                    TeamProject.validateChange(targetServerRoot, ItemType.FOLDER);
                if (teamProjectValidationFailure != null) {
                    failureList.add(teamProjectValidationFailure);
                    continue;
                }
            }

            for (final WorkspaceLocalItem lvEntry : parsedItemSpec.expandFrom(lv, pc, failureList)) {
                final String sourceCommittedServerItem = lvEntry.getServerItem();
                String sourceCurrentServerItem = sourceCommittedServerItem;
                if (lvEntry.isCommitted()) {
                    sourceCurrentServerItem = pc.getTargetServerItemForCommittedServerItem(sourceCommittedServerItem);
                }
                final String targetServerItem = calculateTargetServerItem(
                    sourceServerRoot,
                    sourceCurrentServerItem,
                    targetServerRoot,
                    targetIsFolder,
                    rootIncludedInRename);
                // targetLocalItem may be null if we move file into not mapped
                // location
                final String targetLocalItem =
                    WorkingFolder.getLocalItemForServerItem(targetServerItem, wp.getWorkingFolders());

                final boolean caseOnlyRename = ServerPath.equals(sourceCurrentServerItem, targetServerItem)
                    && !ServerPath.equals(sourceCurrentServerItem, targetServerItem, false);
                if (ServerPath.isRootFolder(sourceCurrentServerItem)) {
                    failureList.add(
                        new Failure(
                            Messages.getString("LocalDataAccessLayer.CanNotChangeRootFolderException"), //$NON-NLS-1$
                            FailureCodes.CANNOT_CHANGE_ROOT_FOLDER_EXCEPTION,
                            SeverityType.ERROR,
                            sourceCurrentServerItem));
                    continue;
                }

                final Failure teamProjectValidationFailure = TeamProject.validateChange(
                    sourceCommittedServerItem,
                    lvEntry.isDirectory() ? ItemType.FOLDER : ItemType.FILE);
                if (teamProjectValidationFailure != null) {
                    failureList.add(teamProjectValidationFailure);
                    continue;
                }

                if (targetServerItem.length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
                    failureList.add(createPathTooLongFailure(targetServerItem));
                    continue;
                }

                // validate mappings
                boolean workspaceMappingFailure = false;
                for (final WorkingFolder mapping : workspaceMappings) {
                    if (ServerPath.equals(mapping.getServerItem(), sourceCurrentServerItem)) {
                        final String format =
                            Messages.getString("LocalDataAccessLayer.RenameWorkingFolderExceptionFormat"); //$NON-NLS-1$
                        failureList.add(
                            new Failure(
                                MessageFormat.format(format, sourceCurrentServerItem),
                                FailureCodes.RENAME_WORKING_FOLDER_EXCEPTION,
                                SeverityType.ERROR,
                                sourceCurrentServerItem));
                        workspaceMappingFailure = true;
                        break;
                    }

                    if (ServerPath.isChild(sourceCurrentServerItem, mapping.getServerItem())) {
                        return sendToServer(failures, onlineOperationRequired);
                    }
                }
                if (workspaceMappingFailure) {
                    continue;
                }

                if (!caseOnlyRename && pc.getByTargetServerItem(targetServerItem) != null) {
                    final String format =
                        Messages.getString("LocalDataAccessLayer.ChangeAlreadyPendingExceptionFormat"); //$NON-NLS-1$
                    failureList.add(
                        new Failure(
                            MessageFormat.format(format, targetServerItem),
                            FailureCodes.CHANGE_ALREADY_PENDING_EXCEPTION,
                            SeverityType.ERROR,
                            sourceCurrentServerItem));
                    continue;
                }

                if (pc.getRecursiveChangeTypeForTargetServerItem(targetServerItem).contains(ChangeType.DELETE)) {
                    failureList.add(createPendingParentDeleteFailure(targetServerItem));
                    continue;
                }

                LocalPendingChange mainPendingChange = pc.getByLocalVersion(lvEntry);
                if (null != mainPendingChange) {
                    if (mainPendingChange.isLock()) {
                        // Cannot offline rename a pending lock.
                        return sendToServer(failures, onlineOperationRequired);
                    }

                    if (mainPendingChange.isDelete()) {
                        final String format =
                            Messages.getString("LocalDataAccessLayer.IncompatibleChangeExceptionFormat"); //$NON-NLS-1$
                        failureList.add(
                            new Failure(
                                MessageFormat.format(format, targetServerItem),
                                FailureCodes.INCOMPATIBLE_CHANGE_EXCEPTION,
                                SeverityType.ERROR,
                                changeRequests[i].getItemSpec().getItem()));
                        continue;
                    }
                    // target server item minus all parent renames
                    // if we have both parent renamed (folder->folder2) and item
                    // renamed(a->b), renaming folder2\a to folder2\b
                    // should undo rename on bar
                    final String targetParent = ServerPath.getParent(targetServerItem);
                    final String committedParent = pc.getCommittedServerItemForTargetServerItem(targetParent);
                    String targetUnrenamedParent = null;
                    if (committedParent != null && committedParent.length() > 0) {
                        targetUnrenamedParent = committedParent + targetServerItem.substring(targetParent.length());
                    }

                    if ((targetUnrenamedParent != null
                        && ServerPath.equals(targetUnrenamedParent, sourceCommittedServerItem, false))
                        || ServerPath.equals(sourceCommittedServerItem, targetServerItem, false)) {
                        // Selective undo
                        final AtomicReference<Failure[]> undoFailures = new AtomicReference<Failure[]>();
                        final List<LocalPendingChange> changes = new ArrayList<LocalPendingChange>();
                        changes.add(mainPendingChange);

                        final GetOperation[] undoOps = undoPendingChanges(
                            workspace,
                            wp,
                            lv,
                            pc,
                            changes,
                            ChangeType.RENAME,
                            undoFailures,
                            onlineOperationRequired);

                        if (onlineOperationRequired.get()) {
                            return sendToServer(failures, onlineOperationRequired);
                        }

                        failureList.addAll(Arrays.asList(undoFailures.get()));
                        getOps.addAll(Arrays.asList(undoOps));
                        continue;
                    }
                }
                WorkspaceLocalItem conflictingItem = (targetLocalItem == null || targetLocalItem.length() == 0) ? null
                    : lv.getByLocalItem(targetLocalItem);
                if (conflictingItem != null && !caseOnlyRename) {
                    final String format = Messages.getString("LocalDataAccessLayer.ItemExistsExceptionFormat"); //$NON-NLS-1$
                    failureList.add(
                        new Failure(
                            MessageFormat.format(format, targetServerItem),
                            FailureCodes.ITEM_EXISTS_EXCEPTION,
                            SeverityType.ERROR,
                            conflictingItem.getLocalItem()));
                    continue;
                }
                final List<GetOperation> affectedItems = new ArrayList<GetOperation>();
                final Map<String, LocalPendingChange> affectedCommittedPendingChanges =
                    new HashMap<String, LocalPendingChange>();
                final Map<String, LocalPendingChange> affectedNotCommittedPendingChanges =
                    new HashMap<String, LocalPendingChange>();
                // we should not updated pending changes in the main loop, since
                // we use them to calculate target path for each item
                // we need to do that afterwards
                final List<KeyValuePair<String, LocalPendingChange>> updatedPendingChanges =
                    new ArrayList<KeyValuePair<String, LocalPendingChange>>();

                // Create or update main pending change (on the item itself)
                if (mainPendingChange == null) {
                    mainPendingChange = new LocalPendingChange(lvEntry, targetServerItem, ChangeType.RENAME);
                    affectedCommittedPendingChanges.put(mainPendingChange.getCommittedServerItem(), mainPendingChange);
                } else {
                    if (mainPendingChange.isAdd() || mainPendingChange.isBranch()) {
                        affectedNotCommittedPendingChanges.put(
                            mainPendingChange.getTargetServerItem(),
                            mainPendingChange);
                    } else {
                        mainPendingChange.setChangeType(mainPendingChange.getChangeType().combine(ChangeType.RENAME));
                        affectedCommittedPendingChanges.put(
                            mainPendingChange.getCommittedServerItem(),
                            mainPendingChange);
                    }
                }

                boolean abort = false;
                if (lvEntry.isDirectory()) {
                    // build lookup of pending changes, so we can update them
                    // and populate GetOps with the right change
                    for (final LocalPendingChange lpEntry : pc.queryByTargetServerItem(
                        sourceCurrentServerItem,
                        RecursionType.FULL,
                        "*")) //$NON-NLS-1$
                    {
                        if (lpEntry.isAdd() || lpEntry.isBranch()) {
                            affectedNotCommittedPendingChanges.put(lpEntry.getTargetServerItem(), lpEntry);
                        } else {
                            affectedCommittedPendingChanges.put(lpEntry.getCommittedServerItem(), lpEntry);
                        }
                    }
                }

                for (final WorkspaceLocalItem childEntry : ParsedItemSpec.queryLocalVersionsByTargetServerItem(
                    lv,
                    pc,
                    sourceCurrentServerItem,
                    RecursionType.FULL,
                    null,
                    ParsedItemSpecOptions.INCLUDE_DELETED)) {
                    String childTargetServerItem;
                    GetOperation childGetOp;

                    LocalPendingChange associatedPendingChange = null;
                    if (childEntry.isCommitted()) {
                        associatedPendingChange = affectedCommittedPendingChanges.get(childEntry.getServerItem());
                    } else if (!childEntry.isCommitted()) {
                        associatedPendingChange = affectedNotCommittedPendingChanges.get(childEntry.getServerItem());
                    }

                    if (associatedPendingChange != null) {
                        if (associatedPendingChange.isLock()) {
                            // Cannot offline rename a pending lock.
                            return sendToServer(failures, onlineOperationRequired);
                        }

                        if (!ServerPath.equals(
                            targetServerItem,
                            associatedPendingChange.getTargetServerItem(),
                            false)) {
                            // do not update if this is new pending change we
                            // just created (mainPendingChange)
                            childTargetServerItem = calculateTargetServerItem(
                                sourceServerRoot,
                                associatedPendingChange.getTargetServerItem(),
                                targetServerRoot,
                                targetIsFolder,
                                rootIncludedInRename);
                        } else {
                            childTargetServerItem = associatedPendingChange.getTargetServerItem();
                        }

                        updatedPendingChanges.add(
                            new KeyValuePair<String, LocalPendingChange>(
                                childTargetServerItem,
                                associatedPendingChange));
                        childGetOp = childEntry.toGetOperation(associatedPendingChange, itemPropertyFilters);
                        // SourceServerItem should be identical with
                        // TargetServerItem for not committed changes (add and
                        // branch)
                        childGetOp.setSourceServerItem(
                            childEntry.isCommitted() ? childGetOp.getSourceServerItem() : childTargetServerItem);
                        childGetOp.setTargetServerItem(childTargetServerItem);
                        childGetOp.setChangeType(
                            childEntry.isCommitted()
                                ? associatedPendingChange.getChangeType().combine(ChangeType.RENAME)
                                : childGetOp.getChangeType());
                    } else {
                        final String currentServerItem = childEntry.isCommitted()
                            ? pc.getTargetServerItemForCommittedServerItem(childEntry.getServerItem())
                            : childEntry.getServerItem();
                        childTargetServerItem = calculateTargetServerItem(
                            sourceServerRoot,
                            currentServerItem,
                            targetServerRoot,
                            targetIsFolder,
                            rootIncludedInRename);
                        childGetOp = childEntry.toGetOperation(itemPropertyFilters);
                        childGetOp.setTargetServerItem(childTargetServerItem);
                        childGetOp.setChangeType(ChangeType.RENAME);
                    }

                    // TODO we include deletes only to add entry to
                    // updatedPendingChanges and update them later. We should
                    // check how costly it is when we rename folder that
                    // contains big deletes subfolder
                    if (childEntry.isDeleted()) {
                        continue;
                    }

                    if (childTargetServerItem.length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
                        abort = true;
                        failureList.add(createPathTooLongFailure(childTargetServerItem));
                        break;
                    }

                    final String childTargetLocalItem =
                        WorkingFolder.getLocalItemForServerItem(childTargetServerItem, wp.getWorkingFolders());

                    conflictingItem = (childTargetLocalItem == null || childTargetLocalItem.length() == 0) ? null
                        : lv.getByLocalItem(childTargetLocalItem);

                    if (conflictingItem != null
                        && !ServerPath.equals(conflictingItem.getServerItem(), childEntry.getServerItem())) {
                        abort = true;
                        final String format = Messages.getString("LocalDataAccessLayer.ItemExistsExceptionFormat"); //$NON-NLS-1$
                        failureList.add(
                            new Failure(
                                MessageFormat.format(format, targetServerItem),
                                FailureCodes.ITEM_EXISTS_EXCEPTION,
                                SeverityType.ERROR,
                                conflictingItem.getLocalItem()));
                        break;
                    }

                    if ((childTargetLocalItem == null || childTargetLocalItem.length() == 0)
                        && childGetOp.getChangeType().contains(ChangeType.EDIT)) {
                        abort = true;
                        final String format = Messages.getString("LocalDataAccessLayer.TargetCloakedExceptionFormat"); //$NON-NLS-1$
                        failureList.add(
                            new Failure(
                                MessageFormat.format(format, sourceCurrentServerItem),
                                FailureCodes.TARGET_CLOAKED_EXCEPTION,
                                SeverityType.ERROR,
                                sourceCurrentServerItem));
                        break;
                    }
                    if (silent) {
                        // we generated all possible warnings, now bail since
                        // it's a silent operation
                        continue;
                    }
                    childGetOp.setTargetLocalItem(childTargetLocalItem);
                    affectedItems.add(childGetOp);
                }
                if (abort) {
                    continue;
                }

                // update pending changes
                for (final KeyValuePair<String, LocalPendingChange> updatedPendingChangeInfo : updatedPendingChanges) {
                    final String updateServerPath = updatedPendingChangeInfo.getKey();
                    final LocalPendingChange updatedPendingChange = updatedPendingChangeInfo.getValue();
                    pc.remove(updatedPendingChange);
                    updatedPendingChange.setTargetServerItem(updateServerPath);
                    pc.pendChange(updatedPendingChange);
                }

                if (!silent) {
                    // update local versions of not committed items (adds and
                    // branches)
                    for (final String originalServerItem : affectedNotCommittedPendingChanges.keySet()) {
                        final LocalPendingChange pendingChange =
                            affectedNotCommittedPendingChanges.get(originalServerItem);
                        final WorkspaceLocalItem addEntry = lv.getByServerItem(originalServerItem, false);
                        if (addEntry != null) {
                            lv.removeByServerItem(originalServerItem, false, true);
                            addEntry.setServerItem(pendingChange.getTargetServerItem());
                            // TODO what about renaming pending branch into not
                            // mapped location?
                            // TODO we already calculated this local path once,
                            // we should store it in
                            // affectedNotCommittedPendingChanges and reuse it
                            addEntry.setLocalItem(
                                WorkingFolder.getLocalItemForServerItem(
                                    pendingChange.getTargetServerItem(),
                                    wp.getWorkingFolders()));
                            lv.add(addEntry);
                        }
                    }
                }
                getOps.addAll(affectedItems);
            }
        }

        for (final Failure failure : failureList) {
            failure.setRequestType(RequestType.RENAME);
        }

        onlineOperationRequired.set(false);
        failures.set(failureList.toArray(new Failure[failureList.size()]));
        return getOps.toArray(new GetOperation[getOps.size()]);
    }

    /**
     *
     *
     *
     * @param workspace
     * @param itemSpecs
     * @param failures
     * @param lastChange
     *        If not null or empty, only items > last change will be returned.
     * @param pageSize
     *        If not zero, only the page size number of pending changes will be
     *        returned.
     * @param includeCandidates
     *        Whether to return candidate changes in the PendingSet object if
     *        true, a pending set object will be returned if the workspace
     *        either has pending changes or candidates
     * @return
     */
    public static PendingSet[] queryPendingChanges(
        final Workspace workspace,
        final ItemSpec[] itemSpecs,
        final AtomicReference<Failure[]> failures,
        final boolean includeCandidates,
        final String lastChange,
        final int pageSize,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final List<PendingChange> pendingChanges = new ArrayList<PendingChange>();
        final List<PendingChange> candidateChanges = new ArrayList<PendingChange>();
        failures.set(new Failure[0]);

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    for (final LocalPendingChange pcEntry : queryPendingChanges(
                        workspace,
                        wp,
                        lv,
                        pc,
                        itemSpecs,
                        new ArrayList<Failure>(),
                        includeCandidates,
                        itemPropertyFilters)) {
                        String targetLocalItem;

                        if (lastChange != null
                            && lastChange.length() != 0
                            && !pcEntry.isCandidate()
                            && ServerPath.compareTopDown(pcEntry.getTargetServerItem(), lastChange) <= 0) {
                            continue;
                        }

                        // if it's a candidate don't bother looking up the local
                        // version row.
                        final WorkspaceLocalItem lvEntry =
                            pcEntry.isCandidate() ? null : lv.getByPendingChange(pcEntry);

                        if (null != lvEntry && !lvEntry.isDeleted()) {
                            targetLocalItem = lvEntry.getLocalItem();
                        } else {
                            // There is no local version entry for this item.
                            // Calculate its target local item using the
                            // workspace mappings.
                            targetLocalItem = WorkingFolder.getLocalItemForServerItem(
                                pcEntry.getTargetServerItem(),
                                wp.getWorkingFolders());
                        }

                        if (!pcEntry.isCandidate()) {
                            pendingChanges.add(pcEntry.toPendingChange(workspace.getClient(), targetLocalItem));

                            if (pageSize != 0 && pageSize == pendingChanges.size()) {
                                break;
                            }
                        } else {
                            candidateChanges.add(pcEntry.toPendingChange(workspace.getClient(), targetLocalItem));
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

        if (pendingChanges.size() == 0 && candidateChanges.size() == 0) {
            return new PendingSet[0];
        } else {
            return new PendingSet[] {
                new PendingSet(
                    workspace.getName(),
                    workspace.getOwnerName(),
                    workspace.getOwnerDisplayName(),
                    OwnershipState.OWNED_BY_AUTHORIZED_USER,
                    workspace.getComputer(),
                    PendingSetType.WORKSPACE,
                    pendingChanges.toArray(new PendingChange[pendingChanges.size()]),
                    candidateChanges.toArray(new PendingChange[candidateChanges.size()]))
            };
        }
    }

    /**
     * Return the distinct set of pending changes from the workspace specified
     * that match the provided itemspecs.
     *
     * @param workspace
     *        Workspace to query pending changes in
     * @param lv
     *        Local version table for the workspace
     * @param pc
     *        Pending changes table for the workspace
     * @param itemSpecs
     *        Itemspecs to match
     * @param failuresAccumulator
     *        Container for failures.
     * @param includeCandidates
     *        Whether to include candidate pending changes
     * @return The set of distinct matching pending changes
     */
    private static Iterable<LocalPendingChange> queryPendingChanges(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ItemSpec[] itemSpecs,
        final List<Failure> failuresAccumulator,
        final boolean includeCandidates,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemspecs"); //$NON-NLS-1$

        workspace.getWorkspaceWatcher().scan(wp, lv, pc);

        final List<List<LocalPendingChange>> itemSpecResults = new ArrayList<List<LocalPendingChange>>();

        for (final ItemSpec itemSpec : itemSpecs) {
            if (null == itemSpec) {
                continue;
            }

            // The set of pending changes matching this itemspec.
            final List<LocalPendingChange> pendingChanges = new ArrayList<LocalPendingChange>();
            final ParsedItemSpecOptions flags = ParsedItemSpecOptions.INCLUDE_DELETED;

            if (ServerPath.isServerPath(itemSpec.getItem())) {
                // Canonicalize the input.
                itemSpec.setItem(ServerPath.canonicalize(itemSpec.getItem()));

                final AtomicReference<Failure> outDummy = new AtomicReference<Failure>();
                final ParsedItemSpec parsedItemSpec =
                    ParsedItemSpec.fromServerItemSpec(itemSpec, wp, lv, pc, flags, outDummy, includeCandidates);

                if (null != parsedItemSpec) {
                    final LocalPendingChange[] pcEntries = pc.queryByTargetServerItem(
                        parsedItemSpec.getTargetItem(),
                        parsedItemSpec.getRecursionType(),
                        parsedItemSpec.getPattern());

                    for (final LocalPendingChange pcEntry : pcEntries) {
                        pendingChanges.add(pcEntry);
                    }

                    if (includeCandidates) {
                        final Iterable<LocalPendingChange> entries = pc.queryCandidatesByTargetServerItem(
                            parsedItemSpec.getTargetItem(),
                            parsedItemSpec.getRecursionType(),
                            parsedItemSpec.getPattern());

                        for (final LocalPendingChange pcEntry : entries) {
                            pendingChanges.add(pcEntry);
                        }
                    }
                }
            } else {
                // Canonicalize the input.
                itemSpec.setItem(LocalPath.canonicalize(itemSpec.getItem()));

                final AtomicReference<Failure> outDummy = new AtomicReference<Failure>();
                final ParsedItemSpec parsedItemSpec =
                    ParsedItemSpec.fromLocalItemSpec(itemSpec, wp, lv, pc, flags, outDummy, includeCandidates);

                if (null != parsedItemSpec) {
                    if (RecursionType.NONE == parsedItemSpec.getRecursionType()) {
                        // Easy case -- one lookup in the pending changes table
                        final Iterator<WorkspaceLocalItem> items =
                            parsedItemSpec.expandFrom(lv, pc, outDummy).iterator();
                        WorkspaceLocalItem lvEntry = items.hasNext() ? items.next() : null;
                        LocalPendingChange pcEntry = null;

                        if (null != lvEntry) {
                            pcEntry = pc.getByLocalVersion(lvEntry);
                        } else {
                            final String targetServerItem = WorkingFolder.getServerItemForLocalItem(
                                parsedItemSpec.getTargetItem(),
                                wp.getWorkingFolders());

                            if (null != targetServerItem) {
                                pcEntry = pc.getByTargetServerItem(targetServerItem);

                                // if we have it in a different spot in our
                                // workspace, don't include it.
                                if (pcEntry != null) {
                                    lvEntry = lv.getByPendingChange(pcEntry);

                                    if (null != lvEntry && !lvEntry.isDeleted()) {
                                        pcEntry = null;
                                    }
                                }
                            }
                        }

                        if (null != pcEntry) {
                            pendingChanges.add(pcEntry);
                        }
                    } else {
                        // Query plan: Scan the entire pending changes table and
                        // look up each committed server item in the local
                        // version table to see if it matches the itemspec
                        // provided. The assumption is that the size of the
                        // pending changes table is much less than the size of
                        // the local version table, at least most of the time.
                        for (final LocalPendingChange pcEntry : pc.queryByTargetServerItem(
                            ServerPath.ROOT,
                            RecursionType.FULL,
                            null)) {
                            String targetLocalItem;
                            final WorkspaceLocalItem lvEntry = lv.getByPendingChange(pcEntry);

                            if (null != lvEntry && !lvEntry.isDeleted()) {
                                targetLocalItem = lvEntry.getLocalItem();
                            } else {
                                // There is no local version entry for this
                                // item.
                                // Calculate its target local item using the
                                // workspace mappings.
                                targetLocalItem = WorkingFolder.getLocalItemForServerItem(
                                    pcEntry.getTargetServerItem(),
                                    wp.getWorkingFolders());
                            }

                            if (null != targetLocalItem && parsedItemSpec.match(targetLocalItem)) {
                                pendingChanges.add(pcEntry);
                            }
                        }
                    }

                    if (includeCandidates) {
                        // The target server item and local item of a candidate
                        // pending change are always in sync with the workspace
                        // mappings, so we can perform a more efficient query
                        // plan here where we walk the candidates table by the
                        // mapped server item of the local item provided.
                        final String targetServerItem = WorkingFolder.getServerItemForLocalItem(
                            parsedItemSpec.getTargetItem(),
                            wp.getWorkingFolders());

                        if (null != targetServerItem) {
                            final Iterable<LocalPendingChange> candidateEntries = pc.queryCandidatesByTargetServerItem(
                                targetServerItem,
                                parsedItemSpec.getRecursionType(),
                                parsedItemSpec.getPattern());

                            for (final LocalPendingChange candidateEntry : candidateEntries) {
                                pendingChanges.add(candidateEntry);
                            }
                        }
                    }
                }
            }

            if (pendingChanges.size() > 0) {
                itemSpecResults.add(pendingChanges);
            } else {
                final String format = Messages.getString("LocalDataAccessLayer.NoPendingChangesMatchingItemSpecFormat"); //$NON-NLS-1$
                failuresAccumulator.add(
                    new Failure(
                        MessageFormat.format(format, itemSpec.getItem()),
                        FailureCodes.ITEM_NOT_CHECKED_OUT_EXCEPTION,
                        SeverityType.ERROR,
                        itemSpec.getItem()));
            }
        }

        if (itemSpecResults.size() == 1) {
            return itemSpecResults.get(0);
        } else {
            final Set<LocalPendingChange> toReturn =
                new TreeSet<LocalPendingChange>(LocalPendingChange.SERVER_ITEM_COMPARATOR);

            for (final List<LocalPendingChange> pendingChanges : itemSpecResults) {
                for (final LocalPendingChange pendingChange : pendingChanges) {
                    toReturn.add(pendingChange);
                }
            }

            return toReturn;
        }
    }

    /**
     *
     *
     *
     * @param workspace
     * @param lv
     * @param pc
     * @param itemSpecs
     * @param failures
     * @param onlineOperationRequired
     * @param invalidateWorkspaceAfterServerCall
     * @return
     */
    public static GetOperation[] undoPendingChanges(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ItemSpec[] itemSpecs,
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean onlineOperationRequired,
        final String[] itemPropertyFilters) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        final List<Failure> failuresList = new ArrayList<Failure>();

        final GetOperation[] toReturn = undoPendingChanges(
            workspace,
            wp,
            lv,
            pc,
            queryPendingChanges(workspace, wp, lv, pc, itemSpecs, failuresList, false, itemPropertyFilters),
            ChangeType.ALL,
            failures,
            onlineOperationRequired);

        for (final Failure failure : failures.get()) {
            failuresList.add(failure);
        }

        failures.set(failuresList.toArray(new Failure[failuresList.size()]));
        return toReturn;
    }

    public static GetOperation[] undoPendingChanges(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final Iterable<LocalPendingChange> pendingChanges,
        final ChangeType selectiveUndo,
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean onlineOperationRequired) {
        // Our collection of GetOperations that we will return. There will be
        // one GetOperation for every undone pending change and for every item
        // affected by an undone recursive pending change. The dictionary is
        // keyed by (SourceServerItem, IsCommitted) just like the local version
        // table.
        final Map<ServerItemIsCommittedTuple, GetOperation> getOps =
            new HashMap<ServerItemIsCommittedTuple, GetOperation>();

        // The UndoneChange structure encapsulates a pending change and its
        // destination server item in pending space (RevertToServerItem).
        final List<UndoneChange> undoneChanges = new ArrayList<UndoneChange>();

        // A hash table where we can check quickly to see if a server path has a
        // pending change being undone (by target server item)
        final Map<String, UndoneChange> undoneChangesMap = new HashMap<String, UndoneChange>();

        // When a recursive pending change is undone, there may be affected
        // child pending changes that are not undone. We store in this queue a
        // list of updates that need to be processed (atomically!).
        final List<RenamedPendingChange> renamedPendingChanges = new ArrayList<RenamedPendingChange>();

        // When undoing a rename, we need to make sure that the current target
        // server item isn't at or below a workspace mapping.
        List<String> workingFolderServerItems = null;

        // Failures generated by undo
        final List<Failure> failureList = new ArrayList<Failure>();

        int undoRenameCount = 0;

        // The RevertToServerItem starts out with the CommittedServerItem of the
        // pending change.
        for (final LocalPendingChange pcEntry : pendingChanges) {
            final ChangeType changeType = pcEntry.getChangeType().retain(selectiveUndo);
            final UndoneChange undoneChange = new UndoneChange(pcEntry, pcEntry.getServerItem(), changeType);

            // Add this item to our data structures
            undoneChanges.add(undoneChange);
            undoneChangesMap.put(undoneChange.getPendingChange().getTargetServerItem(), undoneChange);

            // We can't undo a checkin lock without going to the server. If we
            // match one, the entire undo operation needs to go to the server.
            if (undoneChange.isUndoingLock()
                || (undoneChange.isUndoingRename() && undoneChange.getPendingChange().isLock())) {
                return sendToServer(failures, onlineOperationRequired);
            }

            // Count how many renames we are undoing and make sure the rename is
            // not at or under a workspace mapping
            if (undoneChange.isUndoingRename()) {
                undoRenameCount++;

                // The first rename will initialize our list of working folders
                // and sort it
                if (null == workingFolderServerItems) {
                    final WorkingFolder[] workingFolders = wp.getWorkingFolders();
                    workingFolderServerItems = new ArrayList<String>(workingFolders.length);

                    for (final WorkingFolder workingFolder : workingFolders) {
                        workingFolderServerItems.add(workingFolder.getServerItem());
                    }

                    Collections.sort(workingFolderServerItems, new Comparator<String>() {
                        @Override
                        public int compare(final String x, final String y) {
                            return ServerPath.compareTopDown(x, y);
                        }
                    });
                }

                // Check to see if undoing this rename would modify the
                // workspace mappings
                final int index = Collections.binarySearch(
                    workingFolderServerItems,
                    pcEntry.getTargetServerItem(),
                    ServerPath.TOP_DOWN_COMPARATOR);

                if (index >= 0
                    || (~index < workingFolderServerItems.size()
                        && ServerPath.isChild(pcEntry.getTargetServerItem(), workingFolderServerItems.get(~index)))) {
                    return sendToServer(failures, onlineOperationRequired);
                }
            }
        }

        // Sort by target server item descending
        Collections.sort(undoneChanges, new Comparator<UndoneChange>() {
            @Override
            public int compare(final UndoneChange x, final UndoneChange y) {
                return ServerPath.compareTopDown(
                    y.getPendingChange().getTargetServerItem(),
                    x.getPendingChange().getTargetServerItem());
            }
        });

        // Pass 1: Calculate the RevertToServerItem for each undone pending
        // change.
        if (undoRenameCount > 0) {
            // We should come up with a faster way of figuring out whether the
            // user is undoing all renames in the workspace.
            int totalRenameCount = 0;
            final List<LocalPendingChange> remainingFolderRenames = new ArrayList<LocalPendingChange>();

            for (final LocalPendingChange pcEntry : pc.queryByTargetServerItem(
                ServerPath.ROOT,
                RecursionType.FULL,
                null)) {
                if (pcEntry.isRename()) {
                    totalRenameCount++;

                    if (pcEntry.isRecursiveChange()) {
                        final UndoneChange undoneChange = undoneChangesMap.get(pcEntry.getTargetServerItem());

                        if (undoneChange == null || !undoneChange.isUndoingRename()) {
                            remainingFolderRenames.add(pcEntry);
                        }
                    }
                }
            }

            if (undoneChanges.size() != pc.getCount()) {
                // We are not undoing all the changes in the workspace, so we
                // need to make sure that we do not have any pending changes
                // that are not being undone that have a target server item that
                // is at or underneath the source of a pending rename. Otherwise
                // there will be a collision when we undo the rename and it goes
                // back to the source.

                for (final UndoneChange undoneChange : undoneChanges) {
                    final LocalPendingChange lpc = undoneChange.getPendingChange();

                    // TODO: What kinds of bad situations can we end up with
                    // when you have a merge of a deleted item under a rename --
                    // a pending merge on a deleted item
                    if (0 == lpc.getDeletionID()
                        && lpc.isCommitted()
                        && undoneChange.isUndoingRename()
                        && !ServerPath.equals(lpc.getCommittedServerItem(), lpc.getTargetServerItem())) {
                        // Check to see if there is a namespace-additive pending
                        // change blocking this item from reverting to its
                        // committed server item
                        for (final LocalPendingChange pcEntry : pc.queryByTargetServerItem(
                            undoneChange.getPendingChange().getCommittedServerItem(),
                            RecursionType.FULL,
                            null)) {
                            if (pcEntry.isAdd() || pcEntry.isBranch() || pcEntry.isRename()) {
                                final UndoneChange collision = undoneChangesMap.get(pcEntry.getTargetServerItem());

                                if (collision == null
                                    || !collision.getUndoneChangeType().containsAny(ChangeType.ADD_BRANCH_OR_RENAME)) {
                                    final String format =
                                        Messages.getString("LocalDataAccessLayer.PartialRenameConflictExceptionFormat"); //$NON-NLS-1$
                                    throw new PartialRenameConflictException(
                                        MessageFormat.format(format, pcEntry.getTargetServerItem()));
                                }
                            }
                        }
                    }
                }
            }

            if (undoRenameCount != totalRenameCount) {
                // Only some of the renames in the workspace are being undone

                // Find a pending folder rename (PARENT\a->PARENT\b) that is
                // NOT being undone which is affecting a pending folder rename
                // (PARENT\A\SUB -> OUTSIDE) that IS being undone Where the
                // depth of the target name for the rename being undone
                // (OUTSIDE) is less than the depth of the target name that is
                // NOT being undone (PARENT\B).
                for (final LocalPendingChange remainingFolderRename : remainingFolderRenames) {
                    for (final LocalPendingChange pcEntry : pc.queryByCommittedServerItem(
                        remainingFolderRename.getCommittedServerItem(),
                        RecursionType.FULL,
                        null)) {
                        if (pcEntry.isRename() && pcEntry.isRecursiveChange()) {
                            final UndoneChange undoneChange = undoneChangesMap.get(pcEntry.getTargetServerItem());
                            final int targetFolderDepth = ServerPath.getFolderDepth(pcEntry.getTargetServerItem());
                            final int remainFolderDepth =
                                ServerPath.getFolderDepth(remainingFolderRename.getTargetServerItem());

                            if (undoneChange != null
                                && undoneChange.isUndoingRename()
                                && targetFolderDepth < remainFolderDepth) {
                                final String format =
                                    Messages.getString("LocalDataAccessLayer.PartialRenameConflictExceptionFormat"); //$NON-NLS-1$
                                throw new PartialRenameConflictException(
                                    MessageFormat.format(format, pcEntry.getCommittedServerItem()));
                            }
                        }
                    }
                }

                // Calculate new names for all namespace-changing pending
                // changes.

                // Map of key server item (current target server item) to the
                // data structure used for the name calculation. We'll use this
                // later to sub in the new names.
                final Map<String, RenameCalculationEntry> newNames = new HashMap<String, RenameCalculationEntry>();

                // Our algorithm wants to walk the set of pending renames only.
                final List<RenameCalculationEntry> pendingRenamesOnly =
                    new ArrayList<RenameCalculationEntry>(totalRenameCount);

                for (final UndoneChange undoneChange : undoneChanges) {
                    final RenameCalculationEntry rcEntry = new RenameCalculationEntry(
                        undoneChange.getPendingChange().getTargetServerItem(),
                        undoneChange.getPendingChange().getServerItem(),
                        undoneChange.getPendingChange(),
                        undoneChange.isUndoingRename());

                    newNames.put(undoneChange.getPendingChange().getTargetServerItem(), rcEntry);

                    if (undoneChange.getPendingChange().isRename()) {
                        pendingRenamesOnly.add(rcEntry);
                    }
                }

                for (final LocalPendingChange pcEntry : pc.queryByTargetServerItem(
                    ServerPath.ROOT,
                    RecursionType.FULL,
                    null)) {
                    if ((pcEntry.isRename() || pcEntry.isAdd() || pcEntry.isBranch())
                        && !undoneChangesMap.containsKey(pcEntry.getTargetServerItem())) {
                        final RenameCalculationEntry rcEntry = new RenameCalculationEntry(
                            pcEntry.getTargetServerItem(),
                            pcEntry.getServerItem(),
                            pcEntry,
                            false);

                        newNames.put(pcEntry.getTargetServerItem(), rcEntry);

                        if (pcEntry.isRename()) {
                            pendingRenamesOnly.add(rcEntry);
                        }
                    }
                }

                // Our algorithm wants to walk the set of pending renames only,
                // by source server item ascending.
                Collections.sort(pendingRenamesOnly, new Comparator<RenameCalculationEntry>() {
                    @Override
                    public int compare(final RenameCalculationEntry x, final RenameCalculationEntry y) {
                        return ServerPath.compareTopDown(
                            x.getPendingChange().getCommittedServerItem(),
                            y.getPendingChange().getCommittedServerItem());
                    }
                });

                for (final RenameCalculationEntry newName : pendingRenamesOnly) {
                    // Capture the data from newName into local variables, since
                    // we will be checking/editing values on the very same
                    // instance of RenameCalculationEntry in our up coming
                    // for each loops
                    final String sourceServerItem = newName.getSourceServerItem();
                    final String targetServerItem = newName.getTargetServerItem();

                    if (!newName.isUndoingChange()) {
                        for (final RenameCalculationEntry rcEntry : newNames.values()) {
                            final String entrySourceServerItem = rcEntry.getSourceServerItem();
                            if (ServerPath.isChild(sourceServerItem, entrySourceServerItem)) {
                                final String entryTargetServerItem = rcEntry.getTargetServerItem();
                                final String pendingTargetServerItem = rcEntry.getPendingChange().getTargetServerItem();

                                if (!ServerPath.equals(entrySourceServerItem, entryTargetServerItem)
                                    || ServerPath.equals(pendingTargetServerItem, entrySourceServerItem)) {
                                    rcEntry.setSourceServerItem(
                                        targetServerItem + entrySourceServerItem.substring(sourceServerItem.length()));
                                }
                            }
                        }
                    } else {
                        for (final RenameCalculationEntry rcEntry : newNames.values()) {
                            final String entryTargetServerItem = rcEntry.getTargetServerItem();
                            if (ServerPath.isChild(targetServerItem, entryTargetServerItem)) {
                                final String entrySourceServerItem = rcEntry.getSourceServerItem();
                                final String pendingTargetServerItem = rcEntry.getPendingChange().getTargetServerItem();

                                if (!ServerPath.equals(entrySourceServerItem, entryTargetServerItem)
                                    || ServerPath.equals(pendingTargetServerItem, entrySourceServerItem)) {
                                    rcEntry.setTargetServerItem(
                                        sourceServerItem + entryTargetServerItem.substring(targetServerItem.length()));
                                }
                            }
                        }
                    }
                }

                // If there are duplicate TargetServerItem values in the set of
                // RenameCalculationEntry objects, that indicates we have a
                // collision and cannot perform the undo. Sort by target server
                // item so that duplicates will be adjacent.
                final RenameCalculationEntry[] rcEntries =
                    newNames.values().toArray(new RenameCalculationEntry[newNames.size()]);

                Arrays.sort(rcEntries, new Comparator<RenameCalculationEntry>() {
                    @Override
                    public int compare(final RenameCalculationEntry x, final RenameCalculationEntry y) {
                        return ServerPath.compareTopDown(x.getTargetServerItem(), y.getTargetServerItem());
                    }
                });

                // The loop is complicated because we need to exclude pending
                // renames on deleted items from consideration.
                int duplicateCheckIndex = -1;

                for (int i = 1; i < rcEntries.length; i++) {
                    // Only allow rcEntries[duplicateCheckIndex] to point to a
                    // pending change which meets our criteria
                    if (rcEntries[i - 1].getPendingChange().getDeletionID() == 0
                        || !rcEntries[i - 1].getPendingChange().isRename()) {
                        duplicateCheckIndex = i - 1;
                    }

                    // This pending change must also meet the criteria, we must
                    // have something to compare it against, and the target
                    // server items need to be the same.
                    if (duplicateCheckIndex >= 0
                        && (rcEntries[i].getPendingChange().getDeletionID() == 0
                            || !rcEntries[i].getPendingChange().isRename())
                        && ServerPath.equals(
                            rcEntries[i].getTargetServerItem(),
                            rcEntries[duplicateCheckIndex].getTargetServerItem())) {
                        throw new PartialRenameConflictException(
                            MessageFormat.format(
                                Messages.getString("LocalDataAccessLayer.PartialRenameConflictExceptionFormat"), //$NON-NLS-1$ ,
                                rcEntries[i].getTargetServerItem()));
                    }
                }

                for (final UndoneChange undoneChange : undoneChanges) {
                    final RenameCalculationEntry rcEntry =
                        newNames.get(undoneChange.getPendingChange().getTargetServerItem());

                    if (rcEntry != null) {
                        undoneChange.setRevertToServerItem(rcEntry.getTargetServerItem());
                    }
                }
            } else {
                // All renames in the workspace are being undone.
                for (final UndoneChange undoneChange : undoneChanges) {
                    if (undoneChange.getPendingChange().isCommitted()) {
                        // Committed changes have their revert to server item
                        // already calculated.
                        undoneChange.setRevertToServerItem(undoneChange.getPendingChange().getCommittedServerItem());
                    } else {
                        // Find the closest rename that affects this uncommitted
                        // item and unrename it.
                        undoneChange.setRevertToServerItem(
                            pc.getCommittedServerItemForTargetServerItem(
                                undoneChange.getPendingChange().getTargetServerItem()));
                    }
                }
            }
        } else {
            // Even though we are not undoing a rename, there could be a
            // parental rename. So set the revert to server item based upon
            // existing parental renames.
            for (final UndoneChange undoneChange : undoneChanges) {
                undoneChange.setRevertToServerItem(undoneChange.getPendingChange().getTargetServerItem());
            }
        }

        // Pass 1: One GetOperation for every LocalPendingChange being undone
        for (final UndoneChange undoneChange : undoneChanges) {
            if (undoneChange.getRevertToServerItem().length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
                throw createPathTooLongException(undoneChange.getRevertToServerItem());
            }

            final LocalPendingChange pcEntry = undoneChange.getPendingChange();
            final WorkspaceLocalItem lvEntry = lv.getByPendingChange(undoneChange.getPendingChange());

            final GetOperation getOp = new GetOperation();

            getOp.setTargetServerItem(undoneChange.getRevertToServerItem());
            getOp.setSourceServerItem(
                pcEntry.isCommitted() ? pcEntry.getCommittedServerItem() : pcEntry.getTargetServerItem());

            if (null != lvEntry && !lvEntry.isDeleted()) {
                getOp.setSourceLocalItem(lvEntry.getLocalItem());

                // If we're undoing a pending add, mark the path as changed in
                // the scanner. This is because when the pending change is
                // undone, the item in question will not actually be touched on
                // disk. But we want to have it marked, so that we re-detect the
                // item as a candidate add.
                if (undoneChange.isUndoingAdd()) {
                    workspace.getWorkspaceWatcher().markPathChanged(lvEntry.getLocalItem());
                    LocalWorkspaceTransaction.getCurrent().setRaisePendingChangeCandidatesChanged(true);
                }
            }

            if ((0 == pcEntry.getDeletionID() || undoneChange.getRemainingChangeType().contains(ChangeType.UNDELETE))
                && !undoneChange.isUndoingBranch()) {
                final String targetLocalItem = WorkingFolder.getLocalItemForServerItem(
                    undoneChange.getRevertToServerItem(),
                    wp.getWorkingFolders());

                if (null != lvEntry) {
                    getOp.setTargetLocalItem(targetLocalItem);

                    // We never want the client to delete adds -- even if the
                    // target is cloaked.
                    if (pcEntry.isAdd() && null == getOp.getTargetLocalItem()) {
                        getOp.setTargetLocalItem(getOp.getSourceLocalItem());
                    }
                } else {
                    // We don't have a local version entry for this pending
                    // change, so we can't restore the content.
                    getOp.setTargetLocalItem(null);

                    if (null != targetLocalItem) {
                        final String format = "OfflineUndoNoLocalVersionEntry"; //$NON-NLS-1$
                        failureList.add(
                            new Failure(
                                MessageFormat.format(format, undoneChange.getRevertToServerItem()),
                                FailureCodes.BASELINE_UNAVAILABLE_EXCEPTION,
                                SeverityType.WARNING,
                                undoneChange.getRevertToServerItem()));
                    }
                }

                if (null != getOp.getTargetLocalItem()) {
                    getOp.setLocalVersionEntry(lv.getByLocalItem(getOp.getTargetLocalItem()));
                }
            }

            // This is the current encoding on the pending change -- we need the
            // committed encoding, which is on the local version entry if we
            // have one, but if we don't, we're in trouble and need to go to the
            // server.
            if (!pcEntry.getChangeType().contains(ChangeType.ENCODING)) {
                // If we aren't changing the encoding, then the local pending
                // change row's encoding is the encoding for the item.

                getOp.setEncoding(pcEntry.getEncoding());
            } else if (null != lvEntry) {
                getOp.setEncoding(lvEntry.getEncoding());
            } else {
                // We don't have the committed encoding for this item stored
                // locally. We need to process this undo operation on the
                // server.

                // TODO: Issue a warning and not download the change? The user
                // can go to the server and get it later, we don't want to
                // completely block them while they're offline
                return sendToServer(failures, onlineOperationRequired);
            }

            getOp.setChangeType(undoneChange.getUndoneChangeType());

            // If we are undoing an uncommitted pending change then do not add
            // in parent recursive changetypes
            if (!undoneChange.isUndoingAdd() && !undoneChange.isUndoingBranch()) {
                ChangeType inheritedChangeType = ChangeType.NONE;

                // The ChangeType on the item being undone is equal to the
                // ChangeType on the item itself, plus the recursive ChangeType
                // on its parent pending changes which are also being undone.
                for (final LocalPendingChange parentPcEntry : pc.queryParentsOfTargetServerItem(
                    pcEntry.getTargetServerItem())) {
                    final UndoneChange checkChange = undoneChangesMap.get(parentPcEntry.getTargetServerItem());

                    if (!parentPcEntry.isRecursiveChange() || checkChange == null) {
                        continue;
                    }

                    if (checkChange.getUndoneChangeType().contains(ChangeType.RENAME)) {
                        inheritedChangeType = inheritedChangeType.combine(ChangeType.RENAME);
                    }
                    if (checkChange.getUndoneChangeType().contains(ChangeType.DELETE)) {
                        inheritedChangeType = inheritedChangeType.combine(ChangeType.DELETE);
                    }
                }

                getOp.setChangeType(inheritedChangeType.combine(getOp.getChangeType()));
            }

            getOp.setDeletionID(pcEntry.getDeletionID());
            getOp.setItemType(pcEntry.getItemType());
            getOp.setPendingChangeID(LocalPendingChange.LOCAL_PENDING_CHANGE_ID);
            getOp.setItemID(pcEntry.getItemID());

            if (null != lvEntry) {
                if (lvEntry.isCommitted() && !lvEntry.isDirectory()) {
                    getOp.setBaselineFileGUID(
                        lvEntry.hasBaselineFileGUID() ? lvEntry.getBaselineFileGUID() : new byte[16]);
                }

                getOp.setHashValue(lvEntry.getHashValue());
                getOp.setVersionLocal(lvEntry.isDeleted() ? 0 : lvEntry.getVersion());
                getOp.setVersionServer(lvEntry.getVersion());
                getOp.setVersionServerDate(
                    (-1 == lvEntry.getCheckinDate()) ? DotNETDate.MIN_CALENDAR
                        : DotNETDate.fromWindowsFileTimeUTC(lvEntry.getCheckinDate()));
                getOp.setPropertyValues(pcEntry.getPropertyValues());
            } else {
                getOp.setVersionServer(pcEntry.getVersion());
            }

            getOps.put(new ServerItemIsCommittedTuple(getOp.getSourceServerItem(), pcEntry.isCommitted()), getOp);

            // Remove local version rows for adds, branches where the item is an
            // add, or we are syncing an item on top of an undone branch.
            if (undoneChange.isUndoingAdd() || (undoneChange.isUndoingBranch() && getOp.getTargetLocalItem() != null)) {
                if (null != lvEntry) {
                    lv.removeByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted(), true);
                }
            }
        }

        // Pass 2: Affected items underneath undone recursive changes
        for (final UndoneChange undoneChange : undoneChanges) {
            if (!undoneChange.isUndoingRecursiveChange()) {
                continue;
            }

            // The sort order means that undoneChange is always the closest
            // recursive operation affecting the item
            for (final WorkspaceLocalItem lvEntry : ParsedItemSpec.queryLocalVersionsByTargetServerItem(
                lv,
                pc,
                undoneChange.getPendingChange().getTargetServerItem(),
                RecursionType.FULL,
                null,
                ParsedItemSpecOptions.INCLUDE_DELETED)) {
                if (getOps.containsKey(
                    new ServerItemIsCommittedTuple(lvEntry.getServerItem(), lvEntry.isCommitted()))) {
                    continue;
                }

                final String currentServerItem = lvEntry.isCommitted()
                    ? pc.getTargetServerItemForCommittedServerItem(lvEntry.getServerItem()) : lvEntry.getServerItem();

                final GetOperation getOp = new GetOperation();

                getOp.setSourceLocalItem(lvEntry.isDeleted() ? null : lvEntry.getLocalItem());

                getOp.setTargetServerItem(
                    undoneChange.getRevertToServerItem()
                        + currentServerItem.substring(undoneChange.getPendingChange().getTargetServerItem().length()));

                if (getOp.getTargetServerItem().length() > VersionControlConstants.MAX_SERVER_PATH_SIZE) {
                    throw createPathTooLongException(getOp.getTargetServerItem());
                }

                getOp.setSourceServerItem(
                    lvEntry.isCommitted() ? lvEntry.getServerItem() : getOp.getTargetServerItem());

                getOp.setTargetLocalItem(
                    WorkingFolder.getLocalItemForServerItem(getOp.getTargetServerItem(), wp.getWorkingFolders()));

                if (null != getOp.getTargetLocalItem()) {
                    getOp.setLocalVersionEntry(lv.getByLocalItem(getOp.getTargetLocalItem()));
                }

                getOp.setDeletionID(0);
                getOp.setEncoding(lvEntry.getEncoding());
                getOp.setItemType(lvEntry.isDirectory() ? ItemType.FOLDER : ItemType.FILE);
                getOp.setPropertyValues(lvEntry.getPropertyValues());

                // Even if this item has a pending change which is not being
                // undone -- we return 0 here
                getOp.setPendingChangeID(0);
                getOp.setItemID(lvEntry.getItemID());

                if (!ServerPath.equals(currentServerItem, getOp.getTargetServerItem())) {
                    if (!lvEntry.isCommitted() && !ServerPath.equals(currentServerItem, getOp.getTargetServerItem())) {
                        // Uncommitted items have the itemid of the target
                        // server item, and we're changing paths, so set the
                        // item id to 0 because we have no idea what the item id
                        // of the target server item is
                        getOp.setItemID(0);
                    }

                    final LocalPendingChange pcEntry = pc.getByTargetServerItem(currentServerItem);

                    if (null != pcEntry) {
                        if (pcEntry.isLock()) {
                            // We cannot change the path of an item with a
                            // pending lock locally.
                            return sendToServer(failures, onlineOperationRequired);
                        }

                        if (pcEntry.hasMergeConflict()) {
                            throw new CannotRenameDueToChildConflictException(
                                undoneChange.getPendingChange().getTargetServerItem(),
                                pcEntry.getTargetServerItem());
                        }

                        // Queue this pending change for a later update of its
                        // target path and itemid (if it still exists after
                        // removing those pending changes that are being undone)
                        renamedPendingChanges.add(
                            new RenamedPendingChange(currentServerItem, getOp.getTargetServerItem()));
                    }
                }

                if (lvEntry.isCommitted() && !lvEntry.isDirectory()) {
                    getOp.setBaselineFileGUID(
                        lvEntry.hasBaselineFileGUID() ? lvEntry.getBaselineFileGUID() : new byte[16]);
                }

                getOp.setHashValue(lvEntry.getHashValue());
                getOp.setVersionLocal(lvEntry.isDeleted() ? 0 : lvEntry.getVersion());
                getOp.setVersionServer(lvEntry.getVersion());
                getOp.setVersionServerDate(
                    (-1 == lvEntry.getCheckinDate()) ? DotNETDate.MIN_CALENDAR
                        : DotNETDate.fromWindowsFileTimeUTC(lvEntry.getCheckinDate()));
                getOp.setChangeType(ChangeType.NONE);

                // We'll take a quick look at the parents of this target server
                // item to determine if the item is still deleted. On this same
                // pass, we'll compute the ChangeType of the GetOperation to
                // generate. Since there was no pending change being undone on
                // this item, the ChangeType is equal to the union of the
                // recursive changetype bits on parent pending changes which are
                // being undone.
                boolean stillDeleted = false;

                for (final LocalPendingChange parentPcEntry : pc.queryParentsOfTargetServerItem(currentServerItem)) {
                    final UndoneChange checkChange = undoneChangesMap.get(parentPcEntry.getTargetServerItem());

                    if (checkChange == null) {
                        if (parentPcEntry.isDelete()) {
                            stillDeleted = true;

                            // Who cares what the ChangeType on the GetOperation
                            // is, if we're not going to emit it?
                            break;
                        }
                    } else {
                        ChangeType changeType = getOp.getChangeType();
                        changeType = changeType.combine(checkChange.getUndoneChangeType());
                        changeType = changeType.combine(ChangeType.RENAME_OR_DELETE);
                        getOp.setChangeType(changeType);
                    }
                }

                if (!lvEntry.isDeleted() || !stillDeleted) {
                    if (null != getOp.getTargetLocalItem() || null != getOp.getSourceLocalItem()) {
                        getOps.put(
                            new ServerItemIsCommittedTuple(lvEntry.getServerItem(), lvEntry.isCommitted()),
                            getOp);
                    }
                }
            }
        }

        // Remove the pending change rows, putting back modified changes for
        // those entries which were selective undoes. Do this in two loops so
        // the changes are atomic.
        final List<LocalPendingChange> selectiveUndoChanges = new ArrayList<LocalPendingChange>();

        for (final UndoneChange undoneChange : undoneChanges) {
            pc.remove(undoneChange.getPendingChange());

            // If this was a selective undo, add an entry to the table
            if (!undoneChange.getUndoneChangeType().equals(undoneChange.getPendingChange().getChangeType())) {
                final LocalPendingChange newChange = undoneChange.getPendingChange().clone();
                newChange.setChangeType(
                    undoneChange.getPendingChange().getChangeType().remove(undoneChange.getUndoneChangeType()));
                newChange.setTargetServerItem(undoneChange.getRevertToServerItem());

                selectiveUndoChanges.add(newChange);
            }
        }

        // Second loop -- pend the selective undo changes.
        for (final LocalPendingChange pcEntry : selectiveUndoChanges) {
            pc.pendChange(pcEntry);
        }

        // Update the pending changes that were not undone but were affected by
        // parental renames.
        // Do this in two loops (remove+capture, modify+apply) so the changes
        // are atomic.
        for (final RenamedPendingChange renamedPendingChange : renamedPendingChanges) {
            final LocalPendingChange pcEntry = pc.getByTargetServerItem(renamedPendingChange.getOldTargetServerItem());

            if (null != pcEntry) {
                pc.remove(pcEntry);
                renamedPendingChange.setPendingChange(pcEntry);

                if (!pcEntry.isCommitted()) {
                    final WorkspaceLocalItem lvEntry =
                        lv.getByServerItem(renamedPendingChange.getOldTargetServerItem(), false);

                    if (null != lvEntry) {
                        renamedPendingChange.setLocalVersion(lvEntry);
                        lv.removeByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted(), true);
                    }
                }
            }
        }

        // Second loop -- apply the data to the tables after modifying it
        for (final RenamedPendingChange renamedPendingChange : renamedPendingChanges) {
            final LocalPendingChange pcEntry = renamedPendingChange.getPendingChange();
            final WorkspaceLocalItem lvEntry = renamedPendingChange.getLocalVersion();

            if (null != pcEntry) {
                pcEntry.setTargetServerItem(renamedPendingChange.getTargetServerItem());

                if (!pcEntry.isCommitted()) {
                    pcEntry.setItemID(0);
                }

                pc.pendChange(pcEntry);
            }

            if (null != lvEntry) {
                lvEntry.setServerItem(renamedPendingChange.getTargetServerItem());
                lvEntry.setItemID(0);
                lv.add(lvEntry);
            }
        }

        failures.set(failureList.toArray(new Failure[failureList.size()]));
        onlineOperationRequired.set(false);

        return getOps.values().toArray(new GetOperation[getOps.size()]);
    }

    private static GetOperation[] sendToServer(
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean onlineOperationRequired) {
        failures.set(new Failure[0]);
        onlineOperationRequired.set(true);
        return null;
    }

    public static boolean reconcileLocalWorkspace(
        final Workspace workspace,
        final WebServiceLayer webServiceLayer,
        final boolean unscannedReconcile,
        final boolean reconcileMissingFromDisk,
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean pendingChangesUpdatedByServer) {
        int previousProjectRevisionId = -1;
        boolean processedProjectRenames = false;

        while (true) {
            try {
                final boolean reconciled = reconcileLocalWorkspaceHelper(
                    workspace,
                    webServiceLayer,
                    unscannedReconcile,
                    reconcileMissingFromDisk,
                    failures,
                    pendingChangesUpdatedByServer);

                pendingChangesUpdatedByServer.set(pendingChangesUpdatedByServer.get() || processedProjectRenames);

                return reconciled;
            } catch (final ReconcileBlockedByProjectRenameException renameEx) {
                // Did we just ACK a new project revision ID, but the server
                // sent us another instruction
                // to move to the same revision ID we just moved to? Rather than
                // loop forever we will throw
                // in this case.
                if (previousProjectRevisionId >= 0 && previousProjectRevisionId == renameEx.getNewProjectRevisionId()) {
                    final Failure failure = new Failure(
                        MessageFormat.format(
                            Messages.getString("LocalDataAccessLayer.RepeatedProjectRevisionIdFormat"), //$NON-NLS-1$
                            renameEx.getNewProjectRevisionId()),
                        null,
                        SeverityType.ERROR,
                        null);

                    throw new ReconcileFailedException(new Failure[] {
                        failure
                    });
                }

                final List<KeyValuePair<String, String>> projectRenames = new ArrayList<KeyValuePair<String, String>>();

                for (int i = 0; i < renameEx.getOldProjectNames().length; i++) {
                    projectRenames.add(
                        new KeyValuePair<String, String>(
                            renameEx.getOldProjectNames()[i],
                            renameEx.getNewProjectNames()[i]));
                }

                ProcessProjectRenames(workspace, webServiceLayer, projectRenames, renameEx.getNewProjectRevisionId());

                previousProjectRevisionId = renameEx.getNewProjectRevisionId();
                processedProjectRenames = true;

                continue;
            }
        }

        /*
         * Unreachable code in C#
         */
        // throw new ReconcileFailedException(new Failure[]
        // {
        // new Failure("Could not drain all renames", null, SeverityType.ERROR,
        // null) //$NON-NLS-1$
        // });
    }

    private static boolean reconcileLocalWorkspaceHelper(
        final Workspace workspace,
        final WebServiceLayer webServiceLayer,
        final boolean unscannedReconcile,
        final boolean reconcileMissingFromDisk,
        final AtomicReference<Failure[]> failures,
        final AtomicBoolean pendingChangesUpdatedByServer) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        pendingChangesUpdatedByServer.set(false);
        final List<PendingChange> convertedAdds = new ArrayList<PendingChange>();

        final boolean throwOnProjectRenamed;
        if (EnvironmentVariables.isDefined(EnvironmentVariables.DD_SUITES_PROJECT_RENAME_UNPATCHED_CLIENT)) {
            throwOnProjectRenamed = false;
        } else {
            throwOnProjectRenamed = true;
        }

        final AtomicReference<GUID> serverPendingChangeSignature = new AtomicReference<GUID>(GUID.EMPTY);

        // No optimization away of reconciles when sending up MissingFromDisk
        // rows, since the bit in the header (lvHeader.PendingReconcile) may be
        // false when we actually have work to do (there are rows in the table
        // marked MissingFromDisk).
        if ((unscannedReconcile || !workspace.getWorkspaceWatcher().isScanNecessary()) && !reconcileMissingFromDisk) {
            // Pre-reconcile
            final AtomicBoolean hasPendingLocalVersionRows = new AtomicBoolean(true);
            LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);

            try {
                transaction.execute(new LocalVersionHeaderTransaction() {
                    @Override
                    public void invoke(final WorkspaceVersionTableHeader lvh) {
                        hasPendingLocalVersionRows.set(lvh.getPendingReconcile());
                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }

            final AtomicReference<GUID> clientPendingChangeSignature = new AtomicReference<GUID>(GUID.EMPTY);

            if (!hasPendingLocalVersionRows.get()) {
                transaction = new LocalWorkspaceTransaction(workspace);
                try {
                    transaction.execute(new PendingChangesHeaderTransaction() {
                        @Override
                        public void invoke(final LocalPendingChangesTableHeader pch) {
                            clientPendingChangeSignature.set(pch.getClientSignature());

                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }

                final GUID lastServerPendingChangeGuid =
                    workspace.getOfflineCacheData().getLastServerPendingChangeSignature();
                final Calendar lastReconcileTime = workspace.getOfflineCacheData().getLastReconcileTime();
                lastReconcileTime.add(Calendar.SECOND, 8);

                if (lastServerPendingChangeGuid != GUID.EMPTY
                    && clientPendingChangeSignature.get().equals(lastServerPendingChangeGuid)
                    && lastReconcileTime.after(Calendar.getInstance())) {
                    // This reconcile was optimized away with no server call.

                    failures.set(new Failure[0]);
                    return false;
                }

                serverPendingChangeSignature.set(
                    webServiceLayer.queryPendingChangeSignature(workspace.getName(), workspace.getOwnerName()));

                if (serverPendingChangeSignature.get() != GUID.EMPTY
                    && clientPendingChangeSignature.get().equals(serverPendingChangeSignature)) {
                    // This reconcile was optimized away.

                    workspace.getOfflineCacheData().setLastServerPendingChangeSignature(
                        serverPendingChangeSignature.get());
                    workspace.getOfflineCacheData().setLastReconcileTime(Calendar.getInstance());

                    failures.set(new Failure[0]);
                    return false;
                }
            }
        }

        final AtomicBoolean toReturn = new AtomicBoolean(true);

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            final AtomicReference<Failure[]> delegateFailures = new AtomicReference<Failure[]>(new Failure[0]);
            final AtomicBoolean delegatePCUpdated = new AtomicBoolean(false);

            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    if (!unscannedReconcile) {
                        // The line below has been commented out because we
                        // decided not to force a full scan here, because it
                        // causes significant degradation in UI performance.
                        //
                        // workspace.getWorkspaceWatcher().markPathChanged(null);
                        //
                        // It was an attempt to fix the bug:
                        // Bug 6191: When using local workspaces, get latest
                        // does not get a file that has been deleted from disk.
                        //
                        // The customer has to explicitly invoke
                        // Pending Changes>Actions>Detect local changes
                        // in Team Explorer.
                        //
                        // Note that none of customers reported that as an
                        // issue.
                        // It was detected on our tests only.
                        workspace.getWorkspaceWatcher().scan(wp, lv, pc);
                    }

                    // Pre-reconcile
                    if (!lv.getPendingReconcile()
                        && !reconcileMissingFromDisk
                        && GUID.EMPTY == serverPendingChangeSignature.get()) {
                        serverPendingChangeSignature.set(
                            webServiceLayer.queryPendingChangeSignature(workspace.getName(), workspace.getOwnerName()));

                        if (serverPendingChangeSignature.get() != GUID.EMPTY
                            && pc.getClientSignature().equals(serverPendingChangeSignature.get())) {
                            // This reconcile was optimized away.
                            delegateFailures.set(new Failure[0]);

                            workspace.getOfflineCacheData().setLastServerPendingChangeSignature(
                                serverPendingChangeSignature.get());
                            workspace.getOfflineCacheData().setLastReconcileTime(Calendar.getInstance());

                            toReturn.set(true);

                            return;
                        }
                    }

                    // Acknowledgment of team project renames, if any have been
                    // completed
                    if (wp.getNewProjectRevisionId() > 0) {
                        webServiceLayer.promotePendingWorkspaceMappings(
                            workspace.getName(),
                            workspace.getOwnerName(),
                            wp.getNewProjectRevisionId());

                        wp.setNewProjectRevisionId(0);
                    }

                    final LocalPendingChange[] pendingChanges =
                        pc.queryByTargetServerItem(ServerPath.ROOT, RecursionType.FULL, null);

                    /*
                     * TEE-specific Code
                     *
                     * In order to support offline property changes, which
                     * cannot be reconciled with
                     * WebServiceLayer.reconcileLocalWorkspace (the property
                     * values can't be sent), we have to pull out the pended
                     * property changes and send them to the server before
                     * reconciling.
                     */
                    final List<ChangeRequest> propertyRequests = new ArrayList<ChangeRequest>();

                    for (final LocalPendingChange lpc : pendingChanges) {
                        if (lpc.getChangeType().contains(ChangeType.PROPERTY)) {
                            final PropertyValue[] pv = lpc.getPropertyValues();
                            final String serverItem = lpc.getTargetServerItem();

                            if (pv != null && pv.length > 0 && serverItem != null) {
                                final ChangeRequest request = new ChangeRequest(
                                    new ItemSpec(serverItem, RecursionType.NONE),
                                    new WorkspaceVersionSpec(workspace),
                                    RequestType.PROPERTY,
                                    ItemType.ANY,
                                    VersionControlConstants.ENCODING_UNCHANGED,
                                    LockLevel.UNCHANGED,
                                    0,
                                    null,
                                    false);

                                request.setProperties(pv);

                                propertyRequests.add(request);
                            }
                        }
                    }

                    if (propertyRequests.size() > 0) {
                        ((WebServiceLayerLocalWorkspaces) webServiceLayer).pendChangesInLocalWorkspace(
                            workspace.getName(),
                            workspace.getOwnerName(),
                            propertyRequests.toArray(new ChangeRequest[propertyRequests.size()]),
                            PendChangesOptions.NONE,
                            SupportedFeatures.ALL,
                            new AtomicReference<Failure[]>(),
                            null,
                            null,
                            new AtomicBoolean(),
                            new AtomicReference<ChangePendedFlags>());

                        // TODO handle failures?
                    }

                    // Back to normal, non-TEE behavior

                    final AtomicBoolean outClearLocalVersionTable = new AtomicBoolean();
                    final ServerItemLocalVersionUpdate[] lvUpdates =
                        lv.getUpdatesForReconcile(pendingChanges, reconcileMissingFromDisk, outClearLocalVersionTable);

                    ReconcileResult result = webServiceLayer.reconcileLocalWorkspace(
                        workspace.getName(),
                        workspace.getOwnerName(),
                        pc.getClientSignature(),
                        pendingChanges,
                        lvUpdates,
                        outClearLocalVersionTable.get(),
                        throwOnProjectRenamed);

                    // report any failures
                    Failure[] reconcileFailures = result.getFailures();
                    workspace.getClient().reportFailures(workspace, reconcileFailures);

                    if (reconcileFailures.length > 0) {
                        throw new ReconcileFailedException(reconcileFailures);
                    }

                    GUID newSignature = new GUID(result.getNewSignature());
                    PendingChange[] newPendingChanges = result.getNewPendingChanges();

                    // If the local version rows for this local workspace have
                    // been purged from the server, then the server will set
                    // this flag on the result of the next reconcile.
                    if (result.isReplayLocalVersionsRequired()) {
                        // Reconcile a second time. This time, set the
                        // clearLocalVersionTable flag. This way, we know
                        // we have cleared out any lingering local version rows
                        // for this workspace.
                        if (!outClearLocalVersionTable.get()) {
                            result = webServiceLayer.reconcileLocalWorkspace(
                                workspace.getName(),
                                workspace.getOwnerName(),
                                pc.getClientSignature(),
                                pendingChanges,
                                lvUpdates,
                                true /* clearLocalVersionTable */,
                                throwOnProjectRenamed);

                            // Report any failures
                            reconcileFailures = result.getFailures();
                            workspace.getClient().reportFailures(workspace, reconcileFailures);

                            if (reconcileFailures.length > 0) {
                                throw new ReconcileFailedException(reconcileFailures);
                            }

                            // Grab the new signature and new pending changes
                            newSignature = new GUID(result.getNewSignature());
                            newPendingChanges = result.getNewPendingChanges();
                        }

                        // Now, go through the local version table and replay
                        // every row that we have.
                        final List<ServerItemLocalVersionUpdate> replayUpdates =
                            new ArrayList<ServerItemLocalVersionUpdate>(Math.min(lv.getLocalItemsCount(), 1000));

                        for (final WorkspaceLocalItem lvEntry : lv.queryByServerItem(
                            ServerPath.ROOT,
                            RecursionType.FULL,
                            null,
                            true /* includeDeleted */)) {
                            final ServerItemLocalVersionUpdate replayUpdate =
                                lvEntry.getLocalVersionUpdate(reconcileMissingFromDisk, true /* force */);

                            if (replayUpdate != null) {
                                replayUpdates.add(replayUpdate);

                                // Batch these updates in groups of 1000 items.
                                if (replayUpdates.size() >= 1000) {
                                    webServiceLayer.updateLocalVersion(
                                        workspace.getName(),
                                        workspace.getOwnerName(),
                                        replayUpdates.toArray(new ServerItemLocalVersionUpdate[replayUpdates.size()]));

                                    replayUpdates.clear();
                                }
                            }
                        }

                        if (replayUpdates.size() > 0) {
                            webServiceLayer.updateLocalVersion(
                                workspace.getName(),
                                workspace.getOwnerName(),
                                replayUpdates.toArray(new ServerItemLocalVersionUpdate[replayUpdates.size()]));
                        }
                    }

                    if (result.isPendingChangesUpdated()) {
                        delegatePCUpdated.set(true);

                        final Map<String, ItemType> newPendingDeletes =
                            new TreeMap<String, ItemType>(String.CASE_INSENSITIVE_ORDER);

                        for (final PendingChange pendingChange : newPendingChanges) {
                            if (pendingChange.isAdd()) {
                                final LocalPendingChange oldPendingChange =
                                    pc.getByTargetServerItem(pendingChange.getServerItem());

                                if (null == oldPendingChange || !oldPendingChange.isAdd()) {
                                    // Before calling ReconcileLocalWorkspace,
                                    // we did not have a pending add at this
                                    // target server item.
                                    convertedAdds.add(pendingChange);
                                }
                            } else if (pendingChange.isDelete()) {
                                // If the server removed any of our presented
                                // pending deletes, we want to know about it so
                                // we can get rid of the local version rows that
                                // we have in the deleted state. The server will
                                // remove our pending deletes when the item has
                                // been destroyed on the server.
                                newPendingDeletes.put(
                                    pendingChange.getSourceServerItem() == null ? pendingChange.getServerItem()
                                        : pendingChange.getSourceServerItem(),
                                    pendingChange.getItemType());
                            }
                        }

                        for (final LocalPendingChange oldPendingChange : pc.queryByCommittedServerItem(
                            ServerPath.ROOT,
                            RecursionType.FULL,
                            null)) {
                            if (oldPendingChange.isDelete()
                                && !newPendingDeletes.containsKey(oldPendingChange.getCommittedServerItem())) {
                                // We presented this delete to the server for
                                // Reconcile, but the server removed it from the
                                // pending changes manifest. We need to get rid
                                // of the LV rows for
                                // oldPendingChange.CommittedServerItem since
                                // this item is now destroyed.
                                final List<ServerItemIsCommittedTuple> lvRowsToRemove =
                                    new ArrayList<ServerItemIsCommittedTuple>();

                                final RecursionType recursion =
                                    oldPendingChange.isRecursiveChange() ? RecursionType.FULL : RecursionType.NONE;

                                // Aggregate up the deleted local version
                                // entries at this committed server item
                                // (or below if it's a folder), and we'll remove
                                // them.
                                for (final WorkspaceLocalItem lvEntry : lv.queryByServerItem(
                                    oldPendingChange.getCommittedServerItem(),
                                    recursion,
                                    null,
                                    true /* includeDeleted */)) {
                                    if (lvEntry.isDeleted()) {
                                        lvRowsToRemove.add(
                                            new ServerItemIsCommittedTuple(
                                                lvEntry.getServerItem(),
                                                lvEntry.isCommitted()));
                                    }
                                }

                                for (final ServerItemIsCommittedTuple tuple : lvRowsToRemove) {
                                    // We don't need to reconcile the removal of
                                    // LV entries marked IsDeleted since they
                                    // don't exist on the server anyway.
                                    lv.removeByServerItem(tuple.getCommittedServerItem(), tuple.isCommitted(), false);
                                }
                            }
                        }

                        pc.replacePendingChanges(newPendingChanges);
                    }

                    // If all we're doing to LV is marking it reconciled, then
                    // don't use TxF to commit
                    // both tables atomically as this is slower
                    if (!lv.isDirty()) {
                        transaction.setAllowTxF(false);
                    }

                    if (lvUpdates.length > 0) {
                        lv.markAsReconciled(wp, reconcileMissingFromDisk);

                        // If we removed all missing-from-disk items from the
                        // local version table, then we need to remove
                        // the corresponding candidate delete rows for those
                        // items as well.
                        if (reconcileMissingFromDisk) {
                            List<String> candidatesToRemove = null;

                            for (final LocalPendingChange candidateChange : pc.queryCandidatesByTargetServerItem(
                                ServerPath.ROOT,
                                RecursionType.FULL,
                                null)) {
                                if (candidateChange.isDelete()) {
                                    if (null == candidatesToRemove) {
                                        candidatesToRemove = new ArrayList<String>();
                                    }
                                    candidatesToRemove.add(candidateChange.getTargetServerItem());
                                }
                            }

                            if (null != candidatesToRemove) {
                                for (final String candidateDeleteTargetServerItem : candidatesToRemove) {
                                    pc.removeCandidateByTargetServerItem(candidateDeleteTargetServerItem);
                                }
                                // Set the candidates changed to true so that it
                                // refreshes the UI
                                LocalWorkspaceTransaction.getCurrent().setRaisePendingChangeCandidatesChanged(true);
                            }
                        }
                    }

                    newSignature =
                        webServiceLayer.queryPendingChangeSignature(workspace.getName(), workspace.getOwnerName());

                    if (!newSignature.equals(pc.getClientSignature())) {
                        pc.setClientSignature(newSignature);
                        workspace.getOfflineCacheData().setLastServerPendingChangeSignature(newSignature);
                    }

                    if (!newSignature.equals(pc.getClientSignature())) {
                        pc.setClientSignature(newSignature);
                        workspace.getOfflineCacheData().setLastServerPendingChangeSignature(newSignature);
                    }

                    workspace.getOfflineCacheData().setLastReconcileTime(Calendar.getInstance());
                }
            });

            failures.set(delegateFailures.get());
            pendingChangesUpdatedByServer.set(delegatePCUpdated.get());
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }
        }

        if (convertedAdds.size() > 0) {
            final UpdateLocalVersionQueueOptions options = UpdateLocalVersionQueueOptions.UPDATE_BOTH;
            final UpdateLocalVersionQueue ulvq = new UpdateLocalVersionQueue(workspace, options);

            try {
                for (final PendingChange pc : convertedAdds) {
                    // Every item in this list has a ChangeType of Add. As a
                    // result they are uncommitted items with no committed hash
                    // value, no committed length, and no baseline file GUID.
                    ulvq.queueUpdate(
                        new ClientLocalVersionUpdate(
                            pc.getServerItem(),
                            pc.getItemID(),
                            pc.getLocalItem(),
                            0 /* localVersion */,
                            DotNETDate.MIN_CALENDAR,
                            pc.getEncoding(),
                            null /* committedHashValue */,
                            0 /* committedLength */,
                            null /* baselineFileGuid */,
                            null /* pendingChangeTargetServerItem */,
                            null /* properties */));
                }
            } finally {
                ulvq.close();
            }
        }

        return toReturn.get();
    }

    public static void syncPendingChanges(
        final Workspace workspace,
        final GetOperation[] getOperations,
        final String[] itemPropertyFilters) {
        // This method has two goals in its execution. The primary goal is to
        // replace the pending changes for the workspace with those that we just
        // downloaded from the server. The second goal is to make sure that
        // local version rows for uncommitted items are cleaned up if their
        // pending changes have disappeared. The server will have already done
        // this cleanup for us.

        // We can't remove a pending add from PC and its uncommitted local
        // version row from LV in a single transaction since they are in
        // different tables. And we would much rather have an orphaned PC row
        // than an orphaned LV row. So we will get rid of the local version row
        // first in this case. (If there are no rows to clean up from LV, we
        // will not open the table at all.)

        // To do the scan of the GetOperations to check for this condition, we
        // need to have the new pending changes in a tree structure. The
        // LocalPendingChangesTable would do this anyway when we give it the
        // pending changes -- so we'll cheat a little bit here on the
        // encapsulation and do the work for it ahead of time, so we can use the
        // SparseTree for our own check.

        final AtomicReference<GUID> outServerPendingChangeSignature = new AtomicReference<GUID>();

        final PendingChange[] pendingChanges = workspace.getClient().getWebServiceLayer().queryServerPendingChanges(
            workspace,
            outServerPendingChangeSignature);

        final GUID serverPendingChangeSignature = outServerPendingChangeSignature.get();

        final SparseTree<LocalPendingChange> pendingChangesTarget =
            new SparseTree<LocalPendingChange>(ServerPath.PREFERRED_SEPARATOR_CHARACTER, String.CASE_INSENSITIVE_ORDER);

        for (final PendingChange pendingChange : pendingChanges) {
            final LocalPendingChange pcEntry = LocalPendingChange.fromPendingChange(pendingChange);
            pendingChangesTarget.set(pcEntry.getTargetServerItem(), pcEntry);
        }

        if (null != getOperations) {
            final List<KeyValuePair<String, Boolean>> localVersionsToRemove =
                new ArrayList<KeyValuePair<String, Boolean>>();
            final List<String> localVersionsToUpdate = new ArrayList<String>();

            for (final GetOperation getOp : getOperations) {
                // Make sure any adds still have pending changes. The server
                // will remove the local version row for a pending add when the
                // pending change is removed, and for a pending branch when we
                // are syncing an item on top of it.
                if (getOp.getChangeType().contains(ChangeType.ADD)
                    || (getOp.getChangeType().contains(ChangeType.BRANCH) && null != getOp.getTargetLocalItem())) {
                    final LocalPendingChange pcEntry = pendingChangesTarget.get(getOp.getTargetServerItem());
                    if (pcEntry == null) {
                        localVersionsToRemove.add(
                            new KeyValuePair<String, Boolean>(
                                getOp.getSourceServerItem(),
                                getOp.getVersionLocal() != 0));
                    }
                }

                // if the server pushed an edit to us, we need to update the
                // local version
                if (getOp.getVersionLocal() == -1) {
                    localVersionsToUpdate.add(getOp.getSourceServerItem());
                }
            }

            if (localVersionsToRemove.size() > 0 || localVersionsToUpdate.size() > 0) {
                final LocalWorkspaceTransaction lvTrans = new LocalWorkspaceTransaction(workspace);
                try {
                    lvTrans.execute(new LocalVersionTransaction() {
                        @Override
                        public void invoke(final WorkspaceVersionTable lv) {
                            for (final KeyValuePair<String, Boolean> item : localVersionsToRemove) {
                                lv.removeByServerItem(item.getKey(), item.getValue(), false);
                            }

                            for (final String serverItem : localVersionsToUpdate) {
                                final WorkspaceLocalItem item = lv.getByServerItem(serverItem, true);

                                if (item != null && item.getVersion() != -1) {
                                    lv.removeByServerItem(item.getServerItem(), item.isCommitted(), false);
                                    item.setVersion(-1);
                                    lv.add(item);
                                }
                            }
                        }
                    });
                } finally {
                    try {
                        lvTrans.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
            }
        }

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new PendingChangesTransaction() {
                @Override
                public void invoke(final LocalPendingChangesTable pc) {
                    // The method taking a SparseTree is a violation of
                    // encapsulation, but having us build the SparseTree gives
                    // us a perf benefit since we need it earlier in the method.
                    pc.replacePendingChanges(pendingChangesTarget);
                    pc.setClientSignature(serverPendingChangeSignature);
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

    public static void snapBackToCheckinDate(final Workspace workspace, final GetRequest[] requests) {
        if (null == requests) {
            return;
        }

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    for (final GetRequest getRequest : requests) {
                        if (null == getRequest) {
                            continue;
                        }

                        Iterable<WorkspaceLocalItem> lvEntrySource = null;

                        if (null == getRequest.getItemSpec()) {
                            // A null ItemSpec indicates that we should match
                            // every item in the workspace.
                            lvEntrySource = lv.queryByLocalItem(null, RecursionType.FULL, null);
                        } else {
                            final AtomicReference<Failure> dummy = new AtomicReference<Failure>();

                            final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromItemSpec(
                                getRequest.getItemSpec(),
                                wp,
                                lv,
                                pc,
                                ParsedItemSpecOptions.NONE,
                                dummy);

                            if (null != parsedItemSpec) {
                                lvEntrySource = parsedItemSpec.expandFrom(lv, pc, dummy);
                            }
                        }

                        if (null == lvEntrySource) {
                            continue;
                        }

                        for (final WorkspaceLocalItem lvEntry : lvEntrySource) {
                            if (-1 != lvEntry.getCheckinDate()
                                && lvEntry.getCheckinDate() != lvEntry.getLastModifiedTime()) {
                                final LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                                if ((null == pcEntry || !pcEntry.isEdit())
                                    && new File(lvEntry.getLocalItem()).exists()) {
                                    try {
                                        /*
                                         * Potentially the item is read-only
                                         * (probably not, since this is a local
                                         * workspace.) Clear the read-only bit
                                         * in order to set the last-write time.
                                         */
                                        final FileSystemAttributes attrs =
                                            FileSystemUtils.getInstance().getAttributes(lvEntry.getLocalItem());
                                        boolean restoreReadOnly = false;

                                        if (attrs != null && attrs.isReadOnly()) {
                                            attrs.setReadOnly(false);
                                            FileSystemUtils.getInstance().setAttributes(lvEntry.getLocalItem(), attrs);
                                            restoreReadOnly = true;
                                        }

                                        // Set the last modified time of the
                                        // item to the check-in date.
                                        new File(lvEntry.getLocalItem()).setLastModified(
                                            DotNETDate.fromWindowsFileTimeUTC(
                                                lvEntry.getCheckinDate()).getTimeInMillis());

                                        // Put the read-only bit back if we took
                                        // it off.
                                        if (restoreReadOnly) {
                                            attrs.setReadOnly(true);
                                            FileSystemUtils.getInstance().setAttributes(lvEntry.getLocalItem(), attrs);
                                        }

                                        // Write out that the last mod time is
                                        // now the checkin date.
                                        lvEntry.setLastModifiedTime(lvEntry.getCheckinDate());
                                        lv.setDirty(true);
                                    } catch (final Exception ex) {
                                        log.warn("Error snapping back to checkin date", ex); //$NON-NLS-1$
                                    }
                                }
                            }
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

    }

    public static void afterAdd(final Workspace workspace, final GetOperation[] getOperations) {
        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new LocalVersionTransaction() {
                @Override
                public void invoke(final WorkspaceVersionTable lv) {
                    for (final GetOperation getOp : getOperations) {
                        final WorkspaceLocalItem lvEntry = new WorkspaceLocalItem();
                        lvEntry.setServerItem(getOp.getSourceServerItem());
                        lvEntry.setVersion(0);
                        lvEntry.setLocalItem(getOp.getTargetLocalItem());
                        lvEntry.setEncoding(getOp.getEncoding());
                        lvEntry.setPendingReconcile(true);
                        lvEntry.setPropertyValues(getOp.getPropertyValues());

                        lv.add(lvEntry);
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

    public static void afterCheckin(
        final Workspace workspace,
        final int changesetId,
        final Calendar checkinDate,
        final GetOperation[] localVersionUpdates,
        final PendingChange[] newPendingChanges,
        final UploadedBaselinesCollection uploadedBaselinesCollection) {
        final List<BaselineRequest> baselineRequests = new ArrayList<BaselineRequest>();
        final Map<GetOperation, Long> operationLengths = new HashMap<GetOperation, Long>();

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    for (final GetOperation operation : localVersionUpdates) {
                        final WorkspaceLocalItem lvEntry =
                            lv.getByServerItem(operation.getSourceServerItem(), operation.getVersionLocal() != 0);

                        if (null != lvEntry) {
                            BaselineRequest baselineRequest = null;

                            if (null == operation.getTargetLocalItem() && null != lvEntry.getBaselineFileGUID()) {
                                baselineRequest = BaselineRequest.makeRemoveRequest(lvEntry.getBaselineFileGUID());
                            } else if (operation.getChangeType().contains(ChangeType.EDIT)) {
                                final long uncompressedLength;
                                final byte[] uploadedBaselineFileGuid;

                                if (null != uploadedBaselinesCollection) {
                                    final String targetItem = operation.getTargetLocalItem();
                                    final AtomicLong out = new AtomicLong(-1);

                                    uploadedBaselineFileGuid =
                                        uploadedBaselinesCollection.getUploadedBaseline(targetItem, out);
                                    uncompressedLength = out.get();
                                } else {
                                    uploadedBaselineFileGuid = null;
                                    uncompressedLength = -1;
                                }

                                // Did UploadChanges save gzipped content for
                                // this item for us? If so, we don't have to
                                // re-gzip the content on disk.
                                if (null != uploadedBaselineFileGuid && uncompressedLength >= 0) {
                                    operation.setBaselineFileGUID(uploadedBaselineFileGuid);
                                    operationLengths.put(operation, uncompressedLength);

                                    // Remove the entry from the collection so
                                    // that cleanup code in CheckIn will not
                                    // delete the baseline file from disk on the
                                    // way out.
                                    uploadedBaselinesCollection.removeUploadedBaseline(operation.getTargetLocalItem());
                                } else {
                                    operation.setBaselineFileGUID(GUID.newGUID().getGUIDBytes());

                                    baselineRequest = BaselineRequest.fromDisk(
                                        operation.getBaselineFileGUID(),
                                        operation.getTargetLocalItem(),
                                        operation.getTargetLocalItem(),
                                        operation.getHashValue());

                                    baselineRequests.add(baselineRequest);

                                    try {
                                        // The server doesn't supply the length
                                        // of the content we just committed.
                                        // We'll go grab it from the disk here.
                                        // There is a race here where the
                                        // content could have already changed on
                                        // disk since we uploaded it. This could
                                        // be corrected by having the server
                                        // return the length of each piece of
                                        // content it committed.
                                        final File file = new File(operation.getTargetLocalItem());
                                        operationLengths.put(operation, new Long(file.length()));
                                    } catch (final Throwable t) {
                                        log.warn(t);
                                        operationLengths.put(operation, new Long(-1));
                                    }
                                }
                            } else {
                                operation.setBaselineFileGUID(lvEntry.getBaselineFileGUID());
                                operationLengths.put(operation, new Long(lvEntry.getLength()));
                            }
                        } else if (operation.getTargetLocalItem() != null
                            && operation.getTargetLocalItem().length() > 0) {
                            boolean setTargetLocalItemToNull = true;

                            if (ItemType.FOLDER == operation.getItemType()
                                && null == operation.getSourceLocalItem()
                                && operation.getChangeType().contains(ChangeType.ADD)) {
                                final File directory = new File(operation.getTargetLocalItem());
                                if (!directory.exists()) {
                                    try {
                                        directory.mkdirs();
                                        setTargetLocalItemToNull = false;
                                    } catch (final Throwable t) {
                                        log.warn(t);
                                    }
                                } else {
                                    setTargetLocalItemToNull = false;
                                }
                            }

                            if (setTargetLocalItemToNull) {
                                operation.setTargetLocalItem(null);
                            }
                        }
                    }

                    pc.replacePendingChanges(newPendingChanges);

                    if (baselineRequests.size() != 0) {
                        new BaselineFolderCollection(workspace, wp.getBaselineFolders()).processBaselineRequests(
                            workspace,
                            baselineRequests);
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

        // As a final step, take the GetOperations which were returned by
        // CheckIn and process them.
        final UpdateLocalVersionQueue ulvQueue = new UpdateLocalVersionQueue(
            workspace,
            UpdateLocalVersionQueueOptions.UPDATE_BOTH,
            null,
            5000,
            10000,
            Integer.MAX_VALUE);

        try {
            for (final GetOperation operation : localVersionUpdates) {
                ClientLocalVersionUpdate lvUpdate;

                final String targetLocalItem = operation.getTargetLocalItem();
                if (targetLocalItem == null || targetLocalItem.length() == 0) {
                    lvUpdate = new ClientLocalVersionUpdate(
                        operation.getSourceServerItem(),
                        operation.getItemID(),
                        null,
                        operation.getVersionLocal(),
                        null);
                } else {
                    Long length = operationLengths.get(operation);
                    if (length == null) {
                        length = new Long(-1);
                    }

                    /*
                     * TODO get the current item (after check-in) properties
                     * back on the get operation from the server instead of
                     * detecting them from the filesystem. Getting them from the
                     * filesystem isn't too bad because it should accurately
                     * reflect what we just checked in.
                     */
                    PropertyValue[] detectedItemProperties = null;
                    if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
                        final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(targetLocalItem);
                        if (attrs.exists()) {
                            if (attrs.isSymbolicLink()) {
                                detectedItemProperties = new PropertyValue[] {
                                    PropertyConstants.IS_SYMLINK
                                };
                            } else if (!attrs.isDirectory() && attrs.isExecutable()) {
                                detectedItemProperties = new PropertyValue[] {
                                    PropertyConstants.EXECUTABLE_ENABLED_VALUE
                                };
                            }
                        }
                    }

                    lvUpdate = new ClientLocalVersionUpdate(
                        operation.getTargetServerItem(),
                        operation.getItemID(),
                        operation.getTargetLocalItem(),
                        operation.getVersionServer(),
                        checkinDate,
                        operation.getEncoding(),
                        operation.getHashValue(),
                        // Not supplied by the server; retrieved above from
                        // the disk
                        length.longValue(),
                        // Not supplied by the server; tagged onto the
                        // GetOperation by
                        // the transaction earlier in this method
                        operation.getBaselineFileGUID(),
                        null /* pendingChangeTargetServerItem */,
                        detectedItemProperties);

                    if (workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
                        // This UpdateLocalVersionQueue is going to flush before
                        // we go back to the disk and reset the dates on all the
                        // items which were just checked in (in
                        // Workspace\Checkin.cs). So we'll force
                        // UpdateLocalVersion to put the checkin date as the
                        // last modified date.

                        lvUpdate.setLastModifiedDate(DotNETDate.toWindowsFileTimeUTC(checkinDate));
                    }
                }

                ulvQueue.queueUpdate(lvUpdate);
            }
        } finally {
            ulvQueue.close();
        }
    }

    public static LocalVersion[][] queryLocalVersions(final Workspace workspace, final ItemSpec[] itemSpecs) {
        final LocalVersion[][] toReturn = new LocalVersion[itemSpecs.length][];

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new WorkspacePropertiesLocalVersionTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp, final WorkspaceVersionTable lv) {
                    for (int i = 0; i < itemSpecs.length; i++) {
                        final List<LocalVersion> localVersions = new ArrayList<LocalVersion>();
                        final AtomicReference<Failure> dummy = new AtomicReference<Failure>();

                        final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromLocalItemSpec(
                            itemSpecs[i],
                            wp,
                            lv,
                            null,
                            ParsedItemSpecOptions.NONE,
                            dummy,
                            false);

                        if (null != parsedItemSpec) {
                            for (final WorkspaceLocalItem lvEntry : parsedItemSpec.expandFrom(lv, null, dummy)) {
                                localVersions.add(new LocalVersion(lvEntry.getLocalItem(), lvEntry.getVersion()));
                            }
                        }

                        toReturn[i] = localVersions.toArray(new LocalVersion[localVersions.size()]);
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

        return toReturn;
    }

    /**
     * Creates ExtendedItems using locally stored data.
     */
    public static ExtendedItem[][] queryItemsExtended(
        final Workspace workspace,
        final ItemSpec[] items,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        final ExtendedItem[][] toReturn = new ExtendedItem[items.length][];
        final AtomicReference<Failure> dummy = new AtomicReference<Failure>();

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    workspace.getWorkspaceWatcher().scan(wp, lv, pc);

                    for (int i = 0; i < items.length; i++) {
                        final List<ExtendedItem> extendedItems = new ArrayList<ExtendedItem>();
                        final ParsedItemSpec parsedItemSpec = ParsedItemSpec.fromItemSpec(
                            items[i],
                            wp,
                            lv,
                            pc,
                            ParsedItemSpecOptions.INCLUDE_DELETED,
                            dummy);

                        // If the item is deleted, the result is an empty list
                        if (null != parsedItemSpec && deletedState != DeletedState.DELETED) {
                            for (final WorkspaceLocalItem lvEntry : parsedItemSpec.expandFrom(lv, pc, dummy)) {
                                final LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                                // We ignore deletedState here because all items
                                // we find would be treated as non-deleted
                                // anyway (an item with a pending change is
                                // non-deleted)
                                if (ParsedItemSpec.matchItemType(lvEntry, itemType)) {
                                    extendedItems.add(new ExtendedItem(pc, lvEntry, pcEntry));
                                }
                            }
                        }

                        toReturn[i] = extendedItems.toArray(new ExtendedItem[extendedItems.size()]);
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

        return toReturn;
    }

    /**
     * Updates the local copy of the local version table for this workspace in
     * accordance with the update requests provided by the localVersionUpdates
     * parameter.
     *
     *
     * @param workspace
     *        Workspace whose local version table is being updated
     * @param wp
     *        Workspace properties table
     * @param lv
     *        Local version table
     * @param pc
     *        Pending changes table (const)
     * @param localVersionUpdates
     *        Update requests for the local version table
     * @param persistedDisplacedBaselines
     *        (optional) A HashSet instance for persisting the set of displaced
     *        baselines across multiple calls to UpdateLocalVersion. Used for
     *        circular renames which may cross calls to UpdateLocalVersion
     * @param updateMissingBaselines
     *        The set of ClientLocalVersionUpdate objects which were processed
     *        successfully but which had no baseline file GUID when one was
     *        needed. A baseline file GUID was generated for these items and the
     *        caller should download the required baselines to ensure offline
     *        content is available.
     */
    public static void updateLocalVersion(
        final Workspace workspace,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ILocalVersionUpdate[] localVersionUpdates,
        final Set<byte[]> persistedDisplacedBaselines,
        final AtomicReference<IPopulatableLocalVersionUpdate[]> updateMissingBaselines) {
        final boolean setFileTimeToCheckin = workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN);
        final List<IPopulatableLocalVersionUpdate> updatesMissingBaselinesList =
            new ArrayList<IPopulatableLocalVersionUpdate>();

        Set<byte[]> displacedBaselines = persistedDisplacedBaselines;
        if (displacedBaselines == null) {
            displacedBaselines = new TreeSet<byte[]>(new BaselineFileGUIDComparer());
        }

        for (final ILocalVersionUpdate update : localVersionUpdates) {
            if (update instanceof MoveUncommittedLocalVersionUpdate) {
                handleMoveUncommittedUpdate(wp, lv, (MoveUncommittedLocalVersionUpdate) update);
            } else if (update instanceof InitiallyDeletedLocalVersionUpdate) {
                final InitiallyDeletedLocalVersionUpdate idUpdate = (InitiallyDeletedLocalVersionUpdate) update;

                final boolean updateIsMissingBaseline = handleInitiallyDeletedLocalVersionUpdate(
                    wp,
                    lv,
                    pc,
                    idUpdate,
                    setFileTimeToCheckin,
                    displacedBaselines);

                if (updateIsMissingBaseline) {
                    updatesMissingBaselinesList.add(idUpdate);
                }
            } else if (update instanceof ClientLocalVersionUpdate) {
                final ClientLocalVersionUpdate cUpdate = (ClientLocalVersionUpdate) update;

                final boolean updateIsMissingBaseline =
                    handleClientLocalVersionUpdate(wp, lv, pc, cUpdate, setFileTimeToCheckin, displacedBaselines);

                if (updateIsMissingBaseline) {
                    updatesMissingBaselinesList.add(cUpdate);
                }
            }
        }

        // If the caller did not provide a collection for displaced baselines to
        // persist in beyond the lifetime of this call, then we need to go ahead
        // and purge the displaced baselines now.
        if (null == persistedDisplacedBaselines) {
            for (final byte[] baselineFileGuid : displacedBaselines) {
                wp.deleteBaseline(baselineFileGuid);
            }
        }

        // Return the set of updates which were missing baseline file GUIDs to
        // the caller so that they can download them.
        updateMissingBaselines.set(
            updatesMissingBaselinesList.toArray(
                new IPopulatableLocalVersionUpdate[updatesMissingBaselinesList.size()]));
    }

    /**
     * Called during an UpdateLocalVersionQueue flush that writes both to the
     * local local version table and the server local version table. The local
     * local version table entries are initially marked PendingReconcile by
     * LocalDataAccessLayer.UpdateLocalVersion, above. After the server flush
     * successfully completes, the entries which were flushed have their
     * PendingReconcile flag cleared by a call to this method.
     *
     * @param lv
     *        Local version table
     * @param updates
     *        Updates which were successfully flushed to the server
     */
    public static void acknowledgeUpdateLocalVersion(
        final WorkspaceVersionTable lv,
        final ILocalVersionUpdate[] updates) {
        for (final ILocalVersionUpdate update : updates) {
            if (!update.isSendToServer()) {
                continue;
            }

            // Remove the PendingReconcile bit from this row in the local
            // version table.
            final WorkspaceLocalItem lvEntry = lv.getByServerItem(update.getSourceServerItem(), update.isCommitted());

            if (null != lvEntry && lvEntry.isPendingReconcile()) {
                lv.removeByServerItem(lvEntry.getServerItem(), lvEntry.isCommitted(), false);
                lvEntry.setPendingReconcile(false);
                lv.add(lvEntry);
            }
        }
    }

    /**
     * Retrieves the authoritative copy of the working folders for a local
     * workspace.
     *
     *
     * @param workspace
     *        The local workspace whose working folders should be fetched
     * @return The working folders for the local workspace
     */
    public static WorkingFolder[] queryWorkingFolders(final Workspace workspace) {
        final AtomicReference<WorkingFolder[]> toReturn = new AtomicReference<WorkingFolder[]>(null);

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new WorkspacePropertiesTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp) {
                    // Make a deep copy to return to the caller.
                    toReturn.set(WorkingFolder.clone(wp.getWorkingFolders()));
                }
            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }
        }

        return toReturn.get();
    }

    /**
     * Sets the authoritative copy of the working folders for a local workspace
     * to the provided values. The values provided should always be sourced from
     * the server to ensure they have been properly "reduced" (no redundant
     * mappings, etc.) as most code that does any kind of complex operation on
     * the working folders makes this assumption.
     *
     *
     * @param workspace
     *        The local workspace whose working folders should be set
     * @param workingFolders
     *        The new working folders for the workspace
     */
    public static void setWorkingFolders(final Workspace workspace, final WorkingFolder[] workingFolders) {
        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new WorkspacePropertiesTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp) {
                    wp.setWorkingFolders(workingFolders);
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

    /**
     * Given a local workspace, either sets all items without pending changes on
     * them to +R, or sets all items in the workspace to -R.
     *
     * @param workspace
     *        The local workspace whose local items should have their +R status
     *        changed
     * @param setReadOnly
     *        True to set items to +R, false to set items to -R
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to report progress (must not be
     *        <code>null</code>)
     */
    public static void markReadOnlyBit(
        final Workspace workspace,
        final boolean setReadOnly,
        final TaskMonitor taskMonitor) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(taskMonitor, "taskMonitor"); //$NON-NLS-1$

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    // We can use the total number of local items as the work
                    // size because of our all-inclusive query.
                    taskMonitor.begin("", lv.getLocalItemsCount()); //$NON-NLS-1$

                    for (final WorkspaceLocalItem lvEntry : lv.queryByLocalItem(null, RecursionType.FULL, null)) {
                        taskMonitor.setCurrentWorkDescription(lvEntry.getLocalItem());

                        final FileSystemAttributes attrs =
                            FileSystemUtils.getInstance().getAttributes(lvEntry.getLocalItem());

                        if (attrs != null && attrs.exists() && !attrs.isSymbolicLink() && !attrs.isDirectory()) {
                            if (!attrs.isReadOnly() && setReadOnly) {
                                /*
                                 * Make sure there is no pending edit on this
                                 * item. We want to leave items with pending
                                 * edits as -R.
                                 */
                                final LocalPendingChange pcEntry = pc.getByLocalVersion(lvEntry);

                                if (null == pcEntry || !pcEntry.isEdit()) {
                                    attrs.setReadOnly(true);

                                    if (!FileSystemUtils.getInstance().setAttributes(lvEntry.getLocalItem(), attrs)) {
                                        throw new VersionControlException(MessageFormat.format(
                                            //@formatter:off
                                            Messages.getString("LocalDataAccessLayer.ErrorSettingAttributesOnPathFormat"), //$NON-NLS-1$
                                            //@formatter:on
                                            lvEntry.getLocalItem()));
                                    }
                                }
                            } else if (attrs.isReadOnly() && !setReadOnly) {
                                attrs.setReadOnly(false);

                                if (!FileSystemUtils.getInstance().setAttributes(lvEntry.getLocalItem(), attrs)) {
                                    throw new VersionControlException(MessageFormat.format(
                                        //@formatter:off
                                        Messages.getString("LocalDataAccessLayer.ErrorSettingAttributesOnPathFormat"), //$NON-NLS-1$
                                        //@formatter:on
                                        lvEntry.getLocalItem()));
                                }
                            }
                        }

                        taskMonitor.worked(1);
                    }
                }
            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }

            taskMonitor.done();
        }
    }

    /**
     * <p>
     * Returns the set of baseline folders ($tf directories) for the given local
     * workspace.
     * </p>
     *
     * @param workspace
     *        The local workspace
     * @return The set of baseline folders for the local workspace
     */
    public static String[] getBaselineFolders(final Workspace workspace) {
        final List<String> toReturn = new ArrayList<String>();

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new WorkspacePropertiesTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp) {
                    for (final BaselineFolder folder : wp.getBaselineFolders()) {
                        toReturn.add(folder.getPath());
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

        return toReturn.toArray(new String[toReturn.size()]);
    }

    /**
     * <p>
     * Given a local workspace with an empty version table and a set of
     * WorkspaceItem objects (from QueryWorkspaceItems), constructs the local
     * version table for the workspace and returns a set of baseline requests
     * which must be executed to populate the baseline folders.
     * </p>
     *
     * @param workspace
     *        The local workspace
     * @param workspaceItems
     *        The set of WorkspaceItem objects for this workspace
     * @param taskMonitor
     *        the {@link TaskMonitor} to use to report progress (must not be
     *        <code>null</code>)
     * @return The set of baseline requests which must be executed
     */
    public static BaselineRequest[] populateLocalVersionTable(
        final Workspace workspace,
        final WorkspaceItem[] workspaceItems,
        final TaskMonitor taskMonitor) {
        Check.notNull(taskMonitor, "taskMonitor"); //$NON-NLS-1$
        taskMonitor.begin("", workspaceItems.length); //$NON-NLS-1$

        final List<BaselineRequest> toReturn = new ArrayList<BaselineRequest>();

        final boolean saveCheckinDate = workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN);

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new LocalVersionTransaction() {
                @Override
                public void invoke(final WorkspaceVersionTable lv) {
                    for (final WorkspaceItem workspaceItem : workspaceItems) {
                        final WorkspaceLocalItem lvEntry = new WorkspaceLocalItem();

                        lvEntry.setServerItem(
                            workspaceItem.getCommittedServerItem() != null ? workspaceItem.getCommittedServerItem()
                                : workspaceItem.getServerItem());
                        lvEntry.setItemID(workspaceItem.getItemID());
                        lvEntry.setVersion(workspaceItem.getChangeSetID());
                        lvEntry.setEncoding(workspaceItem.getEncoding().getCodePage());
                        lvEntry.setCheckinDate(
                            saveCheckinDate ? DotNETDate.toWindowsFileTimeUTC(workspaceItem.getCheckinDate()) : -1);
                        lvEntry.setDeleted(workspaceItem.getRecursiveChangeType().contains(ChangeType.DELETE));

                        if (!lvEntry.isDeleted()) {
                            lvEntry.setLocalItem(workspaceItem.getLocalItem());
                        }

                        if (!lvEntry.isDirectory()) {
                            lvEntry.setLastModifiedTime(-1);
                            lvEntry.setLength(workspaceItem.getContentLength());
                            lvEntry.setHashValue(workspaceItem.getContentHashValue());
                            lvEntry.setBaselineFileGUID(GUID.newGUID().getGUIDBytes());

                            // PropertyValues haven't been populated yet, query
                            // property value here
                            final ItemSet[] items = workspace.getClient().getItems(
                                new ItemSpec[] {
                                    new ItemSpec(lvEntry.getServerItem(), RecursionType.NONE)
                            },
                                LatestVersionSpec.INSTANCE,
                                DeletedState.ANY,
                                ItemType.ANY,
                                GetItemsOptions.NONE,
                                PropertyConstants.QUERY_ALL_PROPERTIES_FILTERS);

                            if (items != null
                                && items.length > 0
                                && items[0] != null
                                && items[0].getItems() != null
                                && items[0].getItems().length > 0
                                && items[0].getItems()[0] != null
                                && items[0].getItems()[0].getPropertyValues() != null) {
                                lvEntry.setPropertyValues(items[0].getItems()[0].getPropertyValues());
                            }

                            final BaselineRequest baselineOp = new BaselineRequest();

                            baselineOp.setDownloadURL(workspaceItem.getDownloadURL());
                            baselineOp.setHashValue(workspaceItem.getContentHashValue());
                            baselineOp.setBaselineFileGUID(lvEntry.getBaselineFileGUID());

                            if (!lvEntry.isDeleted()) {
                                final FileSystemAttributes attrs =
                                    FileSystemUtils.getInstance().getAttributes(lvEntry.getLocalItem());
                                if (attrs != null && attrs.getSize() == lvEntry.getLength()) {
                                    /*
                                     * By taking the file time on disk verbatim,
                                     * we might miss an edit if the user had an
                                     * unpended edit in their server workspace
                                     * which happened to leave the file size
                                     * identical. However, because there is no
                                     * pending edit on the item, the baseline
                                     * compression step will use the file on
                                     * disk as its data source (as opposed to
                                     * downloading the content) and we will
                                     * discover that the hash value does not
                                     * match. At that time, we will fall back to
                                     * downloaded content for the item, and mark
                                     * its LastModifiedTime as -1.
                                     */
                                    if (attrs.getModificationTime() != null) {
                                        lvEntry.setLastModifiedTime(
                                            attrs.getModificationTime().getWindowsFilesystemTime());
                                    }

                                    if (!workspaceItem.getChangeType().contains(ChangeType.EDIT)) {
                                        baselineOp.setSourceLocalItem(lvEntry.getLocalItem());
                                    }
                                }
                            }

                            if (!workspaceItem.getChangeType().contains(ChangeType.ADD)
                                && null != workspaceItem.getLocalItem()) {
                                /*
                                 * This determines what partition the baseline
                                 * ends up on. workspaceItem.LocalItem is
                                 * non-null whenever the item is mapped in the
                                 * workspace. (If the item has no local version
                                 * row, i.e. a pending delete or an item
                                 * underneath a pending delete, then it gets a
                                 * computed local item.)
                                 */
                                baselineOp.setBaselinePartitionLocalItem(lvEntry.getLocalItem());

                                toReturn.add(baselineOp);
                            }
                        }

                        lv.add(lvEntry);

                        taskMonitor.worked(1);
                    }
                }
            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }

            taskMonitor.done();
        }

        return toReturn.toArray(new BaselineRequest[toReturn.size()]);
    }

    public static void processConversionBaselineRequests(
        final Workspace workspace,
        final Iterable<BaselineRequest> requests) throws CoreCancelException {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(requests, "requests"); //$NON-NLS-1$

        final AtomicReference<BaselineFolderCollection> baselineFolderCollection =
            new AtomicReference<BaselineFolderCollection>();

        LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new WorkspacePropertiesTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp) {
                    baselineFolderCollection.set(new BaselineFolderCollection(workspace, wp.getBaselineFolders()));
                }
            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }
        }

        final AtomicReference<Iterable<BaselineRequest>> failedLocal = new AtomicReference<Iterable<BaselineRequest>>();

        baselineFolderCollection.get().processBaselineRequests(
            workspace,
            requests,
            true /* throwIfCanceled */,
            failedLocal);

        boolean hasAnyFailed = false;
        if (failedLocal.get() != null) {
            for (final BaselineRequest r : failedLocal.get()) {
                if (r != null) {
                    hasAnyFailed = true;
                    break;
                }
            }
        }

        if (hasAnyFailed) {
            /*
             * The set of BaselineRequests which had a populated LocalItem,
             * indicating that the content on the local disk is the committed
             * content. However, we hashed the content while gzipping it, and
             * found that the hash value did not match. (The length matched, or
             * we would not have put a local item on the BaselineRequest.) As a
             * result, we fell back to the download URL to fetch this content.
             *
             * We need to go back through this list and mark the corresponding
             * local version entries with a LastModifiedTime of -1 so that when
             * the scanner runs, these items are hashed again and discovered as
             * pending edits.
             */

            transaction = new LocalWorkspaceTransaction(workspace);
            try {
                transaction.execute(new LocalVersionTransaction() {

                    @Override
                    public void invoke(final WorkspaceVersionTable lv) {
                        for (final BaselineRequest request : failedLocal.get()) {
                            final WorkspaceLocalItem lvEntry = lv.getByLocalItem(request.getSourceLocalItem());

                            if (null != lvEntry) {
                                lv.removeByLocalItem(request.getSourceLocalItem(), false);
                                lvEntry.setLastModifiedTime(-1);
                                lv.add(lvEntry);
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
        }
    }

    private static void handleMoveUncommittedUpdate(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final MoveUncommittedLocalVersionUpdate update) {
        final WorkspaceLocalItem lvExisting = lv.getByLocalItem(update.getSourceLocalItem());

        if (null != lvExisting && !ServerPath.equals(update.getSourceServerItem(), lvExisting.getServerItem())) {
            Check.isTrue(!lvExisting.isCommitted(), "!lvExisting.isCommitted()"); //$NON-NLS-1$

            // Always queue the removal of this local version row for reconcile
            // to the server, even if we're "online" and sending local version
            // updates both to the local data access layer and to the server.
            lv.removeByServerItem(lvExisting.getServerItem(), lvExisting.isCommitted(), true);

            // Re-file the local version entry at the new location provided by
            // the update.
            final WorkspaceLocalItem lvEntry = lvExisting.clone();
            lvEntry.setServerItem(update.getSourceServerItem());
            lvEntry.setPendingReconcile(true);

            lv.add(lvEntry);
        }
    }

    private static boolean handleInitiallyDeletedLocalVersionUpdate(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final InitiallyDeletedLocalVersionUpdate update,
        final boolean setFileTimeToCheckin,
        final Set<byte[]> displacedBaselines) {
        Check.isTrue(update.isFullyPopulated(setFileTimeToCheckin), "update.isFullyPopulated(setFileTimeToCheckin)"); //$NON-NLS-1$

        final WorkspaceLocalItem lvExisting = lv.getByServerItem(update.getSourceServerItem(), update.isCommitted());
        boolean updateMissingBaseline = false;

        // If we are calling ULV for an uncommitted slot in the table, ensure
        // that we actually
        // have a pending change row. This is for the case where you are undoing
        // both a parent
        // rename and a child add simultaneously.
        if (!update.isCommitted() && null == pc.getByTargetServerItem(update.getSourceServerItem())) {
            return updateMissingBaseline;
        }

        if (null != lvExisting && lvExisting.hasBaselineFileGUID()) {
            wp.deleteBaseline(lvExisting.getBaselineFileGUID());
        }

        final WorkspaceLocalItem lvEntry = new WorkspaceLocalItem();
        lvEntry.setServerItem(update.getSourceServerItem());
        lvEntry.setVersion(update.getVersionLocal());
        lvEntry.setLocalItem(null);
        lvEntry.setItemID(update.getItemID());
        lvEntry.setHashValue(update.getBaselineHashValue());
        lvEntry.setEncoding(update.getEncoding());
        lvEntry.setBaselineFileGUID(update.getBaselineFileGUID());
        lvEntry.setPendingReconcile(true);
        lvEntry.setLength(update.getBaselineFileLength());
        lvEntry.setCheckinDate(update.getVersionLocalDate());
        lvEntry.setDeleted(true);

        // If the item is uncommitted, we don't need to download a baseline for
        // it, unless EnsureUpdatesFullyPopulated has already stuffed a download
        // URL into the update for us (for example on a branch, edit)
        if (VersionControlConstants.ENCODING_FOLDER != lvEntry.getEncoding()
            && (0 != lvEntry.getVersion() || null != update.getDownloadURL())) {
            // Instead, create a GUID and we'll put this
            // ClientLocalVersionUpdate in the return value of this method. The
            // caller will be responsible for downloading the baseline content.
            update.generateNewBaselineFileGuid();
            lvEntry.setBaselineFileGUID(update.getBaselineFileGUID());
            updateMissingBaseline = true;
        }

        lv.add(lvEntry);
        return updateMissingBaseline;
    }

    private static boolean handleClientLocalVersionUpdate(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ClientLocalVersionUpdate update,
        final boolean setFileTimeToCheckin,
        final Set<byte[]> displacedBaselines) {
        Check.isTrue(update.isFullyPopulated(setFileTimeToCheckin), "update.isFullyPopulated(setFileTimeToCheckin)"); //$NON-NLS-1$

        final WorkspaceLocalItem lvExisting = lv.getByServerItem(update.getSourceServerItem(), update.isCommitted());
        boolean updateMissingBaseline = false;

        if (update.getTargetLocalItem() != null && update.getTargetLocalItem().length() > 0) {
            // If we are calling ULV for an uncommitted slot in the table,
            // ensure that we actually
            // have a pending change row. This is for the case where you are
            // undoing both a parent
            // rename and a child add simultaneously.
            if (!update.isCommitted() && null == pc.getByTargetServerItem(update.getSourceServerItem())) {
                return updateMissingBaseline;
            }

            // Displace the baseline (if it exists) for the local version entry
            // which collides with us on local path.
            final WorkspaceLocalItem lvLocalPathCollision = lv.getByLocalItem(update.getTargetLocalItem());

            // If the entry which collides with us on local path is the same
            // entry that collides with us
            // on the primary key (ServerItem, IsCommitted), then there's no
            // work to do. But if we have
            // no collision on primary key at all, or the colliding entries are
            // different, then we need
            // displace the local path collision's entry.
            if (null != lvLocalPathCollision && (null == lvExisting || lvLocalPathCollision != lvExisting)) {
                if (null != lvLocalPathCollision.getBaselineFileGUID()) {
                    displacedBaselines.add(lvLocalPathCollision.getBaselineFileGUID());
                }

                lv.removeByServerItem(lvLocalPathCollision.getServerItem(), lvLocalPathCollision.isCommitted(), false);
            }

            final WorkspaceLocalItem lvEntry = new WorkspaceLocalItem();
            lvEntry.setServerItem(update.getSourceServerItem());
            lvEntry.setVersion(update.getVersionLocal());
            lvEntry.setLocalItem(update.getTargetLocalItem());
            lvEntry.setItemID(update.getItemID());
            lvEntry.setHashValue(update.getBaselineHashValue());
            lvEntry.setEncoding(update.getEncoding());
            lvEntry.setBaselineFileGUID(update.getBaselineFileGUID());
            lvEntry.setPendingReconcile(true);
            lvEntry.setLength(update.getBaselineFileLength());
            lvEntry.setCheckinDate(update.getVersionLocalDate());
            lvEntry.setPropertyValues(update.getPropertyValues());

            if (null != lvExisting && lvExisting.hasBaselineFileGUID()) {
                if (!lvEntry.hasBaselineFileGUID()
                    && -1 != lvExisting.getLength()
                    && lvExisting.getLength() == lvEntry.getLength()
                    && lvExisting.hasHashValue()
                    && lvEntry.hasHashValue()
                    && Arrays.equals(lvExisting.getHashValue(), lvEntry.getHashValue())) {
                    // The update request did not contain a baseline file GUID,
                    // but the existing entry
                    // has a baseline and the metadata about the baseline shows
                    // it is identical. We'll
                    // preserve the existing baseline in the new local version
                    // entry.

                    lvEntry.setBaselineFileGUID(lvExisting.getBaselineFileGUID());
                } else if (!lvEntry.hasBaselineFileGUID()
                    || !Arrays.equals(lvExisting.getBaselineFileGUID(), lvEntry.getBaselineFileGUID())) {
                    // The update request contains a baseline file GUID and it's
                    // different from the
                    // one on the current local version entry.

                    wp.deleteBaseline(lvExisting.getBaselineFileGUID());
                }
            }

            if (VersionControlConstants.ENCODING_FOLDER != update.getEncoding()
                && update.getTargetLocalItem() != null
                && update.getTargetLocalItem().length() > 0) {
                final File file = new File(update.getTargetLocalItem());

                if (-1 != update.getLastModifiedDate()) {
                    // The caller has provided an explicit last modified date
                    // value.
                    lvEntry.setLastModifiedTime(update.getLastModifiedDate());
                } else if (file.exists()) {
                    // Fetch the last modified time from the disk.
                    final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(file);
                    if (attrs != null && attrs.getModificationTime() != null) {
                        lvEntry.setLastModifiedTime(attrs.getModificationTime().getWindowsFilesystemTime());
                    }
                }
            }

            if (null != lvEntry.getBaselineFileGUID()) {
                // We might have re-added a reference to a displaced baseline.
                // If we did, remove it from the set of displaced baselines.
                displacedBaselines.remove(lvEntry.getBaselineFileGUID());
            } else if (VersionControlConstants.ENCODING_FOLDER != lvEntry.getEncoding() &&
            // If the item is uncommitted, we don't need to download a
            // baseline
            // for it, unless EnsureUpdatesFullyPopulated has already
            // stuffed a
            // download URL into the update for us (for example on a branch,
            // edit)
                (0 != lvEntry.getVersion() || null != update.getDownloadURL())) {
                // We're about to save out a local version entry with no
                // baseline file GUID!

                // Instead, create a GUID and we'll put this
                // ClientLocalVersionUpdate in the return value of this method.
                // The caller will be responsible for downloading the baseline
                // content.
                update.generateNewBaselineFileGUID();
                lvEntry.setBaselineFileGUID(update.getBaselineFileGUID());

                updateMissingBaseline = true;
            }

            lv.add(lvEntry);
        } else if (null != lvExisting) {
            // We have an existing local version entry for this item, and we're
            // trying to remove it from the table.
            if (update.getKeepLocalVersionEntryOnDelete()) {
                lv.markAsDeleted(lvExisting.getServerItem(), lvExisting.isCommitted(), true);
            } else {
                lv.removeByServerItem(lvExisting.getServerItem(), lvExisting.isCommitted(), true);

                if (null != lvExisting.getBaselineFileGUID()) {
                    wp.deleteBaseline(lvExisting.getBaselineFileGUID());
                }
            }
        }

        // Logic in support of partial scanner correctness. If a caller is using
        // UpdateLocalVersion and has provided a WorkspaceLock to the
        // transaction (as opposed to having the transaction create its own
        // lock) then we assume that the on-disk operations are being performed
        // under the protection of that lock (as in ProcessGetOperations). This
        // means that asynchronous scans are not occurring in response to these
        // actions while their local version updates are still outstanding. If
        // the opposite is the case -- that there is no lock and
        // UpdateLocalVersion is being used directly (i.e. an
        // UpdateLocalVersionQueue that you just new up and start using), then
        // we need to send the paths that we are touching to the scanner.
        if (LocalWorkspaceTransaction.getCurrent().ownsWorkspaceLock()) {
            if (null != lvExisting
                && null != lvExisting.getLocalItem()
                && !LocalPath.equals(lvExisting.getLocalItem(), update.getTargetLocalItem())) {
                LocalWorkspaceTransaction.getCurrent().getWorkspace().getWorkspaceWatcher().markPathChanged(
                    lvExisting.getLocalItem());
            }

            if (null != update.getTargetLocalItem()
                && (null == lvExisting
                    || null == lvExisting.getLocalItem()
                    || !LocalPath.equals(lvExisting.getLocalItem(), update.getTargetLocalItem()))) {
                LocalWorkspaceTransaction.getCurrent().getWorkspace().getWorkspaceWatcher().markPathChanged(
                    update.getTargetLocalItem());
            }
        }

        return updateMissingBaseline;
    }

    /**
     * Returns true if item exists in the WorkspaceVersionTable and is a folder.
     *
     *
     * @param lv
     * @param pc
     * @param targetServerItem
     * @return
     */
    private static boolean isDirectory(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final String targetServerItem) {
        // If this is team project, we treat it always as a folder
        if (ServerPath.isTeamProject(targetServerItem)) {
            return true;
        }

        final Iterable<WorkspaceLocalItem> list = ParsedItemSpec.queryLocalVersionsByTargetServerItem(
            lv,
            pc,
            targetServerItem,
            RecursionType.NONE,
            null,
            ParsedItemSpecOptions.INCLUDE_DELETED);

        if (list.iterator().hasNext()) {
            final WorkspaceLocalItem lvEntry = list.iterator().next();
            return lvEntry.isDirectory();
        }

        return false;
    }

    private static boolean isMove(
        final String committedTargetServerItem,
        final ItemType specifiedType,
        final WorkspaceVersionTable lv) {
        if (specifiedType != ItemType.ANY) {
            return specifiedType == ItemType.FOLDER;
        }
        WorkspaceLocalItem lvEntry = null;
        boolean itemFound = true;

        // for server items we look in the committed space
        lvEntry = lv.getByServerItem(committedTargetServerItem, true);
        if (lvEntry == null) {
            lvEntry = lv.getByServerItem(committedTargetServerItem, false);
        }
        if (lvEntry == null) {
            lvEntry = lv.getByServerItem(ServerPath.getParent(committedTargetServerItem), true);
            itemFound = false;
        }
        if (lvEntry != null && itemFound) {
            // item found, return it's type
            return lvEntry.isDirectory();
        } else if (lvEntry != null) {
            // parent was found but not item itself, we treat it as file, so we
            // do rename
            return false;
        }
        // we didn't find item nor parent, we treat it as folder and we will do
        // move
        return true;
    }

    /**
     * Calculates target server item based on the source and target roots of the
     * rename, source path and whether targetServerRoot is existing folder or
     * not.
     *
     *
     * @param sourceServerRoot
     * @param sourceServerItem
     * @param targetServerRoot
     * @param targetIsFolder
     * @param rootIsRenamed
     * @return
     */
    private static String calculateTargetServerItem(
        final String sourceServerRoot,
        final String sourceServerItem,
        final String targetServerRoot,
        final boolean targetIsFolder,
        final boolean rootIsRenamed) {
        String sourceSuffix = ""; //$NON-NLS-1$
        String targetFolder = ""; //$NON-NLS-1$

        // if we rename A* into Alpha, A* will include both Alpha and a.txt,
        // when we proces Alpha->Alpha we should just return Alpha, not
        // Alpha\Alpha
        final boolean sourceRenamedIntoTargetRoot = ServerPath.equals(sourceServerItem, targetServerRoot);

        // if we rename A to B and B exists we should create B\A if we
        // move whole A, but not if we move A\* (rootIsRenamed is false
        // then)
        if (targetIsFolder
            && rootIsRenamed
            && !ServerPath.equals(sourceServerRoot, targetServerRoot)
            && !sourceRenamedIntoTargetRoot) {
            // if target exists, we need to create additional item in target
            targetFolder = ServerPath.getFileName(sourceServerRoot);
        }
        if (!ServerPath.equals(sourceServerItem, sourceServerRoot) && !sourceRenamedIntoTargetRoot) {
            sourceSuffix = ServerPath.makeRelative(sourceServerItem, sourceServerRoot);
        }

        final String result = ServerPath.combine(ServerPath.combine(targetServerRoot, targetFolder), sourceSuffix);
        return result;
    }

    /**
     * Provides server item for specified item. If item is server item, just
     * returns it. If item is found in local version table, returns server item
     * from there, with parent renames applied. Otherwise uses workspace to
     * translate local path to server path.
     *
     *
     * @param item
     *        The source control item.
     * @param lv
     *        The local version table.
     * @param pc
     *        The pending changes table.
     * @param workspace
     *        The workspace.
     * @return
     */
    private static String tryGetServerItem(
        final String item,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc) {
        if (ServerPath.isServerPath(item)) {
            return item;
        }

        // using GetByLocalItem instead TryGetServerItemForLocalItem fixes
        // problems with workspace mappings changed without running get
        final WorkspaceLocalItem lvEntry = lv.getByLocalItem(item);
        if (lvEntry != null && lvEntry.isCommitted()) {
            return pc.getTargetServerItemForCommittedServerItem(lvEntry.getServerItem());
        } else if (lvEntry != null) {
            return lvEntry.getServerItem();
        }
        return WorkingFolder.getServerItemForLocalItem(item, wp.getWorkingFolders());
    }

    /**
     * Utility method to create a failure caused by a repository path which is
     * too long.
     */
    private static Failure createPathTooLongFailure(final String repositoryPath) {
        return new Failure(
            createPathTooLongErrorText(repositoryPath),
            FailureCodes.REPOSITORY_PATH_TOO_LONG_EXCEPTION,
            SeverityType.ERROR,
            repositoryPath);
    }

    /**
     * Utility method to create a failure for pending delete conflict change.
     */
    private static Failure createPendingDeleteConflictChangeFailure(final String repositoryPath) {
        final String format = Messages.getString("LocalDataAccessLayer.PendingDeleteConflictChangeFormat"); //$NON-NLS-1$

        return new Failure(
            MessageFormat.format(format, repositoryPath),
            FailureCodes.PENDING_DELETE_CONFLICT_CHANGE_EXCEPTION,
            SeverityType.ERROR,
            repositoryPath);
    }

    /**
     * Utility method to create a failure for item not mapped.
     */
    private static Failure createItemNotMappedFailure(final String repositoryPath) {
        final String format = Messages.getString("LocalDataAccessLayer.ItemNotMappedExceptionFormat"); //$NON-NLS-1$

        return new Failure(
            MessageFormat.format(format, repositoryPath),
            FailureCodes.ITEM_NOT_MAPPED_EXCEPTION,
            SeverityType.ERROR,
            repositoryPath);
    }

    private static Failure createItemCloakedFailure(final String repositoryPath) {
        final String format = Messages.getString("LocalDataAccessLayer.ItemCloakedExceptionFormat"); //$NON-NLS-1$

        return new Failure(
            MessageFormat.format(format, repositoryPath),
            FailureCodes.ITEM_CLOAKED_EXCEPTION,
            SeverityType.ERROR,
            repositoryPath);
    }

    /**
     * Utility method to create a failure for pending parent delete.
     */
    private static Failure createPendingParentDeleteFailure(final String repositoryPath) {
        return createPendingParentDeleteFailure(repositoryPath, repositoryPath);
    }

    /**
     * Utility method to create a failure for pending parent delete.
     */
    private static Failure createPendingParentDeleteFailure(final String path1, final String path2) {
        final String format = Messages.getString("LocalDataAccessLayer.PendingParentDeleteExceptionFormat"); //$NON-NLS-1$

        return new Failure(
            MessageFormat.format(format, path1),
            FailureCodes.PENDING_PARENT_DELETE_EXCEPTION,
            SeverityType.ERROR,
            path2);
    }

    /**
     * Utility method to create an exception caused by a repository path which
     * is too long.
     */
    private static RepositoryPathTooLongException createPathTooLongException(final String repositoryPath) {
        return new RepositoryPathTooLongException(createPathTooLongErrorText(repositoryPath));
    }

    /**
     * Utility method to create a failure display string for a repository path
     * which is too long.
     */
    private static String createPathTooLongErrorText(final String repositoryPath) {
        final String format = Messages.getString("LocalDataAccessLayer.RepositoryPathTooLongFormat"); //$NON-NLS-1$
        return MessageFormat.format(format, repositoryPath);
    }

    /*
     * Project rename related methods
     */

    private static void ProcessProjectRenames(
        final Workspace workspace,
        final WebServiceLayer webServiceLayer,
        final List<KeyValuePair<String, String>> serverRenames,
        final int newProjectRevisionId) {
        log.info("Process project renames"); //$NON-NLS-1$

        if (serverRenames.isEmpty()) {
            log.warn("Expected to receive at least one rename instruction, but none have bee received"); //$NON-NLS-1$
            Check.isTrue(false, "Expected to receive at least one rename instruction"); //$NON-NLS-1$

            // Acknowledgment outside of a transaction; no work to do
            webServiceLayer.promotePendingWorkspaceMappings(
                workspace.getName(),
                workspace.getOwnerName(),
                newProjectRevisionId);

            return;
        }

        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new AllTablesTransaction() {
                @Override
                public void invoke(
                    final LocalWorkspaceProperties wp,
                    final WorkspaceVersionTable lv,
                    final LocalPendingChangesTable pc) {
                    final Set<String> currentProjectNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                    currentProjectNames.addAll(getCurrentProjectNames(wp, lv, pc));

                    final Set<String> newProjectNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                    newProjectNames.addAll(currentProjectNames);

                    /*
                     * 1. Form the set of new project names by transforming the
                     * current project names
                     */
                    {
                        for (final KeyValuePair<String, String> rename : serverRenames) {
                            // Remove the old project names
                            newProjectNames.remove(rename.getKey());
                        }

                        for (final KeyValuePair<String, String> rename : serverRenames) {
                            // Add the new project name
                            newProjectNames.add(rename.getValue());
                        }
                    }

                    /*
                     * 2. If there are any incoming project renames where the
                     * target name is currently occupied and is not being
                     * cleared, then we need to mock an additional rename
                     * operation that clears that name, or we will not be able
                     * to succeed.
                     */
                    final Map<String, String> allRenames = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

                    {
                        final Set<String> renamesFrom = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
                        for (final KeyValuePair<String, String> rename : serverRenames) {
                            renamesFrom.add(rename.getKey());
                        }

                        final List<KeyValuePair<String, String>> injectedRenames =
                            new ArrayList<KeyValuePair<String, String>>();

                        for (final KeyValuePair<String, String> rename : serverRenames) {
                            final String renameTo = rename.getValue();

                            if (currentProjectNames.contains(renameTo) && !renamesFrom.contains(renameTo)) {
                                // We need to insert a mock rename from
                                // newProjectName to a vacant name whose name
                                // has been randomly generated.
                                // The randomly generated name needs to be
                                // vacant and of equal or lesser length to
                                // newProjectName.
                                final String replacementName = createProjectName(renameTo.length(), newProjectNames);

                                injectedRenames.add(new KeyValuePair<String, String>(renameTo, replacementName));
                                newProjectNames.add(replacementName);
                            }
                        }

                        for (final KeyValuePair<String, String> rename : serverRenames) {
                            allRenames.put(rename.getKey(), rename.getValue());
                        }

                        for (final KeyValuePair<String, String> rename : injectedRenames) {
                            allRenames.put(rename.getKey(), rename.getValue());
                        }
                    }

                    // Working folders
                    {
                        final List<WorkingFolder> newWorkingFolders = new ArrayList<WorkingFolder>();

                        for (final WorkingFolder wf : wp.getWorkingFolders()) {
                            newWorkingFolders.add(
                                new WorkingFolder(
                                    mapServerItem(wf.getServerItem(), allRenames),
                                    wf.getLocalItem(),
                                    wf.getType(),
                                    wf.getDepth()));
                        }

                        wp.setWorkingFolders(newWorkingFolders.toArray(new WorkingFolder[newWorkingFolders.size()]));
                        workspace.getWebServiceObject().setFolders(
                            (_WorkingFolder[]) WrapperUtils.unwrap(_WorkingFolder.class, wp.getWorkingFolders()));
                    }

                    // Local version entries
                    lv.renameTeamProjects(new ServerItemMapper() {
                        @Override
                        public String map(final String itemPath) {
                            return mapServerItem(itemPath, allRenames);
                        }
                    });

                    // Pending changes
                    pc.renameTeamProjects(new ServerItemMapper() {
                        @Override
                        public String map(final String itemPath) {
                            return mapServerItem(itemPath, allRenames);
                        }
                    });

                    // Save the new project revision ID. We will acknowledge the
                    // completion of these renames on the next reconcile.
                    wp.setNewProjectRevisionId(newProjectRevisionId);
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

    private static String createProjectName(final int length, final Set<String> extantNames) {
        if (length < 1) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.getString("LocalDataAccessLayer.ValueOutOfRangeFormat"), "length")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        for (int i = 0; i < 1024; i++) {
            final StringBuilder sb = new StringBuilder(length);
            final Random r = new Random();

            for (int j = 0; j < length; j++) {
                sb.append(characterCorpus.charAt(r.nextInt(characterCorpus.length())));
            }

            final String toReturn = sb.toString();

            if (!extantNames.contains(toReturn)) {
                return toReturn;
            }
        }

        throw new RuntimeException(
            MessageFormat.format(
                Messages.getString("LocalDataAccessLayer.UnableToFormReplacementTeamProjectNameFormat"), //$NON-NLS-1$
                length));
    }

    private static String mapServerItem(final String serverItem, final Map<String, String> projectRenames) {
        final String teamProjectName = ServerPath.getTeamProjectName(serverItem);

        if (StringUtil.isNullOrEmpty(teamProjectName) || !projectRenames.containsKey(teamProjectName)) {
            return serverItem;
        } else {
            final String newTeamProjectName = projectRenames.get(teamProjectName);
            return ServerPath.ROOT + newTeamProjectName + serverItem.substring(2 + teamProjectName.length());
        }
    }

    private static List<String> getCurrentProjectNames(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc) {
        final List<String> projectNames = new ArrayList<String>();

        for (final String currentServerItem : getCurrentServerItems(wp, lv, pc)) {
            final String teamProjectName = ServerPath.getTeamProjectName(currentServerItem);

            // The team project name of $/ is String.Empty.
            // Don't emit it as a result.
            if (!StringUtil.isNullOrEmpty(teamProjectName)) {
                projectNames.add(teamProjectName);
            }
        }

        return projectNames;
    }

    private static List<String> getCurrentServerItems(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc) {
        final List<String> serverItems = new ArrayList<String>();

        for (final WorkingFolder folder : wp.getWorkingFolders()) {
            serverItems.add(folder.getServerItem());
        }

        serverItems.addAll(lv.getKnownServerItems());
        serverItems.addAll(pc.getKnownServerItems());

        return serverItems;
    }
    /*
     * ******************************************************************
     */
}
