// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.git;

import java.io.File;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class EclipseProjectInfo {
    private final String projectDescriptionFilePath;
    private final String projectFolderPath;
    private final IWorkspace workspace;

    private String projectName;
    private IProjectDescription description;

    private boolean selected = false;
    private boolean damaged = false;

    private String validationMessage;

    public EclipseProjectInfo(final File projectDescriptionFile, final IWorkspace workspace) {
        Check.notNull(projectDescriptionFile, "projectDescriptionFile"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.projectDescriptionFilePath = LocalPath.canonicalize(projectDescriptionFile.getPath());
        this.projectFolderPath = LocalPath.getParent(projectDescriptionFilePath);
        this.workspace = workspace;

        if (projectDescriptionFile.exists()) {

            if (isTopLevelFolder()) {
                // if the project is inside of the Eclipse workspace use its
                // folder
                // name as the project name
                projectName = LocalPath.getFileName(projectFolderPath);
                description = workspace.newProjectDescription(projectName);
            } else {
                try {
                    final IPath path = new Path(projectDescriptionFilePath);
                    description = workspace.loadProjectDescription(path);
                    projectName = description.getName();
                } catch (final CoreException e) {
                    description = null;
                    damaged = true;
                    projectName = LocalPath.getFileName(projectFolderPath);
                }
            }
        } else {
            this.description = null;
            this.projectName = LocalPath.getFileName(this.projectFolderPath);
        }
    }

    public EclipseProjectInfo(final String folderPath, final IWorkspace workspace) {
        this(new File(folderPath, ".project"), workspace); //$NON-NLS-1$
    }

    protected EclipseProjectInfo(final EclipseProjectInfo projectInfo) {
        this.projectDescriptionFilePath = projectInfo.getProjectDescriptionFilePath();
        this.projectFolderPath = projectInfo.getProjectFolderPath();
        this.projectName = projectInfo.getProjectName();
        this.workspace = projectInfo.getWorkspace();
        this.description = projectInfo.getProjectDescription();
    }

    private String getEclipseWorkspacePath() {
        return workspace.getRoot().getLocation().toOSString();
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(final String newProjectName) {
        projectName = newProjectName;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(final boolean newSelected) {
        selected = newSelected;
    }

    public String getProjectPath() {
        return projectFolderPath;
    }

    public IProjectDescription getProjectDescription() {
        return description;
    }

    public boolean hasProjectDescription() {
        return description != null;
    }

    public boolean isDamaged() {
        return damaged;
    }

    private String getProjectDescriptionFilePath() {
        return projectDescriptionFilePath;
    }

    private String getProjectFolderPath() {
        return projectFolderPath;
    }

    protected IWorkspace getWorkspace() {
        return workspace;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public boolean isTopLevelFolder() {
        return LocalPath.isDirectChild(getEclipseWorkspacePath(), projectFolderPath);
    }

    public boolean isValidProjectName(final String name) {
        final IStatus status = validateProjectName(workspace, name);

        if (status.isOK()) {
            validationMessage = StringUtil.EMPTY;
            return true;
        } else {
            validationMessage = status.getMessage();
            return false;
        }
    }

    public static IStatus validateProjectName(final IWorkspace workspace, final String name) {
        final IStatus status = workspace.validateName(name, IResource.PROJECT);

        if (!status.isOK()) {
            return status;
        }

        if (name.startsWith(".")) //$NON-NLS-1$
        {
            return new Status(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                Messages.getString("EclipseProjectInfo.NameStartsWithDotError")); //$NON-NLS-1$
        }

        return Status.OK_STATUS;
    }
}
