// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.commands.CreateGitProjectFileCommand;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class GitMavenBuildWizard extends Wizard implements ICreateBuildConfigurationWizard {
    private IBuildDefinition buildDefinition;

    private GitMavenPomFileSelectionWizardPage selectionPage;

    @Override
    public void addPages() {
        selectionPage = new GitMavenPomFileSelectionWizardPage(buildDefinition);
        addPage(selectionPage);
    }

    @Override
    public boolean performFinish() {
        final String repoPath = selectionPage.getRepoPath();
        final String buildFilePath = selectionPage.getBuildFileRelativePath();
        final CreateGitProjectFileCommand command =
            new CreateGitProjectFileCommand(buildDefinition, buildFilePath, repoPath, "/templates/maven/git/"); //$NON-NLS-1$

        final ICommandExecutor executor = UICommandExecutorFactory.newWizardCommandExecutor(getContainer());
        final IStatus status = executor.execute(command);

        return status.isOK();
    }

    @Override
    public void init(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
        setWindowTitle(Messages.getString("V2MavenBuildWizard.WindowTitle")); //$NON-NLS-1$
        setDefaultPageImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSTeamBuildPlugin.PLUGIN_ID, "icons/build_wiz.png")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

}
