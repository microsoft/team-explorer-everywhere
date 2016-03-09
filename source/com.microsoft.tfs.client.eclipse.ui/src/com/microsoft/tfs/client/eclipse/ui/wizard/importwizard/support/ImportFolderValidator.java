// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationFlag;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportFolderValidation.ImportFolderValidationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.util.Check;

/**
 * This is a helper class to do (server) path validation in the import wizard.
 * This was broken out because SelectionSet and the ImportWizardTreePage's label
 * decorator were duplicating too much code.
 */
public class ImportFolderValidator {
    private static final String SERVER_ROOT = Messages.getString("ImportFolderValidator.CannotImportServerRoot"); //$NON-NLS-1$
    private static final String PARENT_OF_EXISTING_PROJECT_MESSAGE =
        Messages.getString("ImportFolderValidator.AlreadyHasMappedChildFormat"); //$NON-NLS-1$
    private static final String CHILD_OF_EXISTING_PROJECT_MESSAGE =
        Messages.getString("ImportFolderValidator.BeneathExistingProjectFormat"); //$NON-NLS-1$
    private static final String EXISTING_PROJECT_NAME_AND_LOCATION_MESSAGE =
        Messages.getString("ImportFolderValidator.AlreadyImportedFormat"); //$NON-NLS-1$
    private static final String EXISTING_PROJECT_NAME_MESSAGE =
        Messages.getString("ImportFolderValidator.ProjectNameAlreadyExistsFormat"); //$NON-NLS-1$
    private static final String EXISTING_PROJECT_LOCATION_MESSAGE =
        Messages.getString("ImportFolderValidator.ProjectNameAlreadyExistsAtFormat"); //$NON-NLS-1$
    private static final String INVALID_PROJECT_NAME_MESSAGE =
        Messages.getString("ImportFolderValidator.ProjectMappedLocallyFormat"); //$NON-NLS-1$
    private static final String INVALID_PROJECT_DEPTH_MESSAGE =
        Messages.getString("ImportFolderValidator.ProjectNotMappedDirectlyFormat"); //$NON-NLS-1$
    private static final String CLOAKED_MESSAGE = Messages.getString("ImportFolderValidator.PathIsCloakedFormat"); //$NON-NLS-1$

    private final ImportOptions importOptions;

    /*
     * from full server path -> AWorkingFolder
     */
    private Map existingMappings = null;

    /*
     * from full server path -> ImportPathValidation
     */
    private Map validationCache = new HashMap();

    /*
     * Path to eclipse workspace root
     */
    private String workspaceRoot = null;

    public ImportFolderValidator(final ImportOptions importOptions) {
        Check.notNull(importOptions, "importOptions"); //$NON-NLS-1$

        this.importOptions = importOptions;
    }

    /**
     * Drops internal caches. This should be called when the ImportOptions have
     * changed internally.
     */
    public void refresh() {
        existingMappings = null;
        validationCache = null;
        workspaceRoot = null;
    }

    private WorkingFolder getExistingMapping(final String serverPath) {
        if (existingMappings == null) {
            existingMappings = new HashMap();

            final WorkingFolder[] workingFolders = importOptions.getTFSWorkspace().getFolders();
            for (int i = 0; i < workingFolders.length; i++) {
                existingMappings.put(workingFolders[i].getServerItem(), workingFolders[i]);
            }
        }

        return (WorkingFolder) existingMappings.get(serverPath);
    }

    private String getWorkspaceRoot() {
        if (workspaceRoot == null) {
            workspaceRoot = importOptions.getEclipseWorkspace().getRoot().getLocation().toOSString();
        }

        return workspaceRoot;
    }

    private String getMappedPath(final String path) {
        return importOptions.getTFSWorkspace().getMappedLocalPath(path);
    }

    private String getProjectName(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        /*
         * This is the name (we guess) of the project.
         *
         * TODO: download the .project file from the server and parse it.
         */
        return ServerPath.getFileName(path);
    }

