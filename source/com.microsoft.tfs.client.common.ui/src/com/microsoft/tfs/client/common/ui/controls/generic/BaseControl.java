// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;

/**
 * A simple Control base class which computes control spacing and margins in
 * pixels (instead of DLUs).
 */
public abstract class BaseControl extends Composite {
    private final int horizontalSpacing;
    private final int verticalSpacing;

    private final int spacing;

    private final int horizontalMargin;
    private final int verticalMargin;

    private final int minimumMessageAreaWidth;

    public BaseControl(final Composite parent, final int style) {
        super(parent, style);

        /* Compute metrics in pixels */
        final GC gc = new GC(this);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        horizontalSpacing = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING);
        verticalSpacing = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);

        spacing = Math.max(horizontalSpacing, verticalSpacing);

        horizontalMargin = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
        verticalMargin = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN);

        minimumMessageAreaWidth =
            Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
    }

    public int getHorizontalSpacing() {
        return horizontalSpacing;
    }

    public int getVerticalSpacing() {
        return verticalSpacing;
    }

    public int getSpacing() {
        return spacing;
    }

    public int getHorizontalMargin() {
        return horizontalMargin;
    }

    public int getVerticalMargin() {
        return verticalMargin;
    }

    public int getMinimumMessageAreaWidth() {
        return minimumMessageAreaWidth;
    }
}
