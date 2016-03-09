// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;

import com.microsoft.tfs.client.common.ui.framework.WindowSystemProperties;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.util.Check;

/**
 * A collection of utilities for dealing with {@link Button} objects.
 */
public class ButtonHelper {
    /**
     * Resizes the buttons provided to the largest size if the platform suggests
     * doing so.
     */
    public static final void resizeButtons(final Button[] buttons) {
        Check.notNull(buttons, "buttons"); //$NON-NLS-1$

        if (WindowSystemProperties.groupButtonsShareSize() == false) {
            return;
        }

        final Point maxSize = ControlSize.maxSize(buttons);

        for (int i = 0; i < buttons.length; i++) {
            ControlSize.setSizeHints(buttons[i], maxSize);
        }
    }

    public static final void setButtonsToButtonBarSize(final Button[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            setButtonToButtonBarSize(buttons[i]);
        }
    }

    public static final void setButtonToButtonBarSize(final Button button) {
        final GC gc = new GC(button);
        final FontMetrics fm = gc.getFontMetrics();
        gc.dispose();

        final int widthHint = Dialog.convertHorizontalDLUsToPixels(fm, IDialogConstants.BUTTON_WIDTH);
        final Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        size.x = Math.max(widthHint, size.x);

        ControlSize.setSizeHints(button, size);
    }
}
