// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

/**
 * Helper class used to display messages that can be disabled if the end user
 * finds them annoying.
 */
public class ToggleMessageHelper {
    /**
     * Convenience method to optionally open a standard information dialog and
     * store the preference.
     *
     * @param parent
     *        the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *        the dialog's title, or <code>null</code> if none
     * @param message
     *        the message
     * @param toggleMessage
     *        the message for the toggle control, or <code>null</code> for the
     *        default message
     * @param defaultToggleState
     *        the initial state for the toggle
     * @param key
     *        the key to use when persisting the user's preference;
     *        <code>null</code> if you don't want it persisted.
     */
    public static void openInformation(
        final Shell parent,
        final String title,
        final String message,
        final String toggleMessage,
        final boolean defaultToggleState,
        final String preferenceKey) {
        final IPreferenceStore prefStore = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        final boolean toggleState = MessageDialogWithToggle.ALWAYS.equals(prefStore.getString(preferenceKey));

        if (!toggleState) {
            MessageDialogWithToggle.openInformation(
                parent,
                title,
                message,
                toggleMessage,
                defaultToggleState,
                prefStore,
                preferenceKey);

            TFSCommonUIClientPlugin.getDefault().savePluginPreferences();
        }
    }

    /**
     * Convenience method to optionally open a standard information dialog and
     * store the preference using a standard toggle message which is set to not
     * be checked by default.
     *
     * @param parent
     *        the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *        the dialog's title, or <code>null</code> if none
     * @param message
     *        the message
     * @param key
     *        the key to use when persisting the user's preference;
     *        <code>null</code> if you don't want it persisted.
     */
    public static void openInformation(
        final Shell parent,
        final String title,
        final String message,
        final String preferenceKey) {
        ToggleMessageHelper.openInformation(
            parent,
            title,
            message,
            Messages.getString("ToggleMessageHelper.DontDisplayAgain"), //$NON-NLS-1$
            false,
            preferenceKey);
    }

    /**
     * Convenience method to open a simple confirm (OK/Cancel) dialog.
     *
     * @param parent
     *        the parent shell of the dialog, or <code>null</code> if none
     * @param title
     *        the dialog's title, or <code>null</code> if none
     * @param message
     *        the message
     * @param toggleMessage
     *        the message for the toggle control, or <code>null</code> for the
     *        default message
     * @param toggleState
     *        the initial state for the toggle
     * @param store
     *        the IPreference store in which the user's preference should be
     *        persisted; <code>null</code> if you don't want it persisted
     *        automatically.
     * @param key
     *        the key to use when persisting the user's preference;
     *        <code>null</code> if you don't want it persisted.
     * @return the dialog, after being closed by the user, which the client can
     *         only call <code>getReturnCode()</code> or
     *         <code>getToggleState()</code>
     */
    public static boolean openYesNoQuestion(
        final Shell parent,
        final String title,
        final String message,
        final String toggleMessage,
        final boolean defaultToggleState,
        final String preferenceKey) {
        final IPreferenceStore prefStore = TFSCommonUIClientPlugin.getDefault().getPreferenceStore();

        final boolean toggleState = MessageDialogWithToggle.ALWAYS.equals(prefStore.getString(preferenceKey));

        if (!toggleState) {
            final MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion(
                parent,
                title,
                message,
                toggleMessage,
                defaultToggleState,
                prefStore,
                preferenceKey);

            TFSCommonUIClientPlugin.getDefault().savePluginPreferences();

            return (dialog.getReturnCode() == IDialogConstants.YES_ID);
        }

        return true;
    }
}