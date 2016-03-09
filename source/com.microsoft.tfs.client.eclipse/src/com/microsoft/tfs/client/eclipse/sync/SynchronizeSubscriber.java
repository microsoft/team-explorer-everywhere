// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariantComparator;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.vc.PreviewGetCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.CoreAffectedResourceCollector;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.sync.resourcestore.GetOperationResourceStore;
import com.microsoft.tfs.client.eclipse.sync.resourcestore.ResourceStore;
import com.microsoft.tfs.client.eclipse.sync.syncinfo.SynchronizeInfo;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

/**
 * SynchronizeSubscriber is the synchronize Subscriber point. This handles the
 * "synchronization" state of a repository: that is, it detects "outgoing"
 * changes: local changes (be it through the pending changes list or offline
 * changes) and "incoming" changes: changes on the server (ie, the results of
 * "get latest").
 */
public class SynchronizeSubscriber extends Subscriber {
    public static final CodeMarker CODEMARKER_SYNCH_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.eclipse.sync.SynchronizeSubscriber#SynchComplete"); //$NON-NLS-1$
    private static final Log log = LogFactory.getLog(SynchronizeSubscriber.class);

    private static final int PROJECT_REPOSITORY_STATUS_CODE = 1024;

    private final SynchronizeComparator comparator = new SynchronizeComparator();

    // this is the persistent cache of changes on the local and remote trees
    // (using pending changes in the local tree and getops in the remote tree)
    private final ResourceStore<LocalResourceData> localTree = new ResourceStore<LocalResourceData>();

    private final GetOperationResourceStore remoteGetOperationTree = new GetOperationResourceStore();

    // this is a singleton, no need to keep synchronization state (which could
    // potentially be significant) around multiple times
    private static SynchronizeSubscriber instance;

    private final SynchronizeResourceRefresher resourceRefresher = new SynchronizeResourceRefresher();

    /*
     * Clients may defer automatic refreshes (from core events.) See
     * ShareWizard, which pends adds before connecting the projects to the
     * ProjectRepositoryManager.
     */
    private final Object deferLock = new Object();
    private int deferCount = 0;
    private final List<IResource> deferRefreshes = new ArrayList<IResource>();

    private SynchronizeSubscriber() {
        super();
    }

    public static synchronized SynchronizeSubscriber getInstance() {
        if (instance == null) {
            instance = new SynchronizeSubscriber();
        }

        return instance;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getName()
     */
    @Override
    public String getName() {
        return Messages.getString("SynchronizeSubscriber.Name"); //$NON-NLS-1$
    }

    /**
     * Returns the list of root resources this subscriber considers for
     * synchronization. This will be called before members().
     *
     * @return an IResource[] of root-level projects managed by TFS
     */
    @Override
    public IResource[] roots() {
        final IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        final List<IProject> configuredProjects = new ArrayList<IProject>();

        for (int i = 0; i < allProjects.length; i++) {
            if (allProjects[i].isOpen() == false) {
                continue;
            }

            try {
                final String providerName = allProjects[i].getPersistentProperty(TeamUtils.PROVIDER_PROP_KEY);

                if (providerName != null && providerName.equals(TFSRepositoryProvider.PROVIDER_ID)) {
                    configuredProjects.add(allProjects[i]);
                }
            } catch (final CoreException e) {
                log.warn(
                    MessageFormat.format("Could not determine provider for project {0}", allProjects[i].getName()), //$NON-NLS-1$
                    e);
            }
        }

        return configuredProjects.toArray(new IProject[configuredProjects.size()]);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.core.subscribers.Subscriber#members(org.eclipse.core
     * .resources.IResource)
     */
    @Override
    public IResource[] members(final IResource resource) throws TeamException {
        return new IResource[0];
    }

    @Override
    public SyncInfo getSyncInfo(final IResource resource) throws TeamException {
        if (resource == null || !isSupervised(resource)) {
            return null;
        }

        final ProjectRepositoryStatus projectStatus =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectStatus(resource.getProject());
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (projectStatus == ProjectRepositoryStatus.CONNECTING) {
            throw new TeamException(new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, MessageFormat.format(
                //@formatter:off
                Messages.getString("SynchronizeSubscriber.ProjectIsCurrentlyBeingConnectedWaitBeforeSynchronizingFormat"), //$NON-NLS-1$
                //@formatter:on
                resource.getProject()), null));
        } else if (projectStatus == ProjectRepositoryStatus.OFFLINE) {
            throw new TeamException(new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, MessageFormat.format(
                //@formatter:off
                Messages.getString("SynchronizeSubscriber.ProjectIsCurrentlyOfflineReturnOnlineBeforeSynchronizingFormat"), //$NON-NLS-1$
                //@formatter:on
                resource.getProject()), null));
        } else if (repository == null) {
            throw new TeamException(Messages.getString("SynchronizeSubscriber.ResourceNotConnectedToTFS")); //$NON-NLS-1$
        }

