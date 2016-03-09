// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.commands;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public class CreateGitProjectFileCommand extends CreateV2ProjectFileCommand {

    private final String repoPath;

    public CreateGitProjectFileCommand(
        final IBuildDefinition buildDefinition,
        final String buildFilePath,
        final String repoPath,
        final String templateLocationRoot) {
        super(buildDefinition, buildFilePath, templateLocationRoot);
        this.repoPath = repoPath;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String configPath = getBuildDefinition().getConfigurationFolderPath();
        final String definitionName = getBuildDefinition().getName();

        final String messageFormat = Messages.getString("CreateV2ProjectFileCommand.ProgressMonitorTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, definitionName);
        progressMonitor.beginTask(message, IProgressMonitor.UNKNOWN);

        final String projFileFolder = LocalPath.combine(repoPath, GitProperties.gitUriToLocalRelativePath(configPath));

        (new File(projFileFolder)).mkdirs();
        writeProjectFile(projFileFolder);
        writeResponseFile(projFileFolder);

        progressMonitor.done();
        return Status.OK_STATUS;
    }
}
