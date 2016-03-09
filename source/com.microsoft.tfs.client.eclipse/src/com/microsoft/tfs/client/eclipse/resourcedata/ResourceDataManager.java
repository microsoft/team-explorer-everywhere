// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTableLock;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ExtendedItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * Manages the storage and retrieval of persistent data associated with a
 * resource, using an Eclipse {@link IWorkspace}'s {@link ISynchronizer}. Much
 * faster than persistent properties on resources when large numbers of
 * resources are involved (persistent properties are written to disk immediately
 * on every update).
 * </p>
 *
 * @threadsafety thread-safe
 */
public class ResourceDataManager {
    private static final Log log = LogFactory.getLog(ResourceDataManager.class);

    /**
     * {@link ISynchronizer}s require a qualified partner name.
     */
    private static final QualifiedName SYNCHRONIZER_RESOURCE_DATA_PARTNER_QNAME =
        new QualifiedName(TFSEclipseClientPlugin.PLUGIN_ID, "resourceData"); //$NON-NLS-1$

    /**
     * The name of the persistent property saved on {@link IProject}s to mark
     * that they have had persistent property information set on them in the
     * past (so they are not automatically refreshed by QueryItems when
     * connected).
     */
    private static final QualifiedName PROJECT_HAS_DATA_PROPERTY_QNAME =
        new QualifiedName(TFSEclipseClientPlugin.PLUGIN_ID, "hasResourceData"); //$NON-NLS-1$

    /**
     * The value stored in the persistent property named
     * {@link #PROJECT_HAS_DATA_PROPERTY_QNAME}.
     */
    private static final String PROJECT_HAS_DATA_PROPERTY_TRUTH_VALUE = "true"; //$NON-NLS-1$

    /**
     * The number of retries for getting local workspace item information. We
     * need these because the local workspace files may be locked by other
     * threads/processes.
     */
    private static final int LOCAL_REFRESH_TRIES = 5;

    /**
     * Listens to core events from active {@link TFSRepository} instances and
     * mostly just queues updates for those events.
     */
    private final CoreEventsListener repositoryCoreEventsListener;

    /**
     * A map of {@link TFSRepository} to {@link Set}s of {@link IProject}s. The
     * {@link Set} contains all the {@link IProject}s active for the the key
     * repository, and will never go empty (the entry for that
     * {@link TFSRepository} is removed from the map instead).
     * <p>
     * If a {@link TFSRepository} is a key in this map, it has active projects,
     * and this class is listening to events from that repository. When a
     * {@link TFSRepository} entry is removed from the map, this class stops
     * listening to events from it.
     */
    private final Map<TFSRepository, Set<IProject>> repositoryToProjectSetMap =
        new HashMap<TFSRepository, Set<IProject>>();

    /**
     * Synchronizes access to {@link #repositoryToProjectSetMap}.
     */
    private final Object repositoryMapLock = new Object();

    /**
     * These listeners are notified after we update resource data.
     */
    protected final SingleListenerFacade resourceDataListeners = new SingleListenerFacade(ResourceDataListener.class);

    /**
     * A read cache for the persistent property that tracks whether a project
     * has had its full refresh completed.
     */
    private final Set<IProject> projectsWithCompletedDataCache = Collections.synchronizedSet(new HashSet<IProject>());

    /**
     * The workspace's {@link ISynchronizer} where resource data will be
     * persisted.
     */
    private final ISynchronizer synchronizer;

    /**
     * Background updates being deferred.
     */
    private final Object deferLock = new Object();
    private int deferCount = 0;
    private final Map<TFSRepository, Set<IResource>> deferMap = new HashMap<TFSRepository, Set<IResource>>();

    public ResourceDataManager(final ISynchronizer synchronizer) {
        Check.notNull(synchronizer, "synchronizer"); //$NON-NLS-1$
        this.synchronizer = synchronizer;

        /*
         * Synchronizers require partner registration before use. It's OK to
         * register the same partner multiple times.
         */
        this.synchronizer.add(SYNCHRONIZER_RESOURCE_DATA_PARTNER_QNAME);

        repositoryCoreEventsListener = new CoreEventsListener(this);
    }

    /**
     * Defers any automatic resource data updates that are performed when new
     * projects are added. Background resource data updates will not be
     * performed until {@link #continueAutomaticRefresh()} is called.
     */
    public void deferAutomaticRefresh() {
        synchronized (deferLock) {
            deferCount++;
        }
    }

