// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;

import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.git.EclipseProjectInfo;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.util.Check;

public class FindEclipseProjectsCommand extends TFSCommand {
    final List<File> folders = new ArrayList<File>(1024);
    final List<EclipseProjectInfo> projects = new ArrayList<EclipseProjectInfo>();
    final Set<String> visitedFolders;
    final IWorkspace workspace;
    final boolean searchNested;

    final String subTaskNameFormat = Messages.getString("FindEclipseProjectsCommand.SubtaskNameFormat"); //$NON-NLS-1$
    public static final String METADATA_FOLDER = ".metadata"; //$NON-NLS-1$

    final FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(final File file) {
            return file.isFile() && file.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME);
        }
    };

    final FileFilter folderFilter = new FileFilter() {
        @Override
        public boolean accept(final File folder) {
            if (!folder.isDirectory()) {
                return false;
            }

            final String path = folder.getName();

            if (path.equals(METADATA_FOLDER)) {
                return false;
            }

            if (!path.equals(Constants.DOT_GIT)) {
                return true;
            }

            return !FileKey.isGitRepository(folder, FS.DETECTED);
        }
    };

    public FindEclipseProjectsCommand(
        final List<File> parentFolders,
        final IWorkspace workspace,
        final boolean searchNested) {
        Check.notNull(parentFolders, "parentFolders"); //$NON-NLS-1$
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.folders.addAll(parentFolders);
        this.visitedFolders = new HashSet<String>();
        this.workspace = workspace;
        this.searchNested = searchNested;
    }

    @Override
    public String getName() {
        final String errorMessageFormat = Messages.getString("FindEclipseProjectsCommand.CommandNameFormat"); //$NON-NLS-1$
        return MessageFormat.format(errorMessageFormat, (folders.size() == 0) ? "" : folders.get(0).getPath()); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        final String errorMessageFormat = Messages.getString("FindEclipseProjectsCommand.CommandErrorFormat"); //$NON-NLS-1$
        return MessageFormat.format(errorMessageFormat, (folders.size() == 0) ? "" : folders.get(0).getPath()); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        try {
            for (int i = 0; i < folders.size(); i++) {
                if (progressMonitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                final File folder = folders.get(i);
                progressMonitor.subTask(folder.getPath());

                if (folder.getName().equals(Constants.DOT_GIT) && FileKey.isGitRepository(folder, FS.DETECTED)) {
                    continue;
                }

                final String canonicalFolderPath = folder.getCanonicalPath();
                if (visitedFolders.contains(canonicalFolderPath)) {
                    continue;
                } else {
                    visitedFolders.add(canonicalFolderPath);
                }

                progressMonitor.subTask(MessageFormat.format(subTaskNameFormat, folder.getName()));

                final File[] files = folder.listFiles();
                if (files == null || files.length == 0) {
                    continue;
                }

                /*
                 * Let's check first files for a project description file.
                 */
                boolean found = false;

                for (final File file : files) {
                    if (fileFilter.accept(file)) {
                        projects.add(new EclipseProjectInfo(file, workspace));
                        found = true;
                        break;
                    }
                }

                /*
                 * If we've found one, we skip the nested folders unless deep
                 * search has been requested.
                 */
                if (found && !searchNested) {
                    continue;
                }

                /*
                 * We haven't found a project description file among the direct
                 * children of the folder or nested projects search has been
                 * requested. Let's look into nested sub-folders.
                 */
                for (final File file : files) {
                    if (folderFilter.accept(file) && !visitedFolders.contains(file.getCanonicalPath())) {
                        folders.add(file);
                    }
                }
            }
        } catch (final IOException e) {
            return new Status(IStatus.ERROR, TFSCommonClientPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e);
        }

        return Status.OK_STATUS;
    }

    public List<EclipseProjectInfo> getProjects() {
        return projects;
    }
}
