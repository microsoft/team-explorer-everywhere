// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.VersionControlHelper;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.exceptions.ConfigurationFolderPathNotFoundException;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildDefinition;

/**
 * The project file tab page for TfVc
 */
public class ProjectFileTabPage extends BuildDefinitionTabPage {

    protected ProjectFileControl control = null;

    public ProjectFileTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #createControl(org .eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createControl(final Composite parent) {
        if (control == null) {
            control = new TfsProjectFileControl(parent, SWT.NONE);
            populateControl();
        }
        return control;
    }

    @SuppressWarnings("restriction")
    private void populateControl() {
        String configPath = null;
        try {
            configPath = ((BuildDefinition) getBuildDefinition()).getConfigurationFolderPath();
        } catch (final ConfigurationFolderPathNotFoundException e) {
            configPath = VersionControlHelper.calculateDefaultBuildFileLocation(
                getBuildDefinition().getTeamProject(),
                getBuildDefinition().getName());
        }

        if (configPath != null) {
            getControl().getConfigFolderText().setText(configPath);
        }

        getControl().validate();
    }

    public ProjectFileControl getControl() {
        return control;
    }

    public String getConfigFolderText() {
        return control.getConfigFolderText().getText().trim();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #getName()
     */
    @Override
    public String getName() {
        return Messages.getString("ProjectFileTabPage.TabLabelText"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #isValid()
     */
    @Override
    public boolean isValid() {
        return !control.getConfigFolderText().isEnabled()
            || (control.getConfigFolderText().getText().length() > 0 && control.getProjectFileExists());
    }

    public void updateConfigurationFolderPath(final IBuildDefinition buildDefinition, final String path) {
        buildDefinition.setConfigurationFolderPath(path);
    }
}
