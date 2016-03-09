// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

public class FormHelper {
    public static final int SPACING_NORMAL = 0;
    public static final int SPACING_TIGHT = 1;

    public static int VerticalOffset(final Control control, final Control alignDestination) {
        int offset = ((alignDestination.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
            - control.computeSize(SWT.DEFAULT, SWT.DEFAULT).y) / 2);

        // correct OS X label/button interaction
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            if (control instanceof Label && alignDestination instanceof Button) {
                offset -= 2;
            } else if (control instanceof Button && alignDestination instanceof Button) {
                offset += 2;
            }
        }

        return offset;
    }

    public static int LabelAlignment() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return SWT.RIGHT;
        }

        return SWT.LEFT;
    }

    public static int Spacing() {
        return ControlSpacing();
    }

    public static int GroupSpacing() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return 5;
        }

        return 5;
    }

    public static int ControlSpacing() {
        return ControlSpacing(SPACING_NORMAL);
    }

    public static int ControlSpacing(final int flags) {
        if ((flags & SPACING_TIGHT) == SPACING_TIGHT && WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return 1;
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return 7;
        }

        return 5;
    }

    public static int MarginWidth() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return 7;
        }

        return 5;
    }

    public static int MarginHeight() {
        return 5;
    }

    public static int TextHeight(final Text control, final int width) {
        final GC gc = new GC(control);
        final FontMetrics fm = gc.getFontMetrics();
        gc.dispose();

        if (control.getText() == null) {
            return SWT.DEFAULT;
        }

        return (fm.getHeight()
            * (int) Math.ceil((((fm.getAverageCharWidth() * 1.0) * control.getText().length()) / width) + 0.25)) + 2;
    }

    /**
     *
     * @return The number of pixels to indent other controls to match text
     *         controls.
     */
    public static Point TextControlIndent() {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            return new Point(7, -5);
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)) {
            return new Point(6, -2);
        }

        return new Point(0, 0);
    }
}
