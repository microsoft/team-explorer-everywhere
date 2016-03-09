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

public class SelectMergeTargetMappingWizardPage extends WizardPage {
    public static final String NAME = "SelectMergeTargetMappingWizardPage"; //$NON-NLS-1$

    public SelectMergeTargetMappingWizardPage() {
        super(NAME);
        setTitle(Messages.getString("SelectMergeTargetMappingWizardPage.PageTitle")); //$NON-NLS-1$
        setDescription(Messages.getString("SelectMergeTargetMappingWizardPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NULL);

        final FormLayout formLayout = new FormLayout();
        formLayout.marginHeight = FormHelper.MarginHeight();
        formLayout.marginWidth = FormHelper.MarginWidth();
        formLayout.spacing = FormHelper.Spacing();
        container.setLayout(formLayout);

        setControl(container);

        final Label errorLabel = new Label(container, SWT.WRAP);
        final FormData errorLabelData = new FormData();
        errorLabelData.top = new FormAttachment(0, 0);
        errorLabelData.left = new FormAttachment(0, 0);
        errorLabel.setLayoutData(errorLabelData);
        errorLabel.setText(Messages.getString("SelectMergeTargetMappingWizardPage.ErrorLabelText")); //$NON-NLS-1$
        ControlSize.setCharWidthHint(errorLabel, MergeWizard.TEXT_CHARACTER_WIDTH);
    }

    @Override
    public boolean canFlipToNextPage() {
        return false;
    }
}
