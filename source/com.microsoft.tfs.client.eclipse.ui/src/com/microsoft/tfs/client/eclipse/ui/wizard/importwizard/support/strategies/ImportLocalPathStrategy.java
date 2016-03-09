// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.strategies;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolder;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportNewProjectAction;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * Defines a strategy for determining the local path to use as part of an
 * import.
 */
public abstract class ImportLocalPathStrategy {
    /**
     * Gets the local path involved in the import.
     *
     * @param selectedPath
     *        the selected server path
     * @param importOptions
     *        the import options
     * @return the local path to use for import, or null to cancel this import
     */
    public abstract String getLocalPath(ImportFolder selectedPath, ImportOptions importOptions) throws CoreException;

    /**
     * Gets the local path involved in the operation or perhaps a non-path
     * string like "a folder determined by the wizard", for use in tool-tips,
     * etc.
     *
     * @param selectedPath
     *        the selected server path
     * @param importOptions
     *        the import options
     * @return the local path description
     */
    public abstract String getLocalPathDescription(ImportFolder selectedPath, ImportOptions importOptions);

    /**
     * WARNING: the return value is non-localized and should be used for logging
     * or debugging purposes only, never displayed directly in the UI.
     *
     * @param selectedPath
     *        the selected server path
     * @param importOptions
     *        the import options
     * @return some text describing what this strategy will do
     */
    public abstract String getPlan(ImportFolder selectedPath, ImportOptions importOptions);

    public String[] getFilePathsToAdd() {
        return null;
    }

    /**
     * A LocalPathStrategy that shows the New Project Wizard to determine the
     * local path.
     */
    public static class NewProjectWizard extends ImportLocalPathStrategy {
        private final ImportNewProjectAction newProjectAction;

        private String[] filepaths;

        public NewProjectWizard(final ImportNewProjectAction newProjectAction) {
            this.newProjectAction = newProjectAction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return "determine local path using the New Project Wizard"; //$NON-NLS-1$
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getLocalPathDescription(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return Messages.getString("ImportLocalPathStrategy.LocalPathDescriptionAsDeterminedByTheWizard"); //$NON-NLS-1$
        }

        @Override
        public String getLocalPath(final ImportFolder selectedPath, final ImportOptions importOptions)
            throws CoreException {
            /*
             * We need to use the Eclipse IResourceListener mechanism to pick up
             * the new project when it is created. We are delegating the project
             * creation to NewProjectAction, and we won't be able to directly
             * access the result of NewProjectAction.
             */
            final RCL newProjectListener = new RCL();
            importOptions.getEclipseWorkspace().addResourceChangeListener(
                newProjectListener,
                IResourceChangeEvent.POST_CHANGE);

            TaskMonitorService.getTaskMonitor().setCurrentWorkDescription(
                MessageFormat.format(
                    Messages.getString("ImportLocalPathStrategy.NewProjectWizardForPathFormat"), //$NON-NLS-1$
                    selectedPath.getFullPath()));

            try {
                newProjectAction.run();
            } finally {
                /*
                 * Always remove the IResourceListener, even if we throw an
                 * exception out of this block.
                 */
                importOptions.getEclipseWorkspace().removeResourceChangeListener(newProjectListener);
            }

            final IProject eclipseProject = newProjectListener.getNewProject();
            filepaths = newProjectListener.getFilepaths();

            if (eclipseProject == null) {
                /*
                 * the New Project wizard was cancelled
                 */
                return null;
            }

            return eclipseProject.getLocation().toOSString();
        }

        @Override
        public String[] getFilePathsToAdd() {
            return filepaths;
        }
    }

    /**
     * A LocalPathStrategy that uses the name portion of the selected server
     * path to determine local path to use.
     */
    public static class FromServerPath extends ImportLocalPathStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return MessageFormat.format(
                "local path [{0}] (from server path)", //$NON-NLS-1$
                importOptions.getEclipseWorkspace().getRoot().getLocation().append(
                    selectedPath.getName()).toOSString());
        }

        @Override
        public String getLocalPathDescription(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return importOptions.getEclipseWorkspace().getRoot().getLocation().append(
                selectedPath.getName()).toOSString();
        }

