// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.LocaleUtil;

public class RestoreItemsCommand extends Command {
    private final ChangeItem[] changes;
    private final Workspace workspace;

    public RestoreItemsCommand(final Workspace workspace, final ChangeItem[] changes) {
        setCancellable(true);
        this.workspace = workspace;
        this.changes = changes;
    }

    @Override
    public String getName() {
        return Messages.getString("RestoreItemsCommand.RestoreCommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("RestoreItemsCommand.RestoreCommandErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("RestoreItemsCommand.RestoreCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask("", changes.length); //$NON-NLS-1$

        try {
            for (final ChangeItem change : changes) {
                if (progressMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                progressMonitor.subTask(change.getPendingChange().getLocalItem());

                final PendingChange pendingChange = change.getPendingChange();
                pendingChange.restoreCandidateDelete(workspace);

                progressMonitor.worked(1);
            }
        } finally {
            progressMonitor.done();
        }

        return Status.OK_STATUS;
    }
}