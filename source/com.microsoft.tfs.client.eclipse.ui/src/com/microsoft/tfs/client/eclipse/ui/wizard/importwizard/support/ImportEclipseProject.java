// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.io.File;
import java.io.FileFilter;

import com.microsoft.tfs.client.common.git.EclipseProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;

public class ImportEclipseProject extends EclipseProjectInfo implements Comparable<ImportEclipseProject> {
    private final ImportGitRepository repository;
    private final ImportEclipseProject parentFolder;

    public ImportEclipseProject(final EclipseProjectInfo projectInfo, final ImportGitRepository repository) {
        super(projectInfo);
        this.repository = repository;
        this.parentFolder = null;
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$

    }

    public ImportEclipseProject(final EclipseProjectInfo projectInfo, final ImportEclipseProject parentFolder) {
        super(projectInfo);
        this.repository = null;
        this.parentFolder = parentFolder;
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
    }

    public ImportEclipseProject(final EclipseProjectInfo projectInfo) {
        super(projectInfo);
        this.repository = null;
        this.parentFolder = null;
        Check.notNull(projectInfo, "projectInfo"); //$NON-NLS-1$
    }

    public ImportGitRepository getRepository() {
        return repository;
    }

    public ImportEclipseProject getParentFolder() {
        return parentFolder;
    }

    public ImportEclipseProject[] getSubfolders() {
        if (!hasProjectDescription()) {
            final File[] subFolders = new File(getProjectPath()).listFiles(new FileFilter() {
                @Override
                public boolean accept(final File subFolder) {
                    return subFolder.isDirectory() && isValidProjectName(subFolder.getName());
                }
            });

            final ImportEclipseProject[] projects = new ImportEclipseProject[subFolders.length];
            for (int k = 0; k < subFolders.length; k++) {
                projects[k] = new ImportEclipseProject(
                    new EclipseProjectInfo(subFolders[k].getAbsolutePath(), getWorkspace()),
                    this);
            }

            return projects;
        } else {
            return new ImportEclipseProject[0];
        }
    }

    public String getFolderName() {
        return LocalPath.getFileName(getProjectPath());
    }

    @Override
    public int compareTo(final ImportEclipseProject o) {
        return getProjectPath().compareToIgnoreCase(o.getProjectPath());
    }

    public boolean isParentFolderSelected() {
        Check.isTrue(
            !hasProjectDescription() && getSelected(),
            "The folder has a project description or is not selected"); //$NON-NLS-1$

        ImportEclipseProject p = parentFolder;
        while (p != null) {
            if (p.getSelected()) {
                return true;
            }

            p = p.getParentFolder();
        }

        return false;
    }
}
