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

public class V2AntBuildWizard extends Wizard implements ICreateBuildConfigurationWizard {
    private IBuildDefinition buildDefinition;

    private AntBuildFileSelectionWizardPage buildFileSelectionPage;

    private AntBuildDependencyWizardPage javaAntSettingPage;

    @Override
    public void addPages() {
        buildFileSelectionPage = new AntBuildFileSelectionWizardPage(buildDefinition);
        javaAntSettingPage = new AntBuildDependencyWizardPage(buildDefinition);
        addPage(buildFileSelectionPage);
        addPage(javaAntSettingPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        final String javaLocalPath = javaAntSettingPage.getJavaLocalPath();
        final String javaServerPath = javaAntSettingPage.getJavaServerPath();
        final String antLocalPath = javaAntSettingPage.getAntLocalPath();
        final String antServerPath = javaAntSettingPage.getAntServerPath();

        if (javaLocalPath != null && !LocalPath.isPathRooted(javaLocalPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2AntBuildWizard.InvalidJavaLocalPathTextFormat"), //$NON-NLS-1$
                    javaLocalPath));
            return false;
        }

        if (antLocalPath != null && !LocalPath.isPathRooted(antLocalPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2AntBuildWizard.InvalidAntLocalPathTextFormat"), //$NON-NLS-1$
                    antLocalPath));
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

        if (antServerPath != null && !ServerPath.isServerPath(antServerPath)) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("V2AntBuildWizard.InvalidPathMessageTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("V2AntBuildWizard.InvalidAntServerPathTextFormat"), //$NON-NLS-1$
                    antServerPath));
            return false;
        }

        final CreateV2ProjectFileCommand command = new CreateV2ProjectFileCommand(
            buildDefinition,
            buildFileSelectionPage.getBuildFileServerPath(),
            "/templates/ant/v2", //$NON-NLS-1$
            javaLocalPath,
            javaServerPath,
            antLocalPath,
            antServerPath);

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
