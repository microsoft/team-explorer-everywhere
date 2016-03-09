// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.exception.CommandExceptionHandlerFactory;
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;

/**
 * <p>
 * An {@link ICommandExecutor} presents a service for running {@link ICommand}s.
 * Instead of calling
 * {@link ICommand#run(org.eclipse.core.runtime.IProgressMonitor)} directly,
 * client code can instead use an {@link ICommandExecutor}.
 * </p>
 *
 * <p>
 * The {@link CommandExecutorFactory} class provides easy access to commonly
 * used {@link ICommandExecutor} implementations. It is recommended in many
 * cases that client code go through the {@link CommandExecutorFactory} to
 * create new {@link ICommandExecutor}s.
 * </p>
 *
 * <p>
 * At a minimum, a command executor provides the following services:
 * <ul>
 * <li>Determining an {@link IProgressMonitor} to pass to
 * {@link ICommand#run(IProgressMonitor)}</li>
 * <li>Handling an exception thrown by {@link ICommand#run(IProgressMonitor)}
 * and converting the exception into an {@link IStatus}</li>
 * </ul>
 * Many other services and context is possible. For instance:
 * <ul>
 * <li>An executor may handle the returned {@link IStatus} from a command in
 * some way - for instance, by logging statuses that indicate errors or showing
 * an error dialog.</li>
 * <li>Providing multithreaded running of a command. For instance, the
 * {@link ProgressMonitorDialogCommandExecutor} runs a command in a background
 * thread while displaying a progress monitor on the UI thread.</li>
 * </ul>
 * </p>
 *
 * <p>
 * {@link ICommandExecutor} implementations are either <i>blocking</i> or
 * <i>non-blocking</i>. A blocking {@link ICommandExecutor} will not return from
 * an <code>execute()</code> method until the {@link ICommand} finishes. A
 * non-blocking executor may return from an <code>execute()</code> method while
 * the command is still running. For examples of non-blocking command executors,
 * see {@link JobCommandExecutor} and {@link ThreadCommandExecutor}.
 * </p>
 *
 * <p>
 * Non-blocking {@link ICommandExecutor}s have the following behavior:
 * <ul>
 * <li>They return <code>true</code> from {@link ICommandExecutor#isAsync()}
 * </li>
 * <li>The status returned from the <code>execute</code> methods is a
 * {@link FutureStatus}. This special status has different behavior depending on
 * whether or not the command has finished running, as well as some addition
 * behavior that a normal {@link IStatus} does not have. For more information
 * see {@link FutureStatus}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * An executor handles exceptions thrown by
 * {@link ICommand#run(IProgressMonitor)} by using an
 * {@link ICommandExceptionHandler} to convert the exception into an
 * {@link IStatus}. Executors will first attempt to use the
 * {@link ICommandExceptionHandler} returned by calling
 * {@link ICommand#getExceptionHandler()}. If this handler is <code>null</code>
 * or does not handle the thrown exception, the executor will then use the
 * exception handler returned by {@link CommandExceptionHandlerFactory#DEFAULT}.
 * </p>
 *
 * @see ICommand
 * @see ICommandExceptionHandler
 * @see FutureStatus
 * @see CommandExecutorFactory
 */
public interface ICommandExecutor {
    /**
     * <p>
     * Executes the given {@link ICommand} using any context supplied by this
     * {@link ICommandExecutor}.
     * </p>
     *
     * <p>
     * If this {@link ICommandExecutor} is non-blocking, this method may return
     * before the command finishes running. In this case, the
     * {@link ICommandExecutor#isAsync()} method of this class will return
     * <code>true</code>, and the {@link IStatus} returned by this method will
     * be a {@link FutureStatus}.
     * </p>
     *
     * @param command
     *        the {@link ICommand} to execute (must not be <code>null</code>)
     * @return an {@link IStatus} representing the outcome of the execution
     */
    public IStatus execute(ICommand command);

    /**
     * @return <code>true</code> if this {@link ICommandExecutor} is
     *         non-blocking as described above
     */
    public boolean isAsync();

    /**
     * Sets the command finished callback that will be called whenever a command
     * has finished.
     *
     * @param callback
     *        the command finished callback (never <code>null</code>)
     */
    public void setCommandFinishedCallback(ICommandFinishedCallback callback);

    /**
     * Gets the currently configured command finished callback handler.
     *
     * @return the currently configured command finished callback (never null)
     */
    public ICommandFinishedCallback getCommandFinishedCallback();
}
