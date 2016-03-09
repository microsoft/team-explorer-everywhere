// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.dialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * An extension of the standard Eclipse ErrorDialog that exists purely to hide
 * the inner exception of an {@link IStatus}.
 *
 * Typically, we create {@link IStatus} objects that represent the results of an
 * exception (see command exception handlers.) The message is constructed from
 * the exception details (potentially improved upon.) Typically we do not want
 * to display both the status message and the exception message, to avoid
 * redundancy. This class allows us to hide the exception message and only
 * display the (improved) status message.
 */
public class TeamExplorerErrorDialog extends ErrorDialog {
    private final IStatus status;

    public TeamExplorerErrorDialog(
        final Shell parentShell,
        final String dialogTitle,
        final String message,
        final IStatus status,
        final int displayMask) {
        super(parentShell, dialogTitle, message, status, displayMask);

        this.status = status;
    }

    /**
     * Overrides super class, only shows the details button when the status is a
     * multi status, not when the status contains an exception.
     */
    @Override
    protected boolean shouldShowDetailsButton() {
        return status.isMultiStatus();
    }

    public static int openError(
        final Shell parent,
        final String dialogTitle,
        final String message,
        final IStatus status) {
        return openError(
            parent,
            dialogTitle,
            message,
            status,
            IStatus.OK | IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
    }

    public static int openError(
        final Shell parentShell,
        final String title,
        final String message,
        final IStatus status,
        final int displayMask) {
        final ErrorDialog dialog = new TeamExplorerErrorDialog(parentShell, title, message, status, displayMask);
        return dialog.open();
    }
}
