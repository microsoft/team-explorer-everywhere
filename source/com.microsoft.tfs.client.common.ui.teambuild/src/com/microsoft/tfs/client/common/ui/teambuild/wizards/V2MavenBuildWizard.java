// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.commands.CreateV2ProjectFileCommand;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class V2MavenBuildWizard extends Wizard implements ICreateBuildConfigurationWizard {
    private IBuildDefinition buildDefinition;

    private MavenPomFileSelectionWizardPage selectionPage;
    private MavenBuildDependencyWizardPage mavenPage;

    @Override
    public void addPages() {
        selectionPage = new MavenPomFileSelectionWizardPage(buildDefinition);
        mavenPage = new MavenBuildDependencyWizardPage(buildDefinition);
        addPage(selectionPage);
        addPage(mavenPage);
    }

    @Override
    public boolean performFinish() {
        final String javaLocalPath = mavenPage.getJavaLocalPath();
        final String javaServerPath = mavenPage.getJavaServerPath();
        final String mavenServerPath = mavenPage.getMavenServerPath();
        final String mavenLocalPath = mavenPage.getMavenLocalPath();

        if (javaLocalPath != null && !LocalPath.isPathRooted(javaLocalPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2AntBuildWizard.InvalidJavaLocalPathTextFormat"), //$NON-NLS-1$
                    javaLocalPath));
            return false;
        }

        if (mavenLocalPath != null && !LocalPath.isPathRooted(mavenLocalPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2MavenBuildWizard.InvalidMavenLocalPathTextFormat"), //$NON-NLS-1$
                    mavenLocalPath));
            return false;
        }

        if (javaServerPath != null && !ServerPath.isServerPath(javaServerPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2AntBuildWizard.InvalidJavaServerPathTextFormat"), //$NON-NLS-1$
                    javaServerPath));
            return false;
        }

        if (mavenServerPath != null && !ServerPath.isServerPath(mavenServerPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2MavenBuildWizard.InvalidMavenServerPathTextFormat"), //$NON-NLS-1$
                    mavenServerPath));
            return false;
        }

        final CreateV2ProjectFileCommand command = new CreateV2ProjectFileCommand(
            buildDefinition,
            selectionPage.getBuildFileServerPath(),
            "/templates/maven/v2", //$NON-NLS-1$
            javaLocalPath,
            javaServerPath,
            mavenLocalPath,
            mavenServerPath);

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
