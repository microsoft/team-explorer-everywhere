// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * A command wrapper that both locks the Eclipse workspace and executes the
 * command in the ResourceChangingCommand context, meaning that various TFS
 * plugin components will be notified that we are performing work from the
 * server.
 *
 * @see {@link WorkspaceCommand}
 */
public class ResourceChangingCommand extends WorkspaceCommand {
    public ResourceChangingCommand(final ICommand wrappedCommand) {
        super(wrappedCommand);
    }

    public ResourceChangingCommand(final ICommand wrappedCommand, final ISchedulingRule schedulingRule) {
        super(wrappedCommand, schedulingRule);
    }

    @Override
    public synchronized IStatus run(final IProgressMonitor progressMonitor) throws Exception {
        final SingleListenerFacade listener = ResourceChangingCommandListenerLoader.getListener();

        ((ResourceChangingCommandListener) listener.getListener()).commandStarted();

        try {
            return super.run(progressMonitor);
        } finally {
            ((ResourceChangingCommandListener) listener.getListener()).commandFinished();
        }
    }
}
