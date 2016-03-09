// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ExtendedStatus;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandFinishedCallback;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * An {@link ICommandFinishedCallback} implementation that displays an
 * {@link ErrorDialog} if the {@link IStatus} produced by running a command
 * meets certain criteria.
 * </p>
 *
 * <p>
 * If the status produced by running an {@link ICommand} is an
 * {@link ExtendedStatus}, then the
 * {@link ExtendedStatus#SHOW_MESSAGE_IN_DIALOG} flag is used to determine
 * whether to show an {@link ErrorDialog}. Otherwise, an error dialog is
 * displayed only if the status has a severity of {@link IStatus#ERROR}.
 * </p>
 *
 * @see ICommand
 * @see ExtendedStatus
 * @see ErrorDialog
 */
public class ErrorDialogCommandFinishedCallback implements ICommandFinishedCallback {
    private final Shell shell;

    /**
     * Creates a new {@link ErrorDialogCommandFinishedCallback}. The specified
     * {@link Shell} will be used to control the threading when showing the
     * error dialog.
     *
     * @param display
     *        the {@link Display} to use (must not be <code>null</code>)
     * @param shell
     *        the {@link Shell} to use (must not be <code>null</code>)
     */
    public ErrorDialogCommandFinishedCallback(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        this.shell = shell;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.shared.command.
     * ICommandFinishedCallback #onCommandFinished
     * (com.microsoft.tfs.client.common.ui.shared.command.ICommand,
     * org.eclipse.core.runtime.IStatus)
     */
    @Override
    public void onCommandFinished(final ICommand command, IStatus status) {
        boolean showErrorDialog = false;
        boolean suppressException = false;

        if (status instanceof ExtendedStatus) {
            showErrorDialog = ((ExtendedStatus) status).hasFlags(ExtendedStatus.SHOW_MESSAGE_IN_DIALOG);
            suppressException = ((ExtendedStatus) status).hasFlags(ExtendedStatus.MESSAGE_FROM_EXCEPTION);
        } else {
            showErrorDialog = status.getSeverity() == IStatus.ERROR;
        }

        if (showErrorDialog) {
            final String title = command.getErrorDescription();

            if (suppressException) {
                status = new Status(status.getSeverity(), status.getPlugin(), 0, status.getMessage(), null);
            }

            final Runnable runnable = new ErrorDialogRunnable(status, title, null, shell);

            if (Thread.currentThread() == shell.getDisplay().getThread()) {
                runnable.run();
            } else {
                shell.getDisplay().asyncExec(runnable);
            }
        }
    }

    private static class ErrorDialogRunnable implements Runnable {
        private final IStatus status;
        private final String dialogTitle;
        private final String message;
        private final Shell shell;

        public ErrorDialogRunnable(
            final IStatus status,
            final String dialogTitle,
            final String message,
            final Shell shell) {
            this.status = status;
            this.dialogTitle = dialogTitle;
            this.message = message;
            this.shell = shell;
        }

        @Override
        public void run() {
            ErrorDialog.openError(shell, dialogTitle, message, status);
        }
    }
}
