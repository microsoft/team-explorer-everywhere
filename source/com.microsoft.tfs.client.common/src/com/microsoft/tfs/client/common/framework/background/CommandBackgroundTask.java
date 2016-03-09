// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.background;

import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.util.Check;

/**
 * An {@link IBackgroundTask} that is backed by a TFS {@link ICommand}.
 */
public class CommandBackgroundTask implements IBackgroundTask {
    private final ICommand command;
    private final IProgressMonitor progressMonitor;

    public CommandBackgroundTask(final ICommand command) {
        this(command, null);
    }

    public CommandBackgroundTask(final ICommand command, final IProgressMonitor progressMonitor) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        this.command = command;
        this.progressMonitor = progressMonitor;
    }

    @Override
    public String getName() {
        return command.getName();
    }

    @Override
    public boolean isCancellable() {
        return progressMonitor != null && command.isCancellable();
    }

    @Override
    public boolean cancel() {
        if (progressMonitor != null) {
            progressMonitor.setCanceled(true);
            return progressMonitor.isCanceled();
        }

        return false;
    }
}