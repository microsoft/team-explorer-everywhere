// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.exception.CommandExceptionHandlerUtils;
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A base implementation of {@link ICommandExecutor}, suitable for use directly
 * or for subclassing to build more sophisticated executors.
 * </p>
 *
 * <p>
 * This executor executes commands on the thread that the <code>execute()</code>
 * method is called on and does not supply an {@link IProgressMonitor} to
 * commands (unless {@link #getDefaultProgressMonitor()} is overridden).
 * Subclasses may provide different behavior.
 * </p>
 *
 * <p>
 * In addition, this executor implements the basic exception handling mechansism
 * as described in the documentation for {@link ICommandExecutor}.
 * </p>
 *
 * <p>
 * For subclass implementors:<br/>
 * Subclasses will usually override {@link ICommandExecutor#execute(ICommand)}
 * and may optionally override {@link #getCommandFinishedCallback()}. In the
 * overridden {@link #execute(ICommand)} method, subclasses can call
 * {@link #handleCommandException(ICommand, Throwable)} to convert any exception
 * thrown by the command into an {@link IStatus}. Subclasses should also call
 * {@link #getCommandFinishedCallback()} to obtain a callback object to which
 * the status should be passed before returning.
 * </p>
 *
 * @see ICommandExecutor
 * @see ICommandExceptionHandler
 * @see ICommandFinishedCallback
 * @see ICommand
 */
public class CommandExecutor implements ICommandExecutor {
    private static final Log log = LogFactory.getLog(CommandExecutor.class);

    private final IProgressMonitor progressMonitor;

    private ICommandStartedCallback commandStartedCallback = CommandStartedCallbackFactory.getDefaultCallback();
    private ICommandFinishedCallback commandFinishedCallback = CommandFinishedCallbackFactory.getDefaultCallback();

    public CommandExecutor() {
        this(null);
    }

    public CommandExecutor(final IProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.command.ICommandExecutor#
     * isAsync ()
     */
    @Override
    public boolean isAsync() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.command.ICommandExecutor#
     * execute (com.microsoft.tfs.client.common.ui.shared.command.ICommand)
     */
    @Override
    public IStatus execute(final ICommand command) {
        IStatus status;

        try {
            getCommandStartedCallback().onCommandStarted(command);
        } catch (final Throwable t) {
            log.error("Command started callback failed", t); //$NON-NLS-1$
        }

        try {
            status = command.run(getDefaultProgressMonitor());
            if (status == null) {
                status = Status.OK_STATUS;
            }
        } catch (final Exception e) {
            status = handleCommandException(command, e);
        }

        getCommandFinishedCallback().onCommandFinished(command, status);

        return status;
    }

    public void setCommandStartedCallback(final ICommandStartedCallback callback) {
        Check.notNull(callback, "callback"); //$NON-NLS-1$

        commandStartedCallback = callback;
    }

    @Override
    public void setCommandFinishedCallback(final ICommandFinishedCallback callback) {
        Check.notNull(callback, "callback"); //$NON-NLS-1$

        commandFinishedCallback = callback;
    }

    /**
     * Called by the {@link #execute(ICommand)} method to get an
     * {@link ICommandStartedCallback}. Subclasses can override and return a
     * different {@link ICommandStartedCallback}, but should usually use
     * {@link MultiCommandStartedCallback} and compose their custom callback
     * with the callback returned by this base class.
     *
     * @return an {@link ICommandStartedCallback} as described above
     */
    public ICommandStartedCallback getCommandStartedCallback() {
        return commandStartedCallback;
    }

    /**
     * Called by the {@link #execute(ICommand)} method to get an
     * {@link ICommandFinishedCallback}. Subclasses can override and return a
     * different {@link ICommandFinishedCallback}, but should usually use
     * {@link MultiCommandFinishedCallback} and compose their custom callback
     * with the callback returned by this base class.
     *
     * @return an {@link ICommandFinishedCallback} as described above
     */
    @Override
    public ICommandFinishedCallback getCommandFinishedCallback() {
        return commandFinishedCallback;
    }

    /**
     * Called by the {@link #execute(ICommand)} method to get the progress
     * monitor to use for the command. The default implementation (in
     * {@link CommandExecutor}) returns <code>null</code>, but other classes may
     * override.
     *
     * @return the default {@link IProgressMonitor} to use during
     *         {@link #execute(ICommand)}
     */
    public IProgressMonitor getDefaultProgressMonitor() {
        return progressMonitor;
    }

    /**
     * Called by subclasses to convert an exception thrown by a command into an
     * {@link IStatus}.
     *
     * @param command
     *        the {@link ICommand} that threw the exception
     * @param t
     *        the thrown exception
     * @return an {@link IStatus} that represents the exception
     */
    protected final IStatus handleCommandException(final ICommand command, final Throwable t) {
        return CommandExceptionHandlerUtils.handleCommandException(command, t);
    }
}
