// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;

/**
 * A simple vertical or horizontal separator. Similar to using a {@link Label}
 * as a horizontal or vertical separator, but uses a single pixel.
 *
 * @threadsafety thread safe
 */
public class Separator extends BaseControl {
    private final int width = 1;
    private final int height = 1;

    private int direction = SWT.HORIZONTAL;

    private Color color = getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER);
    private boolean colorOwned = false;

    public Separator(final Composite parent, final int style) {
        super(parent, SWT.NONE);

        if ((style & SWT.VERTICAL) == SWT.VERTICAL) {
            direction = SWT.VERTICAL;
        } else if ((style & SWT.HORIZONTAL) == SWT.HORIZONTAL) {
            direction = SWT.HORIZONTAL;
        }

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) {
            colorOwned = true;
            color = new Color(getShell().getDisplay(), 190, 190, 190);
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            colorOwned = true;
            color = new Color(getShell().getDisplay(), 139, 139, 139);
        }

        setBackground(color);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (colorOwned) {
                    color.dispose();
                }
            }
        });
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        redraw();
    }

    @Override
    public Point computeSize(int wHint, int hHint, final boolean changed) {
        if (wHint == SWT.DEFAULT) {
            wHint = 1;
        }

        if (hHint == SWT.DEFAULT) {
            hHint = 1;
        }

        Point size;

        if (direction == SWT.VERTICAL) {
            size = new Point(width, hHint);
        } else if (direction == SWT.HORIZONTAL) {
            size = new Point(wHint, height);
        } else {
            size = super.computeSize(wHint, hHint, changed);
        }

        return size;
    }
}
