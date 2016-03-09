// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.connectwizard;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.ui.Messages;

public class WizardConnectionBusyPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "WizardConnectionBusyPage"; //$NON-NLS-1$

    public static final String CONNECTION_AVAILABLE = "repositoryConnectionAvailable"; //$NON-NLS-1$

    public WizardConnectionBusyPage() {
        super(
            PAGE_NAME,
            Messages.getString("WizardConnectionBusyPage.InProgress"), //$NON-NLS-1$
            Messages.getString("WizardConnectionBusyPage.InProgress")); //$NON-NLS-1$

        setErrorMessage(Messages.getString("WizardConnectionBusyPage.InProgress")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final SizeConstrainedComposite container = new SizeConstrainedComposite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        /*
         * Set the growing containers default size to the wizard size. This will
         * encourage the multiline text control to wrap (which it tends not to
         * do, it will only wrap at your display's width.)
         */
        container.setDefaultSize(getShell().getSize().x, SWT.DEFAULT);

        String actionText = null;

        if (getExtendedWizard() instanceof EclipseConnectWizard) {
            actionText = ((EclipseConnectWizard) getExtendedWizard()).getActionText();
        }

        if (actionText == null) {
            actionText = Messages.getString("WizardConnectionBusyPage.ActionText"); //$NON-NLS-1$
        }

        final Label errorText = new Label(container, SWT.WRAP | SWT.READ_ONLY);

        final String messageFormat = Messages.getString("WizardConnectionBusyPage.WaitForConnectErrorLabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, actionText);
        errorText.setText(message);

        GridDataBuilder.newInstance().hGrab().applyTo(errorText);
    }

    @Override
    protected void onMovingToPreviousPage() {
        refresh();
    }

    @Override
    protected void refresh() {
        final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

        if (projectManager.isConnecting()) {
            getExtendedWizard().removePageData(CONNECTION_AVAILABLE);
        } else {
            getExtendedWizard().setPageData(CONNECTION_AVAILABLE, Boolean.TRUE);
        }
    }
}
