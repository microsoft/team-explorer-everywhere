// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictEncodingSelectionControl;
import com.microsoft.tfs.client.common.ui.controls.vc.ConflictNameSelectionControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.util.FileEncoding;

public class ConflictResolutionNameAndEncodingDialog extends BaseDialog {
    private ConflictNameSelectionControl nameSelectionControl;
    private ConflictEncodingSelectionControl encodingSelectionControl;

    private ConflictDescription conflictDescription;
    private String filename;
    private FileEncoding encoding;

    public ConflictResolutionNameAndEncodingDialog(final Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ConflictResolutionNameAndEncodingDialog.ConflictDialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label nameDescriptionLabel = new Label(dialogArea, SWT.NONE);
        nameDescriptionLabel.setText(Messages.getString("ConflictResolutionNameSelectionDialog.DescriptionLabelText")); //$NON-NLS-1$

        nameSelectionControl = new ConflictNameSelectionControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(nameSelectionControl);

        SWTUtil.createGridLayoutSpacer(dialogArea, 1, 1);

        final Label encodingDescriptionLabel = new Label(dialogArea, SWT.NONE);
        encodingDescriptionLabel.setText(Messages.getString("ConflictResolutionEncodingDialog.DescriptionLabelText")); //$NON-NLS-1$

        encodingSelectionControl = new ConflictEncodingSelectionControl(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(encodingSelectionControl);

        if (conflictDescription != null) {
            nameSelectionControl.setConflictDescription(conflictDescription);
            encodingSelectionControl.setConflictDescription(conflictDescription);
        }
    }

    @Override
    protected void hookAfterButtonsCreated() {
        nameSelectionControl.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                filename = nameSelectionControl.getFilename();

                getButton(IDialogConstants.OK_ID).setEnabled(filename != null);
            }
        });
    }

    public void setConflictDescription(final ConflictDescription conflictDescription) {
        this.conflictDescription = conflictDescription;

        if (conflictDescription != null && nameSelectionControl != null && !nameSelectionControl.isDisposed()) {
            nameSelectionControl.setConflictDescription(conflictDescription);
        }
    }

    @Override
    protected void okPressed() {
        filename = nameSelectionControl.getFilename();
        encoding = encodingSelectionControl.getFileEncoding();

        if (filename == null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ConflictResolutionNameSelectionDialog.ChooseFileDialogTitle"), //$NON-NLS-1$
                Messages.getString("ConflictResolutionNameSelectionDialog.ChooseFileDialogText")); //$NON-NLS-1$
            return;
        }

        super.okPressed();
    }

    public String getFilename() {
        return filename;
    }

    public FileEncoding getFileEncoding() {
        return encoding;
    }
}
