// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class GetPendingChangesCommand extends TFSCommand {
    private final TFSRepository repository;
    private final ItemSpec[] itemSpecs;
    private final boolean generateDownloadUrls;
    private final String[] itemPropertyFilters;

    private PendingSet pendingSet;
    private PendingChange[] pendingChanges;

    public GetPendingChangesCommand(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = null;
        this.generateDownloadUrls = false;
        this.itemPropertyFilters = null;
    }

    public GetPendingChangesCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls) {
        this(repository, itemSpecs, generateDownloadUrls, null);
    }

    public GetPendingChangesCommand(
        final TFSRepository repository,
        final ItemSpec[] itemSpecs,
        final boolean generateDownloadUrls,
        final String[] itemPropertyFilters) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(itemSpecs, "itemSpecs"); //$NON-NLS-1$

        this.repository = repository;
        this.itemSpecs = itemSpecs;
        this.generateDownloadUrls = generateDownloadUrls;
        this.itemPropertyFilters = itemPropertyFilters;
    }

    @Override
    public String getName() {
        return (Messages.getString("GetPendingChangesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("GetPendingChangesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("GetPendingChangesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (itemSpecs != null) {
            pendingSet =
                repository.getWorkspace().getPendingChanges(itemSpecs, generateDownloadUrls, itemPropertyFilters);
        } else {
            pendingSet = repository.getWorkspace().getPendingChanges();
        }

        if (pendingSet != null) {
            pendingChanges = pendingSet.getPendingChanges();
        }

        return Status.OK_STATUS;
    }

    public PendingSet getPendingSet() {
        return pendingSet;
    }

    public PendingChange[] getPendingChanges() {
        return pendingChanges;
    }
}
