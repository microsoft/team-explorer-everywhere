// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard;

import com.microsoft.tfs.client.common.ui.wizard.common.WizardWorkspacePage;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;

public class ImportWizardWorkspacePage extends WizardWorkspacePage {
    public static final String PAGE_NAME = "ImportWizardWorkspacePage"; //$NON-NLS-1$

    public ImportWizardWorkspacePage() {
        super(
            PAGE_NAME,
            Messages.getString("ImportWizardWorkspacePage.PageTitle"), //$NON-NLS-1$
            Messages.getString("ImportWizardWorkspacePage.PageDescription")); //$NON-NLS-1$

        setText(Messages.getString("ImportWizardWorkspacePage.PageText")); //$NON-NLS-1$
    }

    @Override
    protected void refresh() {
        super.refresh();

        final ImportOptions options = (ImportOptions) getExtendedWizard().getPageData(ImportOptions.class);
        options.setTFSWorkspace(null);
    }

    @Override
    protected boolean onPageFinished() {
        final ImportOptions options = (ImportOptions) getExtendedWizard().getPageData(ImportOptions.class);
        options.setTFSWorkspace(getSelectedWorkspace());

        return super.onPageFinished();
    }
}
