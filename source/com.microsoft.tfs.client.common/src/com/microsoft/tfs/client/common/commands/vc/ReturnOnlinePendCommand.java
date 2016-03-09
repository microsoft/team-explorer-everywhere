// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.commands.helpers.NonFatalErrorHelper;
import com.microsoft.tfs.client.common.framework.command.ExtendedStatus;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChange;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflinePender;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class ReturnOnlinePendCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final OfflineChange[] changes;

    public ReturnOnlinePendCommand(final TFSRepository repository, final OfflineChange[] changes) {
        super();

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(changes, "changes"); //$NON-NLS-1$

        this.repository = repository;
        this.changes = changes;

        setConnection(repository.getConnection());
    }

    @Override
    public String getName() {
        return (Messages.getString("ReturnOnlinePendCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("ReturnOnlinePendCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return (Messages.getString("ReturnOnlinePendCommand.CommandText", LocaleUtil.ROOT)); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final NonFatalErrorHelper nonFatalHelper = new NonFatalErrorHelper(repository.getWorkspace());

        int failures;

        try {
            final OfflinePender pender = new OfflinePender(repository.getWorkspace(), changes);
            failures = pender.pendChanges();
        } finally {
            nonFatalHelper.destroy();
        }

        if (failures > 0) {
            // determine how many changes we should have pended
            final int attemptCount = getPendCount();

            final String message =
                (failures == attemptCount) ? Messages.getString("ReturnOnlinePendCommand.AllPendsFailed") //$NON-NLS-1$
                    : Messages.getString("ReturnOnlinePendCommand.SomePendsFailed"); //$NON-NLS-1$
            final int severity = (failures == attemptCount) ? IStatus.ERROR : IStatus.WARNING;

            return new ExtendedStatus(
                nonFatalHelper.getNonFatalMultiStatus(severity, message),
                ExtendedStatus.SHOW | ExtendedStatus.LOG);
        }

        return Status.OK_STATUS;
    }

    private int getPendCount() {
        int changecount = 0;

        for (int i = 0; i < changes.length; i++) {
            changecount += changes[i].getChangeTypes().length;
        }

        return changecount;
    }
}
