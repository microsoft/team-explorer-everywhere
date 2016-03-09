// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueue;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueueOptions;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class UpdateLocalVersionCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final ClientLocalVersionUpdate[] updates;
    private final UpdateLocalVersionQueueOptions options;

    public UpdateLocalVersionCommand(
        final TFSRepository repository,
        final ClientLocalVersionUpdate[] updates,
        final UpdateLocalVersionQueueOptions options) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(updates, "updates"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$

        this.repository = repository;
        this.updates = updates;
        this.options = options;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("UpdateLocalVersionCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("UpdateLocalVersionCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("UpdateLocalVersionCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(Messages.getString("UpdateLocalVersionCommand.ProgressText"), 3); //$NON-NLS-1$

        UpdateLocalVersionQueue queue = null;
        try {
            queue = new UpdateLocalVersionQueue(repository.getWorkspace(), options);

            for (final ClientLocalVersionUpdate update : updates) {
                queue.queueUpdate(update);
            }
            progressMonitor.worked(1);

            queue.flush();
            progressMonitor.worked(2);

            return Status.OK_STATUS;
        } finally {
            if (queue != null) {
                queue.close();
            }

            progressMonitor.done();
        }
    }
}