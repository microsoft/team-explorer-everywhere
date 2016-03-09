// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.layout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * This class if for debugging only! It sets background colors on a Control
 * hierarchy contained in a Composite. This is useful for debugging
 * layout-related problems.
 */
public class LayoutColorizer {
    private static int[] COLOR_VALUES = new int[] {
        SWT.COLOR_BLUE,
        SWT.COLOR_GREEN,
        SWT.COLOR_YELLOW,
        SWT.COLOR_RED,
        SWT.COLOR_GRAY,
        SWT.COLOR_CYAN
    };

    private static String[] COLOR_LABELS = new String[] {
        "COLOR_BLUE", //$NON-NLS-1$
        "COLOR_GREEN", //$NON-NLS-1$
        "COLOR_YELLOW", //$NON-NLS-1$
        "COLOR_RED", //$NON-NLS-1$
        "COLOR_GRAY", //$NON-NLS-1$
        "COLOR_CYAN" //$NON-NLS-1$
    };

    public static void colorize(final Composite container) {
        final Color[] colors = new Color[COLOR_VALUES.length];
        for (int i = 0; i < COLOR_VALUES.length; i++) {
            colors[i] = Display.getCurrent().getSystemColor(COLOR_VALUES[i]);
        }

        container.setBackground(colors[0]);
        System.out.println(container.getClass().getName() + ": " + COLOR_LABELS[0]); //$NON-NLS-1$
        colorizeChildren(container, colors, 1);
    }

    private static void colorizeChildren(final Composite container, final Color[] colors, final int colorIndex) {
        final Control[] children = container.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].setBackground(colors[colorIndex]);
            if (children[i] instanceof Composite) {
                System.out.println(children[i].getClass().getName() + ": " + COLOR_LABELS[colorIndex]); //$NON-NLS-1$
            }

            if (children[i] instanceof Composite) {
                colorizeChildren(
                    (Composite) children[i],
                    colors,
                    (colorIndex + 1 >= colors.length ? 0 : colorIndex + 1));
            }
        }
    }
}
