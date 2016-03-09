// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.ICommand;

/**
 * Static utility classes for working with {@link ICommandExceptionHandler}s.
 */
public class CommandExceptionHandlerUtils {
    /**
     * Handles an exception thrown by a command by implementing a standard
     * exception handling strategy. If the command supplies an exception handler
     * ({@link ICommand#getExceptionHandler()}) that handler is tried first. If
     * the command does not supply a handler that can handle the exception, the
     * exception handler returned by
     * {@link CommandExceptionHandlerFactory#DEFAULT} is used.
     *
     * @param command
     *        the {@link ICommand} that threw an exception
     * @param t
     *        the thrown exception
     * @return an {@link IStatus} for the exception, never <code>null</code>
     */
    public static IStatus handleCommandException(final ICommand command, final Throwable t) {
        ICommandExceptionHandler exceptionHandler;

        /* Add any command exception handlers specified by this command */
        final ICommandExceptionHandler commandExceptionHandler = command.getExceptionHandler();

        /* And the standard framework exception handlers */
        final ICommandExceptionHandler standardExceptionHandler =
            CommandExceptionHandlerFactory.getDefaultExceptionHandler(command);

        if (commandExceptionHandler != null) {
            exceptionHandler = new MultiCommandExceptionHandler(new ICommandExceptionHandler[] {
                commandExceptionHandler,
                standardExceptionHandler
            });
        } else {
            exceptionHandler = standardExceptionHandler;
        }

        return exceptionHandler.onException(t);
    }
}
