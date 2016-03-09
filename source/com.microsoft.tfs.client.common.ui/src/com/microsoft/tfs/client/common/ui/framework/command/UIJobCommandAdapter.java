// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.client.common.framework.command.ICommandStartedCallback;
import com.microsoft.tfs.client.common.framework.command.JobCommandAdapter;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the {@link Job} class.
 * It is expected to be used with UI-aware CommandFinishedCallbacks, that are
 * capable of raising error dialogs, etc. Thus the status returned is *ALWAYS*
 * {@link IStatus#OK}. This prevents Eclipse from raising another error dialog
 * erroneously.
 * </p>
 *
 * @see JobCommandAdapter
 */
public class UIJobCommandAdapter extends JobCommandAdapter {
    private final Object statusLock = new Object();
    private IStatus status;

    /**
     * Creates a new {@link JobCommandAdapter}, adapting the given
     * {@link ICommand} to the {@link Job} class.
     *
     * @param command
     *        the {@link ICommand} to adapt (must not be <code>null</code>)
     * @param startedCallback
     *        an optional {@link ICommandStartedCallback} to call back to before
     *        the command has started (may be <code>null</code>)
     * @param finishedCallback
     *        an optional {@link ICommandFinishedCallback} to call back to when
     *        the command has finished (may be <code>null</code>)
     */
    public UIJobCommandAdapter(
        final ICommand command,
        final ICommandStartedCallback startedCallback,
        final ICommandFinishedCallback finishedCallback) {
        super(command, startedCallback, finishedCallback);
    }

    /**
     * Note: to prevent Eclipse from raising spurious failure dialogs, the
     * status return is *ALWAYS* {@link IStatus#OK}. To get the status of the
     * command execution, call {@link #getCommandStatus()}.
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        final IStatus status = super.run(monitor);

        synchronized (statusLock) {
            this.status = status;
        }

        return Status.OK_STATUS;
    }

    public IStatus getCommandStatus() {
        /*
         * Synchronized for visibility - the thread calling run() will be a
         * background thread.
         */
        synchronized (statusLock) {
            return status;
        }
    }
}
