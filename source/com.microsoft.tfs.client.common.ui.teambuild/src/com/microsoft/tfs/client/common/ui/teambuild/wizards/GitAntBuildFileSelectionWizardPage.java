// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class GitAntBuildFileSelectionWizardPage extends LocalBuildFileSelectionPage {

    public GitAntBuildFileSelectionWizardPage(final IBuildDefinition buildDefinition) {
        super(
            "GitAntBuildFileSelectionPage", //$NON-NLS-1$
            buildDefinition,
            Messages.getString("AntBuildFileSelectionWizardPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("GitAntBuildFileSelectionWizardPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    public String getBuildFileLabel() {
        return Messages.getString("AntBuildFileSelectionWizardPage.BuildFileLabelText"); //$NON-NLS-1$
    }

}