        if (!resourceRefresher.containsRepository(repository)) {
            resourceRefresher.addRepository(repository);
        }

        final LocalResourceData localData = localTree.getOperation(resource);
        final GetOperation remoteOperation = remoteGetOperationTree.getOperation(resource);

        final SyncInfo syncInfo = new SynchronizeInfo(repository, resource, localData, remoteOperation, comparator);
        syncInfo.init();

        return syncInfo;
    }

    private void blockForConnection() throws TeamException {
        /* Block until there's a connection finished. This is sort of hacky. */
        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

        while (projectManager.isConnecting()) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                throw new TeamException(
                    Messages.getString("SynchronizeSubscriber.CouldNotWaitForConnectionDueToInterruption")); //$NON-NLS-1$
            }
        }
    }

    /**
     * Determine if a particular resource is managed by TFS, and thus eligable
     * for synchronization through this interface.
     *
     * Note that all IResources passed must be in an IProject which is managed
     * by TFS.
     *
     * This method returns <code>true</code> even for ignored resources. If they
     * are pending changes, they should show as outgoing changes. If they are
     * incoming changes, the ignored items list will not prevent them from being
     * retrieved on a recursive get latest.
     *
     * @see org.eclipse.team.core.subscribers.Subscriber#isSupervised(org.eclipse.core.resources.IResource)
     */
    @Override
    public boolean isSupervised(final IResource resource) throws TeamException {
        blockForConnection();

        // make sure we're the repository provider for this project
        if (TeamUtils.isConfiguredWith(resource.getProject(), TFSRepositoryProvider.PROVIDER_ID) == false) {
            return false;
        }

        /*
         * Return true for all other kinds. Projects are inherently managed
         * (otherwise synchronize isn't very useful). Linked, team ignored,
         * .tpignore, and team private resources will have been filtered from
         * becoming pending changes before synchronize runs and we don't want to
         * hide them if they're inbound (get latest will not ignore them).
         */
        return true;
    }

    /*
     * Refresh the given resources for the given depth.
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.core.subscribers.Subscriber#refresh(org.eclipse.core
     * .resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void refresh(final IResource[] resources, final int depth, final IProgressMonitor monitor)
        throws TeamException {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(monitor, "monitor"); //$NON-NLS-1$

        blockForConnection();

        if (monitor.isCanceled()) {
            return;
        }

        // determine RecursionType based on depth
        RecursionType recursionType;

        if (depth == 0) {
            recursionType = RecursionType.NONE;
        } else if (depth == 1) {
            recursionType = RecursionType.ONE_LEVEL;
        } else {
            recursionType = RecursionType.FULL;
        }

        try {
            final IStatus status = refresh(resources, recursionType, monitor);

            if (!status.isOK() && status.getSeverity() == Status.CANCEL) {
                throw new TeamException(status);
            }
        } catch (final CanceledException e) {
            // Just finish cleanly
        }

        CodeMarkerDispatch.dispatch(CODEMARKER_SYNCH_COMPLETE);
    }

    /**
     * Refresh the given resources for the given RecursionType.
     *
     * @param resources
     *        Resources to refresh
     * @param recursionType
     *        RecursionType to update with
     * @param monitor
     *        An IProgressMonitor to display status
     * @return An IStatus containing the refresh status
     */
    private IStatus refresh(
        final IResource[] resources,
        final RecursionType recursionType,
        final IProgressMonitor monitor) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(monitor, "monitor"); //$NON-NLS-1$

        // a collection of changed resources to fire changed events for
        // (use a hashset since a resource could have multiple changes and we
        // only want to fire once)
        final Collection<IResource> changedResources = new HashSet<IResource>();

        // any errors
        final ArrayList<IStatus> errors = new ArrayList<IStatus>();

        // one tick for updating the pending changes,
        // one tick for each resource...
        monitor.beginTask(Messages.getString("SynchronizeSubscriber.RefreshingTFSResources"), (resources.length + 1)); //$NON-NLS-1$

        // refresh the pending changes caches
        final SubProgressMonitor pcMonitor = new SubProgressMonitor(monitor, 1);
        refreshPendingChanges(resources, pcMonitor);
        pcMonitor.done();

        // refresh each named resource
        for (int i = 0; i < resources.length; i++) {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // update the progress monitor
            monitor.subTask(resources[i].getName());
            final SubProgressMonitor resourceMonitor = new SubProgressMonitor(monitor, 1);

            // refresh a single resource
            final IStatus status = refreshResource(resources[i], recursionType, changedResources, resourceMonitor);

            if (!status.isOK()) {
                errors.add(status);
            }

            resourceMonitor.done();
        }

        monitor.done();

        if (!changedResources.isEmpty()) {
            fireTeamResourceChange(changedResources);
        }

        if (!errors.isEmpty()) {
            /*
             * If all resources failed for the same reason, consolidate the
             * error messages.
             */
            if (errors.size() == resources.length) {
                int errorCode = -1;

                for (final Iterator<IStatus> i = errors.iterator(); i.hasNext();) {
                    final IStatus error = i.next();

                    if ((error.getCode() & PROJECT_REPOSITORY_STATUS_CODE) == 0) {
                        errorCode = -1;
                        break;
                    }

                    final int thisErrorCode = (error.getCode() & (~PROJECT_REPOSITORY_STATUS_CODE));

                    if (errorCode < 0) {
                        errorCode = thisErrorCode;
                    } else if (thisErrorCode != errorCode) {
                        errorCode = -1;
                        break;
                    }
                }

                if (errorCode >= 0) {
                    String message = Messages.getString("SynchronizeSubscriber.ProjectsNotCurrentlyConnectedToTFS"); //$NON-NLS-1$

                    if (errorCode == ProjectRepositoryStatus.CONNECTING.getValue()) {
                        //@formatter:off
                        message = Messages.getString("SynchronizeSubscriber.ProjectsBeingConnectedToTFSWaitBeforeRefreshing"); //$NON-NLS-1$
                        //@formatter:on
                    } else if (errorCode == ProjectRepositoryStatus.OFFLINE.getValue()) {
                        //@formatter:off
                        message = Messages.getString("SynchronizeSubscriber.CurrentlyOfflinePleaseReturnOnlineBeforeRefreshing"); //$NON-NLS-1$
                        //@formatter:on
                    } else if (errorCode == ProjectRepositoryStatus.INITIALIZING.getValue()) {
                        //@formatter:off
                        message = Messages.getString("SynchronizeSubscriber.ProjectsNotConnectedToTFSOrHaveBeenPermanentlyDisconnected"); //$NON-NLS-1$
                        //@formatter:on
                    }

                    return new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, errorCode, message, null);
                }
            }

            /* Not all the same errors */
            final IStatus[] status = errors.toArray(new IStatus[errors.size()]);
            return new MultiStatus(
                TFSEclipseClientPlugin.PLUGIN_ID,
                TeamException.NO_REMOTE_RESOURCE,
                status,
                Messages.getString("SynchronizeSubscriber.SomeResourcesCouldNotBeRefreshed"), //$NON-NLS-1$
                null);
        }

        return Status.OK_STATUS;
    }

    /**
     * Refresh the pending changes cache for all repositories containing the
     * given resources.
     *
     * @param resources
     *        All IResources to update pending changes in the repositories for
     * @param monitor
     *        An IProgressMonitor to update (not null)
     */
    private void refreshPendingChanges(final IResource[] resources, final IProgressMonitor monitor) {
        Check.notNull(resources, "resources"); //$NON-NLS-1$
        Check.notNull(monitor, "monitor"); //$NON-NLS-1$

        final Map<TFSRepository, Boolean> repositoryMap = new HashMap<TFSRepository, Boolean>();

        monitor.beginTask(Messages.getString("SynchronizeSubscriber.RefreshingPendingChanges"), 1); //$NON-NLS-1$

        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

        for (int i = 0; i < resources.length; i++) {
            final TFSRepository repository = projectManager.getRepository(resources[i].getProject());

            /*
             * The repository may be null if the user has deleted the project
             * since the last synchronize but the resource remained in the view
             * (and is being refreshed).
             */
            if (repository != null && !repositoryMap.containsKey(repository)) {
                repository.getPendingChangeCache().refresh();
                repositoryMap.put(repository, Boolean.TRUE);
            }
        }

        monitor.worked(1);
    }

    /**
     * Refresh a single resource (typically an IProject, but could be any
     * IResource).
     *
     * @param resource
     *        The Resource to update
     * @param recursionType
     *        The RecursionType to update with
     * @param changedResources
     *        A list of resources which have changed, which will be added to
     *        with any changed resources
     */
    private IStatus refreshResource(
        final IResource resource,
        final RecursionType recursionType,
        final Collection<IResource> changedResources,
        final IProgressMonitor monitor) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(changedResources, "changedResources"); //$NON-NLS-1$

        log.info(MessageFormat.format("Computing synchronization data for {0}", resource)); //$NON-NLS-1$

        monitor.beginTask(MessageFormat.format(
            Messages.getString("SynchronizeSubscriber.RefreshingResourceFormat"), //$NON-NLS-1$
            resource.getName()), 2);

        final ProjectRepositoryStatus projectStatus =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectStatus(resource.getProject());

        if (projectStatus == ProjectRepositoryStatus.CONNECTING) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                (PROJECT_REPOSITORY_STATUS_CODE | projectStatus.getValue()),
                MessageFormat.format(
                    //@formatter:off
                    Messages.getString("SynchronizeSubscriber.ProjectIsCurrentlyBeingConnectedCannotRefreshUntilCompleteFormat"), //$NON-NLS-1$
                    //@formatter:on
                    resource.getProject().getName()),
                null);
        } else if (projectStatus == ProjectRepositoryStatus.OFFLINE) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                (PROJECT_REPOSITORY_STATUS_CODE | projectStatus.getValue()),
                MessageFormat.format(
                    Messages.getString("SynchronizeSubscriber.ProjectOfflineCannotRefreshFormat"), //$NON-NLS-1$
                    resource.getProject().getName()),
                null);
        } else if (projectStatus == ProjectRepositoryStatus.INITIALIZING) {
            /*
             * This happens if the user synchronized a project then disconnected
             * it from our repository provider.
             */
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                (PROJECT_REPOSITORY_STATUS_CODE | projectStatus.getValue()),
                MessageFormat.format(
                    //@formatter:off
                    Messages.getString("SynchronizeSubscriber.ProjectNotConnectedToTFSOrHaveBeenPermanentlyDisconnectedFormat"), //$NON-NLS-1$
                    //@formatter:on
                    resource.getProject().getName()),
                null);
        }

        // determine the TFSRepository for this resource
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(resource.getProject());

        if (repository == null) {
            return new Status(
                Status.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                TeamException.NO_REMOTE_RESOURCE,
                MessageFormat.format(
                    Messages.getString("SynchronizeSubscriber.CouldNotDetermineRepositoryForResourceFormat"), //$NON-NLS-1$
                    resource),
                null);
        }

        final SubProgressMonitor localMonitor = new SubProgressMonitor(monitor, 1);
        final SubProgressMonitor remoteMonitor = new SubProgressMonitor(monitor, 1);

        final IStatus localStatus =
            refreshLocalResource(repository, resource, recursionType, changedResources, localMonitor);
        final IStatus remoteStatus =
            refreshRemoteResource(repository, resource, recursionType, changedResources, remoteMonitor);

        monitor.done();

        if (!localStatus.isOK()) {
            return localStatus;
        }
        if (!remoteStatus.isOK()) {
            return remoteStatus;
        }

        return Status.OK_STATUS;
    }

    /**
     * Refresh the local changes list. We do this by refreshing the local
     * PendingChanges and determining which local resources are affected.
     *
     * @param repository
     *        the TFSRepository that contains this resource
     * @param resource
     *        resource to refresh
     * @param recursionType
     *        recursion level to refresh for
     * @param changedResources
     *        a List to which updated resources will be added
     * @return an IStatus reflecting success or failure
     */
    private IStatus refreshLocalResource(
        final TFSRepository repository,
        final IResource resource,
        final RecursionType recursionType,
        final Collection<IResource> changedResources,
        final IProgressMonitor monitor) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$
        Check.notNull(changedResources, "changedResources"); //$NON-NLS-1$

        monitor.beginTask(
            MessageFormat.format(
                Messages.getString("SynchronizeSubscriber.RefreshingResourceLocalChangesFormat"), //$NON-NLS-1$
                resource.getName()),
            1);

        /*
         * fire a changed event for old affected resources too (if they're not
         * affected now, but were before, then they've changed on the server and
         * we need to fire an event for them too)
         */
        changedResources.addAll(localTree.removeOperation(resource));

        /*
         * We must iterate over all pending changes to see if this resource is
         * affected by them. Can't simply call
         * getPendingChangesByLocalPathRecursive, as that neglects source path.
         */
        final PendingChange[] pendingChanges = repository.getPendingChangeCache().getPendingChanges();

        /*
         * A List of local resources that need item queries (see below.)
         */
        final List<LocalResourceData> needsItemQuery = new ArrayList<LocalResourceData>();

        for (int i = 0; i < pendingChanges.length; i++) {
            if (isAffected(repository, resource, pendingChanges[i])) {
                final IResource[] affectedResources =
                    getAffectedResources(repository, resource, pendingChanges[i], recursionType);

                if (affectedResources.length == 0) {
                    continue;
                }

                final LocalResourceData resourceData = new LocalResourceData(pendingChanges[i]);

                /*
                 * If this is a pending delete, then we need to query the item
                 * for more information. This is to work around new behavior in
                 * 2010: if there's is a delete on an item (note: does not
                 * behave this way for source renames), we do NOT get a conflict
                 * on get when the remote is modified. We only get a conflict on
                 * checkin. Thus, we need to determine if we are NOT deleting
                 * the latest version in order to correctly paint the conflict.
                 */
                if (pendingChanges[i].getChangeType().contains(ChangeType.DELETE)
                    && pendingChanges[i].getServerItem() != null) {
                    needsItemQuery.add(resourceData);
                }

                for (int j = 0; j < affectedResources.length; j++) {
                    if (log.isDebugEnabled()) {
                        log.debug(MessageFormat.format(
                            "Resource {0} is affected by pending change of type {1}", //$NON-NLS-1$
                            affectedResources[j],
                            pendingChanges[i].getChangeType().toUIString(true, pendingChanges[i])));
                    }

                    localTree.addOperation(affectedResources[j], resourceData);
                    changedResources.add(affectedResources[j]);
                }
            }
        }

        /* Query items for any items that need more data. */
        if (needsItemQuery.size() > 0) {
            final ItemSpec[] itemSpecs = new ItemSpec[needsItemQuery.size()];

            for (int i = 0; i < needsItemQuery.size(); i++) {
                itemSpecs[i] =
                    new ItemSpec(needsItemQuery.get(i).getPendingChange().getServerItem(), RecursionType.NONE);
            }

            final ItemSet[] itemSet = repository.getWorkspace().getClient().getItems(
                itemSpecs,
                LatestVersionSpec.INSTANCE,
                DeletedState.ANY,
                ItemType.FILE,
                true);

            if (itemSet.length != needsItemQuery.size()) {
                log.warn(
                    MessageFormat.format(
                        "Could not query items: requested data on {0} items, received data for {1}", //$NON-NLS-1$
                        Integer.toString(needsItemQuery.size()),
                        Integer.toString(itemSet.length)));
            } else {
                for (int i = 0; i < needsItemQuery.size(); i++) {
                    final LocalResourceData resourceData = needsItemQuery.get(i);
                    final Item[] items = itemSet[i].getItems();

                    if (items.length == 1
                        && items[0] != null
                        && items[0].getChangeSetID() > resourceData.getPendingChange().getVersion()) {
                        needsItemQuery.get(i).setItem(items[0]);
                    }
                }
            }
        }

        monitor.worked(1);
        monitor.done();

        return Status.OK_STATUS;
    }

    private boolean isAffected(
        final TFSRepository repository,
        final IResource resource,
        final PendingChange pendingChange) {
        final String localPath = resource.getLocation().toOSString();

        /*
         * Note that isChild tests both path equality and parent / child
         * relationship
         */

        if (pendingChange.getSourceLocalItem() != null
            && LocalPath.isChild(localPath, pendingChange.getSourceLocalItem())) {
            return true;
        }

        if (pendingChange.getSourceServerItem() != null) {
            final String pendingChangeLocalPath =
                repository.getWorkspace().getMappedLocalPath(pendingChange.getSourceServerItem());

            if (pendingChangeLocalPath != null && LocalPath.isChild(localPath, pendingChangeLocalPath)) {
                return true;
            }
        }

        if (pendingChange.getLocalItem() != null && LocalPath.isChild(localPath, pendingChange.getLocalItem())) {
            return true;
        }

        if (pendingChange.getServerItem() != null) {
            final String pendingChangeLocalPath =
                repository.getWorkspace().getMappedLocalPath(pendingChange.getServerItem());

            if (pendingChangeLocalPath != null && LocalPath.isChild(localPath, pendingChangeLocalPath)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets a list of resources at or beneath resource affected by a pending
     * change. Inspects all server and local resources
     *
     * @param resource
     *        base (likely container) resource to examine
     * @param pendingChange
     *        pending change that affects resources
     * @param recursionType
     *        recursion type to look for affected resources at
     * @return array of IResources affected by the pending change
     */
    private IResource[] getAffectedResources(
        final TFSRepository repository,
        final IResource resource,
        final PendingChange pendingChange,
        final RecursionType recursionType) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$

        final List<String> serverPathList = new ArrayList<String>();
        List<String> localPathList = new ArrayList<String>();
        final List<IResource> resourceList = new ArrayList<IResource>();

        final String resourcePath = resource.getLocation().toOSString();
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();

        /*
         * Typically we care about the source and target paths (ie, for renames
         * and merges.) For branches, however, we care only about the target
         * name. This avoids us painting sync info for the source of a branch
         * (which is unchanged by this pending change.)
         */

        // add local paths
        if (!pendingChange.getChangeType().contains(ChangeType.BRANCH) && pendingChange.getSourceLocalItem() != null) {
            localPathList.add(pendingChange.getSourceLocalItem());
        }
        if (pendingChange.getLocalItem() != null) {
            localPathList.add(pendingChange.getLocalItem());
        }

        // deal with server paths - this involves actually querying the
        // workspace to unravel mapped paths
        if (!pendingChange.getChangeType().contains(ChangeType.BRANCH) && pendingChange.getSourceServerItem() != null) {
            serverPathList.add(pendingChange.getSourceServerItem());
        }
        if (pendingChange.getServerItem() != null) {
            serverPathList.add(pendingChange.getServerItem());
        }

        // get local paths by querying the workspace for the server paths
        for (final String serverPath : serverPathList) {
            String mappedPath = null;

            try {
                mappedPath = repository.getWorkspace().getMappedLocalPath(serverPath);
            } catch (final ServerPathFormatException e) {
            }

            if (mappedPath == null) {
                continue;
            }

            localPathList.add(LocalPath.canonicalize(LocalPath.tfsToNative(mappedPath)));
        }

        // remove duplicate local paths. (we could have a local path specified
        // as the mapped product of a server path AND as a local path in a
        // pending change.) we use a collator here because TFS / Windows tends
        // to use Unicode continuation chars, while MacOS does not. thus, we
        // need to squash these into the same type
        localPathList = getUniquePaths(localPathList);

        // now examine our local paths
        for (final String localPath : localPathList) {
            if (isAffected(resourcePath, localPath, recursionType)) {
                IResource localResource;

                if (pendingChange.getItemType() == ItemType.FILE) {
                    localResource = workspaceRoot.getFileForLocation(new Path(localPath));
                } else {
                    localResource = workspaceRoot.getContainerForLocation(new Path(localPath));
                }

                if (localResource != null && !resourceList.contains(localResource)) {
                    resourceList.add(localResource);
                }
            }
        }

        return resourceList.toArray(new IResource[resourceList.size()]);
    }

    /**
     * Given a path to a resource and a path to a pending change and recursion
     * level, determines whether the resource is affected by the change.
     *
     * @param resourcePath
     *        path to the resource to examine
     * @param changed
     *        path to a pending change
     * @param recursionType
     *        Recursion level to examine
     * @return true if resourcePath is affected by the change to changed
     */
    private boolean isAffected(final String resourcePath, final String changed, final RecursionType recursionType) {
        if (recursionType == RecursionType.FULL && LocalPath.isChild(resourcePath, changed)) {
            return true;
        } else if (recursionType == RecursionType.NONE && LocalPath.equals(resourcePath, changed)) {
            return true;
        } else if (recursionType == RecursionType.ONE_LEVEL
            && (LocalPath.equals(resourcePath, changed)
                || LocalPath.equals(resourcePath, LocalPath.getDirectory(changed)))) {
            return true;
        }

        return false;
    }

    /**
     * Refresh the list of remotely-changed resources -- those which are
     * implicated in a pending get. This should include even resources that do
     * not exist on the local file system (ie, remote adds.)
     *
     * @param resource
     *        the base IResource to refresh
     * @param recursionType
     *        how deep to recurse
     * @param changedResources
     *        a List to store changed resources
     * @return IStatus.OK on success, an IStatus indicating error otherwise
     */
    private IStatus refreshRemoteResource(
        final TFSRepository repository,
        final IResource resource,
        RecursionType recursionType,
        final Collection<IResource> changedResources,
        final IProgressMonitor monitor) {
        monitor.beginTask(
            MessageFormat.format(
                Messages.getString("SynchronizeSubscriber.RefreshingResourceRemoteChangesFormat"), //$NON-NLS-1$
                resource.getName()),
            1);

        // double-check recursion type - set to None for files to avoid stupid
        // tfs behavior
        if (resource.getType() == IResource.FILE) {
            recursionType = RecursionType.NONE;
        }

        // fire a changed event for old affected resources too (if they're
        // not affected now, but were before, then they've changed on the
        // server and we need to fire an event for them too)
        changedResources.addAll(remoteGetOperationTree.removeOperation(resource));

        // setup the request item
        final ItemSpec itemSpec = new ItemSpec(resource.getLocation().toOSString(), recursionType);
        final GetRequest[] requests = new GetRequest[] {
            new GetRequest(itemSpec, LatestVersionSpec.INSTANCE)
        };

        log.debug(MessageFormat.format("Previewing latest server state for {0}", resource.getLocation().toOSString())); //$NON-NLS-1$

        // get the operations that would be done if we were to do a get latest
        final PreviewGetCommand getCommand = new PreviewGetCommand(repository, requests, GetOptions.PREVIEW);

        try {
            final IStatus status = getCommand.run(monitor);

            if (status == null) {
                throw new Exception(Messages.getString("SynchronizeSubscriber.CouldNotExecuteCommand")); //$NON-NLS-1$
            } else if (!status.isOK()) {
                return status;
            }
        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        } catch (final Exception e) {
            return new TeamStatus(
                IStatus.ERROR,
                TFSEclipseClientPlugin.PLUGIN_ID,
                TeamException.IO_FAILED,
                Messages.getString("SynchronizeSubscriber.CouldNotGetStatusFromServer"), //$NON-NLS-1$
                e,
                resource);
        }

        final GetOperation[][] operations = getCommand.getOperations();

        // walk through each resultant Get Operation
        for (int i = 0; i < operations.length; i++) {
            // do a quick sort so that children are guaranteed to fall under
            // their parents.
            Arrays.sort(operations[i]);

            for (int j = 0; j < operations[i].length; j++) {
                final IResource[] resources = getAffectedResources(operations[i][j]);

                if (resources == null || resources.length == 0) {
                    return new TeamStatus(
                        IStatus.ERROR,
                        TFSEclipseClientPlugin.PLUGIN_ID,
                        TeamException.UNABLE,
                        MessageFormat.format(
                            Messages.getString("SynchronizeSubscriber.CouldNotDetermineLocalResourceForItemFormat"), //$NON-NLS-1$
                            operations[i][j].getTargetServerItem()),
                        null,
                        resource);
                }

                for (int k = 0; k < resources.length; k++) {
                    if (log.isDebugEnabled()) {
                        log.debug(MessageFormat.format(
                            "Resource {0} is affected by incoming operation of version {1}", //$NON-NLS-1$
                            resources[k],
                            operations[i][j].getVersionServer()));
                    }

                    remoteGetOperationTree.addOperation(resources[k], operations[i][j]);
                    changedResources.add(resources[k]);
                }
            }
        }

        monitor.worked(1);
        monitor.done();

        return Status.OK_STATUS;
    }

    /**
     * Determine the local workspace resource for a given GetOperation. Note
     * that a get operation CAN affect multiple resources, in the case of a
     * rename. (sourceLocalItem != targetLocalItem)
     *
     * @param operation
     *        An AGetOperation to get the resource for
     * @return All IResources for the file/folder represented by the operation
     */
    private IResource[] getAffectedResources(final GetOperation operation) {
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        List<String> localPaths = new ArrayList<String>();
        final List<IResource> localResources = new ArrayList<IResource>();

        // on a delete, our local resource is the source item
        if (operation.getCurrentLocalItem() != null && operation.getTargetLocalItem() == null) {
            localPaths.add(operation.getCurrentLocalItem());
        }

        // on an add, our local resource is going to be the target item
        else if (operation.getCurrentLocalItem() == null && operation.getTargetLocalItem() != null) {
            localPaths.add(operation.getTargetLocalItem());
        }

        // on a rename, our local resource is the target item
        // (the source will be implicated in a delete)
        else if (!operation.getCurrentLocalItem().equals(operation.getTargetLocalItem())) {
            localPaths.add(operation.getCurrentLocalItem());
            localPaths.add(operation.getTargetLocalItem());
        }

        else {
            localPaths.add(operation.getCurrentLocalItem());
        }

        // get the unique paths from all these...
        localPaths = getUniquePaths(localPaths);

        // convert local paths to local resources
        for (final String tfsPath : localPaths) {
            final Path local = new Path(LocalPath.tfsToNative(tfsPath));
            IResource resource;

            if (operation.getItemType() == ItemType.FILE) {
                resource = workspaceRoot.getFileForLocation(local);
            } else {
                resource = workspaceRoot.getContainerForLocation(local);
            }

            if (resource != null) {
                localResources.add(resource);
            }
        }

        return localResources.toArray(new IResource[localResources.size()]);
    }

    /**
     * Given a list of paths, this will remove any duplicates. It uses a
     * Collator to detect duplicates for Unicode safety.
     *
     * @param paths
     *        List of paths to uniqueify
     * @return List of unique paths specified
     */
    private List<String> getUniquePaths(final List<String> paths) {
        final Collator collator = Collator.getInstance(Locale.US);
        final ArrayList<String> uniquePaths = new ArrayList<String>();

        for (final String givenPath : paths) {
            boolean duplicate = false;

            for (final String existingPath : uniquePaths) {
                if (collator.compare(givenPath, existingPath) == 0) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                uniquePaths.add(givenPath);
            }
        }

        return uniquePaths;
    }

    /**
     * Fire a TeamResourceChange event from a List
     *
     * @param changed
     *        List of IResources that have changed
     */
    private void fireTeamResourceChange(final Collection<IResource> changed) {
        if (changed.size() > 0) {
            final IResource[] changedResources = changed.toArray(new IResource[changed.size()]);
            fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, changedResources));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.team.core.subscribers.Subscriber#getResourceComparator()
     */
    @Override
    public IResourceVariantComparator getResourceComparator() {
        return comparator;
    }

    public void deferAutomaticRefresh() {
        synchronized (deferLock) {
            deferCount++;
        }
    }

    public void continueAutomaticRefresh() {
        IResource[] refreshResources = null;

        synchronized (deferLock) {
            deferCount--;

            if (deferCount == 0) {
                refreshResources = deferRefreshes.toArray(new IResource[deferRefreshes.size()]);
                deferRefreshes.clear();
            }
        }

        if (refreshResources != null && refreshResources.length > 0) {
            refreshBackground(refreshResources);
        }
    }

    private final void refreshBackground(final IResource[] resources) {
        new Job(Messages.getString("SynchronizeSubscriber.ExaminingSynchronizationState")) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    refresh(resources, IResource.DEPTH_INFINITE, monitor);

                    return Status.OK_STATUS;
                } catch (final CoreException e) {
                    log.warn(Messages.getString("SynchronizeSubscriber.CouldNotRefreshSynchronizationState"), e); //$NON-NLS-1$

                    return new Status(
                        IStatus.ERROR,
                        TFSEclipseClientPlugin.PLUGIN_ID,
                        0,
                        e.getLocalizedMessage(),
                        null);
                }
            }
        }.schedule();
    }

    private final class SynchronizeResourceRefresher extends CoreAffectedResourceCollector {
        @Override
        protected void resourcesChanged(final Set<IResource> resourceSet) {
            synchronized (deferLock) {
                if (deferCount > 0) {
                    deferRefreshes.addAll(resourceSet);
                    return;
                }
            }

            final IResource[] resources = resourceSet.toArray(new IResource[resourceSet.size()]);
            refreshBackground(resources);
        }
    }
}
