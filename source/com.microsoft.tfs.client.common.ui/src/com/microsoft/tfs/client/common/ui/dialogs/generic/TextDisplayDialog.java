// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.generic;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;

public class TextDisplayDialog extends BaseDialog {
    private static final int COPY_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

    private final String title;
    private final String textData;

    public TextDisplayDialog(
        final Shell parentShell,
        final String title,
        final String textData,
        final String dialogSettingsKey) {
        super(parentShell);
        this.title = title;
        this.textData = textData;
        setOptionDialogSettingsKey(dialogSettingsKey);

        addButtonDescription(IDialogConstants.OK_ID, Messages.getString("TextDisplayDialog.OkButtonText"), true); //$NON-NLS-1$
        addButtonDescription(COPY_BUTTON_ID, Messages.getString("TextDisplayDialog.CopyToClipboardButtonText"), false); //$NON-NLS-1$
        setOptionIncludeDefaultButtons(false);
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == COPY_BUTTON_ID) {
            UIHelpers.copyToClipboard(textData);
        }
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        dialogArea.setLayout(new FillLayout());

        final Text text = new Text(dialogArea, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
        text.setEditable(false);

        text.setText(textData);
    }

    @Override
    protected Point defaultComputeInitialSize() {
        return new Point(500, 300);
    }
}
