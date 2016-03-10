// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.connecterror;

import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.wizard.common.WizardCrossCollectionSelectionPage;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.project.ProjectManagerUIDataProvider;
import com.microsoft.tfs.client.eclipse.ui.wizard.connectwizard.EclipseConnectWizard;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

/**
 * Provides handling for connection errors on project startup for
 * {@link ProjectManagerUIDataProvider}.
 *
 * Note that this does NOT extend {@link EclipseConnectWizard}:
 * {@link EclipseConnectWizard} provides connection busy handling support. This
 * wizard is always used when making a connection, thus connections are (by
 * definition) busy.
 */
public class EclipseConnectErrorWizard extends ConnectWizard {
    private final static ImageHelper imageHelper = new ImageHelper(TFSEclipseClientUIPlugin.PLUGIN_ID);

    public EclipseConnectErrorWizard() {
        super(
            Messages.getString("EclipseConnectErrorWizard.ErrorConnectingDialogTitle"), //$NON-NLS-1$
            imageHelper.getImageDescriptor(TFSCommonUIClientPlugin.PLUGIN_ID, "images/wizard/pageheader.png"), //$NON-NLS-1$
            SourceControlCapabilityFlags.GIT_TFS);

        addConnectionPages();
        initConnectionPages();
    }

    public void setErrorMessage(final String errorMessage) {
        setPageData(WizardCrossCollectionSelectionPage.PROJECT_COLLECTION_ERROR_MESSAGE, errorMessage);
    }

    @Override
    public boolean enableNext(final IWizardPage currentPage) {
        return true;
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        return getNextConnectionPage();
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        return false;
    }

    @Override
    protected boolean doPerformFinish() {
        finishConnection();

        return true;
    }
}