    /*
     * This is either the mapped path, or where will we be after import
     */
    private String getProjectPath(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        final String mappedPath = getMappedPath(path);

        if (mappedPath != null) {
            return mappedPath;
        }

        return LocalPath.combine(getWorkspaceRoot(), getProjectName(path));
    }

    /**
     * Validate a potential path for import
     *
     * @param path
     * @return
     */
    public ImportFolderValidation validate(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        if (validationCache == null) {
            validationCache = new HashMap();
        } else {
            if (validationCache.containsKey(path)) {
                return (ImportFolderValidation) validationCache.get(path);
            }
        }

        final ImportFolderValidation validation = computeValidation(path);

        validationCache.put(path, validation);

        return validation;
    }

    private ImportFolderValidation computeValidation(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        ImportFolderValidation validation;

        /*
         * Simply don't allow root to be imported. (Important because we can't
         * determine a project name for it further down in validation. But it
         * really doesn't make any sense anyway, since we couldn't add a
         * .project file, etc.)
         */
        if (ServerPath.equals(path, ServerPath.ROOT)) {
            return new ImportFolderValidation(
                ImportFolderValidationStatus.ERROR,
                SERVER_ROOT,
                ImportFolderValidationFlag.NO_VISUAL_ERROR);
        }

        if (importOptions.getCapabilityFlags().contains(SourceControlCapabilityFlags.GIT)) {
            return new ImportFolderValidation(ImportFolderValidationStatus.OK, null, ImportFolderValidationFlag.NONE);
        }

        /* This project is cloaked */
        if ((validation = computeCloak(path)) != null) {
            return validation;
        }

        /* This project is at (or beneath) an existing Eclipse project */
        if ((validation = computeAgainstExistingProjects(path)) != null) {
            return validation;
        }

        /*
         * This project is mapped beneath the Eclipse workspace root, but with
         * the wrong name.
         */
        if ((validation = computeInvalidProjectName(path)) != null) {
            return validation;
        }

        /*
         * Some validation errors propagate down. Handle them.
         */
        if ((validation = computeHierarchy(path)) != null) {
            return validation;
        }

        final ImportFolderValidationFlag flags = (getMappedPath(path) != null)
            ? ImportFolderValidationFlag.EXISTING_MAPPING : ImportFolderValidationFlag.NONE;

        return new ImportFolderValidation(ImportFolderValidationStatus.OK, null, flags);
    }

    private ImportFolderValidation computeHierarchy(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        /* Can't have a problem with root's parent! */
        if (ServerPath.equals(ServerPath.ROOT, path)) {
            return null;
        }

        /*
         * Validate will handle recursion for us (by eventually calling
         * computeHierarchy())
         */
        final ImportFolderValidation parentValidation = validate(ServerPath.getParent(path));

        /* Only propogate cloaked and already exists errors */
        if (parentValidation != null && parentValidation.hasFlag(ImportFolderValidationFlag.RECURSIVE)) {
            return parentValidation;
        }

        return null;
    }

    private ImportFolderValidation computeCloak(final String path) {
        final WorkingFolder workfold = getExistingMapping(path);

        /* Stop at the topmost working folder mapping */
        if (workfold != null) {
            /* Cloaked */
            if (workfold.getLocalItem() == null) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.CLOAKED,
                    MessageFormat.format(CLOAKED_MESSAGE, new Object[] {
                        path,
                        importOptions.getTFSWorkspace().getName()
                }));
            }

