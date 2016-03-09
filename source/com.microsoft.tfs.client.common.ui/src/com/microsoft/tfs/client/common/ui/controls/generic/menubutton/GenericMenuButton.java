// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.menubutton;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TypedListener;

import com.microsoft.tfs.client.common.ui.framework.helper.PolygonUtils;
import com.microsoft.tfs.util.Check;

public class GenericMenuButton extends MenuButton {
    /* The button */
    private final Button button;

    /* Size of the button */
    private Point buttonSize = new Point(SWT.DEFAULT, SWT.DEFAULT);

    /* The polygon coordinates of the disclosure triangle */
    private final int[] triangleCoords = new int[] {
        0,
        0,
        5,
        0,
        2,
        3
    };

    /* The size of the polygon described above */
    private final Rectangle triangleBounds = PolygonUtils.getBounds(triangleCoords);

    /* Spacing between text and image on the control */
    private final int spacing;

    /* x-position to start drawing the image */
    private int separatorPosition;

    /* Are we enabled? */
    private boolean enabled = true;

    /* Colors */

    /* The color of the separator line */
    private final Color separatorColor = new Color(getDisplay(), 100, 100, 100);

    /* The color of the triangle */
    private final Color triangleColor = new Color(getDisplay(), 0, 0, 0);

    /* The color to draw when disabled */
    private final Color disabledColor = new Color(getDisplay(), 175, 175, 175);

    private final ButtonTraverseListener traverseListener = new ButtonTraverseListener();
    private final ButtonMouseListener mouseListener = new ButtonMouseListener();

    public GenericMenuButton(final Composite parent, final int style) {
        super(parent, style);

        /* Create the button */
        button = new Button(this, SWT.PUSH | SWT.LEFT);

        /*
         * Determine the spacing between bits in the control - mainly between
         * the text and an image. This is computed by taking the size of the
         * control without anything in it and halving it.
         */
        spacing = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x / 2;

        /*
         * Setup the paint listener, which draws our image onto the button.
         */
        button.addPaintListener(new DisclosurePaintListener());

        button.addMouseListener(mouseListener);
        button.addTraverseListener(traverseListener);
    }

