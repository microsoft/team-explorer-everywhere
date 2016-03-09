// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.dialogs.vc;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.eclipse.filemodification.TFSFileModificationStatusData;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.util.Check;

public class FileModificationFailureDialog extends ErrorDialog {
    public FileModificationFailureDialog(
        final Shell parentShell,
        final TFSFileModificationStatusData[] statusData,
        final IStatus status) {
        super(
            parentShell,
            Messages.getString("FileModificationFailureDialog.DialogTitle"), //$NON-NLS-1$
            getMessage(statusData),
            status,
            IStatus.ERROR | IStatus.WARNING | IStatus.INFO);
    }

    private static final String getMessage(final TFSFileModificationStatusData[] statusData) {
        Check.notNull(statusData, "statusData"); //$NON-NLS-1$

        final StringBuffer message = new StringBuffer();
        if (statusData.length == 1) {
            message.append(Messages.getString("FileModificationFailureDialog.SingleFileCouldNotBeCheckedOut")); //$NON-NLS-1$
        } else {
            message.append(Messages.getString("FileModificationFailureDialog.MultiFilesCouldNotBeCheckedOut")); //$NON-NLS-1$
        }

        message.append("\n\n"); //$NON-NLS-1$
        message.append(Messages.getString("FileModificationFailureDialog.CouldNotCheckOutRemedy")); //$NON-NLS-1$
        return message.toString();
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        createButton(
            parent,
            IDialogConstants.OK_ID,
            Messages.getString("FileModificationFailureDialog.RevertButtonText"), //$NON-NLS-1$
            true);
        createButton(
            parent,
            IDialogConstants.CANCEL_ID,
            Messages.getString("FileModificationFailureDialog.ContinueButtonText"), //$NON-NLS-1$
            false);

        /* Not available in eclipse < 3.2, always included by default. */
        if (SWT.getVersion() >= 3200) {
            createDetailsButton(parent);
        }
    }
}
