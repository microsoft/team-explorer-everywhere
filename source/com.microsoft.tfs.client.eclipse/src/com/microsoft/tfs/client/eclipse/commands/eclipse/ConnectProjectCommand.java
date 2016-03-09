// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.framework.command.UndoableCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Connects TFS Eclipse Plug-in as the repository provider for the given
 * project.
 */
public final class ConnectProjectCommand extends TFSCommand implements UndoableCommand {
    private final IProject project;
    private final TFSRepository repository;

    public ConnectProjectCommand(final IProject project, final TFSRepository repository) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.project = project;
        this.repository = repository;
    }

    @Override
    public String getName() {
        final String messageFormat = Messages.getString("ConnectProjectCommand.CommandTextFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, project.getName());
    }

    @Override
    public String getErrorDescription() {
        return MessageFormat.format(Messages.getString("ConnectProjectCommand.ErrorTextFormat"), project.getName()); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        final String messageFormat = Messages.getString("ConnectProjectCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, project.getName());
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        TFSEclipseClientPlugin.getDefault().getProjectManager().addProject(project, repository);
        RepositoryProvider.map(project, TFSRepositoryProvider.PROVIDER_ID);

        return Status.OK_STATUS;
    }

    @Override
    public IStatus rollback(final IProgressMonitor progressMonitor) throws Exception {
        return new DisconnectProjectCommand(project).run(progressMonitor);
    }
}
