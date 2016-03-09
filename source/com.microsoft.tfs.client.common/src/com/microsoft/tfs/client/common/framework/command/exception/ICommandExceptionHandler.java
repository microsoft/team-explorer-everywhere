// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;

/**
 * <p>
 * An {@link ICommandExceptionHandler} is called in response to an exception
 * thrown by an {@link ICommand} in its
 * {@link ICommand#run(org.eclipse.core.runtime.IProgressMonitor)} method. The
 * exception handler has a chance to examine the exception and return an
 * {@link IStatus} for the exception.
 * </p>
 *
 * <p>
 * {@link ICommand}s can return an {@link ICommandExceptionHandler} from the
 * {@link ICommand#getExceptionHandler()} method.
 * </p>
 *
 * <p>
 * {@link ICommandExceptionHandler} are used primarily by
 * {@link ICommandExecutor}s to handle exceptions thrown during command
 * execution.
 * </p>
 *
 * <p>
 * Exception handlers can be composed. Generally, this is done to allow a chain
 * of exception handlers (arranged from more specific to more general) to all
 * examine an exception. In this case, the more general exception handlers in
 * would only convert the exception into a status if a more specific handler did
 * not do it first. Exception handlers can be composed by using the
 * {@link CommandExceptionHandlerFactory} class.
 * </p>
 *
 * @see ICommand
 * @see ICommandExceptionHandler
 * @see CommandExceptionHandlerFactory
 */
public interface ICommandExceptionHandler {
    /**
     * <p>
     * Handle an exception thrown by a command. This handler can return an
     * {@link IStatus} for the exception, or return <code>null</code> to
     * indicate that this handler can not handle the exception.
     * </p>
     *
     * <p>
     * Since exception handlers are often chained, this method takes an
     * {@link IStatus} parameter. This parameter will be non-<code>null</code>
     * if this handler is part of a chain, and one of the other handlers in the
     * chain has already run and returned a non-<code>null</code> status for the
     * exception. In this case, most handlers should simply return the status
     * without further processing (although this behavior is not required).
     * </p>
     *
     * @param t
     *        an exception thrown by an {@link ICommand}
     * @return an {@link IStatus} for the given exception, or <code>null</code>
     *         if this handler does not wish to handle the exception
     */
    public IStatus onException(Throwable t);
}
