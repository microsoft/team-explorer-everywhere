// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.commands.vc.SetWorkingFolderCommand;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectAction;
import com.microsoft.tfs.client.eclipse.commands.eclipse.share.ShareProjectConfiguration;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * This command is used to share projects to Team Foundation Server. This works
 * in one of two ways:
 *
 * If a project has a working folder mapping, it will merely connect the TFS
 * Eclipse Plug-in as the repository provider for a project.
 *
 * If a project does not have working folder mappings, it will create working
 * folder mappings, pend an add for the project, then connect TFS Eclipse
 * Plug-in as the repository provider.
 *
 * If errors occur sharing any projects, the entire state will be undone.
 */
public final class ShareProjectsCommand extends Command {
    private final TFSRepository repository;
    private final ShareProjectConfiguration[] configuration;

    public ShareProjectsCommand(final TFSRepository repository, final ShareProjectConfiguration[] configuration) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNullOrEmpty(configuration, "configuration"); //$NON-NLS-1$

        this.repository = repository;
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        if (configuration.length == 1) {
            return (Messages.getString("ShareProjectsCommand.CommandTextSingular")); //$NON-NLS-1$
        } else {
            return (Messages.getString("ShareProjectsCommand.CommandTextPlural")); //$NON-NLS-1$
        }
    }

    @Override
    public String getErrorDescription() {
        if (configuration.length == 1) {
            return (Messages.getString("ShareProjectsCommand.ErrorTextSingular")); //$NON-NLS-1$
        } else {
            return (Messages.getString("ShareProjectsCommand.ErrorTextPlural")); //$NON-NLS-1$
        }
    }

    @Override
    public String getLoggingDescription() {
        if (configuration.length == 1) {
            return (Messages.getString("ShareProjectsCommand.CommandTextSingular", LocaleUtil.ROOT)); //$NON-NLS-1$
        } else {
            return (Messages.getString("ShareProjectsCommand.CommandTextPlural", LocaleUtil.ROOT)); //$NON-NLS-1$
        }
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final CommandList commands = new CommandList(getName(), getErrorDescription());

        commands.setRollback(true);

        /* Map the projects */
        for (int i = 0; i < configuration.length; i++) {
            final String serverPath = configuration[i].getServerPath();
            final String localPath = configuration[i].getProject().getLocation().toOSString();

            /*
             * Do not map projects that are already mapped (and only need
             * connecting)
             */
            if (configuration[i].getAction() != ShareProjectAction.MAP_AND_UPLOAD) {
                continue;
            }

            commands.addCommand(new SetWorkingFolderCommand(repository, serverPath, localPath));
        }

        /* Add projects to TFS */
        for (int i = 0; i < configuration.length; i++) {
            /*
             * Do not add projects that are already mapped (and only need
             * connecting)
             */
            if (configuration[i].getAction() != ShareProjectAction.MAP_AND_UPLOAD
                && configuration[0].getAction() != ShareProjectAction.UPLOAD) {
                continue;
            }

            commands.addCommand(new AddProjectCommand(repository, configuration[i].getProject()));
        }

        /* Connect projects to TFS Eclipse Plug-in */
        for (int i = 0; i < configuration.length; i++) {
            commands.addCommand(new ConnectProjectCommand(configuration[i].getProject(), repository));
        }

        return commands.run(progressMonitor);
    }
}
