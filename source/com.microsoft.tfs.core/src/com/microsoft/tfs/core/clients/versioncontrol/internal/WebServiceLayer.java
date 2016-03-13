// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.framework.ServerDataProvider;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.clients.versioncontrol.DestroyFlags;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.ILocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
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
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissions;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.FeatureNotSupportedException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolveErrorHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolvedHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.QueuedEditsTable;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Annotation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ArtifactPropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObjectOwnership;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMergeDetails;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.FileType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelChildOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LabelResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalVersion;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Mapping;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ReconcileResult;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RepositoryProperties;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ServerSettings;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.VersionControlLabel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkspaceItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.CheckinNotificationInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.LocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.ServerItemLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.RuntimeWorkspaceCache;
import com.microsoft.tfs.core.config.persistence.PersistenceStoreProvider;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.exceptions.mappers.VersionControlExceptionMapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

import ms.tfs.versioncontrol.clientservices._03._ArtifactPropertyValue;
import ms.tfs.versioncontrol.clientservices._03._BranchRelative;
import ms.tfs.versioncontrol.clientservices._03._ChangeRequest;
import ms.tfs.versioncontrol.clientservices._03._CheckinNoteFieldDefinition;
import ms.tfs.versioncontrol.clientservices._03._CheckinResult;
import ms.tfs.versioncontrol.clientservices._03._ExtendedItem;
import ms.tfs.versioncontrol.clientservices._03._FileType;
import ms.tfs.versioncontrol.clientservices._03._GetOperation;
import ms.tfs.versioncontrol.clientservices._03._GetRequest;
import ms.tfs.versioncontrol.clientservices._03._ItemIdentifier;
import ms.tfs.versioncontrol.clientservices._03._ItemSpec;
import ms.tfs.versioncontrol.clientservices._03._LabelItemSpec;
import ms.tfs.versioncontrol.clientservices._03._LatestVersionSpec;
import ms.tfs.versioncontrol.clientservices._03._LocalPendingChange;
import ms.tfs.versioncontrol.clientservices._03._LocalVersion;
import ms.tfs.versioncontrol.clientservices._03._LocalVersionUpdate;
import ms.tfs.versioncontrol.clientservices._03._LockLevel;
import ms.tfs.versioncontrol.clientservices._03._Mapping;
import ms.tfs.versioncontrol.clientservices._03._PendingState;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_CheckInResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_CheckInShelvesetResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_CreateBranchResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_MergeResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_PendChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_QueryPendingChangesForWorkspaceResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_QueryPendingSetsResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_QueryShelvedChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_ResolveResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_RollbackResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_UndoPendingChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository4Soap_UnshelveResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_CheckInResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_CheckInShelvesetResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_CreateBranchResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_DestroyResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_LabelItemResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_MergeResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_PendChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_QueryPendingSetsResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_ResolveResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_RollbackResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_UndoPendingChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_UnlabelItemResponse;
import ms.tfs.versioncontrol.clientservices._03._Repository5Soap_UnshelveResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_CheckInResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_CheckInShelvesetResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_MergeResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_PendChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_QueryPendingChangesForWorkspaceResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_ResolveResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_RollbackResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_TrackMergesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_UndoPendingChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositoryExtensionsSoap_UnshelveResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_CheckInResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_DestroyResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_LabelItemResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_MergeResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_PendChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_QueryMergesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_QueryPendingSetsResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_QueryShelvedChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_ResolveResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_UndoPendingChangesResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_UnlabelItemResponse;
import ms.tfs.versioncontrol.clientservices._03._RepositorySoap_UnshelveResponse;
import ms.tfs.versioncontrol.clientservices._03._ServerItemLocalVersionUpdate;
import ms.tfs.versioncontrol.clientservices._03._Shelveset;
import ms.tfs.versioncontrol.clientservices._03._Workspace;
import ms.tfs.versioncontrol.clientservices._03._WorkspaceItemSet;

/**
 * The web service layer in the client object model presents the full suite of
 * webmethods available from all the version control web services. When a
 * webmethod is available from more than one web service (i.e. checkIn) then the
 * *most recent* prototype should be presented here.
 * <p>
 * Implement your compatibility logic in this class. If the caller passes an
 * argument which is not supported by the version of the server they are talking
 * to, throw a FeatureNotSupportedException. An example would be passing
 * property filters to a 2005 or 2008 server.
 * <p>
 * In deciding which web service to call for an overloaded webmethod, follow the
 * compatibility rules that are in place for that webmethod. In most cases the
 * client is obligated to call the most recent version of the webmethod that is
 * available, or functionality may be lost.
 * <p>
 * Webmethods which take a workspace name and owner must be framed by a
 * reconcile when using a local workspace. A small set of webmethods are
 * hijacked entirely and sent to the local workspaces layer for processing there
 * without any network communication.
 * <p>
 * Never call a web service proxy directly. Always route your call through this
 * class.
 *
 * @threadsafety thread-compatible
 */
public abstract class WebServiceLayer {
    private final VersionControlClient client;
    protected final RuntimeWorkspaceCache runtimeWorkspaceCache;

    // Capabilities
    private final WebServiceLevel serviceLevel;
    private SupportedFeatures supportedFeatures;

    // Thread local
    private final static ThreadLocal<Boolean> playingBackQueuedEdits = new ThreadLocal<Boolean>();

    // Proxies
    private final _RepositorySoap repository;
    private final _RepositoryExtensionsSoap repositoryExtensions;
    private final _Repository4Soap repository4;
    private final _Repository5Soap repository5;

    /**
     * Constructs a {@link WebServiceLayer}.
     *
     * @param client
     *        the {@link VersionControlClient} to use (must not be
     *        <code>null</code>)
     * @param repository
     *        the pre-TFS 2010 web service proxy (must not be <code>null</code>)
     * @param repositoryExtensions
     *        the TFS 2010 web service proxy (may be <code>null</code>)
     * @param repository4
     *        the TFS 2012 web service proxy (may be <code>null</code>)
     */
    public WebServiceLayer(
        final VersionControlClient client,
        final _RepositorySoap repository,
        final _RepositoryExtensionsSoap repositoryExtensions,
        final _Repository4Soap repository4,
        final _Repository5Soap repository5) {
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        // TFS 2010 and 2012 proxies may be null

        this.client = client;
        this.runtimeWorkspaceCache = client.getRuntimeWorkspaceCache();

        this.repository = repository;
        this.repositoryExtensions = repositoryExtensions;
        this.repository4 = repository4;
        this.repository5 = repository5;

        if (this.repository5 != null) {
            final ServerDataProvider provider = client.getConnection().getServerDataProvider();

            /**
             * TFS 2012 QU1 early CTPs
             */
            final String repository5Uri = provider.locationForCurrentConnection(
                ServiceInterfaceNames.VERSION_CONTROL_5,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_5);

            /**
             * TFS 2012 QU1 early RTM
             */
                final String repository5Dot1Uri = provider.locationForCurrentConnection(
                    ServiceInterfaceNames.VERSION_CONTROL_5_DOT_1,
                    ServiceInterfaceIdentifiers.VERSION_CONTROL_5_DOT_1);

            if (null != repository5Dot1Uri) {
                serviceLevel = WebServiceLevel.TFS_2012_QU1_1;
            } else if (null != repository5Uri) {
                serviceLevel = WebServiceLevel.TFS_2012_QU1;
            } else {
                serviceLevel = WebServiceLevel.TFS_2012_QU1;
            }
        } else if (this.repository4 != null) {
            final ServerDataProvider provider = client.getConnection().getServerDataProvider();

            final String repository4Dot1Uri = provider.locationForCurrentConnection(
                ServiceInterfaceNames.VERSION_CONTROL_4_DOT_1,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_4_DOT_1);

            final String repository4Dot2Uri = provider.locationForCurrentConnection(
                ServiceInterfaceNames.VERSION_CONTROL_4_DOT_2,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_4_DOT_2);

            final String repository4Dot3Uri = provider.locationForCurrentConnection(
                ServiceInterfaceNames.VERSION_CONTROL_4_DOT_3,
                ServiceInterfaceIdentifiers.VERSION_CONTROL_4_DOT_3);

            if (null != repository4Dot3Uri) {
                serviceLevel = WebServiceLevel.TFS_2012_3;
            } else if (null != repository4Dot2Uri) {
                serviceLevel = WebServiceLevel.TFS_2012_2;
            } else if (null != repository4Dot1Uri) {
                serviceLevel = WebServiceLevel.TFS_2012_1;
            } else {
                serviceLevel = WebServiceLevel.TFS_2012;
            }
        } else if (this.repositoryExtensions != null) {
            this.serviceLevel = WebServiceLevel.TFS_2010;
        } else {
            this.serviceLevel = WebServiceLevel.PRE_TFS_2010;
        }

    }

    public VersionControlClient getVersionControlClient() {
        return this.client;
    }

    /**
     * For use outside this class in very specific scenarios only.
     */
    protected _RepositorySoap getRepository() {
        return this.repository;
    }

    /**
     * For use outside this class in very specific scenarios only.
     */
    protected _RepositoryExtensionsSoap getRepositoryExtensions() {
        return this.repositoryExtensions;
    }

    /**
     * For use outside this class in very specific scenarios only.
     */
    protected _Repository4Soap getRepository4() {
        return this.repository4;
    }

    /**
     * For use outside this class in very specific scenarios only.
     */
    protected _Repository5Soap getRepository5() {
        return this.repository5;
    }

    public WebServiceLevel getServiceLevel() {
        return this.serviceLevel;
    }

