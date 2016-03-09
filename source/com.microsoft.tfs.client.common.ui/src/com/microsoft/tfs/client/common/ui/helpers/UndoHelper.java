// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class UndoHelper {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private static final int YES;
    private static final int YES_TO_ALL;
    private static final int NO;
    private static final int NO_TO_ALL;
    private static final int CANCEL;

    private static final String[] DIALOG_BUTTON_LABELS = new String[5];

    static {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)
            || WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
            /*
             * Mac and GNOME's user interface guidelines instruct that the
             * default button must be far-left, and SWT will move it to this
             * location. The button to the immediate right of the default button
             * must be a do-not-proceed button. This is the order of the buttons
             * in nautilus, and we mimic it. This keeps the spirit of the
             * guidelines (no / yes are in the cancel / ok positions).
             */
            CANCEL = 0;
            NO_TO_ALL = 1;
            YES_TO_ALL = 2;
            NO = 3;
            YES = 4;
        } else {
            YES = 0;
            YES_TO_ALL = 1;
            NO = 2;
            NO_TO_ALL = 3;
            CANCEL = 4;
        }

        DIALOG_BUTTON_LABELS[YES] = IDialogConstants.YES_LABEL;
        DIALOG_BUTTON_LABELS[YES_TO_ALL] = IDialogConstants.YES_TO_ALL_LABEL;
        DIALOG_BUTTON_LABELS[NO] = IDialogConstants.NO_LABEL;
        DIALOG_BUTTON_LABELS[NO_TO_ALL] = IDialogConstants.NO_TO_ALL_LABEL;
        DIALOG_BUTTON_LABELS[CANCEL] = IDialogConstants.CANCEL_LABEL;
    }

    public static PendingChange[] filterChangesToUndo(final PendingChange[] changes, final Shell shell) {
        /*
         * See:
         * Microsoft.VisualStudio.TeamFoundation.VersionControl.ClientHelperVS
         * .Undo for the Microsoft implementation of this
         */

        final List<PendingChange> changesToUndo = new ArrayList<PendingChange>();
        int choice = CANCEL;

        for (int i = 0; i < changes.length; i++) {
            final PendingChange change = changes[i];

            if (choice == YES_TO_ALL || (!change.hasContentChange() && !isDirtyEdit(change))) {
                changesToUndo.add(change);
                continue;
            }

            if (choice == NO_TO_ALL) {
                continue;
            }

            choice = promptForChange(shell, change);

            if (choice == CANCEL) {
                return null;
            }

            if (choice == YES || choice == YES_TO_ALL) {
                changesToUndo.add(change);
            }
        }

        return changesToUndo.toArray(new PendingChange[changesToUndo.size()]);
    }

    private static boolean isDirtyEdit(final PendingChange change) {
        /*
         * TODO
         */
        return false;
    }

    private static int promptForChange(final Shell shell, final PendingChange change) {
        final String message = MessageFormat.format(
            Messages.getString("UndoHelper.ConfirmUndoAndDiscardChangesFormat"), //$NON-NLS-1$
            change.getLocalItem());

        final MessageDialog dialog = new MessageDialog(
            shell,
            Messages.getString("UndoHelper.ConfirmDialogTitle"), //$NON-NLS-1$
            null,
            message,
            MessageDialog.WARNING,
            DIALOG_BUTTON_LABELS,
            YES);

        return dialog.open();
    }
}
