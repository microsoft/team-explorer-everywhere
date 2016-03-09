// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.dialogs;

import java.text.MessageFormat;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;

public class RetentionPolicyDeleteDialog extends DeleteOptionsDialog {

    private final String buildReason;
    private final String buildStatus;

    public RetentionPolicyDeleteDialog(
        final Shell parentShell,
        final String buildReason,
        final String buildStatus,
        final DeleteOptions deleteOptions) {
        super(parentShell, deleteOptions);
        this.buildReason = buildReason;
        this.buildStatus = buildStatus;
    }

    @Override
    protected String getDescriptionText() {
        return MessageFormat.format(
            Messages.getString("RetentionPolicyDeleteDialog.DescriptionLabelMessageFormat"), //$NON-NLS-1$
            buildReason,
            buildStatus);
    }

    @Override
    protected boolean isDetailsDefault() {
        return true;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("RetentionPolicyDeleteDialog.DialogTitleText"); //$NON-NLS-1$
    }

    @Override
    protected String detailsButtonLabel() {
        return Messages.getString("RetentionPolicyDeleteDialog.DeleteButtonLabel"); //$NON-NLS-1$
    }
}
