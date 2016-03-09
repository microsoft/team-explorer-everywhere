// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;

public class UndoPendingChangesDialog extends AbstractUndoPendingChangesDialog {
    public UndoPendingChangesDialog(final Shell parentShell, final ChangeItem[] changeItems) {
        super(parentShell, changeItems);

        setLabelText(Messages.getString("UndoPendingChangesDialog.SummaryLabelText")); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("UndoPendingChangesDialog.UndoButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);

        new ButtonValidatorBinding(button).bind(getChangesTable().getCheckboxValidator());
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("UndoPendingChangesDialog.DialogTitle"); //$NON-NLS-1$
    }
}
