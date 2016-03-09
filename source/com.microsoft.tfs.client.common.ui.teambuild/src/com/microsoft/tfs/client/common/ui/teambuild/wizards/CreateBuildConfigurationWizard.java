// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

/**
 * The CreateBuildConfigurationWizard is responsible for allowing the user to
 * choose which (nested) build configuration wizard to run. The set of available
 * build configuration wizards comes from the CreateBuildConfigurationWizard
 * extension point.
 */
public class CreateBuildConfigurationWizard extends Wizard implements ICreateBuildConfigurationWizard {

    public static final String EXTENSION_POINT = "createBuildConfigurationWizards"; //$NON-NLS-1$
    private static final String EXTENSION_TAG = "wizard"; //$NON-NLS-1$

    protected CBCWizardSelectionPage mainPage;
    protected final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();
    protected IBuildDefinition buildDefinition;
    protected CreateBuildConfigurationWizardNode[] createBuildWizards;

    /**
     * Create the wizard pages
     */
    @Override
    public void addPages() {
        mainPage = new CBCWizardSelectionPage(createBuildWizards);
        addPage(mainPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        // if we're finishing from the main page then perform finish on the
        // selected wizard.
        if (getContainer().getCurrentPage() == mainPage) {
            if (mainPage.canFinishEarly()) {
                final IWizard wizard = mainPage.getSelectedNode().getWizard();
                wizard.setContainer(getContainer());
                return wizard.performFinish();
            }
        }
        return true;
    }

    /**
     * Lazily create the wizards pages
     */
    @Override
    public void init(final IBuildDefinition buildDefinition) {
        this.buildDefinition = buildDefinition;

        createBuildWizards = getCreateBuildConfigurationWizards();

        setWindowTitle(Messages.getString("CreateBuildConfigurationWizard.WindowTitle")); //$NON-NLS-1$
        setDefaultPageImageDescriptor(imageHelper.getImageDescriptor("icons/build_wiz.png")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
        setForcePreviousAndNextButtons(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.IWizard#canFinish()
     */
    @Override
    public boolean canFinish() {
        // we can finish if the first page is current and the the page can
        // finish early.
        if (getContainer().getCurrentPage() == mainPage) {
            return mainPage.canFinishEarly();
        }
        return super.canFinish();
    }

    public CreateBuildConfigurationWizardNode[] getCreateBuildConfigurationWizards() {
        final IExtensionPoint point =
            Platform.getExtensionRegistry().getExtensionPoint(TFSTeamBuildPlugin.PLUGIN_ID, EXTENSION_POINT);

        if (point == null) {
            return new CreateBuildConfigurationWizardNode[0];
        }

        final List<CreateBuildConfigurationWizardNode> wizardNodes =
            new ArrayList<CreateBuildConfigurationWizardNode>();

        final IExtension[] extensions = point.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] elements = extensions[i].getConfigurationElements();
            for (int j = 0; j < elements.length; j++) {
                if (elements[j].getName().equals(EXTENSION_TAG)) {
                    final CreateBuildConfigurationWizardNode descriptor =
                        new CreateBuildConfigurationWizardNode(elements[j], buildDefinition);
                    if (descriptor.isVersionSuitable(buildDefinition.getBuildServer().getBuildServerVersion())
                        && shouldAdd(descriptor)) {
                        wizardNodes.add(descriptor);
                    }
                }
            }
        }

        return wizardNodes.toArray(new CreateBuildConfigurationWizardNode[wizardNodes.size()]);
    }

    protected boolean shouldAdd(final CreateBuildConfigurationWizardNode descriptor) {
        return !descriptor.isGitNode();
    }
}
