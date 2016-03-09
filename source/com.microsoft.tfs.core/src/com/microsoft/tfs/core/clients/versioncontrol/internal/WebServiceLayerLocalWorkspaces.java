// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.QueryMergesExtendedOptions;
import com.microsoft.tfs.core.clients.versioncontrol.ResolveErrorOptions;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.UploadedBaselinesCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ResourceAccessException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolveErrorHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolvedHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.AllTablesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalDataAccessLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalPendingChangesTable;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ArtifactPropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMergeDetails;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelChildOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalVersion;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.CheckinNotificationInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.exceptions.mappers.VersionControlExceptionMapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.tfs.versioncontrol.clientservices._03._BranchRelative;
import ms.tfs.versioncontrol.clientservices._03._ChangeRequest;
import ms.tfs.versioncontrol.clientservices._03._ItemSpec;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_PendChangesInLocalWorkspaceResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_QueryPendingSetsWithLocalWorkspacesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_UndoPendingChangesInLocalWorkspaceResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_PendChangesInLocalWorkspaceResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_QueryPendingSetsWithLocalWorkspacesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_UndoPendingChangesInLocalWorkspaceResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap;

public class WebServiceLayerLocalWorkspaces extends WebServiceLayer {
    private final static Log log = LogFactory.getLog(WebServiceLayerLocalWorkspaces.class);

    // The pending changes signature for all newly created workspaces (which by
    // definition have no pending changes)
    public static final GUID INITIAL_PENDING_CHANGES_SIGNATURE = new GUID("B2140B25-F70A-4B4D-BFB1-184703037010"); //$NON-NLS-1$

    /**
     * @see WebServiceLayer
     */
    public WebServiceLayerLocalWorkspaces(
        final VersionControlClient client,
        final _RepositorySoap repository,
        final _RepositoryExtensionsSoap repositoryExtensions,
        final _Repository4Soap repository4,
        final _Repository5Soap repository5) {
        super(client, repository, repositoryExtensions, repository4, repository5);
    }

    /*
     * If the workspace name and owner name provided are a local workspace on
     * this computer, the Workspace object will be placed in the out parameter
     * and true will be returned. Otherwise localWorkspace will be set to null
     * and false returned.
     */
    protected Workspace getLocalWorkspace(final String workspaceName, final String ownerName) {
        Workspace localWorkspace = null;

        final Workspace workspace = getWorkspace(workspaceName, ownerName);

        if (null != workspace && WorkspaceLocation.LOCAL == workspace.getLocation() && workspace.isLocal()) {
            localWorkspace = workspace;
        }

        return localWorkspace;
    }

    /**
     * If the item provided is a local path, and the workspace name and owner
     * provided are for a local workspace, that local workspace will be
     * reconciled.
     */
    protected Workspace reconcileIfLocalItem(
        final String workspaceName,
        final String ownerName,
        final String item,
        final boolean unscannedReconcile) {
        if (item != null && item.length() > 0) {
            return reconcileIfAnyLocalItems(workspaceName, ownerName, new String[] {
                item
            }, unscannedReconcile);
        }

        return null;
    }

    /**
     * If the item provided is a local path, and the workspace name and owner
     * provided are for a local workspace, that local workspace will be
     * reconciled.
     */
    protected Workspace reconcileIfLocalItem(final String workspaceName, final String ownerName, final String item) {
        if (item != null && item.length() > 0) {
            return reconcileIfAnyLocalItems(workspaceName, ownerName, new String[] {
                item
            });
        }

        return null;
    }

    /**
     * If the itemspec provided is a local path, and the workspace name and
     * owner provided are for a local workspace, that local workspace will be
     * reconciled.
     */
    protected Workspace reconcileIfLocalItemSpec(
        final String workspaceName,
        final String ownerName,
        final ItemSpec itemSpec,
        final boolean unscannedReconcile) {
        if (null != itemSpec) {
            return reconcileIfAnyLocalItemSpecs(workspaceName, ownerName, new ItemSpec[] {
                itemSpec
            }, unscannedReconcile);
        }

        return null;
    }

    protected Workspace reconcileIfLocalItemSpec(
        final String workspaceName,
        final String ownerName,
        final ItemSpec itemSpec) {
        if (null != itemSpec) {
            return reconcileIfAnyLocalItemSpecs(workspaceName, ownerName, new ItemSpec[] {
                itemSpec
            }, false);
        }

        return null;
    }

    /**
     * If at least one of the items provided is a local path, and the workspace
     * name and owner provided are for a local workspace, that local workspace
     * will be reconciled.
     */
    protected Workspace reconcileIfAnyLocalItems(
        final String workspaceName,
        final String ownerName,
        final String[] items) {
        return reconcileIfAnyLocalItems(workspaceName, ownerName, items, false);
    }

    /**
     * If at least one of the items provided is a local path, and the workspace
     * name and owner provided are for a local workspace, that local workspace
     * will be reconciled.
     */
    protected Workspace reconcileIfAnyLocalItems(
        final String workspaceName,
        final String ownerName,
        final String[] items,
        final boolean unscannedReconcile) {
        boolean atLeastOneLocalItem = false;

        if (null != items) {
            for (final String item : items) {
                if (null != item && ServerPath.isServerPath(item) == false) {
                    atLeastOneLocalItem = true;
                    break;
                }
            }
        }

        if (atLeastOneLocalItem) {
            return reconcileIfLocal(workspaceName, ownerName, unscannedReconcile, false, false, null);
        }

        return null;
    }

    protected Workspace reconcileIfAnyLocalItemSpecs(
        final String workspaceName,
        final String ownerName,
        final ItemSpec[] itemSpecs) {
        return reconcileIfAnyLocalItemSpecs(workspaceName, ownerName, itemSpecs, false);
    }

    /**
     * If at least one of the itemspecs provided is a local path, and the
     * workspace name and owner provided are for a local workspace, that local
     * workspace will be reconciled.
     */
    protected Workspace reconcileIfAnyLocalItemSpecs(
        final String workspaceName,
        final String ownerName,
        final ItemSpec[] itemSpecs,
        final boolean unscannedReconcile) {
        boolean atLeastOneLocalItemSpec = false;

        if (null != itemSpecs) {
            for (final ItemSpec itemSpec : itemSpecs) {
                if (null != itemSpec
                    && null != itemSpec.getItem()
                    && ServerPath.isServerPath(itemSpec.getItem()) == false) {
                    atLeastOneLocalItemSpec = true;
                    break;
                }
            }
        }

        if (atLeastOneLocalItemSpec) {
            return reconcileIfLocal(workspaceName, ownerName, unscannedReconcile, false, false, null);
        }

        return null;
    }

    /**
     * If the VersionSpec provided is a WorkspaceVersionSpec and the workspace
     * for that WorkspaceVersionSpec is a local workspace on this computer, that
     * local workspace will be reconciled.
     */
    public Workspace reconcileIfLocalVersionSpec(final VersionSpec versionSpec) {
        return reconcileIfLocalVersionSpec(versionSpec, false);
    }

    /**
     * If the VersionSpec provided is a WorkspaceVersionSpec and the workspace
     * for that WorkspaceVersionSpec is a local workspace on this computer, that
     * local workspace will be reconciled.
     */
    public Workspace reconcileIfLocalVersionSpec(final VersionSpec versionSpec, final boolean unscannedReconcile) {
        if (versionSpec instanceof WorkspaceVersionSpec) {
            final WorkspaceVersionSpec workspaceVersionSpec = (WorkspaceVersionSpec) versionSpec;

            return reconcileIfLocal(
                workspaceVersionSpec.getName(),
                getVersionControlClient().resolveUserUniqueName(workspaceVersionSpec.getOwner()),
                unscannedReconcile,
                false,
                false,
                null);
        }

        return null;
    }

    protected Workspace reconcileIfLocal(final String workspaceName, final String ownerName) {
        return reconcileIfLocal(workspaceName, ownerName, false, false, false, null);
    }

    protected Workspace reconcileIfLocal(
        final String workspaceName,
        final String ownerName,
        final AtomicBoolean reconciled) {
        return reconcileIfLocal(workspaceName, ownerName, false, false, false, null);
    }

