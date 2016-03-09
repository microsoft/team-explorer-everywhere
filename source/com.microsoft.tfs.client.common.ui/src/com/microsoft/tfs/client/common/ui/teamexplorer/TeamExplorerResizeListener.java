// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Section;

import com.microsoft.tfs.client.common.ui.TeamExplorerEventArg;
import com.microsoft.tfs.util.Check;

public class TeamExplorerResizeListener implements TeamExplorerEventListener {
    private final static int MAX_CONTROL_WIDTH = 500;

    private final Control control;
    private final GridData gridData;
    private int additionalMarginWidth;

    public TeamExplorerResizeListener(final Control control) {
        Check.notNull(control, "control"); //$NON-NLS-1$
        Check.isTrue(control.getLayoutData() instanceof GridData, "control.getLayoutData() instanceof GridData"); //$NON-NLS-1$

        this.control = control;
        this.gridData = (GridData) control.getLayoutData();
        this.additionalMarginWidth = 0;
    }

    public TeamExplorerResizeListener(final Control control, final int additionalMarginWidth) {
        this(control);
        this.additionalMarginWidth = additionalMarginWidth;
    }

    @Override
    public void onEvent(final TeamExplorerEventArg arg) {
        final TeamExplorerResizeEventArg resizeArg = (TeamExplorerResizeEventArg) arg;
        final int margin = calculateMarginWidth(control) + additionalMarginWidth;

        gridData.grabExcessHorizontalSpace = false;
        gridData.horizontalAlignment = SWT.LEFT;
        gridData.widthHint = Math.min(MAX_CONTROL_WIDTH - margin, resizeArg.getFormWidth() - margin);
    }

    private static int calculateMarginWidth(final Control control) {
        Control c = control;
        int margin = 0;

        while (c != null && !c.isDisposed() && !(c instanceof Form)) {
            final int leftMargin = c.getBounds().x;

            margin += leftMargin;

            // assume there is an equal amount of right margin if not in a
            // section.
            if (!(c.getParent() instanceof Section)) {
                margin += leftMargin;
            }
            c = c.getParent();
        }

        return margin;
    }
}
