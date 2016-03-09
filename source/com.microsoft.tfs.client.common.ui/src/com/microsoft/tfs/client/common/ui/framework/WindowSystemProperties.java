// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework;

import org.eclipse.jface.dialogs.IDialogConstants;

/**
 * A simple helper class to provide information about the windowing system and
 * suggestions for how to lay objects out in a native manner.
 *
 * @threadsafety unknown
 */
public final class WindowSystemProperties {
    /**
     * Determines whether the native look and feel suggests that buttons in a
     * group (for example, buttons at the bottom of a dialog box) should be the
     * same size. This is the suggestion for Mac OS X, for instance.
     *
     * @return true to resize buttons in a group, false otherwise
     */
    public final static boolean groupButtonsShareSize() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return true;
        }

        return false;
    }

    /**
     * Determines whether the native look and feel suggests that the
     * cancel/close/dismiss button or the ok button is the right-most button in
     * a dialog (in left-to-right languages.)
     *
     * @return {@link IDialogConstants#OK_ID} or
     *         {@link IDialogConstants#CANCEL_ID}
     */
    public final static int getDefaultButton() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return IDialogConstants.OK_ID;
        } else {
            return IDialogConstants.CANCEL_ID;
        }
    }
}
