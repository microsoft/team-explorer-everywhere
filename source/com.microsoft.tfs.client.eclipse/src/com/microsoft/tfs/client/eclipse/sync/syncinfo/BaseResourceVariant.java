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

import com.microsoft.tfs.client.common.commands.vc.DownloadPendingChangeBaselineToTempLocationCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeTempFileStorage;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class BaseResourceVariant implements IResourceVariant {
    private final TFSRepository repository;
    private final IResource resource;
    private final PendingChange localChange;
    private final GetOperation remoteOperation;

    public BaseResourceVariant(
        final TFSRepository repository,
        final IResource resource,
        final PendingChange localChange,
        final Item remoteItem,
        final GetOperation remoteOperation) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        this.repository = repository;
        this.resource = resource;
        this.localChange = localChange;
        this.remoteOperation = remoteOperation;
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
        if (localChange != null) {
            return String.valueOf(localChange.getVersion());
        } else if (remoteOperation != null) {
            return String.valueOf(remoteOperation.getVersionLocal());
        }

        return Messages.getString("BaseResourceVariant.ContentIdentifierBase"); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return resource.getName();
    }

    @Override
    public IStorage getStorage(final IProgressMonitor monitor) throws TeamException {
        /*
         * No pending change means the local copy is current with the base and
         * the local file is current.
         */
        if (localChange == null) {
            return new SynchronizeTempFileStorage(resource.getLocation().toOSString());
        }

        /* Ensure we're still online */
        if (TFSEclipseClientPlugin.getDefault().getProjectManager().getProjectStatus(
            resource.getProject()) != ProjectRepositoryStatus.ONLINE) {
            throw new TeamException(
                new Status(
                    IStatus.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("BaseResourceVariant.NotCurrentlyConnectedReturnOnlineToViewSyncInfo"), //$NON-NLS-1$
                    null));
        }

        final ChangeType changes = localChange.getChangeType();

        // if the pending change is an add, there is no base
        if (changes.contains(ChangeType.ADD)) {
            return null;
        }

        final DownloadPendingChangeBaselineToTempLocationCommand command =
            new DownloadPendingChangeBaselineToTempLocationCommand(repository, localChange);

        IStatus status;
        try {
            status = command.run(monitor);
        } catch (final Exception e) {
            throw new TeamException(
                MessageFormat.format(
                    Messages.getString("BaseResourceVariant.CouldNotGetStorageForBaseVersionOfResourceFormat"), //$NON-NLS-1$
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
                    Messages.getString("BaseResourceVariant.CouldNotGetStorageForBaseVersionOfResourceFormat"), //$NON-NLS-1$
                    resource.getName().toString()),
                (status != null) ? status.getException() : null);
        }

        return new SynchronizeTempFileStorage(command.getTempFile());
    }

    @Override
    public boolean isContainer() {
        if (resource.getType() == IResource.FILE) {
            return false;
        }

        return true;
    }
}
