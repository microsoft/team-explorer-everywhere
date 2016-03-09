// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;

/**
 * <p>
 * An abstract {@link ICommandExecutor} base class that is used to build command
 * executors that use an {@link IRunnableContext}.
 * </p>
 *
 * <p>
 * Subclasses need only provide an implementation of
 * {@link AbstractRunnableContextCommandExecutor#getRunnableContext(ICommand)}.
 * This method is called each time a command is executed and should return a
 * valid {@link IRunnableContext} to use when running the command.
 * </p>
 *
 * <p>
 * This class makes use of {@link RunnableWithProgressCommandAdapter} to wrap an
 * {@link ICommand} and make the command look like an
 * {@link IRunnableWithProgress}.
 * </p>
 *
 * @see ICommandExecutor
 * @see IRunnableContext
 * @see RunnableWithProgressCommandAdapter
 */
public abstract class AbstractRunnableContextCommandExecutor extends AbstractUICommandExecutor {
    private final static Log log = LogFactory.getLog(CommandExecutor.class);

    protected AbstractRunnableContextCommandExecutor(final Shell shell) {
        super(shell);
    }

    /**
     * Subclasses must implement this method and return an
     * {@link IRunnableContext} to run the given {@link ICommand} with. This
     * method is called every time a command is executed with this executor.
     *
     * @param command
     *        the {@link ICommand} about to be executed (never <code>null</code>
     *        )
     * @return an {@link IRunnableContext} to use (must not be <code>null</code>
     *         )
     */
    protected abstract IRunnableContext getRunnableContext(ICommand command);

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.command.CommandExecutor#execute
     * (com.microsoft.tfs.client.common.ui.shared.command.ICommand)
     */
    @Override
    public IStatus execute(final ICommand command) {
        final IRunnableContext runnableContext = getRunnableContext(command);

        final RunnableWithProgressCommandAdapter adapter = new RunnableWithProgressCommandAdapter(command);

        IStatus status = null;

        try {
            getCommandStartedCallback().onCommandStarted(command);
        } catch (final Throwable t) {
            log.error("Command started callback failed", t); //$NON-NLS-1$
        }

        try {
            runnableContext.run(true, command.isCancellable(), adapter);
            status = adapter.getStatus();
        } catch (Throwable t) {
            /*
             * IRunnableWithProgress should throw InvocationTargetExceptions
             * wrapping the proper exception. Unwrap.
             */
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getCause();
            }

            status = handleCommandException(command, t);
        }

        getCommandFinishedCallback().onCommandFinished(command, status);

        return status;
    }
}
