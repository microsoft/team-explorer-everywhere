// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.CommandCancellableListener;
import com.microsoft.tfs.client.common.framework.command.CommandWrapper;
import com.microsoft.tfs.client.common.framework.command.ICancellableCommand;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.exception.ICommandExceptionHandler;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * A command wrapper that executes the command in the ResourceChangingCommand
 * context, meaning that various TFS plugin components will be notified that we
 * are performing work from the server.
 *
 * Note that this command wrapper does NOT take a lock on the Eclipse workspace,
 * which is typically preferred. This is suitable only for use when another
 * component (the caller) already has a lock on the Eclipse workspace (eg,
 * Import Wizard.)
 *
 * @see {@link ResourceChangingCommand}
 */
public class ResourceChangingNonWorkspaceCommand implements ICommand, ICancellableCommand, CommandWrapper {
    /**
     * The wrapped {@link ICommand} (never <code>null</code>).
     */
    private final ICommand wrappedCommand;

    /**
     * Creates a new {@link ResourceChangingNonWorkspaceCommand} that wraps the
     * specified {@link ICommand}.
     *
     * @param wrappedCommand
     *        an {@link ICommand} to wrap (must not be <code>null</code>)
     */
    public ResourceChangingNonWorkspaceCommand(final ICommand wrappedCommand) {
        Check.notNull(wrappedCommand, "wrappedCommand"); //$NON-NLS-1$

        this.wrappedCommand = wrappedCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICommandExceptionHandler getExceptionHandler() {
        return wrappedCommand.getExceptionHandler();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return wrappedCommand.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorDescription() {
        return wrappedCommand.getErrorDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLoggingDescription() {
        return wrappedCommand.getLoggingDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return wrappedCommand.isCancellable();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public ICommand getWrappedCommand() {
        return wrappedCommand;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStatus run(final IProgressMonitor progressMonitor) throws Exception {
        final SingleListenerFacade listener = ResourceChangingCommandListenerLoader.getListener();

        ((ResourceChangingCommandListener) listener.getListener()).commandStarted();

        try {
            return wrappedCommand.run(progressMonitor);
        } finally {
            ((ResourceChangingCommandListener) listener.getListener()).commandFinished();
        }
    }
}