    public SupportedFeatures getSupportedFeatures() {
        if (supportedFeatures == null) {
            try {
                supportedFeatures = getRepositoryProperties().getSupportedFeatures();
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        }
        return this.supportedFeatures;
    }

    private void requireSupportedFeature(final SupportedFeatures required, final String featureNotSupportedMessage) {
        Check.notNull(required, "required"); //$NON-NLS-1$
        Check.notNull(featureNotSupportedMessage, "featureNotSupportedMessage"); //$NON-NLS-1$

        /*
         * We only need to check the supported features bit if we are
         * communicating with a pre-Dev10 server.
         */
        if (getServiceLevel() == WebServiceLevel.PRE_TFS_2010) {
            if (getSupportedFeatures().containsAll(required) == false) {
                throw new FeatureNotSupportedException(featureNotSupportedMessage);
            }
        }
    }

    private void requireServiceLevel(final WebServiceLevel serviceLevel, final String featureNotSupportedMessage) {
        Check.notNull(serviceLevel, "serviceLevel"); //$NON-NLS-1$
        Check.notNull(featureNotSupportedMessage, "featureNotSupportedMessage"); //$NON-NLS-1$

        if (getServiceLevel().getValue() < serviceLevel.getValue()) {
            throw new FeatureNotSupportedException(featureNotSupportedMessage);
        }
    }

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
        try {
            playbackQueuedEdits(workspaceName, ownerName);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                repository5.addConflict(
                    workspaceName,
                    ownerName,
                    conflictType.getWebServiceObject(),
                    itemId,
                    versionFrom,
                    pendingChangeId,
                    LocalPath.nativeToTFS(sourceLocalItem),
                    LocalPath.nativeToTFS(targetLocalItem),
                    reason.getValue(),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else {
                repository.addConflict(
                    workspaceName,
                    ownerName,
                    conflictType.getWebServiceObject(),
                    itemId,
                    versionFrom,
                    pendingChangeId,
                    LocalPath.nativeToTFS(sourceLocalItem),
                    LocalPath.nativeToTFS(targetLocalItem),
                    reason.getValue());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void promotePendingWorkspaceMappings(
        final String workspaceName,
        final String ownerName,
        final int projectNotificationId) {
        try {
            // This webmethod was actually added in Dev14, but it's only called
            // in response to
            // the server throwing a particular exception that is new in Dev14.
            // Consequently,
            // we have no versioning check in the web service layer for this
            // webmethod.

            repository5.promotePendingWorkspaceMappings(workspaceName, ownerName, projectNotificationId);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public String checkAuthentication() {
        try {
            return repository.checkAuthentication();
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public CheckinResult checkIn(
        final String workspaceName,
        final String ownerName,
        final String[] serverItems,
        final Changeset changeset,
        final CheckinNotificationInfo checkinNotificationInfo,
        final CheckinFlags checkinFlags,
        final UploadedBaselinesCollection uploadedBaselinesCollection,
        final AtomicReference<Failure[]> conflicts /* Dev11 */,
        final AtomicReference<Failure[]> failures,
        final boolean deferCheckIn /* Dev10 */,
        final int checkInTicket /* Dev10 */,
        final String[] itemPropertyFilters) {
        // serverItems may be null to make the server check in all changes
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$
        Check.notNull(checkinNotificationInfo, "checkinNotificationInfo"); //$NON-NLS-1$
        Check.notNull(checkinFlags, "checkinFlags"); //$NON-NLS-1$
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$
        Check.notNull(failures, "failures"); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, ownerName);

        if (checkinFlags.contains(CheckinFlags.SUPPRESS_EVENT)
            || checkinFlags.contains(CheckinFlags.VALIDATE_CHECK_IN_OWNER)
            || checkInTicket != 0
            || deferCheckIn) {
            /*
             * The user requested a flag not supported pre-Dev10, is using a
             * check-in ticket, or requested to defer their checkin (paged
             * checkin). These features are not supported before Dev10.
             */

            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.CheckInOptions2NotSupported")); //$NON-NLS-1$
        }

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_CheckInResponse response = repository5.checkIn(
                    workspaceName,
                    ownerName,
                    serverItems,
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo.getWebServiceObject(),
                    checkinFlags.toIntFlags(),
                    deferCheckIn,
                    checkInTicket,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                conflicts.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getConflicts()));
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                final _Repository4Soap_CheckInResponse response = repository4.checkIn(
                    workspaceName,
                    ownerName,
                    serverItems,
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo.getWebServiceObject(),
                    checkinFlags.toIntFlags(),
                    deferCheckIn,
                    checkInTicket);

                conflicts.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getConflicts()));
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                final _RepositoryExtensionsSoap_CheckInResponse response = repositoryExtensions.checkIn(
                    workspaceName,
                    ownerName,
                    serverItems,
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo.getWebServiceObject(),
                    checkinFlags.toIntFlags(),
                    deferCheckIn,
                    checkInTicket);

                failures.set(new Failure[0]);
                conflicts.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInResult());
            } else {
                /* Whidbey -> OrcasSP1 */

                final _RepositorySoap_CheckInResponse response = repository.checkIn(
                    workspaceName,
                    ownerName,
                    serverItems,
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo.getWebServiceObject(),
                    checkinFlags.toCheckinOptions().getWebServiceObject(),
                    deferCheckIn,
                    checkInTicket);

                failures.set(new Failure[0]);
                conflicts.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public CheckinResult checkInShelveset(
        final String shelvesetName,
        final String ownerName,
        final String changesetOwner,
        final CheckinNotificationInfo checkinNotificationInfo,
        final CheckinFlags checkinFlags,
        final AtomicReference<Failure[]> conflicts /* Dev11 */,
        final AtomicReference<Failure[]> failures) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.CheckInShelvesetNotSupported")); //$NON-NLS-1$

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_CheckInShelvesetResponse response = repository5.checkInShelveset(
                    shelvesetName,
                    ownerName,
                    changesetOwner,
                    checkinNotificationInfo.getWebServiceObject(),
                    checkinFlags.toIntFlags(),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                conflicts.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getConflicts()));
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInShelvesetResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                final _Repository4Soap_CheckInShelvesetResponse response = repository4.checkInShelveset(
                    shelvesetName,
                    ownerName,
                    changesetOwner,
                    checkinNotificationInfo.getWebServiceObject(),
                    checkinFlags.toIntFlags());

                conflicts.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getConflicts()));
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInShelvesetResult());
            } else {
                final _RepositoryExtensionsSoap_CheckInShelvesetResponse response =
                    repositoryExtensions.checkInShelveset(
                        shelvesetName,
                        ownerName,
                        changesetOwner,
                        checkinNotificationInfo.getWebServiceObject(),
                        checkinFlags.toIntFlags());

                conflicts.set(new Failure[0]);
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCheckInShelvesetResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Failure[] checkPendingChanges(
        final String workspaceName,
        final String ownerName,
        final String[] serverItems) {
        try {
            playbackQueuedEdits(workspaceName, ownerName);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                return (Failure[]) WrapperUtils.wrap(
                    Failure.class,
                    repository5.checkPendingChanges(
                        workspaceName,
                        ownerName,
                        serverItems,
                        VersionControlConstants.MAX_SERVER_PATH_SIZE));
            } else {
                return (Failure[]) WrapperUtils.wrap(
                    Failure.class,
                    repository.checkPendingChanges(workspaceName, ownerName, serverItems));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void createAnnotation(
        final String annotationName,
        final String annotatedItem,
        final int version,
        final String annotationValue,
        final String comment,
        final boolean overwrite) {
        try {
            repository.createAnnotation(annotationName, annotatedItem, version, annotationValue, comment, overwrite);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public CheckinResult createBranch(
        final String sourcePath,
        final String targetPath,
        final VersionSpec version,
        final Changeset changeset /* Dev10 */,
        final CheckinNotificationInfo checkinNotificationInfo /* Dev10 */,
        final Mapping[] mappings /* Dev10 */,
        final AtomicReference<Failure[]> failures /* Dev11 */) {
        // Orcas SP1
        requireSupportedFeature(
            SupportedFeatures.CREATE_BRANCH,
            Messages.getString("WebServiceLayer.CreateBranchNotSupported")); //$NON-NLS-1$

        /*
         * The Dev10 RTM OM did not fail a request because it specified
         * Dev10-only parameters to CreateBranch. We continue that policy here.
         */

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_CreateBranchResponse response = repository5.createBranch(
                    sourcePath,
                    targetPath,
                    version.getWebServiceObject(),
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo != null ? checkinNotificationInfo.getWebServiceObject() : null,
                    (_Mapping[]) (mappings != null ? WrapperUtils.unwrap(_Mapping.class, mappings) : null),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCreateBranchResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                final _Repository4Soap_CreateBranchResponse response = repository4.createBranch(
                    sourcePath,
                    targetPath,
                    version.getWebServiceObject(),
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo != null ? checkinNotificationInfo.getWebServiceObject() : null,
                    (_Mapping[]) (mappings != null ? WrapperUtils.unwrap(_Mapping.class, mappings) : null));

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                return new CheckinResult(response.getCreateBranchResult());
            } else {
                /* OrcasSP1 -> Dev10 */

                final _CheckinResult response = repository.createBranch(
                    sourcePath,
                    targetPath,
                    version.getWebServiceObject(),
                    changeset.getWebServiceObject(),
                    checkinNotificationInfo != null ? checkinNotificationInfo.getWebServiceObject() : null,
                    (_Mapping[]) (mappings != null ? WrapperUtils.unwrap(_Mapping.class, mappings) : null));

                failures.set(new Failure[0]);
                return new CheckinResult(response);
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void createCheckinNoteDefinition(
        final String associatedServerItem,
        final CheckinNoteFieldDefinition[] checkinNoteFields) {
        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                repository5.createCheckinNoteDefinition(
                    associatedServerItem,
                    (_CheckinNoteFieldDefinition[]) WrapperUtils.unwrap(
                        _CheckinNoteFieldDefinition.class,
                        checkinNoteFields),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else {
                repository.createCheckinNoteDefinition(
                    associatedServerItem,
                    (_CheckinNoteFieldDefinition[]) WrapperUtils.unwrap(
                        _CheckinNoteFieldDefinition.class,
                        checkinNoteFields));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Workspace createWorkspace(final Workspace workspace) {
        if (workspace.getLocation().equals(WorkspaceLocation.LOCAL)) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012,
                Messages.getString("WebServiceLayer.LocalWorkspacesNotSupported")); //$NON-NLS-1$
        }

        if (workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_1,
                Messages.getString("WebServiceLayer.SetFileTimeNotSupported")); //$NON-NLS-1$
        }

        if (workspace.getPermissionsProfile().getBuiltinIndex() != WorkspacePermissionProfile.BUILTIN_PROFILE_INDEX_PRIVATE) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.PublicWorkspacesNotSupported")); //$NON-NLS-1$
        }

        if (getServiceLevel() == WebServiceLevel.PRE_TFS_2010) {
            boolean hasOneLevelMappings = false;

            for (final WorkingFolder folder : workspace.getFolders()) {
                if (folder.getDepth() == RecursionType.ONE_LEVEL) {
                    hasOneLevelMappings = true;
                    break;
                }
            }

            if (hasOneLevelMappings) {
                /* Orcas */
                requireSupportedFeature(
                    SupportedFeatures.ONE_LEVEL_MAPPING,
                    Messages.getString("WebServiceLayer.DepthOneMappingsNotSupported")); //$NON-NLS-1$
            }
        }

        try {
            final _Workspace created = repository.createWorkspace(workspace.getWebServiceObject());

            if (created == null) {
                return null;
            }

            return new Workspace(created, getVersionControlClient());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void deleteAnnotation(
        final String annotationName,
        final String annotatedItem,
        final int version,
        final String annotationValue) {
        try {
            repository.deleteAnnotation(annotationName, annotatedItem, version, annotationValue);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void deleteBranchObject(final ItemIdentifier rootItem) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.BranchObjectsNotSupported")); //$NON-NLS-1$

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                repository5.deleteBranchObject(
                    rootItem.getWebServiceObject(),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else {
                repositoryExtensions.deleteBranchObject(rootItem.getWebServiceObject());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public LabelResult[] deleteLabel(final String labelName, final String labelScope) {
        try {
            return (LabelResult[]) WrapperUtils.wrap(LabelResult.class, repository.deleteLabel(labelName, labelScope));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void deleteProxy(final String proxyUrl) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.DeleteProxyNotSupported")); //$NON-NLS-1$

        try {
            repositoryExtensions.deleteProxy(proxyUrl);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void deleteShelveset(final String shelvesetName, final String ownerName) {
        try {
            repository.deleteShelveset(shelvesetName, ownerName);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void deleteWorkspace(final String workspaceName, final String ownerName) {
        try {
            playbackQueuedEdits(workspaceName, ownerName);

            repository.deleteWorkspace(workspaceName, ownerName);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Item[] destroy(
        final ItemSpec item,
        final VersionSpec versionSpec,
        final VersionSpec stopAtSpec,
        final DestroyFlags flags,
        final AtomicReference<Failure[]> failures,
        final AtomicReference<PendingSet[]> pendingChanges /* OrcasSP1 */,
        final AtomicReference<PendingSet[]> shelvedChanges /* OrcasSP1 */) {
        // Orcas
        requireSupportedFeature(SupportedFeatures.DESTROY, Messages.getString("WebServiceLayer.DestroyNotSupported")); //$NON-NLS-1$

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_DestroyResponse response = repository5.destroy(
                    item.getWebServiceObject(),
                    versionSpec.getWebServiceObject(),
                    stopAtSpec != null ? stopAtSpec.getWebServiceObject() : null,
                    flags.toIntFlags(),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingChanges.set((PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getPendingChanges()));
                shelvedChanges.set((PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getShelvedChanges()));
                return (Item[]) WrapperUtils.wrap(Item.class, response.getDestroyResult());
            } else {
                final _RepositorySoap_DestroyResponse response = repository.destroy(
                    item.getWebServiceObject(),
                    versionSpec.getWebServiceObject(),
                    stopAtSpec != null ? stopAtSpec.getWebServiceObject() : null,
                    flags.toIntFlags());

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingChanges.set((PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getPendingChanges()));
                shelvedChanges.set((PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getShelvedChanges()));

                return (Item[]) WrapperUtils.wrap(Item.class, response.getDestroyResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public GetOperation[][] get(
        final String workspaceName,
        final String ownerName,
        final GetRequest[] requests,
        final int maxResults /* Orcas */,
        GetOptions options /* OrcasSP1; two bools previously */,
        final String[] itemAttributeFilters /* Dev10 */,
        final String[] itemPropertyFilters /* Dev11 */,
        final boolean noGet) {
        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$
        }

        playbackQueuedEdits(workspaceName, ownerName);

        Check.isTrue(
            maxResults == 0 || options.contains(GetOptions.GET_ALL) == false,
            "Should never specify GetOptions.FORCE_GET_ALL and non-zero maxResults simultaneously"); //$NON-NLS-1$

        _GetOperation[][] arrayOfArrays;

        /*
         * Note that when we pass GetOptions over the wire as an int, we do not
         * want to pass NoDiskUpdate flag as that is a TEE client specific
         * thing.
         */
        options = options.remove(GetOptions.NO_DISK_UPDATE);
        if (noGet) {
            options = options.combine(GetOptions.PREVIEW);
        }

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                arrayOfArrays = repository5.get(
                    workspaceName,
                    ownerName,
                    (_GetRequest[]) WrapperUtils.unwrap(_GetRequest.class, requests),
                    maxResults,
                    options.toIntFlags(),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                arrayOfArrays = repository4.get(
                    workspaceName,
                    ownerName,
                    (_GetRequest[]) WrapperUtils.unwrap(_GetRequest.class, requests),
                    maxResults,
                    options.toIntFlags(),
                    itemPropertyFilters,
                    itemAttributeFilters);
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                arrayOfArrays = repositoryExtensions.get(
                    workspaceName,
                    ownerName,
                    (_GetRequest[]) WrapperUtils.unwrap(_GetRequest.class, requests),
                    maxResults,
                    options.toIntFlags(),
                    itemAttributeFilters);
            } else {
                /* Whidbey -> OrcasSP1 */

                arrayOfArrays = repository.get(
                    workspaceName,
                    ownerName,
                    (_GetRequest[]) WrapperUtils.unwrap(_GetRequest.class, requests),
                    options.contains(GetOptions.GET_ALL),
                    options.contains(GetOptions.PREVIEW),
                    maxResults,
                    options.toIntFlags());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        /*
         * Wrap the get operations. Make sure to skip null operations.
         */
        final GetOperation[][] operationsArrays = new GetOperation[arrayOfArrays.length][];
        for (int i = 0; i < arrayOfArrays.length; i++) {
            if (arrayOfArrays[i] == null) {
                continue;
            }

            operationsArrays[i] = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, arrayOfArrays[i]);
        }

        return operationsArrays;
    }

    public ArtifactPropertyValue getChangesetProperty(final int changesetId, final String[] propertyNameFilters) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$

        try {
            return new ArtifactPropertyValue(
                repositoryExtensions.getChangesetProperty(changesetId, propertyNameFilters));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public RepositoryProperties getRepositoryProperties() {
        try {
            return new RepositoryProperties(repository.getRepositoryProperties());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ArtifactPropertyValue[] getVersionedItemProperty(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final VersionSpec versionSpec,
        final DeletedState deletedState,
        final ItemType itemType,
        final String[] propertyNameFilters) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            return (ArtifactPropertyValue[]) WrapperUtils.wrap(
                ArtifactPropertyValue.class,
                repositoryExtensions.getVersionedItemProperty(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    versionSpec.getWebServiceObject(),
                    deletedState.getWebServiceObject(),
                    itemType.getWebServiceObject(),
                    propertyNameFilters));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public LabelResult[] labelItem(
        final String workspaceName,
        final String workspaceOwner,
        final VersionControlLabel label,
        final LabelItemSpec[] labelSpecs,
        final LabelChildOption children,
        final AtomicReference<Failure[]> failures) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_LabelItemResponse response = repository5.labelItem(
                    workspaceName,
                    workspaceOwner,
                    label.getWebServiceObject(),
                    (_LabelItemSpec[]) WrapperUtils.unwrap(_LabelItemSpec.class, labelSpecs),
                    children.getWebServiceObject(),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));

                return (LabelResult[]) WrapperUtils.wrap(LabelResult.class, response.getLabelItemResult());
            } else {
                final _RepositorySoap_LabelItemResponse response = repository.labelItem(
                    workspaceName,
                    workspaceOwner,
                    label.getWebServiceObject(),
                    (_LabelItemSpec[]) WrapperUtils.unwrap(_LabelItemSpec.class, labelSpecs),
                    children.getWebServiceObject());

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));

                return (LabelResult[]) WrapperUtils.wrap(LabelResult.class, response.getLabelItemResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

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
        final String[] itemAttributeFilters, /* Dev10 */
        final String[] itemPropertyFilters, /* Dev11 */
        final AtomicReference<ChangePendedFlags> changePendedFlags /* Dev11 */) {
        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$

        }

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_MergeResponse response = repository5.merge(
                    workspaceName,
                    workspaceOwner,
                    source.getWebServiceObject(),
                    target.getWebServiceObject(),
                    from != null ? from.getWebServiceObject() : null,
                    to != null ? to.getWebServiceObject() : null,
                    lockLevel.getWebServiceObject(),
                    mergeFlags.toIntFlags(),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getMergeResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                final _Repository4Soap_MergeResponse response = repository4.merge(
                    workspaceName,
                    workspaceOwner,
                    source.getWebServiceObject(),
                    target.getWebServiceObject(),
                    from != null ? from.getWebServiceObject() : null,
                    to != null ? to.getWebServiceObject() : null,
                    lockLevel.getWebServiceObject(),
                    mergeFlags.toIntFlags(),
                    itemPropertyFilters,
                    itemAttributeFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getMergeResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                final _RepositoryExtensionsSoap_MergeResponse response = repositoryExtensions.merge(
                    workspaceName,
                    workspaceOwner,
                    source.getWebServiceObject(),
                    target.getWebServiceObject(),
                    from != null ? from.getWebServiceObject() : null,
                    to != null ? to.getWebServiceObject() : null,
                    lockLevel.getWebServiceObject(),
                    mergeFlags.toIntFlags(),
                    itemPropertyFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getMergeResult());
            } else {
                /* Whidbey -> OrcasSP1 */

                final _RepositorySoap_MergeResponse response = repository.merge(
                    workspaceName,
                    workspaceOwner,
                    source.getWebServiceObject(),
                    target.getWebServiceObject(),
                    from != null ? from.getWebServiceObject() : null,
                    to != null ? to.getWebServiceObject() : null,
                    mergeFlags.toMergeOptions().getWebServiceObject(),
                    lockLevel.getWebServiceObject(),
                    mergeFlags.toIntFlags());

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                changePendedFlags.set(ChangePendedFlags.UNKNOWN);

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getMergeResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public GetOperation[] pendChanges(
        final String workspaceName,
        final String ownerName,
        final ChangeRequest[] changes,
        final PendChangesOptions pendChangesOptions /* Orcas */,
        final SupportedFeatures supportedFeatures /* Orcas */,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters, /* Dev11 */
        final String[] itemAttributeFilters, /* Dev10 */
        final boolean updateDisk, /* Not server parameter */
        final AtomicBoolean onlineOperation, /* Dev11 */
        final AtomicReference<ChangePendedFlags> changePendedFlags /* Dev11 */) {
        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$
        }

        if (null != changes && changes.length > 0 && changes[0].getRequestType().equals(RequestType.EDIT)) {
            // Supply the list of items we are trying to pend edits on to
            // PlaybackQueuedEdits so that it will not play back any queued
            // edits on those items. (Otherwise the result is two PendChanges
            // calls in a row on the same items.)
            playbackQueuedEdits(workspaceName, ownerName, changes);
        } else {
            playbackQueuedEdits(workspaceName, ownerName);
        }

        onlineOperation.set(true);

        Check.isTrue(
            pendChangesOptions.contains(PendChangesOptions.FORCE_CHECK_OUT_LOCAL_VERSION) == false
                || pendChangesOptions.contains(PendChangesOptions.GET_LATEST_ON_CHECKOUT) == false,
            "Should never specify PendChangesOptions.FORCE_CHECK_OUT_LOCAL_VERSION and PendChangesOptions.GET_LATEST_ON_CHECKOUT simultaneously"); //$NON-NLS-1$

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_PendChangesResponse response = repository5.pendChanges(
                    workspaceName,
                    ownerName,
                    (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                    pendChangesOptions.toIntFlags(),
                    supportedFeatures.toIntFlags(),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getPendChangesResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                final _Repository4Soap_PendChangesResponse response = repository4.pendChanges(
                    workspaceName,
                    ownerName,
                    (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                    pendChangesOptions.toIntFlags(),
                    supportedFeatures.toIntFlags(),
                    itemPropertyFilters,
                    itemAttributeFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getPendChangesResult());
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                final _RepositoryExtensionsSoap_PendChangesResponse response = repositoryExtensions.pendChanges(
                    workspaceName,
                    ownerName,
                    (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                    pendChangesOptions.toIntFlags(),
                    supportedFeatures.toIntFlags(),
                    itemAttributeFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getPendChangesResult());
            } else {
                /* Whidbey -> OrcasSP1 */

                final _RepositorySoap_PendChangesResponse response = repository.pendChanges(
                    workspaceName,
                    ownerName,
                    (_ChangeRequest[]) WrapperUtils.unwrap(_ChangeRequest.class, changes),
                    pendChangesOptions.toIntFlags(),
                    supportedFeatures.toIntFlags());

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(ChangePendedFlags.UNKNOWN);

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getPendChangesResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Annotation[] queryAnnotation(final String annotationName, final String annotatedItem, final int version) {
        try {
            return (Annotation[]) WrapperUtils.wrap(
                Annotation.class,
                repository.queryAnnotation(annotationName, annotatedItem, version));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    /*
     * This method differs from most others here in that it returns a web
     * service layer type, _BranchRelative, instead of a wrapper type. This is
     * because there is no wrapper, because the layer above this one will always
     * want to transform this array of arrays into one or more trees.
     * Maintaining wrappers isn't called for.
     */
    public _BranchRelative[][] queryBranches(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final VersionSpec version) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            return repository.queryBranches(
                workspaceName,
                workspaceOwner,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                version.getWebServiceObject());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public BranchObjectOwnership[] queryBranchObjectOwnership(final int[] changesets, final ItemSpec pathFilter) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.BranchObjectsNotSupported")); //$NON-NLS-1$

        try {
            return (BranchObjectOwnership[]) WrapperUtils.wrap(
                BranchObjectOwnership.class,
                repositoryExtensions.queryBranchObjectOwnership(changesets, pathFilter.getWebServiceObject()));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public BranchObject[] queryBranchObjects(final ItemIdentifier item, final RecursionType recursion) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.BranchObjectsNotSupported")); //$NON-NLS-1$

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                return (BranchObject[]) WrapperUtils.wrap(
                    BranchObject.class,
                    repository5.queryBranchObjects(
                        item.getWebServiceObject(),
                        recursion.getWebServiceObject(),
                        VersionControlConstants.MAX_SERVER_PATH_SIZE));
            } else {
                return (BranchObject[]) WrapperUtils.wrap(
                    BranchObject.class,
                    repositoryExtensions.queryBranchObjects(
                        item.getWebServiceObject(),
                        recursion.getWebServiceObject()));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Changeset queryChangeset(
        final int changesetId,
        final boolean includeChanges,
        final boolean generateDownloadUrls,
        final boolean includeSourceRenames /* Dev10 */) {
        // Requesting includeSourceRenames from a pre-Dev10 server is not a
        // compat failure.

        try {
            return new Changeset(
                repository.queryChangeset(changesetId, includeChanges, generateDownloadUrls, includeSourceRenames));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Changeset queryChangesetExtended(
        final int changesetId,
        final boolean includeChanges,
        final boolean generateDownloadUrls,
        String[] changesetPropertyFilters,
        final String[] itemAttributeFilters,
        final String[] itemPropertyFilters) {
        try {
            if ((itemPropertyFilters != null && itemPropertyFilters.length > 0)
                || (itemAttributeFilters != null && itemAttributeFilters.length > 0)
                || getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                requireServiceLevel(
                    WebServiceLevel.TFS_2012_2,
                    Messages.getString("WebServiceLayer.PropertyChangesNotSupported")); //$NON-NLS-1$

                return new Changeset(
                    repository4.queryChangesetExtended(
                        changesetId,
                        includeChanges,
                        generateDownloadUrls,
                        changesetPropertyFilters,
                        itemAttributeFilters,
                        itemPropertyFilters));
            } else {
                requireServiceLevel(
                    WebServiceLevel.TFS_2010,
                    Messages.getString("WebServiceLayer.GetChangesWithPropertiesNotSupported")); //$NON-NLS-1$

                // The server errors if the changeset property filters are null
                if (changesetPropertyFilters == null) {
                    changesetPropertyFilters = new String[0];
                }

                return new Changeset(
                    repositoryExtensions.queryChangesetExtended(
                        changesetId,
                        includeChanges,
                        generateDownloadUrls,
                        changesetPropertyFilters));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Change[] queryChangesForChangeset(
        final int changesetId,
        final boolean generateDownloadUrls,
        final int pageSize,
        final ItemSpec lastItem,
        final String[] itemAttributeFilters /* Dev10 */,
        final String[] itemPropertyFilters /* Dev11 */,
        final boolean includeMergeSourceInfo /* Dev10 */) {
        // Orcas SP1
        requireSupportedFeature(
            SupportedFeatures.GET_CHANGES_FOR_CHANGESET,
            Messages.getString("WebServiceLayer.GetChangesForChangesetNotSupported")); //$NON-NLS-1$

        if (null != itemPropertyFilters && itemPropertyFilters.length > 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$
        }

        // Requesting includeMergeSourceInfo from a pre-Dev10 server is not a
        // compat failure.

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                return (Change[]) WrapperUtils.wrap(
                    Change.class,
                    repository4.queryChangesForChangeset(
                        changesetId,
                        generateDownloadUrls,
                        pageSize,
                        lastItem != null ? lastItem.getWebServiceObject() : null,
                        itemPropertyFilters,
                        itemAttributeFilters,
                        includeMergeSourceInfo));
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                return (Change[]) WrapperUtils.wrap(
                    Change.class,
                    repositoryExtensions.queryChangesForChangeset(
                        changesetId,
                        generateDownloadUrls,
                        pageSize,
                        lastItem != null ? lastItem.getWebServiceObject() : null,
                        itemAttributeFilters,
                        includeMergeSourceInfo));
            } else {
                /* OrcasSP1 */

                return (Change[]) WrapperUtils.wrap(
                    Change.class,
                    repository.queryChangesForChangeset(
                        changesetId,
                        generateDownloadUrls,
                        pageSize,
                        lastItem != null ? lastItem.getWebServiceObject() : null));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public CheckinNoteFieldDefinition[] queryCheckinNoteDefinition(final String[] associatedServerItem) {
        try {
            return (CheckinNoteFieldDefinition[]) WrapperUtils.wrap(
                CheckinNoteFieldDefinition.class,
                repository.queryCheckinNoteDefinition(associatedServerItem));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public String[] queryCheckinNoteFieldNames() {
        try {
            return repository.queryCheckinNoteFieldNames();
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Conflict[] queryConflicts(final String workspaceName, final String ownerName, final ItemSpec[] items) {
        try {
            playbackQueuedEdits(workspaceName, ownerName);

            return (Conflict[]) WrapperUtils.wrap(Conflict.class, repository.queryConflicts(
                workspaceName,
                ownerName,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items)));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public String[] queryEffectiveGlobalPermissions(final String identityName) {
        try {

            return repository.queryEffectiveGlobalPermissions(identityName);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public String[] queryEffectiveItemPermissions(
        final String workspaceName,
        final String workspaceOwner,
        final String item,
        final String identityName) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            return repository.queryEffectiveItemPermissions(workspaceName, workspaceOwner, item, identityName);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public FileType[] queryFileTypes() {
        try {
            return (FileType[]) WrapperUtils.wrap(FileType.class, repository.queryFileTypes());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

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
        final boolean sortAscending /* Dev10 */) {
        // Requesting sortAscending from a pre-Dev10 server is not a compat
        // failure.

        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            return (Changeset[]) WrapperUtils.wrap(
                Changeset.class,
                repository.queryHistory(
                    workspaceName,
                    workspaceOwner,
                    itemSpec.getWebServiceObject(),
                    versionItem != null ? versionItem.getWebServiceObject() : null,
                    user,
                    versionFrom != null ? versionFrom.getWebServiceObject() : null,
                    versionTo != null ? versionTo.getWebServiceObject() : null,
                    maxCount,
                    includeFiles,
                    generateDownloadUrls,
                    slotMode,
                    sortAscending));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ItemSet[] queryItems(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final VersionSpec version,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean generateDownloadUrls,
        final GetItemsOptions options /* Dev10 */,
        final String[] itemPropertyFilters /* Dev11 */,
        final String[] itemAttributeFilters /* Dev11 */) {
        if ((itemPropertyFilters != null && itemPropertyFilters.length > 0)
            || (itemAttributeFilters != null && itemAttributeFilters.length > 0)
            || serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyChangesNotSupported")); //$NON-NLS-1$

            try {
                return (ItemSet[]) WrapperUtils.wrap(
                    ItemSet.class,
                    repository4.queryItems(
                        workspaceName,
                        workspaceOwner,
                        (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                        version.getWebServiceObject(),
                        deletedState.getWebServiceObject(),
                        itemType.getWebServiceObject(),
                        generateDownloadUrls,
                        options.toIntFlags(),
                        itemPropertyFilters,
                        itemAttributeFilters));
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        }

        // Specifying options to a pre-Dev10 server is not a compat failure.

        try {
            return (ItemSet[]) WrapperUtils.wrap(
                ItemSet.class,
                repository.queryItems(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    version.getWebServiceObject(),
                    deletedState.getWebServiceObject(),
                    itemType.getWebServiceObject(),
                    generateDownloadUrls,
                    options.toIntFlags()));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Item[] queryItemsByID(
        final int[] itemIds,
        final int changeSet,
        final boolean generateDownloadUrls,
        final GetItemsOptions options /* Dev10 */) {
        // Specifying options to a pre-Dev10 server is not a compat failure.

        try {
            return (Item[]) WrapperUtils.wrap(
                Item.class,
                repository.queryItemsById(itemIds, changeSet, generateDownloadUrls, options.toIntFlags()));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ExtendedItem[][] queryItemsExtended(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options, /* Dev10 */
        final String[] itemPropertyFilters) {
        playbackQueuedEdits(workspaceName, workspaceOwner);

        final _ExtendedItem[][] arrays;

        if ((itemPropertyFilters != null && itemPropertyFilters.length > 0)
            || serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyChangesNotSupported")); //$NON-NLS-1$

            try {
                arrays = repository4.queryItemsExtended(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    deletedState.getWebServiceObject(),
                    itemType.getWebServiceObject(),
                    options.toIntFlags(),
                    itemPropertyFilters);
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        } else {
            // Specifying options to a pre-Dev10 server is not a compat failure.
            try {
                arrays = repository.queryItemsExtended(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    deletedState.getWebServiceObject(),
                    itemType.getWebServiceObject(),
                    options.toIntFlags());
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        }

        if (arrays == null) {
            return new ExtendedItem[0][0];
        }

        final ExtendedItem[][] ret = new ExtendedItem[arrays.length][];

        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i] == null) {
                ret[i] = new ExtendedItem[0];
                continue;
            }

            ret[i] = (ExtendedItem[]) WrapperUtils.wrap(ExtendedItem.class, arrays[i]);

            /*
             * The items come back unsorted. Sort them so they are easier to
             * navigate.
             */
            Arrays.sort(ret[i]);
        }

        return ret;
    }

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
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                return (VersionControlLabel[]) WrapperUtils.wrap(
                    VersionControlLabel.class,
                    repository5.queryLabels(
                        workspaceName,
                        workspaceOwner,
                        labelName,
                        labelScope,
                        owner,
                        filterItem,
                        (versionFilterItem != null) ? versionFilterItem.getWebServiceObject()
                            : new _LatestVersionSpec(),
                        includeItems,
                        generateDownloadUrls,
                        VersionControlConstants.MAX_SERVER_PATH_SIZE));
            } else {
                return (VersionControlLabel[]) WrapperUtils.wrap(
                    VersionControlLabel.class,
                    repository.queryLabels(
                        workspaceName,
                        workspaceOwner,
                        labelName,
                        labelScope,
                        owner,
                        filterItem,
                        (versionFilterItem != null) ? versionFilterItem.getWebServiceObject()
                            : new _LatestVersionSpec(),
                        includeItems,
                        generateDownloadUrls));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public LocalVersion[][] queryLocalVersions(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.QueryLocalVersionsNotSupported")); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, workspaceOwner);

        /*
         * This Dev10 webmethod is on Repository instead of RepositoryExtensions
         * because it was added first to the "Orcas Dogfood" codebase.
         */

        final _LocalVersion[][] arrays;

        try {
            arrays = repository.queryLocalVersions(
                workspaceName,
                workspaceOwner,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        if (arrays == null) {
            return new LocalVersion[0][0];
        }

        final LocalVersion[][] ret = new LocalVersion[arrays.length][];

        for (int i = 0; i < arrays.length; i++) {
            if (arrays[i] == null) {
                ret[i] = new LocalVersion[0];
                continue;
            }

            ret[i] = (LocalVersion[]) WrapperUtils.wrap(LocalVersion.class, arrays[i]);
        }

        return ret;

    }

    public MergeCandidate[] queryMergeCandidates(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec source,
        final ItemSpec target,
        final MergeFlags mergeFlags) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                return (MergeCandidate[]) WrapperUtils.wrap(
                    MergeCandidate.class,
                    repository5.queryMergeCandidates(
                        workspaceName,
                        workspaceOwner,
                        source.getWebServiceObject(),
                        target.getWebServiceObject(),
                        mergeFlags.toIntFlags(),
                        VersionControlConstants.MAX_SERVER_PATH_SIZE));
            } else {
                return (MergeCandidate[]) WrapperUtils.wrap(
                    MergeCandidate.class,
                    repository.queryMergeCandidates(
                        workspaceName,
                        workspaceOwner,
                        source.getWebServiceObject(),
                        target.getWebServiceObject(),
                        mergeFlags.toIntFlags()));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ItemIdentifier[] queryMergeRelationships(final String serverItem) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.QueryMergeRelationshipsNotSupported")); //$NON-NLS-1$

        try {
            return (ItemIdentifier[]) WrapperUtils.wrap(
                ItemIdentifier.class,
                repositoryExtensions.queryMergeRelationships(serverItem));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

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
        final boolean showAll /* Dev10 */,
        final AtomicReference<Changeset[]> changesets) {
        // Specifying showAll to a pre-Dev10 server is not a compat failure.

        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            final _RepositorySoap_QueryMergesResponse response = repository.queryMerges(
                workspaceName,
                workspaceOwner,
                (source != null) ? source.getWebServiceObject() : null,
                (versionSource != null) ? versionSource.getWebServiceObject() : null,
                target.getWebServiceObject(),
                versionTarget.getWebServiceObject(),
                (versionFrom != null) ? versionFrom.getWebServiceObject() : null,
                (versionTo != null) ? versionTo.getWebServiceObject() : null,
                maxChangesets,
                showAll);

            changesets.set((Changeset[]) WrapperUtils.wrap(Changeset.class, response.getChangesets()));

            return (ChangesetMerge[]) WrapperUtils.wrap(ChangesetMerge.class, response.getQueryMergesResult());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ExtendedMerge[] queryMergesExtended(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec target,
        final VersionSpec versionTarget,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final QueryMergesExtendedOptions options) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.ExtendedMergesNotSupported")); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            return (ExtendedMerge[]) WrapperUtils.wrap(
                ExtendedMerge.class,
                repositoryExtensions.queryMergesExtended(
                    workspaceName,
                    workspaceOwner,
                    target.getWebServiceObject(),
                    versionTarget.getWebServiceObject(),
                    versionFrom != null ? versionFrom.getWebServiceObject() : null,
                    versionTo != null ? versionTo.getWebServiceObject() : null,
                    options.toIntFlags()));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

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
        final boolean showAll /* Dev10 */) {
        /*
         * Whidbey SP1 We have no way to distinguish Whidbey RTM from Whidbey
         * SP1, so call the webmethod blindly.
         */

        // Specifying showAll to a pre-Dev10 server is not a compat failure.

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            return new ChangesetMergeDetails(repository.queryMergesWithDetails(
                workspaceName,
                workspaceOwner,
                (source != null) ? source.getWebServiceObject() : null,
                (versionSource != null) ? versionSource.getWebServiceObject() : null,
                target.getWebServiceObject(),
                versionTarget.getWebServiceObject(),
                versionFrom != null ? versionFrom.getWebServiceObject() : null,
                versionTo != null ? versionTo.getWebServiceObject() : null,
                maxChangesets,
                showAll));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public PendingChange[] queryPendingChangesByID(final int[] pendingChangeIds, final boolean generateDownloadUrls) {
        try {
            return (PendingChange[]) WrapperUtils.wrap(
                PendingChange.class,
                repository.queryPendingChangesById(pendingChangeIds, generateDownloadUrls));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public PendingChange[] queryPendingChangesForWorkspace(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final int pageSize,
        final String lastChange,
        final boolean includeMergeInfo,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters /* Dev11 */) {
        playbackQueuedEdits(workspaceName, workspaceOwner);

        if ((itemPropertyFilters != null && itemPropertyFilters.length > 0)
            || serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyChangesNotSupported")); //$NON-NLS-1$

            try {
                final _Repository4Soap_QueryPendingChangesForWorkspaceResponse response =
                    repository4.queryPendingChangesForWorkspace(
                        workspaceName,
                        workspaceOwner,
                        (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                        generateDownloadUrls,
                        pageSize,
                        lastChange,
                        includeMergeInfo,
                        itemPropertyFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));

                return (PendingChange[]) WrapperUtils.wrap(
                    PendingChange.class,
                    response.getQueryPendingChangesForWorkspaceResult());
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        }

        try {
            final _RepositoryExtensionsSoap_QueryPendingChangesForWorkspaceResponse response =
                repositoryExtensions.queryPendingChangesForWorkspace(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls,
                    pageSize,
                    lastChange,
                    includeMergeInfo);

            failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));

            return (PendingChange[]) WrapperUtils.wrap(
                PendingChange.class,
                response.getQueryPendingChangesForWorkspaceResult());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public PendingSet[] queryPendingSets(
        final String localWorkspaceName,
        final String localWorkspaceOwner,
        final String queryWorkspaceName,
        final String ownerName,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final AtomicReference<Failure[]> failures,
        final boolean includeCandidates, /* Dev11 */
        final String[] itemPropertyFilters /* Dev11 */) {
        playbackQueuedEdits(localWorkspaceName, localWorkspaceOwner);
        playbackQueuedEdits(queryWorkspaceName, ownerName);

        final PendingSet[] pendingSets;

        if ((itemPropertyFilters != null && itemPropertyFilters.length > 0)
            || serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyChangesNotSupported")); //$NON-NLS-1$

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_QueryPendingSetsResponse response = repository5.queryPendingSets(
                    localWorkspaceName,
                    localWorkspaceOwner,
                    queryWorkspaceName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls,
                    itemPropertyFilters,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingSets = (PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getQueryPendingSetsResult());
            } else {
                final _Repository4Soap_QueryPendingSetsResponse response = repository4.queryPendingSets(
                    localWorkspaceName,
                    localWorkspaceOwner,
                    queryWorkspaceName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls,
                    itemPropertyFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingSets = (PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getQueryPendingSetsResult());
            }
        } else {
            try {
                final _RepositorySoap_QueryPendingSetsResponse response = repository.queryPendingSets(
                    localWorkspaceName,
                    localWorkspaceOwner,
                    queryWorkspaceName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingSets = (PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getQueryPendingSetsResult());
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        }

        for (final PendingSet ps : pendingSets) {
            ps.setPendingSetDetails();
        }

        return pendingSets;
    }

    public GUID queryPendingChangeSignature(final String workspaceName, final String ownerName) {
        requireServiceLevel(
            WebServiceLevel.TFS_2012,
            Messages.getString("WebServiceLayer.LocalWorkspacesNotSupported")); //$NON-NLS-1$

        try {
            return new GUID(repository4.queryPendingChangeSignature(workspaceName, ownerName));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public PendingSet[] queryShelvedChanges(
        final String localWorkspaceName,
        final String localWorkspaceOwner,
        final String shelvesetName,
        final String ownerName,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final AtomicReference<Failure[]> failures,
        final String[] itemPropertyFilters /* Dev11 */) {
        playbackQueuedEdits(localWorkspaceName, localWorkspaceOwner);

        final PendingSet[] pendingSets;

        if ((itemPropertyFilters != null && itemPropertyFilters.length > 0)
            || serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
            try {
                final _Repository4Soap_QueryShelvedChangesResponse response = repository4.queryShelvedChanges(
                    localWorkspaceName,
                    localWorkspaceOwner,
                    shelvesetName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls,
                    itemPropertyFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingSets =
                    (PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getQueryShelvedChangesResult());
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        } else {
            try {
                final _RepositorySoap_QueryShelvedChangesResponse response = repository.queryShelvedChanges(
                    localWorkspaceName,
                    localWorkspaceOwner,
                    shelvesetName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                    generateDownloadUrls);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                pendingSets =
                    (PendingSet[]) WrapperUtils.wrap(PendingSet.class, response.getQueryShelvedChangesResult());
            } catch (final ProxyException e) {
                throw VersionControlExceptionMapper.map(e);
            }
        }

        for (final PendingSet ps : pendingSets) {
            ps.setPendingSetDetails();
        }

        return pendingSets;
    }

    public Shelveset[] queryShelvesets(
        final String shelvesetName,
        final String ownerName,
        final String[] propertyNameFilters) {
        if (null != propertyNameFilters && propertyNameFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012,
                Messages.getString("WebServiceLayer.ShelvesetPropertyOperationsNotSupported")); //$NON-NLS-1$
        }

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_1.getValue()) {
                return (Shelveset[]) WrapperUtils.wrap(
                    Shelveset.class,
                    repository4.queryShelvesets(shelvesetName, ownerName, propertyNameFilters));
            } else {
                /* Whidbey -> Dev10 */

                return (Shelveset[]) WrapperUtils.wrap(
                    Shelveset.class,
                    repository.queryShelvesets(shelvesetName, ownerName));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Workspace queryWorkspace(final String workspaceName, final String ownerName) {
        playbackQueuedEdits(workspaceName, ownerName);

        _Workspace workspace = null;

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                workspace = repository4.queryWorkspace(workspaceName, ownerName);
            } else {
                /* Whidbey -> Dev10 */
                workspace = repository.queryWorkspace(workspaceName, ownerName);
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        if (workspace == null) {
            return null;
        }

        return new Workspace(workspace, getVersionControlClient());
    }

    public Workspace[] queryWorkspaces(
        final String ownerName,
        final String computer,
        final WorkspacePermissions permissionsFilter /* Dev10 */) {
        _Workspace[] workspaces = new _Workspace[0];
        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                workspaces = repository4.queryWorkspaces(ownerName, computer, permissionsFilter.toIntFlags());
            } else {
                /* Whidbey -> Dev10 */

                // Specifying permissionsFilter to a pre-Dev10 server is not a
                // compat failure.

                workspaces = repository.queryWorkspaces(ownerName, computer, permissionsFilter.toIntFlags());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        /*
         * WrapperUtils can't be used because Workspace is special (needs a
         * VersionControlClient as a construction argument).
         */
        final Workspace[] ret = new Workspace[workspaces.length];
        for (int i = 0; i < workspaces.length; i++) {
            ret[i] = new Workspace(workspaces[i], getVersionControlClient());
        }

        return ret;
    }

    public WorkspaceItemSet[] queryWorkspaceItems(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean generateDownloadUrls,
        final int options) {
        requireServiceLevel(
            WebServiceLevel.TFS_2012_2,
            Messages.getString("WebServiceLayer.LocalWorkspacesNotSupported")); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            final _WorkspaceItemSet[] itemSets = repository4.queryWorkspaceItems(
                workspaceName,
                workspaceOwner,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                deletedState.getWebServiceObject(),
                itemType.getWebServiceObject(),
                generateDownloadUrls,
                options);

            return (WorkspaceItemSet[]) WrapperUtils.wrap(WorkspaceItemSet.class, itemSets);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ReconcileResult reconcileLocalWorkspace(
        final String workspaceName,
        final String ownerName,
        final GUID pendingChangeSignature,
        final LocalPendingChange[] pendingChanges,
        final ServerItemLocalVersionUpdate[] localVersionUpdates,
        final boolean clearLocalVersionTable,
        final boolean throwOnProjectRenamed) {
        requireServiceLevel(
            WebServiceLevel.TFS_2012,
            Messages.getString("WebServiceLayer.LocalWorkspacesNotSupported")); //$NON-NLS-1$

        try {
            playbackQueuedEdits(workspaceName, ownerName);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                return new ReconcileResult(
                    repository5.reconcileLocalWorkspace(
                        workspaceName,
                        ownerName,
                        pendingChangeSignature.getGUIDString(),
                        (_LocalPendingChange[]) WrapperUtils.unwrap(_LocalPendingChange.class, pendingChanges),
                        (_ServerItemLocalVersionUpdate[]) WrapperUtils.unwrap(
                            _ServerItemLocalVersionUpdate.class,
                            localVersionUpdates),
                        clearLocalVersionTable,
                        throwOnProjectRenamed,
                        VersionControlConstants.MAX_SERVER_PATH_SIZE));
            }

            return new ReconcileResult(
                repository4.reconcileLocalWorkspace(
                    workspaceName,
                    ownerName,
                    pendingChangeSignature.getGUIDString(),
                    (_LocalPendingChange[]) WrapperUtils.unwrap(_LocalPendingChange.class, pendingChanges),
                    (_ServerItemLocalVersionUpdate[]) WrapperUtils.unwrap(
                        _ServerItemLocalVersionUpdate.class,
                        localVersionUpdates),
                    clearLocalVersionTable));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void refreshIdentityDisplayName() {
        try {
            repository.refreshIdentityDisplayName();
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void removeLocalConflicts(
        final String workspaceName,
        final String ownerName,
        final List<Conflict> conflicts,
        final ResolveErrorOptions errorOptions,
        final ConflictResolvedHandler onResolvedConflict,
        final ConflictResolveErrorHandler onResolveError) throws CoreCancelException {
        playbackQueuedEdits(workspaceName, ownerName);

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();

        for (final Conflict conflict : conflicts) {
            if (taskMonitor.isCanceled()) {
                throw new CoreCancelException();
            }

            try {
                repository.removeLocalConflict(workspaceName, ownerName, conflict.getConflictID());

                // Fire event for the resolved conflict
                onResolvedConflict.conflictResolved(conflict, null, null, null, ChangePendedFlags.UNKNOWN);
            } catch (RuntimeException ex) {
                if (ex instanceof ProxyException) {
                    ex = VersionControlExceptionMapper.map(ex);
                }

                if (ResolveErrorOptions.THROW_ON_ERROR.equals(errorOptions)) {
                    throw ex;
                }

                onResolveError.conflictResolveError(conflict, ex);
            }
        }
    }

    public void resolve(
        final String workspaceName,
        final String ownerName,
        final Conflict[] conflicts,
        final String[] itemAttributeFilters /* Dev10 */,
        final String[] itemPropertyFilters /* Dev 11 */,
        final ResolveErrorOptions errorOptions,
        final ConflictResolvedHandler onResolvedConflict,
        final ConflictResolveErrorHandler onResolveError) throws CoreCancelException {
        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$
        }

        playbackQueuedEdits(workspaceName, ownerName);

        final Set<Integer> previouslyResolved = new HashSet<Integer>();

        for (final Conflict conflict : conflicts) {
            if (TaskMonitorService.getTaskMonitor().isCanceled()) {
                throw new CoreCancelException();
            }

            try {
                if (previouslyResolved.contains(conflict.getConflictID())) {
                    continue;
                }

                final String newPath = conflict.getResolutionOptions().getNewPath() == null ? null
                    : ItemPath.smartNativeToTFS(conflict.getResolutionOptions().getNewPath());

                final int acceptMergeEncoding = conflict.getResolutionOptions().getAcceptMergeEncoding() == null
                    ? VersionControlConstants.ENCODING_UNCHANGED
                    : conflict.getResolutionOptions().getAcceptMergeEncoding().getCodePage();

                final ChangePendedFlags flags;
                final GetOperation[] getOps;
                final GetOperation[] undoOps;
                final Conflict[] resolvedConflicts;

                if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                    final _Repository5Soap_ResolveResponse response = repository5.resolve(
                        workspaceName,
                        ownerName,
                        conflict.getConflictID(),
                        conflict.getResolution().getWebServiceObject(),
                        newPath,
                        acceptMergeEncoding,
                        _LockLevel.Unchanged,
                        (_PropertyValue[]) WrapperUtils.unwrap(
                            _PropertyValue.class,
                            conflict.getResolutionOptions().getAcceptMergeProperties()),
                        itemPropertyFilters,
                        itemAttributeFilters,
                        VersionControlConstants.MAX_SERVER_PATH_SIZE);

                    getOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getResolveResult());
                    undoOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoOperations());
                    resolvedConflicts = (Conflict[]) WrapperUtils.wrap(Conflict.class, response.getResolvedConflicts());
                    flags = new ChangePendedFlags(response.getChangePendedFlags());
                } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                    final _Repository4Soap_ResolveResponse response = repository4.resolve(
                        workspaceName,
                        ownerName,
                        conflict.getConflictID(),
                        conflict.getResolution().getWebServiceObject(),
                        newPath,
                        acceptMergeEncoding,
                        _LockLevel.Unchanged,
                        (_PropertyValue[]) WrapperUtils.unwrap(
                            _PropertyValue.class,
                            conflict.getResolutionOptions().getAcceptMergeProperties()),
                        itemPropertyFilters,
                        itemAttributeFilters);

                    getOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getResolveResult());
                    undoOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoOperations());
                    resolvedConflicts = (Conflict[]) WrapperUtils.wrap(Conflict.class, response.getResolvedConflicts());
                    flags = new ChangePendedFlags(response.getChangePendedFlags());
                } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                    final _RepositoryExtensionsSoap_ResolveResponse response = repositoryExtensions.resolve(
                        workspaceName,
                        ownerName,
                        conflict.getConflictID(),
                        conflict.getResolution().getWebServiceObject(),
                        newPath,
                        acceptMergeEncoding,
                        _LockLevel.Unchanged,
                        itemAttributeFilters);

                    getOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getResolveResult());
                    undoOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoOperations());
                    resolvedConflicts = (Conflict[]) WrapperUtils.wrap(Conflict.class, response.getResolvedConflicts());
                    flags = new ChangePendedFlags(response.getChangePendedFlags());
                } else
                /* Whidbey -> OrcasSP1 */
                {
                    final _RepositorySoap_ResolveResponse response = repository.resolve(
                        workspaceName,
                        ownerName,
                        conflict.getConflictID(),
                        conflict.getResolution().getWebServiceObject(),
                        newPath,
                        acceptMergeEncoding,
                        _LockLevel.Unchanged);

                    getOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getResolveResult());
                    undoOps = (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoOperations());
                    resolvedConflicts = (Conflict[]) WrapperUtils.wrap(Conflict.class, response.getResolvedConflicts());
                    flags = ChangePendedFlags.UNKNOWN;
                }

                for (final Conflict resolvedConflict : resolvedConflicts) {
                    previouslyResolved.add(resolvedConflict.getConflictID());
                }

                /* Fire event for the resolved conflict */
                onResolvedConflict.conflictResolved(conflict, getOps, undoOps, resolvedConflicts, flags);
            } catch (RuntimeException ex) {
                if (ex instanceof ProxyException) {
                    ex = VersionControlExceptionMapper.map(ex);
                }

                if (ResolveErrorOptions.THROW_ON_ERROR.equals(errorOptions)) {
                    throw ex;
                }

                onResolveError.conflictResolveError(conflict, ex);
            }
        }
    }

    public void resetCheckinDates(final Calendar lastCheckinDate) {
        /*
         * Use supported features as this was added in a qfe, and we couldn't
         * add a new web service.
         */
        requireSupportedFeature(
            SupportedFeatures.CHECKIN_DATES,
            Messages.getString("WebServiceLayer.CheckinDatesNotSupported")); //$NON-NLS-1$

        try {
            repositoryExtensions.resetCheckinDates(lastCheckinDate);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

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
        final String[] itemPropertyFilters, /* Dev11 */
        final AtomicReference<ChangePendedFlags> changePendedFlags /* Dev11 */) {
        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        }

        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (serviceLevel.getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_RollbackResponse response = repository5.rollback(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemVersion != null ? itemVersion.getWebServiceObject() : null,
                    from.getWebServiceObject(),
                    to.getWebServiceObject(),
                    rollbackOptions.toIntFlags(),
                    lockLevel.getWebServiceObject(),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getRollbackResult());
            } else if (serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                final _Repository4Soap_RollbackResponse response = repository4.rollback(
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemVersion != null ? itemVersion.getWebServiceObject() : null,
                    from.getWebServiceObject(),
                    to.getWebServiceObject(),
                    rollbackOptions.toIntFlags(),
                    lockLevel.getWebServiceObject(),
                    itemPropertyFilters,
                    itemAttributeFilters);

                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getRollbackResult());
            }

            requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.RollbackNotSupported")); //$NON-NLS-1$

            final _RepositoryExtensionsSoap_RollbackResponse response = repositoryExtensions.rollback(
                workspaceName,
                workspaceOwner,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                itemVersion != null ? itemVersion.getWebServiceObject() : null,
                from.getWebServiceObject(),
                to.getWebServiceObject(),
                rollbackOptions.toIntFlags(),
                lockLevel.getWebServiceObject(),
                itemAttributeFilters);

            conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
            failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
            changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

            return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getRollbackResult());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void setChangesetProperty(final int changesetId, final PropertyValue[] propertyValues) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$

        try {
            repositoryExtensions.setChangesetProperty(
                changesetId,
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, propertyValues));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void setFileTypes(final FileType[] fileTypes) {
        try {
            repository.setFileTypes((_FileType[]) WrapperUtils.unwrap(_FileType.class, fileTypes));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Failure[] setPendingChangeProperty(
        final String workspaceName,
        final String workspaceOwner,
        final ArtifactPropertyValue[] pendingChangePropertyValues) {
        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            return (Failure[]) WrapperUtils.wrap(
                Failure.class,
                repositoryExtensions.setPendingChangeProperty(
                    workspaceName,
                    workspaceOwner,
                    (_ArtifactPropertyValue[]) WrapperUtils.unwrap(
                        _ArtifactPropertyValue.class,
                        pendingChangePropertyValues)));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void setVersionedItemProperty(
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] itemSpecs,
        final VersionSpec versionSpec,
        final DeletedState deletedState,
        final ItemType itemType,
        final PropertyValue[] propertyValues) {
        if (versionSpec == null && serviceLevel.getValue() < WebServiceLevel.TFS_2012_2.getValue()) {
            throw new NotSupportedException(Messages.getString("WebServiceLayer.UnversionedAttributesNotSupported")); //$NON-NLS-1$
        }

        requireServiceLevel(
            WebServiceLevel.TFS_2010,
            Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$

        playbackQueuedEdits(workspaceName, workspaceOwner);

        try {
            repositoryExtensions.setVersionedItemProperty(
                workspaceName,
                workspaceOwner,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, itemSpecs),
                versionSpec.getWebServiceObject(),
                deletedState.getWebServiceObject(),
                itemType.getWebServiceObject(),
                (_PropertyValue[]) WrapperUtils.unwrap(PropertyValue.class, propertyValues));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Failure[] shelve(
        final String workspaceName,
        final String workspaceOwner,
        final String[] serverItems,
        final Shelveset shelveset,
        final boolean replace) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (serviceLevel.getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                return (Failure[]) WrapperUtils.wrap(
                    Failure.class,
                    repository5.shelve(
                        workspaceName,
                        workspaceOwner,
                        serverItems,
                        shelveset.getWebServiceObject(),
                        replace,
                        VersionControlConstants.MAX_SERVER_PATH_SIZE));
            } else {
                return (Failure[]) WrapperUtils.wrap(
                    Failure.class,
                    repository.shelve(
                        workspaceName,
                        workspaceOwner,
                        serverItems,
                        shelveset.getWebServiceObject(),
                        replace));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ExtendedMerge[] trackMerges(
        final int[] sourceChangesets,
        final ItemIdentifier sourceItem,
        final ItemIdentifier[] targetItems,
        final ItemSpec pathFilter,
        final AtomicReference<String[]> partialTargetItems) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.TrackMergesNotSupported")); //$NON-NLS-1$

        try {
            final _RepositoryExtensionsSoap_TrackMergesResponse response = repositoryExtensions.trackMerges(
                sourceChangesets,
                sourceItem.getWebServiceObject(),
                (_ItemIdentifier[]) WrapperUtils.unwrap(_ItemIdentifier.class, targetItems),
                pathFilter.getWebServiceObject());

            partialTargetItems.set(response.getPartialTargetItems());

            return (ExtendedMerge[]) WrapperUtils.wrap(ExtendedMerge.class, response.getTrackMergesResult());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public GetOperation[] undoPendingChanges(
        final String workspaceName,
        final String ownerName,
        final ItemSpec[] items,
        final AtomicReference<Failure[]> failures,
        final String[] itemAttributeFilters /* Dev10 */,
        final String[] itemPropertyFilters /* Dev11 */,
        final AtomicBoolean onlineOperation,
        final boolean deleteAdds,
        final AtomicReference<ChangePendedFlags> changePendedFlags /* Dev11 */) {
        onlineOperation.set(true);

        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$
        }

        try {
            playbackQueuedEdits(workspaceName, ownerName);

            if (serviceLevel.getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_UndoPendingChangesResponse response = repository5.undoPendingChanges(
                    workspaceName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoPendingChangesResult());
            } else if (serviceLevel.getValue() >= WebServiceLevel.TFS_2012_2.getValue()) {
                final _Repository4Soap_UndoPendingChangesResponse response = repository4.undoPendingChanges(
                    workspaceName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemPropertyFilters,
                    itemAttributeFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoPendingChangesResult());
            }

            else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                final _RepositoryExtensionsSoap_UndoPendingChangesResponse response =
                    repositoryExtensions.undoPendingChanges(
                        workspaceName,
                        ownerName,
                        (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                        itemAttributeFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));
                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoPendingChangesResult());
            } else {
                final _RepositorySoap_UndoPendingChangesResponse response = repository.undoPendingChanges(
                    workspaceName,
                    ownerName,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items));

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                changePendedFlags.set(ChangePendedFlags.UNKNOWN);

                return (GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getUndoPendingChangesResult());
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public LabelResult[] unlabelItem(
        final String workspaceName,
        final String workspaceOwner,
        final String labelName,
        final String labelScope,
        final ItemSpec[] items,
        final VersionSpec version,
        final AtomicReference<Failure[]> failures) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (serviceLevel.getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_UnlabelItemResponse response = repository5.unlabelItem(
                    workspaceName,
                    workspaceOwner,
                    labelName,
                    labelScope,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    version.getWebServiceObject(),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));

                return (LabelResult[]) WrapperUtils.wrap(LabelResult.class, response.getUnlabelItemResult());
            }

            final _RepositorySoap_UnlabelItemResponse response = repository.unlabelItem(
                workspaceName,
                workspaceOwner,
                labelName,
                labelScope,
                (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                version.getWebServiceObject());

            failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));

            return (LabelResult[]) WrapperUtils.wrap(LabelResult.class, response.getUnlabelItemResult());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Shelveset unshelve(
        final String shelvesetName,
        final String shelvesetOwner,
        final String workspaceName,
        final String workspaceOwner,
        final ItemSpec[] items,
        final String[] itemAttributeFilters, /* Dev10 */
        final String[] itemPropertyFilters, /* Dev11 */
        final String[] shelvesetPropertyFilters /* Dev11 */,
        final boolean merge /* Dev11 */,
        final AtomicReference<Failure[]> failures,
        final AtomicReference<GetOperation[]> getOperations,
        final AtomicReference<Conflict[]> conflicts /* Dev11 */,
        final AtomicReference<ChangePendedFlags> changePendedFlags /* Dev11 */) {
        if (null != itemPropertyFilters && itemPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_2,
                Messages.getString("WebServiceLayer.PropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (merge) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012,
                Messages.getString("WebServiceLayer.UnshelveWithMergeNotSupported")); //$NON-NLS-1$
        } else if (null != shelvesetPropertyFilters && shelvesetPropertyFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012,
                Messages.getString("WebServiceLayer.ShelvesetPropertyOperationsNotSupported")); //$NON-NLS-1$
        } else if (null != itemAttributeFilters && itemAttributeFilters.length != 0) {
            requireServiceLevel(
                WebServiceLevel.TFS_2010,
                Messages.getString("WebServiceLayer.AttributeOperationsNotSupported")); //$NON-NLS-1$
        }

        _Shelveset shelveset = null;

        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                final _Repository5Soap_UnshelveResponse response = repository5.unshelve(
                    shelvesetName,
                    shelvesetOwner,
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    shelvesetPropertyFilters,
                    merge,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                getOperations.set((GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getGetOperations()));
                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                shelveset = response.getUnshelveResult();
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                final _Repository4Soap_UnshelveResponse response = repository4.unshelve(
                    shelvesetName,
                    shelvesetOwner,
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemPropertyFilters,
                    itemAttributeFilters,
                    shelvesetPropertyFilters,
                    merge);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                getOperations.set((GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getGetOperations()));
                conflicts.set((Conflict[]) WrapperUtils.wrap(Conflict.class, response.getConflicts()));
                changePendedFlags.set(new ChangePendedFlags(response.getChangePendedFlags()));

                shelveset = response.getUnshelveResult();
            } else if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()) {
                final _RepositoryExtensionsSoap_UnshelveResponse response = repositoryExtensions.unshelve(
                    shelvesetName,
                    shelvesetOwner,
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items),
                    itemAttributeFilters);

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                getOperations.set((GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getGetOperations()));
                conflicts.set(new Conflict[0]);
                changePendedFlags.set(ChangePendedFlags.UNKNOWN);

                shelveset = response.getUnshelveResult();
            } else {
                /* Whidbey -> OrcasSP1 */

                final _RepositorySoap_UnshelveResponse response = repository.unshelve(
                    shelvesetName,
                    shelvesetOwner,
                    workspaceName,
                    workspaceOwner,
                    (_ItemSpec[]) WrapperUtils.unwrap(_ItemSpec.class, items));

                failures.set((Failure[]) WrapperUtils.wrap(Failure.class, response.getFailures()));
                getOperations.set((GetOperation[]) WrapperUtils.wrap(GetOperation.class, response.getGetOperations()));
                conflicts.set(new Conflict[0]);
                changePendedFlags.set(ChangePendedFlags.UNKNOWN);

                shelveset = response.getUnshelveResult();

            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }

        /*
         * Turns out that the result.value can be null if the unshelve fails. In
         * which case return null and let the caller display the error message
         * that will have been thrown from the above events.
         */
        if (shelveset == null) {
            return null;
        }

        return new Shelveset(shelveset);
    }

    public void updateBranchObject(final BranchProperties branchProperties, final boolean updateExisting) {
        requireServiceLevel(WebServiceLevel.TFS_2010, Messages.getString("WebServiceLayer.BranchObjectsNotSupported")); //$NON-NLS-1$

        try {
            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                repository5.updateBranchObject(
                    branchProperties.getWebServiceObject(),
                    updateExisting,
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else {
                repositoryExtensions.updateBranchObject(branchProperties.getWebServiceObject(), updateExisting);
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void updateChangeset(final int changeset, final String comment, final CheckinNote checkinNote) {
        try {
            repository.updateChangeset(changeset, comment, checkinNote.getWebServiceObject());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void updateCheckinNoteFieldName(
        final String path,
        final String existingFieldName,
        final String newFieldName) {
        try {
            repository.updateCheckinNoteFieldName(path, existingFieldName, newFieldName);
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void updateLocalVersion(
        final String workspaceName,
        final String ownerName,
        final LocalVersionUpdate[] updates) {
        try {
            playbackQueuedEdits(workspaceName, ownerName);
            repository.updateLocalVersion(
                workspaceName,
                ownerName,
                (_LocalVersionUpdate[]) WrapperUtils.unwrap(_LocalVersionUpdate.class, updates));
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void updateLocalVersion(
        final String workspaceName,
        final String ownerName,
        final ServerItemLocalVersionUpdate[] updates) {
        requireServiceLevel(
            WebServiceLevel.TFS_2012,
            Messages.getString("WebServiceLayer.LocalWorkspacesNotSupported")); //$NON-NLS-1$

        try {
            playbackQueuedEdits(workspaceName, ownerName);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                repository5.updateLocalVersion(
                    workspaceName,
                    ownerName,
                    (_ServerItemLocalVersionUpdate[]) WrapperUtils.unwrap(_ServerItemLocalVersionUpdate.class, updates),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else {
                repository4.updateLocalVersion(
                    workspaceName,
                    ownerName,
                    (_ServerItemLocalVersionUpdate[]) WrapperUtils.unwrap(
                        _ServerItemLocalVersionUpdate.class,
                        updates));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void updateLocalVersion(
        final String workspaceName,
        final String ownerName,
        final ILocalVersionUpdate[] updates) {
        final List<ServerItemLocalVersionUpdate> serverItemUpdates = new ArrayList<ServerItemLocalVersionUpdate>();
        final List<LocalVersionUpdate> localVersionUpdates = new ArrayList<LocalVersionUpdate>();

        for (final ILocalVersionUpdate update : updates) {
            if (!update.isSendToServer()) {
                continue;
            }

            if (update.getSourceServerItem() != null
                && update.getSourceServerItem().length() > 0
                && getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                serverItemUpdates.add(new ServerItemLocalVersionUpdate(update));
            } else {
                localVersionUpdates.add(new LocalVersionUpdate(update));
            }
        }

        if (localVersionUpdates.size() > 0) {
            updateLocalVersion(
                workspaceName,
                ownerName,
                localVersionUpdates.toArray(new LocalVersionUpdate[localVersionUpdates.size()]));
        }

        if (serverItemUpdates.size() > 0) {
            updateLocalVersion(
                workspaceName,
                ownerName,
                serverItemUpdates.toArray(new ServerItemLocalVersionUpdate[serverItemUpdates.size()]));
        }
    }

    public void updatePendingState(
        final String workspaceName,
        final String workspaceOwner,
        final PendingState[] updates) {
        try {
            playbackQueuedEdits(workspaceName, workspaceOwner);

            if (getServiceLevel().getValue() >= WebServiceLevel.TFS_2012_QU1.getValue()) {
                repository5.updatePendingState(
                    workspaceName,
                    workspaceOwner,
                    (_PendingState[]) WrapperUtils.unwrap(_PendingState.class, updates),
                    VersionControlConstants.MAX_SERVER_PATH_SIZE);
            } else {
                repository.updatePendingState(
                    workspaceName,
                    workspaceOwner,
                    (_PendingState[]) WrapperUtils.unwrap(_PendingState.class, updates));
            }
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void updateShelveset(
        final String shelvesetName,
        final String shelvesetOwner,
        final Shelveset updatedShelveset) {
        requireServiceLevel(
            WebServiceLevel.TFS_2012_1,
            Messages.getString("WebServiceLayer.ShelvesetPropertyOperationsNotSupported")); //$NON-NLS-1$

        try {
            repository4.updateShelveset(shelvesetName, shelvesetOwner, updatedShelveset.getWebServiceObject());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public Workspace updateWorkspace(
        final String oldWorkspaceName,
        final String ownerName,
        final Workspace newWorkspace,
        final SupportedFeatures supportedFeatures /* Orcas */) {
        playbackQueuedEdits(oldWorkspaceName, ownerName);

        if (getServiceLevel() == WebServiceLevel.PRE_TFS_2010) {
            boolean hasOneLevelMappings = false;

            for (final WorkingFolder folder : newWorkspace.getFolders()) {
                if (folder.getDepth() == RecursionType.ONE_LEVEL) {
                    hasOneLevelMappings = true;
                    break;
                }
            }

            if (hasOneLevelMappings) {
                /* Orcas */
                requireSupportedFeature(
                    SupportedFeatures.ONE_LEVEL_MAPPING,
                    Messages.getString("WebServiceLayer.DepthOneMappingsNotSupported")); //$NON-NLS-1$
            }
        }

        if (newWorkspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
            requireServiceLevel(
                WebServiceLevel.TFS_2012_1,
                Messages.getString("WebServiceLayer.SetFileTimeNotSupported")); //$NON-NLS-1$
        }

        try {
            return new Workspace(
                repository.updateWorkspace(
                    oldWorkspaceName,
                    ownerName,
                    newWorkspace.getWebServiceObject(),
                    supportedFeatures.toIntFlags()),
                getVersionControlClient());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public ServerSettings getServerSettings() {
        if (getServiceLevel().getValue() < WebServiceLevel.TFS_2012_1.getValue()) {
            return null;
        }

        try {
            return new ServerSettings(repository4.getServerSettings());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    public void setServerSettings(final ServerSettings settings) {
        requireServiceLevel(WebServiceLevel.TFS_2012, Messages.getString("WebServiceLayer.ServerSettingsNotSupported")); //$NON-NLS-1$

        Check.notNull(settings, "settings"); //$NON-NLS-1$

        try {
            repository4.setServerSettings(settings.getWebServiceObject());
        } catch (final ProxyException e) {
            throw VersionControlExceptionMapper.map(e);
        }
    }

    /**
     * Given a workspace, returns all pending changes for that workspace by
     * calling queryPendingSets on the server.
     */
    public PendingChange[] queryServerPendingChanges(final Workspace workspace, final String[] itemPropertyFilters) {
        return queryServerPendingChanges(workspace, new ItemSpec[] {
            new ItemSpec(ServerPath.ROOT, RecursionType.FULL)
        }, false, itemPropertyFilters);
    }

    /**
     * Given a workspace, returns all pending changes for that workspace by
     * calling QueryPendingSets on the server.
     */
    public abstract PendingChange[] queryServerPendingChanges(
        Workspace workspace,
        ItemSpec[] itemSpecs,
        boolean generateDownloadUrls,
        String[] itemPropertyFilters);

    /**
     * Given a local workspace, returns all pending changes for that workspace
     * by calling QueryPendingSets on the server. Also returns the current
     * pending change signature from the server.
     */
    public abstract PendingChange[] queryServerPendingChanges(
        Workspace localWorkspace,
        AtomicReference<GUID> serverPendingChangeSignature);

    /**
     * Given a workspace name and owner, returns the server's copy of the
     * Workspace object.
     */
    public abstract Workspace queryServerWorkspace(String workspaceName, String ownerName);

    /**
     * Calls UpdateWorkspace without reconciling first
     */
    public abstract Workspace updateWorkspaceNoReconcile(
        String oldWorkspaceName,
        String ownerName,
        Workspace newWorkspace,
        SupportedFeatures supportedFeatures);

    protected Workspace getWorkspace(final String workspaceName, final String ownerName) {
        Workspace workspace = null;

        if (!StringUtil.isNullOrEmpty(workspaceName) && !StringUtil.isNullOrEmpty(ownerName)) {
            final VersionControlClient client = getVersionControlClient();

            if (!ownerName.equals(VersionControlConstants.AUTHENTICATED_USER)) {
                // Try the runtime workspace cache with the DOMAIN \ user key
                // string.
                workspace = runtimeWorkspaceCache.tryGetWorkspace(workspaceName, ownerName);
            }

            if (null == workspace) {
                // The workspace was not in the runtime workspace cache. Try and
                // get it from the cache file.
                WorkspaceInfo info = null;
                final Workstation workstation =
                    Workstation.getCurrent(client.getConnection().getPersistenceStoreProvider());

                if (!ownerName.equals(VersionControlConstants.AUTHENTICATED_USER)) {
                    info = workstation.getLocalWorkspaceInfo(client.getServerGUID(), workspaceName, ownerName);
                } else {
                    final WorkspaceInfo[] matching = workstation.queryLocalWorkspaceInfo(client, workspaceName, null);

                    if (matching.length == 1) {
                        info = matching[0];
                    }
                }

                if (info != null) {
                    workspace = runtimeWorkspaceCache.getWorkspace(info);
                }
            }
        }

        return workspace;
    }

    protected Workspace getServerWorkspace(final String workspaceName, final String ownerName) {
        final Workspace workspace = getWorkspace(workspaceName, ownerName);

        if (null != workspace && WorkspaceLocation.SERVER == workspace.getLocation() && workspace.isLocal()) {
            return workspace;
        }

        return null;
    }

    private void playbackQueuedEdits(final String workspaceName, final String ownerName) {
        playbackQueuedEdits(workspaceName, ownerName, null);
    }

    private void playbackQueuedEdits(
        final String workspaceName,
        final String ownerName,
        final ChangeRequest[] changeRequests) {
        final Workspace workspace = getServerWorkspace(workspaceName, ownerName);

        // We require the Tfs2010 web service level because the
        // GetWorkspaceDirectory call needs to know the SecurityToken of the
        // workspace in order to calculate the directory for the queued edits
        // table.
        final Boolean playingBack = playingBackQueuedEdits.get();

        if (null != workspace
            && getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue()
            && null != workspace.getSecurityToken()
            && null != playingBack
            && !playingBack.booleanValue()) {
            playingBackQueuedEdits.set(true);

            try {
                PersistenceStoreProvider provider;
                provider = workspace.getClient().getConnection().getPersistenceStoreProvider();

                final String path = LocalPath.combine(
                    workspace.getWorkspaceDirectory(Workstation.getConfigurationDirectory(provider).getAbsolutePath()),
                    "queuededits"); //$NON-NLS-1$

                final File slotOne = new File(LocalMetadataTable.getSlotOnePath(path));
                final File slotTwo = new File(LocalMetadataTable.getSlotTwoPath(path));

                if (!slotOne.exists() && !slotTwo.exists()) {
                    // No work to do.
                    return;
                }

                try {
                    QueuedEditsTable qe = new QueuedEditsTable(path);
                    final Set<String> queuedEdits = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

                    for (final String queuedEdit : qe.getQueuedEdits()) {
                        queuedEdits.add(queuedEdit);
                    }

                    qe.close();

                    // Filter out the provided change requests.
                    if (null != changeRequests) {
                        for (final ChangeRequest request : changeRequests) {
                            // Of course, this might not work if the item we
                            // have is a local item and a server item is
                            // provided on the ChangeRequest. In that case we
                            // could double-request an edit.
                            if (null != request.getItemSpec() && null != request.getItemSpec().getItem()) {
                                queuedEdits.remove(request.getItemSpec().getItem());
                            }
                        }
                    }

                    if (queuedEdits.size() > 0) {
                        boolean removeQueuedEdits = false;
                        final String[] queuedEditStrings = queuedEdits.toArray(new String[queuedEdits.size()]);

                        try {
                            workspace.pendEdit(
                                queuedEditStrings,
                                RecursionType.NONE,
                                LockLevel.UNCHANGED,
                                null,
                                GetOptions.NONE,
                                PendChangesOptions.FORCE_CHECK_OUT_LOCAL_VERSION);

                            removeQueuedEdits = true;
                        }
                        // TODO: TEE does not have this exception or an
                        // equivalent.
                        // catch (TeamFoundationServiceUnavailableException ex)
                        // {
                        // TeamFoundationTrace.TraceException(ex);
                        // }
                        catch (final Exception ex) {
                            client.getEventEngine().fireNonFatalError(
                                new NonFatalErrorEvent(EventSource.newFromHere(), workspace, ex));
                            removeQueuedEdits = true;
                        }

                        if (removeQueuedEdits) {
                            qe = new QueuedEditsTable(path);
                            qe.RemoveQueuedEdits(queuedEdits);
                            qe.close();
                        }
                    }
                } catch (final Exception e) {
                    // TODO: NYI
                }
            } finally {
                playingBackQueuedEdits.set(false);
            }
        }
    }
}
