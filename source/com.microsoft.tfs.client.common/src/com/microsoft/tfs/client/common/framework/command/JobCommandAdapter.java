// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.framework.command.exception.CommandExceptionHandlerUtils;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the {@link Job} class.
 * </p>
 *
 * <p>
 * The {@link Job#run(IProgressMonitor)} method is implemented by directly
 * calling the {@link ICommand#run(IProgressMonitor)} method of the command
 * being wrapped by this adapter. Any exception thrown by the command will be
 * converted to an {@link IStatus} by calling
 * {@link CommandExceptionHandlerUtils#handleCommandException(ICommand, Throwable)}
 * . This wrapper can optionally take an {@link ICommandFinishedCallback} that
 * is called back after running the command and producing an {@link IStatus}.
 * After the job has finished, the status produced by the command run is
 * available by calling the {@link Job#getResult()} method.
 * </p>
 *
 * @see ICommand
 * @see Job
 * @see ICommandFinishedCallback
 */
public class JobCommandAdapter extends Job {
    private static final Log log = LogFactory.getLog(JobCommandAdapter.class);

    private final ICommand command;
    private final ICommandStartedCallback startedCallback;
    private final ICommandFinishedCallback finishedCallback;

    public JobCommandAdapter(final ICommand command) {
        this(command, null, null);
    }

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
    public JobCommandAdapter(
        final ICommand command,
        final ICommandStartedCallback startedCallback,
        final ICommandFinishedCallback finishedCallback) {
        super(command.getName());

        this.command = command;
        this.startedCallback = startedCallback;
        this.finishedCallback = finishedCallback;
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        IStatus status;

        if (startedCallback != null) {
            try {
                startedCallback.onCommandStarted(command);
            } catch (final Throwable t) {
                log.error("Command started callback failed", t); //$NON-NLS-1$
            }
        }

        try {
            status = command.run(monitor);
            if (status == null) {
                status = Status.OK_STATUS;
            }
        } catch (final Exception e) {
            status = CommandExceptionHandlerUtils.handleCommandException(command, e);
        }

        if (finishedCallback != null) {
            finishedCallback.onCommandFinished(command, status);
        }

        return status;
    }
}
