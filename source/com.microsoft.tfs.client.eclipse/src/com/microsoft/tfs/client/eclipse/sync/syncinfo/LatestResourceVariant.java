// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.sync.syncinfo;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;

import com.microsoft.tfs.client.common.commands.vc.AbstractGetToTempLocationCommand;
import com.microsoft.tfs.client.common.commands.vc.GetDownloadURLToTempLocationCommand;
import com.microsoft.tfs.client.common.commands.vc.GetVersionedItemToTempLocationCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeTempFileStorage;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.util.Check;

public class LatestResourceVariant implements IResourceVariant {
    private final TFSRepository repository;
    private final IResource resource;
    private final PendingChange pendingChange;
    private final Item remoteItem;
    private final GetOperation operation;

    public LatestResourceVariant(
        final TFSRepository repository,
        final IResource resource,
        final PendingChange pendingChange,
        final Item remoteItem,
        final GetOperation operation) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        this.repository = repository;
        this.resource = resource;
        this.pendingChange = pendingChange;
        this.remoteItem = remoteItem;
        this.operation = operation;
    }

    public TFSRepository getRepository() {
        return repository;
    }

    @Override
    public byte[] asBytes() {
        return null;
    }

    @Override
    public String getContentIdentifier() {
        /* Changeset number is given in the get operation. */
        if (operation != null) {
            return String.valueOf(operation.getVersionServer());
        }

        /*
         * Local delete against a 2010 with a conflicting edit.
         */
        else if (remoteItem != null && remoteItem.getChangeSetID() > 0) {
            return String.valueOf(remoteItem.getChangeSetID());
        }

        /*
         * No get implies that we're at the latest version locally, use the
         * local version number.
         */
        else if (pendingChange != null && pendingChange.getVersion() > 0) {
            return String.valueOf(pendingChange.getVersion());
        }

        /* Remote resource does not exist. */
        return Messages.getString("LatestResourceVariant.ContentIdentifierLatest"); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return resource.getName();
    }

    /**
     * Returns true if the remote operation is "effectively" a delete on the
     * local resource. Ie, a remote rename or an actual delete.
     *
     * @return true if sync should treat this as a deletion.
     */
    private boolean isEffectiveDelete() {
        if (operation == null) {
            /* The remote item is a delete. */
            if (pendingChange != null
                && pendingChange.getChangeType().contains(ChangeType.DELETE)
                && remoteItem != null
                && remoteItem.getDeletionID() > 0) {
                return true;
            }

            return false;
        }

        if (operation.isDelete()) {
            return true;
        }

        /*
         * This is the source of a rename (2008), this acts like a deletion in
         * this view.
         */
        if (operation.getCurrentLocalItem() != null
            && operation.getTargetLocalItem() != null
            && !operation.getCurrentLocalItem().equals(operation.getTargetLocalItem())) {
            final String source = operation.getCurrentLocalItem();

            /* The source of the rename is the effective delete. */
            if (source.equals(resource.getLocation().toOSString())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public IStorage getStorage(final IProgressMonitor monitor) throws TeamException {
        String tempFilename;

        /* Ensure we're still online */
        if (TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectStatus(
            resource.getProject()) != ProjectRepositoryStatus.ONLINE) {
            throw new TeamException(
                new Status(
                    IStatus.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("LatestResourceVariant.NotCurrentlyConnectedReturnOnlineToViewSyncInfo"), //$NON-NLS-1$
                    null));
        }

        /*
         * The remote is a deletion, do not display the latest contents (empty.)
         */
        if (isEffectiveDelete()) {
            return null;
        }

        AbstractGetToTempLocationCommand getCommand;

        /*
         * If there's a conflict-free get operation, then the DURL in the get
         * operation is the latest version. (In a conflict, the DURL in a get
         * operation is the common ancestor's.)
         */
        if (operation != null && !operation.hasConflict()) {
            getCommand =
                new GetDownloadURLToTempLocationCommand(repository, operation.getDownloadURL(), resource.getName());
        }
        /*
         * Otherwise, if there's a get operation, get the latest version of this
         * file with this name. (Synchronize is inherently slot-mode.)
         */
        else if (operation != null) {
            final String serverPath =
                repository.getWorkspace().getMappedServerPath(resource.getLocation().toOSString());

            getCommand = new GetVersionedItemToTempLocationCommand(repository, serverPath, LatestVersionSpec.INSTANCE);
        }
        /*
         * We may have an Item identifying the latest resource. (On 2010
         * servers, pending deletes will suppress get operations even when the
         * latest version is newer.
         */
        else if (remoteItem != null && remoteItem.getDownloadURL() != null) {
            getCommand = new GetDownloadURLToTempLocationCommand(repository, remoteItem.getDownloadURL());
        }
        /*
         * Otherwise, there's no get operation, which means we were latest at
         * synchronize time.
         */
        else {
            final String serverPath =
                repository.getWorkspace().getMappedServerPath(resource.getLocation().toOSString());

            /*
             * If this is an outgoing file creation (add, branch, etc. - the
             * remote latest version is 0) then there is no remote copy.
             */
            if (pendingChange != null && pendingChange.getVersion() == 0) {
                return null;
            }

            /*
             * If this is the target of an outgoing rename, treat this as if
             * there is no remote copy.
             */
            if (pendingChange != null
                && pendingChange.isRename()
                && ServerPath.equals(serverPath, pendingChange.getServerItem())) {
                return null;
            }

            /*
             * Otherwise, simply get the latest version of the file at this
             * name.
             */

            getCommand = new GetVersionedItemToTempLocationCommand(repository, serverPath, LatestVersionSpec.INSTANCE);
        }

        IStatus status;
        try {
            status = getCommand.run(monitor);
        } catch (final Exception e) {
            throw new TeamException(
                MessageFormat.format(
                    Messages.getString("LatestResourceVariant.CouldNotGetStorageForLatestVersionOfResourceFormat"), //$NON-NLS-1$
                    resource.getName().toString()),
                e);
        }

        /*
         * The synchronization framework may cancel the monitor while the
         * command is running, which causes a cancel status, or maybe the cancel
         * happened after the command was finished. Either way, the correct way
         * to signal that the storage could not be fetched for cancel is to
         * throw.
         */
        if (monitor.isCanceled() || (status != null && status.getSeverity() == IStatus.CANCEL)) {
            throw new OperationCanceledException();
        }

        if (status == null || !status.isOK()) {
            throw new TeamException(
                MessageFormat.format(
                    Messages.getString("LatestResourceVariant.CouldNotGetStorageForLatestVersionOfResourceFormat"), //$NON-NLS-1$
                    resource.getName().toString()),
                (status != null) ? status.getException() : null);
        }

        tempFilename = getCommand.getTempLocation();

        return new SynchronizeTempFileStorage(tempFilename);
    }

    @Override
    public boolean isContainer() {
        if (resource.getType() == IResource.FILE) {
            return false;
        }

        return true;
    }

    public GetOperation getOperation() {
        return operation;
    }
}
