// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UpdateBranchPropertiesCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final BranchProperties branchProperties;
    private boolean updateExisting = true;

    public UpdateBranchPropertiesCommand(final TFSRepository repository, final BranchProperties branchProperties) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(branchProperties, "branchProperties"); //$NON-NLS-1$

        this.repository = repository;
        this.branchProperties = branchProperties;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("UpdateBranchPropertiesCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("UpdateBranchPropertiesCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("UpdateBranchPropertiesCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    public UpdateBranchPropertiesCommand(
        final TFSRepository repository,
        final BranchProperties branchProperties,
        final boolean updateExisting) {
        this(repository, branchProperties);
        this.updateExisting = updateExisting;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        repository.getVersionControlClient().updateBranchObject(branchProperties, updateExisting);
        return Status.OK_STATUS;
    }
}