// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.clients.security.AccessControlEntry;
import com.microsoft.tfs.core.clients.security.AccessControlEntryDetails;
import com.microsoft.tfs.core.clients.security.AccessControlListDetails;
import com.microsoft.tfs.core.clients.versioncontrol.AutoResolveOptions;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistoryTreeItem;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.CheckinFlags;
import com.microsoft.tfs.core.clients.versioncontrol.FailureCodes;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.ItemProperties;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.PropertiesMergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.ReconcilePendingChangesStatus;
import com.microsoft.tfs.core.clients.versioncontrol.ResolveErrorOptions;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.UnshelveResult;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissionProfile;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspacePermissions;
import com.microsoft.tfs.core.clients.versioncontrol.Workstation;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictCategory;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescriptionFactory;
import com.microsoft.tfs.core.clients.versioncontrol.engines.MergeEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.CheckinEngine;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetOperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergeOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergeOperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendOperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ResolveConflictCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ResolveConflictStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ResolveConflictsCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ResolveConflictsStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.RollbackOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.RollbackOperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ShelveEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndoOperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndoOperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UnshelveShelvesetCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UnshelveShelvesetStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ActionDeniedBySubscriberException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.AutoMergeDisallowedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ChangeRequestValidationException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.GatedCheckinException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotMappedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MappingConflictException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ShelveException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.UnshelveException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.WorkspaceDeletedException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.WritableConflictOnSourcePathListener;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalDataAccessLayer;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.OfflineCacheData;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalItemExclusionEvaluator;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.WorkspaceWatcher;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.internal.FileSystemWalker;
import com.microsoft.tfs.core.clients.versioncontrol.path.internal.FileSystemWalker.FileSystemVisitor;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.CheckinNotificationInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.CheckinNotificationWorkItemInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.SavedCheckin;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkItemCheckedInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.RuntimeWorkspaceCache;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptorComparer;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemActions;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.pendingcheckin.CheckinConflict;
import com.microsoft.tfs.core.pendingcheckin.CheckinConflictContainer;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationOptions;
import com.microsoft.tfs.core.pendingcheckin.CheckinEvaluationResult;
import com.microsoft.tfs.core.pendingcheckin.CheckinNoteFailure;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinNotes;
import com.microsoft.tfs.core.pendingcheckin.StandardPendingCheckin;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.FileEncodingDetector;
import com.microsoft.tfs.core.util.ServerURIComparator;
import com.microsoft.tfs.core.util.UserNameUtil;
import com.microsoft.tfs.core.util.notifications.Notification;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;
import com.microsoft.tfs.util.NewlineUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

import ms.tfs.versioncontrol.clientservices._03._BranchRelative;
import ms.tfs.versioncontrol.clientservices._03._CheckinNotificationInfo;
import ms.tfs.versioncontrol.clientservices._03._WorkingFolder;
import ms.tfs.versioncontrol.clientservices._03._Workspace;

