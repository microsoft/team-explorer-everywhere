// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public abstract class CancellableCommand implements ICommand, ICancellableCommand {
    private static final Log log = LogFactory.getLog(CancellableCommand.class);

    private final SingleListenerFacade cancellableChangedListeners =
        new SingleListenerFacade(CommandCancellableListener.class);

    private boolean cancellable = false;

    /**
     * Sets the cancellable state of this command. Subsequent calls to
     * {@link ICommand#isCancellable()} will return the argument.
     *
     * @param cancelable
     *        the cancellable state of this command
     */
    protected void setCancellable(final boolean cancellable) {
        final boolean fireEvent = (cancellable != this.cancellable);

        this.cancellable = cancellable;

        if (fireEvent) {
            ((CommandCancellableListener) cancellableChangedListeners.getListener()).cancellableChanged(cancellable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return cancellable;
    }

    /**
     * Adds a cancellable changed listener that will be notified when the
     * cancellability of a command changes.
     *
     * @param listener
     *        The {@link ICommandCancellableListener} that is no longer notified
     *        of cancellability changes (not <code>null</code>)
     */
    @Override
    public void addCancellableChangedListener(final CommandCancellableListener listener) {
        cancellableChangedListeners.addListener(listener);
    }

    /**
     * Removes a cancellable changed listener.
     *
     * @param listener
     *        The {@link ICommandCancellableListener} that is no longer notified
     *        of cancellability changes (not <code>null</code>)
     */
    @Override
    public void removeCancellableChangedListener(final CommandCancellableListener listener) {
        cancellableChangedListeners.removeListener(listener);
    }

    /**
     * A convenience method that subclasses can call to do one-line cancelation
     * checks. The given {@link IProgressMonitor} is checked for cancelation. If
     * it is canceled, a {@link CoreException} is thrown with a
     * <code>CANCEL</code> status inside of it.
     *
     * @param progressMonitor
     *        an {@link IProgressMonitor} to check for cancelation (must not be
     *        <code>null</code>)
     * @throws CoreException
     *         if the given {@link IProgressMonitor} was canceled
     */
    protected final void checkForCancellation(final IProgressMonitor progressMonitor) throws CoreException {
        Check.notNull(progressMonitor, "progressMonitor"); //$NON-NLS-1$

        if (progressMonitor.isCanceled()) {
            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "command [{0}] was canceled - throwing CoreException with Status.CANCEL_STATUS"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, getClass().getName());
                log.trace(message);
            }

            throw new CoreException(Status.CANCEL_STATUS);
        }
    }
}
