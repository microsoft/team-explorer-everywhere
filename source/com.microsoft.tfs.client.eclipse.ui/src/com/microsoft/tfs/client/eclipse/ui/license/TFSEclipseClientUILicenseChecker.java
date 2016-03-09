// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.license;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.wizard.eula.EULAWizard;
import com.microsoft.tfs.client.eclipse.license.TFSEclipseClientLicenseChecker;

public final class TFSEclipseClientUILicenseChecker extends TFSEclipseClientLicenseChecker {
    @Override
    public boolean isLicensed() {
        final Display display = Display.getDefault();

        if (display == null) {
            return super.isLicensed();
        }

        UIHelpers.runOnUIThread(false, new Runnable() {
            @Override
            public void run() {
                if (TFSEclipseClientUILicenseChecker.needsPrompt()) {
                    final EULAWizard eulaWizard = new EULAWizard();
                    final WizardDialog eulaWizardDialog = new WizardDialog(ShellUtils.getWorkbenchShell(), eulaWizard);

                    eulaWizardDialog.open();
                }
            }
        });

        /* Retest EULA and product key */
        return super.isLicensed();
    }

    private static boolean needsPrompt() {
        return (!LicenseManager.getInstance().isEULAAccepted());
    }
}
