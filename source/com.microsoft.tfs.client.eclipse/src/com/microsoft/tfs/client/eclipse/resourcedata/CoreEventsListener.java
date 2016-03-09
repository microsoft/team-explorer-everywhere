// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.util.TeamUtils;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationStartedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.util.Check;

class CoreEventsListener
    implements OperationStartedListener, CheckinListener, GetListener, OperationCompletedListener,
    UndonePendingChangeListener, NewPendingChangeListener, ConflictResolvedListener, WorkspaceUpdatedListener {
    private static final Log log = LogFactory.getLog(CoreEventsListener.class);

    /**
     * {@link LocalItemChangeData}s are built during {@link #onGet(GetEvent)}
     * and {@link #onCheckin(CheckinEvent)}, and queued here for processing
     * during {@link #onOperationCompleted(OperationCompletedEvent)}.
     */
    private final List<LocalItemChangeData> queuedLocalItemChanges =
        Collections.synchronizedList(new ArrayList<LocalItemChangeData>());

    /**
     * The queued updates are sent to this manager for sync info updating after
     * they are refreshed in the workspace.
     */
    private final ResourceDataManager manager;

    protected CoreEventsListener(final ResourceDataManager manager) {
        Check.notNull(manager, "manager"); //$NON-NLS-1$

        this.manager = manager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConflictResolved(final ConflictResolvedEvent e) {
        /*
         * This method exists to handle just some of the conflict situations,
         * not all of them require action here.
         *
         * For an edit change, when a conflict is resolved with
         * "Discard server changes" (Resolution.ACCEPT_YOURS), the server
         * doesn't send us a get operation or a new pending change after
         * resolution. This method has to calculate the correct new base version
         * for the file after the merge. Same for "Accept merge"
         * (Resolution.ACCEPT_THEIRS) and "Accept yours, rename theirs"
         * (Resolution.ACCEPT_YOURS_RENAME_THEIRS).
         *
         * When choosing "Undo my local changes" (Resolution.ACCEPT_THEIRS), no
         * action is needed here because the server will send us an undo
         * operation and get operation (for new content if needed), and the new
         * versions will be handled by other methods in this listener.
         *
         * This method can also ignore Resolution.OVERWRITE_LOCAL (which can be
         * chosen after a writable file is in the way of a "get") because a
         * "get" operation will happen later to force the overwrite.
         */

        final Conflict conflict = e.getConflict();
        final Resolution resolution = conflict.getResolution();

        final String localItem = conflict.getTargetLocalItem();
        String serverItem = null;
        ItemType itemType = null;
        int version = 0;

        if (resolution == Resolution.ACCEPT_YOURS
            || resolution == Resolution.ACCEPT_MERGE
            || resolution == Resolution.ACCEPT_YOURS_RENAME_THEIRS) {
            serverItem = conflict.getYourServerItem();
            itemType = conflict.getYourItemType();
            version = conflict.getTheirVersion();
        }

        if (localItem != null && serverItem != null && itemType != null) {
            queuedLocalItemChanges.add(new LocalItemChangeData(localItem, itemType, serverItem, version));
        }

        /*
         * No "operation completed" event happens for conflict resolution, so
         * flush for each resolved item. This may cause perf problems when
         * resolving large numbers of conflicts.
         */
        processQueuedLocalItems();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewPendingChange(final PendingChangeEvent e) {
        final PendingChange change = e.getPendingChange();

        /*
         * New pending changes may come from an unshelve operation, or from a
         * user-initiated change.
         */
        if (change.getChangeType().contains(ChangeType.RENAME)) {
            /*
             * A rename needs two updates (one for the source, one for the
             * target). The old and new paths in the change object are
             * straightforward.
             */

            /*
             * Push the source update to the beginning of the list so that it is
             * handled before a target update for the same file. (A shelveset
             * may include a rename A -> B, then an update of file A again.)
             *
             * Note that source local item may be null when you're unshelving a
             * cyclical rename. (A -> B, B -> A.)
             */
            if (change.getSourceLocalItem() != null) {
                queuedLocalItemChanges.add(
                    0,
                    new LocalItemChangeData(
                        change.getSourceLocalItem(),
                        change.getItemType(),
                        change.getSourceServerItem(),
                        0));
            }

            queuedLocalItemChanges.add(
                new LocalItemChangeData(
                    change.getLocalItem(),
                    change.getItemType(),
                    change.getServerItem(),
                    change.getVersion()));
        } else if (change.getChangeType().contains(ChangeType.DELETE)) {
            queuedLocalItemChanges.add(
                new LocalItemChangeData(change.getLocalItem(), change.getItemType(), change.getServerItem(), 0));
        } else {
            /*
             * A new pending change, possibly from an unshelve operation.
             */

            queuedLocalItemChanges.add(
                new LocalItemChangeData(
                    change.getLocalItem(),
                    change.getItemType(),
                    change.getServerItem(),
                    change.getVersion()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onGet(final GetEvent e) {
        final GetOperation getOperation = e.getOperation();

        /*
         * Operations which are conflicts are not processed by core; there will
         * be no new version on the filesystem.
         */
        if (getOperation.hasConflict()) {
            return;
        }

        /*
         * Only update the version if the OperationStatus shows no error types.
         */
        final OperationStatus status = e.getStatus();
        if (status == OperationStatus.GETTING || status == OperationStatus.REPLACING) {
            final String localPath = getOperation.getTargetLocalItem() != null ? getOperation.getTargetLocalItem()
                : getOperation.getCurrentLocalItem();

            queuedLocalItemChanges.add(
                new LocalItemChangeData(
                    localPath,
                    getOperation.getItemType(),
                    getOperation.getTargetServerItem(),
                    getOperation.getVersionServer()));
        } else if (status == OperationStatus.DELETING) {
            final String localPath = getOperation.getTargetLocalItem() != null ? getOperation.getTargetLocalItem()
                : getOperation.getCurrentLocalItem();

            queuedLocalItemChanges.add(
                new LocalItemChangeData(localPath, getOperation.getItemType(), getOperation.getTargetServerItem(), 0));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUndonePendingChange(final PendingChangeEvent e) {
        final PendingChange undoneChange = e.getPendingChange();

        if (undoneChange.isLocalItemDelete()) {
            /*
             * The undo deletes the local item, so queue a delete. The local
             * item may be null in the case of some undo operations (like for an
             * undo of a delete of an unmapped item). In these cases, we can
             * ignore the change because no local resource should be affected.
             */
            if (undoneChange.getLocalItem() != null) {
                queuedLocalItemChanges.add(
                    new LocalItemChangeData(
                        undoneChange.getLocalItem(),
                        undoneChange.getItemType(),
                        undoneChange.getServerItem(),
                        0));
            }
        } else if (undoneChange.getChangeType().contains(ChangeType.RENAME)) {
            /*
             * A rename needs two updates (one for the source, one for the
             * target). It's important to note that "source" and "target" are
             * assigned from the GetOperation that was processed in this undo
             * event: that is, the source local item is the _new_ file name the
             * user chose, and the target local item is the old name the file
             * had before the user renamed it. The source item will get version
             * 0 for its version (delete the resource data), the target gets the
             * version from the pending change (the workspace version).
             */

            /*
             * Push the source resource data change to the beginning of the
             * queue. It's important that we handle source changes first,
             * because we may have a target change for the same file. (This can
             * happen when you're undoing circular renames, ie, file A -> file B
             * and file B -> file A. We don't want to delete resource data for
             * A, update it for B, then for the next operation, delete it for B
             * and update it for A - this would leave B (incorrectly) without
             * resource data.)
             */

            /*
             * Note: a file may be undoing a delete (of the file itself) and a
             * rename (of the parent), for a pending change of rename / delete.
             * This would cause source local item to be null, thus we also need
             * to examine local item.
             */

            if (undoneChange.getSourceLocalItem() != null) {
                queuedLocalItemChanges.add(
                    0,
                    new LocalItemChangeData(
                        undoneChange.getSourceLocalItem(),
                        undoneChange.getItemType(),
                        undoneChange.getSourceServerItem(),
                        0));
            }

            if (undoneChange.getLocalItem() != null) {
                queuedLocalItemChanges.add(
                    new LocalItemChangeData(
                        undoneChange.getLocalItem(),
                        undoneChange.getItemType(),
                        undoneChange.getServerItem(),
                        undoneChange.getVersion()));
            }
        } else {
            /*
             * The normal undo case (covers adds).
             */

            final String localPath =
                undoneChange.getLocalItem() != null ? undoneChange.getLocalItem() : undoneChange.getSourceLocalItem();

            queuedLocalItemChanges.add(
                new LocalItemChangeData(
                    localPath,
                    undoneChange.getItemType(),
                    undoneChange.getServerItem(),
                    undoneChange.getVersion()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCheckin(final CheckinEvent e) {
        /* Changeset 0 indicates that all pending changes were undone */
        if (e.getChangesetID() == 0) {
            return;
        }

        final PendingChange[] committedChanges = e.getCommittedChanges();
        final PendingChange[] undoneChanges = e.getUndoneChanges();

        /*
         * If we did an initial refresh on the projects for these changes,
         * return early. Check all changes (not just committed).
         */
        final List<PendingChange> allChanges = new ArrayList<PendingChange>();
        allChanges.addAll(Arrays.asList(committedChanges));
        allChanges.addAll(Arrays.asList(undoneChanges));

        doInitialProjectsRefresh(
            TFSEclipseClientPlugin.getDefault().getRepositoryManager().getRepository(e.getWorkspace()),
            allChanges.toArray(new PendingChange[allChanges.size()]));

        /*
         * Process the committed changes.
         */
        for (int i = 0; i < committedChanges.length; i++) {
            /*
             * Pending changes which are deletes may not processed here. Pending
             * changes that have a null local path will be ignored. However, we
             * may still have a pending change that has a non-null server path
             * that is a deletion.
             */

            if (committedChanges[i].getSourceLocalItem() != null
                && !ServerPath.equals(committedChanges[i].getSourceLocalItem(), committedChanges[i].getLocalItem())) {
                /*
                 * This is the source of a rename/move and the source resource
                 * data should go away. Note that we always remove resource data
                 * before adding new resource data - the source of a rename may
                 * also be implicated in another get operation (eg, rename A->B
                 * and B->A), thus order is important.
                 */
                queuedLocalItemChanges.add(
                    0,
                    new LocalItemChangeData(
                        committedChanges[i].getSourceLocalItem(),
                        committedChanges[i].getItemType(),
                        null,
                        0));
            }

            if (committedChanges[i].getLocalItem() != null) {
                final int csId =
                    (committedChanges[i].getChangeType().containsAny(ChangeType.DELETE)) ? 0 : e.getChangesetID();

                /*
                 * This is the target of a rename/move or new file and it should
                 * get the new version.
                 */
                queuedLocalItemChanges.add(
                    new LocalItemChangeData(
                        committedChanges[i].getLocalItem(),
                        committedChanges[i].getItemType(),
                        committedChanges[i].getServerItem(),
                        csId));
            }
        }

        processQueuedLocalItems();
    }

    /**
     * This method is kind of a special hack to solve the problem of doing a
     * full resource data update (for a project) after a "share" of a new
     * project. We can't do it in the share wizard, because "share" only pends
     * adds (they're not checked in yet, so no server data to get!).
     *
     * (A share of an existing project, which was already mapped, never gets a
     * check in, but we catch that case in
     * {@link ProjectRepositoryManager#addProject(IProject, TFSRepository)}.)
     *
     * The best we can do is call this method in response to core events and try
     * to catch the case as soon as possible. This method tests whether the
     * given items' projects need a refresh, and schedules an asynchronous
     * refresh if needed.
     *
     * @param respository
     *        the connection to the repository in which all the changes reside
     * @param changes
     *        the pending changes just checked in
     */
    private void doInitialProjectsRefresh(final TFSRepository repository, final PendingChange[] changes) {
        if (repository == null || changes == null || changes.length == 0) {
            return;
        }

        final Set<IProject> projectsNeedingRefresh = new HashSet<IProject>();

        for (int i = 0; i < changes.length; i++) {
            final PendingChange change = changes[i];
            final ResourceType resourceType =
                (change.getItemType() == ItemType.FILE) ? ResourceType.FILE : ResourceType.CONTAINER;

            /*
             * Note that we pass "false" for mustExist: we want to get resources
             * that were recently deleted (as part of the get operation that
             * queued this update), so these resources may not actually exist on
             * disk in the workspace location.
             */
            final IResource resource = Resources.getResourceForLocation(change.getLocalItem(), resourceType, false);

            /*
             * It's possible the path isn't in a managed project yet, or is in
             * some other state. Ignore it.
             */
            if (resource == null) {
                log.trace(
                    MessageFormat.format(
                        "Could not find a resource for initial project refresh (even inaccessible ones) for local path {0}, ignoring", //$NON-NLS-1$
                        change.getLocalItem()));
                continue;
            }

            /*
             * Add it to the set if it needs a refresh.
             */
            final IProject project = resource.getProject();

            if (manager.hasCompletedRefresh(project) == false) {
                projectsNeedingRefresh.add(project);
            }
        }

        if (projectsNeedingRefresh.size() > 0) {
            manager.refreshAsync(
                repository,
                projectsNeedingRefresh.toArray(new IProject[projectsNeedingRefresh.size()]));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOperationStarted(final OperationStartedEvent e) {
        /*
         * No good resource information comes from this event.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOperationCompleted(final OperationCompletedEvent event) {
        processQueuedLocalItems();
    }

    @Override
    public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
        final WorkspaceLocation newLocation = e.getWorkspace().getLocation();
        final WorkspaceLocation oldLocation = e.getOriginalLocation();

        // oldLocation will be null if the event came from IPC, don't consider
        // that a switch.
        if (oldLocation != null && newLocation != oldLocation) {
            final TFSRepository repository =
                TFSEclipseClientPlugin.getDefault().getRepositoryManager().getRepository(e.getWorkspace());

            if (repository != null) {
                final IProject[] projects =
                    TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectsForRepository(repository);

                if (newLocation == WorkspaceLocation.SERVER) {
                    /*
                     * When converting to server, refresh server item
                     * information for all projects managed by our repository
                     * provider. This isn't required when converting to local,
                     * because that information is fetched from the server and
                     * written to the local metadata tables as part of the
                     * conversion process.
                     */

                    if (projects.length > 0) {
                        manager.refreshAsync(repository, projects);
                    }
                } else if (newLocation == WorkspaceLocation.LOCAL) {
                    // Mark any new .tf/$tf folders that appeared as team
                    // private
                    for (final IProject project : projects) {
                        try {
                            TeamUtils.markBaselineFoldersTeamPrivate(project);
                        } catch (final CoreException ex) {
                            log.warn("Error during share marking baseline folders as team private members", ex); //$NON-NLS-1$
                        }
                    }
                }
            }
        }
    }

    private void processQueuedLocalItems() {
        /*
         * For each queued item, refresh the workspace resource. Then send a
         * ResourceDataUpdate for each item to the ResourceDataManager so it can
         * apply sync info for each of them.
         */
        LocalItemChangeData[] changes;

        synchronized (queuedLocalItemChanges) {
            if (queuedLocalItemChanges.size() == 0) {
                return;
            }

            changes = queuedLocalItemChanges.toArray(new LocalItemChangeData[queuedLocalItemChanges.size()]);
            queuedLocalItemChanges.clear();
        }

        /*
         * Create ResourceDataUpdates in the loop and collect them here.
         */
        final List<ResourceDataUpdate> resourceDataUpdates = new ArrayList<ResourceDataUpdate>();

        for (int i = 0; i < changes.length; i++) {
            final LocalItemChangeData localItemChange = changes[i];

            if (localItemChange == null) {
                continue;
            }

            final ResourceDataUpdate update = localItemChange.createResourceDataUpdate();

            if (update == null) {
                continue;
            }

            /*
             * Build a ResourceDataUpdate. Use a null ResourceData if the
             * version is now 0.
             */
            resourceDataUpdates.add(update);
        }

        /*
         * Send the updates we built to the manager so they can be saved. This
         * returns quickly.
         */
        if (resourceDataUpdates.size() > 0) {
            manager.update(resourceDataUpdates.toArray(new ResourceDataUpdate[resourceDataUpdates.size()]));
        }
    }
}