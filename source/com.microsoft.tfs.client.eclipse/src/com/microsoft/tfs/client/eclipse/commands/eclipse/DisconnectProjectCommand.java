// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Disconnects TFS Eclipse Plug-in as the repository provider for the given
 * project.
 *
 * Note: does not actually check that TFS Eclipse Plug-in is the repository
 * provider.
 */
public final class DisconnectProjectCommand extends Command {
    private final IProject project;

    public DisconnectProjectCommand(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        this.project = project;
    }

    @Override
    public String getName() {
        return (MessageFormat.format(
            Messages.getString("DisconnectProjectCommand.CommandTextFormat"), //$NON-NLS-1$
            project.getName()));
    }

    @Override
    public String getErrorDescription() {
        return (MessageFormat.format(
            Messages.getString("DisconnectProjectCommand.ErrorTextFormat"), //$NON-NLS-1$
            project.getName()));
    }

    @Override
    public String getLoggingDescription() {
        return (MessageFormat.format(
            Messages.getString("DisconnectProjectCommand.CommandTextFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            project.getName()));
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        RepositoryProvider.unmap(project);

        TFSEclipseClientPlugin.getDefault().getProjectManager().removeProject(project);

        return Status.OK_STATUS;
    }
}
