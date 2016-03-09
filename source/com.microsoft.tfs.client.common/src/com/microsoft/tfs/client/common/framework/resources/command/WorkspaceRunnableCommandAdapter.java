// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.command;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the
 * {@link IWorkspaceRunnable} interface.
 * </p>
 *
 * <p>
 * The {@link IWorkspaceRunnable#run(IProgressMonitor)} method is implemented by
 * directly calling the {@link ICommand#run(IProgressMonitor)} method of the
 * command being wrapped by this adapter. If the command throws an exception,
 * that exception is wrapped by an {@link IStatus} and re-thrown as a
 * {@link CoreException} per the
 * {@link IWorkspaceRunnable#run(IProgressMonitor)} contract. Otherwise, the
 * {@link IStatus} returned from {@link ICommand#run(IProgressMonitor)} is saved
 * and made available by calling {@link #getStatus()}.
 * </p>
 */
public class WorkspaceRunnableCommandAdapter implements IWorkspaceRunnable {
    /**
     * The {@link ICommand} being wrapped by this adapter (never
     * <code>null</code>).
     */
    private final ICommand command;

    /**
     * The {@link IStatus} saved off each time the wrapped command is run (may
     * be <code>null</code>).
     */
    private IStatus status;

    /**
     * Creates a new {@link WorkspaceRunnableCommandAdapter} that wraps the
     * specified {@link ICommand}.
     *
     * @param command
     *        an {@link ICommand} to adapt (must not be <code>null</code>)
     */
    public WorkspaceRunnableCommandAdapter(final ICommand command) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        this.command = command;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.
     * runtime .IProgressMonitor)
     */
    @Override
    public void run(final IProgressMonitor monitor) throws CoreException {
        status = null;
        try {
            status = command.run(monitor);
            if (status == null) {
                status = Status.OK_STATUS;
            }
        } catch (final Exception e) {
            throw new CoreException(
                new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
        }
    }

    /**
     * @return The {@link IStatus} from the most recent run of the underlying
     *         {@link ICommand}, or <code>null</code> if the command has never
     *         run or the most recent run of the command threw an exception. If
     *         the command did not throw an exception and returned
     *         <code>null</code> from {@link ICommand#run(IProgressMonitor)},
     *         this method returns {@link Status#OK_STATUS}.
     */
    public IStatus getStatus() {
        return status;
    }
}