    /**
     * If the workspace name and owner provided correspond to a local workspace
     * on this computer, that local workspace will be reconciled.
     */
    protected Workspace reconcileIfLocal(
        final String workspaceName,
        final String ownerName,
        final boolean unscannedReconcile,
        final boolean reconcileMissingLocalItems,
        final boolean skipIfAccessDenied,
        final AtomicBoolean reconciled) {
        if (reconciled != null) {
            reconciled.set(false);
        }

        if (workspaceName == null || workspaceName.length() == 0 || ownerName == null || ownerName.length() == 0) {
            return null;
        }

        Workspace localWorkspace = null;

        if ((localWorkspace = getLocalWorkspace(workspaceName, ownerName)) != null) {
            final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();
            final AtomicBoolean pendingChangesUpdatedByServer = new AtomicBoolean();

            try {
                final boolean wasReconciled = LocalDataAccessLayer.reconcileLocalWorkspace(
                    localWorkspace,
                    this,
                    unscannedReconcile,
                    reconcileMissingLocalItems,
                    failures,
                    pendingChangesUpdatedByServer);

                if (wasReconciled) {
                    localWorkspace.invalidateMappings();
                    localWorkspace.refreshMappingsIfNeeded();
                }

                if (reconciled != null) {
                    reconciled.set(wasReconciled);
                }
            } catch (final ResourceAccessException e) {
                if (!skipIfAccessDenied) {
                    throw e;
                }

                return null;
            }

            getVersionControlClient().reportFailures(localWorkspace, failures.get());
        }

        return localWorkspace;
    }

    /**
     * @equivalence syncPendingChangesIfLocal(workspace, new GetOperation[0],
     *              itemPropertyFilters)
     */
    protected void syncPendingChangesIfLocal(final Workspace workspace, final String[] itemPropertyFilters) {
        syncPendingChangesIfLocal(workspace, new GetOperation[0], itemPropertyFilters);
    }

    /**
     * If the workspace name and owner provided correspond to a local workspace
     * on this computer, that local workspace will dump its pending changes and
     * reload them from the server by making a call to QueryPendingSets.
     */
    protected void syncPendingChangesIfLocal(
        final Workspace workspace,
        final GetOperation[] getOperations,
        final String[] itemPropertyFilters) {
        if (workspace != null && workspace.getLocation() == WorkspaceLocation.LOCAL) {
            LocalDataAccessLayer.syncPendingChanges(workspace, getOperations, itemPropertyFilters);
        }
    }

    /**
     * Returns a workspace lock suitable for use in a using block
     */
    protected WorkspaceLock lockIfLocal(final Workspace workspace) {
        if (null != workspace) {
            try {
                return workspace.lock();
            } catch (final Exception e) {
                throw new VersionControlException(e.getLocalizedMessage());
            }
        }

        return null;
    }

