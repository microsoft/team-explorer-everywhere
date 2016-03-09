// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.OfflineSynchronizerCommand;
import com.microsoft.tfs.client.common.commands.vc.ReturnOnlinePendCommand;
import com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.DetectLocalChangesDialog;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineChange;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerFilter;
import com.microsoft.tfs.core.clients.versioncontrol.offline.OfflineSynchronizerProvider;
import com.microsoft.tfs.util.Check;

/**
 * Detects changes made to a server or local workspace while offline and
 * presents a dialog so the user can synchronize those changes with the server.
 * <p>
 * For server workspaces, {@link OfflineSynchronizerCommand} is used. For server
 * workspaces, a full scan is performed.
 *
 * @threadsafety unknown
 */
public class DetectLocalChangesTask extends BaseTask {
    private final TFSRepository repository;
    private final OfflineSynchronizerProvider provider;
    private final OfflineSynchronizerFilter filter;

    /**
     * Creates a {@link DetectLocalChangesTask}.
     *
     * @param shell
     *        the shell (must not be <code>null</code>)
     * @param repository
     *        the repository (must not be <code>null</code>)
     * @param itemSpecs
     *        the items to reconcile (must not be <code>null</code>)
     */
    public DetectLocalChangesTask(
        final Shell shell,
        final TFSRepository repository,
        final OfflineSynchronizerProvider provider,
        final OfflineSynchronizerFilter filter) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(provider, "provider"); //$NON-NLS-1$

        this.repository = repository;
        this.provider = provider;
        this.filter = filter;
    }

    @Override
    public IStatus run() {
        if (repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
            return getCommandExecutor().execute(new ScanLocalWorkspaceCommand(repository));
        } else {
            return scanServerWorkspace();
        }
    }

    private IStatus scanServerWorkspace() {
        final OfflineSynchronizerCommand synchronizerCommand =
            new OfflineSynchronizerCommand(repository, provider, filter);

        final IStatus synchronizerStatus = getCommandExecutor().execute(synchronizerCommand);

        if (!synchronizerStatus.isOK()) {
            return synchronizerStatus;
        }

        final OfflineChange[] changes = synchronizerCommand.getChanges();

        if (changes.length == 0) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("DetectLocalChangesTask.NoChangesDialogTitle"), //$NON-NLS-1$
                Messages.getString("DetectLocalChangesTask.NoChangesDialogText")); //$NON-NLS-1$

            return Status.OK_STATUS;
        }

        final DetectLocalChangesDialog onlineDialog = new DetectLocalChangesDialog(getShell(), changes);

        if (onlineDialog.open() != IDialogConstants.OK_ID) {
            return Status.CANCEL_STATUS;
        }

        final ReturnOnlinePendCommand pendCommand = new ReturnOnlinePendCommand(repository, onlineDialog.getChanges());
        return getCommandExecutor().execute(pendCommand);
    }
}
