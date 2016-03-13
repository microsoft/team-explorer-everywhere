// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.ProjectFileTabPage;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.exceptions.ConfigurationFolderPathNotFoundException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.StringUtil;

public class GitProjectFileTabPage extends ProjectFileTabPage {
    public GitProjectFileTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    @Override
    public Control createControl(final Composite parent) {
        if (control == null) {
            control = new GitProjectFileControl(parent, SWT.NONE);
            populateControl();
        }
        return control;
    }

    private void populateControl() {
        String configPath = null;
        try {
            configPath = getBuildDefinition().getConfigurationFolderPath();
        } catch (final ConfigurationFolderPathNotFoundException e) {
            // default to the team project root
            if (!StringUtil.isNullOrEmpty(getBuildDefinition().getTeamProject())) {
                configPath = GitProperties.GitPathBeginning
                    + getBuildDefinition().getTeamProject()
                    + ServerPath.PREFERRED_SEPARATOR_CHARACTER
                    + getBuildDefinition().getTeamProject()
                    + ServerPath.PREFERRED_SEPARATOR_CHARACTER
                    + "master"; //$NON-NLS-1$

                if (!StringUtil.isNullOrEmpty(getBuildDefinition().getName())) {
                    configPath += ServerPath.PREFERRED_SEPARATOR_CHARACTER + getBuildDefinition().getName();
                }
            }
        }

        if (!StringUtil.isNullOrEmpty(configPath)) {
            getControl().getConfigFolderText().setText(configPath);
        }

        getControl().validate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GitProjectFileControl getControl() {
        return (GitProjectFileControl) super.getControl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return super.isValid() || getControl().getLocalCopyExists();
    }
}
