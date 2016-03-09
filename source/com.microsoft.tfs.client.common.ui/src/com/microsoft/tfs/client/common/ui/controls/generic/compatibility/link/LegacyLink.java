// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class LegacyLink extends Canvas {
    // private static final RGB LINK_FOREGROUND_COLOR_RGB = new RGB(0, 51, 153);
    private static final RGB LINK_FOREGROUND_COLOR_RGB = new RGB(0, 0, 128);

    private final SingleListenerFacade selectionListeners = new SingleListenerFacade(SelectionListener.class);
    private final Color linkForegroundColor;

    private String text;
    private boolean hasFocus;
    private boolean armed;

    public LegacyLink(final Composite parent, final int style) {
        super(parent, style);

        text = ""; //$NON-NLS-1$
        setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        linkForegroundColor = new Color(getDisplay(), LINK_FOREGROUND_COLOR_RGB);

        final Listener listener = new Listener() {
            @Override
            public void handleEvent(final Event e) {
                switch (e.type) {
                    case SWT.FocusIn:
                        LegacyLink.this.onFocusIn(e);
                        break;
                    case SWT.FocusOut:
                        LegacyLink.this.onFocusOut(e);
                        break;
                    case SWT.Traverse:
                        LegacyLink.this.onTraverse(e);
                        break;
                    case SWT.Dispose:
                        LegacyLink.this.onDispose(e);
                        break;
                    case SWT.Paint:
                        LegacyLink.this.onPaint(e);
                        break;
                    case SWT.KeyDown:
                        LegacyLink.this.onKeyDown(e);
                        break;
                    case SWT.MouseDown:
                        LegacyLink.this.onMouseDown(e);
                        break;
                    case SWT.MouseUp:
                        LegacyLink.this.onMouseUp(e);
                        break;
                }
            }
        };

        addListener(SWT.FocusIn, listener);
        addListener(SWT.FocusOut, listener);
        addListener(SWT.Traverse, listener);
        addListener(SWT.Dispose, listener);
        addListener(SWT.Paint, listener);
        addListener(SWT.KeyDown, listener);
        addListener(SWT.MouseDown, listener);
        addListener(SWT.MouseUp, listener);
    }

    public void addSelectionListener(final SelectionListener listener) {
        selectionListeners.addListener(listener);
    }

    public void removeSelectionListener(final SelectionListener listener) {
        selectionListeners.removeListener(listener);
    }

    private void onMouseUp(final Event e) {
        if (!armed || e.button != 1) {
            return;
        }

        armed = false;

        final Point size = getSize();
        if (e.x < 0 || e.y < 0 || e.x >= size.x || e.y >= size.y) {
            return;
        }

        linkActivated(e);
    }

    private void onMouseDown(final Event e) {
        if (e.button != 1) {
            return;
        }
        armed = true;
    }

    private void onKeyDown(final Event e) {
        if (e.character == SWT.CR) {
            linkActivated(e);
        }
    }

    private void onPaint(final Event e) {
        final GC gc = e.gc;

        final Rectangle clientArea = getClientArea();

        final Color oldForeground = gc.getForeground();
        if (isEnabled()) {
            gc.setForeground(linkForegroundColor);
        } else {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        }
        gc.drawText(text, 0, 0);

        final int descent = gc.getFontMetrics().getDescent();
        final int lineY = clientArea.height - descent + 1;
        gc.drawLine(0, lineY, clientArea.width, lineY);

        gc.setForeground(oldForeground);

        if (hasFocus) {
            gc.drawFocus(0, 0, clientArea.width, clientArea.height);
        }
    }

    private void onDispose(final Event e) {
        linkForegroundColor.dispose();
    }

    private void onTraverse(final Event e) {
        switch (e.detail) {
            case SWT.TRAVERSE_PAGE_NEXT:
            case SWT.TRAVERSE_PAGE_PREVIOUS:
            case SWT.TRAVERSE_ARROW_NEXT:
            case SWT.TRAVERSE_ARROW_PREVIOUS:
            case SWT.TRAVERSE_RETURN:
                e.doit = false;
                return;
        }

        e.doit = true;
    }

    private void onFocusOut(final Event e) {
        hasFocus = false;
        redraw();
    }

    private void onFocusIn(final Event e) {
        hasFocus = true;
        redraw();
    }

    public void setText(String text) {
        if (text == null) {
            text = ""; //$NON-NLS-1$
        }

        /*
         * Undo SWT mnemonics: convert escaped double-ampersands to null
         * character, remove single ampersands, then convert nulls back to
         * single ampersands.
         */
        text = text.replaceAll("&&", new String(new byte[] //$NON-NLS-1$
        {
            0
        }));
        text = text.replaceAll("&", ""); //$NON-NLS-1$ //$NON-NLS-2$
        text = text.replaceAll(new String(new byte[] {
            0
        }), "&"); //$NON-NLS-1$

        this.text = text;

        redraw();
    }

    public String getText() {
        return text;
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        int width = 0;
        int height = 0;

        if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
            final GC gc = new GC(this);
            final Point extent = gc.stringExtent(text);
            gc.dispose();

            width = extent.x;
            height = extent.y;
        }

        if (wHint != SWT.DEFAULT) {
            width = wHint;
        }
        if (hHint != SWT.DEFAULT) {
            height = hHint;
        }

        return new Point(width, height);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        redraw();
    }

    private void linkActivated(final Event e) {
        setCursor(getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

        final SelectionListener listener = (SelectionListener) selectionListeners.getListener();
        listener.widgetSelected(new SelectionEvent(e));

        if (!isDisposed()) {
            setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        }
    }
}
