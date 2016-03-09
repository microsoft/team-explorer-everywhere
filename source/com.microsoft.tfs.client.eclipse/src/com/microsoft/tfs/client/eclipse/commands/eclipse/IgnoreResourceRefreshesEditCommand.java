// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.EditCommand;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.util.Check;

/**
 * Wraps an {@link EditCommand} that instructs the plugin to ignores resource
 * refreshes.
 *
 * @threadsafety unknown
 */
public class IgnoreResourceRefreshesEditCommand extends TFSCommand {
    private final EditCommand editCommand;

    public IgnoreResourceRefreshesEditCommand(final EditCommand editCommand) {
        Check.notNull(editCommand, "editCommand"); //$NON-NLS-1$

        this.editCommand = editCommand;
    }

    @Override
    public String getName() {
        return editCommand.getName();
    }

    @Override
    public String getErrorDescription() {
        return editCommand.getErrorDescription();
    }

    @Override
    public String getLoggingDescription() {
        return editCommand.getLoggingDescription();
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            TFSEclipseClientPlugin.getDefault().getResourceRefreshManager().startIgnoreThreadResourceRefreshEvents();

            return editCommand.run(progressMonitor);
        } finally {
            TFSEclipseClientPlugin.getDefault().getResourceRefreshManager().stopIgnoreThreadResourceRefreshEvents();
        }
    }
}