    /**
     * Given a local workspace, returns all pending changes for that workspace
     * by calling QueryPendingSets on the server. Also returns the current
     * pending change signature from the server.
     */
    @Override
    public PendingChange[] queryServerPendingChanges(
        final Workspace localWorkspace,
        final AtomicReference<GUID> outServerPendingChangeSignature) {
        Check.isTrue(
            WorkspaceLocation.LOCAL == localWorkspace.getLocation(),
            "WorkspaceLocation.LOCAL == localWorkspace.getLocation()"); //$NON-NLS-1$

        Failure[] failures;
        PendingSet[] pendingSets;

        final ItemSpec[] itemSpecs = new ItemSpec[] {
            new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
        };

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_QueryPendingSetsWithLocalWorkspacesResponse response =
                    getRepository5().queryPendingSetsWithLocalWorkspaces(
                        null,
                        null,
                        localWorkspace.getName(),
                        localWorkspace.getOwnerName(),
                        (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                        false /* generateDownloadUrls */,
                        null,
                        VersionControlConstants.MAX_SERVER_PATH_SIZE);

                pendingSets = (PendingSet[]) WrapperUtils.wrap(
                    PendingSet.class,
                    response.getQueryPendingSetsWithLocalWorkspacesResult());

                failures = (Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures());
            } else {
                final _Repository4Soap_QueryPendingSetsWithLocalWorkspacesResponse response =
                    getRepository4().queryPendingSetsWithLocalWorkspaces(
                        null,
                        null,
                        localWorkspace.getName(),
                        localWorkspace.getOwnerName(),
                        (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                        false /* generateDownloadUrls */,
                        null);

                pendingSets = (PendingSet[]) WrapperUtils.wrap(
                    PendingSet.class,
                    response.getQueryPendingSetsWithLocalWorkspacesResult());

                failures = (Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        getVersionControlClient().reportFailures(localWorkspace, failures);

        if (pendingSets.length == 0) {
            outServerPendingChangeSignature.set(
                queryPendingChangeSignature(localWorkspace.getName(), localWorkspace.getOwnerName()));
            return new PendingChange[0];
        } else {
            outServerPendingChangeSignature.set(pendingSets[0].getPendingChangeSignature());

            // If the server does not have the change where
            // PendingChangeSignature comes down with PendingSet objects, then
            // we'll have to go fetch it ourselves. This change was made to the
            // server in the Dev11 CTP3 iteration.
            if (outServerPendingChangeSignature.get().equals(GUID.EMPTY)) {
                outServerPendingChangeSignature.set(
                    queryPendingChangeSignature(localWorkspace.getName(), localWorkspace.getOwnerName()));
            }

            return pendingSets[0].getPendingChanges();
        }
    }

    /**
     * Given a workspace, returns all pending changes for that workspace by
     * calling QueryPendingSets on the server.
     */
    @Override
    public PendingChange[] queryServerPendingChanges(
        final Workspace workspace,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final String[] itemPropertyFilters) {
        Failure[] failures;
        PendingSet[] pendingSets;

        if (workspace.getLocation().equals(WorkspaceLocation.LOCAL)) {
            final _Repository4Soap_QueryPendingSetsWithLocalWorkspacesResponse response;

            try {
                response = getRepository4().queryPendingSetsWithLocalWorkspaces(
                    null,
                    null,
                    workspace.getName(),
                    workspace.getOwnerName(),
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls,
                    null /*
                          * TODO pass itemPropertyFilters, but servers <=
                          * 2011-10-19 throw null ref exception if you do
                          */);
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }

            pendingSets = (PendingSet[]) WrapperUtils.wrap(
                PendingSet.class,
                response.getQueryPendingSetsWithLocalWorkspacesResult());

            failures = (Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures());
        } else {
            final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();

            pendingSets = super.queryPendingSets(
                null,
                null,
                workspace.getName(),
                workspace.getOwnerName(),
                itemSpecs,
                generateDownloadUrls,
                failuresHolder,
                false,
                null);

            failures = failuresHolder.get();
        }

        getVersionControlClient().reportFailures(workspace, failures);

        if (pendingSets.length == 0) {
            return new PendingChange[0];
        } else {
            return pendingSets[0].getPendingChanges();
        }
    }

    /**
     * After a server call which may have updated the working folders of the
     * given local workspace, call SyncWorkingFoldersIfNecessary and pass the
     * ChangePendedFlags value returned by the server. If the value indicates
     * the working folders have been updated, then the working folders in the
     * workspace properties table will be synced with those on the server.
     *
     *
     * @param localWorkspace
     *        The local workspace whose mappings may have changed
     * @param flags
     *        The ChangePendedFlags returned by the server call
     */
    public void syncWorkingFoldersIfNecessary(final Workspace localWorkspace, final ChangePendedFlags flags) {
        if (null != localWorkspace) {
            if (flags.contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
                final Workspace serverUpdatedWorkspace =
                    super.queryWorkspace(localWorkspace.getName(), localWorkspace.getOwnerName());

                if (null != serverUpdatedWorkspace) {
                    LocalDataAccessLayer.setWorkingFolders(localWorkspace, serverUpdatedWorkspace.getFolders());
                }
            }
        }
    }

    @Override
    public void addConflict(
        final String workspaceName,
        final String ownerName,
        final ConflictType conflictType,
        final int itemId,
        final int versionFrom,
        final int pendingChangeId,
        final String sourceLocalItem,
        final String targetLocalItem,
        final OperationStatus reason,
        final String[] itemPropertyFilters) {
        final Workspace w = reconcileIfLocal(workspaceName, ownerName);
        final WorkspaceLock lock = lockIfLocal(w);

        try {
            super.addConflict(
                workspaceName,
                ownerName,
                conflictType,
                itemId,
                versionFrom,
                pendingChangeId,
                sourceLocalItem,
                targetLocalItem,
                reason,
                itemPropertyFilters);

            // If we create a merge conflict with AddConflict, then we need to
            // sync the pending changes from the server, since that will change
            // the HasMergeConflict bit on the corresponding LocalPendingChange.
            if (conflictType == ConflictType.MERGE) {
                syncPendingChangesIfLocal(w, itemPropertyFilters);
            }
        } finally {
            if (lock != null) {
                lock.close();
            }
        }

    }

    @Override
    public CheckinResult checkIn(
        final String workspaceName,
        final String ownerName,
        final String[] serverItems,
        final Changeset info,
        final CheckinNotificationInfo checkinNotificationInfo,
        final CheckinFlags checkinOptions,
        final UploadedBaselinesCollection uploadedBaselinesCollection,
        final AtomicReference<Failure[]> conflicts,
        final AtomicReference<Failure[]> failures,
        final boolean deferCheckIn,
        final int checkInTicket,
        final String[] itemPropertyFilters) {
        final Workspace w = reconcileIfLocal(workspaceName, ownerName, true, false, false, null);
        final WorkspaceLock lock = lockIfLocal(w);

        try {
            final CheckinResult toReturn = super.checkIn(
                workspaceName,
                ownerName,
                serverItems,
                info,
                checkinNotificationInfo,
                checkinOptions,
                uploadedBaselinesCollection,
                conflicts,
                failures,
                deferCheckIn,
                checkInTicket,
                itemPropertyFilters);

            if (w != null && w.getLocation() == WorkspaceLocation.LOCAL) {
                if (toReturn.getChangeset() > 0) {
                    LocalDataAccessLayer.afterCheckin(
                        w,
                        toReturn.getChangeset(),
                        toReturn.getDate(),
                        toReturn.getLocalVersionUpdates(),
                        queryServerPendingChanges(w, itemPropertyFilters),
                        uploadedBaselinesCollection);
                } else if (0 == toReturn.getChangeset()) {
                    // All pending changes were undone
                    syncPendingChangesIfLocal(w, itemPropertyFilters);
                }
            }

            return toReturn;
        } finally {
            if (lock != null) {
                lock.close();
            }
        }
    }

    @Override
    public Failure[] checkPendingChanges(
        final String workspaceName,
        final String ownerName,
        final String[] serverItems) {
        reconcileIfLocal(workspaceName, ownerName);

        return super.checkPendingChanges(workspaceName, ownerName, serverItems);
    }

    @Override
    public GetOperation[][] get(
        final String workspaceName,
        final String ownerName,
        final GetRequest[] requests,
        final int maxResults,
        final GetOptions options,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters,
        final boolean noGet) {
        final Workspace localWorkspace = getLocalWorkspace(workspaceName, ownerName);
        if (localWorkspace != null) {
            // The change #5491 has been rolled back as not related to the bug
            // it tried to fix:
            // Bug 6191: When using local workspaces, get latest does not get a
            // file that has been deleted from disk.

            boolean reconcileMissingLocalItems = false;

            for (final GetRequest getRequest : requests) {
                if (null == getRequest) {
                    continue;
                }

                final VersionSpec versionSpec = getRequest.getVersionSpec();

                if (null == versionSpec
                    || (versionSpec instanceof WorkspaceVersionSpec
                        && (!Workspace.matchName(
                            localWorkspace.getName(),
                            ((WorkspaceVersionSpec) versionSpec).getName())
                            || !localWorkspace.ownerNameMatches(((WorkspaceVersionSpec) versionSpec).getOwner())))) {
                    reconcileMissingLocalItems = true;

                    break;
                }
            }

            final AtomicBoolean reconciled = new AtomicBoolean();

            reconcileIfLocal(
                workspaceName,
                ownerName,
                false /* unscannedReconcile */,
                reconcileMissingLocalItems /* reconcileMissingLocalItems */,
                false /* skipIfAccessDenied */,
                reconciled);

            /*
             * If the workspace is marked with the SetFileTimeToCheckin flag,
             * then we should take this opportunity to ensure that the file time
             * on items without a pending edit matches the checkin date. This
             * operation is scoped to the provided GetRequest objects.
             */
            if (localWorkspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
                LocalDataAccessLayer.snapBackToCheckinDate(localWorkspace, requests);
            }
        }

        final GetOperation[][] result = super.get(
            workspaceName,
            ownerName,
            requests,
            maxResults,
            options,
            itemAttributeFilters,
            itemPropertyFilters,
            noGet);

        return result;
    }

    @Override
    public LabelResult[] labelItem(
        final String workspaceName,
        final String workspaceOwner,
        final VersionControlLabel label,
        final LabelItemSpec[] labelSpecs,
        final LabelChildOption children,
        final AtomicReference<Failure[]> failures) {
        final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();

        if (null != labelSpecs) {
            for (final LabelItemSpec labelItemSpec : labelSpecs) {
                if (null != labelItemSpec) {
                    itemSpecs.add(labelItemSpec.getItemSpec());

                    reconcileIfLocalVersionSpec(labelItemSpec.getVersion(), true);
                }
            }
        }

        if (itemSpecs.size() > 0) {
            reconcileIfAnyLocalItemSpecs(
                workspaceName,
                workspaceOwner,
                itemSpecs.toArray(new ItemSpec[itemSpecs.size()]),
                true);
        }

        return super.labelItem(workspaceName, workspaceOwner, label, labelSpecs, children, failures);
    }

    @Override
    public GetOperation[] merge(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec source,
        final ItemSpec target,
        final VersionSpec from,
        final VersionSpec to,
        final LockLevel lockLevel,
        final MergeFlags mergeFlags,
        final AtomicReference<Failure[]> failures,
        final AtomicReference<Conflict[]> conflicts,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters,
        final AtomicReference<ChangePendedFlags> changePendedFlags) {
        final Workspace w = reconcileIfLocal(workspaceName, workspaceOwner);
        reconcileIfLocalVersionSpec(from, true);
        reconcileIfLocalVersionSpec(to, true);

        final WorkspaceLock lock = lockIfLocal(w);

        try {
            final GetOperation[] toReturn = super.merge(
                workspaceName,
                workspaceOwner,
                source,
                target,
                from,
                to,
                lockLevel,
                mergeFlags,
                failures,
                conflicts,
                itemAttributeFilters,
                itemPropertyFilters,
                changePendedFlags);

            syncWorkingFoldersIfNecessary(w, changePendedFlags.get());
            syncPendingChangesIfLocal(w, toReturn, itemPropertyFilters);

            return toReturn;
        } finally {
            if (lock != null) {
                lock.close();
            }
        }
    }

    /**
     * This method supports {@link LocalDataAccessLayer} when it needs to pend
     * property changes while reconciling a local workspace. The normal
     * pendChanges() method would start another reconcile.
     */
    public GetOperation[] pendChangesInLocalWorkspace(
        final String workspaceName,
        final String ownerName,
        final ChangeRequest[] changes,
        final PendChangesOptions pendChangesOptions,
        final SupportedFeatures supportedFeatures,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters,
        final String[] itemAttributeFilters,
        final AtomicBoolean onlineOperation,
        final AtomicReference<ChangePendedFlags> changePendedFlags) {
        final _Repository4Soap_PendChangesInLocalWorkspaceResponse response;

        try {
            response = getRepository4().pendChangesInLocalWorkspace(
                workspaceName,
                ownerName,
                (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                pendChangesOptions.toIntFlags(),
                supportedFeatures.toIntFlags(),
                itemPropertyFilters,
                itemAttributeFilters);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        final GetOperation[] toReturn =
            (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getPendChangesInLocalWorkspaceResult());

        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
        changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

        onlineOperation.set(true);
        return toReturn;
    }

    @Override
    public GetOperation[] pendChanges(
        final String workspaceName,
        final String ownerName,
        final ChangeRequest[] changes,
        final PendChangesOptions pendChangesOptions,
        final SupportedFeatures supportedFeatures,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters,
        final String[] itemAttributeFilters,
        final boolean updateDisk,
        final AtomicBoolean onlineOperation,
        final AtomicReference<ChangePendedFlags> changePendedFlags) {
        onlineOperation.set(false);

        // set this to none for local workspaces, if the call reaches the server
        // the flag will get overwritten
        changePendedFlags.set(ChangePendedFlags.NONE);
        int unlockCount = 0;

        final Workspace localWorkspace = getLocalWorkspace(workspaceName, ownerName);
        if (localWorkspace != null) {
            boolean attributeChange = false;
            boolean nonExecuteSymlinkBitPropertyChange = false;

            if (null != itemAttributeFilters && itemAttributeFilters.length > 0) {
                attributeChange = true;
            }

            // If the property filters are only for the executable bit, we can
            // handle that locally, otherwise we must go to the server.
            if (null != itemPropertyFilters && itemPropertyFilters.length > 0) {
                for (final String filter : itemPropertyFilters) {
                    /*
                     * Not using wildcard matching here: just because a wildcard
                     * _does_ match the executable key _doesn't_ mean it
                     * wouldn't match others on the server. So only consider a
                     * direct match against the executable key to keep
                     * processing locally.
                     */
                    if (PropertyValue.comparePropertyNames(PropertyConstants.EXECUTABLE_KEY, filter) != 0
                        && PropertyValue.comparePropertyNames(PropertyConstants.SYMBOLIC_KEY, filter) != 0) {
                        nonExecuteSymlinkBitPropertyChange = true;
                        break;
                    }
                }
            }

            RequestType requestType = RequestType.NONE;
            boolean requestingLock = false;

            for (final ChangeRequest changeRequest : changes) {
                if (RequestType.NONE == requestType) {
                    requestType = changeRequest.getRequestType();
                } else if (requestType != changeRequest.getRequestType()) {
                    // TODO: Move string from server assembly
                    throw new VersionControlException("Not all changes had the same request type"); //$NON-NLS-1$
                }

                // If the caller is requesting a lock, then the call is a server
                // call, unless the user is performing an add and the LockLevel
                // is None.

                // Is it possible to have different locklevels on different
                // ChangeRequest objects?

                if (changeRequest.getLockLevel() != LockLevel.UNCHANGED
                    && !(changeRequest.getLockLevel() == LockLevel.NONE
                        && changeRequest.getRequestType() == RequestType.ADD)) {
                    requestingLock = true;
                }

                if (changeRequest.getLockLevel() == LockLevel.NONE
                    && changeRequest.getRequestType().equals(RequestType.LOCK)) {
                    unlockCount++;
                }
            }

            final boolean silent = pendChangesOptions.contains(PendChangesOptions.SILENT);

            if (!requestingLock && !attributeChange && !nonExecuteSymlinkBitPropertyChange) {
                if (requestType == RequestType.ADD
                    || requestType == RequestType.EDIT
                    || requestType == RequestType.DELETE
                    || requestType == RequestType.RENAME
                    || requestType == RequestType.PROPERTY) {
                    final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(localWorkspace);
                    try {
                        final AtomicReference<Failure[]> delegateFailures = new AtomicReference<Failure[]>();
                        final AtomicReference<GetOperation[]> toReturn = new AtomicReference<GetOperation[]>();

                        final RequestType transactionRequestType = requestType;

                        transaction.execute(new AllTablesTransaction() {
                            @Override
                            public void invoke(
                                final LocalWorkspaceProperties wp,
                                final WorkspaceVersionTable lv,
                                final LocalPendingChangesTable pc) {
                                if (transactionRequestType == RequestType.ADD) {
                                    toReturn.set(
                                        LocalDataAccessLayer.pendAdd(
                                            localWorkspace,
                                            wp,
                                            lv,
                                            pc,
                                            changes,
                                            silent,
                                            delegateFailures,
                                            itemPropertyFilters));
                                } else if (transactionRequestType == RequestType.EDIT) {
                                    toReturn.set(
                                        LocalDataAccessLayer.pendEdit(
                                            localWorkspace,
                                            wp,
                                            lv,
                                            pc,
                                            changes,
                                            silent,
                                            delegateFailures,
                                            itemPropertyFilters));
                                } else if (transactionRequestType == RequestType.DELETE) {
                                    toReturn.set(
                                        LocalDataAccessLayer.pendDelete(
                                            localWorkspace,
                                            wp,
                                            lv,
                                            pc,
                                            changes,
                                            silent,
                                            delegateFailures,
                                            itemPropertyFilters));
                                } else if (transactionRequestType == RequestType.RENAME) {
                                    final AtomicBoolean onlineOperationRequired = new AtomicBoolean(false);

                                    toReturn.set(
                                        LocalDataAccessLayer.pendRename(
                                            localWorkspace,
                                            wp,
                                            lv,
                                            pc,
                                            changes,
                                            silent,
                                            delegateFailures,
                                            onlineOperationRequired,
                                            itemPropertyFilters));

                                    if (onlineOperationRequired.get()) {
                                        toReturn.set(null);
                                        transaction.abort();
                                    } else if (updateDisk) {
                                        // we don't want to file a conflict
                                        // while offline, so we check up front.
                                        for (final GetOperation getOp : toReturn.get()) {
                                            if (getOp.getTargetLocalItem() != null
                                                && !LocalPath.equals(
                                                    getOp.getSourceLocalItem(),
                                                    getOp.getTargetLocalItem())
                                                && new File(getOp.getTargetLocalItem()).exists()) {
                                                throw new VersionControlException(MessageFormat.format(
                                                    //@formatter:off
                                                    Messages.getString("WebServiceLayerLocalWorkspaces.FileExistsFormat"), //$NON-NLS-1$
                                                    //@formatter:on
                                                    getOp.getTargetLocalItem()));
                                            }
                                        }
                                    }
                                }

                                if (transactionRequestType == RequestType.PROPERTY) {
                                    final AtomicBoolean onlineOperationRequired = new AtomicBoolean(false);

                                    toReturn.set(
                                        LocalDataAccessLayer.pendPropertyChange(
                                            localWorkspace,
                                            wp,
                                            lv,
                                            pc,
                                            changes,
                                            silent,
                                            delegateFailures,
                                            onlineOperationRequired,
                                            itemPropertyFilters));

                                    if (onlineOperationRequired.get()) {
                                        toReturn.set(null);
                                        transaction.abort();
                                    }
                                }
                            }
                        });

                        if (toReturn.get() != null) {
                            // Offline operation successfully completed.
                            failures.set(delegateFailures.get());
                            return toReturn.get();
                        }
                    } finally {
                        try {
                            transaction.close();
                        } catch (final IOException e) {
                            throw new VersionControlException(e);
                        }
                    }
                } else if (requestType == RequestType.BRANCH
                    || requestType == RequestType.UNDELETE
                    || requestType == RequestType.LOCK) {
                    // Forward to server
                } else {
                    // TODO: Remove this when all RequestTypes are supported
                    // here.
                    throw new VersionControlException("Not currently implemented for local workspaces"); //$NON-NLS-1$
                }
            }
        }

        if (null != localWorkspace) {
            // if we only have requests for unlocking, move on if the reconcile
            // fails this is needed for unlock other
            final Workspace w =
                reconcileIfLocal(workspaceName, ownerName, false, false, unlockCount == changes.length, null);

            // Lock the workspace which will receive the pending changes
            final WorkspaceLock lock = lockIfLocal(w);

            try {

                final GetOperation[] toReturn;
                try {
                    if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                        final _Repository5Soap_PendChangesInLocalWorkspaceResponse response =
                            getRepository5().pendChangesInLocalWorkspace(
                                workspaceName,
                                ownerName,
                                (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                                pendChangesOptions.toIntFlags(),
                                supportedFeatures.toIntFlags(),
                                itemPropertyFilters,
                                itemAttributeFilters,
                                VersionControlConstants.MAX_SERVER_PATH_SIZE);
                        toReturn = (GetOperation[]) WrapperUtils.wrap(
                            GetOperation.class,
                            response.getPendChangesInLocalWorkspaceResult());

                        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                        changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                    } else {
                        final _Repository4Soap_PendChangesInLocalWorkspaceResponse response =
                            getRepository4().pendChangesInLocalWorkspace(
                                workspaceName,
                                ownerName,
                                (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                                pendChangesOptions.toIntFlags(),
                                supportedFeatures.toIntFlags(),
                                itemPropertyFilters,
                                itemAttributeFilters);
                        toReturn = (GetOperation[]) WrapperUtils.wrap(
                            GetOperation.class,
                            response.getPendChangesInLocalWorkspaceResult());

                        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                        changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                    }
                } catch (final ProxyException e) {
                    throw VersionControlExceptionMapper.map(e);
                }

                syncWorkingFoldersIfNecessary(w, changePendedFlags.get());
                syncPendingChangesIfLocal(w, toReturn, itemPropertyFilters);

                if (RequestType.ADD == changes[0].getRequestType()) {
                    // The client does not process the getops returned from a
                    // PendAdd call. Because the server has created local
                    // version rows for us, we need to update the local version
                    // table to contain these rows too.
                    LocalDataAccessLayer.afterAdd(localWorkspace, toReturn);

                    // When a pending add is created, the item on disk is not
                    // touched; so we need to inform the scanner that the item
                    // is invalidated so it is re-scanned. Rather than go
                    // through the local paths on which adds were pended, we'll
                    // invalidate the workspace. This is not a common code path.
                    localWorkspace.getWorkspaceWatcher().markPathChanged(""); //$NON-NLS-1$
                }

                onlineOperation.set(true);
                return toReturn;
            } finally {
                if (lock != null) {
                    lock.close();
                }
            }
        } else {
            return super.pendChanges(
                workspaceName,
                ownerName,
                changes,
                pendChangesOptions,
                supportedFeatures,
                failures,
                itemPropertyFilters,
                itemAttributeFilters,
                updateDisk,
                onlineOperation,
                changePendedFlags);
        }
    }

    /*
     * See super.queryBranches for why we return web service object and not a
     * wrapped type.
     */
    @Override
    public _BranchRelative[][] queryBranches(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final VersionSpec version) {
        reconcileIfAnyLocalItemSpecs(workspaceName, workspaceOwner, items, true);
        reconcileIfLocalVersionSpec(version, true);

        return super.queryBranches(workspaceName, workspaceOwner, items, version);
    }

    @Override
    public Conflict[] queryConflicts(final String workspaceName, final String ownerName, final ItemSpec[] items) {
        reconcileIfLocal(workspaceName, ownerName);

        return super.queryConflicts(workspaceName, ownerName, items);
    }

    @Override
    public String[] queryEffectiveItemPermissions(
        final String workspaceName,
        final String workspaceOwner,
        final String item,
        final String identityName) {
        reconcileIfLocalItem(workspaceName, workspaceOwner, item, true);

        return super.queryEffectiveItemPermissions(workspaceName, workspaceOwner, item, identityName);
    }

    @Override
    public Changeset[] queryHistory(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec itemSpec,
        final VersionSpec versionItem,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean includeFiles,
        final boolean generateDownloadUrls,
        final boolean slotMode,
        final boolean sortAscending) {
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, itemSpec, true);
        reconcileIfLocalVersionSpec(versionItem, true);
        reconcileIfLocalVersionSpec(versionFrom, true);
        reconcileIfLocalVersionSpec(versionTo, true);

        return super.queryHistory(
            workspaceName,
            workspaceOwner,
            itemSpec,
            versionItem,
            user,
            versionFrom,
            versionTo,
            maxCount,
            includeFiles,
            generateDownloadUrls,
            slotMode,
            sortAscending);
    }

    @Override
    public ItemSet[] queryItems(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean generateDownloadUrls,
        final GetItemsOptions options,
        final String[] itemPropertyFilters,
        final String[] itemAttributeFilters) {
        reconcileIfAnyLocalItemSpecs(workspaceName, workspaceOwner, items, true);
        reconcileIfLocalVersionSpec(version, true);

        return super.queryItems(
            workspaceName,
            workspaceOwner,
            items,
            version,
            deletedState,
            itemType,
            generateDownloadUrls,
            options,
            itemPropertyFilters,
            itemAttributeFilters);
    }

    @Override
    public ExtendedItem[][] queryItemsExtended(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options,
        final String[] itemPropertyFilters) {
        final boolean includeLocalOnly = options.contains(GetItemsOptions.LOCAL_ONLY);

        Workspace localWorkspace;
        if (includeLocalOnly
            && workspaceName != null
            && workspaceName.length() > 0
            && workspaceOwner != null
            && workspaceOwner.length() > 0
            && (localWorkspace = getLocalWorkspace(workspaceName, workspaceOwner)) != null) {
            return LocalDataAccessLayer.queryItemsExtended(localWorkspace, items, deletedState, itemType, options);
        } else {
            reconcileIfLocal(workspaceName, workspaceOwner);

            ExtendedItem[][] result = super.queryItemsExtended(
                workspaceName,
                workspaceOwner,
                items,
                deletedState,
                itemType,
                options,
                itemPropertyFilters);

            if (includeLocalOnly) {
                result = removeNonLocalExtendedItems(result);
            }

            return result;
        }
    }

    /**
     * Returnes ExtendedItem object which have local version or have pending
     * deletes.
     */
    private ExtendedItem[][] removeNonLocalExtendedItems(final ExtendedItem[][] extendedItems) {
        final ExtendedItem[][] result = new ExtendedItem[extendedItems.length][];
        for (int i = 0; i < extendedItems.length; i++) {
            // First count number of items we need to filter out.
            // GuiCache which is the biggest consumer of this, requests for
            // non-deleted items
            // so in the most common case we don't need to filter out anything.
            final ExtendedItem[] items = extendedItems[i];
            int nonLocalNumber = 0;
            for (int j = 0; j < items.length; j++) {
                if (isNonLocal(items[j])) {
                    nonLocalNumber++;
                }
            }
            if (nonLocalNumber == 0) {
                result[i] = items;
            } else {
                // Filter out non-local items
                result[i] = new ExtendedItem[items.length - nonLocalNumber];
                for (int originalIndex = 0, newIndex = 0; originalIndex < items.length
                    && newIndex < result[i].length; originalIndex++) {
                    if (!isNonLocal(items[originalIndex])) {
                        result[i][newIndex] = items[originalIndex];
                        newIndex++;
                    }
                }
                Check.isTrue(
                    result[i].length == 0 || result[i][result[i].length - 1] != null,
                    "We didn't fill full result array with extendedItems - caller can fail with Nullrefs"); //$NON-NLS-1$
            }
        }
        return result;
    }

    /**
     * Returns true if item should be ignored when LocalOnly flag is specified -
     * item does not have pending change and does not reside in the workspace.
     */
    private boolean isNonLocal(final ExtendedItem item) {
        return item.getLocalItem() == null && item.getPendingChange().contains(ChangeType.DELETE) == false;
    }

    @Override
    public VersionControlLabel[] queryLabels(
        final String workspaceName,
        final String workspaceOwner,
        final String labelName,
        final String labelScope,
        final String owner,
        final String filterItem,
        final VersionSpec versionFilterItem,
        final boolean includeItems,
        final boolean generateDownloadUrls) {
        reconcileIfLocalItem(workspaceName, workspaceOwner, filterItem, true);
        reconcileIfLocalVersionSpec(versionFilterItem, true);

        return super.queryLabels(
            workspaceName,
            workspaceOwner,
            labelName,
            labelScope,
            owner,
            filterItem,
            versionFilterItem,
            includeItems,
            generateDownloadUrls);
    }

    @Override
    public LocalVersion[][] queryLocalVersions(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs) {
        Workspace localWorkspace;

        if ((localWorkspace = getLocalWorkspace(workspaceName, workspaceOwner)) != null) {
            return LocalDataAccessLayer.queryLocalVersions(localWorkspace, itemSpecs);
        } else {
            return super.queryLocalVersions(workspaceName, workspaceOwner, itemSpecs);
        }
    }

    @Override
    public MergeCandidate[] queryMergeCandidates(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec source,
        final ItemSpec target,
        final MergeFlags mergeFlags) {
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, source, true);
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, target, true);

        return super.queryMergeCandidates(workspaceName, workspaceOwner, source, target, mergeFlags);
    }

    @Override
    public ChangesetMerge[] queryMerges(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec source,
        final VersionSpec versionSource,
        final ItemSpec target,
        final VersionSpec versionTarget,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxChangesets,
        final boolean showAll,
        final AtomicReference<Changeset[]> changesets) {
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, source, true);
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, target, true);
        reconcileIfLocalVersionSpec(versionSource, true);
        reconcileIfLocalVersionSpec(versionTarget, true);
        reconcileIfLocalVersionSpec(versionFrom, true);
        reconcileIfLocalVersionSpec(versionTo, true);

        return super.queryMerges(
            workspaceName,
            workspaceOwner,
            source,
            versionSource,
            target,
            versionTarget,
            versionFrom,
            versionTo,
            maxChangesets,
            showAll,
            changesets);
    }

    @Override
    public ExtendedMerge[] queryMergesExtended(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec target,
        final VersionSpec versionTarget,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final QueryMergesExtendedOptions options) {
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, target, true);
        reconcileIfLocalVersionSpec(versionTarget, true);
        reconcileIfLocalVersionSpec(versionFrom, true);
        reconcileIfLocalVersionSpec(versionTo, true);

        return super.queryMergesExtended(
            workspaceName,
            workspaceOwner,
            target,
            versionTarget,
            versionFrom,
            versionTo,
            options);
    }

