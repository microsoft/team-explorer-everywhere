// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.command;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * This class adapts an instance of {@link ICommand} to the
 * {@link IRunnableWithProgress} interface.
 * </p>
 *
 * <p>
 * The {@link IRunnableWithProgress#run(IProgressMonitor)} method is implemented
 * by directly calling the {@link ICommand#run(IProgressMonitor)} method of the
 * command being wrapped by this adapter. If the command throws an exception,
 * that exception is wrapped by an {@link InvocationTargetException} and
 * re-thrown as per the {@link IRunnableWithProgress#run(IProgressMonitor)}
 * contract. Otherwise, the {@link IStatus} returned from
 * {@link ICommand#run(IProgressMonitor)} is saved and made available by calling
 * {@link #getStatus()}.
 * </p>
 *
 * @see ICommand
 * @see IRunnableWithProgress
 */
public class RunnableWithProgressCommandAdapter implements IRunnableWithProgress {
    private final ICommand command;
    private IStatus status;

    /**
     * Creates a new {@link RunnableWithProgressCommandAdapter}, adapting the
     * given {@link ICommand} to the {@link IRunnableWithProgress} interface.
     *
     * @param command
     *        the {@link ICommand} to adapt (must not be null)
     */
    public RunnableWithProgressCommandAdapter(final ICommand command) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        this.command = command;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core
     * .runtime.IProgressMonitor)
     */
    @Override
    public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        status = null;
        try {
            status = command.run(monitor);
            if (status == null) {
                status = Status.OK_STATUS;
            }
        } catch (final Exception e) {
            throw new InvocationTargetException(e);
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

    @Override
    public String toString() {
        final String messageFormat = "IRWP adapter for command \"{0}\""; //$NON-NLS-1$
        return MessageFormat.format(messageFormat, command.getName());
    }
}
