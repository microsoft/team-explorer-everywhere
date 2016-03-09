// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class AntBuildFileSelectionWizardPage extends FileSelectionWizardPage {

    public AntBuildFileSelectionWizardPage(final IBuildDefinition buildDefinition) {
        super(
            "buildFileSelectionPage", //$NON-NLS-1$
            buildDefinition,
            Messages.getString("AntBuildFileSelectionWizardPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("AntBuildFileSelectionWizardPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    public String getDefaultBuildFileName() {
        return "build.xml"; //$NON-NLS-1$
    }

    @Override
    public String getBuildFileLabel() {
        return Messages.getString("AntBuildFileSelectionWizardPage.BuildFileLabelText"); //$NON-NLS-1$
    }

}