/**
 * <p>
 * A {@link Workspace} is a named set of user-configured working folders. This
 * class provides methods to make changes to that configuration as well as to
 * perform source control tasks that are workspace-centric (check out, check-in,
 * get latest/specific versions, etc.).
 * </p>
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public final class Workspace extends WebServiceObjectWrapper implements Comparable<Workspace> {
    /**
     * The path to the resource that contains the header text written at the top
     * of new .tfignore files.
     */
    private static final String TFIGNORE_HEADER_RESOURCE = "com/microsoft/tfs/core/tfignoreheader.txt"; //$NON-NLS-1$
    private static final String BUILD_CHECKIN_SUBSCRIBER =
        "Microsoft.TeamFoundation.Build.Server.BuildCheckinNotificationSubscriber"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(Workspace.class);

    /**
     * The client this workspace will use for all VC operations.
     */
    private final VersionControlClient client;

    /**
     * Cached local workspace data.
     */
    private final OfflineCacheData offlineCache = new OfflineCacheData();

    /*
     * To avoid the need for exclusive locking in Workspace, most fields are
     * "volatile" and the objects assigned to them are not updated directly.
     * Instead, those objects are treated as though they are immutable (even
     * though the implementations may not be) and new instances are assigned to
     * the fields instead of updates.
     *
     * This strategy extends to updating the wrapped web service object (see
     * updateFromWorkspace()).
     */

    /**
     * Contains the permissions to use for this {@link Workspace}. The wrapped
     * web service object does not contain all the profile information, so we
     * store the full object here.
     * <p>
     * To ensure visibility across threads, don't modify this object. Instead,
     * assign a new object to the field.
     */
    private volatile WorkspacePermissionProfile permissionsProfile = null;

    /**
     * A workspace watcher used to detect changes for local workspaces.
     * <p>
     * To ensure visibility across threads, don't modify this object. Instead,
     * assign a new object to the field.
     */
    private volatile WorkspaceWatcher workspaceWatcher;

    /**
     * Set to true if the workspace has been deleted.
     */
    private volatile boolean deleted;

    /**
     * Set to true by {@link #invalidate()} to cause the next
     * {@link #refreshIfNeeded()} to retrieve data from the server.
     */
    private volatile boolean uncachedPropertiesStale;

    /**
     * Set to true by {@link #invalidate()} and {@link #invalidateMappings()} to
     * cause the next {@link #refreshIfNeeded()} to retrieve data from the
     * server.
     */
    private volatile boolean workingFoldersStale;

    /**
     * Constructs a {@link Workspace} that wraps a web service object. This
     * constructor is for internal use only. Do not use it to create new TFS
     * workspaces, use the createWorkspace methods on
     * {@link VersionControlClient} instead.
     *
     * @param workspace
     *        a {@link _Workspace} object returned from the server (must not be
     *        <code>null</code>)
     * @param client
     *        a {@link VersionControlClient} instace this {@link Workspace} can
     *        use (must not be <code>null</code>)
     */
    /*
     * NOTE:
     *
     * Do not chain other constructors to this one unless they're OK with the
     * behavior that overwrites the display name! This isn't correct for most
     * cases.
     */
    public Workspace(final _Workspace workspace, final VersionControlClient client) {
        super(workspace);

        this.client = client;

        final String displayName = workspace.getOwnerdisp();
        if (displayName == null || displayName.length() == 0) {
            workspace.setOwnerdisp(workspace.getOwner());
        }
    }

    /**
     * Copy constructor for {@link #updateFromWorkspace(Workspace)}.
     *
     * @param workspace
     *        a {@link Workspace} to clone
     */
    private Workspace(final Workspace workspace) {
        super(workspace.getWebServiceObject());

        this.client = workspace.client;
        this.deleted = workspace.deleted;

        this.permissionsProfile = workspace.permissionsProfile == null ? null : new WorkspacePermissionProfile(
            workspace.permissionsProfile.getName(),
            workspace.permissionsProfile.getAccessControlEntries());

        this.uncachedPropertiesStale = workspace.uncachedPropertiesStale;
        this.workingFoldersStale = workspace.workingFoldersStale;
    }

    /**
     * Constructs a Workspace.
     * <p>
     * This overload should only be used if this object is going to be sent to
     * the server because it doesn't take in the ownerDisplayName or
     * ownerAliases.
     */
    public Workspace(
        final VersionControlClient client,
        final String name,
        final String owner,
        final String comment,
        final WorkingFolder[] workingFolders,
        final String computer,
        final WorkspaceLocation location) {
        this(
            client,
            name,
            owner,
            null,
            null,
            comment,
            workingFolders,
            computer,
            location,
            WorkspacePermissions.NONE_OR_NOT_SUPPORTED);
    }

    public Workspace(
        final VersionControlClient client,
        final String name,
        final String owner,
        final String ownerDisplayName,
        final String[] ownerAliases,
        final String comment,
        final WorkingFolder[] workingFolders,
        final String computer,
        final WorkspaceLocation location,
        final WorkspacePermissions effectivePermissions) {
        this(
            client,
            name,
            owner,
            ownerDisplayName,
            ownerAliases,
            comment,
            null,
            workingFolders,
            computer,
            location,
            effectivePermissions,
            null,
            WorkspaceOptions.NONE);
    }

    /**
     * Constructs a {@link Workspace}.
     *
     * @param client
     *        a {@link VersionControlClient} instace this {@link Workspace} can
     *        use (must not be <code>null</code>)
     * @param name
     *        the name of this workspace (must not be <code>null</code> or
     *        empty)
     * @param owner
     *        the TFS user who owns this workspace (must include domain) (may be
     *        <code>null</code> or empty only if the {@link Workspace} will be
     *        sent to the server)
     * @param ownerDisplayName
     *        the display name of the TFS user who owns this workspace (may be
     *        <code>null</code> or empty only if the {@link Workspace} will be
     *        sent to the server)
     * @param ownerAliases
     *        the aliases of the TFS user who owns this workspace (may be
     *        <code>null</code> only if the {@link Workspace} will be sent to
     *        the server)
     * @param comment
     *        an optional comment (may be <code>null</code>)
     * @param securityToken
     *        the user's security identifier (may be <code>null</code>)
     * @param workingFolders
     *        working folder mappings for this workspace (may be
     *        <code>null</code>)
     * @param computer
     *        the computer where this workspace exists (must not be
     *        <code>null</code> or empty)
     * @param location
     *        where the workspace data is stored (must not be <code>null</code>)
     * @param effectivePermissions
     *        the permissions for this workspace (must not be <code>null</code>)
     * @param permissionProfile
     *        the permission profile to use for this workspace (may be null)
     * @param options
     *        the workspace options (must not be <code>null</code>)
     */
    public Workspace(
        final VersionControlClient client,
        final String name,
        final String owner,
        final String ownerDisplayName,
        final String[] ownerAliases,
        final String comment,
        final String securityToken,
        final WorkingFolder[] workingFolders,
        final String computer,
        final WorkspaceLocation location,
        final WorkspacePermissions effectivePermissions,
        final WorkspacePermissionProfile permissionProfile,
        final WorkspaceOptions options) {
        // Don't call this() (see the other constructor note)
        super(
            new _Workspace(
                computer,
                WorkspaceLocation.LOCAL.equals(location),
                name,
                owner,
                ownerDisplayName,
                owner,
                effectivePermissions.toIntFlags(),
                securityToken,
                null,
                null,
                comment,
                workingFolders != null ? (_WorkingFolder[]) WrapperUtils.unwrap(_WorkingFolder.class, workingFolders)
                    : new _WorkingFolder[0],
                ownerAliases,
                Calendar.getInstance(),
                options.toIntFlags()));

        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        Check.notNullOrEmpty(owner, "owner"); //$NON-NLS-1$
        Check.notNullOrEmpty(computer, "computer"); //$NON-NLS-1$
        Check.notNull(location, "location"); //$NON-NLS-1$

        this.client = client;
        this.permissionsProfile = permissionProfile;
    }

    /**
     * Gets the disk scanner used to detect changes in local workspaces.
     */
    public WorkspaceWatcher getWorkspaceWatcher() {
        if (workspaceWatcher == null) {
            workspaceWatcher = new WorkspaceWatcher(this);
        }

        return workspaceWatcher;
    }

    /**
     * Get the cached local workspace data.
     */
    public OfflineCacheData getOfflineCacheData() {
        return offlineCache;
    }

    /**
     * The full path to the metadata directory for this workspace (local
     * workspaces only). The path is in the ProgramData directory.
     */
    public String getLocalMetadataDirectory() {
        Check.isTrue(getLocation() == WorkspaceLocation.LOCAL, "getLocation() == WorkspaceLocation.LOCAL"); //$NON-NLS-1$

        final File dir =
            Workstation.getOfflineMetadataFileRoot(getClient().getConnection().getPersistenceStoreProvider());
        return getWorkspaceDirectory(dir.getAbsolutePath());
    }

    /**
     * Helper method which returns a subdirectory of the provided path. The
     * subdirectory is unique to this workspace and contains the TPC GUID, the
     * workspace name, and the workspace owner's TFID in it.
     *
     *
     * @param basePath
     *        Base path to form the path from
     * @return Workspace-unique directory
     */
    public String getWorkspaceDirectory(final String basePath) {
        final String serverBase = LocalPath.combine(basePath, client.getServerGUID().getGUIDString());

        final String workspaceBase =
            LocalPath.combine(serverBase, FileHelpers.removeInvalidNTFSFileNameCharacters(getSecurityToken()));

        return workspaceBase;
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Workspace getWebServiceObject() {
        return (_Workspace) webServiceObject;
    }

    /**
     * Gets a name for the current server (and project collection) that the
     * workspace is connected to that is suitable for display to the end-user.
     *
     * @return the server name in a user displayable form.
     */
    public String getServerName() {
        return client.getConnection().getName();
    }

    /**
     * @return the Team Foundation Server URI
     *
     * @see TFSConnection#getBaseURI()
     */
    public URI getServerURI() {
        return client.getConnection().getBaseURI();
    }

    /**
     * @return the Team Foundation Server project collection GUID
     */
    public GUID getServerGUID() {
        return client.getServerGUID();
    }

    /**
     * Gets the source code control client instance used by this workspace,
     * which will always describe a connection to the server in which this
     * workspace resides.
     *
     * @return the client used by this workspace.
     */
    public VersionControlClient getClient() {
        return client;
    }

    /**
     * Used by the {@link VersionControlClient} to notify the workspace object
     * that its mappings have potentially changed. This may happen when the file
     * on disk is changed, a pending change is made or undone that renames a
     * mapping. There may be other operations that invalidate the cache as well.
     */
    public void invalidate() {
        uncachedPropertiesStale = true;
        workingFoldersStale = true;
        permissionsProfile = null;
    }

    /**
     * Invalidates the cached copy of the workspace mappings held by this
     * Workspace object and causes the next access to them to re-fetch them from
     * the server or local data access layer
     */
    public void invalidateMappings() {
        workingFoldersStale = true;
    }

    /**
     * <p>
     * Gets extended information about items.
     * </p>
     * <p>
     * Uses {@link GetOptions#NONE} for the query, so no download information,
     * branch history, or source renames are returned. Use
     * {@link VersionControlClient#getExtendedItems(String, String, ItemSpec[], DeletedState, ItemType, GetItemsOptions)}
     * for full control over results.
     * </p>
     *
     * @param itemPath
     *        the server or local path to get extended information for. (must
     *        not be <code>null</code> or empty)
     * @param deletedState
     *        the deleted state of items you want to list (must not be
     *        <code>null</code>)
     * @param itemType
     *        the types of items you want to list (must not be <code>null</code>
     *        )
     * @return an array of {@link ExtendedItem}
     */
    public ExtendedItem[] getExtendedItems(
        final String itemPath,
        final DeletedState deletedState,
        final ItemType itemType) {
        return getExtendedItems(new ItemSpec[] {
            new ItemSpec(itemPath, RecursionType.NONE)
        }, deletedState, itemType)[0];
    }

    /**
     * <p>
     * Gets extended information about items.
     * </p>
     * <p>
     * Uses {@link GetOptions#NONE} for the query, so no download information,
     * branch history, or source renames are returned. Use
     * {@link #getExtendedItems(ItemSpec[], DeletedState, ItemType, GetItemsOptions)}
     * for full control over results.
     * </p>
     *
     * @equivalence getExtendedItems(itemSpecs, deletedState, itemType,
     *              GetItemsOptions.NONE)
     */
    public ExtendedItem[][] getExtendedItems(
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType) {
        return getExtendedItems(itemSpecs, deletedState, itemType, GetItemsOptions.NONE);
    }

    /**
     * @equivalence getExtendedItems(itemSpecs, deletedState, itemType, options,
     *              null)
     */
    public ExtendedItem[][] getExtendedItems(
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options) {
        return getExtendedItems(itemSpecs, deletedState, itemType, options, null);
    }

    /**
     * <p>
     * Gets extended information about items, with full results control.
     * </p>
     *
     * @param itemSpecs
     *        instances of {@link ItemSpec} that describe the item sets you want
     *        returned. One {@link ItemSet} will be returned for each
     *        {@link ItemSpec}. (must not be <code>null</code> or empty)
     * @param deletedState
     *        the deleted state of items you want to list (must not be
     *        <code>null</code>)
     * @param itemType
     *        the types of items you want to list (must not be <code>null</code>
     *        )
     * @param options
     *        the {@link GetItemsOptions} which control the returned results
     *        (must not be <code>null</code>)
     * @param itemPropertyFilters
     *        a list of properties to query (may be <code>null</code>)
     * @return an array of {@link ExtendedItem} arrays, each outer array
     *         representing one given {@link ItemSpec}, and each inner array
     *         representing the matches found for those {@link ItemSpec}s
     *         (should be only one object in these inner arrays because
     *         recursion is not an option). Inner arrays may be empty but are
     *         never null.
     */
    public ExtendedItem[][] getExtendedItems(
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final GetItemsOptions options,
        final String[] itemPropertyFilters) {
        return client.getExtendedItems(
            getName(),
            getOwnerName(),
            itemSpecs,
            deletedState,
            itemType,
            options,
            itemPropertyFilters);
    }

    /**
     * For each provided ItemSpec, returns a corresponding WorkspaceItemSet
     * containing data about items in the workspace.
     */
    public WorkspaceItemSet[] getItems(
        final ItemSpec[] itemSpecs,
        final DeletedState deletedState,
        final ItemType itemType,
        final boolean generateDownloadUrls,
        final GetItemsOptions getItemsOptions) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        return client.getWebServiceLayer().queryWorkspaceItems(
            getName(),
            getOwnerName(),
            itemSpecs,
            deletedState,
            itemType,
            generateDownloadUrls,
            getItemsOptions.toIntFlags());
    }

    /**
     * Notifies the server that we've discovered a conflict with our local
     * files.
     */
    public void addConflict(
        final ConflictType conflictType,
        final int itemID,
        final int versionServer,
        final int pendingChangeID,
        final String sourceLocalItem,
        final String targetLocalItem,
        final OperationStatus conflictReason) {
        client.getWebServiceLayer().addConflict(
            getName(),
            getOwnerName(),
            conflictType,
            itemID,
            versionServer,
            pendingChangeID,
            sourceLocalItem,
            targetLocalItem,
            conflictReason,
            client.mergeWithDefaultItemPropertyFilters(null));
    }

    /**
     * Checks for required checkin notes and evaluates the given changes against
     * defined checkin policies before they are sent to the server for check-in.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param options
     *        flags that affect which things are evaluated (must not be
     *        <code>null</code>)
     * @param allChanges
     *        all pending changes for this workspace (required by checkin
     *        policies). (must not be <code>null</code> or empty)
     * @param changesToCheckin
     *        the changes that are "checked" in the user interface, or are
     *        otherwise selected for checkin (must not be <code>null</code> or
     *        empty).
     * @param comment
     *        a user-supplied checkin comment (must not be <code>null</code>)
     * @param checkinNotes
     *        the user's checkin notes (must not be <code>null</code>)
     * @param workItemChanges
     *        work items that will be updated for this checkin (must not be
     *        <code>null</code>)
     * @param policyEvaluator
     *        a {@link PolicyEvaluator} object configured with a
     *        {@link PolicyLoader} to use for this evaluation (must not be
     *        <code>null</code>)
     * @param policyContext
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     * @return a result object containing the failures for each category
     *         specified in the given options.
     * @throws PolicyEvaluationCancelledException
     *         if the user cancelled check-in policy evaluation.
     */
    public CheckinEvaluationResult evaluateCheckIn(
        final CheckinEvaluationOptions options,
        final PendingChange[] allChanges,
        final PendingChange[] changesToCheckin,
        final String comment,
        final CheckinNote checkinNotes,
        final WorkItemCheckinInfo[] workItemChanges,
        final PolicyEvaluator policyEvaluator,
        final PolicyContext policyContext) throws PolicyEvaluationCancelledException {
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNullOrEmpty(allChanges, "allChanges"); //$NON-NLS-1$
        Check.notNullOrEmpty(changesToCheckin, "changesToCheckin"); //$NON-NLS-1$
        Check.notNull(checkinNotes, "checkinNote"); //$NON-NLS-1$
        Check.notNull(workItemChanges, "workItemChanges"); //$NON-NLS-1$
        Check.notNull(policyEvaluator, "policyEvaluator"); //$NON-NLS-1$
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        return evaluateCheckIn(
            options,
            new StandardPendingCheckin(
                this,
                allChanges,
                changesToCheckin,
                comment,
                checkinNotes,
                workItemChanges,
                policyEvaluator),
            policyContext);
    }

    /**
     * Checks for required checkin notes and evaluates the given changes against
     * defined checkin policies before they are sent to the server for check-in.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param options
     *        flags that affect which things are evaluated (must not be
     *        <code>null</code>)
     * @param pendingCheckin
     *        the pending checkin data to validate (must not be
     *        <code>null</code>)
     * @param policyContext
     *        contextual settings that may include information about the user
     *        interface, etc. (must not be <code>null</code>)
     * @return a result object containing the failures for each category
     *         specified in the given options.
     * @throws PolicyEvaluationCancelledException
     *         if the user cancelled check-in policy evaluation.
     */
    public CheckinEvaluationResult evaluateCheckIn(
        final CheckinEvaluationOptions options,
        final PendingCheckin pendingCheckin,
        final PolicyContext policyContext) throws PolicyEvaluationCancelledException {
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        final TaskMonitor tm = TaskMonitorService.getTaskMonitor();

        /*
         * Checkin notes.
         */

        if (tm.isCanceled()) {
            throw new CanceledException();
        }

        CheckinNoteFailure[] noteFailures = new CheckinNoteFailure[0];
        if (options.containsAny(CheckinEvaluationOptions.NOTES)) {
            log.debug("Evaluating checkin notes"); //$NON-NLS-1$

            final PendingCheckinNotes notes = pendingCheckin.getCheckinNotes();

            noteFailures = notes.evaluate();

            log.debug("Checking for notes that don't match field definitions"); //$NON-NLS-1$

            /*
             * Make sure that each of the checkin note fields supplied is
             * actually defined in the server, otherwise add it as a failure.
             */
            final List<CheckinNoteFailure> unknownNotes = new ArrayList<CheckinNoteFailure>();
            if (notes.getCheckinNotes() != null && notes.getCheckinNotes().getValues() != null) {
                for (int i = 0; i < notes.getCheckinNotes().getValues().length; i++) {
                    final CheckinNoteFieldValue value = notes.getCheckinNotes().getValues()[i];

                    boolean noteNameDefinied = false;
                    for (int j = 0; j < notes.getFieldDefinitions().length; j++) {
                        final CheckinNoteFieldDefinition definition = notes.getFieldDefinitions()[j];

                        if (value.getName().equalsIgnoreCase(definition.getName())) {
                            noteNameDefinied = true;
                            break;
                        }
                    }

                    if (noteNameDefinied == false) {
                        /*
                         * The note wasn't defined. Create a fake definition for
                         * it for consistency in reporting failures.
                         */
                        final CheckinNoteFieldDefinition fakeDefinition =
                            new CheckinNoteFieldDefinition(value.getName(), false, 0);
                        unknownNotes.add(
                            new CheckinNoteFailure(
                                fakeDefinition,
                                MessageFormat.format(
                                    Messages.getString("Workspace.TheFieldDoesNotExistsFormat"), //$NON-NLS-1$
                                    value.getName())));
                    }
                }
            }

            /*
             * Add any unknown checkin notes to the failures.
             */
            if (unknownNotes.size() > 0) {
                /*
                 * Combine any evaluation failures into the unknown list, then
                 * make it the canonical list.
                 */
                unknownNotes.addAll(Arrays.asList(noteFailures));
                noteFailures = unknownNotes.toArray(new CheckinNoteFailure[unknownNotes.size()]);
            }
        }

        /*
         * Checkin policies.
         */

        if (tm.isCanceled()) {
            throw new CanceledException();
        }

        PolicyFailure[] policyFailures = new PolicyFailure[0];
        PolicyEvaluatorState evaluatorState = null;
        Exception policyException = null;
        if (options.containsAny(CheckinEvaluationOptions.POLICIES)) {
            log.debug("Evaluating check-in policies"); //$NON-NLS-1$

            try {
                policyFailures = pendingCheckin.getCheckinPolicies().evaluate(policyContext);
            } catch (final PolicyEvaluationCancelledException e) {
                client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));
                throw e;
            } catch (final Exception e) {
                // Store it for the result.
                policyException = e;
            }

            evaluatorState = pendingCheckin.getCheckinPolicies().getPolicyEvaluatorState();

            log.debug(MessageFormat.format("Final state of evaluator was: {0}", evaluatorState.toString())); //$NON-NLS-1$
            log.debug(MessageFormat.format(
                "Policy exception was: {0}", //$NON-NLS-1$
                ((policyException != null) ? policyException.getMessage() : "<null>"))); //$NON-NLS-1$
            log.debug(MessageFormat.format(
                "Failures count: {0}", //$NON-NLS-1$
                ((policyFailures != null) ? Integer.toString(policyFailures.length) : "<null>"))); //$NON-NLS-1$
        }

        /*
         * Conflicts.
         */

        if (tm.isCanceled()) {
            throw new CanceledException();
        }

        CheckinConflict[] conflicts = new CheckinConflict[0];
        if (options.containsAny(CheckinEvaluationOptions.CONFLICTS)) {
            final Failure[] failures = checkPendingChanges(
                PendingChange.toServerItems(pendingCheckin.getPendingChanges().getCheckedPendingChanges()));
            conflicts = convertFailuresToConflicts(failures).getConflicts();
        }

        if (tm.isCanceled()) {
            throw new CanceledException();
        }

        return new CheckinEvaluationResult(conflicts, noteFailures, policyFailures, evaluatorState, policyException);
    }

    /**
     * @equivalence checkIn(changes, comment, null, null, null)
     */
    public int checkIn(final PendingChange[] changes, final String comment) throws CheckinException {
        return checkIn(changes, comment, null, null, null);
    }

    /**
     * @equivalence checkIn(changes, null, comment, checkinNote,
     *              associatedWorkItems, null)
     */
    public int checkIn(
        final PendingChange[] changes,
        final String comment,
        final CheckinNote checkinNote,
        final WorkItemCheckinInfo[] associatedWorkItems,
        final PolicyOverrideInfo policyOverrideInfo) throws CheckinException {
        return checkIn(changes, null, null, comment, checkinNote, associatedWorkItems, null);
    }

    /**
     * @equivalence checkIn(changes, author, authorDisplayName, comment,
     *              checkinNote, associatedWorkItems, policyOverrideInfo,
     *              CheckinFlags.NONE)
     */
    public int checkIn(
        final PendingChange[] changes,
        final String author,
        final String authorDisplayName,
        final String comment,
        final CheckinNote checkinNote,
        final WorkItemCheckinInfo[] associatedWorkItems,
        final PolicyOverrideInfo policyOverrideInfo) throws CheckinException {
        return checkIn(
            changes,
            author,
            authorDisplayName,
            comment,
            checkinNote,
            associatedWorkItems,
            policyOverrideInfo,
            CheckinFlags.NONE);
    }

    /**
     * @equivalence checkIn(changes, null, null, author, authorDisplayName,
     *              comment, checkinNote, associatedWorkItems,
     *              policyOverrideInfo, flags)
     */
    public int checkIn(
        final PendingChange[] changes,
        final String author,
        final String authorDisplayName,
        final String comment,
        final CheckinNote checkinNote,
        final WorkItemCheckinInfo[] associatedWorkItems,
        final PolicyOverrideInfo policyOverrideInfo,
        final CheckinFlags flags) throws CheckinException {
        return checkIn(
            changes,
            null,
            null,
            author,
            authorDisplayName,
            comment,
            checkinNote,
            associatedWorkItems,
            policyOverrideInfo,
            flags);
    }

    /**
     * Checkin pending changes in this workspace.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param changes
     *        the changes to checkin (may be <code>null</code> to have the
     *        server check in all changes in this workspace)
     * @param committer
     *        if not <code>null</code>, the committer of this change. if
     *        <code>null</code>, the committer will be the authenticated user.
     * @param author
     *        if not <code>null</code>, the author of this change. if
     *        <code>null</code>, the author will be the authenticated user.
     * @param comment
     *        a text comment that will be associated with this checkin (can be
     *        <code>null</code>).
     * @param checkinNote
     *        {@link CheckinNote} object containing array of
     *        ACheckinNoteFieldValue objects. If <code>null</code>, no checkin
     *        notes are added to changeset. For a list of checkin note field
     *        names applicable to the items use the
     *        queryCheckinNoteFieldDefinitionsForServerPaths on the
     *        VersionControlClient.
     * @param associatedWorkItems
     *        work items to associate with the checkin. if <code>null</code>, no
     *        work items are associated with this checkin
     * @param policyOverrideInfo
     *        Optional information describing why checkin policies were
     *        overridden for this checkin. Pass null for a normal check-in
     *        (policies were not overridden).
     * @param flags
     *        {@link CheckinFlags} which control the checkin (must not be
     *        <code>null</code>)
     * @return the changeset number the server chooses for this changeset.
     * @throws CheckinException
     *         if conflicts caused the checkin to fail or if other errors
     *         occurred.
     * @throws ActionDeniedBySubscriberException
     *         if the check-in was denied by the server (because of a gated
     *         build definition, etc.).
     *
     * @see VersionControlClient#queryCheckinNoteFieldDefinitionsForServerPaths(String[])
     */
    public int checkIn(
        PendingChange[] changes,
        final String committer,
        final String committerDisplayName,
        String author,
        String authorDisplayName,
        final String comment,
        final CheckinNote checkinNote,
        WorkItemCheckinInfo[] associatedWorkItems,
        final PolicyOverrideInfo policyOverrideInfo,
        final CheckinFlags flags) throws CheckinException {
        Check.isTrue(
            changes == null || changes.length > 0,
            "changes must be null for server-side change selection or non-empty"); //$NON-NLS-1$
        Check.notNull(flags, "flags"); //$NON-NLS-1$

        /**
         * TFS 2010 behaves strangely with gated check-ins if we send null for
         * associated work items (a non-standard subcode comes back in the
         * ActionDeniedBySubscriberException. Always send at least an empty
         * list.
         */
        if (associatedWorkItems == null) {
            associatedWorkItems = new WorkItemCheckinInfo[0];
        }

        final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();

        /*
         * The total work for our progress monitor is set to 100, and subtasks
         * are allocated as percentages. For example, checkin for conflicts is
         * quick, so it takes only 2 percent. Uploading files usually takes
         * longer, so it's 80 percent. Make sure all the work done in this
         * method adds to 100.
         */
        monitor.begin("", 100); //$NON-NLS-1$

        /*
         * We sort the changes by server path so they appear in the correct
         * order when giving status information to the user.
         */
        String[] serverItems = null;
        if (changes != null) {
            changes = changes.clone();
            Arrays.sort(changes, new PendingChangeComparator(PendingChangeComparatorType.SERVER_ITEM));
            serverItems = PendingChange.toServerItems(changes);
        }

        // Lets us detect all abnormal exits (Throwable, Exception, Gated
        // checkin exception) for saved checkin reset
        boolean success = false;

        try {
            TaskMonitorService.pushTaskMonitor(monitor.newSubTaskMonitor(75));
            try {
                // Upload contents
                if (changes != null) {
                    final CheckinEngine ci = new CheckinEngine(client, this);

                    final long start = System.currentTimeMillis();
                    ci.uploadChanges(changes, false, getLocation() == WorkspaceLocation.LOCAL);
                    log.debug(MessageFormat.format(
                        "total time for upload of {0} was {1} ms", //$NON-NLS-1$
                        changes.length,
                        (System.currentTimeMillis() - start)));
                } else {
                    log.debug("null changes (server side change selection), skipped upload"); //$NON-NLS-1$
                }
            } finally {
                TaskMonitorService.popTaskMonitor(true);
            }

            if (author == null) {
                author = VersionControlConstants.AUTHENTICATED_USER;
            }
            if (authorDisplayName == null) {
                authorDisplayName = UserNameUtil.getCurrentUserName();
                final String domainName = UserNameUtil.getCurrentUserDomain();
                if (!StringUtil.isNullOrEmpty(domainName)) {
                    authorDisplayName = UserNameUtil.format(authorDisplayName, domainName);
                }
            }

            /*
             * Finally, create a Changeset and send it to the server to be
             * committed. It's important to pass "null" for the date so TFS 2010
             * and later do not require CheckinOther permissions (required to
             * set a specific date on a new changeset).
             */
            final Changeset changeset = new Changeset(
                null,
                comment,
                checkinNote,
                policyOverrideInfo,
                committer,
                committerDisplayName,
                null,
                -1,
                author,
                authorDisplayName,
                null);

            /*
             * Test one final time before the change set is fully committed.
             */
            if (monitor.isCanceled()) {
                // Caught in this method below.
                throw new CoreCancelException();
            }

            monitor.setCurrentWorkDescription(Messages.getString("Workspace.CheckinInNewChangeset")); //$NON-NLS-1$

            final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();
            final AtomicReference<Failure[]> conflicts = new AtomicReference<Failure[]>();
            final boolean noAutoResolve = flags.contains(CheckinFlags.NO_AUTO_RESOLVE);

            final CheckinResult result;
            try {
                /*
                 * If changes was null when this method was called, serverItems
                 * will be null here, which causes the server to check in all
                 * workspace changes.
                 */
                result = getClient().getWebServiceLayer().checkIn(
                    getName(),
                    getOwnerName(),
                    serverItems,
                    changeset,
                    makeCheckinNotificationInfo(associatedWorkItems),
                    flags,
                    null,
                    conflicts,
                    failures,
                    false,
                    0,
                    client.mergeWithDefaultItemPropertyFilters(null));
            } catch (final ActionDeniedBySubscriberException e) {
                if (e.getSubscriberType().equals(BUILD_CHECKIN_SUBSCRIBER) && e.getStatusCode() == 1) {
                    /*
                     * For ease of use, convert the
                     * ActionDeniedBySubscriberException into a stronger type,
                     * GatedCheckinException. This exception has helper
                     * properties and is typed in a way that customers expect.
                     * It is still an ActionDeniedBySubscriberException.
                     */
                    throw new GatedCheckinException(e);
                } else {
                    /*
                     * Some other subscriber has denied the decision point.
                     * Throw the ActionDeniedBySubscriberException verbatim.
                     */
                    throw e;
                }
            }

            monitor.worked(10);

            changeset.setChangesetID(result.getChangeset());

            // Report any failures.
            reportCheckinConflictsAndThrow(result, conflicts.get(), failures.get(), noAutoResolve);

            /*
             * When the SetFileTimeToCheckin workspace option is set, then the
             * full checkin manifest is returned to the client in the form of
             * GetOperations, even in a server workspace. (In a server
             * workspace, the local version updates are still performed by the
             * server at the end of the CheckIn call.) We use this manifest to
             * set the check-in date on each item in the changeset, even
             * implicitly included missing parents and affected items of
             * recursive changes.
             */
            final TaskMonitor setFileTimeMonitor = monitor.newSubTaskMonitor(5);
            try {
                if (getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)) {
                    final GetOperation[] updates = result.getLocalVersionUpdates();

                    if (updates != null && updates.length > 0) {
                        setFileTimeMonitor.begin(Messages.getString("Workspace.SettingFileTime"), updates.length); //$NON-NLS-1$

                        for (final GetOperation getOp : updates) {
                            if (ItemType.FILE == getOp.getItemType()
                                && null != getOp.getTargetLocalItem()
                                && new File(getOp.getTargetLocalItem()).exists()) {
                                setFileTimeMonitor.setCurrentWorkDescription(getOp.getTargetLocalItem());

                                try {
                                    final FileSystemAttributes attributes =
                                        FileSystemUtils.getInstance().getAttributes(getOp.getTargetLocalItem());
                                    boolean restoreReadOnly = false;

                                    /*
                                     * Temporarily remove the read-only flag so
                                     * we can modify the time (Windows requires
                                     * this).
                                     */
                                    if (attributes.isReadOnly()) {
                                        attributes.setReadOnly(false);
                                        FileSystemUtils.getInstance().setAttributes(
                                            getOp.getTargetLocalItem(),
                                            attributes);
                                        restoreReadOnly = true;
                                    }

                                    new File(getOp.getTargetLocalItem()).setLastModified(
                                        result.getCreationDate().getTimeInMillis());

                                    if (restoreReadOnly) {
                                        attributes.setReadOnly(true);
                                        FileSystemUtils.getInstance().setAttributes(
                                            getOp.getTargetLocalItem(),
                                            attributes);
                                    }
                                } catch (final Exception e) {
                                    client.getEventEngine().fireNonFatalError(
                                        new NonFatalErrorEvent(EventSource.newFromHere(), this, e));
                                }
                            }
                        }
                    }
                }
            } finally {
                setFileTimeMonitor.done();
            }

            /*
             * If this is a server workspace, set files read-only.
             */
            final TaskMonitor makeReadOnlyMonitor = monitor.newSubTaskMonitor(5);
            try {
                if (changes != null && getLocation() == WorkspaceLocation.SERVER) {
                    makeReadOnlyMonitor.begin(Messages.getString("Workspace.SettingReadOnly"), changes.length); //$NON-NLS-1$

                    for (final PendingChange change : changes) {
                        if (change.getChangeType().contains(ChangeType.EDIT)
                            && change.getLocalItem() != null
                            && new File(change.getLocalItem()).exists()) {
                            makeReadOnlyMonitor.setCurrentWorkDescription(change.getLocalItem());

                            try {
                                final FileSystemAttributes attributes =
                                    FileSystemUtils.getInstance().getAttributes(change.getLocalItem());
                                if (!attributes.isSymbolicLink() && !attributes.isDirectory()) {
                                    attributes.setReadOnly(true);
                                    FileSystemUtils.getInstance().setAttributes(change.getLocalItem(), attributes);
                                }
                            } catch (final Exception e) {
                                client.getEventEngine().fireNonFatalError(
                                    new NonFatalErrorEvent(EventSource.newFromHere(), this, e));
                            }
                        } else {
                            // Skipping this one.
                            makeReadOnlyMonitor.setCurrentWorkDescription(""); //$NON-NLS-1$
                        }

                        makeReadOnlyMonitor.worked(1);
                    }
                }
            } finally {
                makeReadOnlyMonitor.done();
            }

            monitor.setCurrentWorkDescription(Messages.getString("Workspace.NotifyingListeners")); //$NON-NLS-1$

            /*
             * Determine which pending changes were committed and which were
             * undone. Preserve the sorted order in the sublists.
             */

            PendingChange[] committedChangesArray = new PendingChange[0];
            PendingChange[] undoneChangesArray = new PendingChange[0];

            if (changes != null && changes.length > 0) {
                final Set<String> undoneServerItems = new TreeSet<String>(ServerPath.TOP_DOWN_COMPARATOR);

                for (final String undoneServerItem : result.getUndoneServerItems()) {
                    undoneServerItems.add(undoneServerItem);
                }

                final List<PendingChange> undonePendingChanges = new ArrayList<PendingChange>(undoneServerItems.size());
                final List<PendingChange> committedPendingChanges = new ArrayList<PendingChange>();

                for (final PendingChange change : changes) {
                    if (undoneServerItems.contains(change.getServerItem())) {
                        undonePendingChanges.add(change);
                    } else {
                        committedPendingChanges.add(change);
                    }
                }

                committedChangesArray =
                    committedPendingChanges.toArray(new PendingChange[committedPendingChanges.size()]);
                undoneChangesArray = undonePendingChanges.toArray(new PendingChange[undonePendingChanges.size()]);
            }

            // Notify the user that the checkin iCheckinEvents complete.
            client.getEventEngine().fireCheckin(
                new CheckinEvent(
                    EventSource.newFromHere(),
                    this,
                    result.getChangeset(),
                    committedChangesArray,
                    undoneChangesArray));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED);

            monitor.worked(1);

            final int cset = changeset.getChangesetID();

            TaskMonitorService.pushTaskMonitor(monitor.newSubTaskMonitor(4));
            try {
                /*
                 * Only update work items if we have a valid (non-0) changeset.
                 * Changeset 0 indicates all the pending changes were undone on
                 * the server.
                 */
                if (cset != 0) {
                    updateWorkItems(associatedWorkItems, cset, comment);
                }
            } finally {
                TaskMonitorService.popTaskMonitor(true);
            }

            // Remove any saved attempted checkin info.
            setLastSavedCheckin(buildEmptyLastSavedCheckin());

            success = true;
            return cset;
        } catch (final CanceledException e) {
            // Fire as non-fatal
            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    this,
                    new CoreCancelException(Messages.getString("Workspace.CheckinCancelled")))); //$NON-NLS-1$
            return 0;
        } catch (final CoreCancelException e) {
            // Convert to CanceledException and fire as non-fatal
            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    this,
                    new CanceledException(Messages.getString("Workspace.CheckinCancelled")))); //$NON-NLS-1$
            return 0;
        } finally {
            /*
             * If the checkin didn't succeed, save the info for the next
             * attempt. success will be false for expected things like gated
             * checkin and cancelation exceptions, and also for unexpected
             * exceptions.
             */
            if (!success) {
                updateLastSavedCheckin(comment, checkinNote, associatedWorkItems, policyOverrideInfo);
            }

            monitor.done();
        }
    }

    /**
     * Checks the given server items for conflicts before a checkin is
     * performed.
     *
     * @param serverItems
     *        the items to check for conflicts (must not be <code>null</code>)
     * @return the failures detected by the server, empty if there were none,
     *         never null
     */
    public Failure[] checkPendingChanges(final String[] serverItems) {
        Check.notNull(serverItems, "serverItems"); //$NON-NLS-1$

        return client.getWebServiceLayer().checkPendingChanges(getName(), getOwnerName(), serverItems);
    }

    /**
     * Converts the given failures to conflicts.
     *
     * @param failures
     *        the failures to convert (must not be <code>null</code>)
     * @return conflicts that were constructed from the given failures.
     */
    private CheckinConflictContainer convertFailuresToConflicts(final Failure[] failures) {
        Check.notNull(failures, "failures"); //$NON-NLS-1$

        final CheckinConflict[] ret = new CheckinConflict[failures.length];

        /*
         * Determine if any of the problems are resolvable, so we can throw the
         * exception with the correct resolvable information.
         */
        boolean anyResolvable = true;
        for (int i = 0; i < failures.length; i++) {
            boolean thisOneResolvable = true;
            final Failure failure = failures[i];

            if (failure.getCode().equals(VersionControlConstants.LOCAL_ITEM_OUT_OF_DATE_EXCEPTION) == false
                && failure.getCode().equals(VersionControlConstants.MERGE_CONFLICT_EXISTS_EXCEPTION) == false
                && failure.getCode().equals(VersionControlConstants.ITEM_EXISTS_EXCEPTION) == false
                && failure.getCode().equals(VersionControlConstants.ITEM_DELETED_EXCEPTION) == false
                && failure.getCode().equals(VersionControlConstants.LATEST_VERSION_DELETED_EXCEPTION) == false) {
                // This one is not resolvable.
                thisOneResolvable = false;
                anyResolvable = false;
            }

            ret[i] = new CheckinConflict(
                failure.getServerItem(),
                failure.getCode(),
                failure.getMessage(),
                thisOneResolvable);
        }

        return new CheckinConflictContainer(ret, anyResolvable);
    }

    /**
     * Takes failures from {@link #checkPendingChanges(String[])}, reports them
     * via events, and throws an exception at the end (if there were any
     * failures to report). If there are no failures, simply returns.
     *
     * @param failures
     *        the failures returned from {@link #checkPendingChanges(String[])}
     */
    private void reportCheckinConflictsAndThrow(
        final CheckinResult checkinResult,
        final Failure[] conflicts,
        final Failure[] failures,
        final boolean noAutoResolve) {
        boolean throwErrorOnFailedCheckin = false;

        if (failures != null && failures.length > 0) {
            /* Print out any warnings or failures. Report the failures. */
            for (final Failure failure : failures) {
                if (SeverityType.ERROR.equals(failure.getSeverity())) {
                    /*
                     * We want to throw NoFilesCheckedIn if there was an error.
                     */
                    throwErrorOnFailedCheckin = true;
                }

                client.getEventEngine().fireNonFatalError(
                    new NonFatalErrorEvent(EventSource.newFromHere(), this, failure));
            }
        }

        boolean isAnyResolvable = false;
        CheckinConflict[] checkinConflicts;

        /* Report the conflicts. */
        if (conflicts != null && conflicts.length > 0) {
            /* We want to throw NoFilesCheckedIn if there was a conflict. */
            throwErrorOnFailedCheckin = true;

            final CheckinConflictContainer checkinConflictContainer = convertFailuresToConflicts(conflicts);

            checkinConflicts = checkinConflictContainer.getConflicts();
            isAnyResolvable = checkinConflictContainer.isAnyResolvable();

            /*
             * Workspace is null if we are doing BranchCheckin or
             * CheckinShelveset and in both of these cases we don't want to auto
             * resolve conflicts. For other cases, try to auto resolve.
             */
            boolean allConflictsResolved = false;
            if (isAnyResolvable && !noAutoResolve) {
                /*
                 * Use the checkin conflicts that came back to get the list of
                 * paths we will use to call QueryConflicts.
                 */
                final List<String> conflictingPaths = new ArrayList<String>();

                for (final CheckinConflict conflict : checkinConflicts) {
                    if (conflict.isResolvable()) {
                        conflictingPaths.add(conflict.getServerItem());
                    }
                }

                /* Query the conflicts that pertain to this checkin. */
                final Conflict[] actualConflicts =
                    queryConflicts(conflictingPaths.toArray(new String[conflictingPaths.size()]), false);

                /* Resolve the conflicts that we can */
                final Conflict[] remainingActualConflicts =
                    client.autoResolveValidConflicts(this, actualConflicts, AutoResolveOptions.ALL_SILENT);
                allConflictsResolved = (remainingActualConflicts.length == 0);

                final Set<String> hashedRemainingConflicts = new TreeSet<String>(ServerPath.TOP_DOWN_COMPARATOR);
                for (final Conflict conflict : remainingActualConflicts) {
                    if (conflict.getYourServerItemSource() != null) {
                        hashedRemainingConflicts.add(conflict.getYourServerItemSource());
                    }
                    if (conflict.getYourServerItem() != null) {
                        hashedRemainingConflicts.add(conflict.getYourServerItem());
                    }
                }

                final List<CheckinConflict> remainingCheckinConflicts = new ArrayList<CheckinConflict>();
                for (final CheckinConflict checkinConflict : checkinConflicts) {
                    if (hashedRemainingConflicts.contains(checkinConflict.getServerItem())) {
                        remainingCheckinConflicts.add(checkinConflict);
                    }
                }

                checkinConflicts =
                    remainingCheckinConflicts.toArray(new CheckinConflict[remainingCheckinConflicts.size()]);
            }

            for (final CheckinConflict conflict : checkinConflicts) {
                client.getEventEngine().fireConflict(
                    new ConflictEvent(
                        EventSource.newFromHere(),
                        conflict.getServerItem(),
                        this,
                        conflict.getMessage(),
                        conflict.isResolvable()));
            }

            /* Throw an exception because of the conflicts. */
            if (isAnyResolvable) {
                final String errorMessage =
                    allConflictsResolved ? Messages.getString("Workspace.AutoResolvedAllReCheckin") //$NON-NLS-1$
                        : Messages.getString("Workspace.ResolveAndReCheckin"); //$NON-NLS-1$

                throw new CheckinException(checkinConflicts, isAnyResolvable, allConflictsResolved, errorMessage);
            }
        } else {
            checkinConflicts = new CheckinConflict[0];
        }

        /*
         * We don't want to throw the CheckinException if there was nothing
         * checked in but no errors or conflicts. An example of this is if all
         * of the changes were undone.
         */
        if (checkinResult.getChangeset() == 0 && throwErrorOnFailedCheckin) {
            throw new CheckinException(
                checkinConflicts,
                isAnyResolvable,
                false,
                Messages.getString("Workspace.NoFilesCheckedIn")); //$NON-NLS-1$
        }
    }

    private void updateWorkItems(final WorkItemCheckinInfo[] workItemInfos, final int changeSet, String comment) {
        if (workItemInfos == null) {
            return;
        }

        final WorkItemClient wic = (WorkItemClient) getClient().getConnection().getClient(WorkItemClient.class);

        final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();
        monitor.begin("", workItemInfos.length); //$NON-NLS-1$

        // Convert changeset comment to a single line
        comment = StringUtil.formatCommentForOneLine(comment);

        try {
            for (int i = 0; i < workItemInfos.length; i++) {
                final WorkItem workItem = workItemInfos[i].getWorkItem();
                final boolean resolve = CheckinWorkItemAction.RESOLVE == workItemInfos[i].getAction();

                monitor.setCurrentWorkDescription(
                    MessageFormat.format(
                        Messages.getString("Workspace.UpdatingWorkItemFormat"), //$NON-NLS-1$
                        Integer.toString(workItem.getFields().getID())));

                workItem.syncToLatest();
                workItem.open();

                String history = MessageFormat.format(
                    Messages.getString("Workspace.AssociatedWithChangesetFormat"), //$NON-NLS-1$
                    Integer.toString(changeSet));

                final RegisteredLinkType changesetLinkType =
                    wic.getRegisteredLinkTypes().get(RegisteredLinkTypeNames.CHANGESET);
                if (changesetLinkType != null) {
                    final ArtifactID artifactId = ArtifactIDFactory.newChangesetArtifactID(changeSet);
                    final ExternalLink link =
                        LinkFactory.newExternalLink(changesetLinkType, artifactId, comment, false);
                    workItem.getLinks().add(link);
                }

                if (resolve) {
                    final String nextState = workItem.getNextState(WorkItemActions.VS_CHECKIN);
                    if (nextState != null) {
                        workItem.getFields().getField(CoreFieldReferenceNames.STATE).setValue(nextState);
                    }
                    history = MessageFormat.format(
                        Messages.getString("Workspace.ResolvedWithChangesetFormat"), //$NON-NLS-1$
                        Integer.toString(changeSet));
                }

                workItem.getFields().getField(CoreFieldReferenceNames.HISTORY).setValue(history);

                try {
                    workItem.save();
                } catch (final UnableToSaveException e) {
                    log.warn("Unable to save work item", e); //$NON-NLS-1$
                    // TODO
                }

                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Creates a {@link _CheckinNotificationInfo} for checkin from an array of
     * {@link WorkItemCheckinInfo}s.
     *
     * @param workItemInfos
     *        the info items (must not be <code>null</code>)
     * @return a {@link _CheckinNotificationInfo} containing the web service
     *         objects from the given {@link WorkItemCheckinInfo}s
     */
    private CheckinNotificationInfo makeCheckinNotificationInfo(final WorkItemCheckinInfo[] workItemInfos) {
        Check.notNull(workItemInfos, "workItemInfos"); //$NON-NLS-1$

        /*
         * Slightly more complicated than WrapperUtils.unwrap, because we need
         * to get the web service object through .getNotification().
         */

        final CheckinNotificationWorkItemInfo[] notificationInfos =
            new CheckinNotificationWorkItemInfo[workItemInfos.length];

        for (int i = 0; i < workItemInfos.length; i++) {
            notificationInfos[i] = workItemInfos[i].getNotification();
        }

        return new CheckinNotificationInfo(notificationInfos);
    }

    /**
     * <p>
     * A convenience method to create a new regular {@link WorkingFolder} from
     * the given server and local path.
     * </p>
     *
     * <p>
     * This method interprets the <code>serverPath</code> parameter in such a
     * way that depth-one mappings can be encoded in the server path. If the
     * file part of the server path is equal to
     * {@link WorkingFolder#DEPTH_ONE_STRING}, a depth-one mapping will be
     * returned.
     * </p>
     *
     * @param serverPath
     *        the server path to map (must not be <code>null</code>)
     * @param localPath
     *        the local path to map (must not be <code>null</code>)
     */
    public WorkingFolder getWorkingFolderFromPaths(String serverPath, final String localPath) {
        RecursionType recursionType = RecursionType.FULL;

        if (WorkingFolder.DEPTH_ONE_STRING.equals(ServerPath.getFileName(serverPath))) {
            serverPath = ServerPath.getParent(serverPath);
            recursionType = RecursionType.ONE_LEVEL;
        }

        return new WorkingFolder(serverPath, LocalPath.canonicalize(localPath), WorkingFolderType.MAP, recursionType);
    }

    /**
     * <p>
     * A convenience method to create a new regular (non-cloaked) working folder
     * mapping on this {@link Workspace}.
     * </p>
     *
     * <p>
     * This method interprets the <code>serverPath</code> parameter in such a
     * way that depth-one mappings can be encoded in the server path. If the
     * file part of the server path is equal to
     * {@link WorkingFolder#DEPTH_ONE_STRING}, a depth-one mapping will be
     * created.
     * </p>
     *
     * </p>
     * If a mapping already exists with the same local path or server path, an
     * exception is thrown.
     * </p>
     *
     * @param serverPath
     *        the server path to map (must not be <code>null</code>)
     * @param localPath
     *        the local path to map (must not be <code>null</code>)
     */
    public void map(final String serverPath, final String localPath) {
        createWorkingFolder(getWorkingFolderFromPaths(serverPath, localPath));
    }

    /**
     * <p>
     * A convenience method to create or change a new regular (non-cloaked)
     * working folder mapping on this {@link Workspace}.
     * </p>
     *
     * <p>
     * This method interprets the <code>serverPath</code> parameter in such a
     * way that depth-one mappings can be encoded in the server path. If the
     * file part of the server path is equal to
     * {@link WorkingFolder#DEPTH_ONE_STRING}, a depth-one mapping will be
     * created.
     * </p>
     *
     * </p>
     * If two different mappings already exist with the same local path or
     * server path, an exception is thrown.
     * </p>
     *
     * @param serverPath
     *        the server path to map (must not be <code>null</code>)
     * @param localPath
     *        the local path to map (must not be <code>null</code>)
     */
    public void addOrChangeMapping(final String serverPath, final String localPath) {
        createWorkingFolder(getWorkingFolderFromPaths(serverPath, localPath), true);
    }

    /**
     * Create a working folder mapping for this workspace. If a mapping already
     * exists with the same local path or server path, an exception is thrown.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param wf
     *        the working folder mapping to create.
     */
    public void createWorkingFolder(final WorkingFolder wf) {
        Check.notNull(wf, "wf"); //$NON-NLS-1$

        createWorkingFolder(wf, false);
    }

    /**
     * Create a working folder mapping for this workspace. This method allows
     * you to specify what happens when an existing mapping has the same local
     * path or server path.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param newWorkingFolder
     *        the working folder mapping to create.
     * @param overwriteExisting
     *        if true, any existing mapping with the same server path or local
     *        path is overwritten
     */
    public void createWorkingFolder(final WorkingFolder newWorkingFolder, final boolean overwriteExisting) {
        Check.notNull(newWorkingFolder, "newWorkingFolder"); //$NON-NLS-1$

        createWorkingFolders(new WorkingFolder[] {
            newWorkingFolder
        }, overwriteExisting);
    }

    /**
     * Create new working folder mappings for this workspace. If a mapping
     * already exists with the same local path or server path, an exception is
     * thrown.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param newWorkingFolders
     *        the working folder mappings to create.
     */
    public void createWorkingFolders(final WorkingFolder[] newWorkingFolders) {
        createWorkingFolders(newWorkingFolders, false);
    }

    /**
     * Create new working folder mappings for this workspace. This method allows
     * you to specify what happens when an existing mapping has the same local
     * path or server path.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param newWorkingFolders
     *        the working folder mappings to create.
     * @param overwriteExisting
     *        if true, any existing mapping with the same server path or local
     *        path is overwritten
     */
    public void createWorkingFolders(final WorkingFolder[] newWorkingFolders, final boolean overwriteExisting) {
        Check.notNullOrEmpty(newWorkingFolders, "newWorkingFolders"); //$NON-NLS-1$

        final WorkingFolder[] foldersArray = getFolders();
        final List<WorkingFolder> folders = new ArrayList<WorkingFolder>();
        if (foldersArray != null) {
            folders.addAll(Arrays.asList(foldersArray));
        }

        for (final WorkingFolder newWorkingFolder : newWorkingFolders) {
            // Check the proposed mapping to see if it's allowed, so we don't
            // have to wait for the server to enforce the rules later.
            for (final Iterator<WorkingFolder> i = folders.iterator(); i.hasNext();) {
                final WorkingFolder existingWorkingFolder = i.next();

                /*
                 * We need to check that no more than one existing mapping
                 * matches to the new one. This helps to prevent unintentionally
                 * destroying multiple existing mappings with a single command.
                 */
                int numRemoved = 0;

                /*
                 * VS comment: If we match on server item, remove
                 */
                if (ServerPath.equals(existingWorkingFolder.getServerItem(), newWorkingFolder.getServerItem())) {
                    if (overwriteExisting && numRemoved == 0) {
                        i.remove();
                        numRemoved++;
                        continue;
                    } else {
                        throw new VersionControlException(
                            MessageFormat.format(
                                Messages.getString("Workspace.NewMappingConflictsWithServerPathOfOtherMappingFormat"), //$NON-NLS-1$
                                newWorkingFolder.getServerItem(),
                                newWorkingFolder.getLocalItem(),
                                existingWorkingFolder.getServerItem(),
                                existingWorkingFolder.getLocalItem()));
                    }
                }

                /*
                 * VS comment: If we match on local item, remove. Note that
                 * cloaked mappings have a null local items, so only match on
                 * non-empty ones.
                 */

                if (!StringUtil.isNullOrEmpty(existingWorkingFolder.getLocalItem())
                    && LocalPath.equals(existingWorkingFolder.getLocalItem(), newWorkingFolder.getLocalItem())) {
                    if (overwriteExisting && numRemoved == 0) {
                        i.remove();
                        numRemoved++;
                        continue;
                    } else {
                        throw new VersionControlException(
                            MessageFormat.format(
                                Messages.getString("Workspace.NewMappingConflictsWithLocalPathOfOtherMappingFormat"), //$NON-NLS-1$
                                newWorkingFolder.getServerItem(),
                                newWorkingFolder.getLocalItem(),
                                existingWorkingFolder.getServerItem(),
                                existingWorkingFolder.getLocalItem()));
                    }
                }

                // Check for CLOAKED children of a new CLOAK mapping.
                if (newWorkingFolder.getType() == WorkingFolderType.CLOAK
                    && existingWorkingFolder.getType() == WorkingFolderType.CLOAK) {
                    final String parentPath = newWorkingFolder.getServerItem();
                    final String possibleChild = existingWorkingFolder.getServerItem();

                    if (ServerPath.isChild(parentPath, possibleChild)) {
                        i.remove();
                        continue;
                    }
                }
            }

            folders.add(newWorkingFolder);
        }

        update(null, null, folders.toArray(new WorkingFolder[folders.size()]));

        this.client.getEventEngine().fireWorkspaceUpdated(
            new WorkspaceUpdatedEvent(
                EventSource.newFromHere(),
                this,
                getName(),
                getLocation(),
                WorkspaceEventSource.INTERNAL));
    }

    /**
     * Delete a working folder mapping from this workspace.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param wf
     *        the working folder mapping to remove (any object matching field
     *        values of parameter is deleted).
     */
    public void deleteWorkingFolder(final WorkingFolder wf) throws ServerPathFormatException, IOException {
        Check.notNull(wf, "wf"); //$NON-NLS-1$

        deleteWorkingFolders(new WorkingFolder[] {
            wf
        });
    }

    /**
     * Delete a working folder mapping from this workspace.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param wf
     *        the working folder mapping to remove (any object matching field
     *        values of parameter is deleted).
     */
    public void deleteWorkingFolders(final WorkingFolder[] workingFolders)
        throws ServerPathFormatException,
            IOException {
        Check.notNullOrEmpty(workingFolders, "workingFolders"); //$NON-NLS-1$

        final WorkingFolder[] foldersArray = getFolders();
        final List<WorkingFolder> folders = new ArrayList<WorkingFolder>();
        if (foldersArray != null) {
            folders.addAll(Arrays.asList(foldersArray));
        }

        for (final WorkingFolder wf : workingFolders) {
            for (final Iterator<WorkingFolder> i = folders.iterator(); i.hasNext();) {
                final WorkingFolder w = i.next();

                // We're testing for matches on either the local item or the
                // repository item.

                // If the server path matches, we have a match.
                if (ServerPath.equals(w.getServerItem(), wf.getServerItem())) {
                    i.remove();
                    continue;
                }

                // Cloaked mappings don't have a local item, so we have to test.
                if (w.getLocalItem() != null && w.getLocalItem().length() > 0) {
                    if (LocalPath.equals(w.getLocalItem(), wf.getLocalItem())) {
                        i.remove();
                        continue;
                    }
                }
            }

            /*
             * Remove any orphaned cloaked working folders. If the user just
             * removed a working folder mapping, there may be old cloaked
             * mappings underneath it that should be cleaned up.
             */
            for (final Iterator<WorkingFolder> i = folders.iterator(); i.hasNext();) {
                final WorkingFolder cloaked = i.next();

                if (cloaked.getType() == WorkingFolderType.CLOAK) {
                    // We found a cloaked mapping, so scan the whole list again
                    // for any
                    // of its parents. If we find one, we know we don't delete
                    // this cloak.
                    boolean foundParent = false;

                    for (final Iterator<WorkingFolder> j = folders.iterator(); j.hasNext();) {
                        final WorkingFolder possibleParent = j.next();

                        if (possibleParent.getType() != WorkingFolderType.CLOAK
                            && ServerPath.isChild(possibleParent.getServerItem(), cloaked.getServerItem())) {
                            foundParent = true;
                            break;
                        }
                    }

                    if (foundParent == false) {
                        i.remove();
                        continue;
                    }
                }
            }
        }

        update(null, null, folders.toArray(new WorkingFolder[folders.size()]));

        this.client.getEventEngine().fireWorkspaceUpdated(
            new WorkspaceUpdatedEvent(
                EventSource.newFromHere(),
                this,
                getName(),
                getLocation(),
                WorkspaceEventSource.INTERNAL));
    }

    /**
     * Queries the server for history about an item. History items are returned
     * as an array of changesets.
     *
     * @see VersionControlClient#queryHistory(String, VersionSpec, int,
     *      RecursionType, String, VersionSpec, VersionSpec, int, boolean,
     *      boolean, boolean, boolean)
     */
    public Changeset[] queryHistory(
        final String serverOrLocalPath,
        final VersionSpec version,
        final int deletionID,
        final RecursionType recursion,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean includeFileDetails,
        final boolean slotMode,
        final boolean includeDownloadInfo,
        final boolean sortAscending) throws ServerPathFormatException {
        return client.queryHistory(
            serverOrLocalPath,
            version,
            deletionID,
            recursion,
            user,
            versionFrom,
            versionTo,
            maxCount,
            includeFileDetails,
            slotMode,
            includeDownloadInfo,
            sortAscending);
    }

    /**
     * Queries the server for history about an item. Results are returned as an
     * {@link Iterator} of {@link Changeset}s.
     *
     * @see VersionControlClient#queryHistory(String, VersionSpec, int,
     *      RecursionType, String, VersionSpec, VersionSpec, int, boolean,
     *      boolean, boolean, boolean)
     */
    public Iterator<Changeset> queryHistoryIterator(
        final String serverOrLocalPath,
        final VersionSpec version,
        final int deletionID,
        final RecursionType recursion,
        final String user,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final int maxCount,
        final boolean includeFileDetails,
        final boolean slotMode,
        final boolean includeDownloadInfo,
        final boolean sortAscending) throws ServerPathFormatException {
        return client.queryHistoryIterator(
            serverOrLocalPath,
            version,
            deletionID,
            recursion,
            user,
            versionFrom,
            versionTo,
            maxCount,
            includeFileDetails,
            slotMode,
            includeDownloadInfo,
            sortAscending);
    }

    /**
     * Creates a branch of the given source path at the given version to the
     * given target path. Only server paths are supported.
     *
     * @see VersionControlClient#createBranch(String, String, VersionSpec)
     */
    public int createBranch(final String sourceServerPath, final String targetServerPath, final VersionSpec version) {
        return client.createBranch(sourceServerPath, targetServerPath, version);
    }

    /**
     * Create or update a label for items in this workspace.
     *
     * @see VersionControlClient#createLabel(VersionControlLabel,
     *      LabelItemSpec[], LabelChildOption)
     */
    public LabelResult[] createLabel(
        final VersionControlLabel label,
        final LabelItemSpec[] items,
        final LabelChildOption options) {
        return client.createLabel(label, items, options);
    }

    /**
     * Removes a label that was applied to an item.
     *
     * @see VersionControlClient#unlabelItem(String, String, ItemSpec[],
     *      VersionSpec)
     */
    public LabelResult[] unlabelItem(
        final String label,
        final String scope,
        final ItemSpec[] items,
        final VersionSpec version) {
        return client.unlabelItem(label, scope, items, version);
    }

    /**
     * Query the collection of labels that match the given specifications.
     *
     * @see VersionControlClient#queryLabels(String, String, String, boolean,
     *      String, VersionSpec)
     */
    public VersionControlLabel[] queryLabels(
        final String label,
        final String scope,
        final String owner,
        final boolean includeItemDetails,
        final String filterItem,
        final VersionSpec filterItemVersion) {
        return client.queryLabels(label, scope, owner, includeItemDetails, filterItem, filterItemVersion);
    }

    /**
     * @return the workspace comment
     */
    public String getComment() {
        return getWebServiceObject().getComment();
    }

    /**
     * @return the computer where this workspace exists
     */
    public String getComputer() {
        return getWebServiceObject().getComputer();
    }

    /**
     * @return the options for this workspace
     */
    public WorkspaceOptions getOptions() {
        return WorkspaceOptions.fromFlags(getWebServiceObject().getOptions());
    }

    /**
     * @return the owner's {@link IdentityDescriptor}, if set, otherwise null
     */
    public IdentityDescriptor getOwnerDescriptor() {
        refreshIfNeeded();

        if (getWebServiceObject().getOwnerid() == null) {
            return null;
        }

        return new IdentityDescriptor(getWebServiceObject().getOwnertype(), getWebServiceObject().getOwnerid());
    }

    /**
     * @return the user's security identifier
     */
    public String getSecurityToken() {
        return getWebServiceObject().getSecuritytoken();
    }

    /**
     * Update all items in this workspace to the latest version.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param options
     *        options for the get operation (must not be <code>null</code>)
     * @return a GetStatus instance with the results of the get operation.
     */
    public GetStatus get(final GetOptions options) {
        Check.notNull(options, "options"); //$NON-NLS-1$

        return get(LatestVersionSpec.INSTANCE, options);
    }

    /**
     * Update all items in this workspace to the given version.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param version
     *        the version to update all items in this workspace to (must not be
     *        <code>null</code>)
     * @param options
     *        options for the get operation (must not be <code>null</code>)
     * @return a GetStatus instance with the results of the get operation.
     */
    public GetStatus get(final VersionSpec version, final GetOptions options) {
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        /*
         * This null tells the server to expand the request into all items in
         * this workspace, but to use the given version.
         */
        return get(new GetRequest(null, version), options);
    }

    /**
     * Update the given items for the given workspace.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param request
     *        the request to process (must not be <code>null</code>)
     * @param options
     *        options for the get operation (must not be <code>null</code>)
     * @return a GetStatus instance with the results of the get operation.
     */
    public GetStatus get(final GetRequest request, final GetOptions options) {
        Check.notNull(request, "request"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        return get(new GetRequest[] {
            request
        }, options);
    }

    /**
     * @equivalence get(requests, options, null, false, new AtomicReference
     *              <Conflict[]>())
     */
    public GetStatus get(final GetRequest[] requests, final GetOptions options) {
        return get(requests, options, null, false, new AtomicReference<Conflict[]>());
    }

    /**
     * Update the given items for the given workspace.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param requests
     *        the request items to process (must not be null; items in array
     *        must not be null). To update all items in this workspace, pass a
     *        single {@link GetRequest} with a null itemSpec.
     * @param options
     *        options for the get operation (must not be <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @param alwaysQueryConflicts
     *        true to always query conflicts, false if we may omit this step
     * @param conflicts
     *        a reference to a list of conflicts to return (must not be
     *        <code>null</code>)
     * @return a GetStatus instance with the results of the get operation.
     */
    public GetStatus get(
        final GetRequest[] requests,
        final GetOptions options,
        String[] itemPropertyFilters,
        final boolean alwaysQueryConflicts,
        final AtomicReference<Conflict[]> conflicts) {
        Check.notNull(requests, "requests"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        client.getEventEngine().fireOperationStarted(
            new GetOperationStartedEvent(EventSource.newFromHere(), this, requests));

        /*
         * Always work toward 100 work units. The progress indicator will only
         * be accurate for gets that fit in one page (result set). If we have to
         * process more than one set we'll fill up the task monitor prematurely,
         * which is not an error. Pages are huge so almost all requests will fit
         * inside one.
         */
        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        taskMonitor.begin("", 100); //$NON-NLS-1$
        taskMonitor.setCurrentWorkDescription(Messages.getString("Workspace.ContactingServertoGetListOfItemsToUpdate")); //$NON-NLS-1$

        /*
         * Specify a limit to number of Get operation results that server may
         * return from a single call. This is to guard against client running
         * out of memory, and also to guard against Http runtime timeout on
         * server when streaming a large result set back to the client. This
         * "paging" technique relies on the page advancing once we process the
         * results from the previous call. This is incompatible with force
         * option.
         */

        final boolean getAll = options.contains(GetOptions.GET_ALL);
        final int maxResults = getAll ? 0 : VersionControlConstants.MAX_GET_RESULTS;

        final GetStatus getStatus = new GetStatus();

        try {
            String[] sourceWritableConflicts;
            final WritableConflictOnSourcePathListener conflictListener = new WritableConflictOnSourcePathListener();

            try {
                client.getEventEngine().addGetListener(conflictListener);

                GetStatus latestStatus = null;
                final GetEngine getEngine = new GetEngine(client);

                int resultCount;

                /*
                 * Call the server. If we specify a page limit, call repeatedly
                 * as long as we keep getting a full page back.
                 *
                 * Paging like this makes progress monitoring hard, because we
                 * don't know the total amount of work up front.
                 */
                do {
                    log.debug("Call server for GetOperations."); //$NON-NLS-1$

                    final GetOperation[][] results = client.getWebServiceLayer().get(
                        getName(),
                        getOwnerName(),
                        requests,
                        maxResults,
                        options,
                        null,
                        itemPropertyFilters,
                        false);

                    // Web service call always gets 5
                    taskMonitor.worked(5);

                    // How many results were returned? Is it a full page (see
                    // loop
                    // terminating condition)?
                    resultCount = 0;
                    for (final GetOperation[] result : results) {
                        resultCount += result.length;
                    }

                    log.debug("Process GetOperations"); //$NON-NLS-1$
                    // Use 95 for the processing.
                    TaskMonitorService.pushTaskMonitor(taskMonitor.newSubTaskMonitor(95));
                    try {
                        latestStatus = getEngine.processGetOperations(
                            this,
                            ProcessType.GET,
                            RequestType.NONE,
                            results,
                            options,
                            false,
                            true,
                            ChangePendedFlags.UNKNOWN);
                    } catch (final Exception e) {
                        log.error("Error processing GET operations", e); //$NON-NLS-1$
                        if (e instanceof VersionControlException) {
                            throw (VersionControlException) e;
                        } else {
                            throw new VersionControlException(e);
                        }
                    } finally {
                        TaskMonitorService.popTaskMonitor(true);
                    }

                    log.debug("Latest GetOperations status:"); //$NON-NLS-1$
                    log.debug("    NumOperations        = " + latestStatus.getNumOperations()); //$NON-NLS-1$
                    log.debug("    NumUpdated           = " + latestStatus.getNumUpdated()); //$NON-NLS-1$
                    log.debug("    NumWarnings          = " + latestStatus.getNumWarnings()); //$NON-NLS-1$
                    log.debug("    NumFailures          = " + latestStatus.getNumFailures()); //$NON-NLS-1$
                    log.debug("    NumConflicts         = " + latestStatus.getNumConflicts()); //$NON-NLS-1$
                    log.debug("    NumResolvedConflicts = " + latestStatus.getNumResolvedConflicts()); //$NON-NLS-1$

                    getStatus.combine(latestStatus);
                } while (VersionControlConstants.MAX_GET_RESULTS > 0
                    && resultCount == VersionControlConstants.MAX_GET_RESULTS
                    && latestStatus.getNumUpdated() >= 1);

                sourceWritableConflicts = conflictListener.getMovedPaths();
            } finally {
                client.getEventEngine().removeGetListener(conflictListener);
            }

            final boolean attemptAutoResolve = (!options.contains(GetOptions.NO_AUTO_RESOLVE));

            if (getStatus.getNumConflicts() > 0
                || getStatus.haveResolvableWarnings() && (alwaysQueryConflicts || attemptAutoResolve)) {
                log.debug("Querying conflicts."); //$NON-NLS-1$
                taskMonitor.setCurrentWorkDescription(Messages.getString("Workspace.QueryingConflicts")); //$NON-NLS-1$

                final AtomicBoolean recursive = new AtomicBoolean();
                final String[] conflictScope = calculateConflictScope(requests, sourceWritableConflicts, recursive);
                Conflict[] unresolvedConflicts = queryConflicts(conflictScope, recursive.get());

                if (attemptAutoResolve) {
                    log.debug("Resolving conflicts."); //$NON-NLS-1$
                    taskMonitor.setCurrentWorkDescription(Messages.getString("Workspace.ResolvingConflicts")); //$NON-NLS-1$

                    /* Auto resolve the conflicts */
                    unresolvedConflicts =
                        client.autoResolveValidConflicts(this, unresolvedConflicts, AutoResolveOptions.ALL_SILENT);

                    /*
                     * Update the get status information about conflicts. We
                     * don't change the value of HaveResolvableWarnings because
                     * we won't auto resolve local conflicts.
                     */
                    getStatus.setNumConflicts(unresolvedConflicts.length);
                }

                log.debug("Unresolved conflicts: " + unresolvedConflicts.length); //$NON-NLS-1$
                conflicts.set(unresolvedConflicts);
            }
        } finally {
            /*
             * Event handlers may run a while but not update the work
             * description, so set a generic message so the user knows things
             * are wrapping up.
             */
            taskMonitor.setCurrentWorkDescription(Messages.getString("Workspace.FinishingGetOperation")); //$NON-NLS-1$

            client.getEventEngine().fireOperationCompleted(
                new GetOperationCompletedEvent(EventSource.newFromHere(), this, requests, getStatus));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_GET_COMPLETED);

            taskMonitor.done();
        }

        return getStatus;
    }

    /**
     * Calculates set of paths, used for querying conflicts.
     *
     * @param getRequests
     *        paths specified by the user
     * @param writableConflictPaths
     *        paths of files causing SourceWritable or TargetWritable conflicts
     * @param recursive
     *        recursive if the conflict query needs to be recursive
     * @return List of paths to query
     */
    private String[] calculateConflictScope(
        final GetRequest[] getRequests,
        final String[] writableConflictPaths,
        final AtomicBoolean recursive) {
        recursive.set(false);

        final List<String> result = new ArrayList<String>();

        /* Always query for all paths specified by the user. */
        for (final GetRequest getRequest : getRequests) {
            if (getRequest.getItemSpec() == null) {
                /* This is a workspace get so just query all conflicts. */
                return null;
            }

            /*
             * If we see any full recursion then we mark this request as
             * recursive.
             */
            if (RecursionType.FULL.equals(getRequest.getItemSpec().getRecursionType())) {
                recursive.set(true);
            }

            final String path = getRequest.getItemSpec().getItem();
            if (RecursionType.ONE_LEVEL.equals(getRequest.getItemSpec().getRecursionType())) {
                /*
                 * We can make QueryConflicts one level, ending it with *. It's
                 * incorrect to have local path with multiple *, so we append it
                 * only when there is none, both for local and server path.
                 */
                if (path.indexOf('*') == -1) {
                    if (ServerPath.isServerPath(path)) {
                        result.add(ServerPath.combine(path, "*")); //$NON-NLS-1$
                    } else {
                        result.add(LocalPath.combine(path, "*")); //$NON-NLS-1$
                    }
                }
            }

            /*
             * Even if we added one-level recursion path with *, we need to add
             * path itself as well because $/tp/a/* does not include $/tp/a
             * itself, but one level get on $/tp/a does.
             */
            result.add(path);
        }

        if (writableConflictPaths != null) {
            /*
             * Now let's add source path of files causing SourceWritable or
             * TargetWritable conflicts that are not rooted in the paths
             * specified by the user.
             */
            for (final String sourcePath : writableConflictPaths) {
                boolean includePath = true;
                for (final GetRequest getRequest : getRequests) {
                    final String getRequestPath = getRequest.getItemSpec().getItem();

                    /*
                     * sourcePath is a local path so we call IsSubItem only if
                     * getRequestPath is local path as well.
                     */
                    if (getRequestPath != null
                        && getRequestPath.length() > 0
                        && !ServerPath.isServerPath(getRequestPath)
                        && LocalPath.isChild(getRequestPath, sourcePath)) {
                        includePath = false;
                        break;
                    }
                }
                if (includePath) {
                    result.add(sourcePath);
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * @equivalence rollback(itemSpecs, itemVersionSpec, versionFrom, versionTo,
     *              lockLevel, options, null)
     */
    public GetStatus rollback(
        final ItemSpec[] itemSpecs,
        final VersionSpec itemVersionSpec,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final LockLevel lockLevel,
        final RollbackOptions options) {
        return rollback(itemSpecs, itemVersionSpec, versionFrom, versionTo, lockLevel, options, null);
    }

    /**
     * Pends a change that reverts the contents of one or more items to a
     * previous version's contents. <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param itemSpecs
     *        the items to roll back (must not be <code>null</code>)
     * @param itemVersionSpec
     *        the version of the given item specs (may be null)
     * @param versionFrom
     *        the version to rollback from (not sure) (must not be
     *        <code>null</code>)
     * @param versionTo
     *        the version to rollback to (not sure) (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level for the pending change (must not be
     *        <code>null</code>)
     * @param options
     *        rollback options (must not be <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @since TFS 2010
     */
    public GetStatus rollback(
        final ItemSpec[] itemSpecs,
        final VersionSpec itemVersionSpec,
        final VersionSpec versionFrom,
        final VersionSpec versionTo,
        final LockLevel lockLevel,
        final RollbackOptions options,
        String[] itemPropertyFilters) {
        Check.notNull(versionFrom, "versionFrom"); //$NON-NLS-1$
        Check.notNull(versionTo, "versionTo"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        client.getEventEngine().fireOperationStarted(
            new RollbackOperationStartedEvent(EventSource.newFromHere(), this, itemSpecs, options));

        GetStatus status = null;

        try {
            // Using web service directly so merge filters configured on client
            itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

            final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();
            final AtomicReference<Conflict[]> conflictsHolder = new AtomicReference<Conflict[]>();
            final AtomicReference<ChangePendedFlags> changePendedFlagsHolder = new AtomicReference<ChangePendedFlags>();

            final GetOperation[] operations = getClient().getWebServiceLayer().rollback(
                getName(),
                getOwnerName(),
                itemSpecs,
                itemVersionSpec,
                versionFrom,
                versionTo,
                options,
                lockLevel,
                conflictsHolder,
                failuresHolder,
                null,
                itemPropertyFilters,
                changePendedFlagsHolder);

            final Failure[] failures = failuresHolder.get();
            Conflict[] conflicts = conflictsHolder.get();
            int nonResolvedConflicts = 0;
            int resolvedConflicts = 0;
            final ChangePendedFlags changePendedFlags = changePendedFlagsHolder.get();

            /*
             * Match up these getOps and rollback details (returned as Conflict
             * objects). The conflicts with matching getOps have already been
             * resolved.
             */
            final Map<Integer, Conflict> itemIDConflictMap = new HashMap<Integer, Conflict>();
            final List<String> conflictPaths = new ArrayList<String>();
            for (final Conflict conflict : conflicts) {
                if (conflict.isResolved()) {
                    itemIDConflictMap.put(new Integer(conflict.getYourItemID()), conflict);
                } else {
                    conflictPaths.add(conflict.getServerPath());
                }
            }

            /*
             * Set the merge details for each of the resolved conflicts we found
             * previously.
             */
            for (final GetOperation operation : operations) {
                final Conflict conflict = itemIDConflictMap.get(new Integer(operation.getItemID()));

                if (conflict != null) {
                    operation.setMergeDetails(conflict);
                }
            }

            if (isLocal()) {
                // We want to auto resolve conflicts if the option is set
                if (!options.contains(RollbackOptions.NO_AUTO_RESOLVE)) {
                    if (getClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012_1.getValue()) {
                        /*
                         * The download urls for base files were not populated
                         * on conflicts in pre 2012 servers. Let's call
                         * QueryConflicts now so that we have that information
                         */
                        conflicts = queryConflicts(conflictPaths.toArray(new String[conflictPaths.size()]), false);
                    }

                    /*
                     * Try to resolve any AutoResolve candidates and add
                     * failures to the unresolved conflicts list.
                     */
                    final Conflict[] remainingConflicts =
                        getClient().autoResolveValidConflicts(this, conflicts, AutoResolveOptions.ALL_SILENT);

                    resolvedConflicts = conflicts.length - remainingConflicts.length;
                    conflicts = remainingConflicts;
                }

                /*
                 * Fire events for rollback operations that did not get resolved
                 * by the server. The others will get fired in get.cs as they
                 * are processed.
                 */
                for (final Conflict conflict : conflicts) {
                    if (conflict.getResolution() == Resolution.NONE) {
                        // The pending change arg is null because of the
                        // conflict.
                        getClient().getEventEngine().fireMerging(
                            new MergingEvent(
                                EventSource.newFromHere(),
                                conflict,
                                this,
                                false,
                                null,
                                OperationStatus.CONFLICT,
                                ChangeType.NONE,
                                true,
                                new PropertyValue[0]));

                        nonResolvedConflicts++;
                    }
                }

                final GetEngine getEngine = new GetEngine(getClient());

                status = getEngine.processGetOperations(
                    this,
                    ProcessType.ROLLBACK,
                    operations,
                    GetOptions.NONE,
                    changePendedFlags);

                status.setNumConflicts(status.getNumConflicts() + nonResolvedConflicts);
                status.setNumResolvedConflicts(resolvedConflicts);
            } else if (operations.length > 0) {
                getClient().getEventEngine().fireNonFatalError(
                    new NonFatalErrorEvent(
                        EventSource.newFromHere(),
                        getClient(),
                        new Exception(
                            MessageFormat.format(
                                Messages.getString(
                                    "Workspace.OperationCompletedForRemoteWorkspaceButGetRequiredFormat"), //$NON-NLS-1$
                                getDisplayName()))));
            }

            if (status == null) {
                status = new GetStatus();
                status.setNumOperations(operations.length);
            }

            if (changePendedFlags.contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
                invalidateMappings();
            }

            getClient().reportFailures(this, failures);

            for (final Failure failure : failures) {
                status.addFailure(failure);
            }
        } finally {
            getClient().getEventEngine().fireOperationCompleted(
                new RollbackOperationCompletedEvent(EventSource.newFromHere(), this, itemSpecs, options, status));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED);
        }

        return status;
    }

    /**
     * @equivalence previewGetItems(requests, options, null)
     */
    public GetOperation[][] previewGetItems(final GetRequest[] requests, final GetOptions options) {
        return previewGetItems(requests, options, null);
    }

    /**
     * Gets the {@link GetOperation}s for the given {@link GetRequest}s, but
     * does not update the workspace or fire any events.
     *
     * @param requests
     *        the request items to process (must not be null; items in array
     *        must not be null). To update all items in this workspace, pass a
     *        single AGetRequest with a null itemSpec.
     * @param options
     *        options for the get operation (must not be <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return an array of arrays of {@link GetOperation}s. There is a top-level
     *         array element for each given {@link GetRequest}, and the inner
     *         arrays may be empty but never null.
     */
    public GetOperation[][] previewGetItems(
        final GetRequest[] requests,
        final GetOptions options,
        String[] itemPropertyFilters) {
        Check.notNull(requests, "requests"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        return client.getWebServiceLayer().get(
            getName(),
            getOwnerName(),
            requests,
            0,
            options,
            null,
            itemPropertyFilters,
            false);
    }

    /**
     * Gets this workspace's name.
     *
     * @return this workspace's name.
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getOwnerName() {
        return getWebServiceObject().getOwner();
    }

    public String getOwnerDisplayName() {
        return getWebServiceObject().getOwnerdisp();
    }

    public String[] getOwnerAliases() {
        if (getWebServiceObject().getOwnerAliases() != null) {
            return getWebServiceObject().getOwnerAliases();
        } else {
            return new String[0];
        }
    }

    public Calendar getLastAccessDate() {
        return getWebServiceObject().getLastAccessDate();
    }

    /**
     * @equivalence WorkspaceSpec(this.getName(),
     *              this.getOwnerName()).toString();
     */
    public String getQualifiedName() {
        return new WorkspaceSpec(getName(), getOwnerName()).toString();
    }

    // Working Folder Mappings

    /**
     * Tests whether the given local path is mapped to a working folder in this
     * workspace and that mapping is not a cloak.
     *
     * @param localPath
     *        the local path (file or directory) to test (must not be
     *        <code>null</code> or empty)
     * @return <code>true</code> if the given local path is mapped to a working
     *         folder in this workspace that is not a cloak mapping,
     *         <code>false</code> otherwise
     */
    public boolean isLocalPathMapped(final String localPath) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$

        final PathTranslation translation = translateLocalPathToServerPath(localPath);
        return translation != null && translation.isCloaked() == false;
    }

    /**
     * Tests whether the given server path is mapped to a working folder in this
     * workspace and that mapping is not a cloak.
     *
     * @param serverPath
     *        the server path (file or directory) to test (must not be
     *        <code>null</code> or empty)
     * @return <code>true</code> if the given server path is mapped to a working
     *         folder in this workspace that is not a cloak mapping,
     *         <code>false</code> otherwise
     */
    public boolean isServerPathMapped(final String serverPath) {
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$

        final PathTranslation translation = translateServerPathToLocalPath(serverPath);
        return translation != null && translation.isCloaked() == false;
    }

    /**
     * <p>
     * Maps the given local path to a server path using the most precise working
     * folder mapping in this workspace. Returns <code>null</code> if the most
     * precise working folder mapping is a cloak mapping.
     * </p>
     *
     * @param localPath
     *        the local path to map to a server path (must not be
     *        <code>null</code> or empty)
     * @return the server path that maps to the given local path, or
     *         <code>null</code> if no mapping exists or the item is cloaked
     */
    public String getMappedServerPath(final String localPath) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$
        Check.isTrue(ServerPath.isServerPath(localPath) == false, "ServerPath.isServerPath(localPath) == false"); //$NON-NLS-1$

        final PathTranslation translation = translateLocalPathToServerPath(localPath);

        if (translation != null && translation.isCloaked() == false) {
            return translation.getTranslatedPath();
        }

        return null;
    }

    /**
     * <p>
     * Maps the given server path to a local path using the most precise working
     * folder mapping in this workspace. Returns <code>null</code> if the most
     * precise working folder mapping is a cloak mapping.
     * </p>
     *
     * @param serverPath
     *        the server path to map to a local path (must not be
     *        <code>null</code> or empty)
     * @return the local path that maps to the given server path, or
     *         <code>null</code> if no mapping exists or the item is cloaked
     */
    public String getMappedLocalPath(final String serverPath) {
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$
        Check.isTrue(ServerPath.isServerPath(serverPath), "ServerPath.isServerPath(serverPath)"); //$NON-NLS-1$

        final PathTranslation translation = translateServerPathToLocalPath(serverPath);

        if (translation != null && translation.isCloaked() == false) {
            return translation.getTranslatedPath();
        }

        return null;
    }

    /**
     * <p>
     * Gets the working folder that contains an exact match for the specified
     * local path (including cloak mappings) or <code>null</code> if there is no
     * exact match.
     * </p>
     *
     * @param localPath
     *        the local path to get the mapping for (must not be
     *        <code>null</code>)
     * @return the working folder that directly maps the specified local path
     *         (including cloak mappings), or <code>null</code> if there is no
     *         exact mapping
     */
    public WorkingFolder getExactMappingForLocalPath(final String localPath) {
        /*
         * A path translation is needed to match a cloaked working folder
         * mapping (which does not have a local path to compare with). The
         * translation is deferred until we actually encounter a cloaked
         * mapping.
         */
        PathTranslation translation = null;
        final WorkingFolder[] workingFolders = getFolders();

        for (final WorkingFolder workingFolder : workingFolders) {
            final String localItem = workingFolder.getLocalItem();
            if (localItem == null) {
                // This is a cloaked mapping. Determine the server mapping
                // for this local path and return the working folder if it
                // matches the translated server path.
                if (translation == null) {
                    translation = translateLocalPathToServerPath(localPath);
                }

                if (translation != null) {
                    final String translatedServerPath = translation.getTranslatedPath();
                    final String workingFolderServerPath = workingFolder.getServerItem();

                    if (ServerPath.equals(translatedServerPath, workingFolderServerPath)) {
                        return workingFolder;
                    }
                }
            } else if (LocalPath.equals(localItem, localPath)) {
                return workingFolder;
            }
        }

        return null;
    }

    /**
     * <p>
     * Gets the working folder that contains an exact match for the specified
     * server path (including cloak mappings) or <code>null</code> if there is
     * no exact match.
     * </p>
     *
     * @param serverPath
     *        the server path to get the mapping for (must not be
     *        <code>null</code>)
     * @return the working folder that directly maps the specified server path
     *         (including cloak mappings), or <code>null</code> if there is no
     *         exact mapping
     */
    public WorkingFolder getExactMappingForServerPath(final String serverPath) {
        final WorkingFolder[] workingFolders = getFolders();

        for (final WorkingFolder workingFolder : workingFolders) {
            final String serverItem = workingFolder.getServerItem();
            if (serverItem != null && ServerPath.equals(serverItem, serverPath)) {
                return workingFolder;
            }
        }

        return null;
    }

    /**
     * Gets the closest working folder mapping for the specified server path.
     * This method can return a mapping which is an exact match for the
     * specified server path.
     *
     * @param serverPath
     *        the server path whose parent mapping we wish to find (must not be
     *        <code>null</code>)
     * @return the closest parent mapping or <code>null</code> if no there is no
     *         mapping
     */
    public WorkingFolder getClosestMappingForServerPath(final String serverPath) {
        WorkingFolder closestParentFolder = null;
        final WorkingFolder[] workingFolders = getFolders();

        for (final WorkingFolder workingFolder : workingFolders) {
            final String serverItem = workingFolder.getServerItem();
            if (ServerPath.isChild(serverItem, serverPath)) {
                if (closestParentFolder == null || serverItem.length() > closestParentFolder.getServerItem().length()) {
                    closestParentFolder = workingFolder;
                }
            }
        }

        return closestParentFolder;
    }

    /**
     * Gets the closest working folder mapping for the specified local path.
     * This method can return a mapping which is an exact match for the
     * specified local path.
     *
     * @param localPath
     *        the local path whose parent mapping we wish to find (must not be
     *        <code>null</code>)
     * @return the closest parent mapping or <code>null</code> if no there is no
     *         mapping
     */
    public WorkingFolder getClosestMappingForLocalPath(final String localPath) {
        WorkingFolder closestParentFolder = null;
        final WorkingFolder[] workingFolders = getFolders();

        for (final WorkingFolder workingFolder : workingFolders) {
            final String localItem = workingFolder.getLocalItem();
            if (LocalPath.isChild(localItem, localPath)) {
                if (closestParentFolder == null || localItem.length() > closestParentFolder.getLocalItem().length()) {
                    closestParentFolder = workingFolder;
                }
            }
        }

        return closestParentFolder;
    }

    /**
     * Check whether serverPath exists on the server
     *
     * @param serverPath
     * @return
     */
    public boolean serverPathExists(final String serverPath) {
        return serverPathExists(serverPath, LatestVersionSpec.INSTANCE);
    }

    /**
     * Check whether serverPath exists on the server in a certain versionSpec
     *
     * @param serverPath
     * @param versionSpec
     * @return
     */
    public boolean serverPathExists(final String serverPath, final VersionSpec versionSpec) {
        Check.notNullOrEmpty(serverPath, "serverPath"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$
        Item item = null;
        try {
            item = getClient().getItem(serverPath, versionSpec);
        } catch (final Exception e) {
            return false;
        }
        return item != null;
    }

    /**
     * <p>
     * Gets a new working folder that describes the mapping between the given
     * local path and its server item. The mapping returned is not necessarily
     * the mapping the user explicitly mapped; it will contain the most specific
     * item paths (files or folders).
     * </p>
     * <p>
     * A {@link PathTranslation} is returned for items that are cloaked and the
     * translated item will be non-<code>null</code>.
     * </p>
     *
     * @param localPath
     *        the local path to find a mapping for (must not be
     *        <code>null</code> or empty)
     * @return the working folder mapping that most precisely matches the given
     *         path (including cloak mappings), or <code>null</code> if the item
     *         is not mapped
     */
    public PathTranslation translateLocalPathToServerPath(final String localPath) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$

        return WorkingFolder.translateLocalItemToServerItem(localPath, getFolders());
    }

    /**
     * <p>
     * Gets a new working folder that describes the mapping between the given
     * server path and its local item. The mapping returned is not necessarily
     * the mapping the user explicitly mapped; it will contain the most specific
     * item paths (files or folders).
     * </p>
     * <p>
     * A {@link PathTranslation} is returned for items that are cloaked, but the
     * translated item will be <code>null</code>.
     * </p>
     *
     * @param serverPath
     *        the server path to find a mapping for (must not be
     *        <code>null</code> or empty).
     * @return the working folder mapping that most precisely matches the given
     *         path ({@link PathTranslation#getTranslatedPath()} is
     *         <code>null</code> for cloaked items), or null if the item is not
     *         mapped.
     */
    public PathTranslation translateServerPathToLocalPath(final String serverPath) {
        return WorkingFolder.translateServerItemToLocalItem(serverPath, getFolders(), true);
    }

    /**
     * Gets all the working folder mappings in this workspace.
     *
     * @return a copy of the array of working folder mappings that belong in
     *         this workspace. May be empty but never null.
     */
    public WorkingFolder[] getFolders() {
        refreshMappingsIfNeeded();

        final _WorkingFolder[] folders = getWebServiceObject().getFolders();

        if (folders == null || folders.length == 0) {
            return new WorkingFolder[0];
        }

        return WorkingFolder.clone((WorkingFolder[]) WrapperUtils.wrap(WorkingFolder.class, folders));
    }

    public void setFolders(final WorkingFolder[] folders) {
        getWebServiceObject().setFolders((_WorkingFolder[]) WrapperUtils.unwrap(_WorkingFolder.class, folders));
    }

    // Field Compare

    public static int compareName(final String name1, final String name2) {
        if (name1 == name2) {
            return 0;
        }

        if (name1 == null) {
            return -1;
        }

        if (name2 == null) {
            return 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
    }

    public static int compareOwner(final String owner1, final String owner2) {
        if (owner1 == owner2) {
            return 0;
        }

        if (owner1 == null) {
            return -1;
        }

        if (owner2 == null) {
            return 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(owner1, owner2);
    }

    public static int compareComputer(final String computer1, final String computer2) {
        if (computer1 == computer2) {
            return 0;
        }

        if (computer1 == null) {
            return -1;
        }

        if (computer2 == null) {
            return 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(computer1, computer2);
    }

    public static int compareComment(final String comment1, final String comment2) {
        if (comment1 == comment2) {
            return 0;
        }

        if (comment1 == null) {
            return -1;
        }

        if (comment2 == null) {
            return 1;
        }

        return comment1.compareTo(comment2);
    }

    public static int compareServerURI(final URI uri1, final URI uri2) {
        if (uri1 == uri2) {
            return 0;
        }

        if (uri1 == null) {
            return -1;
        }

        if (uri2 == null) {
            return 1;
        }

        return ServerURIComparator.INSTANCE.compare(uri1, uri2);
    }

    public static int compareSecurityToken(final String securityToken1, final String securityToken2) {
        if (securityToken1 == securityToken2) {
            return 0;
        }

        if (securityToken1 == null) {
            return -1;
        }

        if (securityToken2 == null) {
            return 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(securityToken1, securityToken2);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getMappedPaths() {
        return WorkingFolder.extractMappedPaths(getFolders());
    }

    // Field Matching

    public static boolean matchName(final String name1, final String name2) {
        return compareName(name1, name2) == 0;
    }

    public static boolean matchOwner(final String owner1, final String owner2) {
        return compareOwner(owner1, owner2) == 0;
    }

    public static boolean matchComputer(final String computer1, final String computer2) {
        return compareComputer(computer1, computer2) == 0;
    }

    public static boolean matchServerGUID(final GUID serverGUID1, final GUID serverGUID2) {
        return serverGUID1.equals(serverGUID2);
    }

    public static boolean matchComment(final String comment1, final String comment2) {
        return compareComment(comment1, comment2) == 0;
    }

    public static boolean matchServerURI(final URI serverURI1, final URI serverURI2) {
        return compareServerURI(serverURI1, serverURI2) == 0;
    }

    public static boolean matchSecurityToken(final String securityToken1, final String securityToken2) {
        return compareSecurityToken(securityToken1, securityToken2) == 0;
    }

    /**
     * Returns true if the owner name matches any of the valid owner names (
     * {@link #getOwnerName()}, {@link #getOwnerDisplayName()},
     * {@link #getOwnerAliases()}) for this workspace.
     */
    public boolean ownerNameMatches(final String ownerName) {
        if (matchOwner(getOwnerName(), ownerName)) {
            return true;
        }

        if (matchOwner(getOwnerDisplayName(), ownerName)) {
            return true;
        }

        if (getOwnerAliases() != null) {
            for (final String aliasName : getOwnerAliases()) {
                if (matchOwner(aliasName, ownerName)) {
                    return true;
                }
            }
        }

        return false;
    }

    // Field Hashing

    public static int hashName(final String name) {
        return LocaleInvariantStringHelpers.caseInsensitiveHashCode(name);
    }

    public static int hashOwner(final String ownerName) {
        return LocaleInvariantStringHelpers.caseInsensitiveHashCode(ownerName);
    }

    /**
     * Reconciles a local workspace with the server.
     *
     *
     * @param reconcileMissingLocalItems
     *        True to remove local version rows for items that no longer exist
     *        on disk
     * @param AtomicBboolean
     */
    public void reconcile(final boolean reconcileMissingLocalItems, final AtomicBoolean pendingChangesUpdatedByServer) {
        pendingChangesUpdatedByServer.set(false);
        if (WorkspaceLocation.LOCAL != this.getLocation()) {
            // No work to do.
            return;
        }

        final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();

        LocalDataAccessLayer.reconcileLocalWorkspace(
            this,
            client.getWebServiceLayer(),
            false,
            reconcileMissingLocalItems,
            failures,
            pendingChangesUpdatedByServer);

        client.reportFailures(this, failures.get());
    }

    // Working Folder Mappings

    /**
     * Ask the server for permission to add file from local working folders. One
     * file encoding (possibly a special encoding value to force binary or
     * autodetection) is applied to all paths.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the local disk paths of files or folders to add (must not be
     *        <code>null</code> or empty)
     * @param recursive
     *        whether the given paths, if directories, should be descended and
     *        subitems added.
     * @param fileEncoding
     *        the encoding these files are in (if <code>null</code> or
     *        {@link FileEncoding#AUTOMATICALLY_DETECT}, the encoding is
     *        detected).
     * @param lockLevel
     *        the type of lock requested during this add (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @return the number of file add requests that were successfully processed
     *         by the server. This number may be greater than the number of
     *         paths given.
     */
    public int pendAdd(
        final String[] paths,
        final boolean recursive,
        final FileEncoding fileEncoding,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        Check.notNull(paths, "paths"); //$NON-NLS-1$

        return pendAdd(paths, recursive, lockLevel, getOptions, pendOptions, null, fileEncoding);
    }

    /**
     * Ask the server for permission to add file from local working folders. The
     * file encodings for each file may be specified individually (the encodings
     * array must be the same size as the paths array).
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the local disk paths of files or folders to add (must not be
     *        <code>null</code> or empty)
     * @param recursive
     *        whether the given paths, if directories, should be descended and
     *        subitems added.
     * @param lockLevel
     *        the type of lock requested during this add (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param encodingHints
     *        a map of paths to the {@link FileEncoding} that should be used
     *        when the {@link ChangeRequest} for that path is created. This map
     *        is only consulted for files that exist on disk (directories and
     *        non-existing files might have different encodings). Paths not
     *        present in this map use the defaultEncoding. May be
     *        <code>null</code>.
     * @param defaultEncoding
     *        the {@link FileEncoding} to use to process files that do not have
     *        hints in the encodingHints map. May be <code>null</code> to
     *        automatically detect file encodings.
     * @return the number of file add requests that were successfully processed
     *         by the server. This number may be greater than the number of
     *         paths given.
     */
    public int pendAdd(
        final String[] paths,
        final boolean recursive,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        PendChangesOptions pendOptions,
        final Map<String, FileEncoding> encodingHints,
        final FileEncoding defaultEncoding) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        /*
         * Determine if we want to show errors around files already exsiting in
         * source control. We only want to show those errors if the user
         * explicitly passed that path in. As a heuristic we won't show errors
         * if any wildcard or recursion is passed in.
         */
        boolean showItemExistsFailures = !recursive;
        if (!recursive) {
            // Check for wildcards since there is no recursion.
            for (final String path : paths) {
                showItemExistsFailures &= !ItemPath.isWildcard(path);
            }
        }

        if (!showItemExistsFailures) {
            pendOptions = pendOptions.combine(PendChangesOptions.SUPPRESS_ITEM_NOT_FOUND_FAILURES);
        }

        // Walk the file system and find the files matching fileSpecs.
        final FileSystemWalker walker = new FileSystemWalker(
            this,
            paths,
            recursive,
            true,
            pendOptions.contains(PendChangesOptions.TREAT_MISSING_ITEMS_AS_FILES),
            pendOptions.contains(PendChangesOptions.APPLY_LOCAL_ITEM_EXCLUSIONS));

        final List<ChangeRequest> requests = new ArrayList<ChangeRequest>();
        final List<ChangeRequest> propertyRequests = new ArrayList<ChangeRequest>();
        final List<ItemSpec> itemSpecs = new ArrayList<ItemSpec>();

        // Ugh
        final PendChangesOptions finalPendChangesOptions = pendOptions;

        walker.walk(new FileSystemVisitor() {
            @Override
            public void visit(final String path) {
                final ItemType missingItemsItemType =
                    finalPendChangesOptions.contains(PendChangesOptions.TREAT_MISSING_ITEMS_AS_FILES) ? ItemType.FILE
                        : ItemType.ANY;

                final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(path);

                // Detect the item type and code page

                final ItemType itemType;
                final int codePage;
                if (!attrs.exists()) {
                    if (missingItemsItemType == ItemType.ANY) {
                        // The file disappeared.
                        throw new VersionControlException(
                            MessageFormat.format(Messages.getString("Workspace.FileOrFolderNotFoundFormat"), path)); //$NON-NLS-1$
                    } else {
                        itemType = missingItemsItemType;

                        // Use binary code page for missing files
                        codePage = VersionControlConstants.ENCODING_BINARY;
                    }
                } else {
                    if (attrs.isSymbolicLink()) {
                        itemType = ItemType.FILE;

                        // Encoding for a symbolic link doesn't matter
                        codePage = VersionControlConstants.ENCODING_BINARY;
                    } else if (!attrs.isDirectory()) {
                        itemType = ItemType.FILE;

                        FileEncoding encoding = encodingHints != null ? encodingHints.get(path) : defaultEncoding;
                        if (encoding == null) {
                            encoding = FileEncoding.AUTOMATICALLY_DETECT;
                        }

                        codePage = FileEncodingDetector.detectEncoding(path, encoding).getCodePage();
                    } else {
                        itemType = ItemType.FOLDER;

                        // Encoding for a directory doesn't matter
                        codePage = VersionControlConstants.ENCODING_BINARY;
                    }
                }

                try {
                    final ItemSpec spec = new ItemSpec(path, RecursionType.NONE);
                    itemSpecs.add(spec);

                    final ChangeRequest cr =
                        new ChangeRequest(spec, null, RequestType.ADD, itemType, codePage, lockLevel, 0, null, true);

                    /*
                     * TEE-specific code to detect Unix execute bit. We can only
                     * pend one type of change at a time, so this is a separate
                     * request.
                     */
                    if (Platform.isCurrentPlatform(Platform.GENERIC_UNIX)) {
                        if (PlatformMiscUtils.getInstance().getEnvironmentVariable(
                            EnvironmentVariables.DISABLE_SYMBOLIC_LINK_PROP) == null && attrs.isSymbolicLink()) {
                            if (isLocalWorkspace()) {
                                cr.setProperties(new PropertyValue[] {
                                    PropertyConstants.IS_SYMLINK
                                });
                            } else {
                                final ChangeRequest r = new ChangeRequest(
                                    spec,
                                    null,
                                    RequestType.PROPERTY,
                                    itemType,
                                    codePage,
                                    lockLevel,
                                    0,
                                    null,
                                    true);
                                r.setProperties(new PropertyValue[] {
                                    PropertyConstants.IS_SYMLINK
                                });
                                propertyRequests.add(r);
                            }
                        } else if (!attrs.isDirectory()
                            && attrs.isExecutable()
                            && PlatformMiscUtils.getInstance().getEnvironmentVariable(
                                EnvironmentVariables.DISABLE_DETECT_EXECUTABLE_PROP) == null) {
                            if (isLocalWorkspace()) {
                                cr.setProperties(new PropertyValue[] {
                                    PropertyConstants.EXECUTABLE_ENABLED_VALUE
                                });
                            } else {
                                final ChangeRequest r = new ChangeRequest(
                                    spec,
                                    null,
                                    RequestType.PROPERTY,
                                    itemType,
                                    codePage,
                                    lockLevel,
                                    0,
                                    null,
                                    false);
                                r.setProperties(new PropertyValue[] {
                                    PropertyConstants.EXECUTABLE_ENABLED_VALUE
                                });
                                propertyRequests.add(r);
                            }
                        }
                    }
                    requests.add(cr);
                } catch (final ChangeRequestValidationException e) {
                    log.info("Cannot create change request:", e); //$NON-NLS-1$

                    // Shouldn't happen for adds
                    client.getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(EventSource.newFromHere(), Workspace.this, e));
                }
            }
        });

        // Return now if there's nothing left to do.
        if (requests.size() == 0) {
            // No items matched.
            return 0;
        }

        final int numPended =
            pendChanges(requests.toArray(new ChangeRequest[requests.size()]), getOptions, pendOptions, null);

        if (numPended > 0 && propertyRequests.size() > 0) {
            if (getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                final PendingSet set = getPendingChanges(itemSpecs.toArray(new ItemSpec[itemSpecs.size()]), false);
                if (set != null && set.getPendingChanges() != null && set.getPendingChanges().length > 0) {
                    final HashSet<String> newAdds = new HashSet<String>();
                    final PendingChange[] pendingChanges = set.getPendingChanges();
                    for (final PendingChange pc : pendingChanges) {
                        if (pc.isAdd()) {
                            newAdds.add(pc.getLocalItem());
                        }
                    }

                    final List<ChangeRequest> propRequests = new ArrayList<ChangeRequest>();
                    for (final ChangeRequest r : propertyRequests) {
                        if (newAdds.contains(r.getItemSpec().getItem())) {
                            propRequests.add(r);
                        }
                    }

                    if (propRequests.size() > 0) {
                        pendChanges(
                            propRequests.toArray(new ChangeRequest[propRequests.size()]),
                            GetOptions.NONE,
                            pendOptions,
                            null);
                    }
                }
            } else {
                client.getEventEngine().fireNonFatalError(
                    new NonFatalErrorEvent(
                        EventSource.newFromHere(),
                        this,
                        new VersionControlException(Messages.getString("Workspace.PropertyNotSupportedText")))); //$NON-NLS-1$
            }
        }

        // Report warnings for any applied exclusions
        final String[] exclusionsApplied = walker.getExclusionsApplied();

        if (exclusionsApplied.length > 0) {
            final String exclusionsList = StringUtil.join(exclusionsApplied, ";"); //$NON-NLS-1$

            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    this,
                    new Exception(
                        MessageFormat.format(
                            Messages.getString("Workspace.ItemsIgnoredBecauseOfExclusionsFormat"), //$NON-NLS-1$
                            exclusionsList))));
        }

        return numPended;
    }

    /**
     * @equivalence pendBranch(sourcePath, targetPath, version, lockLevel,
     *              recursion, getOptions, pendOptions, null)
     */
    public int pendBranch(
        final String sourcePath,
        final String targetPath,
        final VersionSpec version,
        final LockLevel lockLevel,
        final RecursionType recursion,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendBranch(sourcePath, targetPath, version, lockLevel, recursion, getOptions, pendOptions, null);
    }

    /**
     * Pends a branch change. The item specified by sourcePath at the given
     * version will be branched into the targetPath (if targetPath is a folder,
     * a new folder with sourcePath's name is created inside it).
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param sourcePath
     *        the existing item to branch.
     * @param targetPath
     *        the destination for the branch to end up. If this path is a
     *        folder, a new item will be created inside it with the sourcePath
     *        item name.
     * @param version
     *        the version at which to branch the source item.
     * @param lockLevel
     *        the lock type to pend the change with.
     * @param recursion
     *        the type of recursion to apply to the item paths (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of branch changes pended.
     */
    public int pendBranch(
        final String sourcePath,
        final String targetPath,
        final VersionSpec version,
        final LockLevel lockLevel,
        final RecursionType recursion,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(sourcePath, "sourcePath"); //$NON-NLS-1$
        Check.notNullOrEmpty(targetPath, "targetPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        final ChangeRequest[] changes = new ChangeRequest[1];

        /*
         * Sending RecursionType.Full to the server will do the right thing: if
         * the item is a folder, it and all its items are recursively branched.
         * If the item is a file, no recursion is applied.
         */
        try {
            changes[0] = new ChangeRequest(
                new ItemSpec(sourcePath, recursion),
                version,
                RequestType.BRANCH,
                ItemType.ANY,
                VersionControlConstants.ENCODING_UNCHANGED,
                lockLevel,
                0,
                targetPath,
                true);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(changes, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * Pend these changes for this workspace on the server.
     *
     * @param requests
     *        the requested changes to pend (must not be <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of changes that were successfully processed by the
     *         server.
     */
    private int pendChanges(
        final ChangeRequest[] requests,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        String[] itemPropertyFilters) {
        Check.notNull(requests, "requests"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.notNull(pendOptions, "pendOptions"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        client.getEventEngine().fireOperationStarted(
            new PendOperationStartedEvent(EventSource.newFromHere(), this, requests));

        if (getClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()) {
            if (hasPropertyChange(requests)) {
                client.getEventEngine().fireNonFatalError(
                    new NonFatalErrorEvent(
                        EventSource.newFromHere(),
                        this,
                        new VersionControlException(Messages.getString("Workspace.PropertyNotSupportedText")))); //$NON-NLS-1$
            }
        }

        int ret = 0;
        try {
            SupportedFeatures features = SupportedFeatures.ALL;

            /*
             * If the get operation "Force Checkout Local Version" is set, we do
             * not advertise to the server that we support get latest on
             * checkout. Presumably, there may be a state where the server
             * wishes to update us to the local version and this explicitly
             * stops that.
             */
            if (pendOptions.contains(PendChangesOptions.FORCE_CHECK_OUT_LOCAL_VERSION)) {
                features = features.remove(SupportedFeatures.GET_LATEST_ON_CHECKOUT);
            }

            final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();
            final AtomicBoolean onlineOperation = new AtomicBoolean();
            final AtomicReference<ChangePendedFlags> changePendedFlags = new AtomicReference<ChangePendedFlags>();

            final GetOperation[] operations = client.getWebServiceLayer().pendChanges(
                getName(),
                getOwnerName(),
                requests,
                pendOptions,
                features,
                failures,
                itemPropertyFilters,
                null,
                true,
                onlineOperation,
                changePendedFlags);

            // Get any required files.
            if (operations.length > 0) {

                /*
                 * The TFS server (as of TFS 2013 QU1) provides in the response
                 * only properties saved in the server's database, i.e. already
                 * checked in. Thus, to process the executable bit and symlinks
                 * using properties mechanism correctly in the client file
                 * system, we have to merge properties received in the response
                 * with those submitted in the change request.
                 *
                 * Note that for the local workspaces it's already done in the
                 * LocalDataAccessLayer class.
                 */
                if (WorkspaceLocation.SERVER == this.getLocation()
                    && getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
                    for (final ChangeRequest request : requests) {
                        final PropertyValue[] requestProperties = request.getProperties();
                        if (requestProperties != null) {
                            final GetOperation operation = findMatchingOperation(operations, request);

                            if (operation != null) {
                                final PropertyValue[] operationProperties = operation.getPropertyValues();

                                if (operationProperties != null) {
                                    operation.setPropertyValues(
                                        PropertyUtils.mergePendingValues(operationProperties, requestProperties));
                                }
                            }
                        }
                    }
                }

                final GetEngine getEngine = new GetEngine(client);

                getEngine.processGetOperations(
                    this,
                    ProcessType.PEND,
                    requests[0].getRequestType(),
                    new GetOperation[][] {
                        operations
                    }, getOptions, false, onlineOperation.get(), changePendedFlags.get());

                // Return the number of operations that were successful.
                ret = operations.length;
            }

            if (changePendedFlags.get().contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
                invalidateMappings();
            }

            /*
             * If it was requested by the caller, strip out any Failure objects
             * from the failure set which are of type ItemNotFoundException.
             */
            if (failures.get() != null
                && failures.get().length > 0
                && pendOptions.contains(PendChangesOptions.SUPPRESS_ITEM_NOT_FOUND_FAILURES)) {
                final List<Failure> otherFailures = new ArrayList<Failure>();

                for (final Failure f : failures.get()) {
                    if (f.getCode() == null || !f.getCode().equals(FailureCodes.ITEM_EXISTS_EXCEPTION)) {
                        otherFailures.add(f);
                    }
                }

                failures.set(otherFailures.toArray(new Failure[otherFailures.size()]));
            }

            client.reportFailures(this, failures.get());

        } finally {
            client.getEventEngine().fireOperationCompleted(
                new PendOperationCompletedEvent(EventSource.newFromHere(), this, requests));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED);
        }

        return ret;
    }

    private GetOperation findMatchingOperation(final GetOperation[] operations, final ChangeRequest request) {
        for (final GetOperation operation : operations) {
            if (LocalPath.equals(request.getItemSpec().getItem(), operation.getSourceLocalItem())) {
                return operation;
            }
        }

        return null;
    }

    private boolean hasPropertyChange(final ChangeRequest[] requests) {
        for (final ChangeRequest request : requests) {
            if (request.getRequestType().equals(RequestType.PROPERTY)) {
                return true;
            } else if (request.getProperties() != null && request.getProperties().length > 0) {
                return true;
            }
        }
        return false;
    }

    // Routine operations.

    /**
     * @equivalence pendDelete(paths, recursion, lockLevel, getOptions,
     *              pendOptions, null)
     */
    public int pendDelete(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendDelete(paths, recursion, lockLevel, getOptions, pendOptions, null);
    }

    /**
     * Pend a deletion of one or more items, all at the same lock level.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        local disk or server paths of the items to delete (must not be
     *        <code>null</code> or empty)
     * @param recursion
     *        the type of recursion to apply to the item paths (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendDelete(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[paths.length];

        for (int i = 0; i < paths.length; i++) {
            lockLevels[i] = lockLevel;
        }

        return pendDelete(paths, recursion, lockLevels, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendDelete(paths, recursion, lockLevels, getOptions,
     *              pendOptions, null)
     */
    public int pendDelete(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendDelete(paths, recursion, lockLevels, getOptions, pendOptions, null);
    }

    /**
     * Pend a deletion of one or more items, with possibly differing lock levels
     * for each item.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        local disk or server paths of the items to delete (must not be
     *        <code>null</code> or empty)
     * @param recursion
     *        the type of recursion to apply to the item paths (must not be
     *        <code>null</code>)
     * @param lockLevels
     *        an array containing the lock level for each path to hold during
     *        this change (must not be <code>null</code> or empty)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendDelete(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNullOrEmpty(lockLevels, "lockLevels"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromStrings(paths, RequestType.DELETE, lockLevels, recursion, null);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the ChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendDelete(specs, lockLevel, getOptions, pendOptions, null)
     */
    public int pendDelete(
        final ItemSpec[] specs,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendDelete(specs, lockLevel, getOptions, pendOptions, null);
    }

    /**
     * Pend a deletion of one or more items, all at the same lock level.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param specs
     *        the item specs to delete (must not be <code>null</code> or empty)
     * @param lockLevel
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendDelete(
        final ItemSpec[] specs,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(specs, "specs"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[specs.length];

        for (int i = 0; i < specs.length; i++) {
            lockLevels[i] = lockLevel;
        }

        return pendDelete(specs, lockLevels, getOptions, pendOptions, null);
    }

    /**
     * @equivalence pendDelete(specs, lockLevels, getOptions, pendOptions, null)
     */
    public int pendDelete(
        final ItemSpec[] specs,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendDelete(specs, lockLevels, getOptions, pendOptions, null);
    }

    /**
     * Pend a deletion of one or more items, with possibly differing lock levels
     * for each item.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param specs
     *        the item specs to delete (must not be <code>null</code> or empty)
     * @param lockLevels
     *        an array containing the lock level for each path to hold during
     *        this change (must not be <code>null</code> or empty)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendDelete(
        final ItemSpec[] specs,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(specs, "specs"); //$NON-NLS-1$
        Check.notNullOrEmpty(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromSpecs(specs, RequestType.DELETE, lockLevels, null);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendUndelete(items, lockLevel, getOptions, pendOptions,
     *              null)
     */
    public int pendUndelete(
        final ItemSpec[] items,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendUndelete(items, lockLevel, getOptions, pendOptions, null);
    }

    /**
     * Pend a restore of a deleted items, all at the same lock level.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param items
     *        local disk or server items to undelete (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendUndelete(
        final ItemSpec[] items,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(items, "items"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[items.length];

        for (int i = 0; i < items.length; i++) {
            lockLevels[i] = lockLevel;
        }

        return pendUndelete(items, lockLevels, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendUndelete, items, lockLevels, getOptions, pendOptions,
     *              null)
     */
    public int pendUndelete(
        final ItemSpec[] items,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendUndelete(items, lockLevels, getOptions, pendOptions, null);
    }

    /**
     * Pend a restore of a deleted items, with possibly differing lock levels
     * for each item.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param items
     *        local disk or server items to undelete (must not be
     *        <code>null</code>)
     * @param lockLevels
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendUndelete(
        final ItemSpec[] items,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(items, "paths"); //$NON-NLS-1$
        Check.notNull(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromSpecs(items, RequestType.UNDELETE, lockLevels, null);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendUndelete(item, newName, lockLevel, getOptions,
     *              pendOptions, null)
     */
    public int pendUndelete(
        final ItemSpec item,
        final String newName,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendUndelete(item, newName, lockLevel, getOptions, pendOptions, null);
    }

    /**
     * Pend a restore of a single deleted item and declare a new name for it.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param item
     *        local disk or server item to undelete (must not be
     *        <code>null</code>)
     * @param newName
     *        the new name (short name or full path) for the item (must not be
     *        <code>null</code> or empty)
     * @param lockLevel
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of delete changes pended for the given items.
     */
    public int pendUndelete(
        final ItemSpec item,
        String newName,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNullOrEmpty(newName, "newName"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromSpecs(new ItemSpec[] {
                item
            }, RequestType.UNDELETE, new LockLevel[] {
                lockLevel
            }, null);

            Check.isTrue(requests.length == 1, "requests.length == 1"); //$NON-NLS-1$

            if (ServerPath.isServerPath(newName) == false) {
                newName = LocalPath.canonicalize(newName);
            }

            requests[0].setTargetItem(newName);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendRename(oldPath, newPath, lockLevel, getOptions,
     *              detectTargetItemType, pendOptions, null)
     */
    public int pendRename(
        final String oldPath,
        final String newPath,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final boolean detectTargetItemType,
        final PendChangesOptions pendOptions) {
        return pendRename(oldPath, newPath, lockLevel, getOptions, detectTargetItemType, pendOptions, null);
    }

    /**
     * Pend a rename of an item.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param oldPath
     *        the existing local or server path of the item to rename (must not
     *        be <code>null</code> or empty)
     * @param newPath
     *        the new desired local or server path of the item to rename (must
     *        not be <code>null</code> or empty)
     * @param lockLevel
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param detectTargetItemType
     *        true to detect whether the target path exists and may be
     *        overwritten (and throw an exception if this is the case), false to
     *        ignore any existing item at the target path.
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of rename operations successfully pended.
     */
    public int pendRename(
        final String oldPath,
        final String newPath,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final boolean detectTargetItemType,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(oldPath, "oldPath"); //$NON-NLS-1$
        Check.notNullOrEmpty(newPath, "newPath"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;

        try {
            requests = new ChangeRequest[] {
                new ChangeRequest(
                    new ItemSpec(oldPath, RecursionType.NONE),
                    null,
                    RequestType.RENAME,
                    ItemType.ANY,
                    VersionControlConstants.ENCODING_UNCHANGED,
                    lockLevel,
                    0,
                    newPath,
                    detectTargetItemType)
            };
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendRename(oldPaths, newPaths, lockLevel, getOptions,
     *              detectTargetItemType, pendOptions, null)
     */
    public int pendRename(
        final String[] oldPaths,
        final String[] newPaths,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final boolean detectTargetItemType,
        final PendChangesOptions pendOptions) {
        return pendRename(oldPaths, newPaths, lockLevel, getOptions, detectTargetItemType, pendOptions, null);
    }

    /**
     * Pends renames of multiple items.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param oldPaths
     *        the existing local or server paths of the items to rename (must
     *        not be <code>null</code> or empty)
     * @param newPaths
     *        the new desired local or server paths of the item to rename (must
     *        not be <code>null</code> or empty)
     * @param lockLevel
     *        the lock level to hold during this change (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param detectTargetItemType
     *        true to detect whether the target path exists and may be
     *        overwritten (and throw an exception if this is the case), false to
     *        ignore any existing item at the target path.
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of rename operations successfully pended.
     */
    public int pendRename(
        final String[] oldPaths,
        final String[] newPaths,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final boolean detectTargetItemType,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(oldPaths, "oldPaths"); //$NON-NLS-1$
        Check.notNullOrEmpty(newPaths, "newPaths"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$
        Check.isTrue(oldPaths.length == newPaths.length, "oldPaths.length == newPaths.length"); //$NON-NLS-1$

        final ChangeRequest[] requests = new ChangeRequest[oldPaths.length];

        try {
            for (int i = 0; i < oldPaths.length; i++) {
                Check.notNull(oldPaths[i], "oldPaths[i]"); //$NON-NLS-1$
                Check.notNull(newPaths[i], "newPaths[i]"); //$NON-NLS-1$

                requests[i] = new ChangeRequest(
                    new ItemSpec(oldPaths[i], RecursionType.NONE),
                    LatestVersionSpec.INSTANCE,
                    RequestType.RENAME,
                    ItemType.ANY,
                    VersionControlConstants.ENCODING_UNCHANGED,
                    lockLevel,
                    0,
                    newPaths[i],
                    detectTargetItemType);
            }
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendEdit(specs, lockLevel, fileEncoding, getOptions,
     *              pendOptions, null)
     */
    public int pendEdit(
        final ItemSpec[] specs,
        final LockLevel lockLevel,
        final FileEncoding fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendEdit(specs, lockLevel, fileEncoding, getOptions, pendOptions, null);
    }

    /**
     * Ask the server for permission to begin editing one or more files. This
     * allows callers to use multiple item specs, each with their own recursion
     * specification.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param specs
     *        the item specs (local or server paths) of files or folders to edit
     *        (must not be <code>null</code>)
     * @param lockLevel
     *        the type of lock requested during this edit (must not be
     *        <code>null</code>)
     * @param fileEncoding
     *        the encoding these files are in. If null, the encoding is not
     *        submitted to the server, and the file's encoding will remain the
     *        same until it is checked in.
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file edit requests that were successfully processed
     *         by the server.
     */
    public int pendEdit(
        final ItemSpec[] specs,
        final LockLevel lockLevel,
        final FileEncoding fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(specs, "specs"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[specs.length];
        final FileEncoding[] fileEncodings = new FileEncoding[specs.length];

        for (int i = 0; i < specs.length; i++) {
            lockLevels[i] = lockLevel;
            fileEncodings[i] = fileEncoding;
        }

        return pendEdit(specs, lockLevels, fileEncodings, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendEdit(specs, lockLevels, fileEncoding, getOptions,
     *              pendOptions, null)
     */
    public int pendEdit(
        final ItemSpec[] specs,
        final LockLevel[] lockLevels,
        final FileEncoding[] fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendEdit(specs, lockLevels, fileEncoding, getOptions, pendOptions, null);
    }

    /**
     * Ask the server for permission to begin editing one or more files. This
     * allows callers to use multiple item specs, each with their own recursion
     * specification.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param specs
     *        the item specs (local or server paths) of files or folders to edit
     *        (must not be <code>null</code>)
     * @param lockLevels
     *        the array of lock levels to request for each item spec during this
     *        edit (must not be <code>null</code>)
     * @param fileEncoding
     *        an array of the encoding for each given item spec. If any encoding
     *        in this array is null, the encoding is not submitted to the server
     *        for that file. If the array null, no encodings are submitted to
     *        the server for any file.
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file edit requests that were successfully processed
     *         by the server.
     */
    public int pendEdit(
        final ItemSpec[] specs,
        final LockLevel[] lockLevels,
        final FileEncoding[] fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(specs, "specs"); //$NON-NLS-1$
        Check.notNull(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromSpecs(specs, RequestType.EDIT, lockLevels, fileEncoding);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendEdit(paths, recursion, lockLevel, fileEncoding,
     *              getOptions, pendOptions, null)
     */
    public int pendEdit(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final FileEncoding fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendEdit(paths, recursion, lockLevel, fileEncoding, getOptions, pendOptions, null);
    }

    /**
     * Ask the server for permission to begin editing one or more files.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the local disk or server paths of files or folders to edit (not
     *        null).
     * @param recursion
     *        the type of recursion to apply to the given paths (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the type of lock requested during this edit (must not be
     *        <code>null</code>)
     * @param fileEncoding
     *        the encoding these files are in. If null, the encoding is not
     *        submitted to the server, and the file's encoding will remain the
     *        same until it is checked in.
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file edit requests that were successfully processed
     *         by the server.
     */
    public int pendEdit(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final FileEncoding fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[paths.length];
        final FileEncoding[] fileEncodings = new FileEncoding[paths.length];

        for (int i = 0; i < paths.length; i++) {
            lockLevels[i] = lockLevel;
            fileEncodings[i] = fileEncoding;
        }

        return pendEdit(paths, recursion, lockLevels, fileEncodings, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence pendEdit(paths, recursion, lockLevels, fileEncoding,
     *              getOptions, pendOptions, null)
     */
    public int pendEdit(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel[] lockLevels,
        final FileEncoding[] fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return pendEdit(paths, recursion, lockLevels, fileEncoding, getOptions, pendOptions, null);
    }

    /**
     * Ask the server for permission to begin editing one or more files.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the local disk or server paths of files or folders to edit (not
     *        null).
     * @param recursion
     *        the type of recursion to apply to the given paths (must not be
     *        <code>null</code>)
     * @param lockLevels
     *        an array of lock levels corresponding to each given path (must not
     *        be <code>null</code>)
     * @param fileEncoding
     *        an array of file encodings for each given path. If an encoding is
     *        null, it is not submitted to the server and the file's encoding
     *        will remain the same until it is checked in. If the array is null,
     *        no file encodings are changed.
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file edit requests that were successfully processed
     *         by the server.
     */
    public int pendEdit(
        final String[] paths,
        final RecursionType recursion,
        final LockLevel[] lockLevels,
        final FileEncoding[] fileEncoding,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNull(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNull(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {

            requests = ChangeRequest.fromStrings(paths, RequestType.EDIT, lockLevels, recursion, fileEncoding);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * Pends changes on the passed in path giving it the passed in property.
     *
     * Note, property changes are merged with existing properties on the server.
     * To delete an existing property pass in a property with the desired name
     * and a value of null.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param path
     *        the path to pend the property changes on (must not be
     *        <code>null</code> or empty)
     * @param property
     *        the property to pend as a change (must not be <code>null</code>)
     *
     * @return the number of files with properties changes pended
     */
    public int pendPropertyChange(final String path, final PropertyValue property) {
        return pendPropertyChange(path, property, RecursionType.NONE, LockLevel.UNCHANGED);
    }

    /**
     * Pends changes on the passed in path giving it the passed in properties.
     *
     * Note, property changes are merged with existing properties on the server.
     * To delete an existing property pass in a property with the desired name
     * and a value of null.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param path
     *        the path to pend the property changes on (must not be
     *        <code>null</code> or empty)
     * @param property
     *        the property to pend as a change (must not be <code>null</code>)
     * @param recursion
     *        the amount of recursion to use on the items (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to place on the resulting pending change (must not
     *        be <code>null</code>)
     * @return the number of files with properties changes pended
     */
    public int pendPropertyChange(
        final String path,
        final PropertyValue property,
        final RecursionType recursion,
        final LockLevel lockLevel) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$
        Check.notNull(property, "property"); //$NON-NLS-1$

        return pendPropertyChange(path, new PropertyValue[] {
            property
        }, recursion, lockLevel);
    }

    /**
     * Pends changes on the passed in path giving it the passed in properties.
     *
     * Note, property changes are merged with existing properties on the server.
     * To delete an existing property pass in a property with the desired name
     * and a value of null.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param path
     *        the path to pend the property changes on (must not be
     *        <code>null</code> or empty)
     * @param properties
     *        the properties to pend as changes (must not be <code>null</code>)
     * @param recursion
     *        the amount of recursion to use on the items (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to place on the resulting pending change (must not
     *        be <code>null</code>)
     * @return the number of files with properties changes pended
     */
    public int pendPropertyChange(
        final String path,
        final PropertyValue[] properties,
        final RecursionType recursion,
        final LockLevel lockLevel) {
        Check.notNullOrEmpty(path, "path"); //$NON-NLS-1$
        Check.notNull(properties, "properties"); //$NON-NLS-1$

        return pendPropertyChange(new String[] {
            path
        }, properties, recursion, lockLevel, PendChangesOptions.NONE, null);
    }

    /**
     * Pends changes on the passed in paths giving each of them the passed in
     * properties.
     *
     * Note, property changes are merged with existing properties on the server.
     * To delete an existing property pass in a property with the desired name
     * and a value of null.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the paths to pend the property changes on (must not be
     *        <code>null</code> or empty)
     * @param property
     *        the property to pend as a change (must not be <code>null</code>)
     * @param recursion
     *        the amount of recursion to use on the items (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to place on the resulting pending change (must not
     *        be <code>null</code>)
     * @return the number of files with properties changes pended
     */
    public int pendPropertyChange(
        final String[] paths,
        final PropertyValue property,
        final RecursionType recursion,
        final LockLevel lockLevel) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNull(property, "property"); //$NON-NLS-1$

        return pendPropertyChange(paths, new PropertyValue[] {
            property
        }, recursion, lockLevel, PendChangesOptions.NONE, null);
    }

    /**
     * Pends changes on the passed in paths giving each of them the passed in
     * properties.
     *
     * Note, property changes are merged with existing properties on the server.
     * To delete an existing property pass in a property with the desired name
     * and a value of null.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the paths to pend the property changes on (must not be
     *        <code>null</code> or empty)
     * @param properties
     *        the properties to pend as changes (must not be <code>null</code>)
     * @param recursion
     *        the amount of recursion to use on the items (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to place on the resulting pending change (must not
     *        be <code>null</code>)
     * @param options
     *        the set of {@link PendChangesOptions}. Some relevant options are:
     *        {@link PendChangesOptions#SILENT} (don't return
     *        {@link GetOperation}s for the changes pended).
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation
     * @return the number of files with properties changes pended
     */
    public int pendPropertyChange(
        final String[] paths,
        final PropertyValue[] properties,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final PendChangesOptions options,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNullOrEmpty(properties, "properties"); //$NON-NLS-1$

        if (getClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()) {
            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    this,
                    new VersionControlException(Messages.getString("Workspace.PropertyNotSupportedText")))); //$NON-NLS-1$
            return 0;
        }

        final List<ChangeRequest> changeRequests = new ArrayList<ChangeRequest>(paths.length);
        for (final String path : paths) {
            final ChangeRequest cr = new ChangeRequest(
                new ItemSpec(path, recursion),
                null,
                RequestType.PROPERTY,
                ItemType.ANY,
                VersionControlConstants.ENCODING_UNCHANGED,
                lockLevel,
                0,
                null,
                false);
            cr.setProperties(properties);

            changeRequests.add(cr);
        }

        return pendChanges(
            changeRequests.toArray(new ChangeRequest[changeRequests.size()]),
            GetOptions.NONE,
            options,
            itemPropertyFilters);
    }

    /**
     * Pends changes on the passed in paths giving each of them the passed in
     * properties.
     *
     * Note, property changes are merged with existing properties on the server.
     * To delete an existing property pass in a property with the desired name
     * and a value of null.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param specs
     *        the property changes to make (must not be <code>null</code> or
     *        empty)
     * @param recursion
     *        the amount of recursion to use on the items (must not be
     *        <code>null</code>)
     * @param lockLevel
     *        the lock level to place on the resulting pending change (must not
     *        be <code>null</code>)
     * @param options
     *        the set of {@link PendChangesOptions}. Some relevant options are:
     *        {@link PendChangesOptions#SILENT} (don't return
     *        {@link GetOperation}s for the changes pended).
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation
     * @return the number of files with properties changes pended
     */
    public int pendPropertyChange(
        final ItemProperties[] specs,
        final RecursionType recursion,
        final LockLevel lockLevel,
        final PendChangesOptions options,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(specs, "specs"); //$NON-NLS-1$

        if (getClient().getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()) {
            return 0;
        }

        final List<ChangeRequest> changeRequests = new ArrayList<ChangeRequest>(specs.length);
        for (final ItemProperties spec : specs) {
            final ChangeRequest cr = new ChangeRequest(
                new ItemSpec(spec.getPath(), recursion),
                null,
                RequestType.PROPERTY,
                ItemType.ANY,
                VersionControlConstants.ENCODING_UNCHANGED,
                lockLevel,
                0,
                null,
                false);
            cr.setProperties(spec.getProperties());

            changeRequests.add(cr);
        }

        return pendChanges(
            changeRequests.toArray(new ChangeRequest[changeRequests.size()]),
            GetOptions.NONE,
            options,
            itemPropertyFilters);
    }

    /**
     * @equivalence setLock(paths, lockLevel, recursion, getOptions,
     *              pendOptions, null)
     */
    public int setLock(
        final String[] paths,
        final LockLevel lockLevel,
        final RecursionType recursion,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return setLock(paths, lockLevel, recursion, getOptions, pendOptions, null);
    }

    /**
     * @equivalence setLock(paths, lockLevels, recursion, getOptions,
     *              pendOptions, null)
     */
    public int setLock(
        final String[] paths,
        final LockLevel[] lockLevels,
        final RecursionType recursion,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return setLock(paths, lockLevels, recursion, getOptions, pendOptions, null);
    }

    /**
     * Sets (or unsets) a lock on a file or folder.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the local disk or server paths of files or folders to lock (must
     *        not be <code>null</code> or empty)
     * @param lockLevel
     *        the type of lock requested (must not be <code>null</code>)
     * @param recursion
     *        the type of recursion to apply to the given paths (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file lock requests that were successfully processed
     *         by the server.
     */
    public int setLock(
        final String[] paths,
        final LockLevel lockLevel,
        final RecursionType recursion,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[paths.length];

        for (int i = 0; i < paths.length; i++) {
            lockLevels[i] = lockLevel;
        }

        return setLock(paths, lockLevels, recursion, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * Sets (or unsets) a lock on a file or folder.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param paths
     *        the local disk or server paths of files or folders to lock (must
     *        not be <code>null</code> or empty)
     * @param lockLevels
     *        an array of lock levels requested corresponding to the given file
     *        path (must not be <code>null</code> or empty)
     * @param recursion
     *        the type of recursion to apply to the given paths (must not be
     *        <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file lock requests that were successfully processed
     *         by the server.
     */
    public int setLock(
        final String[] paths,
        final LockLevel[] lockLevels,
        final RecursionType recursion,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(paths, "paths"); //$NON-NLS-1$
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$
        Check.notNullOrEmpty(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromStrings(paths, RequestType.LOCK, lockLevels, recursion, null);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence setLock(itemSpecs, lockLevel, getOptions, pendOptions, null)
     */
    public int setLock(
        final ItemSpec[] itemSpecs,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return setLock(itemSpecs, lockLevel, getOptions, pendOptions, null);
    }

    /**
     * Sets (or unsets) a lock on a file or folder.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param itemSpecs
     *        the item specs to lock (must not be <code>null</code> or empty)
     * @param lockLevel
     *        the type of lock requested (must not be <code>null</code>)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file lock requests that were successfully processed
     *         by the server.
     */
    public int setLock(
        final ItemSpec[] itemSpecs,
        final LockLevel lockLevel,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        final LockLevel[] lockLevels = new LockLevel[itemSpecs.length];

        for (int i = 0; i < lockLevels.length; i++) {
            lockLevels[i] = lockLevel;
        }

        return setLock(itemSpecs, lockLevels, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * @equivalence setLock(itemSpecs, lockLevels, getOptions, pendOptions,
     *              null)
     */
    public int setLock(
        final ItemSpec[] itemSpecs,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions) {
        return setLock(itemSpecs, lockLevels, getOptions, pendOptions, null);
    }

    /**
     * Sets (or unsets) a lock on a file or folder.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param itemSpecs
     *        the item specs to lock (must not be <code>null</code> or empty)
     * @param lockLevels
     *        an array of lock levels requested corresponding to the given file
     *        path (must not be <code>null</code> or empty)
     * @param getOptions
     *        options that affect how files on disk are treated during
     *        processing (must not be <code>null</code>)
     * @param pendOptions
     *        options that affect how items are pended (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of file lock requests that were successfully processed
     *         by the server.
     */
    public int setLock(
        final ItemSpec[] itemSpecs,
        final LockLevel[] lockLevels,
        final GetOptions getOptions,
        final PendChangesOptions pendOptions,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNullOrEmpty(lockLevels, "lockLevels"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        ChangeRequest[] requests = null;
        try {
            requests = ChangeRequest.fromSpecs(itemSpecs, RequestType.LOCK, lockLevels, null);
        } catch (final ChangeRequestValidationException e) {
            /*
             * This is thrown if a file is in the way of the change request, and
             * we have detected it during the AChangeRequest construction.
             */
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), this, e));

            return 0;
        }

        return pendChanges(requests, getOptions, pendOptions, itemPropertyFilters);
    }

    /**
     * Finds the closest existing .tfignore file to the specified local item, or
     * creates one if none was found, and adds a relative path exclusion item
     * that will exclude the local item.
     *
     * @param localItem
     *        the local item to find or create the .tfignore file by and add an
     *        exclusion for (must not be <code>null</code> or empty)
     * @throws FileNotFoundException
     *         if the specified ignore file directory does not exist or is not a
     *         directory
     * @throws IOException
     *         if there was an error reading or writing the ignore file
     * @return the full path to the .tfignore file that was modified
     */
    public String addIgnoreFileExclusionAuto(final String localItem) throws FileNotFoundException, IOException {
        Check.notNullOrEmpty(localItem, "localItem"); //$NON-NLS-1$

        return addIgnoreFileExclusionAuto(localItem, null);
    }

    /**
     * Finds the closest existing .tfignore file to the specified local item, or
     * creates one if none was found, and adds the specified exclusion text.
     *
     * @param localItem
     *        the local item to find or create the .tfignore file by (must not
     *        be <code>null</code> or empty)
     * @param exclusion
     *        ignore glob to add to the .tfignore file (if <code>null</code>, an
     *        exclusion for the specified local item is calculated
     *        automatically)
     * @throws FileNotFoundException
     *         if the specified ignore file directory does not exist or is not a
     *         directory
     * @throws IOException
     *         if there was an error reading or writing the ignore file
     * @return the full path to the .tfignore file that was modified
     */
    public String addIgnoreFileExclusionAuto(final String localItem, String exclusion)
        throws FileNotFoundException,
            IOException {
        Check.notNullOrEmpty(localItem, "localItem"); //$NON-NLS-1$

        // Get the closest mapping to the path provided.
        final WorkingFolder wf = getClosestMappingForLocalPath(localItem);

        if (wf == null) {
            throw new ItemNotMappedException(
                MessageFormat.format(Messages.getString("Workspace.ItemNotMappedExceptionFormat"), localItem)); //$NON-NLS-1$
        }

        String currentItem = LocalPath.getParent(localItem);
        String itemParent;

        while (LocalPath.isChild(wf.getLocalItem(), currentItem)) {
            final String ignoreFilePath = LocalPath.combine(currentItem, LocalItemExclusionEvaluator.IGNORE_FILE_NAME);

            if (new File(ignoreFilePath).exists() && isLocalPathMapped(ignoreFilePath)) {
                break;
            }

            itemParent = LocalPath.getParent(currentItem);

            if (ServerPath.isRootFolder(getMappedServerPath(itemParent))
                || !LocalPath.isChild(wf.getLocalItem(), itemParent)) {
                break;
            }

            currentItem = itemParent;
        }

        if (exclusion == null) {
            exclusion = LocalPath.makeRelative(localItem, currentItem);

            // Convert to Windows separators (backslash) on all platforms
            exclusion = LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR
                + exclusion.replace(File.separatorChar, LocalPath.TFS_PREFERRED_LOCAL_PATH_SEPARATOR);
        }

        return addIgnoreFileExclusion(exclusion, currentItem);
    }

    /**
     * Adds an ignore file exclusion to the .tfignore file in the specified
     * directory, creating the file if necessary.
     *
     * @param exclusion
     *        ignore glob to add to the .tfignore file (must not be
     *        <code>null</code> or empty)
     * @param ignoreFileDirectory
     *        directory whose .tfignore file should be used (must not be
     *        <code>null</code> or empty)
     * @throws FileNotFoundException
     *         if the specified ignore file directory does not exist or is not a
     *         directory
     * @throws IOException
     *         if there was an error reading or writing the ignore file
     * @return the full path to the .tfignore file that was modified
     */
    public String addIgnoreFileExclusion(final String exclusion, final String ignoreFileDirectory)
        throws FileNotFoundException,
            IOException {
        Check.notNullOrEmpty(exclusion, "exclusion"); //$NON-NLS-1$
        Check.notNullOrEmpty(ignoreFileDirectory, "ignoreFileDirectory"); //$NON-NLS-1$

        final File ignoreFileDirectoryFile = new File(ignoreFileDirectory);

        if (!ignoreFileDirectoryFile.exists() || !ignoreFileDirectoryFile.isDirectory()) {
            throw new FileNotFoundException(
                MessageFormat.format(
                    Messages.getString("Workspace.PathDoesNotExistOrIsNotDirectoryFormat"), //$NON-NLS-1$
                    ignoreFileDirectoryFile));
        }

        final File ignoreFile = new File(ignoreFileDirectoryFile, LocalItemExclusionEvaluator.IGNORE_FILE_NAME);

        if (!isLocalPathMapped(ignoreFile.getAbsolutePath())) {
            throw new ItemNotMappedException(
                MessageFormat.format(Messages.getString("Workspace.ItemNotMappedExceptionFormat"), ignoreFile)); //$NON-NLS-1$
        }

        final boolean exists = ignoreFile.exists();

        if (!exists) {
            // Write the file out in UTF-8 (.NET impl writes BOM but we don't
            // control that in Java)
            FileOutputStream outputStream = null;
            OutputStreamWriter streamWriter = null;
            try {
                outputStream = new FileOutputStream(ignoreFile, false);
                streamWriter = new OutputStreamWriter(outputStream, "UTF-8"); //$NON-NLS-1$

                streamWriter.write(getTFIgnoreHeader());
                streamWriter.write(NewlineUtils.PLATFORM_NEWLINE);
                streamWriter.write(NewlineUtils.PLATFORM_NEWLINE);
                streamWriter.write(exclusion);
                streamWriter.write(NewlineUtils.PLATFORM_NEWLINE);
            } finally {
                if (streamWriter != null) {
                    IOUtils.closeSafely(streamWriter);
                }
                if (outputStream != null) {
                    IOUtils.closeSafely(outputStream);
                }
            }
        } else {
            // Discover the current encoding of the file and use that to write
            // out the data

            final FileEncoding tfsEncoding =
                FileEncodingDetector.detectEncoding(ignoreFile.getAbsolutePath(), FileEncoding.AUTOMATICALLY_DETECT);

            Charset charset = CodePageMapping.getCharset(tfsEncoding.getCodePage(), false);

            if (charset == null) {
                /*
                 * getCharset() couldn't find a Java Charset for the code page.
                 * This can happen if the file was detected as
                 * FileEncoding.BINARY.
                 */
                charset = CodePageMapping.getCharset(FileEncoding.getDefaultTextEncoding().getCodePage());
            }

            boolean writeNewLine = false;

            final FileInputStream inputStream = new FileInputStream(ignoreFile);
            try {
                final String existingContent = IOUtils.toString(inputStream, charset.name());

                if (existingContent.length() > 0) {
                    // Covers Unix (bare \n) and Windows (\r\n).
                    final char lastCharacter = existingContent.charAt(existingContent.length() - 1);
                    if (lastCharacter != '\n') {
                        writeNewLine = true;
                    }
                }
            } finally {
                if (inputStream != null) {
                    IOUtils.closeSafely(inputStream);
                }
            }

            // Write the file out in UTF-8 (.NET impl writes BOM but we don't
            // control that in Java)
            FileOutputStream outputStream = null;
            OutputStreamWriter streamWriter = null;
            try {
                // Append
                outputStream = new FileOutputStream(ignoreFile, true);
                streamWriter = new OutputStreamWriter(outputStream, charset);

                if (writeNewLine) {
                    streamWriter.write(NewlineUtils.PLATFORM_NEWLINE);
                }

                streamWriter.write(exclusion);
                streamWriter.write(NewlineUtils.PLATFORM_NEWLINE);
            } finally {
                if (streamWriter != null) {
                    IOUtils.closeSafely(streamWriter);
                }
                if (outputStream != null) {
                    IOUtils.closeSafely(outputStream);
                }
            }
        }

        if (WorkspaceLocation.LOCAL == this.getLocation()) {
            /*
             * In case the caller calls QueryPendingChanges immediately after
             * this call (as the Promote Candidate Changes dialog does) and the
             * watcher does not pick up the change in time.
             */
            getWorkspaceWatcher().markPathChanged(ignoreFile.getAbsolutePath());
        }

        if (!exists) {
            // Always UTF-8 encoding if the file didn't already exist
            pendAdd(new String[] {
                ignoreFile.getAbsolutePath()
            }, false, FileEncoding.UTF_8, LockLevel.UNCHANGED, GetOptions.NONE, PendChangesOptions.NONE);
        }

        return ignoreFile.getAbsolutePath();
    }

    /**
     * Queries the server for conflicts in this workspace that match the given
     * filters.
     *
     * @param pathFilters
     *        the paths to include in the conflict search. If <code>null</code>,
     *        the entire workspace is queried for conflicts.
     * @param recursive
     *        if true, paths given will match their subitems too. If false, no
     *        recursion is performed.
     * @return the conflicts that matched the given path filters (an empty array
     *         if no conflicts found, never <code>null</code>).
     */
    public Conflict[] queryConflicts(final String[] pathFilters, final boolean recursive) {
        ItemSpec[] specs = null;

        /*
         * Convert to ItemSpec and call that method.
         */
        if (pathFilters != null && pathFilters.length > 0) {
            specs = new ItemSpec[pathFilters.length];
            for (int i = 0; i < pathFilters.length; i++) {
                specs[i] = new ItemSpec(pathFilters[i], (recursive) ? RecursionType.FULL : RecursionType.NONE);
            }
        }

        return queryConflicts(specs);
    }

    /**
     * Queries the server for any conflicts in this workspace that match the
     * given filters.
     *
     * @param itemSpecs
     *        The {@link ItemSpec}s to search for conflicts. If
     *        <code>null</code>, the entire workspace is queried for conflicts.
     * @return the conflicts that matched the given items (an empty array if no
     *         conflicts found, never <code>null</code>).
     */
    public Conflict[] queryConflicts(final ItemSpec[] itemSpecs) {
        final Conflict[] ret = client.getWebServiceLayer().queryConflicts(getName(), getOwnerName(), itemSpecs);

        Arrays.sort(ret);

        return ret;
    }

    /**
     * @equivalence resolveConflict(conflict, new AtomicReference<Conflict[]>())
     */
    public void resolveConflict(final Conflict conflict) {
        resolveConflict(conflict, new AtomicReference<Conflict[]>());
    }

    /**
     * @equivalence resolveConflict(conflict, resolvedConflicts, null)
     */
    public void resolveConflict(final Conflict conflict, final AtomicReference<Conflict[]> resolvedConflicts) {
        resolveConflict(conflict, resolvedConflicts, null);
    }

    /**
     * @equivalence resolveConflict(conflict, resolvedConflicts,
     *              itemPropertyFilters, null, null, null, null)
     */
    public void resolveConflict(
        final Conflict conflict,
        final AtomicReference<Conflict[]> resolvedConflicts,
        final String[] itemPropertyFilters) {
        resolveConflict(conflict, resolvedConflicts, itemPropertyFilters, null, null, null, null);
    }

    /**
     * @equivalence resolveConflicts(new Conflict[] { conflict },
     *              itemPropertyFilters, ResolveErrorOptions.THROW_ON_ERROR,
     *              resolvedConflicts, externalToolset, finishedHandler,
     *              capturedStandardOutput, capturedStandardError)
     */
    public void resolveConflict(
        final Conflict conflict,
        final AtomicReference<Conflict[]> resolvedConflicts,
        final String[] itemPropertyFilters,
        final ExternalToolset externalToolset,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        client.getEventEngine().fireOperationStarted(
            new ResolveConflictStartedEvent(EventSource.newFromHere(), this, conflict));

        try {
            resolveConflicts(
            new Conflict[] {
                conflict
            },
                itemPropertyFilters,
                ResolveErrorOptions.THROW_ON_ERROR,
                resolvedConflicts,
                externalToolset,
                finishedHandler,
                capturedStandardOutput,
                capturedStandardError);
        } finally {
            client.getEventEngine().fireOperationCompleted(
                new ResolveConflictCompletedEvent(EventSource.newFromHere(), this, conflict));
        }
    }

    /**
     * @equivalence resolveConflicts(conflicts, itemPropertyFilters,
     *              ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR,
     *              resolvedConflicts)
     */
    public void resolveConflicts(
        final Conflict[] conflicts,
        final String[] itemPropertyFilters,
        final AtomicReference<Conflict[]> resolvedConflicts) {
        resolveConflicts(
            conflicts,
            itemPropertyFilters,
            ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR,
            resolvedConflicts);
    }

    /**
     * @equivalence resolveConflicts(conflicts, itemPropertyFilters,
     *              errorOptions, resolvedConflicts, null, null, null, null)
     */
    public void resolveConflicts(
        final Conflict[] conflicts,
        final String[] itemPropertyFilters,
        final ResolveErrorOptions errorOptions,
        final AtomicReference<Conflict[]> resolvedConflicts) {
        resolveConflicts(conflicts, itemPropertyFilters, errorOptions, resolvedConflicts, null, null, null, null);
    }

    /**
     * Attempts to resolve conflicts according to their Resolution property. If
     * a conflict is successfully resolved, calls to
     * {@link Conflict#isResolved()} will return true. If resolving these
     * conflicts caused other conflicts to be deleted besides the current
     * conflicts, then the list of other deleted conflicts will be returned.
     *
     * Note: all resolution options for the conflicts must be the same.
     *
     * @param conflicts
     *        The conflicts to resolve (not <code>null</code>)
     * @param itemAttributeFilters
     *        A list of versioned item properties to return with each get
     *        operation
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation
     * @param errorOptions
     *        how to proceed on errors (must not be <code>null</code>)
     * @param resolvedConflicts
     *        A container holding an array of additional conflicts that were
     *        resolved by the server when the given conflict was resolved (must
     *        not be <code>null</code>). The container will hold an empty array
     *        when no additional conflicts were resolved.
     */
    public void resolveConflicts(
        final Conflict[] conflicts,
        String[] itemPropertyFilters,
        final ResolveErrorOptions errorOptions,
        final AtomicReference<Conflict[]> resolvedConflicts,
        final ExternalToolset externalToolset,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) {
        Check.notNull(conflicts, "conflicts"); //$NON-NLS-1$
        Check.notNull(errorOptions, "errorOptions"); //$NON-NLS-1$
        Check.notNull(resolvedConflicts, "resolvedConflicts"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        /* Verify that we don't have mixed resolution options. */
        Resolution resolution = Resolution.NONE;

        for (final Conflict conflict : conflicts) {
            Check.notNull(conflict, "conflict"); //$NON-NLS-1$

            if (Resolution.NONE.equals(resolution)) {
                resolution = conflict.getResolution();
                continue;
            }

            if (!resolution.equals(conflict.getResolution())) {
                throw new VersionControlException(
                    Messages.getString("Workspace.ResolveConflictsRequiresOneResolutionException")); //$NON-NLS-1$
            }
        }

        client.getEventEngine().fireOperationStarted(
            new ResolveConflictsStartedEvent(EventSource.newFromHere(), this, conflicts));

        try {
            resolveConflictsInternal(
                conflicts,
                itemPropertyFilters,
                errorOptions,
                resolvedConflicts,
                externalToolset,
                finishedHandler,
                capturedStandardOutput,
                capturedStandardError);
        } finally {
            client.getEventEngine().fireOperationCompleted(
                new ResolveConflictsCompletedEvent(EventSource.newFromHere(), this, conflicts));
        }
    }

    private void resolveConflictsInternal(
        final Conflict[] conflicts,
        final String[] itemPropertyFilters,
        final ResolveErrorOptions errorOptions,
        final AtomicReference<Conflict[]> resolvedConflicts,
        final ExternalToolset externalToolset,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) {
        final List<Conflict> conflictsToResolve = new ArrayList<Conflict>();

        for (final Conflict conflict : conflicts) {
            Check.notNull(conflict, "conflict"); //$NON-NLS-1$

            if (conflict.isResolved()) {
                continue;
            }

            /*
             * If auto merge is not allowed on this conflict, and the
             * MergedFileName is empty, it means the user tried AcceptMerge from
             * the command line or the UI, throw.
             */
            if (Resolution.ACCEPT_MERGE.equals(conflict.getResolution())
                && (conflict.getMergedFileName() == null || conflict.getMergedFileName().length() == 0)
                && conflict.getConflictOptions().contains(ConflictOptions.DISALLOW_AUTO_MERGE)) {
                /*
                 * YourServerItem may be null if this is a writable conflict
                 * against a file that is not in the workspace.
                 */
                final AutoMergeDisallowedException exception = new AutoMergeDisallowedException(
                    MessageFormat.format(
                        Messages.getString("Workspace.AutoMergeDisallowedFormat"), //$NON-NLS-1$
                        conflict.getYourServerItem() != null ? conflict.getYourServerItem()
                            : conflict.getTheirServerItem()));

                if (ResolveErrorOptions.THROW_ON_ERROR.equals(errorOptions)) {
                    throw exception;
                } else if (ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR.equals(errorOptions)) {
                    client.getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(EventSource.newFromHere(), this, exception));
                }

                continue;
            }

            if (Resolution.ACCEPT_MERGE.equals(conflict.getResolution()) && conflict.canMergeContent()) {
                /*
                 * If the file didn't exist at the specified version there is no
                 * actual content to merge.
                 */
                if (!conflict.theirFileExists()) {
                    log.trace("Skipping content merge, file does not exist."); //$NON-NLS-1$
                } else {
                    /*
                     * If the caller has not already performed the content
                     * merge, use the auto-merge.
                     */
                    if (conflict.getMergedFileName() == null || conflict.getMergedFileName().length() == 0) {
                        /*
                         * Auto-merge the contents and only accept the result if
                         * there are no conflicts.
                         */
                        try {
                            final MergeEngine me = new MergeEngine(this, client, externalToolset);
                            final boolean isProblem = !me.mergeContent(
                                conflict,
                                false,
                                finishedHandler,
                                capturedStandardOutput,
                                capturedStandardError);

                            // Stop now if there was a problem merging.
                            if (isProblem
                                || conflict.getContentMergeSummary() != null
                                    && !conflict.getResolutionOptions().isAcceptMergeWithConflicts()
                                    && conflict.getContentMergeSummary().getTotalConflictingLines() != 0) {
                                /*
                                 * Only delete the mergedFile if there were no
                                 * exceptions so that the user doesn't lose work
                                 * if something blows up or the user hits
                                 * Ctrl-C.
                                 */
                                if (conflict.getMergedFileName() != null && conflict.getMergedFileName().length() > 0) {
                                    new File(conflict.getMergedFileName()).delete();
                                }
                                conflict.setMergedFileName(null);

                                log.debug("Failed content merge."); //$NON-NLS-1$

                                continue;
                            }
                        } catch (final Exception ex) {
                            if (conflict.getMergedFileName() != null) {
                                client.getEventEngine().fireNonFatalError(
                                    new NonFatalErrorEvent(
                                        EventSource.newFromHere(),
                                        this,
                                        new VersionControlException(
                                            MessageFormat.format(
                                                Messages.getString("Workspace.MergeSavedFormat"), //$NON-NLS-1$
                                                conflict.getMergedFileName()))));
                            } else if (ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR.equals(errorOptions)) {
                                client.getEventEngine().fireNonFatalError(
                                    new NonFatalErrorEvent(EventSource.newFromHere(), this, ex));
                            } else if (ResolveErrorOptions.THROW_ON_ERROR.equals(errorOptions)) {
                                throw new VersionControlException(ex);
                            }

                            continue;
                        }
                    }
                }
            }

            if (Resolution.ACCEPT_MERGE.equals(conflict.getResolution())
                && conflict.isPropertyConflict()
                && conflict.getResolutionOptions().getAcceptMergeProperties() == null) {
                try {
                    /* Perform the automerge if it hasn't been done yet. */
                    final PropertiesMergeSummary propertiesMergeSummary = conflict.mergeProperties(this);

                    if (propertiesMergeSummary.getTotalConflicts() != 0) {
                        continue;
                    }

                    conflict.getResolutionOptions().setAcceptMergeProperties(
                        propertiesMergeSummary.getMergedProperties());
                } catch (final Exception ex) {
                    if (ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR.equals(errorOptions)) {
                        client.getEventEngine().fireNonFatalError(
                            new NonFatalErrorEvent(EventSource.newFromHere(), this, ex));
                    } else if (ResolveErrorOptions.THROW_ON_ERROR.equals(errorOptions)) {
                        throw new VersionControlException(ex);
                    }
                }
            }

            conflictsToResolve.add(conflict);
        }

        client.resolveConflicts(
            this,
            conflictsToResolve.toArray(new Conflict[conflictsToResolve.size()]),
            itemPropertyFilters,
            errorOptions,
            resolvedConflicts);
    }

    /**
     * Get all pending changes for items in this workspace. Download URLs for
     * the items will not be generated.
     *
     * @return a pending set including all the pending changes, null if there
     *         are no pending changes in this workspace.
     */
    public PendingSet getPendingChanges() {
        return getPendingChanges(new String[] {
            ServerPath.ROOT
        }, RecursionType.FULL, false);
    }

    /**
     * @equivalence getPendingChanges(serverPaths, recursionType,
     *              includeDownloadInfo, null)
     */
    public PendingSet getPendingChanges(
        final String[] serverPaths,
        final RecursionType recursionType,
        final boolean includeDownloadInfo) {
        return getPendingChanges(serverPaths, recursionType, includeDownloadInfo, null);
    }

    /**
     * Get all pending changes for the given server item.
     *
     * @param serverPaths
     *        the server items (files or directories) to get pending changes
     *        for. Pass ServerPath.ROOT with RecursionType.Full to match all
     *        (must not be <code>null</code> or empty).
     * @param recursionType
     *        the type of recursion to apply to the given server paths (not
     *        null).
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return a pending set including all the pending changes, null if there
     *         are no pending changes
     */
    public PendingSet getPendingChanges(
        final String[] serverPaths,
        final RecursionType recursionType,
        final boolean includeDownloadInfo,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(serverPaths, "serverPaths"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$

        final PendingSet[] ret = queryPendingSets(
            serverPaths,
            recursionType,
            getName(),
            getOwnerName(),
            includeDownloadInfo,
            itemPropertyFilters);

        if (ret.length == 0) {
            return null;
        }

        return ret[0];
    }

    /**
     * @equivalence getPendingChanges(specs, includeDownloadInfo, null)
     */
    public PendingSet getPendingChanges(final ItemSpec[] specs, final boolean includeDownloadInfo) {
        return getPendingChanges(specs, includeDownloadInfo, null);
    }

    /**
     * Get all pending changes for the given server items.
     *
     * @param specs
     *        {@link ItemSpec}s to get pending changes for(must not be
     *        <code>null</code> or empty)
     * @param includeDownloadInfo
     *        if true, the server will include the information needed to
     *        download files. Only set this to true if you are going to be
     *        downloading the files using the objects that are returned. The
     *        call will be faster and require less bandwidth when this parameter
     *        is false (default for overloads that don't specify it)
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return a pending set including all the pending changes, null if there
     *         are no pending changes for the given specs
     */
    public PendingSet getPendingChanges(
        final ItemSpec[] specs,
        final boolean includeDownloadInfo,
        final String[] itemPropertyFilters) {
        Check.notNullOrEmpty(specs, "specs"); //$NON-NLS-1$

        final PendingSet[] ret =
            queryPendingSets(specs, getName(), getOwnerName(), includeDownloadInfo, itemPropertyFilters);

        if (ret.length == 0) {
            return null;
        }

        return ret[0];
    }

    /**
     * @equivalence getPendingChangesWithCandidates(itemSpecs,
     *              includeDownloadInfo, candidateChanges, null)
     */
    public PendingChange[] getPendingChangesWithCandidates(
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        final AtomicReference<PendingChange[]> candidateChanges) {
        return getPendingChangesWithCandidates(itemSpecs, includeDownloadInfo, candidateChanges, null);
    }

    /**
     * Gets all of the pending changes and candidate changes.
     *
     * @param specs
     *        {@link ItemSpec}s to get pending changes for (must not be
     *        <code>null</code> or empty)
     * @param includeDownloadInfo
     *        if true, the server will include the information needed to
     *        download files. Only set this to true if you are going to be
     *        downloading the files using the objects that are returned. The
     *        call will be faster and require less bandwidth when this parameter
     *        is false (default for overloads that don't specify it)
     * @param candidateChanges
     *        a a reference to receive the candidate changes (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of property names to return on the pending change object if
     *        they exist (may be <code>null</code>)
     * @return a pending set including all the pending changes,
     *         <code>null</code> or an empty array if there are no pending
     *         changes for the given specs
     */
    public PendingChange[] getPendingChangesWithCandidates(
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        final AtomicReference<PendingChange[]> candidateChanges,
        String[] itemPropertyFilters) {
        Check.notNullOrEmpty(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(candidateChanges, "candidateChanges"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();
        final PendingSet[] pendingSets = getClient().getWebServiceLayer().queryPendingSets(
            getName(),
            getOwnerName(),
            getName(),
            getOwnerName(),
            itemSpecs,
            includeDownloadInfo,
            failures,
            true,
            itemPropertyFilters);

        Check.isTrue(pendingSets.length <= 1, "How did we get more than 1 pending set for a workspace?"); //$NON-NLS-1$

        // report failures
        getClient().reportFailures(this, failures.get());

        if (pendingSets.length == 0) {
            candidateChanges.set(new PendingChange[0]);
            return new PendingChange[0];
        } else {
            candidateChanges.set(pendingSets[0].getCandidatePendingChanges());
            if (candidateChanges.get() == null) {
                candidateChanges.set(new PendingChange[0]);
            }
            return pendingSets[0].getPendingChanges();
        }
    }

    /**
     * @equivalence queryPendingSets(serverOrLocalPaths, recursionType,
     *              queryWorkspaceName, queryWorkspaceOwner,
     *              includeDownloadInfo, null)
     */
    public PendingSet[] queryPendingSets(
        final String[] serverOrLocalPaths,
        final RecursionType recursionType,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        final boolean includeDownloadInfo) {
        return queryPendingSets(
            serverOrLocalPaths,
            recursionType,
            queryWorkspaceName,
            queryWorkspaceOwner,
            includeDownloadInfo,
            null);
    }

    /**
     * @see VersionControlClient#queryPendingSets(String[], RecursionType,
     *      boolean, String, String, String[])
     */
    public PendingSet[] queryPendingSets(
        final String[] serverOrLocalPaths,
        final RecursionType recursionType,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        final boolean includeDownloadInfo,
        final String[] itemPropertyFilters) {
        return client.queryPendingSets(
            serverOrLocalPaths,
            recursionType,
            includeDownloadInfo,
            queryWorkspaceName,
            queryWorkspaceOwner,
            itemPropertyFilters);
    }

    /**
     * @equivalence queryPendingSets(itemSpecs, queryWorkspaceName,
     *              queryWorkspaceOwner, includeDownloadInfo, null)
     */
    public PendingSet[] queryPendingSets(
        final ItemSpec[] itemSpecs,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        final boolean includeDownloadInfo) {
        return queryPendingSets(itemSpecs, queryWorkspaceName, queryWorkspaceOwner, includeDownloadInfo, null);
    }

    /**
     * @see VersionControlClient#queryPendingSets(ItemSpec[], boolean, String,
     *      String, boolean, String[])
     */
    public PendingSet[] queryPendingSets(
        final ItemSpec[] itemSpecs,
        final String queryWorkspaceName,
        final String queryWorkspaceOwner,
        final boolean includeDownloadInfo,
        final String[] itemPropertyFilters) {
        return client.queryPendingSets(
            itemSpecs,
            includeDownloadInfo,
            queryWorkspaceName,
            queryWorkspaceOwner,
            false,
            itemPropertyFilters);
    }

    /**
     * @equivalence queryShelvedChanges(shelvesetName, shelvesetOwner,
     *              itemSpecs, false)
     */
    public PendingSet[] queryShelvedChanges(
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs) {
        return queryShelvedChanges(shelvesetName, shelvesetOwner, itemSpecs, false);
    }

    /**
     * @equivalence queryShelvedChanges(shelvesetName, shelvesetOwner,
     *              itemSpecs, includeDownloadInfo, null)
     */
    public PendingSet[] queryShelvedChanges(
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo) {
        return queryShelvedChanges(shelvesetName, shelvesetOwner, itemSpecs, includeDownloadInfo, null);
    }

    /**
     * @see VersionControlClient#queryShelvedChanges(String, String, ItemSpec[],
     *      boolean, String[])
     */
    public PendingSet[] queryShelvedChanges(
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs,
        final boolean includeDownloadInfo,
        final String[] itemPropertyFilters) {
        return client.queryShelvedChanges(
            shelvesetName,
            shelvesetOwner,
            itemSpecs,
            includeDownloadInfo,
            itemPropertyFilters);
    }

    /**
     * Update a workspace.
     *
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer name or <code>null</code> to keep the existing
     *        value
     */
    public void update(final String newName, final String newComment, final WorkingFolder[] newMappings) {
        update(newName, newComment, newMappings, false);
    }

    /**
     * Update a workspace.
     *
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer name or <code>null</code> to keep the existing
     *        value
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping
     */
    public void update(
        final String newName,
        final String newComment,
        final WorkingFolder[] newMappings,
        final boolean removeUnparentedCloaks) {
        update(newName, null, newComment, newMappings, removeUnparentedCloaks);
    }

    /**
     * Update a workspace.
     *
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping
     */
    public void update(
        final String newName,
        final String newOwner,
        final String newComment,
        final WorkingFolder[] newMappings,
        final boolean removeUnparentedCloaks) {
        update(newName, newOwner, newComment, null, newMappings, null, removeUnparentedCloaks);
    }

    /**
     * Update a workspace.
     *
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newPermissionProfile
     *        the new permission profile or <code>null</code> to keep the
     *        existing value
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping
     */
    public void update(
        final String newName,
        final String newOwner,
        final String newComment,
        final String newComputer,
        final WorkingFolder[] newMappings,
        final WorkspacePermissionProfile newPermissionProfile,
        final boolean removeUnparentedCloaks) {
        update(
            newName,
            newOwner,
            newComment,
            newComputer,
            newMappings,
            newPermissionProfile,
            removeUnparentedCloaks,
            null);
    }

    /**
     * Update a workspace.
     *
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newPermissionProfile
     *        the new permission profile or <code>null</code> to keep the
     *        existing value
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping
     * @param newOptions
     *        the new options or <code>null</code> to keep the existing value
     */
    public void update(
        final String newName,
        final String newOwner,
        final String newComment,
        final String newComputer,
        final WorkingFolder[] newMappings,
        final WorkspacePermissionProfile newPermissionProfile,
        final boolean removeUnparentedCloaks,
        final WorkspaceOptions newOptions) {
        update(
            newName,
            newOwner,
            newComment,
            newComputer,
            newMappings,
            newPermissionProfile,
            removeUnparentedCloaks,
            newOptions,
            null);
    }

    /**
     * Update a workspace.
     *
     * @param newName
     *        the new name or <code>null</code> to keep the existing value
     * @param newOwner
     *        the new owner or <code>null</code> to keep the existing value
     * @param newComment
     *        the new comment or <code>null</code> to keep the existing value
     * @param newComputer
     *        the new computer or <code>null</code> to keep the existing value
     * @param newMappings
     *        the new mappings or <code>null</code> to keep the existing value
     * @param newPermissionProfile
     *        the new permission profile or <code>null</code> to keep the
     *        existing value
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping
     * @param newOptions
     *        the new options or <code>null</code> to keep the existing value
     * @param newLocation
     *        the new workspace location or <code>null</code> to keep the
     *        existing value
     */
    public void update(
        final String newName,
        final String newOwner,
        final String newComment,
        final String newComputer,
        final WorkingFolder[] newMappings,
        final WorkspacePermissionProfile newPermissionProfile,
        final boolean removeUnparentedCloaks,
        final WorkspaceOptions newOptions,
        final WorkspaceLocation newLocation) {
        client.updateWorkspace(
            this,
            newName,
            newOwner,
            newComment,
            newMappings,
            newComputer,
            newPermissionProfile,
            removeUnparentedCloaks,
            newOptions,
            newLocation);
    }

    /**
     * Update the name of the computer in the repository. This is useful when
     * the user has renamed the machine on which the workspace is hosted.
     */
    public void updateComputerName() {
        update(null, null, null, LocalHost.getShortName(), null, null, false);
    }

    /**
     * An internal helper method called by {@link RuntimeWorkspaceCache} to
     * update properties of a {@link Workspace} object when new information has
     * been retrieved from the server.
     *
     * @param updateSource
     *        the workspace to update from (must not be <code>null</code>)
     */
    public void updateFromWorkspace(final Workspace updateSource) {
        Check.notNull(updateSource, "updateSource"); //$NON-NLS-1$

        /*
         * Create a copy of "this", which will take a reference to the current
         * (treated as immutable) web service object. This prevents the web
         * service object changing as we use it if another thread is
         * concurrently updating "this".
         */
        final Workspace currentWorkspace = new Workspace(this);

        boolean invalidateLocalWorkspaceScanner = false;

        boolean cachedPropertiesModified = !Workspace.matchName(currentWorkspace.getName(), updateSource.getName())
            || !Workspace.matchOwner(currentWorkspace.getOwnerName(), updateSource.getOwnerName())
            || !Workspace.matchOwner(currentWorkspace.getOwnerDisplayName(), updateSource.getOwnerDisplayName())
            || !Workspace.matchComment(currentWorkspace.getComment(), updateSource.getComment())
            || !Workspace.matchComputer(currentWorkspace.getComputer(), updateSource.getComputer())
            || !Workspace.matchSecurityToken(currentWorkspace.getSecurityToken(), updateSource.getSecurityToken())
            || currentWorkspace.getLocation() != updateSource.getLocation()
            || !currentWorkspace.getOptions().equals(updateSource.getOptions());

        if (updateSource.getLocation() == WorkspaceLocation.LOCAL
            && !WorkingFolder.areSetsEqual(currentWorkspace.getFolders(), updateSource.getFolders())) {
            invalidateLocalWorkspaceScanner = true;
        }

        // Get the workspace info of the current workspace.
        final WorkspaceInfo wi =
            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).getCache().getWorkspace(
                getClient().getServerGUID(),
                currentWorkspace.getName(),
                currentWorkspace.getOwnerName());

        if (!cachedPropertiesModified) {
            if (currentWorkspace.getLocation() == WorkspaceLocation.LOCAL) {
                // If the workspace is a local workspace, include the
                // working folders in the set of cached properties.
                cachedPropertiesModified = invalidateLocalWorkspaceScanner
                    || !WorkingFolder.areSetsEqual(currentWorkspace.getFolders(), updateSource.getFolders());
            } else {
                // If the workspace is a server workspace, check to see if
                // the set of mapped local paths has changed.
                if (null != wi) {
                    cachedPropertiesModified = !WorkspaceInfo.areMappedPathSetsEqual(
                        wi.getMappedPaths(),
                        WorkingFolder.extractMappedPaths(updateSource.getFolders()));
                }
            }
        }

        if (cachedPropertiesModified) {
            // Remove the workspace from the cache at the old name.
            getClient().removeCachedWorkspace(currentWorkspace.getName(), currentWorkspace.getOwnerName());
        }

        /*
         * Clone the update source's web service object (just in case the caller
         * wants to modify it later) and swap it for the existing web service
         * object.
         */
        final _Workspace clone = cloneWebServiceObject(updateSource.getWebServiceObject());
        webServiceObject = clone;

        /*
         * The permission profile has to be refreshed from the clonned web
         * service object of the updated source first time it's requested
         */
        setPermissionsProfile(null);

        /*
         * For the remainder of this method, access new data through "clone"
         * instead of "this" so another thread running this method doesn't mess
         * us up.
         */

        // The workspace is now fresh.
        uncachedPropertiesStale = false;
        workingFoldersStale = false;

        // Only store the workspace in the cache file if it's on this computer.
        if (cachedPropertiesModified) {
            if (matchComputer(LocalHost.getShortName(), clone.getComputer())) {
                // Clear the new name from the workspace cache.
                getClient().removeCachedWorkspace(clone.getName(), clone.getOwner());

                Workstation.getCurrent(
                    getClient().getConnection().getPersistenceStoreProvider()).insertWorkspaceIntoCache(this);

                // Set the last saved check-in on the new.
                if (null != wi) {
                    setLastSavedCheckin(wi.getLastSavedCheckin());
                }
            }
        }

        // The mappings for a local workspace changed; invalidate the scanner.
        if (invalidateLocalWorkspaceScanner) {
            getWorkspaceWatcher().workingFoldersChanged(currentWorkspace.getFolders());
        }

        Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).saveConfigIfDirty();
    }

    /**
     * Updates the last attempted checkin information with the information from
     * a failed or canceled checkin.
     *
     * @param comment
     *        the comment (may be <code>null</code> or empty)
     * @param note
     *        the note (may be <code>null</code>)
     * @param workItems
     *        the work items (may be <code>null</code>)
     * @param overrideInfo
     *        the policy override info (may be <code>null</code>)
     */
    private void updateLastSavedCheckin(
        final String comment,
        final CheckinNote note,
        final WorkItemCheckinInfo[] associatedWorkItems,
        final PolicyOverrideInfo policyOverrideInfo) {
        // Load the old one so we preserve exclusions
        SavedCheckin savedCheckin = getLastSavedCheckin();
        if (savedCheckin == null) {
            savedCheckin = new SavedCheckin();
        }

        savedCheckin.setComment(comment);
        savedCheckin.setCheckinNotes(note);
        savedCheckin.setPersistentWorkItemsCheckedInfo(WorkItemCheckedInfo.fromCheckinInfo(associatedWorkItems));
        savedCheckin.setPolicyOverrideComment(
            (policyOverrideInfo != null && policyOverrideInfo.getComment() != null) ? policyOverrideInfo.getComment()
                : null);

        setLastSavedCheckin(savedCheckin);
    }

    /**
     * Updates the last attempted checkin information with the information from
     * the shelveset.
     *
     * @param shelveset
     *        the shelveset (must not be <code>null</code>)
     */
    public void updateLastSavedCheckin(final Shelveset shelveset) {
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        // Create the replacement saved checkin data from the shelveset
        final SavedCheckin newSavedCheckin = new SavedCheckin(shelveset);

        // Preserve the previous excluded server paths and work items
        final SavedCheckin lastSavedCheckin = getLastSavedCheckin();
        if (lastSavedCheckin != null) {
            newSavedCheckin.setExcludedServerPaths(lastSavedCheckin.getExcludedServerPaths());
            newSavedCheckin.mergeWorkItems(lastSavedCheckin.getWorkItemsCheckedInfo());
        }

        setLastSavedCheckin(newSavedCheckin);
    }

    /**
     * Updates the last attempted checkin information with the information from
     * the shelveset and ensures that the specified set of pending changes have
     * a checked state set.
     *
     * @param shelveset
     *        the shelveset containing the desired metadata (must not be
     *        <code>null</code>)
     * @param unshelvedChanges
     *        the pending changes that where unshelved (may be <code>null</code>
     *        )
     */
    public void updateLastSavedCheckin(final Shelveset shelveset, final PendingChange[] unshelvedChanges) {
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        if (getLastSavedCheckin() == null) {
            setLastSavedCheckin(new SavedCheckin(shelveset));
        }

        getLastSavedCheckin().mergeShelvesetMetadata(shelveset, unshelvedChanges);
    }

    private static _Workspace cloneWebServiceObject(final _Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        /*
         * To make a full clone, mutable field types must be cloned.
         */

        _WorkingFolder[] folders = null;
        if (workspace.getFolders() != null) {
            folders = new _WorkingFolder[workspace.getFolders().length];
            for (int i = 0; i < folders.length; i++) {
                folders[i] = new _WorkingFolder(
                    workspace.getFolders()[i].getItem(),
                    workspace.getFolders()[i].getType(),
                    workspace.getFolders()[i].getDepth(),
                    workspace.getFolders()[i].getLocal());
            }
        }

        String[] ownerAliases = null;
        if (workspace.getOwnerAliases() != null) {
            ownerAliases = new String[workspace.getOwnerAliases().length];
            for (int i = 0; i < ownerAliases.length; i++) {
                ownerAliases[i] = workspace.getOwnerAliases()[i];
            }
        }

        return new _Workspace(
            workspace.getComputer(),
            workspace.isIslocal(),
            workspace.getName(),
            workspace.getOwner(),
            workspace.getOwnerdisp(),
            workspace.getOwner(),
            workspace.getPermissions(),
            workspace.getSecuritytoken(),
            workspace.getOwnertype(),
            workspace.getOwnerid(),
            workspace.getComment(),
            folders,
            ownerAliases,
            workspace.getLastAccessDate() != null ? (Calendar) workspace.getLastAccessDate().clone() : null,
            workspace.getOptions());
    }

    /**
     * Refreshes this Workspace instance from the server if uncacheable
     * properties are stale or have not been loaded. Uncacheable properties
     * include working folders and effective permissions.
     */
    public void refreshIfNeeded() {
        if (WorkspaceLocation.LOCAL == getLocation()) {
            // Local workspace
            if (uncachedPropertiesStale) {
                refresh();
            } else if (workingFoldersStale) {
                // Just the working folders are stale; we don't have to go to
                // the server to fetch them
                final WorkingFolder[] folders = LocalDataAccessLayer.queryWorkingFolders(this);
                getWebServiceObject().setFolders((_WorkingFolder[]) WrapperUtils.unwrap(_WorkingFolder.class, folders));
                workingFoldersStale = false;
            }
        } else {
            // Server workspace
            if (uncachedPropertiesStale || workingFoldersStale) {
                refresh();
            }
        }
    }

    /**
     * Refreshes the mappings from the server if:
     * <ol>
     * <li>The mappings are missing (no matter what type of workspace is in
     * question)</li>
     * <li>The workspace is a server workspace and the uncached properties are
     * stale</li>
     * </ol>
     */
    public void refreshMappingsIfNeeded() {
        if (getWebServiceObject().getFolders() == null || workingFoldersStale) {
            final WorkingFolder[] folders;
            if (getLocation() == WorkspaceLocation.LOCAL) {
                folders = LocalDataAccessLayer.queryWorkingFolders(this);
            } else {
                folders = client.queryWorkspace(getName(), getOwnerName()).getFolders();
            }
            getWebServiceObject().setFolders((_WorkingFolder[]) WrapperUtils.unwrap(_WorkingFolder.class, folders));
            workingFoldersStale = false;
        }
    }

    /**
     * Refreshes workspace information based on updated information on the
     * server. Currently only updates working folder mappings. The workspace
     * cache is also updated to reflect this information.
     *
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     */
    public void refresh() {
        /*
         * Call QueryWorkspace and run the returned Workspace object through the
         * runtime workspace cache. The result is that our method
         * UpdateFromWorkspace will be called, and we will have our contents
         * stuffed with new data.
         */
        final Workspace ws = getClient().getRepositoryWorkspace(getName(), getOwnerName());

        if (ws != this) {
            // Runtime cache should be updating this object, not returning
            // another. However we shouldn't throw an exception in the
            // production mode.
            log.debug("Runtime workspace cache returned a different object after this workspace refresh."); //$NON-NLS-1$
            log.debug("    this.name  = " + getName()); //$NON-NLS-1$
            log.debug("    this.owner = " + getOwnerName()); //$NON-NLS-1$
            log.debug("      ws.name  = " + ws.getName()); //$NON-NLS-1$
            log.debug("      ws.owner = " + ws.getOwnerName()); //$NON-NLS-1$
            log.debug("    user.name  = " + getClient().getConnection().getAuthenticatedIdentity().getDisplayName()); //$NON-NLS-1$
            log.debug("                 " + getClient().getConnection().getAuthenticatedIdentity().getUniqueName()); //$NON-NLS-1$
        }
    }

    /**
     * Undoes pending changes for matching items in this workspace. Items in
     * working folders are immediately updated.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param items
     *        the items to undo (must not be <code>null</code>)
     * @return the number of items successfully undone.
     */
    public int undo(final ItemSpec[] items) {
        return undo(items, GetOptions.NONE);
    }

    /**
     * @equivalence undo(items, getOptions, null)
     */
    public int undo(final ItemSpec[] items, final GetOptions getOptions) {
        return undo(items, getOptions, null);
    }

    /**
     * @equivalence undo(items, getOptions, false, itemPropertyFilters)
     */
    public int undo(final ItemSpec[] items, final GetOptions getOptions, final String[] itemPropertyFilters) {
        return undo(items, getOptions, false, itemPropertyFilters);
    }

    /**
     * Undoes pending changes for matching items in this workspace. Items in
     * working folders are immediately updated. If <code>updateDisk</code> is
     * <code>false</code>, then the files on disk will not be updated in
     * response to the server commands to do so. Passing <code>false</code> for
     * <code>updateDisk</code> is not common and should only be needed in rare
     * situations.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param items
     *        the items to undo (must not be <code>null</code>)
     * @param getOptions
     *        the {@link GetOptions} to use (must not be <code>null</code>)
     * @param deleteAdds
     *        determines if adds should be deleted
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return the number of items successfully undone.
     */
    public int undo(
        final ItemSpec[] items,
        final GetOptions getOptions,
        final boolean deleteAdds,
        String[] itemPropertyFilters) {
        Check.notNull(items, "items"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        client.getEventEngine().fireOperationStarted(
            new UndoOperationStartedEvent(EventSource.newFromHere(), this, items));

        int ret = 0;
        try {
            if (items.length == 0) {
                return 0;
            }

            final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();
            final AtomicBoolean onlineOperationHolder = new AtomicBoolean();
            final AtomicReference<ChangePendedFlags> changePendedFlagsHolder = new AtomicReference<ChangePendedFlags>();

            final GetOperation[] operations = client.getWebServiceLayer().undoPendingChanges(
                getName(),
                getOwnerName(),
                items,
                failuresHolder,
                null,
                itemPropertyFilters,
                onlineOperationHolder,
                false,
                changePendedFlagsHolder);

            if (operations != null && operations.length > 0) {
                if (isLocal()) {
                    final GetEngine getEngine = new GetEngine(client);
                    getEngine.processGetOperations(this, ProcessType.UNDO, RequestType.NONE, new GetOperation[][] {
                        operations
                    }, getOptions, deleteAdds, onlineOperationHolder.get(), changePendedFlagsHolder.get());

                    ret = operations.length;
                } else {
                    final String messageFormat =
                        Messages.getString("Workspace.OperationCompletedForRemoteWorkspaceButGetRequiredFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, getDisplayName());

                    client.getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(EventSource.newFromHere(), this, new Exception(message)));
                }
            }

            if (changePendedFlagsHolder.get().contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
                invalidateMappings();
            }

            client.reportFailures(this, failuresHolder.get());
        } finally {
            client.getEventEngine().fireOperationCompleted(
                new UndoOperationCompletedEvent(EventSource.newFromHere(), this, items));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED);
        }

        return ret;
    }

    /**
     * @return the object that holds the metadata for the last unsuccessful
     *         checkin or the metadata from unshelving (comment, work items,
     *         etc.). May be <code>null</code>.
     */
    public SavedCheckin getLastSavedCheckin() {
        final Workstation workstation =
            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider());

        final WorkspaceInfo workspaceInfo =
            workstation.getLocalWorkspaceInfo(getClient().getServerGUID(), getName(), getOwnerName());

        if (workspaceInfo != null) {
            return workspaceInfo.getLastSavedCheckin();
        }

        return null;
    }

    /**
     * @param newSavedCheckin
     *        the new {@link SavedCheckin}, which may be <code>null</code>
     */
    public void setLastSavedCheckin(final SavedCheckin newSavedCheckin) {
        final Workstation workstation =
            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider());

        final WorkspaceInfo workspaceInfo =
            workstation.getLocalWorkspaceInfo(getClient().getServerGUID(), getName(), getOwnerName());

        if (workspaceInfo != null) {
            workspaceInfo.setLastSavedCheckin(newSavedCheckin, workstation);
        }
    }

    /**
     * Tests whether this workspace is for this computer (the computer name
     * matches this computer's name and the working folder mappings are for
     * paths on this computer).
     *
     * @return true if this workspace is for this computer, false if it is for
     *         some other computer
     *
     * @see #getLocation()
     */
    public boolean isLocal() {
        return matchComputer(LocalHost.getShortName());
    }

    /**
     * @return true if this workspace is local workspace, false if it is server
     *         workspace
     *
     * @see #getLocation()
     */
    public boolean isLocalWorkspace() {
        return getLocation() == WorkspaceLocation.LOCAL;
    }

    /**
     * @return a value that indicates the location where data (pending changes,
     *         local versions) for this workspace are stored.
     *
     * @see #isLocal()
     */
    public WorkspaceLocation getLocation() {
        if (getWebServiceObject().isIslocal()) {
            return WorkspaceLocation.LOCAL;
        }

        return WorkspaceLocation.SERVER;
    }

    /**
     * @return true if the workspace has been deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Locks the local workspace from changes by other threads and processes.
     *
     * @return the {@link WorkspaceLock} or <code>null</code> if the workspace
     *         type is not lockable (workspace location is not
     *         {@link WorkspaceLocation#LOCAL})
     */
    public WorkspaceLock lock() {
        if (getLocation() == WorkspaceLocation.SERVER) {
            return null;
        }

        return new WorkspaceLock(this);
    }

    /**
     * Removes a local conflict from the server's list of conflicts for this
     * workspace.
     *
     * @param conflict
     *        the local conflict (not <code>null</code>)
     */
    public void removeLocalConflict(final Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        removeLocalConflicts(new Conflict[] {
            conflict
        }, ResolveErrorOptions.THROW_ON_ERROR);
    }

    /**
     * Removes local conflicts from the server's list of conflicts for this
     * workspace.
     *
     * @param conflicts
     *        the local conflicts (not <code>null</code> or empty)
     */
    public void removeLocalConflicts(final Conflict[] conflicts) {
        removeLocalConflicts(conflicts, ResolveErrorOptions.RAISE_WARNINGS_FOR_ERROR);
    }

    private void removeLocalConflicts(final Conflict[] conflicts, final ResolveErrorOptions errorOptions) {
        Check.notNullOrEmpty(conflicts, "conflicts"); //$NON-NLS-1$

        client.resolveLocalConflicts(this, conflicts, errorOptions);
    }

    /**
     * Queries the server for registered file types.
     *
     * @return an array of AFileType objects, each representing a FileType,
     *         which is a server-registered file type. May be empty but never
     *         null.
     */
    public FileType[] queryFileTypes() {
        return getClient().getWebServiceLayer().queryFileTypes();
    }

    /**
     * Sets the list of registered file types on the server.
     *
     * @param fileTypes
     *        array of AFileType objects, each representing a FileType, which is
     *        a server-registered file type (must not be <code>null</code>)
     */
    public void setFileTypes(final FileType[] fileTypes) {
        Check.notNull(fileTypes, "fileTypes"); //$NON-NLS-1$

        getClient().getWebServiceLayer().setFileTypes(fileTypes);
    }

    /**
     * Send pending changes to the server for storage without being checked in.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param set
     *        the shelveset description object (must not be <code>null</code>)
     * @param changesToShelve
     *        the pending changes to shelve (must not be <code>null</code> or
     *        empty).
     * @param replace
     *        if true, an existing shelveset with a name matching the one given
     *        in the shelveset description object will have its contents
     *        replaced by the given changesToShelve.
     * @param move
     *        if true, changes in the workspace will be removed after they are
     *        shelved in favor of the baseline content.
     * @throws ShelveException
     *         if there were failures that caused the shelveset to not be
     *         created.
     */
    public void shelve(
        final Shelveset set,
        final PendingChange[] changesToShelve,
        final boolean replace,
        final boolean move) throws ShelveException {
        Check.notNull(set, "set"); //$NON-NLS-1$
        Check.notNullOrEmpty(changesToShelve, "changesToShelve"); //$NON-NLS-1$

        /*
         * Total count is number of changes to upload plus the sort phase plus
         * the final shelve operation.
         */
        final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();
        monitor.begin(Messages.getString("Workspace.Shelve"), changesToShelve.length + 2); //$NON-NLS-1$

        /*
         * Resolve authorized user name (bug 5743)
         */
        set.setOwnerName(
            IdentityHelper.getUniqueNameIfCurrentUser(
                client.getConnection().getAuthorizedIdentity(),
                set.getOwnerName()));

        /*
         * Sort the pending changes by server path.
         */
        Arrays.sort(changesToShelve, new PendingChangeComparator(PendingChangeComparatorType.SERVER_ITEM));
        monitor.worked(1);

        try {
            final CheckinEngine ci = new CheckinEngine(client, this);

            /*
             * Upload all the content.
             */
            try {
                ci.uploadChanges(changesToShelve, true, false);
            } catch (final CheckinException e) {
                throw new ShelveException(e);
            }

            /*
             * Get the string names for the shelve operation.
             */
            final String[] sortedServerItems = PendingChange.toServerItems(changesToShelve);

            /*
             * Check one last time before creating the shelveset.
             */
            if (monitor.isCanceled()) {
                // Caught in this method below.
                throw new CoreCancelException();
            }

            final Failure[] failures =
                client.getWebServiceLayer().shelve(getName(), getOwnerName(), sortedServerItems, set, replace);

            /*
             * Failures cause this method to abort since the shelveset wasn't
             * created.
             */
            if (failures != null && failures.length > 0) {
                client.reportFailures(this, failures);
                throw new ShelveException(Messages.getString("Workspace.NoChangesShelved")); //$NON-NLS-1$
            }

            if (move) {
                setLastSavedCheckin(buildEmptyLastSavedCheckin());

                /*
                 * Undo all the changes (revert to the baseline versions).
                 */
                undo(ItemSpec.fromStrings(sortedServerItems, RecursionType.NONE));

                /*
                 * Any shelved changes that were for added files or directories
                 * need to be manually deleted. Delete files first.
                 */
                for (int i = 0; i < changesToShelve.length; i++) {
                    final PendingChange change = changesToShelve[i];
                    Check.notNull(change, "change"); //$NON-NLS-1$

                    if (change.getItemType() == ItemType.FILE
                        && change.getChangeType().contains(ChangeType.ADD)
                        && change.getLocalItem() != null) {
                        try {
                            if (new File(change.getLocalItem()).delete() == false) {
                                throw new IOException(
                                    MessageFormat.format(
                                        Messages.getString("Workspace.CannotDeleteFileFormat"), //$NON-NLS-1$
                                        change.getLocalItem()));
                            }
                        } catch (final Exception e) {
                            client.getEventEngine().fireNonFatalError(
                                new NonFatalErrorEvent(EventSource.newFromHere(), this, e));
                        }
                    }
                }

                /*
                 * Now delete any left-over directories.
                 */
                for (int i = 0; i < changesToShelve.length; i++) {
                    final PendingChange change = changesToShelve[i];
                    Check.notNull(change, "change"); //$NON-NLS-1$

                    if (change.getItemType() == ItemType.FOLDER
                        && change.getChangeType().contains(ChangeType.ADD)
                        && change.getLocalItem() != null) {
                        final File dir = new File(change.getLocalItem());

                        if (dir.exists() == false) {
                            client.getEventEngine().fireNonFatalError(
                                new NonFatalErrorEvent(
                                    EventSource.newFromHere(),
                                    this,
                                    new IOException(
                                        MessageFormat.format(
                                            Messages.getString("Workspace.DirectoryDoesNotExistFormat"), //$NON-NLS-1$
                                            change.getLocalItem()))));
                        } else {
                            try {
                                if (new File(change.getLocalItem()).delete() == false) {
                                    throw new IOException(
                                        MessageFormat.format(
                                            Messages.getString("Workspace.CannotDeleteDirectoryFormat"), //$NON-NLS-1$
                                            change.getLocalItem()));
                                }
                            } catch (final Exception e) {
                                client.getEventEngine().fireNonFatalError(
                                    new NonFatalErrorEvent(EventSource.newFromHere(), this, e));
                            }
                        }
                    }
                }
            }

            // Notify the user that the shelve is complete.
            client.getEventEngine().fireShelve(
                new ShelveEvent(EventSource.newFromHere(), this, set, changesToShelve, move));
        } catch (final CoreCancelException e) {
            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    this,
                    new CoreCancelException(Messages.getString("Workspace.ShelveCancelled")))); //$NON-NLS-1$
        }
    }

    /**
     * @return a new {@link SavedCheckin} that has the save excluded server
     *         paths that the existing {@link SavedCheckin} has, or
     *         <code>null</code> if this workpsace has no {@link SavedCheckin}
     */
    public SavedCheckin buildEmptyLastSavedCheckin() {
        final SavedCheckin lastSavedCheckin = getLastSavedCheckin();
        if (lastSavedCheckin != null) {
            final String[] excludedServerPaths = lastSavedCheckin.getExcludedServerPaths();
            if (excludedServerPaths != null && excludedServerPaths.length > 0) {
                return new SavedCheckin(Arrays.asList(excludedServerPaths), "", null, null, ""); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return null;
    }

    /**
     * @equivalence unshelve(shelvesetName, shelvesetOwner, itemSpecs, null,
     *              null, false, true)
     */
    public UnshelveResult unshelve(final String shelvesetName, final String shelvesetOwner, final ItemSpec[] itemSpecs)
        throws UnshelveException {
        return unshelve(shelvesetName, shelvesetOwner, itemSpecs, null, null, false, true);
    }

    /**
     * Unshelves changes, restoring them to the workspace.
     *
     * @param shelvesetName
     *        the name of the shelveset to restore (must not be
     *        <code>null</code> or empty).
     * @param shelvesetOwner
     *        the owner of the shelveset to restore (must not be
     *        <code>null</code> or empty).
     * @param itemSpecs
     *        the items to restore. If this array is null, all changes in the
     *        named shelveset are restored. If this array is empty, no changes
     *        will be unshelved.
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @param shelvesetPropertyFilters
     *        the list of properties to be returned on the shelvesets. To get
     *        all properties pass a single filter that is simply "*" (may be
     *        <code>null</code>)
     * @param merge
     *        <code>true</code> to allow merging conflicting changes in the
     *        shelveset with local pending changes ({@link Conflict}s will be
     *        produced)
     * @param noAutoResolve
     *        <code>true</code> to disable automerging of conflicting changes,
     *        <code>false</code> to perform an automerge
     * @return the {@link UnshelveResult} which contains the {@link Shelveset}
     *         unshelved and the {@link GetStatus} for the get operations which
     *         were processed, or <code>null</code> if the unshelve operation
     *         failed with no further status from the server
     */
    public UnshelveResult unshelve(
        final String shelvesetName,
        final String shelvesetOwner,
        final ItemSpec[] itemSpecs,
        String[] itemPropertyFilters,
        final String[] shelvesetPropertyFilters,
        final boolean merge,
        final boolean noAutoResolve) throws UnshelveException {
        Check.notNullOrEmpty(shelvesetName, "shelvesetName"); //$NON-NLS-1$
        Check.notNullOrEmpty(shelvesetOwner, "shelvesetOwner"); //$NON-NLS-1$

        log.debug("Unshelve started."); //$NON-NLS-1$

        final AtomicReference<Failure[]> failuresHolder = new AtomicReference<Failure[]>();
        final AtomicReference<GetOperation[]> getOperationsHolder = new AtomicReference<GetOperation[]>();
        final AtomicReference<Conflict[]> conflictsHolder = new AtomicReference<Conflict[]>();
        final AtomicReference<ChangePendedFlags> changePendedFlagsHolder = new AtomicReference<ChangePendedFlags>();

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final Shelveset shelveset = client.getWebServiceLayer().unshelve(
            shelvesetName,
            shelvesetOwner,
            getName(),
            getOwnerName(),
            itemSpecs,
            null,
            itemPropertyFilters,
            shelvesetPropertyFilters,
            merge,
            failuresHolder,
            getOperationsHolder,
            conflictsHolder,
            changePendedFlagsHolder);

        log.debug("Preparing get operations."); //$NON-NLS-1$

        /*
         * Print out any failures and bail if there were any serious errors.
         */
        if (failuresHolder.get().length > 0) {
            client.reportFailures(this, failuresHolder.get());

            if (getOperationsHolder.get().length == 0) {
                throw new UnshelveException(Messages.getString("Workspace.NoChangesUnshelved")); //$NON-NLS-1$
            }
        }

        /*
         * Turns out that the result.value can be null if the unshelve fails. In
         * which case return null and let the caller display the error message
         * that will have been thrown from the above events.
         */
        if (shelveset == null) {
            return null;
        }

        final GetOperation[] getOperations = getOperationsHolder.get();
        Conflict[] conflicts = conflictsHolder.get();

        /*
         * Convert the get operations into pending changes so we can pass them
         * through the event.
         */
        final PendingChange[] unshelvedChanges =
            getOperations != null ? new PendingChange[getOperations.length] : new PendingChange[0];

        if (getOperations != null && getOperations.length > 0) {
            for (int i = 0; i < getOperations.length; i++) {
                getOperations[i].setProcessType(ProcessType.UNSHELVE);
                unshelvedChanges[i] = new PendingChange(this, getOperations[i], ProcessType.UNSHELVE);
            }
        }

        client.getEventEngine().fireOperationStarted(
            new UnshelveShelvesetStartedEvent(EventSource.newFromHere(), this, shelveset, unshelvedChanges));

        GetStatus getStatus = new GetStatus();

        try {
            if (getOperations != null && getOperations.length > 0) {
                final GetEngine getEngine = new GetEngine(client);

                getStatus = getEngine.processGetOperations(
                    this,
                    ProcessType.UNSHELVE,
                    getOperations,
                    GetOptions.NONE,
                    changePendedFlagsHolder.get());
            }

            /* We want to auto resolve conflicts if the option is set */
            int resolvedConflicts = 0;
            if (conflicts != null && !noAutoResolve) {
                /* Actually auto resolve the conflicts */
                final Conflict[] remainingConflicts =
                    client.autoResolveValidConflicts(this, conflicts, AutoResolveOptions.ALL_SILENT);

                resolvedConflicts = conflicts.length - remainingConflicts.length;
                conflicts = remainingConflicts;
            }

            /* Fire events for conflicts found during the unshelve. */
            int nonResolvedConflicts = 0;
            if (conflicts != null) {
                for (final Conflict conflict : conflicts) {
                    if (conflict.getResolution() == Resolution.NONE) {
                        nonResolvedConflicts++;

                        client.getEventEngine().fireConflict(
                            new ConflictEvent(
                                EventSource.newFromHere(),
                                conflict.getYourServerItemSource(),
                                this,
                                ConflictDescriptionFactory.getConflictDescription(
                                    ConflictCategory.getConflictCategory(conflict),
                                    conflict).getDescription(),
                                true));
                    }
                }
            }

            getStatus.setNumConflicts(getStatus.getNumConflicts() + nonResolvedConflicts);
            getStatus.setNumResolvedConflicts(resolvedConflicts);
        } finally {
            client.getEventEngine().fireOperationCompleted(
                new UnshelveShelvesetCompletedEvent(EventSource.newFromHere(), this, shelveset, unshelvedChanges));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED);
        }

        if (changePendedFlagsHolder.get().contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
            invalidateMappings();
        }

        return new UnshelveResult(shelveset, getStatus, unshelvedChanges, conflicts);
    }

    public boolean matchName(final String workspaceName) {
        Check.notNull(workspaceName, "workspaceName"); //$NON-NLS-1$

        return Workspace.matchName(workspaceName, getName());
    }

    public boolean matchOwner(final String owner) {
        Check.notNull(owner, "owner"); //$NON-NLS-1$

        return Workspace.matchOwner(owner, getOwnerName());
    }

    public boolean matchComputer(final String computer) {
        Check.notNull(computer, "computer"); //$NON-NLS-1$

        return Workspace.matchComputer(computer, getComputer());
    }

    /**
     * Gets the branch history for the specified item at the specified version.
     *
     * @param itemSpec
     *        the item to query branch history for (must not be
     *        <code>null</code>)
     * @param versionSpec
     *        the version of the item to query branch history for (must not be
     *        <code>null</code>)
     * @return for each item spec passed, a {@link BranchHistory} is returned in
     *         the array. The array may be empty but never null. Items in the
     *         array may be null.
     */
    public BranchHistory getBranchHistory(final ItemSpec itemSpec, final VersionSpec versionSpec) {
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        return getBranchHistory(new ItemSpec[] {
            itemSpec
        }, versionSpec)[0];
    }

    /**
     * Gets the branch history for the specified items at the specified version.
     *
     * @param itemSpecs
     *        the items to query branch history for (must not be
     *        <code>null</code>)
     * @param versionSpec
     *        the version of the item to query branch history for (must not be
     *        <code>null</code>)
     * @return for each item spec passed, a {@link BranchHistory} is returned in
     *         the array. The array may be empty but never null. Items in the
     *         array may be null.
     */
    public BranchHistory[] getBranchHistory(final ItemSpec[] itemSpecs, final VersionSpec versionSpec) {
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$
        Check.notNull(versionSpec, "versionSpec"); //$NON-NLS-1$

        final _BranchRelative[][] arrayOfArray =
            client.getWebServiceLayer().queryBranches(getName(), getOwnerName(), itemSpecs, versionSpec);

        // For each passed Itemspec that exists in the server we get an array of
        // branch relatives back.
        final BranchHistory[] historyItems = new BranchHistory[arrayOfArray.length];

        for (int i = 0; i < arrayOfArray.length; i++) {
            final _BranchRelative[] relatives = arrayOfArray[i];
            if (relatives == null) {
                historyItems[i] = null;
            } else {
                historyItems[i] = buildBranchHistoryTree(relatives);
            }
        }

        return historyItems;
    }

    /**
     * Put the flattened results from the SOAP query into a hierarchy.
     */
    protected BranchHistory buildBranchHistoryTree(final _BranchRelative[] relatives) {
        final Map<Integer, List<BranchHistoryTreeItem>> missingParents =
            new HashMap<Integer, List<BranchHistoryTreeItem>>(relatives.length);
        final Map<Integer, BranchHistoryTreeItem> items = new HashMap<Integer, BranchHistoryTreeItem>(relatives.length);

        for (int i = 0; i < relatives.length; i++) {
            final int id = relatives[i].getReltoid();
            final int parentId = relatives[i].getRelfromid();

            final BranchHistoryTreeItem item = BranchHistoryTreeItem.createFromRelativeToItem(relatives[i]);

            if (item != null) {
                if (relatives[i].getBranchFromItem() != null) {
                    item.setFromItem(new Item(relatives[i].getBranchFromItem()));
                }

                // Remember the item
                items.put(id, item);

                // Let's check if there are items waiting this one to be their
                // parent
                if (missingParents.containsKey(id)) {
                    final List<BranchHistoryTreeItem> rootCandidates = missingParents.get(id);
                    for (final BranchHistoryTreeItem child : rootCandidates) {
                        child.setParent(item);
                        item.addChild(child);
                    }
                    missingParents.remove(id);
                }

                // Let's check if we've already met this item's parent
                if (items.containsKey(parentId)) {
                    final BranchHistoryTreeItem parent = items.get(parentId);
                    item.setParent(parent);
                    parent.addChild(item);
                }
                // Let's check if there are already items waiting for the same
                // parent
                else if (missingParents.containsKey(parentId)) {
                    missingParents.get(parentId).add(item);
                }
                // This is the first time we met an item referring to that
                // parent
                else {
                    final List<BranchHistoryTreeItem> rootCandidates = new ArrayList<BranchHistoryTreeItem>();
                    rootCandidates.add(item);
                    missingParents.put(parentId, rootCandidates);
                }
            }
        }

        final BranchHistory history = new BranchHistory();

        // All items still waiting for their parents are roots
        for (final List<BranchHistoryTreeItem> roots : missingParents.values()) {
            for (final BranchHistoryTreeItem root : roots) {
                setBranchHistoryItemLevel(0, root);

                history.addChild(root);
            }
        }

        return history;
    }

    /**
     * Set the level on a branch history item, recursively set the levels on any
     * children that may exsist.
     */
    private void setBranchHistoryItemLevel(final int level, final BranchHistoryTreeItem item) {
        item.setLevel(level);

        for (final BranchHistoryTreeItem child : item.getChildrenAsList()) {
            setBranchHistoryItemLevel(level + 1, child);
        }
    }

    /**
     * Look up the merge candidates for merging between the requested items.
     *
     * @param sourcePath
     *        the local or server path of the source of the potential merge (not
     *        null or empty).
     * @param targetPath
     *        the local or server path of the target of the potential merge (not
     *        null or empty).
     * @param recursion
     *        what level of recursion we should apply to the candidate search
     *        (may be null).
     * @param mergeFlags
     *        merge command option(s) compatible with the /cadidate option (must
     *        not be <code>null</code>).
     * @return the array of merge candidates returned by the server. May be
     *         empty but never null.
     */
    public MergeCandidate[] getMergeCandidates(
        final String sourcePath,
        final String targetPath,
        final RecursionType recursion,
        final MergeFlags mergeFlags) {
        return client.getMergeCandidates(sourcePath, targetPath, recursion, mergeFlags);
    }

    /**
     * @equivalence merge(sourcePath, targetPath, sourceVersionFrom,
     *              sourceVersionTo, lockLevel, recursion, mergeFlags, null)
     */
    public GetStatus merge(
        final String sourcePath,
        final String targetPath,
        final VersionSpec sourceVersionFrom,
        final VersionSpec sourceVersionTo,
        final LockLevel lockLevel,
        final RecursionType recursion,
        final MergeFlags mergeFlags) {
        return merge(
            sourcePath,
            targetPath,
            sourceVersionFrom,
            sourceVersionTo,
            lockLevel,
            recursion,
            mergeFlags,
            null);
    }

    /**
     * @equivalence merge(new ItemSpec(sourcePath, recursion), targetPath,
     *              sourceVersionFrom, sourceVersionTo, lockLevel, mergeFlags,
     *              null)
     */
    public GetStatus merge(
        final String sourcePath,
        final String targetPath,
        final VersionSpec sourceVersionFrom,
        final VersionSpec sourceVersionTo,
        final LockLevel lockLevel,
        final RecursionType recursion,
        final MergeFlags mergeFlags,
        final String[] itemPropertyFilters) {
        return merge(
            new ItemSpec(sourcePath, recursion),
            targetPath,
            sourceVersionFrom,
            sourceVersionTo,
            lockLevel,
            mergeFlags,
            itemPropertyFilters);
    }

    /**
     * Merge changes made between two versions to items in a given source path
     * into a given target path. The result of this method is that one or more
     * merge items are pended.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param sourceSpec
     *        the local or server path of the source of the merge; where the
     *        changes are copied from, and the recursion type to match the given
     *        source path with (must not be <code>null</code>, path must not be
     *        <code>null</code> or empty)
     * @param targetPath
     *        the local or server path of the target of the merge; where the
     *        changes will end up (must not be <code>null</code> or empty).
     * @param sourceVersionFrom
     *        the version (inclusive) of the source item to start including
     *        changes from for this merge operation. null indicates all versions
     *        beginning with version 1.
     * @param sourceVersionTo
     *        the version (inclusive) of the source item to stop including
     *        changes from for this merge operation. null indicates all versions
     *        up to and including tip version.
     * @param lockLevel
     *        the lock level to apply to the pended changes (must not be
     *        <code>null</code>)
     * @param mergeFlags
     *        any merge options to apply during this merge (must not be
     *        <code>null</code>)
     * @param itemPropertyFilters
     *        a list of versioned item properties to return with each get
     *        operation (may be <code>null</code>)
     * @return a GetStatus instance with the results of the merge operation.
     */
    public GetStatus merge(
        final ItemSpec sourceSpec,
        final String targetPath,
        final VersionSpec sourceVersionFrom,
        final VersionSpec sourceVersionTo,
        final LockLevel lockLevel,
        final MergeFlags mergeFlags,
        String[] itemPropertyFilters) {
        Check.notNull(sourceSpec, "sourceSpec"); //$NON-NLS-1$
        Check.notNullOrEmpty(sourceSpec.getItem(), "sourceSpec.item"); //$NON-NLS-1$
        Check.notNull(sourceSpec.getRecursionType(), "sourceSpec.recursion"); //$NON-NLS-1$
        Check.notNullOrEmpty(targetPath, "targetPath"); //$NON-NLS-1$
        Check.notNull(lockLevel, "lockLevel"); //$NON-NLS-1$
        Check.notNull(mergeFlags, "mergeFlags"); //$NON-NLS-1$

        // Using web service directly so merge filters configured on client
        itemPropertyFilters = client.mergeWithDefaultItemPropertyFilters(itemPropertyFilters);

        final ItemSpec targetSpec = new ItemSpec(targetPath, RecursionType.NONE);

        int nonResolvedConflicts = 0;
        int resolvedConflicts = 0;

        client.getEventEngine().fireOperationStarted(new MergeOperationStartedEvent(EventSource.newFromHere(), this));

        final AtomicReference<Failure[]> failures = new AtomicReference<Failure[]>();
        final AtomicReference<Conflict[]> conflicts = new AtomicReference<Conflict[]>();
        final AtomicReference<ChangePendedFlags> changePendedFlags = new AtomicReference<ChangePendedFlags>();

        try {
            final GetOperation[] getOps = client.getWebServiceLayer().merge(
                getName(),
                getOwnerName(),
                sourceSpec,
                targetSpec,
                sourceVersionFrom,
                sourceVersionTo,
                lockLevel,
                mergeFlags,
                failures,
                conflicts,
                null,
                itemPropertyFilters,
                changePendedFlags);

            final boolean discard = mergeFlags.contains(MergeFlags.ALWAYS_ACCEPT_MINE);

            // Match up these getOps and merge details (returned as Conflict
            // objects). The conflicts with matching getOps have already been
            // resolved.
            final Map<String, Conflict> pathConflictDict =
                new TreeMap<String, Conflict>(ServerPath.TOP_DOWN_COMPARATOR);
            final Map<Integer, Conflict> itemIdConflictDict = new HashMap<Integer, Conflict>();
            for (final Conflict conflict : conflicts.get()) {
                if (conflict.isResolved()) {
                    // if it is a branch, and the user asked for preview, we do
                    // not assign ItemIds
                    // so we need to lookup by path
                    if (conflict.getBaseChangeType().contains(ChangeType.BRANCH) && conflict.getYourItemID() == 0) {
                        pathConflictDict.put(conflict.getYourServerItem(), conflict);
                    } else {
                        itemIdConflictDict.put(conflict.getYourItemID(), conflict);
                    }
                }
            }
            for (final GetOperation getOp : getOps) {
                if (getOp.getChangeType().contains(ChangeType.BRANCH) && getOp.getItemID() == 0) {
                    final Conflict conflict = pathConflictDict.get(getOp.getTargetServerItem());
                    if (conflict != null) {
                        getOp.setMergeDetails(conflict);
                    }
                } else {
                    final Conflict conflict = itemIdConflictDict.get(getOp.getItemID());
                    if (conflict != null) {
                        getOp.setMergeDetails(conflict);
                    }
                }
            }

            GetStatus getStatus = null;

            if (isLocal()) {
                /*
                 * We want to auto resolve conflicts if the option is set to do
                 * so and we are not discarding
                 */
                if (!discard
                    && !mergeFlags.contains(MergeFlags.NO_AUTO_RESOLVE)
                    && !mergeFlags.contains(MergeFlags.NO_MERGE)) {
                    if (getClient().getWebServiceLayer().getServiceLevel().getValue() < WebServiceLevel.TFS_2012_1.getValue()) {
                        /*
                         * The download urls for base files were not populated
                         * on conflicts in pre 2012 servers. Let's call
                         * QueryConflicts now so that we have that information.
                         * We use sourceSpec.RecursionType here because that
                         * corresponds to what the user passed in.
                         */
                        conflicts.set(queryConflicts(new String[] {
                            targetSpec.getItem()
                        }, sourceSpec.getRecursionType() != RecursionType.NONE));
                    }

                    final Conflict[] remainingConflicts =
                        getClient().autoResolveValidConflicts(this, conflicts.get(), AutoResolveOptions.ALL_SILENT);

                    resolvedConflicts = conflicts.get().length - remainingConflicts.length;
                    conflicts.set(remainingConflicts);
                }

                // Fire events for merges that did not get resolved by the
                // server.
                // The others will get fired in get.cs as they are processed.
                for (final Conflict conflict : conflicts.get()) {
                    if (conflict.getResolution() == Resolution.NONE) {
                        log.trace("Firing event on conflict: " + conflict); //$NON-NLS-1$

                        // The pending change arg is null because of the
                        // conflict.
                        client.getEventEngine().fireMerging(
                            new MergingEvent(
                                EventSource.newFromHere(),
                                new Conflict(conflict),
                                this,
                                false,
                                null,
                                OperationStatus.CONFLICT,
                                ChangeType.NONE,
                                true,
                                new PropertyValue[0]));

                        nonResolvedConflicts++;
                    }
                }

                // When we are discarding source changes, we simply call
                // ResolveConflict.
                if (discard) {
                    for (final Conflict conflict : conflicts.get()) {
                        // There is nothing to do when the resolution is
                        // AcceptYours.
                        client.getEventEngine().fireConflictResolved(
                            new ConflictResolvedEvent(
                                EventSource.newFromHere(),
                                this,
                                conflict,
                                changePendedFlags.get()));
                    }

                    getStatus = new GetStatus();
                    getStatus.setNumOperations(conflicts.get().length);
                    return getStatus;
                }

                final GetOptions options =
                    mergeFlags.contains(MergeFlags.NO_MERGE) ? GetOptions.PREVIEW : GetOptions.NONE;

                final GetEngine getEngine = new GetEngine(client);
                getStatus =
                    getEngine.processGetOperations(this, ProcessType.MERGE, getOps, options, changePendedFlags.get());

                getStatus.setNumConflicts(getStatus.getNumConflicts() + nonResolvedConflicts);
                getStatus.setNumResolvedConflicts(resolvedConflicts);
            } else if (getOps.length > 0) {
                client.getEventEngine().fireNonFatalError(
                    new NonFatalErrorEvent(
                        EventSource.newFromHere(),
                        this,
                        new VersionControlException(
                            MessageFormat.format(
                                Messages.getString("Workspace.NoLocalChangesRemoteWorkspaceFormat"), //$NON-NLS-1$
                                getDisplayName()))));
            }

            if (getStatus == null) {
                getStatus = new GetStatus();
                getStatus.setNumOperations(getOps.length);
            }

            if (changePendedFlags.get().contains(ChangePendedFlags.WORKING_FOLDER_MAPPINGS_UPDATED)) {
                invalidateMappings();
            }

            client.reportFailures(this, failures.get());

            for (final Failure failure : failures.get()) {
                getStatus.addFailure(failure);
            }

            return getStatus;
        } finally {
            client.getEventEngine().fireOperationCompleted(
                new MergeOperationCompletedEvent(EventSource.newFromHere(), this));

            Workstation.getCurrent(getClient().getConnection().getPersistenceStoreProvider()).notifyForWorkspace(
                this,
                Notification.VERSION_CONTROL_PENDING_CHANGES_CHANGED);
        }
    }

    /**
     * <p>
     * Finds pending changes in the given array which can be safely reconciled
     * (undone) to match the changes in the given changeset. This method is
     * primarily used to undo pending changes after a gated checkin build has
     * completed, so the local workspace does not have changes which conflict
     * with the latest versions of items.
     * </p>
     * <p>
     * This method may run for a long time for large changesets or large files
     * (their contents are hashed and compared with the content hashes in the
     * given changeset). Cancelation via {@link TaskMonitor} is supported.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param changeset
     *        the {@link Changeset} whose changes are to be reconciled with the
     *        given pending changes (must not be <code>null</code>)
     * @param pendingChanges
     *        the pending changes to be reconciled with the changeset (must not
     *        be <code>null</code>)
     * @return a {@link ReconcilePendingChangesStatus} containing the pending
     *         changes and additional information (never <code>null</code>)
     * @throws CanceledException
     *         if the operation was canceled via the {@link TaskMonitor}
     */
    public ReconcilePendingChangesStatus findReconcilablePendingChangesForChangeset(
        final Changeset changeset,
        final PendingChange[] pendingChanges) throws CanceledException {
        /*
         * Many of the comments and logic in this method came from the Visual
         * Studio file DialogUndoUnchanged.cs (m_worker_DoWork).
         */

        Check.notNull(changeset, "changeset"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

        if (pendingChanges.length == 0) {
            return new ReconcilePendingChangesStatus(false);
        }

        final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();

        /*
         * Work size of pending changes size plus 10 for querying deferred
         * items.
         */
        monitor.begin(MessageFormat.format(
            Messages.getString("Workspace.FindingReconcilableChangesForChangesetFormat"), //$NON-NLS-1$
            Integer.toString(changeset.getChangesetID())), pendingChanges.length + 10);

        try {
            final boolean schemaChangeRenameSupported =
                client.getServiceLevel().getValue() >= WebServiceLevel.TFS_2010.getValue();

            /*
             * Find the interesection of the pending changes and the historic
             * changes which are safe to undo (no local changes).
             */

            // Map server path to Changes.
            final Map<String, Change> historicServerItemToChangeMap =
                new TreeMap<String, Change>(ServerPath.TOP_DOWN_COMPARATOR);
            for (final Change historicChange : changeset.getChanges()) {
                historicServerItemToChangeMap.put(historicChange.getItem().getServerItem(), historicChange);
            }

            /*
             * atLeastOneMatched tells us whether we found at least one local
             * change that was a probable match for one in the changeset.
             */
            boolean atLeastOneMatched = false;

            final List<PendingChange> reconcilablePendingChanges = new ArrayList<PendingChange>();
            final List<PendingChange> deferredChanges = new ArrayList<PendingChange>();

            /*
             * Filter the list of pending changes down to just those in the
             * changeset and check that those changes may be safely undone.
             */
            for (final PendingChange pendingChange : pendingChanges) {
                if (monitor.isCanceled()) {
                    throw new CanceledException();
                }

                monitor.worked(1);

                boolean safeChange = false;
                boolean deferForRenameSourceCheck = false;

                /*
                 * Get matching change from changeset. Skip this change if it is
                 * not found
                 */
                final Change historicChange = historicServerItemToChangeMap.get(pendingChange.getServerItem());
                if (historicChange == null) {
                    continue;
                }

                /*
                 * We found at least one matching change between the local set
                 * and the committed set.
                 */
                atLeastOneMatched = true;

                /*
                 * if workspace has pending rename but changeset does not, it is
                 * incompatible change
                 */
                if (pendingChange.getChangeType().contains(ChangeType.RENAME)
                    && historicChange.getChangeType().contains(ChangeType.RENAME) == false) {
                    continue;
                } else if (historicChange.getChangeType().contains(ChangeType.RENAME)) {
                    /*
                     * In the previous IF we verified that we don't have
                     * incompatible rename. Now we check if the changeset has a
                     * rename change, if it does we need to validate source and
                     * target path even if pending change is not rename because
                     * pending change changes on parents. We are validating that
                     * the renames were both renaming the same item. For
                     * pre-Rosario servers we just make sure ItemId matches. For
                     * Rosario servers (with Rename Schema Changes) we have to
                     * get branch history to check the rename source.
                     */
                    if (schemaChangeRenameSupported == false) {
                        if (pendingChange.getItemID() != historicChange.getItem().getItemID()) {
                            // In this case both were renames but they did not
                            // rename the same item, so skip this change
                            continue;
                        }
                    } else {
                        deferForRenameSourceCheck = true;
                    }
                }

                /*
                 * if local change does not have an edit or an encoding change
                 * and change types match then the change is safe to undo.
                 */
                if (pendingChange.getChangeType().contains(ChangeType.EDIT) == false
                    && pendingChange.getChangeType().contains(ChangeType.ENCODING) == false
                    && pendingChange.getChangeType().contains(ChangeType.BRANCH) == false) {
                    /*
                     * if change types match (discounting lock) then the change
                     * may be safe to undo (check for rename).
                     */
                    if (pendingChange.getChangeType().remove(ChangeType.LOCK).equals(
                        historicChange.getChangeType().remove(ChangeType.LOCK))) {
                        /*
                         * no local content change and change type matches --
                         * SAFE to undo
                         */
                        safeChange = true;
                    } else {
                        // changes differ enough to be UNSAFE to undo.
                        safeChange = false;
                    }
                } else {
                    // Make sure encodings match
                    if (pendingChange.getEncoding() == historicChange.getItem().getEncoding().getCodePage()) {
                        /*
                         * If the change types match (discounting edit, lock,
                         * and rename)
                         */
                        if (pendingChange.getChangeType().remove(ChangeType.EDIT).remove(ChangeType.LOCK).remove(
                            ChangeType.RENAME).equals(
                                historicChange.getChangeType().remove(ChangeType.EDIT).remove(ChangeType.LOCK).remove(
                                    ChangeType.RENAME))) {
                            /*
                             * If the local change is an edit then we disregard
                             * whether the server had an edit. This allows us to
                             * undo a change where we have a pending edit but
                             * have not actually changed any content. We also
                             * validate content for branch as a way of
                             * mitigating the risk that item was branched from
                             * different source, OM does not support retrieving
                             * source of pending branch so we need to live with
                             * this limitation. Folders are validated just upon
                             * their change type.
                             */
                            if (pendingChange.getChangeType().contains(ChangeType.EDIT)
                                || (pendingChange.getChangeType().contains(ChangeType.BRANCH)
                                    && pendingChange.getItemType() == ItemType.FILE)) {
                                /*
                                 * If we have a local edit and change types
                                 * match (other than edit and lock bits), then
                                 * look at content to see if undo is safe
                                 */

                                safeChange = false;
                                if (pendingChange.getLocalItem() != null
                                    && pendingChange.getLocalItem().length() > 0
                                    && new File(pendingChange.getLocalItem()).exists()) {
                                    byte[] localItemHash;
                                    try {
                                        localItemHash =
                                            CheckinEngine.computeMD5Hash(pendingChange.getLocalItem(), monitor);
                                    } catch (final CoreCancelException e) {
                                        throw new CanceledException();
                                    }

                                    safeChange =
                                        Arrays.equals(localItemHash, historicChange.getItem().getContentHashValue());
                                }
                            } else if (historicChange.getChangeType().contains(ChangeType.EDIT) == false) {
                                /*
                                 * no local content change and change type
                                 * matches -- SAFE to undo
                                 */
                                safeChange = true;
                            } else {
                                // change types did not match
                                safeChange = false;
                            }
                        }
                    } else {
                        /*
                         * either changes differ or encodings differ -- UNSAFE
                         * to undo.
                         */
                        safeChange = false;
                    }
                }

                if (safeChange) {
                    if (deferForRenameSourceCheck) {
                        deferredChanges.add(pendingChange);
                    } else {
                        reconcilablePendingChanges.add(pendingChange);
                    }
                }
            }

            if (atLeastOneMatched == false) {
                return new ReconcilePendingChangesStatus(false);
            }

            /*
             * Perform (IF NEEDED) QueryHistory to compare local rename source
             * items with the committed items' source server items
             */
            if (deferredChanges.size() > 0) {
                final ChangesetVersionSpec changeSetVersionSpec = new ChangesetVersionSpec(changeset.getChangesetID());
                final ChangesetVersionSpec previousChangeSetVersionSpec =
                    new ChangesetVersionSpec(changeset.getChangesetID() - 1);

                for (final PendingChange deferredChange : deferredChanges) {
                    if (deferredChange.getSourceServerItem() == null
                        || deferredChange.getSourceServerItem().length() == 0
                        || deferredChange.getServerItem() == null
                        || deferredChange.getServerItem().length() == 0) {
                        continue;
                    }

                    /*
                     * We need to validate rename even if actual pending change
                     * is not rename because of parent pending change case
                     * However if source and target path are identical
                     * (case-sensitive) there is no way it can be this case
                     */
                    if (deferredChange.getChangeType().contains(ChangeType.RENAME) == false
                        && deferredChange.getSourceServerItem().equals(deferredChange.getServerItem())) {
                        continue;
                    }

                    /*
                     * We need to query for history of every rename item, so we
                     * try to do those queries as quick as possible We ask for
                     * at most one change, ending with changeset m_changesetId-1
                     */
                    final Changeset[] historicChangesets = queryHistory(
                        deferredChange.getServerItem(),
                        changeSetVersionSpec,
                        0,
                        RecursionType.NONE,
                        null,
                        null,
                        previousChangeSetVersionSpec,
                        1,
                        true,
                        false,
                        false,
                        false);

                    if (monitor.isCanceled()) {
                        throw new CanceledException();
                    }

                    if (historicChangesets != null && historicChangesets.length > 0) {
                        final Changeset sourceOfRenameChangeset = historicChangesets[0];

                        if (sourceOfRenameChangeset != null
                            && sourceOfRenameChangeset.getChanges().length > 0
                            && sourceOfRenameChangeset.getChanges()[0] != null) {
                            final String sourceServerItem =
                                sourceOfRenameChangeset.getChanges()[0].getItem().getServerItem();
                            if (ServerPath.equals(deferredChange.getSourceServerItem(), sourceServerItem)) {
                                /*
                                 * In this case both were renames and they
                                 * renamed the same item; add this change to the
                                 * list
                                 */
                                reconcilablePendingChanges.add(deferredChange);
                            }
                        }
                    }
                }
            }

            monitor.worked(10);

            return new ReconcilePendingChangesStatus(
                true,
                reconcilablePendingChanges.toArray(new PendingChange[reconcilablePendingChanges.size()]));
        } finally {
            monitor.done();
        }
    }

    /**
     * Obtains the display name of this workspace. The display name is the name
     * and owner of this workspace in the "workspace spec" format as defined by
     * the {@link WorkspaceSpec} class.
     *
     * @return the display name of this workspace
     */
    public String getDisplayName() {
        return new WorkspaceSpec(getName(), getOwnerDisplayName()).toString();
    }

    /**
     * Obtains the unique name of this workspace. The unique name is the name
     * and owner of this workspace in the "workspace spec" format as defined by
     * the {@link WorkspaceSpec} class.
     *
     *
     * @return the qualified unique name of this workspace
     */
    public String getUniqueName() {
        return new WorkspaceSpec(getName(), getOwnerName()).toString();
    }

    public WorkspacePermissions getPermissions() {
        return WorkspacePermissions.fromIntFlags(getWebServiceObject().getPermissions());
    }

    /**
     * @return the permission profile for this {@link Workspace}
     */
    public WorkspacePermissionProfile getPermissionsProfile() {
        throwIfDeleted();

        refreshIfNeeded();

        if (permissionsProfile == null) {
            refreshPermissionsProfile();
        }

        return permissionsProfile;
    }

    public void setPermissionsProfile(final WorkspacePermissionProfile newPermissionProfile) {
        throwIfDeleted();

        permissionsProfile = newPermissionProfile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String spec = new WorkspaceSpec(getName(), getOwnerDisplayName()).toString();
        final String comment = getComment() != null ? getComment() : ""; //$NON-NLS-1$
        return MessageFormat.format("{0} [{1}] ({2})", spec, getComputer(), comment); //$NON-NLS-1$
    }

    /**
     * Compares two workspaces first by server URL, then by workspace name, then
     * by owner (all case insensitive). See {@link CachedWorkspace} for an
     * identical implementation of {@link #compareTo(Object)}.
     *
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Workspace other) {
        int ret = getServerURI().compareTo(other.getServerURI());
        if (ret != 0) {
            return ret;
        }

        ret = getName().compareToIgnoreCase(other.getName());
        if (ret != 0) {
            return ret;
        }

        return getOwnerName().compareToIgnoreCase(other.getOwnerName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Workspace == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final Workspace other = (Workspace) obj;

        return ((getName() == null) ? other.getName() == null : getName().equals(other.getName()))
            && ((getOwnerName() == null) ? other.getOwnerName() == null : getOwnerName().equals(other.getOwnerName()))
            && ((getComputer() == null) ? other.getComputer() == null : getComputer().equals(other.getComputer()))
            && ((getServerURI() == null) ? other.getServerURI() == null : getServerURI().equals(other.getServerURI()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + ((getName() == null) ? 0 : getName().hashCode());
        result = result * 37 + ((getOwnerName() == null) ? 0 : getOwnerName().hashCode());
        result = result * 37 + ((getComputer() == null) ? 0 : getComputer().hashCode());
        result = result * 37 + ((getServerURI() == null) ? 0 : getServerURI().hashCode());

        return result;
    }

    /**
     * Checks for conflicting working folders within the working folder array
     * itself.
     *
     * @param workspaceName
     *        the name of the workspace being checked (must not be
     *        <code>null</code> or empty)
     * @param folders
     *        the new working folders to check (may be <code>null</code>)
     * @param removeUnparentedCloaks
     *        When true, will strip from the mappings any cloaks not parented by
     *        a mapping
     * @return the new working folders (never <code>null</code>)
     * @throws MappingConflictException
     *         if there was a mapping conflict
     */
    public static WorkingFolder[] checkForInternalMappingConflicts(
        final String workspaceName,
        final WorkingFolder[] folders,
        final boolean removeUnparentedCloaks) {
        if (null == folders) {
            return null;
        }

        // convert to a list so we can operate on it
        final List<WorkingFolder> mappings = new ArrayList<WorkingFolder>(Arrays.asList(folders));

        for (int i = 0; i < mappings.size(); i++) {
            final WorkingFolder wf = mappings.get(i);

            Check.notNull(wf, "wf"); //$NON-NLS-1$

            // used to store the immediate parent.
            WorkingFolder parent = null;

            // Check for simple in the working folders array itself.
            for (int j = 0; j < mappings.size(); j++) {
                final WorkingFolder wf2 = mappings.get(j);
                if (wf == wf2) {
                    continue;
                }

                // The server allows everything but having the same server path
                // explicitly mapped
                // more than once and having the same local path explicitly
                // mapped more than once.
                if (!wf.isCloaked() && !wf2.isCloaked()) {
                    if (LocalPath.equals(wf.getLocalItem(), wf2.getLocalItem())) {
                        throw new MappingConflictException(
                            MessageFormat.format(
                                Messages.getString("Workspace.MultipleWorkingFoldersForPathFormat"), //$NON-NLS-1$
                                workspaceName,
                                wf.getLocalItem()));
                    }
                }

                if (ServerPath.equals(wf.getServerItem(), wf2.getServerItem())) {
                    throw new MappingConflictException(
                        MessageFormat.format(
                            Messages.getString("Workspace.MultipleWorkingFoldersForPathFormat"), //$NON-NLS-1$
                            workspaceName,
                            wf.getServerItem()));
                }

                if (wf.isCloaked() && ServerPath.isChild(wf2.getServerItem(), wf.getServerItem())) {
                    if (parent == null || wf2.getServerItem().length() > parent.getServerItem().length()) {
                        parent = wf2;
                    }
                }
            }

            // if the folder is cloaked and there is not a mapped parent,
            // and the caller wanted us to filter them out
            if (removeUnparentedCloaks && wf.isCloaked() && (parent == null || parent.isCloaked())) {
                mappings.remove(i);
                i--;
                continue;
            }
        }

        return mappings.toArray(new WorkingFolder[mappings.size()]);
    }

    /**
     * @return true if the AuthorizedUser has CheckIn permission on this
     *         workspace.
     */
    public boolean hasAdministerPermission() {
        return hasWorkspacePermission(WorkspacePermissions.ADMINISTER);
    }

    /**
     * Checks to see whether this.VersionControlServer.AuthorizedUser has the
     * provided WorkspacePermissions bits on this workspace.
     * <p>
     *
     * @param permissionToCheck
     *        WorkspacePermissions bits to check
     * @return true if the AuthorizedUser has the requested permissions, false
     *         otherwise
     */
    public boolean hasWorkspacePermission(WorkspacePermissions permissionToCheck) {
        // Quickly compare with the owner. This is for performance. The
        // assumption being made is that the owner of a workspace is never
        // denied permissions to their own workspace.
        if (IdentityHelper.identityHasName(client.getConnection().getAuthorizedIdentity(), getOwnerName())) {
            return true;
        }

        if (client.getServiceLevel().getValue() < WebServiceLevel.TFS_2010.getValue()) {
            // This is an Orcas or Whidbey server, and we are not the owner.

            // Read permission is granted for all workspaces.
            permissionToCheck = permissionToCheck.remove(WorkspacePermissions.READ);

            // Administer permission is granted via the AdminWorkspaces bit on
            // global permissions.
            if (permissionToCheck.contains(WorkspacePermissions.ADMINISTER)) {
                final String[] effectiveGlobalPermissions = client.getWebServiceLayer().queryEffectiveGlobalPermissions(
                    client.getConnection().getAuthorizedIdentity().getUniqueName());

                for (final String permission : effectiveGlobalPermissions) {
                    if (permission.equalsIgnoreCase("AdminWorkspaces")) //$NON-NLS-1$
                    {
                        permissionToCheck = permissionToCheck.remove(WorkspacePermissions.ADMINISTER);
                        break;
                    }
                }
            }

            // At this point, if we still have bits uncleared, we have failed
            // the check.
            return permissionToCheck.isEmpty();
        } else {
            // This server supports workspace permissions, and we are not the
            // owner.
            return getPermissions().contains(permissionToCheck);
        }

    }

    /**
     * Gets the big block of header text that's written at the top of new
     * .tfignore files. This is stored as a classpath resource.
     * <p>
     * Newlines are normalized to the current platform newline sequence.
     *
     * @return the header text as stored in the classpath resource
     * @throws IOException
     *         if an error occurred reading the resource
     */
    private String getTFIgnoreHeader() throws IOException {
        return NewlineUtils.normalizeToPlatform(
            IOUtils.toString(getClass().getClassLoader().getResourceAsStream(TFIGNORE_HEADER_RESOURCE), "UTF-8")); //$NON-NLS-1$
    }

    /**
     * Throws an exception if the workspace is deleted (i.e., something called
     * Delete()).
     */
    private void throwIfDeleted() {
        if (isDeleted()) {
            throw new WorkspaceDeletedException(this);
        }
    }

    /**
     * Calls the security service to update the PermissionsProfile field
     */
    private void refreshPermissionsProfile() {
        throwIfDeleted();

        if (getSecurityToken() != null && client.getWorkspaceSecurity() != null) {
            final AccessControlListDetails allPermissions =
                client.getWorkspaceSecurity().queryAccessControlList(getSecurityToken(), null, false);

            final List<AccessControlEntryDetails> aceList = new ArrayList<AccessControlEntryDetails>();

            if (allPermissions != null) {
                Collections.addAll(aceList, allPermissions.getAccessControlEntries());
            }

            if (aceList.isEmpty()) {
                // Private workspace.
                permissionsProfile = WorkspacePermissionProfile.getPrivateProfile();
                return;
            } else {

                removeWorkspaceOwnerAce(aceList);

                for (final WorkspacePermissionProfile profile : WorkspacePermissionProfile.getBuiltInProfiles()) {
                    if (aceListsMatch(Arrays.asList(profile.getAccessControlEntries()), aceList)) {
                        // All ACEs in ACL must match for the profile to match
                        permissionsProfile = profile;
                        return;
                    }
                }
            }

            // Fallback: If we get here, its a custom profile
            permissionsProfile = new WorkspacePermissionProfile("Custom", aceList.toArray(new AccessControlEntry[0])); //$NON-NLS-1$
        } else {
            permissionsProfile = WorkspacePermissionProfile.getPrivateProfile();
        }
    }

    private void removeWorkspaceOwnerAce(final List<AccessControlEntryDetails> workspaceAceList) {
        final IdentityDescriptor ownerDescriptor = getOwnerDescriptor();

        if (ownerDescriptor != null) {
            for (final AccessControlEntryDetails ace : workspaceAceList) {
                if (IdentityDescriptorComparer.INSTANCE.compare(
                    ace.getSerializableDescriptor(),
                    ownerDescriptor) == 0) {
                    workspaceAceList.remove(ace);
                    return;
                }
            }
        }
    }

    private boolean aceListsMatch(
        final List<AccessControlEntry> profileAceList,
        final List<AccessControlEntryDetails> workspaceAceList) {
        if (profileAceList.size() != workspaceAceList.size()) {
            return false;
        }

        for (final AccessControlEntry profileAce : profileAceList) {
            boolean aceFound = false;

            for (final AccessControlEntryDetails workspaceAce : workspaceAceList) {
                if (acesMatch((AccessControlEntryDetails) profileAce, workspaceAce)) {
                    aceFound = true;
                    break;
                }
            }

            if (!aceFound) {
                return false;
            }
        }

        return true;
    }

    private boolean acesMatch(
        final AccessControlEntryDetails profileAce,
        final AccessControlEntryDetails workspaceAce) {
        if (IdentityDescriptorComparer.INSTANCE.compare(
            profileAce.getSerializableDescriptor(),
            workspaceAce.getSerializableDescriptor()) != 0) {
            return false;
        }

        if (profileAce.getAllow() != workspaceAce.getAllow()) {
            return false;
        }

        if (profileAce.getDeny() != workspaceAce.getDeny()) {
            return false;
        }

        return true;
    }

    public static String computeNewWorkspaceName(final String baseName, final Workspace[] existingWorkspaces) {

        if (existingWorkspaces == null || existingWorkspaces.length == 0) {
            return baseName;
        }

        /*
         * Given a base name like "machinea", we are trying to compute a name
         * that doesn't already exist in the exisstingWorkspaces array. If
         * "machinea" is taken, we try "machinea_1", then "machinea_2", etc.
         */

        String candidateName = baseName;
        boolean keepGoing = true;
        int ix = 0;
        while (keepGoing) {
            boolean matched = false;
            for (int i = 0; !matched && i < existingWorkspaces.length; i++) {
                matched = existingWorkspaces[i].getName().equalsIgnoreCase(candidateName);
                if (matched) {
                    break;
                }
            }
            if (!matched) {
                keepGoing = false;
            } else {
                final String messageFormat = Messages.getString("Workspace.DefaultNewWorkspaceNameFormat"); //$NON-NLS-1$
                candidateName = MessageFormat.format(messageFormat, baseName, Integer.toString(++ix));
            }
        }
        return candidateName;
    }
}
