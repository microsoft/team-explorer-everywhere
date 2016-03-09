// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;

public class UndoPendingChangesFromChangesetDialog extends AbstractUndoPendingChangesDialog {
    final int changesetID;

    public static final String DIALOG_AUTOMATION_ID =
        "com.microsoft.tfs.client.common.ui.dialogs.vc.UndoPendingChangesFromChangesetDialog.dialog"; //$NON-NLS-1$

    public UndoPendingChangesFromChangesetDialog(
        final Shell parentShell,
        final ChangeItem[] changeItems,
        final int changesetID) {
        super(parentShell, changeItems);

        this.changesetID = changesetID;

        setLabelText(Messages.getString("UndoPendingChangesFromChangesetDialog.SummaryLabelText")); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("UndoPendingChangesFromChangesetDialog.ReconcileButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);

        new ButtonValidatorBinding(button).bind(getChangesTable().getCheckboxValidator());

        AutomationIDHelper.setWidgetID(getDialogArea(), DIALOG_AUTOMATION_ID);
    }

    @Override
    protected String provideDialogTitle() {
        return MessageFormat.format(
            Messages.getString("UndoPendingChangesFromChangesetDialog.DialogTitleFormat"), //$NON-NLS-1$
            Integer.toString(changesetID));
    }
}
