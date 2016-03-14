// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.egit.core.op.ConnectProviderOperation;
import org.eclipse.ui.IWorkingSet;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.git.EclipseProjectInfo;
import com.microsoft.tfs.client.common.ui.helpers.WorkingSetHelper;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportEclipseProject;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.CanceledException;

public class ImportEclipseProjectsCommand extends TFSCommand {
    private static final String GIT_FOLDER_NAME = ".git"; //$NON-NLS-1$

    private static final Log logger = LogFactory.getLog(ImportEclipseProjectsCommand.class);

    private final EclipseProjectInfo[] projects;
    private final IWorkspace workspace;
    private final IWorkingSet workingSet;

    public ImportEclipseProjectsCommand(
        final IWorkspace workspace,
        final EclipseProjectInfo[] projects,
        final IWorkingSet workingSet) {
        this.workspace = workspace;
        this.projects = projects;
        this.workingSet = workingSet;
        setCancellable(true);
    }

    @Override
    public String getName() {
        return Messages.getString("ImportEclipseProjectsCommand.CommandName"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("ImportEclipseProjectsCommand.ErrorMessage"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(getName(), projects.length * 100);

        MultiStatus status = new MultiStatus(
            TFSCommonClientPlugin.PLUGIN_ID,
            0,
            Messages.getString("ImportEclipseProjectsCommand.ImportWarningMessage"), //$NON-NLS-1$
            null);
        boolean noProjectsCreated = true;

        for (final EclipseProjectInfo projectInfo : projects) {
            try {
                final String projectName = projectInfo.getProjectName();
                final IProject project = workspace.getRoot().getProject(projectName);
                final IProjectDescription description;

                if (projectInfo.getProjectDescription() == null) {
                    description = workspace.newProjectDescription(projectName);
                } else {
                    description = projectInfo.getProjectDescription();
                }

                if (LocalPath.isDirectChild(
                    workspace.getRoot().getLocation().toOSString(),
                    projectInfo.getProjectPath())) {
                    /*
                     * The Project's folder is a direct child of the Eclipse
                     * workspace. Use default location.
                     */
                    description.setLocation(null);
                } else {
                    description.setLocation(new Path(projectInfo.getProjectPath()));
                }

                description.setName(projectName);

                project.create(description, new SubProgressMonitor(progressMonitor, 10));
                project.open(new SubProgressMonitor(progressMonitor, 30));

                if (projectInfo instanceof ImportEclipseProject) {
                    final String repoPath = getRepoPath((ImportEclipseProject) projectInfo);

                    if (StringUtil.isNullOrEmpty(repoPath)) {
                        logger.warn(
                            "Git repository not found for the project " + projectInfo.getProjectPath(), //$NON-NLS-1$
                            new Exception("Fake exception for call stack trace:")); //$NON-NLS-1$
                    } else {
                        final ConnectProviderOperation op = new ConnectProviderOperation(project, new File(repoPath));
                        op.execute(new NullProgressMonitor());
                    }
                }

                project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(progressMonitor, 50));

                noProjectsCreated = false;

                if (workingSet != null) {
                    WorkingSetHelper.addToWorkingSet(project, workingSet);
                }
                progressMonitor.worked(10);
            } catch (final CanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (final Exception e) {
                TFSEclipseClientPlugin.getDefault().getConsole().printErrorMessage(e.getLocalizedMessage());
                logger.error(e.getLocalizedMessage(), e);
                status.add(
                    new Status(IStatus.WARNING, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), null));
            }
        }

        progressMonitor.done();

        if (!status.isOK()) {
            if (noProjectsCreated) {
                /*
                 * Raise the status severity to ERROR
                 */
                final IStatus[] children = status.getChildren();

                status = new MultiStatus(TFSCommonClientPlugin.PLUGIN_ID, 0, new IStatus[] {
                    new Status(
                        IStatus.ERROR,
                        TFSCommonClientPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("ImportEclipseProjectsCommand.ImportErrorMessage"), //$NON-NLS-1$
                        null)
                }, Messages.getString("ImportEclipseProjectsCommand.ImportErrorMessage"), null); //$NON-NLS-1$

                for (final IStatus child : children) {
                    status.add(child);
                }
            }

            return status;
        } else {
            return Status.OK_STATUS;
        }
    }

    private static String getRepoPath(final ImportEclipseProject project) {
        if (project.getRepository() != null) {
            return LocalPath.combine(project.getRepository().getWorkingDirectory(), GIT_FOLDER_NAME);
        }

        /*
         * The project has been created as a General Eclipse Project form a
         * folder (possible nested deep in the repository working directory). In
         * this case the Repository reference is null (NB! a bad design, should
         * be reviewed in the future) and we have to browse the folder structure
         * to find the parent working directory.
         */
        String path = project.getProjectPath();
        final String rootPath = LocalPath.getPathRoot(path);

        while (!StringUtil.isNullOrEmpty(path) && rootPath != path) {
            final String repoPath = LocalPath.combine(path, GIT_FOLDER_NAME);

            if (LocalPath.exists(repoPath)) {
                return repoPath;
            }

            path = LocalPath.getParent(path);
        }

        return null;
    }
}
