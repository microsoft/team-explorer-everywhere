// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.CancellableCommand;
import com.microsoft.tfs.client.common.framework.command.CommandCancellableListener;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.ui.framework.runnable.DeferredProgressMonitorDialogContext;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A subclass of {@link AbstractRunnableContextCommandExecutor} that provides a
 * {@link ProgressMonitorDialog}-based {@link IRunnableContext}. This executor
 * is used to run an {@link ICommand} on a background thread and display a
 * progress monitor dialog while the command runs.
 * </p>
 *
 * <p>
 * Clients of this executor can specify the <b>defer time</b> when instantiating
 * an instance of this class. The defer time is the period of time during which
 * the progress monitor dialog is suppressed after the command has started
 * running. Using a deferred UI like this prevents flicker when command runs are
 * short-lived.
 * </p>
 *
 * <p>
 * In addition, this executor will raise an {@link ErrorDialog} on the specified
 * {@link Shell} if the status when a command is finished meets certain
 * criteria, as determined by {@link ErrorDialogCommandFinishedCallback}.
 * </p>
 *
 * @see ProgressMonitorDialog
 * @see AbstractRunnableContextCommandExecutor
 * @see ErrorDialogCommandFinishedCallback
 */
public class ProgressMonitorDialogCommandExecutor extends AbstractRunnableContextCommandExecutor {
    /**
     * The default UI defer time, in milliseconds (see above). 2 seconds is the
     * standard delay for Visual Studio products.
     */
    public static final long DEFAULT_PROGRESS_UI_DEFER_TIME = 2000;

    private final Shell shell;
    private final long progressUIDeferTime;

    /**
     * Creates a new {@link ProgressMonitorDialogCommandExecutor} with a
     * specified {@link Shell} and the default UI defer time (
     * {@link #DEFAULT_PROGRESS_UI_DEFER_TIME}).
     *
     * @param parentShell
     *        the {@link Shell} to use for the {@link ProgressMonitorDialog}
     *        (must not be <code>null</code>)
     */
    public ProgressMonitorDialogCommandExecutor(final Shell parentShell) {
        this(parentShell, DEFAULT_PROGRESS_UI_DEFER_TIME);
    }

    /**
     * Creates a new {@link ProgressMonitorDialogCommandExecutor} with a
     * specified {@link Shell} and a specified UI defer time. If the specified
     * defer time is 0 or negative, the progress monitor dialog is not deferred
     * at all and displays immediately when the command run starts.
     *
     * @param shell
     *        the {@link Shell} to use for the {@link ProgressMonitorDialog}
     *        (must not be <code>null</code>)
     * @param progressUIDeferTime
     *        the UI defer time in milliseconds, or <code>0</code> for no defer
     *        time (see above)
     */
    public ProgressMonitorDialogCommandExecutor(final Shell shell, final long progressUIDeferTime) {
        super(shell);

        this.shell = shell;
        this.progressUIDeferTime = progressUIDeferTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IRunnableContext getRunnableContext(final ICommand command) {
        final IRunnableContext runnableContext;

        if (progressUIDeferTime <= 0) {
            runnableContext = new ProgressMonitorDialog(shell);
        } else {
            runnableContext = new DeferredProgressMonitorDialogContext(shell, progressUIDeferTime);
        }

        /*
         * Set up a state change listener if the command changes is
         * cancellability.
         */
        if (command instanceof CancellableCommand) {
            ((CancellableCommand) command).addCancellableChangedListener(
                new CommandCancellableChangedListener(runnableContext));
        }

        return runnableContext;
    }

    private static class CommandCancellableChangedListener implements CommandCancellableListener {
        private final IRunnableContext runnableContext;

        public CommandCancellableChangedListener(final IRunnableContext runnableContext) {
            Check.notNull(runnableContext, "runnableContext"); //$NON-NLS-1$

            this.runnableContext = runnableContext;
        }

        @Override
        public void cancellableChanged(final boolean isCancellable) {
            if (runnableContext instanceof ProgressMonitorDialog) {
                ((ProgressMonitorDialog) runnableContext).setCancelable(isCancellable);
            } else if (runnableContext instanceof DeferredProgressMonitorDialogContext) {
                ((DeferredProgressMonitorDialogContext) runnableContext).setCancelable(isCancellable);
            }
        }
    }
}
