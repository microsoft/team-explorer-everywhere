// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.eula;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.eula.EULAControl;
import com.microsoft.tfs.client.common.ui.controls.eula.EULAControl.EULAControlAcceptedEvent;
import com.microsoft.tfs.client.common.ui.controls.eula.EULAControl.EULAControlAcceptedListener;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;

public class WizardEULAPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "WizardEulaPage"; //$NON-NLS-1$

    public static final String EULA_ACCEPTED = "WizardEulaPage.EulaAccepted"; //$NON-NLS-1$

    private EULAControl eulaControl;

    public WizardEULAPage() {
        this(PAGE_NAME);
    }

    protected WizardEULAPage(final String pageName) {
        super(
            pageName,
            Messages.getString("WizardEulaPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("WizardEulaPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        /*
         * Create a non-size-constrained composite as our root control. This is
         * so that subclasses can add to this without size constraint.
         */
        final Composite rootContainer = new Composite(parent, SWT.NONE);
        setControl(rootContainer);

        final GridLayout rootLayout = new GridLayout(1, false);
        rootLayout.marginWidth = getHorizontalMargin();
        rootLayout.marginHeight = getVerticalMargin();
        rootLayout.horizontalSpacing = getHorizontalSpacing();
        rootLayout.verticalSpacing = getVerticalSpacing();
        rootContainer.setLayout(rootLayout);

        final SizeConstrainedComposite container = new SizeConstrainedComposite(rootContainer, SWT.NONE);
        GridDataBuilder.newInstance().grab().fill().applyTo(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        eulaControl = new EULAControl(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().applyTo(eulaControl);

        eulaControl.addAcceptedListener(new EULAControlAcceptedListener() {
            @Override
            public void eulaAccepted(final EULAControlAcceptedEvent event) {
                final boolean accepted = event.isAccepted();
                getExtendedWizard().setPageData(EULA_ACCEPTED, Boolean.valueOf(accepted));
                setPageComplete(accepted);
            }
        });

        setPageComplete(false);
    }

    @Override
    protected boolean onPageFinished() {
        /* Sanity check: Should never happen */
        if (!eulaControl.isAccepted()) {
            setErrorMessage(Messages.getString("WizardEulaPage.MustAcceptEula")); //$NON-NLS-1$
            return false;
        }

        getExtendedWizard().setPageData(EULA_ACCEPTED, Boolean.TRUE);

        /* Save to license manager */
        LicenseManager.getInstance().setEULAAccepted(true);
        LicenseManager.getInstance().write();

        return true;
    }

    @Override
    protected void refresh() {
        getExtendedWizard().removePageData(EULA_ACCEPTED);

        /* Unsave from license manager */
        LicenseManager.getInstance().setEULAAccepted(false);
        LicenseManager.getInstance().write();
    }
}
