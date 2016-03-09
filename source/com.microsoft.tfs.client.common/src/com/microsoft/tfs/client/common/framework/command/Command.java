// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.client.common.framework.command.exception.MultiCommandExceptionHandler;
import com.microsoft.tfs.client.common.framework.command.exception.NullCommandExceptionHandler;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A convenience abstract implementation of the <code>ICommand</code> interface.
 * Clients subclass this class and need provide only an implementation of the
 * abstract {@link #doRun(IProgressMonitor)} method.
 * </p>
 *
 * <p>
 * This implementation contains private fields to store the name and cancelable
 * state of this command. By default, subclasses are not cancelable and the
 * command name is the short name of the subclass. Subclasses can override these
 * defaults by calling {@link #setCancelable(boolean)} and
 * {@link #setName(String)}. In addition, this implementation guarantees that
 * the {@link IProgressMonitor} instance passed to subclasses in the
 * {@link #doRun(IProgressMonitor)} method is non-<code>null</code> and that
 * {@link IProgressMonitor#done()} will always be called on that instance.
 * </p>
 *
 * <p>
 * This base class does not provide an {@link ICommandExceptionHandler}.
 * Subclasses may override the {@link #getExceptionHandler()} method and return
 * an exception handler if needed.
 * </p>
 *
 * @see ICommand
 */
public abstract class Command extends CancellableCommand implements ICommand {
    private static final Log log = LogFactory.getLog(Command.class);

    private final List<ICommandExceptionHandler> exceptionHandlerList = new ArrayList<ICommandExceptionHandler>();

    /**
     * Subclasses may add initialization / completion runnables. For example,
     * another base command class may perform the same routines at all
     * initialization. This allows the base class to do this (without
     * implementing doRun() and requiring concrete classes to override yet
     * another run method.)
     */
    private final List<CommandInitializationRunnable> initializationRunnables =
        new ArrayList<CommandInitializationRunnable>();

    /**
     * Subclasses must override this method to perform the actual work of this
     * command.
     *
     * @param progressMonitor
     *        An {@link IProgressMonitor} to use (guaranteed to not be
     *        <code>null</code>). Subclasses do not need to call
     *        {@link IProgressMonitor#done()} on the instance.
     * @return the outcome of this command run as an {@link IStatus} (see
     *         {@link ICommand#run(IProgressMonitor)})
     * @throws Exception
     */
    protected abstract IStatus doRun(IProgressMonitor progressMonitor) throws Exception;

    /**
     * {@inheritDoc}
     */
    @Override
    public final IStatus run(IProgressMonitor progressMonitor) throws Exception {
        if (progressMonitor == null) {
            progressMonitor = new NullProgressMonitor();
        }

        progressMonitor.setTaskName(getName());

        try {
            for (final CommandInitializationRunnable initializationRunnable : initializationRunnables) {
                initializationRunnable.initialize(progressMonitor);
            }

            return doRun(progressMonitor);
        } finally {
            /*
             * Run the completion runnables - if there's an exception in any of
             * them, keep processing them and do not propogate the exception.
             */
            for (final CommandInitializationRunnable initializationRunnable : initializationRunnables) {
                try {
                    initializationRunnable.complete(progressMonitor);
                } catch (final Throwable t) {
                    log.info("Caught unexception in command completion runnable", t); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Sets the exception handler for this command.
     *
     * @param handler
     *        The {@link ICommandExceptionHandler} to use for exceptions.
     */
    protected final void setExceptionHandler(final ICommandExceptionHandler handler) {
        setExceptionHandlers(new ICommandExceptionHandler[] {
            handler
        });
    }

    /**
     * Sets the list of exception handlers for this command. Exception handlers
     * will be called in order (index 0 first, then index 1), and the first
     * returning an IStatus will make the IStatus for this command.
     *
     * @param handlers
     *        The {@link ICommandExceptionHandler}s to use for exceptions.
     */
    protected final void setExceptionHandlers(final ICommandExceptionHandler[] handlers) {
        exceptionHandlerList.clear();
        exceptionHandlerList.addAll(Arrays.asList(handlers));
    }

    /**
     * Appends the given exception handler to the front of the exception handler
     * list. That is, this will be called first on an exception.
     *
     * @param handler
     *        The exception handler to add.
     */
    protected final void addExceptionHandler(final ICommandExceptionHandler handler) {
        exceptionHandlerList.add(0, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICommandExceptionHandler getExceptionHandler() {
        if (exceptionHandlerList.size() == 0) {
            return new NullCommandExceptionHandler();
        } else {
            final ICommandExceptionHandler[] exceptionHandlers =
                exceptionHandlerList.toArray(new ICommandExceptionHandler[exceptionHandlerList.size()]);
            return new MultiCommandExceptionHandler(exceptionHandlers);
        }
    }

    /**
     * Adds a {@link CommandInitializationRunnable} that will be called before
     * and after command subclass execution. Runnables will be executed in the
     * order they are added, and if the command subclasses's
     * {@link doRun(IProgressMonitor)} method throws an exception, the cleanup
     * methods will still be executed.
     *
     * @param runnable
     *        The {@link CommandInitializationRunnable} to add.
     */
    public void addCommandInitializationRunnable(final CommandInitializationRunnable runnable) {
        Check.notNull(runnable, "runnable"); //$NON-NLS-1$

        initializationRunnables.add(runnable);
    }
}