        @Override
        public String getLocalPath(final ImportFolder selectedPath, final ImportOptions importOptions)
            throws CoreException {
            return importOptions.getEclipseWorkspace().getRoot().getLocation().append(
                selectedPath.getName()).toOSString();
        }
    }

    /**
     * A LocalPathStrategy that is used when a working folder mapping already
     * exists (ie the import process does not need to create a mapping). In this
     * case the local path is easy to determine as it's already contained in the
     * working folder mapping.
     */
    public static class ExistingMapping extends ImportLocalPathStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return MessageFormat.format(
                "local path [{0}] (existing mapping)", //$NON-NLS-1$
                selectedPath.getExistingWorkingFolderMapping());
        }

        @Override
        public String getLocalPathDescription(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return selectedPath.getExistingWorkingFolderMapping();
        }

        @Override
        public String getLocalPath(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return selectedPath.getExistingWorkingFolderMapping();
        }
    }

    /**
     * A LocalPathStrategy that is used when a .project file already exists on
     * the server. The name of the local path comes from the name of the project
     * in the .project file.
     */
    public static class ExistingProjectMetadataFile extends ImportLocalPathStrategy {
        @Override
        public String getPlan(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return MessageFormat.format(
                "download [{0}] to get local path", //$NON-NLS-1$
                selectedPath.getExistingProjectMetadataFileItem().getServerItem());
        }

        @Override
        public String getLocalPathDescription(final ImportFolder selectedPath, final ImportOptions importOptions) {
            return selectedPath.getExistingProjectMetadataFileItem().getServerItem();
        }

        @Override
        public String getLocalPath(final ImportFolder selectedPath, final ImportOptions importOptions)
            throws CoreException {
            /*
             * download the .project file
             */
            final VersionControlClient vcClient = importOptions.getTFSWorkspace().getClient();

            final String localTemporaryDotProjectFilePath =
                selectedPath.getExistingProjectMetadataFileItem().downloadFileToTempLocation(
                    vcClient,
                    IProjectDescription.DESCRIPTION_FILE_NAME).getAbsolutePath();

            /*
             * parse in the .project file and create an IProjectDescription
             */
            final IProjectDescription temporaryProjectDescription =
                importOptions.getEclipseWorkspace().loadProjectDescription(new Path(localTemporaryDotProjectFilePath));

            /*
             * the projectName comes from the IProjectDescription
             */
            final String projectName = temporaryProjectDescription.getName();

            /*
             * Make sure that there are no existing projects with this name.
             * (This would be a mapping failure later, better to catch it now
             * with a better explanation.)
             */
            final IProject existingProject = importOptions.getEclipseWorkspace().getRoot().getProject(projectName);
            if (existingProject != null && existingProject.exists()) {
                throw new RuntimeException(
                    MessageFormat.format(
                        Messages.getString("ImportLocalPathStrategy.ProjectWithSameNameExistsFormat"), //$NON-NLS-1$
                        selectedPath.getFullPath(),
                        projectName));
            }

            return importOptions.getEclipseWorkspace().getRoot().getLocation().append(projectName).toOSString();
        }
    }

    /**
     * An IResourceChangeListener used by the "new project wizard" strategy to
     * pick up where the new project wizard creates the project.
     *
     * Resources are filtered using the standard action filter.
     *
     * Had to shorten this class name from ResourceChangeListener to RCL to work
     * around Windows path length limits during build.
     */
    private static class RCL implements IResourceChangeListener {
        private IProject newProject = null;
        private final List filepaths = new ArrayList();

        @Override
        public void resourceChanged(final IResourceChangeEvent event) {
            try {
                event.getDelta().accept(new IResourceDeltaVisitor() {
                    @Override
                    public boolean visit(final IResourceDelta delta) throws CoreException {
                        if (delta.getKind() == IResourceDelta.ADDED) {
                            final IResource resource = delta.getResource();
                            if (resource.getType() == IResource.PROJECT) {
                                newProject = (IProject) resource;
                            } else if (resource.getType() == IResource.FILE
                                && PluginResourceFilters.STANDARD_FILTER.filter(
                                    resource) == ResourceFilterResult.ACCEPT) {
                                filepaths.add(resource.getLocation().toOSString());
                            }
                        }
                        return true;
                    }
                });
            } catch (final CoreException e) {
                throw new RuntimeException(e);
            }
        }

        public IProject getNewProject() {
            return newProject;
        }

        public String[] getFilepaths() {
            return (String[]) filepaths.toArray(new String[filepaths.size()]);
        }
    }
}