            /* Not cloaked */
            return null;
        }

        /* Stop recusion at the root */
        if (ServerPath.equals(ServerPath.ROOT, path)) {
            return null;
        }

        /* Recurse into the parent */
        return computeCloak(ServerPath.getParent(path));
    }

    private ImportFolderValidation computeInvalidProjectName(final String path) {
        /*
         * Get the existing working folder mapping.
         */
        final String mappedPath = getMappedPath(path);

        if (mappedPath == null) {
            return null;
        }

        /* Eclipse workspace root */
        final String workspaceRootPath = getWorkspaceRoot();

        /* Project name */
        final String projectName = getProjectName(path);

        /* We're okay if the mapped path is not beneath the workspace root */
        if (!LocalPath.isChild(workspaceRootPath, mappedPath)) {
            return null;
        }

        /* We're okay if the mapped path is what Eclipse would like it to be */
        if (LocalPath.equals(mappedPath, LocalPath.combine(workspaceRootPath, projectName))) {
            return null;
        }

        /*
         * If we're not directly under the workspace root, this isn't going to
         * work.
         */
        if (!LocalPath.equals(LocalPath.getParent(mappedPath), workspaceRootPath)) {
            return new ImportFolderValidation(
                ImportFolderValidationStatus.ERROR,
                MessageFormat.format(INVALID_PROJECT_DEPTH_MESSAGE, new Object[] {
                    projectName
            }));
        }

        return new ImportFolderValidation(
            ImportFolderValidationStatus.ERROR,
            MessageFormat.format(INVALID_PROJECT_NAME_MESSAGE, new Object[] {
                getProjectName(path),
                LocalPath.getLastComponent(mappedPath)
        }), ImportFolderValidationFlag.RECURSIVE);
    }

    private ImportFolderValidation computeAgainstExistingProjects(final String path) {
        /*
         * Determine the destination of this import
         */
        final String projectPath = getProjectPath(path);

        /*
         * Determine the project name of this import
         */
        final String projectName = getProjectName(path);

        /*
         * Examine this project w/r/t already existing projects. Make sure that
         * we're not trying to import something beneath an existing project.
         */
        final IProject[] existingProjects = importOptions.getEclipseWorkspace().getRoot().getProjects();

        for (int i = 0; i < existingProjects.length; i++) {
            if (existingProjects[i].getLocation() == null) {
                continue;
            }

            final String existingProjectLocation = existingProjects[i].getLocation().toOSString();

            /*
             * If this TFS project already exists in the workspace
             */
            if (LocalPath.equals(existingProjectLocation, projectPath) && getExistingMapping(path) != null) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.ALREADY_EXISTS,
                    MessageFormat.format(EXISTING_PROJECT_NAME_AND_LOCATION_MESSAGE, new Object[] {
                        existingProjects[i].getName()
                }), ImportFolderValidationFlag.RECURSIVE);
            }

            /*
             * If a project already exists in the workspace by this name
             */
            if (existingProjects[i].getName().equalsIgnoreCase(projectName)) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.ALREADY_EXISTS,
                    MessageFormat.format(EXISTING_PROJECT_NAME_MESSAGE, new Object[] {
                        existingProjects[i].getName()
                }));
            }

            /*
             * If a project already exists at the target location
             */
            if (LocalPath.equals(existingProjectLocation, projectPath)) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.ALREADY_EXISTS,
                    MessageFormat.format(EXISTING_PROJECT_LOCATION_MESSAGE, new Object[] {
                        existingProjects[i].getName(),
                        existingProjectLocation
                }), ImportFolderValidationFlag.RECURSIVE);
            }

            /*
             * If a project already exists beneath where this is intended to be
             * mapped.
             */
            if (LocalPath.isChild(projectPath, existingProjectLocation)) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.ALREADY_EXISTS,
                    MessageFormat.format(PARENT_OF_EXISTING_PROJECT_MESSAGE, new Object[] {
                        path,
                        existingProjects[i].getName()
                }));
            }

            /*
             * If this is the child of an already imported project.
             */
            if (LocalPath.isChild(existingProjectLocation, projectPath)) {
                return new ImportFolderValidation(
                    ImportFolderValidationStatus.ALREADY_EXISTS,
                    MessageFormat.format(CHILD_OF_EXISTING_PROJECT_MESSAGE, new Object[] {
                        path,
                        existingProjects[i].getName()
                }));
            }
        }

        return null;
    }
}
