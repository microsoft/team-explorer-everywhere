// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ThreadedCancellableCommand} is a command wrapper that makes any
 * {@link ICommand} cancellable, even if the command calls a blocking,
 * non-interruptable API.
 * </p>
 *
 * <p>
 * {@link ThreadedCancellableCommand} works by creating a new {@link Thread} to
 * run the wrapped command whenever it is run. The thread that the outer run
 * method is called on is then free to respond to cancellation requests. If such
 * a cancellation request is detected, the created thread running the wrapped
 * command is ignored. The thread is NOT interrupted, as interruption leads to
 * major problems with file locking. (Old JDK bug.)
 * </p>
 *
 * <p>
 * Each instance of {@link ThreadedCancellableCommand} has a configurable time
 * values. The <b>poll time</b> is the amount of time between checks for
 * cancellation in the outer run method. This value should be small enough to
 * give responsive cancellation, but large enough to give good performance. The
 * default value ({@link #DEFAULT_POLL_TIME}) is 250 ms.
 * </p>
 *
 * <p>
 * Typical usage:
 *
 * <pre>
 *     ICommandExecutor executor = ...
 *     ICommand command = ...
 *     command = new ThreadedCancelableCommand(command);
 *     IStatus status = executor.execute(command);
 * </pre>
 *
 * </p>
 *
 * @see ICommand
 * @see Thread
 */
public class ThreadedCancellableCommand extends Command implements CommandWrapper {
    /**
     * The default poll time in milliseconds (see above). Equal to
     * <code>250</code>.
     */
    public static final long DEFAULT_POLL_TIME = 250;

    private final ICommand wrappedCommand;
    private final long pollTime;

    /**
     * Creates a new {@link ThreadedCancellableCommand}. This convenience
     * constructor is fully equivalent to:
     *
     * <pre>
     * new {@link ThreadedCancellableCommand}(wrapped, {@link #DEFAULT_POLL_TIME}});
     * </pre>
     */
    public ThreadedCancellableCommand(final ICommand wrapped) {
        this(wrapped, DEFAULT_POLL_TIME);
    }

    /**
     * Creates a new {@link ThreadedCancellableCommand} with the specified poll
     * time and wait time.
     *
     * @param wrappedCommand
     *        the {@link ICommand} to wrap (must not be <code>null</code>)
     * @param pollTime
     *        the poll time in milliseconds (see above)
     * @param waitTime
     *        the wait time in milliseconds (see above)
     */
    public ThreadedCancellableCommand(final ICommand wrappedCommand, final long pollTime) {
        Check.notNull(wrappedCommand, "wrappedCommand"); //$NON-NLS-1$

        this.wrappedCommand = wrappedCommand;
        this.pollTime = pollTime;

        setCancellable(true);
    }

    @Override
    public String getName() {
        return wrappedCommand.getName();
    }

    @Override
    public String getErrorDescription() {
        return wrappedCommand.getErrorDescription();
    }

    @Override
    public String getLoggingDescription() {
        return wrappedCommand.getLoggingDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCancellableChangedListener(final CommandCancellableListener listener) {
        if (wrappedCommand instanceof ICancellableCommand) {
            ((ICancellableCommand) wrappedCommand).addCancellableChangedListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeCancellableChangedListener(final CommandCancellableListener listener) {
        if (wrappedCommand instanceof ICancellableCommand) {
            ((ICancellableCommand) wrappedCommand).removeCancellableChangedListener(listener);
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final RunnableCommandAdapter adapter = new RunnableCommandAdapter(wrappedCommand, progressMonitor, null, null);

        final Thread commandThread = new Thread(adapter);

        final String nameFormat = "CancellableCommandThread-{0}"; //$NON-NLS-1$
        final String name = MessageFormat.format(nameFormat, Long.toString(commandThread.getId()));

        commandThread.setName(name);

        InterruptedException interruptedException = null;

        commandThread.start();

        /*
         * Loop until either the thread finishes, the progress monitor is
         * cancelled, or until this thread is interrupted.
         */
        while (commandThread.isAlive() && !progressMonitor.isCanceled()) {
            try {
                commandThread.join(pollTime);
            } catch (final InterruptedException ex) {
                interruptedException = ex;
                break;
            }
        }

        if (!commandThread.isAlive()) {
            return adapter.getStatus();
        }

        if (interruptedException != null) {
            throw interruptedException;
        }

        return Status.CANCEL_STATUS;
    }

    @Override
    public ICommand getWrappedCommand() {
        return wrappedCommand;
    }
}
