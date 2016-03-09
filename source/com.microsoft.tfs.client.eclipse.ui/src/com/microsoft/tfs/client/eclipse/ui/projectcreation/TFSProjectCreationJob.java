// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.projectcreation;

import java.text.MessageFormat;
import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;

import com.microsoft.tfs.client.common.commands.vc.ScanLocalWorkspaceCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;

public class TFSProjectCreationJob extends Job {
    private final IProject project;

    TFSProjectCreationJob(final IProject project) {
        super(MessageFormat.format(Messages.getString("TFSProjectCreationJob.JobNameFormat"), project.getName())); //$NON-NLS-1$

        this.project = project;
    }

    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        /*
         * Get the current TFS Repository (may be null)
         */
        final TFSRepository repository =
            TFSEclipseClientPlugin.getDefault().getRepositoryManager().getDefaultRepository();

        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

        try {
            boolean projectOnline = false;

            if (repository != null
                && repository.getWorkspace().getMappedServerPath(project.getLocation().toOSString()) != null) {
                /*
                 * This is in the current (online) repository, simply hook it up
                 * as such.
                 */
                RepositoryProvider.map(project, TFSRepositoryProvider.PROVIDER_ID);
                projectManager.addProject(project, repository);
                projectOnline = true;
            } else {
                /*
                 * If there are no other projects configured as TFS projects,
                 * then connect this one to the server. Otherwise, defer
                 * connection.
                 */
                final boolean connectProject = (projectManager.getProjects().length == 0);

                /* Otherwise, configure ourselves as the repository provider. */
                RepositoryProvider.map(project, TFSRepositoryProvider.PROVIDER_ID);

                if (connectProject) {
                    projectManager.addAndConnectProject(project);
                    projectOnline = true;
                } else {
                    projectManager.addOfflineProject(project);
                }
            }

            if (projectOnline
                && repository != null
                && repository.getWorkspace().getLocation() == WorkspaceLocation.LOCAL) {
                UICommandExecutorFactory.newUICommandExecutor(ShellUtils.getWorkbenchShell()).execute(
                    new ScanLocalWorkspaceCommand(repository, Arrays.asList(new String[] {
                        project.getLocation().toOSString()
                })));
            }
        } catch (final TeamException e) {
            return new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, e.getLocalizedMessage(), e);
        }

        return Status.OK_STATUS;
    }
}