    /**
     * Continues any automatic resource data updates that were paused using
     * {@link #deferAutomaticRefresh()}. Any waiting resource data updates will
     * be performed as a background job.
     */
    public void continueAutomaticRefresh() {
        synchronized (deferLock) {
            deferCount--;

            if (deferCount == 0) {
                for (final Iterator<Entry<TFSRepository, Set<IResource>>> i =
                    deferMap.entrySet().iterator(); i.hasNext();) {
                    final Entry<TFSRepository, Set<IResource>> deferEntry = i.next();
                    final IResource[] deferredResources =
                        deferEntry.getValue().toArray(new IResource[deferEntry.getValue().size()]);

                    refreshAsyncInternal(deferEntry.getKey(), deferredResources);

                    i.remove();
                }
            }
        }
    }

    /**
     * Adds a project to the resource data manager. Events for the project's
     * {@link TFSRepository} and {@link IWorkspace} are hooked up and changes to
     * resources detected in these events cause automatic persistent resource
     * data updates. Call {@link #removeProject(TFSRepository, IProject)} to
     * unhook the events and cause the updates to stop (existing persistent
     * resource data is not deleted when
     * {@link #removeProject(TFSRepository, IProject)} is called).
     *
     * @param repository
     *        the project's repository (must not be <code>null</code>)
     * @param project
     *        the project to add (must not be <code>null</code>)
     */
    public void addProject(final TFSRepository repository, final IProject project) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(project, "project"); //$NON-NLS-1$

