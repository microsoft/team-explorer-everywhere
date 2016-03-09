// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.vc.SetWorkingFolderDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class AddMappingDialog extends SetWorkingFolderDialog {

    private final String title;

    public AddMappingDialog(
        final Shell parentShell,
        final Workspace workspace,
        final String title,
        final String hint,
        final String serverPath,
        final String localPath) {
        super(parentShell, workspace, serverPath, hint, localPath, false);
        this.title = title;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String provideDialogTitle() {
        return title;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        super.hookAfterButtonsCreated();
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("AddMappingDialog.MapButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);
    }

    @Override
    protected String provideServerFolderLabelText() {
        return Messages.getString("AddMappingDialog.ServerFolderLabelText"); //$NON-NLS-1$
    }

    @Override
    protected void okPressed() {
        if (localFolderAlreadyMapped()) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("AddMappingDialog.AlreadyMappedTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("AddMappingDialog.AlreadyMappedDialogTextFormat"), //$NON-NLS-1$
                    this.getLocalFolder()));
            return;
        }

        super.okPressed();
    }
}
