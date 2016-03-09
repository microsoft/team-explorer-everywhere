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

public class GitAntBuildWizard extends Wizard implements ICreateBuildConfigurationWizard {
    private IBuildDefinition buildDefinition;

    private GitAntBuildFileSelectionWizardPage buildFileSelectionPage;

    @Override
    public void addPages() {
        buildFileSelectionPage = new GitAntBuildFileSelectionWizardPage(buildDefinition);
        addPage(buildFileSelectionPage);
    }

    @Override
    public boolean performFinish() {
        final String repoPath = buildFileSelectionPage.getRepoPath();
        final String buildFilePath = buildFileSelectionPage.getBuildFileRelativePath();
        final CreateGitProjectFileCommand command =
            new CreateGitProjectFileCommand(buildDefinition, buildFilePath, repoPath, "/templates/ant/git/"); //$NON-NLS-1$

        final ICommandExecutor executor = UICommandExecutorFactory.newWizardCommandExecutor(getContainer());
        final IStatus status = executor.execute(command);

        return status.isOK();
    }

    @Override
    public void init(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
        setWindowTitle(Messages.getString("V2AntBuildWizard.WindowTitle")); //$NON-NLS-1$
        setDefaultPageImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSTeamBuildPlugin.PLUGIN_ID, "icons/ant_wiz.png")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

}
