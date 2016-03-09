// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class ConflictResolutionRenameDialog extends BaseDialog {
    private Text nameText;

    private String filename;

    public ConflictResolutionRenameDialog(final Shell parentShell) {
        super(parentShell);

        setOptionResizableDirections(SWT.HORIZONTAL);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ConflictResolutionRenameDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label descriptionLabel = new Label(dialogArea, SWT.WRAP);
        descriptionLabel.setText(Messages.getString("ConflictResolutionRenameDialog.DescriptionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).applyTo(descriptionLabel);

        SWTUtil.createGridLayoutSpacer(dialogArea, 2, 1);

        final Label namePromptLabel = new Label(dialogArea, SWT.NONE);
        namePromptLabel.setText(Messages.getString("ConflictResolutionRenameDialog.NameLabelText")); //$NON-NLS-1$

        nameText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(nameText);

        if (filename != null) {
            nameText.setText(filename);
        } else {
            nameText.setText(""); //$NON-NLS-1$
        }

        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updateEnablement();
            }
        });
    }

    public void setFilename(final String filename) {
        this.filename = filename;

        if (nameText != null && !nameText.isDisposed()) {
            nameText.setText(filename);
        }
    }

    private void updateEnablement() {
        getButton(IDialogConstants.OK_ID).setEnabled(nameText.getText().trim().length() > 0);
    }

    @Override
    protected void hookDialogAboutToClose() {
        filename = nameText.getText();
    }

    public String getFilename() {
        return filename;
    }
}
