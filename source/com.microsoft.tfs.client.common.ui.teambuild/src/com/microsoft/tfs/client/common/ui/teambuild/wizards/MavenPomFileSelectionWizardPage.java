// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class MavenPomFileSelectionWizardPage extends FileSelectionWizardPage {

    public MavenPomFileSelectionWizardPage(final IBuildDefinition buildDefinition) {
        super(
            "buildFileSelectionPage", //$NON-NLS-1$
            buildDefinition,
            Messages.getString("MavenPomFileSelectionWizardPage.PageName"), //$NON-NLS-1$
            Messages.getString("MavenPomFileSelectionWizardPage.PageDescription")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.common.ui.teambuild.wizards.
     * FileSelectionWizardPage #getBuildFileLabel ()
     */
    @Override
    public String getBuildFileLabel() {
        return Messages.getString("MavenPomFileSelectionWizardPage.BuildFileLabelText"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.teambuild.wizards.
     * FileSelectionWizardPage# getDefaultBuildFileName()
     */
    @Override
    public String getDefaultBuildFileName() {
        return "pom.xml"; //$NON-NLS-1$
    }

}
