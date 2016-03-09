// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sync;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.BaseWizardPage;
import com.microsoft.tfs.client.eclipse.ui.Messages;

public class SynchronizeWizardErrorPage extends BaseWizardPage {
    public static final SynchronizeWizardErrorPageData CONNECTING =
        new SynchronizeWizardErrorPageData(
            Messages.getString("SynchronizeWizardErrorPage.ConnectingShortMessage"), //$NON-NLS-1$
            Messages.getString("SynchronizeWizardErrorPage.ConnectingLongMessage")); //$NON-NLS-1$

    public static final SynchronizeWizardErrorPageData OFFLINE =
        new SynchronizeWizardErrorPageData(
            Messages.getString("SynchronizeWizardErrorPage.WorkingOfflineShortMessage"), //$NON-NLS-1$
            Messages.getString("SynchronizeWizardErrorPage.WorkingOfflineLongMessage")); //$NON-NLS-1$

    private final SynchronizeWizardErrorPageData data;

    private static final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public SynchronizeWizardErrorPage(final SynchronizeWizardErrorPageData data) {
        super(
            Messages.getString("SynchronizeWizardErrorPage.PageTitle"), //$NON-NLS-1$
            data.getShortMessage(),
            imageHelper.getImageDescriptor("images/wizard/pageheader.png")); //$NON-NLS-1$

        this.data = data;

        setPageComplete(false);
    }

    @Override
    public void createControl(final Composite parent) {
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

        final Label errorText = new Label(container, SWT.WRAP | SWT.READ_ONLY);
        errorText.setText(data.getMessage());
        GridDataBuilder.newInstance().hGrab().applyTo(errorText);
    }

    private static final class SynchronizeWizardErrorPageData {
        private final String shortMessage;
        private final String message;

        public SynchronizeWizardErrorPageData(final String shortMessage, final String message) {
            this.shortMessage = shortMessage;
            this.message = message;
        }

        public String getShortMessage() {
            return shortMessage;
        }

        public String getMessage() {
            return message;
        }
    }
}
