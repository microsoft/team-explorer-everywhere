// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;

/**
 * <p>
 * An {@link ICommand} represents a task to be performed.
 * </p>
 *
 * <p>
 * Commands are implemented by directly implementing the {@link ICommand}
 * interface, or more often by subclassing a common base class, such as
 * {@link Command}.
 * </p>
 *
 * <p>
 * Note that commands are generally stateful, and are generally not designed to
 * be run by more than one thread at a time. A command implementation usually
 * takes its "input" in its constructor, and returns its "output" to clients
 * through public accessor methods not defined in this interface.
 * </p>
 *
 * <p>
 * Commands can be run simply by calling the <code>run()</code> method directly.
 * More often, clients use an {@link ICommandExecutor} to execute a command.
 * Using a command executor provides many advantages. See the documentation for
 * {@link ICommandExecutor} for more details.
 * </p>
 *
 * @see ICommandExecutor
 */
public interface ICommand {
    /**
     * <p>
     * The "do work" method of an {@link ICommand}.
     * </p>
     *
     * <p>
     * The <code>progressMonitor</code> argument, if not <code>null</code>,
     * should be used to report the progress of this command as it runs. Callers
     * of this method are <b>not</b> required to supply a progress monitor.
     * {@link ICommand} implementations <b>must not</b> expect the
     * <code>progressMonitor</code> argument to be non-<code>null</code>.
     * However, if the argument is non-<code>null</code>, the command should
     * ideally make use of the supplied progress monitor.
     * </p>
     *
     * <p>
     * This methods returns an {@link IStatus} to allow the command to report
     * its outcome to callers of this method. It is perfectly acceptable for a
     * command that successfully runs to return <code>null</code> as a status.
     * In that case, clients should interpret null to indicate that the command
     * had nothing interesting to report, and are allowed to treat a
     * <code>null</code> return value the same as {@link Status#OK_STATUS}.
     * </p>
     *
     * <p>
     * If a command returns early because it has been cancelled (either through
     * the supplied {@link IProgressMonitor} or through some other means) it
     * should indicate this cancellation in some way. Commonly, canceled
     * commands will return an {@link IStatus} with a severity of
     * {@link IStatus#CANCEL}. A command can also indicate cancellation by
     * throwing {@link OperationCanceledException} or by some other way that is
     * defined by the command implementation.
     * </p>
     *
     * <p>
     * A command may throw any exception from this method. Clients will have to
     * decide how such exceptions should be handled.
     * </p>
     *
     * @param progressMonitor
     *        an optional {@link IProgressMonitor} for the command to use
     * @return an {@link IStatus} as described above
     * @throws Exception
     */
    public IStatus run(IProgressMonitor progressMonitor) throws Exception;

    /**
     * <p>
     * Called to determine whether or not this {@link ICommand} is cancelable.
     * An {@link ICommand} is considered cancelable if it checks the
     * <code>isCanceled()</code> property of its {@link IProgressMonitor} when
     * run and returns early if <code>isCanceled()</code> returns
     * <code>true</code>.
     * </p>
     *
     * <p>
     * This method is often used in order to decide how to render a UI when
     * running an {@link ICommand}. For example, the UI may have a cancel button
     * that allows the user to stop the command before it finishes. The return
     * value from this method could be used to decide visibility or enablement
     * of such a cancel button.
     * </p>
     *
     * @return <code>true</code> if this {@link ICommand} is cancelable as
     *         described above
     */
    public boolean isCancellable();

    /**
     * Called to obtain an end user readable name for this {@link ICommand}. For
     * example, this name could be used in the UI when displaying the progress
     * of a running command.
     *
     * @return a descriptive name for this {@link ICommand} (must not be
     *         <code>null</code>)
     */
    public String getName();

    /**
     * Called to obtain an "error description" which is used if this command
     * fails. For example, "An error occurred while checking out files".
     *
     * @return a descriptive name for the error case of this {@link ICommand}
     *         (must not be <code>null</code>)
     */
    public String getErrorDescription();

    /**
     * Called to obtain a "logging description" which is logged to the product
     * log file when this command runs. This may be more informative than the
     * "name" (used for UI presentation). On null, no message should be logged.
     *
     * @return a descriptive piece of information for the log (or
     *         <code>null</code>)
     */
    public String getLoggingDescription();

    /**
     * <p>
     * Obtains an optional {@link ICommandExceptionHandler} for use in handling
     * exceptions thrown by this command.
     * </p>
     *
     * <p>
     * Typically, commands will supply an {@link ICommandExceptionHandler} to
     * have control over how specific types of exceptions that could be thrown
     * by the {@link #run(IProgressMonitor)} method should be handled. If the
     * command does not wish to supply an exception handler, it returns
     * <code>null</code> from this method. Higher-level code that is running a
     * command should generally respect a command's exception handler if it has
     * one and use it before a more general exception handler.
     * </p>
     *
     * @see ICommandExceptionHandler
     *
     * @return an {@link ICommandExceptionHandler} for this command or
     *         <code>null</code> if this command does not supply an exception
     *         handler
     */
    public ICommandExceptionHandler getExceptionHandler();
}
