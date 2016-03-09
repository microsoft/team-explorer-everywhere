// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.datepicker;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TypedListener;

import com.microsoft.tfs.util.Platform;

public class DatepickerDate extends Canvas implements PaintListener, DisposeListener {
    public final static int SHAPE_NONE = 0;
    public final static int SHAPE_LEFT = 1;
    public final static int SHAPE_RIGHT = 2;

    private int day = 0;
    private int monthOffset = 0;
    private Point padding = new Point(0, 1);

    private Color backgroundColor = null;
    private Color foregroundColor = null;
    private Color focusedBackgroundColor = null;
    private Color focusedForegroundColor = null;
    private Color defaultBackgroundColor = null;
    private Color defaultForegroundColor = null;
    private Color defaultStrokeColor = null;
    private Color otherMonthBackgroundColor = null;
    private Color otherMonthForegroundColor = null;

    private boolean isDefault = false;
    private boolean isFocused = false;
    private boolean focusedAndDefaultShowDefault = false;

    private int shape = 0;
    private int shapePadding = 0;

    private int alignment = SWT.LEFT;

    public DatepickerDate(final Composite parent, final int style) {
        super(parent, style);

        addPaintListener(this);
        addDisposeListener(this);

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            configureMacOSX();
        } else {
            configureOther();
        }

        setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