    @Override
    public Button getButton() {
        return button;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.widgets.disclosurebutton.
     * DisclosureButton#setText(java.lang.String)
     */
    @Override
    public void setText(final String text) {
        /*
         * In Windows, when the text alignment on a button is SWT.LEFT, then we
         * need to add a leading space.
         */
        button.setText(" " + text); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    @Override
    public void setEnabled(final boolean enabled) {
        button.setEnabled(enabled);
        this.enabled = enabled;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.widgets.menubutton.MenuButton
     * #isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        /* Get the size of the button with text */
        buttonSize = button.computeSize(wHint, hHint, true);

        /*
         * Store the original width of the button - this is where we want to
         * start drawing
         */
        separatorPosition = buttonSize.x;

        /*
         * To accommodate our image, we need to add the size of the image, plus
         * spacing between the text and the image, plus some padding on the
         * right side. (Padding is half the spacing.)
         */
        buttonSize.x += triangleBounds.width + spacing + 1 + spacing + (spacing / 2);

        /*
         * If the image is taller than the font, we need to pad the height
         */
        if (triangleBounds.height > button.getFont().getFontData()[0].getHeight()) {
            buttonSize.y += (triangleBounds.height - button.getFont().getFontData()[0].getHeight());
        }

        /* Update the button size */
        button.setSize(buttonSize);

        return buttonSize;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.widgets.disclosurebutton.
     * DisclosureButton
     * #addSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    @Override
    public void addSelectionListener(final SelectionListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        addListener(SWT.Selection, new TypedListener(listener));
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.widgets.disclosurebutton.
     * DisclosureButton
     * #removeSelectionListener(org.eclipse.swt.events.SelectionListener)
     */
    @Override
    public void removeSelectionListener(final SelectionListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$
        removeListener(SWT.Selection, listener);
    }

    private class ButtonTraverseListener implements TraverseListener {
        @Override
        public void keyTraversed(final TraverseEvent e) {
            if (e.widget == button && (e.detail == SWT.TRAVERSE_RETURN || e.keyCode == ' ')) {
                final Event selectionEvent = new Event();
                selectionEvent.data = new Integer(SWT.Selection);
                notifyListeners(SWT.Selection, selectionEvent);

                e.doit = false;
            } else if (e.widget == button && e.keyCode == SWT.ARROW_DOWN) {
                openMenu();
            }
        }
    }

    private class ButtonMouseListener extends MouseAdapter {
        private static final int NONE = 0;
        private static final int BUTTON = 1;
        private static final int DISCLOSURE = 2;

        private int mouseDown = NONE;

        /**
         * Gets the location of the mouse event: the BUTTON side, the DISCLOSURE
         * side or NONE.
         *
         * @param e
         *        The MouseEvent to investigate
         * @return BUTTON if the event occurred on the button side, DISCLOSURE
         *         if the event occurred on the disclosure triangle side, or
         *         NONE if this was out of the control's bounds
         */
        private int mouseEventLocation(final MouseEvent e) {
            if (buttonSize.x < 0 || buttonSize.y < 0) {
                computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
            }

            /* Filter out events which did not occur in the control */
            if (e.x < 0 || e.y < 0 || e.x > buttonSize.x || e.y > buttonSize.y) {
                return NONE;
            }

            /* On the right side of the separator */
            if (e.x > separatorPosition) {
                return DISCLOSURE;
            }

            return BUTTON;
        }

        @Override
        public void mouseDown(final MouseEvent e) {
            mouseDown = mouseEventLocation(e);
        }

        @Override
        public void mouseUp(final MouseEvent e) {
            /*
             * Ensure that the mouse went down in the same location as it's
             * going up.
             */
            final int upLocation = mouseEventLocation(e);

            /* This is the selection event we'll fire */
            final Event selectionEvent = new Event();

            /* On the button side of the button */
            if (mouseDown == upLocation && upLocation == BUTTON) {
                selectionEvent.data = new Integer(SWT.Selection);
                notifyListeners(SWT.Selection, selectionEvent);
            }

            /* On the disclosure triangle side of the button */
            else if (mouseDown == upLocation && upLocation == DISCLOSURE) {
                selectionEvent.data = new Integer(SWT.Expand);
                openMenu();
            }

            mouseDown = 0;
        }
    }

    private class DisclosurePaintListener implements PaintListener {
        @Override
        public void paintControl(final PaintEvent e) {
            final Point buttonSize = button.getSize();

            /* Get the colors to use */
            final Color lineColor = enabled ? separatorColor : disabledColor;
            final Color fillColor = enabled ? triangleColor : disabledColor;

            /* Set the GC color to the separator color */
            final Color forecolor = e.gc.getForeground();
            e.gc.setForeground(lineColor);

            /* Compute the line height - just larger than the font */
            final int lineHeight = button.getFont().getFontData()[0].getHeight() + 4;

            /*
             * Compute the start and end positions for the line: the center +/-
             * half the line height
             */
            final int lineStartY = (buttonSize.y - lineHeight) / 2;
            final int lineEndY = (buttonSize.y + lineHeight) / 2;

            e.gc.drawLine(separatorPosition, lineStartY, separatorPosition, lineEndY);

            /* Restore the GC foreground color */
            e.gc.setForeground(forecolor);

            /*
             * The coordinates for drawing the image: draw <spacing> away from
             * the separator (if there is one) or at the drawing position (if
             * not). Center in the button in the y coordinate.
             */
            final int imageX = separatorPosition + spacing + 1;
            final int imageY = (buttonSize.y - triangleBounds.height) / 2;

            /*
             * GC.fill* methods use the background color - we actually want the
             * foreground color. Swap them temporarily.
             */
            final Color backColor = e.gc.getBackground();
            e.gc.setBackground(fillColor);

            /* Draw the polygon at the computed coordinates */
            e.gc.fillPolygon(PolygonUtils.transformPosition(new Point(imageX, imageY), triangleCoords));

            /* Restore the gc background color */
            e.gc.setBackground(backColor);
        }
    }
}