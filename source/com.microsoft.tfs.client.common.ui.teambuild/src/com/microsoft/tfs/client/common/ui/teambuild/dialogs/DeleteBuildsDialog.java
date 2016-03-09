// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDetail;

/**
 * Show the delete build dialog.
 */
public class DeleteBuildsDialog extends DeleteOptionsDialog {

    private final IBuildDetail buildsToDelete[];

    public DeleteBuildsDialog(final Shell parentShell, final IBuildDetail[] buildDetails, final boolean isV3OrGreater) {
        super(parentShell, isV3OrGreater);
        this.buildsToDelete = buildDetails;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("DeleteBuildsDialog.DialogTitleText"); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("DeleteBuildsDialog.DeleteButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);
    }

    @Override
    protected String getDescriptionText() {
        final StringBuffer text = new StringBuffer();
        if (buildsToDelete.length > 0) {
            text.append(buildsToDelete[0].getBuildNumber());
        }
        for (int i = 1; i < buildsToDelete.length; i++) {
            text.append(", "); //$NON-NLS-1$
            text.append(buildsToDelete[i].getBuildNumber());
        }
        return MessageFormat.format(
            Messages.getString("DeleteBuildsDialog.DescriptionLabelMessageFormat"), //$NON-NLS-1$
            text.toString());
    }

    @Override
    protected boolean isDetailsDefault() {
        return false;
    }

    @Override
    protected String detailsButtonLabel() {
        return Messages.getString("DeleteBuildsDialog.DetailsButtonLabel"); //$NON-NLS-1$
    }

}
