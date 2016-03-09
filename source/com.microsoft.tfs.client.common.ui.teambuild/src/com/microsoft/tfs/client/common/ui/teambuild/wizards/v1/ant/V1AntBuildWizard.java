// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards.v1.ant;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.commands.CreateV1ProjectFileCommand;
import com.microsoft.tfs.client.common.ui.teambuild.wizards.AntBuildFileSelectionWizardPage;
import com.microsoft.tfs.client.common.ui.teambuild.wizards.ICreateBuildConfigurationWizard;
import com.microsoft.tfs.client.common.ui.teambuild.wizards.v1.LocationWizardPage;
import com.microsoft.tfs.client.common.ui.teambuild.wizards.v1.WorkspaceWizardPage;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

/**
 * A Wizard to create a TFS 2005 style build definition.
 */
public class V1AntBuildWizard extends Wizard implements ICreateBuildConfigurationWizard {
    private IBuildDefinition buildDefinition;

    private V1AntWelcomeWizardPage welcomePage;
    private WorkspaceWizardPage workspacePage;
    private AntBuildFileSelectionWizardPage buildFileSelectionPage;
    private LocationWizardPage locationPage;

    @Override
    public void init(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;
        setWindowTitle(Messages.getString("V1AntBuildWizard.WizardTitle")); //$NON-NLS-1$
        setDefaultPageImageDescriptor(
            AbstractUIPlugin.imageDescriptorFromPlugin(TFSTeamBuildPlugin.PLUGIN_ID, "icons/ant_wiz.png")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);

        // setup default workspace.
        if (buildDefinition.getWorkspace().getMappings().length == 0) {
            buildDefinition.getWorkspace().map(
                ServerPath.ROOT + buildDefinition.getTeamProject(),
                BuildConstants.SOURCE_DIR_ENVIRONMENT_VARIABLE);
        }

    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        final IWizardPage nextPage = super.getNextPage(page);

        if (nextPage == null) {
            return nextPage;
        }

        if (nextPage.equals(buildFileSelectionPage)) {
            buildFileSelectionPage.setDefaultServerItem(workspacePage.getWorkspaceTemplate());
        }

        return nextPage;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    @Override
    public void addPages() {
        welcomePage = new V1AntWelcomeWizardPage(buildDefinition);
        addPage(welcomePage);
        workspacePage = new WorkspaceWizardPage(buildDefinition);
        addPage(workspacePage);
        buildFileSelectionPage = new AntBuildFileSelectionWizardPage(buildDefinition);
        addPage(buildFileSelectionPage);
        locationPage = new LocationWizardPage();
        addPage(locationPage);
    }

    @Override
    public boolean performFinish() {
        buildDefinition.setName(welcomePage.getBuildDefinitionName());
        buildDefinition.setDescription(welcomePage.getBuildDefinitionDescription());

        final CreateV1ProjectFileCommand command = new CreateV1ProjectFileCommand(
            buildDefinition,
            buildDefinition.getTeamProject(),
            buildDefinition.getName(),
            buildDefinition.getDescription(),
            workspacePage.getWorkspaceTemplate(),
            buildFileSelectionPage.getBuildFileServerPath(),
            locationPage.getBuildAgent(),
            locationPage.getBuildDirectory(),
            locationPage.getDropLocation());

        final ICommandExecutor executor = UICommandExecutorFactory.newWizardCommandExecutor(getContainer());
        final IStatus status = executor.execute(command);

        return status.isOK();
    }

}
