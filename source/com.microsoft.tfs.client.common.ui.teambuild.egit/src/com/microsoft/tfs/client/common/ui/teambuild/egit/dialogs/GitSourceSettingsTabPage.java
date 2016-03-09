// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.egit.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition.BuildDefinitionTabPage;
import com.microsoft.tfs.client.common.ui.teambuild.egit.Messages;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSourceProvider;

public class GitSourceSettingsTabPage extends BuildDefinitionTabPage {
    private GitSourceSettingsControl control;

    public GitSourceSettingsTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    @Override
    public Control createControl(final Composite parent) {
        control = new GitSourceSettingsControl(parent, SWT.NONE, getBuildDefinition());
        return control;
    }

    @Override
    public String getName() {
        return Messages.getString("GitSourceSettingsTabPage.TabLabelText"); //$NON-NLS-1$
    }

    @Override
    public boolean isValid() {
        if (control == null) {
            return false;
        }

        return control.getSelectedRepo() != null;
    }

    public GitSourceSettingsControl getControl() {
        return control;
    }

    public String getUniqueRepoName() {
        return control.getUniqueRepoName();
    }

    public String getRepo() {
        return control.getSelectedRepo();
    }

    public String getShortBranchName() {
        return GitProperties.getShortBranchName(control.getSelectedBranch());
    }

    public String getFullBranchName() {
        if (control == null) {
            return null;
        }
        return control.getSelectedBranch();
    }

    public String getCIBranchesSpec() {
        final String[] branches = control.getSelectedCIBranches();

        return GitProperties.joinBranches(branches);
    }

    public String getRepoPath() {
        if (control == null) {
            return null;
        }
        return control.getRepoPath();
    }

    public void updateSourceProvider(final IBuildDefinition buildDefinition) {
        final IBuildDefinitionSourceProvider sourceProvider = buildDefinition.getDefaultSourceProvider();
        sourceProvider.setNameValueField(GitProperties.RepositoryName, getUniqueRepoName());
        sourceProvider.setNameValueField(GitProperties.DefaultBranch, getFullBranchName());
        sourceProvider.setNameValueField(GitProperties.CIBranches, getCIBranchesSpec());
        sourceProvider.setNameValueField(GitProperties.LocalRepoPath, getRepoPath());

        // Keep the Repository Url for backward compatibility
        final String repositoryUrl = GitProperties.createGitRepositoryUrl(
            buildDefinition.getBuildServer().getConnection().toString(),
            buildDefinition.getTeamProject(),
            getRepo());
        sourceProvider.setNameValueField(GitProperties.RepositoryUrl, repositoryUrl);
        sourceProvider.prepareToSave();
    }
}