        pack();
    }

    private void configureMacOSX() {
        backgroundColor = new Color(getDisplay(), 230, 230, 230);
        foregroundColor = new Color(getDisplay(), 0, 0, 0);
        focusedBackgroundColor = new Color(getDisplay(), 147, 147, 147);
        focusedForegroundColor = new Color(getDisplay(), 255, 255, 255);
        defaultBackgroundColor = new Color(getDisplay(), 81, 161, 240);
        defaultForegroundColor = new Color(getDisplay(), 0, 0, 0);
        otherMonthBackgroundColor = new Color(getDisplay(), 230, 230, 230);
        otherMonthForegroundColor = new Color(getDisplay(), 255, 255, 255);

        padding = new Point(3, 2);

        shapePadding = 5;

        focusedAndDefaultShowDefault = true;
    }

    private void configureOther() {
        backgroundColor = new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB());
        foregroundColor = new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB());
        focusedBackgroundColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB());
        focusedForegroundColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT).getRGB());
        defaultBackgroundColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB());
        defaultForegroundColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND).getRGB());
        defaultStrokeColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW).getRGB());
        otherMonthBackgroundColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB());
        otherMonthForegroundColor =
            new Color(getDisplay(), getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW).getRGB());

        padding = new Point(7, 2);

        shapePadding = 0;
    }

    public void setDay(final int day) {
        setVisible(true);
        this.day = day;
    }

    public int getDay() {
        return day;
    }

    public void setMonthOffset(final int monthOffset) {
        this.monthOffset = monthOffset;
    }

    public int getMonthOffset() {
        return monthOffset;
    }

    public void setShape(final int shape) {
        this.shape = shape;
    }

    public int getShape() {
        return shape;
    }

    public void setPadding(final Point padding) {
        this.padding.x = padding.x;
        this.padding.y = padding.y;
    }

    public void setAlignment(final int alignment) {
        this.alignment = alignment;
    }

    public int getAlignment() {
        return alignment;
    }

    @Override
    public Point computeSize(final int xHint, final int yHint, final boolean changed) {
        // assume "33" is the largest number...?
        final GC gc = new GC(this);
        final Point size = gc.textExtent("33"); //$NON-NLS-1$
        gc.dispose();

        size.x += (padding.x * 2);
        size.y += (padding.y * 2);

        if (shape != SHAPE_NONE && shapePadding > 0) {
            size.x += (shapePadding - padding.x);
        }

        return size;
    }

    @Override
    public void paintControl(final PaintEvent e) {
        if (!isVisible()) {
            return;
        }

        Color bg, fg, stroke = null;

        final GC gc = e.gc;
        final String dayString = (day > 0) ? String.valueOf(day) : ""; //$NON-NLS-1$
        final Point size = getSize();

        // compute text size
        final Point textSize = gc.textExtent(dayString);

        // always center vertically...
        int x;
        final int y = ((size.y - textSize.y) / 2);

        if (alignment == SWT.RIGHT) {
            x = (size.x - padding.x) - textSize.x;
        } else if (alignment == SWT.CENTER) {
            x = (size.x - textSize.x) / 2;
        } else {
            x = padding.x;
        }

        if (shape == SHAPE_RIGHT && shapePadding > 0) {
            x -= (shapePadding - padding.x);
        }

        if (isFocused && isDefault) {
            bg = (focusedAndDefaultShowDefault == true) ? defaultBackgroundColor : focusedBackgroundColor;
            fg = focusedForegroundColor;
            stroke = defaultStrokeColor;
        } else if (isFocused) {
            bg = focusedBackgroundColor;
            fg = focusedForegroundColor;
        } else if (monthOffset != 0) {
            bg = otherMonthBackgroundColor;
            fg = otherMonthForegroundColor;
        } else if (isDefault) {
            bg = defaultBackgroundColor;
            fg = defaultForegroundColor;
            stroke = defaultStrokeColor;
        } else {
            bg = backgroundColor;
            fg = foregroundColor;
        }

        if (bg != null) {
            gc.setBackground(bg);
        }
        if (fg != null) {
            gc.setForeground(fg);
        }

        int rectXStart = 0;
        int rectXEnd = size.x;

        // osx has funny shapes for the left/right side days of the week...
        if (shape == SHAPE_LEFT && shapePadding > 0) {
            gc.fillOval(0, 0, size.y, size.y);
            rectXStart += (size.y / 2);
        } else if (shape == SHAPE_RIGHT && shapePadding > 0) {
            gc.fillOval(size.x - size.y, 0, size.y, size.y);
            rectXEnd -= (size.y / 2);
        }

        gc.fillRectangle(rectXStart, 0, rectXEnd, size.y);

        if (day > 0) {
            gc.drawText(dayString, x, y);
        }

        if (stroke != null) {
            gc.setForeground(stroke);
            gc.drawRectangle(rectXStart, 0, rectXEnd - 1, size.y - 1);
        }
    }

    public void setDefault(final boolean def) {
        isDefault = def;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setFocused(final boolean focused) {
        final boolean mustRedraw = (isFocused != focused);

        isFocused = focused;

        if (mustRedraw) {
            redraw();
        }
    }

    public boolean isFocused() {
        return isFocused;
    }

    @Override
    public void addMouseListener(final MouseListener listener) {
        addListener(SWT.MouseDown, new TypedListener(listener));
        addListener(SWT.MouseUp, new TypedListener(listener));
        addListener(SWT.MouseDoubleClick, new TypedListener(listener));
    }

    @Override
    public void removeMouseListener(final MouseListener listener) {
        removeListener(SWT.MouseDown, listener);
        removeListener(SWT.MouseUp, listener);
        removeListener(SWT.MouseDoubleClick, new TypedListener(listener));
    }

    @Override
    public void addMouseMoveListener(final MouseMoveListener listener) {
        addListener(SWT.MouseMove, new TypedListener(listener));
    }

    @Override
    public void removeMouseMoveListener(final MouseMoveListener listener) {
        removeListener(SWT.MouseMove, listener);
    }

    @Override
    public void widgetDisposed(final DisposeEvent e) {
        if (backgroundColor != null && !backgroundColor.isDisposed()) {
            backgroundColor.dispose();
        }
        if (foregroundColor != null && !foregroundColor.isDisposed()) {
            foregroundColor.dispose();
        }
        if (focusedBackgroundColor != null && !focusedBackgroundColor.isDisposed()) {
            focusedBackgroundColor.dispose();
        }
        if (focusedForegroundColor != null && !focusedForegroundColor.isDisposed()) {
            focusedForegroundColor.dispose();
        }
        if (defaultBackgroundColor != null && !defaultBackgroundColor.isDisposed()) {
            defaultBackgroundColor.dispose();
        }
        if (defaultForegroundColor != null && !defaultForegroundColor.isDisposed()) {
            defaultForegroundColor.dispose();
        }
        if (defaultStrokeColor != null && !defaultStrokeColor.isDisposed()) {
            defaultStrokeColor.dispose();
        }
        if (otherMonthBackgroundColor != null && !otherMonthBackgroundColor.isDisposed()) {
            otherMonthBackgroundColor.dispose();
        }
        if (otherMonthForegroundColor != null && !otherMonthForegroundColor.isDisposed()) {
            otherMonthForegroundColor.dispose();
        }
    }
}