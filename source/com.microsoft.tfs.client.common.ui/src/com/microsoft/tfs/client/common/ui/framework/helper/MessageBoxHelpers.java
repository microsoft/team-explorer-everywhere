// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.core.product.ProductInformation;

public class MessageBoxHelpers {
    public static boolean dialogConfirmPrompt(Shell parent, String dialogTitle, final String confirmMessage) {
        parent = ShellUtils.getBestParent(parent);
        dialogTitle = sanitizeTitle(dialogTitle);

        return MessageDialog.openConfirm(parent, dialogTitle, confirmMessage);
    }

    public static boolean dialogYesNoPrompt(Shell parent, String dialogTitle, final String yesNoMessage) {
        parent = ShellUtils.getBestParent(parent);
        dialogTitle = sanitizeTitle(dialogTitle);

        return MessageDialog.openQuestion(parent, dialogTitle, yesNoMessage);
    }

    public static void errorMessageBox(Shell parent, String dialogTitle, final String message) {
        parent = ShellUtils.getBestParent(parent);
        dialogTitle = sanitizeTitle(dialogTitle);

        MessageDialog.openError(parent, dialogTitle, message);
    }

    public static void warningMessageBox(Shell parent, String dialogTitle, final String message) {
        parent = ShellUtils.getBestParent(parent);
        dialogTitle = sanitizeTitle(dialogTitle);

        MessageDialog.openWarning(parent, dialogTitle, message);
    }

    public static void messageBox(Shell parent, String dialogTitle, final String message) {
        parent = ShellUtils.getBestParent(parent);
        dialogTitle = sanitizeTitle(dialogTitle);

        MessageDialog.openInformation(parent, dialogTitle, message);
    }

    private static String sanitizeTitle(final String title) {
        if (title == null) {
            return ProductInformation.getCurrent().toString();
        }

        return title;
    }
}
