// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.datepicker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;

public class DatepickerButton extends Canvas
    implements PaintListener, DisposeListener, MouseListener, MouseMoveListener {
    public static final int BUTTON_DEFAULT = 0;
    public static final int BUTTON_PREV = 1;
    public static final int BUTTON_NEXT = 2;

    private int type = BUTTON_DEFAULT;

    private Color backgroundColor = null;
    private Color foregroundColor = null;
    private Color hilightBackgroundColor = null;
    private Color hilightForegroundColor = null;

    private Font font = null;

    boolean isMouseDown = false;
    boolean isFocused = false;

    public DatepickerButton(final Composite parent, final int style) {
        super(parent, style);

        configureMacOSX();

        addPaintListener(this);
        addMouseListener(this);
        addDisposeListener(this);
        addMouseMoveListener(this);

        setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
    }

    private void configureMacOSX() {
        backgroundColor = new Color(getDisplay(), 166, 166, 166);
        foregroundColor = new Color(getDisplay(), 255, 255, 255);
        hilightBackgroundColor = new Color(getDisplay(), 181, 213, 255);
        hilightForegroundColor = new Color(getDisplay(), 255, 255, 255);
    }

    public void setType(final int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Override
    public void setFont(final Font font) {
        if (this.font != null && !this.font.isDisposed()) {
            this.font.dispose();
        }

        this.font = new Font(getDisplay(), font.getFontData());
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public Point computeSize(final int xHint, final int yHint, final boolean changed) {
        // we only care about computing the vertical height...
        final GC gc = new GC(this);

        if (font != null) {
            gc.setFont(font);
        }

        final Point size = gc.textExtent("Jp"); //$NON-NLS-1$
        size.x -= 1;
        size.y -= 1;
        gc.dispose();

        size.x = size.y;

        return size;
    }

    @Override
    public void paintControl(final PaintEvent e) {
        final GC gc = e.gc;
        final Point size = getSize();

        // draw the button background
        gc.setBackground(isFocused ? hilightBackgroundColor : backgroundColor);
        gc.fillOval(0, 0, size.x, size.y);

        gc.setBackground(isFocused ? hilightForegroundColor : foregroundColor);
        gc.fillPolygon(getPolygon());
    }

    private int[] getPolygon() {
        if (type == BUTTON_PREV) {
            return getPrevPolygon();
        } else if (type == BUTTON_NEXT) {
            return getNextPolygon();
        } else {
            return getDefaultPolygon();
        }
    }

    private int[] getDefaultPolygon() {
        final Point size = getSize();

        // osx diamond
        return new int[] {
            (int) Math.round(0.25 * size.x),
            (int) Math.round(0.5 * size.y),
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.25 * size.y),
            (int) Math.round(0.75 * size.x),
            (int) Math.round(0.5 * size.y),
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.75 * size.y)
        };
    }

    private int[] getPrevPolygon() {
        final Point size = getSize();

        // osx arrow
        return new int[] {
            (int) Math.round(0.25 * size.x),
            (int) Math.round(0.5 * size.y),
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.25 * size.y),
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.5 * size.y) - 1,
            (int) Math.round(0.75 * size.x),
            (int) Math.round(0.5 * size.y) - 1,
            (int) Math.round(0.75 * size.x),
            (int) Math.round(0.5 * size.y) + 1,
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.5 * size.y) + 1,
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.75 * size.y)
        };
    }

    private int[] getNextPolygon() {
        final Point size = getSize();

        // osx arrow
        return new int[] {
            (int) Math.round(0.25 * size.x),
            (int) Math.round(0.5 * size.y) - 1,
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.5 * size.y) - 1,
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.25 * size.y),
            (int) Math.round(0.75 * size.x),
            (int) Math.round(0.5 * size.y),
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.75 * size.y),
            (int) Math.round(0.5 * size.x),
            (int) Math.round(0.5 * size.y) + 1,
            (int) Math.round(0.25 * size.x),
            (int) Math.round(0.5 * size.y) + 1
        };
    }

    public void addSelectionListener(final SelectionListener listener) {
        addListener(SWT.Selection, new TypedListener(listener));
    }

    public void removeSelectionListener(final SelectionListener listener) {
        removeListener(SWT.Selection, listener);
    }

    @Override
    public void mouseMove(final MouseEvent e) {
        boolean selectionChanged = false;
        final Point size = getSize();

        // they've left the control with the mouse down
        if (isMouseDown && isFocused && (e.x < 0 || e.x > size.x || e.y < 0 || e.y > size.y)) {
            isFocused = false;
            selectionChanged = true;
        }
        // they've reentered the control with the mouse down
        else if (isMouseDown && !isFocused && (e.x > 0 && e.x <= size.x && e.y > 0 && e.y <= size.y)) {
            isFocused = true;
            selectionChanged = true;
        }

        if (selectionChanged) {
            redraw();
        }
    }

    @Override
    public void mouseDoubleClick(final MouseEvent e) {
    }

    @Override
    public void mouseDown(final MouseEvent e) {
        isMouseDown = true;
        isFocused = true;

        redraw();
    }

    @Override
    public void mouseUp(final MouseEvent e) {
        isMouseDown = false;
        isFocused = false;

        redraw();

        // make sure they clicked within the bounds of this control...
        final Point size = getSize();
        if (e.x > 0 && e.x <= size.x && e.y > 0 && e.y <= size.y) {
            notifyListeners(SWT.Selection, new Event());
        }
    }

    @Override
    public void widgetDisposed(final DisposeEvent e) {
        if (backgroundColor != null && !backgroundColor.isDisposed()) {
            backgroundColor.dispose();
        }
        if (foregroundColor != null && !foregroundColor.isDisposed()) {
            foregroundColor.dispose();
        }
        if (hilightBackgroundColor != null && !hilightBackgroundColor.isDisposed()) {
            hilightBackgroundColor.dispose();
        }
        if (hilightForegroundColor != null && !hilightForegroundColor.isDisposed()) {
            hilightForegroundColor.dispose();
        }
        if (font != null && !font.isDisposed()) {
            font.dispose();
        }
    }
}
