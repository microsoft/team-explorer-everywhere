// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.eula;

import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;

public class EULAWizard extends AbstractEULAWizard {
    private final static ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public EULAWizard() {
        super(Messages.getString("EULAWizard.ConnectToTfs"), imageHelper.getImageDescriptor( //$NON-NLS-1$
            TFSCommonUIClientPlugin.PLUGIN_ID,
            "images/wizard/pageheader.png")); //$NON-NLS-1$

        addEULAPages();
        initEULAPages();
    }

    @Override
    public boolean enableNext(final IWizardPage page) {
        final IWizardPage nextPage = getNextEULAPage();

        /*
         * getNextLicensePage is sneaky - it does some strange enforcement of
         * the next page routine based on whether the product key page has been
         * visually displayed to the user. As such, we can end up getting asked
         * for our next page by the framework before the product key page has
         * even been displayed. Thus, it will return itself as the next page
         * even when it's the current page. Thus if it is the current page *and*
         * the next page, we veto the next button.
         */
        return (nextPage != null && nextPage != page);
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        return getNextEULAPage();
    }

    @Override
    protected boolean enableFinish(final IWizardPage currentPage) {
        return (getNextEULAPage() == null);
    }

    @Override
    protected boolean doPerformFinish() {
        return true;
    }
}
