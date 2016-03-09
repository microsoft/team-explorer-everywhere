// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TypedListener;

import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;

/**
 * This is a button-like widget which makes use of images to draw itself. It
 * simply draws the image, without any client trimmings - thus differing from
 * the SWT Button widget.
 *
 * Clients may optionally set images depending on the state of the button. The
 * different types of images are:
 *
 * enabled: used whenever the button is drawn disabled: used when
 * setEnabled(false) is called, or the shell is not focused hover: used when the
 * mouse "hovers" over the button depressed: used when the mouse clicks in the
 * button
 *
 * You are responsible for disposing any images you pass to this widget.
 */
public class ImageButton extends Canvas {
    /*
     * Images
     */

    /* The standard image to use for this button */
    private Image enabledImage;

    /* Image to use when the button is disabled */
    private Image disabledImage;

    /* Image to use when the mouse hovers over the image */
    private Image hoverImage;

    /* Image to use when the button is clicked */
    private Image depressedImage;

    /*
     * State
     */

    /* User can force the size of the control */
    private final Point size = new Point(SWT.DEFAULT, SWT.DEFAULT);

    /* Are we currently hovering */
    private boolean hover = false;

    /* Is the button depressed (as in clicked, not sad) */
    private boolean depressed = false;

    /* Does the container shell have focus */
    private boolean shellFocused = true;

    /* Has this widget successfully painted yet? */
    private boolean hasPainted = false;

    private final ImageButtonListener listener = new ImageButtonListener();

    public ImageButton(final Composite parent, final int style) {
        super(parent, style);

        addMouseListener(listener);
        addMouseMoveListener(listener);
        addPaintListener(listener);
    }

    /**
     * Sets the image for use when the button is enabled.
     *
     * @param enabledImage
     *        The image to use when the button is enabled.
     */
    public void setEnabledImage(final Image enabledImage) {
        this.enabledImage = enabledImage;
    }

    /**
     * Gets the image used when the button is enabled.
     *
     * @return The image drawn when the button is enabled.
     */
    public Image getEnabledImage() {
        return enabledImage;
    }

    /**
     * Sets the image for use when the button is disabled.
     *
     * @param disabledImage
     *        The image to use when the button is disabled.
     */
    public void setDisabledImage(final Image disabledImage) {
        this.disabledImage = disabledImage;
    }

    /**
     * Gets the image used when the button is disabled.
     *
     * @return The image drawn when the button is disabled.
     */
    public Image getDisabledImage() {
        return disabledImage;
    }

    /**
     * Sets the image for use when the mouse "hovers" over the button
     *
     * @param hoverImage
     *        The image to use when the button is hovered over.
     */
    public void setHoverImage(final Image hoverImage) {
        this.hoverImage = hoverImage;
    }

    /**
     * Gets the image used when the button is hovered over.
     *
     * @return The image drawn when the button is hovered over.
     */
    public Image getHoverImage() {
        return hoverImage;
    }

    /**
     * Sets the image for use when the user presses the button
     *
     * @param depressedImage
     *        The image to use when the button is pressed.
     */
    public void setDepressedImage(final Image depressedImage) {
        this.depressedImage = depressedImage;
    }

