// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class PreviewGetCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final GetRequest[] getRequests;
    private final GetOptions getOptions;

    private GetOperation[][] operations;

    public PreviewGetCommand(
        final TFSRepository repository,
        final GetRequest[] getRequests,
        final GetOptions getOptions) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(getRequests, "getRequests"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        this.repository = repository;
        this.getRequests = getRequests;
        this.getOptions = getOptions;

        setCancellable(true);

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("PreviewGetCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("PreviewGetCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("PreviewGetCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        operations = repository.getWorkspace().previewGetItems(getRequests, getOptions.combine(GetOptions.PREVIEW));

        return Status.OK_STATUS;
    }

    public GetOperation[][] getOperations() {
        return operations;
    }
}