        synchronized (repositoryMapLock) {
            hookRepositoryEvents(project, repository);

            /*
             * Schedule a job to query the information from the server if this
             * project is new to the ResourceDataManager. When the async job
             * finishes, it sets the persistent property (so it's not queried
             * next time).
             */
            if (hasCompletedRefresh(project) == false) {
                refreshAsync(repository, new IProject[] {
                    project
                });
            }
        }
    }

    public void removeProject(final TFSRepository repository, final IProject project) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(project, "project"); //$NON-NLS-1$

        synchronized (repositoryMapLock) {
            unhookRepositoryEvents(project, repository);
        }

        /*
         * Purge it from the cached list of projects with data.
         */
        projectsWithCompletedDataCache.remove(project);
    }

    private void hookRepositoryEvents(final IProject project, final TFSRepository repository) {
        Set<IProject> projectSet;

        /*
         * If we haven't seen this TFSRepository yet, hook up core event
         * listeners. These events happen whenever core operations happy (get,
         * undo, checkin .etc.), and this class generally queues updates in
         * response.
         */
        if (repositoryToProjectSetMap.containsKey(repository) == false) {
            /*
             * Create a new set and add the set for the key.
             */
            projectSet = new HashSet<IProject>();
            repositoryToProjectSetMap.put(repository, projectSet);

            repository.getWorkspace().getClient().getEventEngine().addOperationStartedListener(
                repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addCheckinListener(repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addGetListener(repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addOperationCompletedListener(
                repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addNewPendingChangeListener(
                repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addConflictResolvedListener(
                repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addUndonePendingChangeListener(
                repositoryCoreEventsListener);
            repository.getWorkspace().getClient().getEventEngine().addWorkspaceUpdatedListener(
                repositoryCoreEventsListener);
        } else {
            projectSet = repositoryToProjectSetMap.get(repository);
        }

        // Always add the project (it may already be in the set, no harm).
        projectSet.add(project);
    }

    private void unhookRepositoryEvents(final IProject project, final TFSRepository repository) {
        if (repositoryToProjectSetMap.containsKey(repository)) {
            final Set<IProject> projectSet = repositoryToProjectSetMap.get(repository);
            Check.notNull(projectSet, "projectSet"); //$NON-NLS-1$

            projectSet.remove(project);

            /*
             * Clear out the entry if we have no more projects for this
             * repository.
             */
            if (projectSet.isEmpty()) {
                repository.getWorkspace().getClient().getEventEngine().removeOperationStartedListener(
                    repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeCheckinListener(
                    repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeGetListener(repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeOperationCompletedListener(
                    repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeNewPendingChangeListener(
                    repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeConflictResolvedListener(
                    repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeUndonePendingChangeListener(
                    repositoryCoreEventsListener);
                repository.getWorkspace().getClient().getEventEngine().removeWorkspaceUpdatedListener(
                    repositoryCoreEventsListener);

                /*
                 * Remove from the map after unhooking events so any events
                 * fired really late can still consult the map.
                 */
                repositoryToProjectSetMap.remove(repository);
            }
        }
    }

    /**
     * Tests whether the given project has had its data refreshed (the call to
     * {@link #forceRefresh(TFSRepository, IProject[])} or
     * {@link #forceRefreshAsync(TFSRepository, IProject[])} has completed).
     *
     * @param project
     *        the project to test (must not be <code>null</code>)
     * @return <code>true</code> if the resources in the given project have
     *         data, <code>false</code> if they do not have data (the job may
     *         not have finished yet)
     */
    public boolean hasCompletedRefresh(final IProject project) {
        /*
         * Persistent properties are slow (even to read), and this method may be
         * called often, so check the cache first.
         */
        if (projectsWithCompletedDataCache.contains(project)) {
            return true;
        }

        boolean hasData = false;

        try {
            hasData = PROJECT_HAS_DATA_PROPERTY_TRUTH_VALUE.equals(
                project.getPersistentProperty(PROJECT_HAS_DATA_PROPERTY_QNAME));

            /*
             * Add it to the cache for faster lookups later.
             */
            if (hasData) {
                projectsWithCompletedDataCache.add(project);
            }
        } catch (final CoreException e) {
            TFSEclipseClientPlugin.getDefault().getLog().log(e.getStatus());
        }

        return hasData;
    }

    protected void setCompletedRefresh(final IProject project) {
        try {
            project.setPersistentProperty(
                ResourceDataManager.PROJECT_HAS_DATA_PROPERTY_QNAME,
                ResourceDataManager.PROJECT_HAS_DATA_PROPERTY_TRUTH_VALUE);

            projectsWithCompletedDataCache.add(project);
        } catch (final CoreException e) {
            TFSEclipseClientPlugin.getDefault().getLog().log(e.getStatus());
        }
    }

    /**
     * Like {@link #getResourceData(IResource)} but faster, because it only
     * checks that there is sync info for the given resource (the byte array is
     * not null); it does not deserialize the data.
     *
     * @param resource
     *        the resource to check for resource data for (must not be
     *        <code>null</code>)
     * @return true if the given resource has resource data, false if the
     *         resource does not have data
     */
    public boolean hasResourceData(final IResource resource) {
        try {
            return synchronizer.getSyncInfo(SYNCHRONIZER_RESOURCE_DATA_PARTNER_QNAME, resource) != null;
        } catch (final CoreException e) {
            log.debug(MessageFormat.format(
                "Could not test for existence of sync info for {0}", //$NON-NLS-1$
                resource.getLocation().toOSString()), e);
            return false;
        }
    }

    /**
     * Gets the data associated with the given resource (which may be an
     * {@link IProject}).
     *
     * @param resource
     *        the resource to get data for (must not be <code>null</code>)
     * @return the {@link ResourceData} previously set on this resource, or
     *         <code>null</code> if no resource data has been set
     */
    public ResourceData getResourceData(final IResource resource) {
        byte[] value = null;
        try {
            value = synchronizer.getSyncInfo(SYNCHRONIZER_RESOURCE_DATA_PARTNER_QNAME, resource);
        } catch (final CoreException e) {
            log.debug(MessageFormat.format("Could not load sync info for {0}", resource.getLocation().toOSString()), e); //$NON-NLS-1$
        }

        if (value == null) {
            return null;
        }

        return ResourceData.fromByteArray(value);
    }

    /**
     * Sets data for the given resource (which may be an {@link IProject}). Pass
     * <code>null</code> for the resourceData to remove any existing data.
     *
     * @param resource
     *        the resource to set data for (must not be <code>null</code>)
     * @param resourceData
     *        the resource data, which may be <code>null</code>
     */
    void setResourceDataInternal(final IResource resource, final ResourceData resourceData) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        /*
         * NOTE: this refresh is probably not necessary. It is believed to be
         * legacy from before ResourceRefreshManager existed. Validate this
         * assumption and remove if possible (or document the reason why this is
         * necessary.)
         */
        try {
            final int depth = (resource.getType() == IResource.FILE) ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
            resource.refreshLocal(depth, new NullProgressMonitor());
        } catch (final Throwable t) {
            /*
             * The resource may have just been deleted. If not, the error may be
             * interesting.
             */
            if (resource.exists()) {
                log.warn(MessageFormat.format("Could not refresh resource {0}", resource.getLocation().toOSString())); //$NON-NLS-1$
            }
        }

        /* Continue to update resource data even if the refresh failed. */

        try {
            if (log.isTraceEnabled()) {
                log.trace(MessageFormat.format("Set {0} = {1}", resource.toString(), (resourceData == null ? "<null>" //$NON-NLS-1$ //$NON-NLS-2$
                    : resourceData.toString())));
            }

            synchronizer.setSyncInfo(
                SYNCHRONIZER_RESOURCE_DATA_PARTNER_QNAME,
                resource,
                (resourceData != null) ? resourceData.toByteArray() : null);
        } catch (final Throwable t) {
            /*
             * The resource may have just been deleted. If not, the error may be
             * interesting.
             */
            if (resource.exists()) {
                log.warn(MessageFormat.format(
                    "Could not save sync info for resource {0}", //$NON-NLS-1$
                    resource.getLocation().toString()), t);
            }
        }
    }

    /**
     * Queries item information (via {@link VersionControlClient#getItems()})
     * recursively for the given resources, and updates all the resources given
     * that match returned server items.
     * <p>
     * Resources which have resource data already, but do not appear in the
     * server's returned items, are not updated (their data is not deleted).
     * <p>
     * This method will refresh the resource data in another thread, and these
     * updates may be queued until another thread completes work (to prevent
     * contention on other long-running tasks that need to take locks.)
     *
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param resources
     *        the resources to refresh (must not be <code>null</code>)
     */
    public void refreshAsync(final TFSRepository repository, final IResource[] resources) {
        synchronized (deferLock) {
            if (deferCount > 0) {
                Set<IResource> deferredForRepository = deferMap.get(repository);

                if (deferredForRepository == null) {
                    deferredForRepository = new HashSet<IResource>();
                    deferMap.put(repository, deferredForRepository);
                }

                for (int i = 0; i < resources.length; i++) {
                    deferredForRepository.add(resources[i]);
                }
            } else {
                refreshAsyncInternal(repository, resources);
            }
        }
    }

    /**
     * Creates and schedules a {@link Job} which runs
     * {@link #forceRefresh(TFSRepository, IResource[])}.
     *
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param resources
     *        the additionSet to refresh (must not be <code>null</code>)
     */
    private void refreshAsyncInternal(final TFSRepository repository, final IResource[] resources) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        final String label;
        if (resources.length == 1) {
            label =
                MessageFormat.format(
                    Messages.getString("ResourceDataManager.RefreshingServerItemInformationForFormat"), //$NON-NLS-1$
                    resources[0].getName());
        } else {
            label = MessageFormat.format(
                Messages.getString("ResourceDataManager.RefreshingServerItemInformationForCountFormat"), //$NON-NLS-1$
                resources.length);
        }

        final Job updateJob = new Job(label) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                refreshInternal(repository, resources);
                return Status.OK_STATUS;
            }
        };

        updateJob.schedule();
    }

    /**
     * Queries item information (via {@link VersionControlClient#getItems()})
     * recursively for the given resources, and updates all the resources given
     * that match returned server items.
     * <p>
     * Resources which have resource data already, but do not appear in the
     * server's returned items, are not updated (their data is not deleted).
     *
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param resources
     *        the resources to refresh (must not be <code>null</code>)
     */
    private void refreshInternal(final TFSRepository repository, final IResource[] resources) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        /*
         * Build item specs that include all the projects.
         */
        final List<ItemSpec> itemSpecList = new ArrayList<ItemSpec>();
        for (int resourceIndex = 0; resourceIndex < resources.length; resourceIndex++) {
            Check.notNull(resources[resourceIndex], "resources[resourceIndex]"); //$NON-NLS-1$

            /* May get called for linked resources, ignore them. */
            if (resources[resourceIndex].getLocation() == null) {
                continue;
            }

            final RecursionType recursionType =
                (resources[resourceIndex].getType() == IResource.FILE) ? RecursionType.NONE : RecursionType.FULL;

            itemSpecList.add(new ItemSpec(resources[resourceIndex].getLocation().toOSString(), recursionType));
        }

        final ItemSpec[] itemSpecs = itemSpecList.toArray(new ItemSpec[itemSpecList.size()]);
        final List<ResourceDataUpdate> updates;

        if (repository.getWorkspace().getLocation().equals(WorkspaceLocation.SERVER)) {
            updates = getServerWorkspaceUpdates(repository, itemSpecs);
        } else {
            updates = getLocalWorkspaceUpdates(repository, itemSpecs);
        }

        if (updates.size() > 0) {
            update(updates.toArray(new ResourceDataUpdate[updates.size()]));
        }
    }

    private List<ResourceDataUpdate> getServerWorkspaceUpdates(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs) {
        final List<ResourceDataUpdate> updates = new ArrayList<ResourceDataUpdate>();

        if (itemSpecs.length == 1) {
            log.info(MessageFormat.format("Refreshing server item information for {0}", itemSpecs[0].getItem())); //$NON-NLS-1$
        } else {
            log.info(MessageFormat.format("Refreshing server item information for {0} resources", itemSpecs.length)); //$NON-NLS-1$
        }

        final ItemSet[] itemSets = repository.getVersionControlClient().getItems(
            itemSpecs,
            new WorkspaceVersionSpec(repository.getWorkspace()),
            DeletedState.NON_DELETED,
            ItemType.ANY,
            GetItemsOptions.UNSORTED);

        for (int setIndex = 0; setIndex < itemSets.length; setIndex++) {
            final Item[] items = itemSets[setIndex].getItems();

            for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
                final String serverPath = items[itemIndex].getServerItem();
                String localPath;

                try {
                    localPath = repository.getWorkspace().getMappedLocalPath(serverPath);
                } catch (final ServerPathFormatException e) {
                    log.warn(MessageFormat.format("Could not compute local path from mapped path {0}", serverPath), e); //$NON-NLS-1$
                    continue;
                }

                if (localPath != null) {
                    final ResourceDataUpdate update = new LocalItemChangeData(
                        localPath,
                        items[itemIndex].getItemType(),
                        items[itemIndex].getServerItem(),
                        items[itemIndex].getChangeSetID()).createResourceDataUpdate();

                    if (update != null) {
                        updates.add(update);
                    }
                } else {
                    log.warn(
                        MessageFormat.format(
                            "The server path {0} is not mapped to any local path, cannot refresh!", //$NON-NLS-1$
                            serverPath));
                }
            }
        }

        return updates;
    }

    private List<ResourceDataUpdate> getLocalWorkspaceUpdates(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs) {
        final List<ResourceDataUpdate> updates = new ArrayList<ResourceDataUpdate>();

        if (itemSpecs.length == 1) {
            log.info(
                MessageFormat.format("Refreshing local workspace item information for {0}", itemSpecs[0].getItem())); //$NON-NLS-1$
        } else {
            log.info(MessageFormat.format(
                "Refreshing local workspace item information for {0} resources", //$NON-NLS-1$
                itemSpecs.length));
        }

        ExtendedItem[][] items = null;

        for (int i = 0; i < LOCAL_REFRESH_TRIES; i++) {
            try {
                items = repository.getVersionControlClient().getExtendedItems(
                    itemSpecs,
                    DeletedState.NON_DELETED,
                    ItemType.ANY,
                    GetItemsOptions.LOCAL_ONLY);
                break;
            } catch (final LocalMetadataTableLock.LocalMetadataTableTimeoutException e) {
                log.info("Timeout reading local workspace server item information for ResourceDataManager, retrying"); //$NON-NLS-1$
                log.debug("ResourceDataManager timeout exception details", e); //$NON-NLS-1$
            }
        }

        /*
         * If we timed out on all tries, we'll have no updates.
         */
        if (items != null) {
            for (final ExtendedItem[] itemSubArray : items) {
                for (final ExtendedItem item : itemSubArray) {
                    final ResourceDataUpdate update = new LocalItemChangeData(
                        item.getLocalItem(),
                        item.getItemType(),
                        item.getTargetServerItem(),
                        item.getLocalVersion()).createResourceDataUpdate();

                    if (update != null) {
                        updates.add(update);
                    }
                }
            }
        }

        return updates;
    }

    /**
     * Saves the given {@link ResourceDataUpdate}s. Starts a background
     * {@link Job} to do the work so the workspace resources can be safely
     * updated in their own transaction. Returns quickly.
     *
     * @param updates
     *        the updates to save (must not be <code>null</code>)
     */
    public void update(final ResourceDataUpdate[] updates) {
        if (updates.length == 0) {
            log.debug("0 updates given, not launching apply job"); //$NON-NLS-1$
            return;
        }

        /*
         * This work runs as a job because updating sync data can only happen
         * when the resource tree is unlocked, and this method is often called
         * while the tree is locked.
         *
         * Because these jobs are so disk intensive (reading local file path
         * information and saving synchronize data in the workspace), they
         * should be run serially.
         */
        final ApplyQueuedUpdatesJob job = new ApplyQueuedUpdatesJob(updates, this);
        job.setRule(ResourcesPlugin.getWorkspace().getRoot());
        job.schedule();
    }

    public void addListener(final ResourceDataListener listener) {
        resourceDataListeners.addListener(listener);
    }

    public void removeListener(final ResourceDataListener listener) {
        resourceDataListeners.removeListener(listener);
    }

    void fireResourceDataChanged(final IResource[] changedResources) {
        ((ResourceDataListener) resourceDataListeners.getListener()).resourceDataChanged(changedResources);
    }
}
