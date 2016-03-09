// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.merge;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.FormHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;

public class MergeEndPage extends WizardPage {
    public static final String NAME = "MergeEndPage"; //$NON-NLS-1$

    /**
     * Create the wizard
     */
    public MergeEndPage() {
        super(NAME);
        setTitle(Messages.getString("MergeEndPage.PageTitle")); //$NON-NLS-1$
        setDescription(Messages.getString("MergeEndPage.PageDescription")); //$NON-NLS-1$
    }

    /**
     * Create contents of the wizard
     *
     * @param parent
     */
    @Override
    public void createControl(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NULL);

        final FormLayout formLayout = new FormLayout();
        formLayout.marginHeight = FormHelper.MarginHeight();
        formLayout.marginWidth = FormHelper.MarginWidth();
        container.setLayout(formLayout);

        setControl(container);

        final Label label = new Label(container, SWT.WRAP);
        final FormData labelData = new FormData();
        labelData.top = new FormAttachment(0, 0);
        labelData.left = new FormAttachment(0, 0);
        label.setLayoutData(labelData);
        label.setText(Messages.getString("MergeEndPage.StatusLabelText")); //$NON-NLS-1$
        ControlSize.setCharWidthHint(label, MergeWizard.TEXT_CHARACTER_WIDTH);

        final Label label2 = new Label(container, SWT.WRAP);
        final FormData label2Data = new FormData();
        label2Data.top = new FormAttachment(label, 10, SWT.BOTTOM);
        label2Data.left = new FormAttachment(0, 0);
        label2.setLayoutData(label2Data);
        label2.setText(Messages.getString("MergeEndPage.ExplainMergeProcess")); //$NON-NLS-1$
        ControlSize.setCharWidthHint(label2, MergeWizard.TEXT_CHARACTER_WIDTH);

        final Label label3 = new Label(container, SWT.WRAP);
        final FormData label3Data = new FormData();
        label3Data.top = new FormAttachment(label2, 10, SWT.BOTTOM);
        label3Data.left = new FormAttachment(0, 0);
        label3.setLayoutData(label3Data);
        label3.setText(Messages.getString("MergeEndPage.InformConflictResolution")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    @Override
    public void setVisible(final boolean visible) {
        if (visible) {
            ((MergeWizard) getWizard()).setComplete(true);
        }
        super.setVisible(visible);
    }
}
