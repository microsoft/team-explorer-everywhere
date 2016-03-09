// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.GetCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.util.Check;

public abstract class AbstractGetTask extends BaseTask {
    private final TFSRepository repository;

    public AbstractGetTask(final Shell shell, final TFSRepository repository) {
        super(shell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
    }

    protected TFSRepository getRepository() {
        return repository;
    }

    protected abstract boolean init();

    protected abstract GetRequest[] getGetRequests();

    protected abstract GetOptions getGetOptions();

    protected void finish(final GetStatus getStatus) {
    }

    @Override
    public final IStatus run() {
        if (!init()) {
            return Status.OK_STATUS;
        }

        final GetRequest[] getRequests = getGetRequests();
        GetOptions getOptions = getGetOptions();

        Check.notNull(getRequests, "getRequests"); //$NON-NLS-1$
        Check.notNull(getOptions, "getOptions"); //$NON-NLS-1$

        /* Mix-in the "no auto resolve" preference, if set. */
        if (TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
            UIPreferenceConstants.AUTO_RESOLVE_CONFLICTS) == false) {
            getOptions = getOptions.combine(GetOptions.NO_AUTO_RESOLVE);
        }

        final GetCommand getCommand = new GetCommand(repository, getRequests, getOptions);

        /*
         * Use a command executor that does not raise error dialogs so that we
         * don't throw an error dialog on conflicts.
         */
        final ICommandExecutor commandExecutor = getCommandExecutor();
        commandExecutor.setCommandFinishedCallback(UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final IStatus getStatus = commandExecutor.execute(new ResourceChangingCommand(getCommand));

        if (!getStatus.isOK() && getCommand.hasConflicts()) {
            final ConflictDescription[] conflicts = getCommand.getConflictDescriptions();

            final ConflictResolutionTask conflictTask = new ConflictResolutionTask(getShell(), repository, conflicts);
            final IStatus conflictStatus = conflictTask.run();

            if (conflictStatus.isOK()) {
                return Status.OK_STATUS;
            }

            return getStatus;
        } else if (!getStatus.isOK()) {
            ErrorDialog.openError(getShell(), Messages.getString("GetTask.ErrorDialogTitle"), null, getStatus); //$NON-NLS-1$

            return getStatus;
        }

        final GetStatus status = getCommand.getGetStatus();

        finish(status);

        return Status.OK_STATUS;
    }
}