    @Override
    public ChangesetMergeDetails queryMergesWithDetails(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec source,
        final VersionSpec versionSource,
        final ItemSpec target,
        final VersionSpec versionTarget,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxChangesets,
        final boolean showAll) {
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, source, true);
        reconcileIfLocalItemSpec(workspaceName, workspaceOwner, target, true);

        reconcileIfLocalVersionSpec(versionSource, true);
        reconcileIfLocalVersionSpec(versionTarget, true);
        reconcileIfLocalVersionSpec(versionFrom, true);
        reconcileIfLocalVersionSpec(versionTo, true);

        return super.queryMergesWithDetails(
            workspaceName,
            workspaceOwner,
            source,
            versionSource,
            target,
            versionTarget,
            versionFrom,
            versionTo,
            maxChangesets,
            showAll);
    }

    @Override
    public PendingChange[] queryPendingChangesForWorkspace(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final int pageSize,
        final String lastChange,
        final boolean includeMergeInfo,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters) {
        Workspace localWorkspace;

        if ((itemPropertyFilters == null || itemPropertyFilters.length == 0)
            && (localWorkspace = getLocalWorkspace(workspaceName, workspaceOwner)) != null) {
            boolean okayToPerformLocalQuery = true;

            if (itemSpecs != null && itemSpecs.length > 1) {
                // TODO: We need to support doing this locally Caller is
                // requesting the distinct pending changes that match the
                // itemspecs provided -- not one set of pending changes for each
                // itemspec.
                okayToPerformLocalQuery = false;
            }

            if (includeMergeInfo) {
                // Can't get pending merge information from the client.
                okayToPerformLocalQuery = false;
            }

            if (generateDownloadUrls) {
                // Can't generate download URLs from the client.
                okayToPerformLocalQuery = false;
            }

            if (okayToPerformLocalQuery) {
                final PendingSet[] pendingSets = LocalDataAccessLayer.queryPendingChanges(
                    localWorkspace,
                    itemSpecs != null ? itemSpecs : new ItemSpec[] {
                        new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
                }, failures, false, lastChange, pageSize, itemPropertyFilters);

                // QueryPendingChanges does not return empty PendingSet objects.
                if (0 == pendingSets.length) {
                    return new PendingChange[0];
                } else {
                    // Perf: Avoid the clone from the PendingChanges property
                    return pendingSets[0].getPendingChanges();
                }
            } else {
                // Reconcile
            }
        }

        return super.queryPendingChangesForWorkspace(
            workspaceName,
            workspaceOwner,
            itemSpecs,
            generateDownloadUrls,
            pageSize,
            lastChange,
            includeMergeInfo,
            failures,
            itemPropertyFilters);
    }

    @Override
    public PendingSet[] queryPendingSets(
        final String localWorkspaceName,
        final String localWorkspaceOwner,
        final String queryWorkspaceName,
        final String ownerName,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final AtomicReference<Failure[]> failures,
        final boolean includeCandidates,
        final String[] itemPropertyFilters) {
        Workspace localWorkspace;

        if ((localWorkspace = getLocalWorkspace(queryWorkspaceName, ownerName)) != null) {
            boolean nonExecuteSymlinkBitPropertyChange = false;
            if (itemPropertyFilters != null) {
                for (final String filter : itemPropertyFilters) {
                    /*
                     * Not using wildcard matching here: just because a wildcard
                     * _does_ match the executable key _doesn't_ mean it
                     * wouldn't match others on the server. So only consider a
                     * direct match against the executable key to keep
                     * processing locally.
                     */
                    if (PropertyValue.comparePropertyNames(PropertyConstants.EXECUTABLE_KEY, filter) != 0
                        && PropertyValue.comparePropertyNames(PropertyConstants.SYMBOLIC_KEY, filter) != 0) {
                        nonExecuteSymlinkBitPropertyChange = true;
                        break;
                    }
                }
            }

            boolean okayToPerformLocalQuery = true;
            if ((null != localWorkspaceName
                && null != localWorkspaceOwner
                && (Workspace.matchName(localWorkspaceName, queryWorkspaceName) == false
                    || localWorkspace.ownerNameMatches(localWorkspaceOwner) == false))
                || nonExecuteSymlinkBitPropertyChange) {
                // Caller claims he is sending local paths in the itemspecs, and
                // that the local paths are from a different workspace or that
                // he is requesting properties that must go to the server
                okayToPerformLocalQuery = false;
            }

            PendingSet[] pendingSets;
            if (okayToPerformLocalQuery) {
                pendingSets = LocalDataAccessLayer.queryPendingChanges(
                    localWorkspace,
                    itemSpecs,
                    failures,
                    includeCandidates,
                    null,
                    0,
                    itemPropertyFilters);
            } else {
                reconcileIfLocal(queryWorkspaceName, ownerName);
                reconcileIfAnyLocalItemSpecs(localWorkspaceName, localWorkspaceOwner, itemSpecs);

                // Through this codepath, local workspace pending changes are
                // returned verbatim. So we must reconcile before calling

                try {
                    if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                        final _Repository5Soap_QueryPendingSetsWithLocalWorkspacesResponse response =
                            getRepository5().queryPendingSetsWithLocalWorkspaces(
                                localWorkspaceName,
                                localWorkspaceOwner,
                                queryWorkspaceName,
                                ownerName,
                                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                                generateDownloadUrls,
                                itemPropertyFilters,
                                VersionControlConstants.MAX_SERVER_PATH_SIZE);
                        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                        pendingSets = (PendingSet[]) WrapperUtils.wrap(
                            PendingSet.class,
                            response.getQueryPendingSetsWithLocalWorkspacesResult());
                    } else {
                        final _Repository4Soap_QueryPendingSetsWithLocalWorkspacesResponse response =
                            getRepository4().queryPendingSetsWithLocalWorkspaces(
                                localWorkspaceName,
                                localWorkspaceOwner,
                                queryWorkspaceName,
                                ownerName,
                                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                                generateDownloadUrls,
                                itemPropertyFilters);
                        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                        pendingSets = (PendingSet[]) WrapperUtils.wrap(
                            PendingSet.class,
                            response.getQueryPendingSetsWithLocalWorkspacesResult());
                    }
                } catch (final ProxyException e) {
                    throw VersionControlExceptionMapper.map(e);
                }
            }

            for (final PendingSet ps : pendingSets) {
                ps.setPendingSetDetails();
            }
            return pendingSets;
        }

        // Through this codepath, local workspace pending changes are masked to
        // show only the "lock" bit
        return super.queryPendingSets(
            localWorkspaceName,
            localWorkspaceOwner,
            queryWorkspaceName,
            ownerName,
            itemSpecs,
            generateDownloadUrls,
            failures,
            includeCandidates,
            itemPropertyFilters);
    }

    @Override
    public PendingSet[] queryShelvedChanges(
        final String localWorkspaceName,
        final String localWorkspaceOwner,
        final String shelvesetName,
        final String ownerName,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters) {
        reconcileIfAnyLocalItemSpecs(localWorkspaceName, localWorkspaceOwner, itemSpecs, true);

        return super.queryShelvedChanges(
            localWorkspaceName,
            localWorkspaceOwner,
            shelvesetName,
            ownerName,
            itemSpecs,
            generateDownloadUrls,
            failures,
            itemPropertyFilters);
    }

    @Override
    public Workspace queryServerWorkspace(final String workspaceName, final String ownerName) {
        return super.queryWorkspace(workspaceName, ownerName);
    }

    @Override
    public Workspace queryWorkspace(final String workspaceName, final String ownerName) {
        final Workspace localWorkspace = getLocalWorkspace(workspaceName, ownerName);

        if (localWorkspace != null) {
            // If the mappings returned from the server don't match our
            // authoritative copy, then update the server's copy to match ours.
            final WorkingFolder[] authoritativeMappings = LocalDataAccessLayer.queryWorkingFolders(localWorkspace);
            Workspace toReturn = super.queryWorkspace(workspaceName, ownerName);

            if (!WorkingFolder.areSetsEqual(toReturn.getFolders(), authoritativeMappings)) {
                toReturn.setFolders(authoritativeMappings);
                toReturn = super.updateWorkspace(workspaceName, ownerName, toReturn, SupportedFeatures.ALL);
            }

            return toReturn;
        } else {
            return super.queryWorkspace(workspaceName, ownerName);
        }
    }

    @Override
    public WorkspaceItemSet[] queryWorkspaceItems(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean generateDownloadUrls,
        final int options) {
        reconcileIfLocal(workspaceName, workspaceOwner);

        return super.queryWorkspaceItems(
            workspaceName,
            workspaceOwner,
            items,
            deletedState,
            itemType,
            generateDownloadUrls,
            options);
    }

    @Override
    public void removeLocalConflicts(
        final String workspaceName,
        final String ownerName,
        final List<Conflict> conflicts,
        final ResolveErrorOptions errorOptions,
        final ConflictResolvedHandler onResolvedConflict,
        final ConflictResolveErrorHandler onResolveError) throws CoreCancelException {
        reconcileIfLocal(workspaceName, ownerName);

        super.removeLocalConflicts(
            workspaceName,
            ownerName,
            conflicts,
            errorOptions,
            onResolvedConflict,
            onResolveError);
    }

    @Override
    public void resolve(
        final String workspaceName,
        final String ownerName,
        final Conflict[] conflicts,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters,
        final ResolveErrorOptions errorOptions,
        final ConflictResolvedHandler onResolvedConflict,
        final ConflictResolveErrorHandler onResolveError) throws CoreCancelException {
        final Workspace w = reconcileIfLocal(workspaceName, ownerName);

        final LocalWorkspaceResolvedConflictHandler localHandler =
            new LocalWorkspaceResolvedConflictHandler(onResolvedConflict);

        /*
         * If this is a local workspace, then it's expected that the caller will
         * have locked the workspace, since for Resolve and Undo it's necessary
         * to lock all the way until the completion of ProcessGetOperations.
         */
        super.resolve(
            workspaceName,
            ownerName,
            conflicts,
            itemAttributeFilters,
            itemPropertyFilters,
            errorOptions,
            localHandler,
            onResolveError);

        syncWorkingFoldersIfNecessary(w, localHandler.getFlags());
        syncPendingChangesIfLocal(w, localHandler.getAllOperations(), itemPropertyFilters);
    }

    @Override
    public GetOperation[] rollback(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final VersionSpec itemVersion,
        final VersionSpec from,
        final VersionSpec to,
        final RollbackOptions rollbackOptions,
        final LockLevel lockLevel,
        final AtomicReference<Conflict[]> conflicts,
        final AtomicReference<Failure[]> failures,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters,
        final AtomicReference<ChangePendedFlags> changePendedFlags) {
        final Workspace w = reconcileIfLocal(workspaceName, workspaceOwner);
        reconcileIfLocalVersionSpec(itemVersion, true);
        reconcileIfLocalVersionSpec(from, true);
        reconcileIfLocalVersionSpec(to, true);

        final WorkspaceLock lock = lockIfLocal(w);

        try {
            final GetOperation[] toReturn = super.rollback(
                workspaceName,
                workspaceOwner,
                items,
                itemVersion,
                from,
                to,
                rollbackOptions,
                lockLevel,
                conflicts,
                failures,
                itemAttributeFilters,
                itemPropertyFilters,
                changePendedFlags);

            syncWorkingFoldersIfNecessary(w, changePendedFlags.get());
            syncPendingChangesIfLocal(w, toReturn, itemPropertyFilters);

            return toReturn;
        } finally {
            if (lock != null) {
                lock.close();
            }
        }
    }

    @Override
    public Failure[] setPendingChangeProperty(
        final String workspaceName,
        final String workspaceOwner,
        final ArtifactPropertyValue[] pendingChangePropertyValues) {
        reconcileIfLocal(workspaceName, workspaceOwner);

        return super.setPendingChangeProperty(workspaceName, workspaceOwner, pendingChangePropertyValues);
    }

    @Override
    public void setVersionedItemProperty(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final VersionSpec versionSpec,
        final DeletedState deletedState,
        final ItemType itemType,
        final PropertyValue[] propertyValues) {
        reconcileIfAnyLocalItemSpecs(workspaceName, workspaceOwner, itemSpecs, true);
        reconcileIfLocalVersionSpec(versionSpec, true);

        super.setVersionedItemProperty(
            workspaceName,
            workspaceOwner,
            itemSpecs,
            versionSpec,
            deletedState,
            itemType,
            propertyValues);
    }

    @Override
    public Failure[] shelve(
        final String workspaceName,
        final String workspaceOwner,
        final String[] serverItems,
        final Shelveset shelveset,
        final boolean replace) {
        reconcileIfLocal(workspaceName, workspaceOwner);

        return super.shelve(workspaceName, workspaceOwner, serverItems, shelveset, replace);
    }

    @Override
    public GetOperation[] undoPendingChanges(
        final String workspaceName,
        final String ownerName,
        final ItemSpec[] items,
        final AtomicReference<Failure[]> failures,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters,
        final AtomicBoolean onlineOperation,
        final boolean deleteAdds,
        final AtomicReference<ChangePendedFlags> changePendedFlags) {
        onlineOperation.set(true);

        // set this to none for local workspaces, if the call reaches the server
        // the flag will get overwritten
        changePendedFlags.set(ChangePendedFlags.NONE);

        final Workspace localWorkspace = getLocalWorkspace(workspaceName, ownerName);

        if (localWorkspace != null) {
            final AtomicReference<GetOperation[]> toReturn = new AtomicReference<GetOperation[]>();

            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(localWorkspace);
            try {
                final AtomicReference<Failure[]> delegateFailures = new AtomicReference<Failure[]>(new Failure[0]);
                final AtomicBoolean onlineOperationRequired = new AtomicBoolean(false);

                transaction.execute(new AllTablesTransaction() {
                    @Override
                    public void invoke(
                        final LocalWorkspaceProperties wp,
                        final WorkspaceVersionTable lv,
                        final LocalPendingChangesTable pc) {
                        toReturn.set(
                            LocalDataAccessLayer.undoPendingChanges(
                                localWorkspace,
                                wp,
                                lv,
                                pc,
                                items,
                                delegateFailures,
                                onlineOperationRequired,
                                itemPropertyFilters));

                        if (onlineOperationRequired.get()) {
                            transaction.abort();
                            toReturn.set(null);
                        }

                        /*
                         * we should check to see if we are going to cause an
                         * existing file conflict if we are abort the
                         * transaction - since we don't want to try and contact
                         * the server in an offline undo.
                         */
                        if (toReturn.get() != null) {
                            Map<String, GetOperation> localItemDictionary = null;

                            for (final GetOperation op : toReturn.get()) {
                                if (op.getItemType() == ItemType.FILE
                                    && op.getTargetLocalItem() != null
                                    && op.getTargetLocalItem().length() > 0
                                    && LocalPath.equals(op.getTargetLocalItem(), op.getCurrentLocalItem()) == false) {
                                    final WorkspaceLocalItem item = lv.getByLocalItem(op.getTargetLocalItem());

                                    if ((item == null || item.isDeleted())
                                        && new File(op.getTargetLocalItem()).exists()) {
                                        if (localItemDictionary == null) {
                                            localItemDictionary = new HashMap<String, GetOperation>();
                                            /*
                                             * we go through our list and keep
                                             * track of adds we are removing
                                             * this is for the shelve /move
                                             * case.
                                             */
                                            for (final GetOperation getOp : toReturn.get()) {
                                                if (getOp.getTargetLocalItem() != null
                                                    && getOp.getTargetLocalItem().length() > 0
                                                    && getOp.getItemType() == ItemType.FILE) {
                                                    final GetOperation currentValue =
                                                        localItemDictionary.get(getOp.getTargetLocalItem());
                                                    if (currentValue != null) {
                                                        // don't overwrite an
                                                        // add
                                                        if (currentValue.getChangeType().contains(ChangeType.ADD)) {
                                                            localItemDictionary.put(getOp.getTargetLocalItem(), getOp);
                                                        }
                                                    } else {
                                                        localItemDictionary.put(getOp.getTargetLocalItem(), getOp);
                                                    }
                                                }
                                            }
                                        }

                                        final GetOperation existingItem =
                                            localItemDictionary.get(op.getTargetLocalItem());
                                        if (existingItem != null
                                            && existingItem.getChangeType().contains(ChangeType.ADD)) {
                                            /*
                                             * if we are going to be removing
                                             * this anyway don't worry
                                             */
                                            if (deleteAdds) {
                                                continue;
                                            }
                                        }

                                        if (existingItem == null
                                            || !tryMoveAddLocation(existingItem, localItemDictionary)) {
                                            throw new VersionControlException(MessageFormat.format(
                                                //@formatter:off
                                                Messages.getString("WebServiceLayerLocalWorkspaces.UndoItemExistsLocallyFormat"), //$NON-NLS-1$
                                                //@formatter:on
                                                (op.getCurrentLocalItem() != null
                                                    && op.getCurrentLocalItem().length() > 0) ? op.getCurrentLocalItem()
                                                        : op.getTargetLocalItem(),
                                                op.getTargetLocalItem()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                });

                if (null != toReturn.get()) {
                    onlineOperation.set(false);
                    failures.set(delegateFailures.get());
                    return toReturn.get();
                }
            } finally {
                try {
                    transaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }

            final Workspace w = reconcileIfLocal(workspaceName, ownerName);

            // Lock the workspace which will receive the pending changes
            final WorkspaceLock lock = lockIfLocal(w);

            try {
                try {
                    if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                        final _Repository5Soap_UndoPendingChangesInLocalWorkspaceResponse response =
                            getRepository5().undoPendingChangesInLocalWorkspace(
                                workspaceName,
                                ownerName,
                                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                                itemPropertyFilters,
                                itemAttributeFilters,
                                VersionControlConstants.MAX_SERVER_PATH_SIZE);

                        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                        changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                        toReturn.set(
                            (GetOperation[]) WrapperUtils.wrap(
                                GetOperation.class,
                                response.getUndoPendingChangesInLocalWorkspaceResult()));
                    } else {
                        final _Repository4Soap_UndoPendingChangesInLocalWorkspaceResponse response =
                            getRepository4().undoPendingChangesInLocalWorkspace(
                                workspaceName,
                                ownerName,
                                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                                itemPropertyFilters,
                                itemAttributeFilters);

                        failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                        changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                        toReturn.set(
                            (GetOperation[]) WrapperUtils.wrap(
                                GetOperation.class,
                                response.getUndoPendingChangesInLocalWorkspaceResult()));
                    }
                } catch (final ProxyException e) {
                    VersionControlExceptionMapper.map(e);
                }

                syncWorkingFoldersIfNecessary(w, changePendedFlags.get());
                syncPendingChangesIfLocal(w, toReturn.get(), itemPropertyFilters);

                // When a pending add is undone, the item on disk is not
                // touched; so we need to inform the scanner that the item is
                // invalidated so it is re-scanned. We'll invalidate the scanner
                // if we detect that we went to the server to undo a pending
                // add.
                if (null != toReturn.get()) {
                    for (final GetOperation op : toReturn.get()) {
                        if (op.getChangeType().contains(ChangeType.ADD)) {
                            localWorkspace.getWorkspaceWatcher().markPathChanged(""); //$NON-NLS-1$
                            break;
                        }
                    }
                }

                return toReturn.get();
            } finally {
                if (lock != null) {
                    lock.close();
                }
            }
        } else {
            return super.undoPendingChanges(
                workspaceName,
                ownerName,
                items,
                failures,
                itemAttributeFilters,
                itemPropertyFilters,
                onlineOperation,
                deleteAdds,
                changePendedFlags);
        }
    }

    private boolean tryMoveAddLocation(
        final GetOperation conflictingAdd,
        final Map<String, GetOperation> localItemDictionary) {
        // if it's not an add, we do not mess with it.
        if (conflictingAdd.getChangeType().contains(ChangeType.ADD) == false) {
            return false;
        }

        int iteration = 0;
        String extension = ".add"; //$NON-NLS-1$

        do {
            final String newLocalItem = conflictingAdd.getTargetLocalItem() + extension;

            if (!localItemDictionary.containsKey(newLocalItem) && new File(newLocalItem).exists() == false) {
                conflictingAdd.setTargetLocalItem(newLocalItem);
                return true;
            }

            iteration++;
            extension = ".add" + iteration; //$NON-NLS-1$
        } while (iteration <= 10);

        return false;
    }

    @Override
    public LabelResult[] unlabelItem(
        final String workspaceName,
        final String workspaceOwner,
        final String labelName,
        final String labelScope,
        final ItemSpec[] items,
        final VersionSpec version,
        final AtomicReference<Failure[]> failures) {
        reconcileIfAnyLocalItemSpecs(workspaceName, workspaceOwner, items, true);
        reconcileIfLocalVersionSpec(version, true);

        return super.unlabelItem(workspaceName, workspaceOwner, labelName, labelScope, items, version, failures);
    }

    @Override
    public Shelveset unshelve(
        final String shelvesetName,
        final String shelvesetOwner,
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters,
        final String[] shelvesetPropertyFilters,
        final boolean merge,
        final AtomicReference<Failure[]> failures,
        final AtomicReference<GetOperation[]> getOperations,
        final AtomicReference<Conflict[]> conflicts,
        final AtomicReference<ChangePendedFlags> changePendedFlags) {
        final Workspace w = reconcileIfLocal(workspaceName, workspaceOwner);

        // Lock the workspace which will receive the pending changes
        final WorkspaceLock lock = lockIfLocal(w);

        try {
            final Shelveset toReturn = super.unshelve(
                shelvesetName,
                shelvesetOwner,
                workspaceName,
                workspaceOwner,
                items,
                itemAttributeFilters,
                itemPropertyFilters,
                shelvesetPropertyFilters,
                merge,
                failures,
                getOperations,
                conflicts,
                changePendedFlags);

            syncWorkingFoldersIfNecessary(w, changePendedFlags.get());
            syncPendingChangesIfLocal(w, getOperations.get(), itemPropertyFilters);

            return toReturn;
        } finally {
            if (lock != null) {
                lock.close();
            }
        }
    }

    @Override
    public Workspace updateWorkspace(
        final String oldWorkspaceName,
        final String ownerName,
        final Workspace newWorkspace,
        final SupportedFeatures supportedFeatures) {
        // We need to reconcile because there are rules around changing owner
        // when the workspace has pending changes, and because there are rules
        // around changing workspace mappings which have pending renames
        // above/below them
        final Workspace localWorkspace = reconcileIfLocal(oldWorkspaceName, ownerName);

        final WorkspaceLock lock = lockIfLocal(localWorkspace);

        try {
            final Workspace returnedWorkspace =
                super.updateWorkspace(oldWorkspaceName, ownerName, newWorkspace, supportedFeatures);

            // In the block of code below, it's important to remember that the
            // workspace object on the server is now referred to by
            // newWorkspace.Name;newWorkspace.OwnerName. But for our purposes
            // below, the name and owner have not changed yet.
            if (null != localWorkspace) {
                // Commit the new working folders to the localWorkspace object.
                LocalDataAccessLayer.setWorkingFolders(localWorkspace, returnedWorkspace.getFolders());
            }

            return returnedWorkspace;
        } finally {
            if (lock != null) {
                lock.close();
            }
        }
    }

    @Override
    public Workspace updateWorkspaceNoReconcile(
        final String oldWorkspaceName,
        final String ownerName,
        final Workspace newWorkspace,
        final SupportedFeatures supportedFeatures) {
        return super.updateWorkspace(oldWorkspaceName, ownerName, newWorkspace, supportedFeatures);
    }
}
