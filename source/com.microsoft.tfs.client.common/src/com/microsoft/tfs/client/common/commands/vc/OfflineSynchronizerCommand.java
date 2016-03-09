// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChange;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizer;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerFilter;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerProvider;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class OfflineSynchronizerCommand extends TFSCommand {
    private final TFSRepository repository;
    private final OfflineSynchronizerProvider resourceProvider;
    private final OfflineSynchronizerFilter resourceFilter;

    private OfflineChange[] changes;

    public OfflineSynchronizerCommand(
        final TFSRepository repository,
        final OfflineSynchronizerProvider resourceProvider) {
        this(repository, resourceProvider, null);
    }

    public OfflineSynchronizerCommand(
        final TFSRepository repository,
        final OfflineSynchronizerProvider resourceProvider,
        final OfflineSynchronizerFilter resourceFilter) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resourceProvider, "resourceProvider"); //$NON-NLS-1$

        this.repository = repository;
        this.resourceProvider = resourceProvider;
        this.resourceFilter = resourceFilter;

        setCancellable(true);
    }

    @Override
    public String getName() {
        return (Messages.getString("OfflineSynchronizerCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("OfflineSynchronizerCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("OfflineSynchronizerCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    public OfflineChange[] getChanges() {
        return changes;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final OfflineSynchronizer synchronizer =
            new OfflineSynchronizer(repository.getWorkspace(), resourceProvider, resourceFilter);

        // default to find adds and deletes on the filesystem
        synchronizer.setDetectAdded(true);
        synchronizer.setDetectDeleted(true);
        synchronizer.setRecursionType(RecursionType.FULL);

        // determine changes
        synchronizer.detectChanges();

        changes = synchronizer.getChanges();

        return Status.OK_STATUS;
    }
}