    /**
     * Gets the image used when the button is pressed.
     *
     * @return The image drawn when the button is pressed.
     */
    public Image getDepressedImage() {
        return depressedImage;
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the control is selected by the user, by sending it one of the
     * messages defined in the <code>SelectionListener</code> interface.
     * <p>
     * <code>widgetSelected</code> is called when the control is selected by the
     * user. <code>widgetDefaultSelected</code> is not called.
     * </p>
     *
     * @param listener
     *        the listener which should be notified
     */
    public void addSelectionListener(final SelectionListener listener) {
        addListener(SWT.Selection, new TypedListener(listener));
    }

    /**
     * Removes the given listener from the collection of listeners who will be
     * notified when the control is selected by the user.
     *
     * @param listener
     *        the listener which should no longer be notified
     */
    public void removeSelectionListener(final SelectionListener listener) {
        removeListener(SWT.Selection, listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        redraw();
    }

    /**
     * Forces this control to be the given size. Images will be scaled to fit.
     *
     * @param size
     *        The size to force the image to.
     */
    @Override
    public void setSize(final Point size) {
        super.setSize(size);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
     */
    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean changed) {
        final Point imageSize = new Point(0, 0);

        /* If the user has forced the size of this control, use theirs */
        if (size.x != SWT.DEFAULT && size.y != SWT.DEFAULT) {
            return size;
        }

        /* Get the size of the largest image we intend to display */
        final Image[] images = new Image[] {
            enabledImage,
            disabledImage,
            hoverImage,
            depressedImage
        };

        for (int i = 0; i < images.length; i++) {
            if (images[i] == null) {
                continue;
            }

            final Rectangle imageBounds = images[i].getBounds();

            if (imageBounds.width > imageSize.x) {
                imageSize.x = imageBounds.width;
            }
            if (imageBounds.height > imageSize.y) {
                imageSize.y = imageBounds.height;
            }
        }

        /*
         * If the user has forced one dimension, use it and keep the computed
         * other dimension
         */
        if (size.x != SWT.DEFAULT) {
            imageSize.x = size.x;
        } else if (size.y != SWT.DEFAULT) {
            imageSize.y = size.y;
        }

        return imageSize;
    }

    /**
     * Determines whether or not the shell containing this control is currently
     * focused. This works by testing whether or not the control with focus is
     * in our shell. If so, we're focused - if not, we're not focused.
     *
     * @return true if the container shell for this control is focused
     */
    private boolean shellIsFocused() {
        final Control focusControl = getDisplay().getFocusControl();

        if (focusControl == null) {
            return false;
        }

        final Shell focusedShell = ShellUtils.getParentShell(focusControl);
        final Shell ourShell = ShellUtils.getParentShell(this);

        return (focusedShell == ourShell);
    }

    /**
     * Determines whether or not the shell's focus has changed since the last
     * time we drew.
     *
     * This method returns true if and only if the focus has changed since the
     * last call.
     *
     * @return true if focus has changed, false otherwise
     */
    private boolean shellFocusChanged() {
        final boolean oldShellFocused = shellFocused;

        shellFocused = shellIsFocused();

        return (oldShellFocused != shellFocused);
    }

    /**
     * Gets the image to be drawn for the current state of the button. (ie, if
     * the widget is current disabled, then this will return the disabled
     * image.)
     *
     * @return The Image to draw
     */
    private Image getImage() {
        /*
         * Note that on the first pass we always return the enabled image - this
         * is because as we're being laid out the first time, we are not yet
         * initialized to the point where we can determine properly our
         * enablement based on our shell.
         */

        if (((!shellFocused && hasPainted == true) || !isEnabled()) && disabledImage != null) {
            return disabledImage;
        } else if (hover && depressed && depressedImage != null) {
            return depressedImage;
        } else if (hover && hoverImage != null) {
            return hoverImage;
        } else {
            return enabledImage;
        }
    }

    /**
     * Returns true if the point (x, y) in control coordinates is in the
     * control. Useful for determining whether mouse events are inside your
     * control.
     *
     * @param x
     *        the x-position to test
     * @param y
     *        the y-position to test
     * @return true if the point is inside the control, false otherwise
     */
    protected boolean controlContainsPoint(final int x, final int y) {
        final Point controlSize = computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        return (x >= 0 && y >= 0 && x < controlSize.x && y < controlSize.y);
    }

    /*
     * Hooks for subclasses
     */

    /**
     * @param event
     *        The MouseEvent to process
     * @return true if the mouse down event should be processed
     */
    protected boolean hookMouseDown(final MouseEvent event) {
        return true;
    }

    /**
     * @param event
     *        The MouseEvent to process
     * @return true if the mouse up event should be processed
     */
    protected boolean hookMouseUp(final MouseEvent event) {
        return true;
    }

    private class ImageButtonListener implements MouseListener, PaintListener, MouseMoveListener {
        @Override
        public void paintControl(final PaintEvent e) {
            /*
             * We get a paint event when the shell containing this control loses
             * or gains focus. Unfortunately, we are not guaranteed to paint our
             * entire control, so we force a redraw to guarantee a full
             * painting.
             */
            if (hasPainted && shellFocusChanged()) {
                redraw();
                return;
            }

            final Image buttonImage = getImage();

            if (buttonImage != null && !e.gc.isDisposed()) {
                e.gc.drawImage(buttonImage, 0, 0);
            }

            hasPainted = true;
        }

        @Override
        public void mouseDoubleClick(final MouseEvent e) {
        }

        @Override
        public void mouseDown(final MouseEvent e) {
            if (hookMouseDown(e) && controlContainsPoint(e.x, e.y)) {
                depressed = true;

                /* Redraw to handle drawing depressed state */
                if (!isDisposed()) {
                    redraw();
                }
            }
        }

        @Override
        public void mouseUp(final MouseEvent e) {
            /* If we weren't actually depressed, leave now */
            if (!depressed || !hookMouseUp(e)) {
                return;
            }

            if (controlContainsPoint(e.x, e.y)) {
                notifyListeners(SWT.Selection, new Event());
            }

            depressed = false;

            /* Redraw to remove depressed state */
            if (!isDisposed()) {
                redraw();
            }
        }

        @Override
        public void mouseMove(final MouseEvent e) {
            final boolean oldHover = hover;

            if (controlContainsPoint(e.x, e.y)) {
                hover = true;
            } else {
                hover = false;
            }

            if (hover != oldHover && !isDisposed()) {
                redraw();
            }
        }
    }
}
