// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.RepositoryProvider;

import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

/**
 * Defines a strategy for opening an Eclipse project as part of an import
 * operation.
 */
public abstract class ImportOpenProjectStrategy {
    /**
     * @return some text describing what this strategy will do
     */
    public abstract String getPlan();

    /**
     * Opens an Eclipse project.
     *
     * @param localPath
     *        the local path
     * @param importOptions
     *        the import options
     */
    public IProject open(final String localPath, final ImportOptions importOptions) throws CoreException {
        final IProject project = createProject(localPath, importOptions);

        if (!project.isOpen()) {
            project.open(null);
        }

        /* Register with project manager. */
        TFSEclipseClientPlugin.getDefault().getProjectManager().addProject(project, importOptions.getTFSRepository());

        if (RepositoryProvider.getProvider(project, TFSRepositoryProvider.PROVIDER_ID) == null) {
            RepositoryProvider.map(project, TFSRepositoryProvider.PROVIDER_ID);
        }

        return project;
    }

    protected abstract IProject createProject(String localPath, ImportOptions importOptions) throws CoreException;

    /**
     * An OpenProjectStrategy that will create a basic .project file.
     */
    public static class NewSimpleProject extends ImportOpenProjectStrategy {
        @Override
        public String getPlan() {
            return "create .project file"; //$NON-NLS-1$
        }

        @Override
        protected IProject createProject(final String localPath, final ImportOptions importOptions)
            throws CoreException {
            final IPath projectRootPath = new Path(localPath);

            final String projectName = new File(localPath).getName();
            final IProject eclipseProject = importOptions.getEclipseWorkspace().getRoot().getProject(projectName);
            final IProjectDescription description =
                importOptions.getEclipseWorkspace().newProjectDescription(projectName);

            description.setLocation(projectRootPath);

            /*
             * If this project is NOT to be beneath the workspace root, then we
             * need to specify the project description specifically.
             */
            final IPath workspaceRootLocation = importOptions.getEclipseWorkspace().getRoot().getLocation();

            if (LocalPath.isChild(workspaceRootLocation.toOSString(), projectRootPath.toOSString())) {
                /* Inside workspace root */
                eclipseProject.create(null, null);
            } else {
                /* Outside workspace root */
                eclipseProject.create(description, null);
            }

            return eclipseProject;
        }
    }

    /**
     * An OpenProjectStrategy that is used when an Eclipse project is already
     * open (before the import process starts).
     */
    public static class AlreadyOpen extends ImportOpenProjectStrategy {
        @Override
        public String getPlan() {
            return "Eclipse project already open"; //$NON-NLS-1$
        }

        @Override
        protected IProject createProject(final String localPath, final ImportOptions importOptions)
            throws CoreException {
            final String projectName = new File(localPath).getName();
            return importOptions.getEclipseWorkspace().getRoot().getProject(projectName);
        }
    }

    /**
     * An OpenProjectStrategy used when a .project file either already exists on
     * the server or is created by some earlier part of the import process (eg
     * the New Project Wizard).
     */
    public static class ExistingProjectMetadataFile extends ImportOpenProjectStrategy {
        @Override
        public String getPlan() {
            return "use existing or already created .project file"; //$NON-NLS-1$
        }

        @Override
        protected IProject createProject(final String localPath, final ImportOptions importOptions)
            throws CoreException {
            final IPath projectRootPath = new Path(localPath);

            final IProjectDescription description = importOptions.getEclipseWorkspace().loadProjectDescription(
                projectRootPath.append(IProjectDescription.DESCRIPTION_FILE_NAME));

            final String projectName = description.getName();

            description.setLocation(projectRootPath);

            final IProject eclipseProject = importOptions.getEclipseWorkspace().getRoot().getProject(projectName);

            /*
             * If this project is NOT to be beneath the workspace root, then we
             * need to specify the project description specifically.
             */
            final IPath workspaceRootLocation = importOptions.getEclipseWorkspace().getRoot().getLocation();

            /*
             * We can only import into the workspace root if the path is
             * workspaceroot/projectname
             */
            if (LocalPath.equals(
                projectRootPath.toOSString(),
                LocalPath.combine(workspaceRootLocation.toOSString(), projectName))) {
                /* Must not pass description if path is in workspace root. */
                eclipseProject.create(null, null);
            }
            /*
             * Otherwise, the import is into the workspace root, and the
             * directory name is not the project name -- fail here. The UI
             * should have caught this ahead of time.
             */
            else if (LocalPath.isChild(workspaceRootLocation.toOSString(), projectRootPath.toOSString())) {
                throw new CoreException(
                    new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, MessageFormat.format(
                        //@formatter:off
                        Messages.getString("ImportOpenProjectStrategy.ProjectIsMappedInEclipserootButInWrongFolderNameFormat"), //$NON-NLS-1$
                        //@formatter:on
                        projectName,
                        projectRootPath.lastSegment(),
                        projectName), null));
            }
            /* Outside the workspace root, users can do whatever they want. */
            else {
                /*
                 * Must pass description if the path is not in workspace root.
                 */
                eclipseProject.create(description, null);
            }

            return eclipseProject;
        }
    }
}
