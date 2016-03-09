// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync.syncinfo;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.sync.LocalResourceData;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeComparator;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class SynchronizeInfo extends SyncInfo {
    private final TFSRepository repository;
    private final IResource local;
    private final PendingChange localChanges;
    private final Item remoteItem;
    private final GetOperation remoteOperation;

    public SynchronizeInfo(
        final TFSRepository repository,
        final IResource local,
        final LocalResourceData localData,
        final GetOperation remoteOperation,
        final SynchronizeComparator comparator) {
        super(
            local,
            createBaseVariant(repository, local, localData, remoteOperation),
            createLatestVariant(repository, local, localData, remoteOperation),
            comparator);

        final PendingChange pendingChange = (localData != null) ? localData.getPendingChange() : null;
        final Item remoteItem = (localData != null) ? localData.getItem() : null;

        this.repository = repository;
        this.local = local;
        localChanges = pendingChange;
        this.remoteItem = remoteItem;
        this.remoteOperation = remoteOperation;
    }

    private static IResourceVariant createBaseVariant(
        final TFSRepository repository,
        final IResource local,
        final LocalResourceData localData,
        final GetOperation operation) {
        final PendingChange pendingChange = (localData != null) ? localData.getPendingChange() : null;
        final Item remoteItem = (localData != null) ? localData.getItem() : null;

        return new BaseResourceVariant(repository, local, pendingChange, remoteItem, operation);
    }

    private static IResourceVariant createLatestVariant(
        final TFSRepository repository,
        final IResource local,
        final LocalResourceData localData,
        final GetOperation operation) {
        Check.notNull(local, "local"); //$NON-NLS-1$

        final PendingChange pendingChange = (localData != null) ? localData.getPendingChange() : null;
        final Item remoteItem = (localData != null) ? localData.getItem() : null;

        return new LatestResourceVariant(repository, local, pendingChange, remoteItem, operation);
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public PendingChange getLocalChanges() {
        return localChanges;
    }

    public Item getRemoteItem() {
        return remoteItem;
    }

    public GetOperation getRemoteOperation() {
        return remoteOperation;
    }

    /**
     * Method that is invoked from the <code>init()</code> method to calculate
     * the sync kind for this instance of <code>SyncInfo</code>. The result is
     * assigned to an instance variable and is available using
     * <code>getKind()</code>. We override this because Subclasses should not
     * invoke this method but may override it in order to customize the sync
     * kind calculation algorithm.
     *
     * @return the sync kind of this <code>SyncInfo</code>
     * @throws TeamException
     *         if there were problems calculating the sync state.
     */
    @Override
    protected int calculateKind() throws TeamException {
        int description = IN_SYNC;

        final boolean localExists = local.exists();
        final String localPath = LocalPath.canonicalize(local.getLocation().toOSString());

        // no pending changes, no get op means we're synced
        if (localChanges == null && remoteOperation == null) {
            description = IN_SYNC;
        }

        // local has changes but remote is synced with base
        else if (localChanges != null && remoteOperation == null) {
            description = OUTGOING;
        }

        // local is unchanged, remote has changed
        else if (localChanges == null && remoteOperation != null) {
            description = INCOMING;
        }

        // both changed
        else {
            description = CONFLICTING;
        }

        if (localChanges != null) {
            final ChangeType changeTypes = localChanges.getChangeType();

            // add/branch/merge/undelete create files on local side (and are
            // thus additions)
            if (changeTypes.contains(ChangeType.ADD)
                || changeTypes.contains(ChangeType.BRANCH)
                || changeTypes.contains(ChangeType.MERGE)
                || changeTypes.contains(ChangeType.UNDELETE)) {
                description |= ADDITION;
            }
            // deletes
            else if (changeTypes.contains(ChangeType.DELETE)) {
                description |= DELETION;

                /*
                 * In 2010, we do not get a get operation for items with a
                 * pending delete. Thus, we need to determine if we are deleting
                 * an item that is not at the latest version, this will cause a
                 * checkin conflict and should be decorated as such.
                 */
                if (remoteItem != null && localChanges.getVersion() < remoteItem.getChangeSetID()) {
                    description |= CONFLICTING;
                }
            }
            // renames could be adds or deletes for this resource
            else if (changeTypes.contains(ChangeType.RENAME)) {
                String source = null, dest = null;

                try {
                    if (localChanges.getSourceServerItem() != null) {
                        source = repository.getWorkspace().getMappedLocalPath(localChanges.getSourceServerItem());
                    }

                    if (localChanges.getServerItem() != null) {
                        dest = repository.getWorkspace().getMappedLocalPath(localChanges.getServerItem());
                    }
                } catch (final ServerPathFormatException e) {
                    // suppress
                }

                // a rename is a delete on the source and an add on the target
                if (source != null && source.equalsIgnoreCase(localPath)) {
                    description |= DELETION;
                } else if (dest != null && dest.equalsIgnoreCase(localPath)) {
                    description |= ADDITION;
                } else {
                    description |= CHANGE;
                }
            } else {
                description |= CHANGE;
            }
        }

        // remote has changes
        if (remoteOperation != null) {
            // standard ol deletion
            if (remoteOperation.isDelete()) {
                description |= DELETION;

                // catch writable conflicts
                if (local.getType() == IWorkspaceRoot.FILE && !local.isReadOnly()) {
                    description |= CONFLICTING;
                }
            }
            /* local file exists but is not managed by us is a conflict. */
            else if (localExists
                && TFSEclipseClientPlugin.getDefault().getResourceDataManager().hasResourceData(local) == false) {
                description |= (CONFLICTING | ADDITION);
            }
            /* writable conflict */
            else if (!repository.getWorkspace().isLocalWorkspace()
                && localExists
                && local.getType() == IWorkspaceRoot.FILE
                && !local.isReadOnly()) {
                description |= (CONFLICTING | CHANGE);
            }
            /* local file doesn't exist means this is an add */
            else if (!localExists) {
                description |= ADDITION;
            }
            /*
             * incoming rename is specified by the remote operation having this
             * resource as a source and another for the target...
             */
            else if (remoteOperation.getCurrentLocalItem() != null
                && remoteOperation.getTargetLocalItem() != null
                && !remoteOperation.getCurrentLocalItem().equals(remoteOperation.getTargetLocalItem())) {
                description |= DELETION;

                /* catch writable conflicts */
                if (local.getType() == IWorkspaceRoot.FILE && !local.isReadOnly()) {
                    description |= CONFLICTING;
                }
            }
            /* otherwise, simply a change */
            else {
                description |= CHANGE;
            }
        }

        return description;
    }
}
