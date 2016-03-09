// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;

/**
 * <p>
 * An {@link ICommandFinishedCallback} is used to perform some processing after
 * an {@link ICommand} has finished being executed by an
 * {@link ICommandExecutor}.
 * </p>
 *
 * <p>
 * For example, an {@link ICommandFinishedCallback} could log the status
 * produced by running a command or show it in an error dialog.
 * </p>
 */
public interface ICommandFinishedCallback {
    /**
     * Called after a command has finished being executed, and an
     * {@link IStatus} has been produced. The {@link IStatus} given here is
     * either returned by
     * {@link ICommand#run(org.eclipse.core.runtime.IProgressMonitor)} or is
     * produced by converting an exception thrown by
     * {@link ICommand#run(org.eclipse.core.runtime.IProgressMonitor)} into an
     * {@link IStatus} by using an {@link ICommandExceptionHandler}.
     *
     * @param command
     *        the {@link ICommand} that has finished executing
     * @param status
     *        the {@link IStatus} produced by executing the command
     */
    public void onCommandFinished(ICommand command, IStatus status);
}
