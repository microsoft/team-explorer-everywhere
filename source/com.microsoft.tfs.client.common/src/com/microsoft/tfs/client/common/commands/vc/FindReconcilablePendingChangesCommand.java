// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.commands.TFSConnectedCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.ReconcilePendingChangesStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

/**
 * Finds reconcilable pending changes after a gated checkin build has completed.
 */
public class FindReconcilablePendingChangesCommand extends TFSConnectedCommand {
    private final TFSRepository repository;
    private final PendingChange[] pendingChanges;
    private final Changeset changeset;

    private PendingChange[] reconcilablePendingChanges = null;
    private boolean matchedAtLeastOnePendingChange = false;

    public FindReconcilablePendingChangesCommand(
        final TFSRepository repository,
        final PendingChange[] pendingChanges,
        final Changeset changeset) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$

        this.repository = repository;
        this.pendingChanges = pendingChanges;
        this.changeset = changeset;

        setConnection(repository.getConnection());
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    @Override
    public String getName() {
        return Messages.getString("FindReconcilablePendingChangesCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("FindReconcilablePendingChangesCommand.ErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("FindReconcilablePendingChangesCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            final ReconcilePendingChangesStatus findStatus =
                repository.getWorkspace().findReconcilablePendingChangesForChangeset(changeset, pendingChanges);

            reconcilablePendingChanges = findStatus.getReconcilablePendingChanges();
            matchedAtLeastOnePendingChange = findStatus.matchedAtLeastOnePendingChange();

        } catch (final CanceledException e) {
            return Status.CANCEL_STATUS;
        }

        return Status.OK_STATUS;
    }

    public PendingChange[] getReconcilablePendingChanges() {
        return reconcilablePendingChanges;
    }

    public boolean matchedAtLeastOnePendingChange() {
        return matchedAtLeastOnePendingChange;
    }
}