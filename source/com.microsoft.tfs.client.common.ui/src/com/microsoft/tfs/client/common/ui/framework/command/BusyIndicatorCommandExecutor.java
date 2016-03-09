// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.RunnableCommandAdapter;

/**
 * <p>
 * An {@link ICommandExecutor} implementation that displays a busy cursor while
 * executing an {@link ICommand}.
 * </p>
 *
 * <p>
 * This executor executes commands on the thread that the <code>execute()</code>
 * method is called on and does not supply an {@link IProgressMonitor} to
 * commands.
 * </p>
 *
 * <p>
 * In addition, this executor will raise an {@link ErrorDialog} if the status
 * when a command is finished meets certain criteria, as determined by
 * {@link ErrorDialogCommandFinishedCallback}.
 * </p>
 *
 * <p>
 * This class makes use of {@link RunnableCommandAdapter} to wrap an
 * {@link ICommand} and make the command look like a {@link Runnable}. The
 * {@link Runnable} is passed to
 * {@link BusyIndicator#showWhile(Display, Runnable)}.
 * </p>
 *
 * @see ICommandExecutor
 * @see ErrorDialogCommandFinishedCallback
 * @see RunnableCommandAdapter
 * @see BusyIndicator
 */
public class BusyIndicatorCommandExecutor extends AbstractUICommandExecutor {
    private Display display;

    /**
     * Creates a new {@link BusyIndicatorCommandExecutor} that uses the
     * specified {@link Display}.
     *
     * @param display
     *        the {@link Display} to use (must not be <code>null</code>)
     */
    public BusyIndicatorCommandExecutor(final Shell shell) {
        super(shell);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.command.CommandExecutor#execute
     * (com.microsoft.tfs.client.common.ui.shared.command.ICommand)
     */
    @Override
    public IStatus execute(final ICommand command) {
        final RunnableCommandAdapter adapter =
            new RunnableCommandAdapter(command, null, getCommandStartedCallback(), getCommandFinishedCallback());

        BusyIndicator.showWhile(display, adapter);

        return adapter.getStatus();
    }
}
