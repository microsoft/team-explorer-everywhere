// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.eula;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;

import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizard;

/**
 * An abstract wizard that implements EULA and Product Key validation. Should be
 * extended by all wizards.
 *
 * @threadsafety unknown
 */
public abstract class AbstractEULAWizard extends ExtendedWizard {
    protected AbstractEULAWizard(final String windowTitle, final ImageDescriptor defaultPageImageDescriptor) {
        super(windowTitle, defaultPageImageDescriptor);
    }

    protected AbstractEULAWizard(
        final String windowTitle,
        final ImageDescriptor defaultPageImageDescriptor,
        final String dialogSettingsKey) {
        super(windowTitle, defaultPageImageDescriptor, dialogSettingsKey);
    }

    protected void addEULAPages() {
        addPage(new WizardEULAPage());
    }

    protected void initEULAPages() {
        /* Look for EULA and product id */
        final boolean eulaAccepted = LicenseManager.getInstance().isEULAAccepted();

        if (eulaAccepted) {
            setPageData(WizardEULAPage.EULA_ACCEPTED, Boolean.TRUE);
        }
    }

    protected IWizardPage getNextEULAPage() {
        if (!hasPageData(WizardEULAPage.EULA_ACCEPTED)) {
            return getPage(WizardEULAPage.PAGE_NAME);
        }

        return null;
    }
}
