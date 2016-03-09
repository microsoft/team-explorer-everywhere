// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class GitMavenPomFileSelectionWizardPage extends LocalBuildFileSelectionPage {

    public GitMavenPomFileSelectionWizardPage(final IBuildDefinition buildDefinition) {
        super(
            "GitMavenPomFileSelectionWizardPage", //$NON-NLS-1$
            buildDefinition,
            Messages.getString("MavenPomFileSelectionWizardPage.PageName"), //$NON-NLS-1$
            Messages.getString("GitMavenPomFileSelectionWizardPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    public String getBuildFileLabel() {
        return Messages.getString("MavenPomFileSelectionWizardPage.BuildFileLabelText"); //$NON-NLS-1$
    }
}
