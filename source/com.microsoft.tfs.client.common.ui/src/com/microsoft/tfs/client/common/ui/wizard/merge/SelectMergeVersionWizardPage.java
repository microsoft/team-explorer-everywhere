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
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl;
import com.microsoft.tfs.client.common.ui.framework.helper.FormHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public class SelectMergeVersionWizardPage extends WizardPage {

    public static final String NAME = "SelectMergeVersionWizardPage"; //$NON-NLS-1$

    private VersionPickerControl version;

    /**
     * Create the wizard
     */
    public SelectMergeVersionWizardPage() {
        super(NAME);
        setTitle(Messages.getString("SelectMergeVersionWizardPage.PageTitle")); //$NON-NLS-1$
        setDescription(Messages.getString("SelectMergeVersionWizardPage.PageDescription")); //$NON-NLS-1$
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
        formLayout.spacing = FormHelper.Spacing();
        container.setLayout(formLayout);

        setControl(container);

        final Label specifyTheVersionLabel = new Label(container, SWT.WRAP);
        final FormData specifyTheVersionLabelData = new FormData();
        specifyTheVersionLabelData.top = new FormAttachment(0, 0);
        specifyTheVersionLabelData.left = new FormAttachment(0, 0);
        specifyTheVersionLabel.setLayoutData(specifyTheVersionLabelData);
        specifyTheVersionLabel.setText(Messages.getString("SelectMergeVersionWizardPage.ExplainMergeProcess")); //$NON-NLS-1$
        ControlSize.setCharWidthHint(specifyTheVersionLabel, MergeWizard.TEXT_CHARACTER_WIDTH);

        version = new VersionPickerControl(container, SWT.NONE);
        version.setRepository(((MergeWizard) getWizard()).getRepository());
        version.setText(Messages.getString("SelectMergeVersionWizardPage.VersionTypeLabelText")); //$NON-NLS-1$

        final FormData versionData = new FormData();
        versionData.top = new FormAttachment(specifyTheVersionLabel, FormHelper.ControlSpacing(), SWT.BOTTOM);
        versionData.left = new FormAttachment(0, 0);
        versionData.right = new FormAttachment(100, 0);
        version.setLayoutData(versionData);
    }

    public VersionSpec getVersionSpec() {
        return version.getVersionSpec();
    }
}
