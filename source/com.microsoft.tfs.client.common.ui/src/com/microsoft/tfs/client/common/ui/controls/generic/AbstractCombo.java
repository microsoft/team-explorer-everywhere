// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TypedListener;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;

/**
 * AbstractCombo is a combobox-like control that allows opening any composite
 * when the disclosure button (down arrow) is pressed. This allows one to create
 * things which look like the standard combo boxes but host more advanced
 * controls. For example: a datepicker can be popped open and the date can be in
 * the text box.
 */
public abstract class AbstractCombo extends Composite {
    /* The text control and the disclosure button */
    protected final Text text;
    private final Control revealControl;

    /* Style controls passed in */
    private final int style;

    /* Cached sizes of the controls */
    private Point textSize;
    private Point revealSize;

    /* This is a dummy control used for size calculations */
    private final Combo dummyCombo;

    /* The button listener, which opens the control */
    private final SelectionListener revealControlListener = new RevealOpenListener();

    /*
     * This is the shell that we open when the disclosure button is clicked.
     */
    private Shell popupShell = null;
    private final int popupStyle;

    /* Should we call pack() on the popup shell? */
    private boolean packPopup = true;

    /* Spacing between the text control and the disclosure button */
    private int spacing = 0;

    /* The last event (primarily for MacOS AbstractComboButton, see below) */
    private Event lastEvent;

    /* Is the popup open? */
    private boolean popupIsOpen = false;

    /* Are we (or a child) focused? */
    private boolean hasFocus = false;

    /* Focus listener */
    private final ComponentFocusListener componentFocusListener;

    /* Selection listener for clicks outside our area */
    private final NonComponentSelectionListener nonComponentSelectionListener;

    /* Popup listeners */
    ArrayList<PopupListener> popupListeners = new ArrayList<PopupListener>();

    /**
     * <p>
     * AbstractCombo is a combobox-like control that allows opening any
     * composite when the disclosure button (down arrow) is pressed.
     * </p>
     * <p>
     * If the style parameter has the SWT.BORDER bit set, then the AbstractCombo
     * will appear with a border around it. On Windows, this border will appear
     * on the composite that makes up the AbstractCombo. On other platforms,
     * this border will appear on the Text control inside the AbstractCombo.
     * Normally the border bit should be set as in most use cases this control
     * looks most natural with a border. However, when being used inside a cell
     * editor, it will look best with the border bit off.
     * </p>
     *
     * @param parent
     *        parent shell
     * @param style
     *        SWT widget style
     */
    public AbstractCombo(final Composite parent, final int style) {
        super(parent, checkStyle(style));

        this.style = style;

        /* Create the text control */
        text = new Text(this, checkStyleForTextControl(style));

        /*
         * Create the reveal control. On MacOS this is an ImageButton since
         * drop-down buttons look stupid. Note that this is only done when style
         * is SWT.BORDER -- otherwise, we use an ugly button, which is typically
         * used in cell editors.
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA) && (style & SWT.BORDER) == SWT.BORDER) {
            final AbstractComboButton revealButton = new AbstractComboButton(this, SWT.DOWN);
            revealButton.addSelectionListener(revealControlListener);
            revealButton.setTextControl(text);
            revealButton.setSize(SWT.DEFAULT, text.getSize().y);
            revealControl = revealButton;

            /*
             * Back up 5px right to compensate for focus ring around the text
             * control
             */
            spacing = -5;

            /*
             * Reveal control needs to be above the text control (in the
             * z-plane) on MacOS because it intentionally clips the edge of the
             * text control.
             */
            revealControl.moveAbove(text);
        } else {
            final Button revealButton = new Button(this, SWT.ARROW | SWT.DOWN | (style & SWT.FLAT));
            revealButton.addSelectionListener(revealControlListener);
            revealControl = revealButton;
        }

        /* Compute popup's style */
        popupStyle = checkStyleForPopup(style);

        /*
         * Create a dummy combo box which we use for height calculation. Ensure
         * it uses the same font as our text control so that our size
         * calculations are sane. Use a dummy shell so we don't ever paint the
         * control into this shell.
         */
        dummyCombo = new Combo(this, (style & SWT.BORDER));
        dummyCombo.setVisible(false);

        /*
         * mandatory to set the background on the windows overflow area around
         * the text area to the same color as the windows text field
         */
        setBackground(null);

        addControlListener(new ControlListener() {
            @Override
            public void controlMoved(final ControlEvent e) {
                layout(true);
            }

            @Override
            public void controlResized(final ControlEvent e) {
                layout(true);
            }
        });

        /* Handle focus events by our components */
        componentFocusListener = new ComponentFocusListener();

        text.addListener(SWT.FocusIn, componentFocusListener);
        text.addListener(SWT.FocusOut, componentFocusListener);
        revealControl.addListener(SWT.FocusIn, componentFocusListener);
        revealControl.addListener(SWT.FocusOut, componentFocusListener);

        /* Handle clicks out of our area on OS X */
        nonComponentSelectionListener = new NonComponentSelectionListener();

        /* Remove our display listener! Muoy importante! */
        addListener(SWT.Dispose, new Listener() {
            @Override
            public void handleEvent(final Event e) {
                getDisplay().removeFilter(SWT.FocusIn, componentFocusListener);
                getDisplay().removeFilter(SWT.Selection, nonComponentSelectionListener);
            }
        });

        return;
    }

    /**
     * Handles style differences for different platforms. In particular, this
     * determines where the border is drawn when constructed with the SWT.BORDER
     * style bit. On Windows, the border is drawn around the composite (this
     * control). On other platforms, the border is not drawn around the
     * composite (see checkStyleForTextControl.)
     *
     * @param style
     *        The style bits from the constructor
     * @return The style bits appropriate for this composite
     */
    private static int checkStyle(final int style) {
        int baseStyle = SWT.NONE;

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS) && (style & SWT.BORDER) > 0) {
            baseStyle = SWT.BORDER;
        }

        return (baseStyle | (style & SWT.FLAT));
    }

    /**
     * Handles style differences for different platforms. In particular, this
     * determines where the border is drawn when constructed with the SWT.BORDER
     * style bit. On Windows, the border is drawn around the composite (see
     * checkStyle). On other platforms, the border is drawn around the text
     * control.
     *
     * @param style
     *        The style bits from the constructor
     * @return The style bits appropriate for the text control
     */
    private static int checkStyleForTextControl(final int style) {
        int baseStyle = SWT.NONE;

        if (!WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS) && (style & SWT.BORDER) > 0) {
            baseStyle = SWT.BORDER;
        }

        return (baseStyle | (style & SWT.FLAT) | (style & SWT.READ_ONLY));
    }

    /**
     * Handles style bits for the popup shells. Enforces modality.
     *
     * @param style
     *        The style bits from the constructor
     * @return Modality bits from the style
     */
    private static int checkStyleForPopup(final int style) {
        return SWT.NO_TRIM | (style & SWT.SYSTEM_MODAL) | (style & SWT.APPLICATION_MODAL) | (style & SWT.PRIMARY_MODAL);
    }

    public Text getTextControl() {
        return text;
    }

    /*
     * Sets the background color of the text control.
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.swt.widgets.Control#setBackground(org.eclipse.swt.graphics
     * .Color)
     */
    @Override
    public void setBackground(final Color color) {
        text.setBackground(color);

        /*
         * Note the Windows hack.
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)) {
            super.setBackground(text.getBackground());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#getBackground()
     */
    @Override
    public Color getBackground() {
        return text.getBackground();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.swt.widgets.Control#setForeground(org.eclipse.swt.graphics
     * .Color)
     */
    @Override
    public void setForeground(final Color color) {
        text.setForeground(color);
        revealControl.setForeground(color);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#getForeground()
     */
    @Override
    public Color getForeground() {
        return text.getForeground();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.swt.widgets.Control#setFont(org.eclipse.swt.graphics.Font)
     */
    @Override
    public void setFont(final Font font) {
        /*
         * Note that if the font changes, we need to update the dummy combo
         * control also for proper height calculations
         */
        text.setFont(font);
        dummyCombo.setFont(font);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#getFont()
     */
    @Override
    public Font getFont() {
        return text.getFont();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);

        /*
         * Cocoa feature: composite enablement doesn't always bubble down to the
         * text control...
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) {
            text.setEnabled(enabled);
            revealControl.setEnabled(enabled);
        }

        redraw();
    }

    /**
     * Returns true if the given control is "ours" -- ie, the text control, the
     * disclosure widget, or anything in a popup shell.
     *
     * @param control
     *        The control to test
     * @return true if the control is "ours"
     */
    private boolean isOurControl(final Control control) {
        if (control != null && !control.isDisposed() && ShellUtils.getParentShell(control) == popupShell) {
            return true;
        }

        if (control == text || control == revealControl) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#isFocusControl()
     */
    @Override
    public boolean isFocusControl() {
        return isOurControl(getDisplay().getFocusControl());
    }

    /**
     * Focus is somewhat complex for us: if the text control or the button which
     * comprises this control has focus, then we have focus. Also, we have focus
     * if our popup has focus.
     */
    private void handleFocus() {
        if (isDisposed()) {
            return;
        }

        final boolean focus = isFocusControl();

        /* We are focused when we weren't before */
        if (focus == true && hasFocus == false) {
            hasFocus = true;

            /*
             * Always remove before adding to ensure we don't add multiple times
             */
            getShell().removeListener(SWT.Deactivate, componentFocusListener);
            getShell().addListener(SWT.Deactivate, componentFocusListener);

            getDisplay().removeFilter(SWT.FocusIn, componentFocusListener);
            getDisplay().addFilter(SWT.FocusIn, componentFocusListener);

            /*
             * OS X doesn't lose focus properly when some widgets are clicked:
             * for example, a combo in a TabFolder
             */
            if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
                getDisplay().removeFilter(SWT.Selection, nonComponentSelectionListener);
                getDisplay().addFilter(SWT.Selection, nonComponentSelectionListener);
            }

            notifyListeners(SWT.FocusIn, new Event());
        }

        /* We were focused but no longer are */
        else if (focus == false && hasFocus == true) {
            hasFocus = false;

            getShell().removeListener(SWT.Deactivate, componentFocusListener);
            getDisplay().removeFilter(SWT.FocusIn, componentFocusListener);
            getDisplay().removeFilter(SWT.Selection, nonComponentSelectionListener);

            notifyListeners(SWT.FocusOut, new Event());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Composite#setFocus()
     */
    @Override
    public boolean setFocus() {
        /* Don't steal focus from our inner controls */
        if (isFocusControl()) {
            return true;
        }

        return text.setFocus();
    }

    /**
     * Sets the editable state.
     *
     * @param editable
     *        the new editable state
     */
    public void setEditable(final boolean editable) {
        text.setEditable(editable);
    }

    public void setPackPopup(final boolean pack) {
        packPopup = pack;
    }

    @Override
    public Point computeSize(final int xHint, final int yHint, final boolean changed) {
        /* Compute the height and width of the individual controls */
        revealSize = revealControl.computeSize(SWT.DEFAULT, yHint, changed);

        /*
         * If we were given a hint width that is not SWT.DEFAULT, then the
         * parent layout has determined a constrained size for us. We want to
         * hint the text size with the overall size minus the reveal button size
         * minus spacing between. Otherwise, let the text lay out to its
         * preferred size.
         */
        final int xHintText = (xHint != SWT.DEFAULT) ? xHint - (revealSize.x + spacing) : SWT.DEFAULT;
        textSize = text.computeSize(xHintText, yHint, changed);

        /* On Windows, we want to emulate the size of a native combo box */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)) {
            return dummyCombo.computeSize(xHint, yHint, changed);
        }

        /* On Mac OS Cocoa, we need to pad the top a few pixels */
        int yPadding = 0;

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) {
            yPadding = 4;
        }

        return new Point(textSize.x + revealSize.x + spacing, Math.max(textSize.y, revealSize.y) + yPadding);
    }

    @Override
    public void layout(final boolean changed) {
        final Rectangle clientArea = getClientArea();

        if (clientArea.width == 0 && clientArea.height == 0) {
            text.setBounds(0, 0, 0, 0);
            revealControl.setBounds(0, 0, 0, 0);
        } else {
            /*
             * Our size may be constrained beyond what we suggested by
             * computeSize()
             */
            if (clientArea.height < textSize.y) {
                textSize.y = clientArea.height;
            }

            /* Fill horizontally, center the text box vertically */
            text.setBounds(
                0,
                (clientArea.height - textSize.y) / 2,
                clientArea.width - revealSize.x - spacing,
                textSize.y);

            /* Right-align, vertically centered */
            revealControl.setBounds(
                clientArea.width - revealSize.x,
                (clientArea.height - revealSize.y) / 2,
                revealSize.x,
                revealSize.y);
        }
    }

    /**
     * overridable, will be called when we are about to open the child composite
     */
    public void hookBeforeOpen() {
    }

    /**
     * must be overridden by subclasses, this is the Control to draw on
     * disclosure click
     *
     * @param parent
     *        the shell
     * @return the composite to draw on disclosure click
     */
    public abstract Composite getPopup(Composite parent);

    /**
     * overridable, will be called when we are about to close the child
     * composite
     */
    public void hookBeforeClose() {
    }

    /**
     * the method that opens the child composite
     */
    protected void openPopup() {
        if (popupShell != null && !popupShell.isDisposed()) {
            popupShell.setFocus();
            popupShell.setVisible(true);

            return;
        }

        popupShell = new Shell(getShell(), popupStyle);

        /*
         * Give a 1 px margin - use this to draw a border (see paint listener)
         */
        final FillLayout layout = new FillLayout();
        layout.marginHeight = 1;
        layout.marginWidth = 1;

        popupShell.setLayout(layout);

        getPopup(popupShell);

        /*
         * sometimes we want to pack the popup (DatePicker does this for
         * instance), but allow users to set width to span the width of the
         * control (wit, eg.)
         */
        if (packPopup) {
            popupShell.pack();
        } else {
            popupShell.setSize(popupShell.getChildren()[0].computeSize(getSize().x, SWT.DEFAULT));
        }

        /* Create a listener to handle closing the popup */
        final PopupShellListener popupShellListener = new PopupShellListener();

        /* Add a deactivate listener to the shell which will close our popup. */
        popupShell.addListener(SWT.Deactivate, popupShellListener);

        /*
         * Add selection and escape key listeners to the composite we're
         * hosting. These also close the popup.
         */
        popupShell.addListener(SWT.Paint, popupShellListener);

        hookBeforeOpen();

        popupShell.setLocation(getPopupLocation());

        popupShell.setVisible(true);
        popupShell.setFocus();

        popupIsOpen = true;

        notifyPopupListener(true);
    }

    private Point getPopupLocation() {
        int x = ((getParent().toDisplay(getLocation())).x + getSize().x) - popupShell.getSize().x;
        int y = getParent().toDisplay(getLocation()).y + getSize().y;

        /*
         * Compensate for the focus ring around the control
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA) && (style & SWT.BORDER) == SWT.BORDER) {
            x -= 4;
            y -= 4;
        } else if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            y += 1;
        }

        return new Point(x, y);
    }

    protected final void closePopup() {
        popupIsOpen = false;

        if (popupShell != null && !popupShell.isDisposed()) {
            hookBeforeClose();

            popupShell.close();
            popupShell.dispose();
            popupShell = null;
        }

        notifyPopupListener(false);
    }

    public boolean isPopupOpen() {
        return popupIsOpen;
    }

    @Override
    public void setToolTipText(final String string) {
        checkWidget();
        super.setToolTipText(string);
        revealControl.setToolTipText(string);
        text.setToolTipText(string);
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

    /**
     * Adds a modify listener to this AbstractCombo's text widget.
     *
     * @param listener
     *        the listener to add
     */
    public void addModifyListener(final ModifyListener listener) {
        text.addModifyListener(listener);
    }

    /**
     * Removes a previously added modify listener to this AbstractCombo's text
     * widget.
     *
     * @param modifyListener
     *        the listener to remove
     */
    public void removeModifyListener(final ModifyListener modifyListener) {
        text.removeModifyListener(modifyListener);
    }

    /**
     * Adds a listener which will be notified when the popup is opened.
     *
     * @param listener
     *        the listener to notify
     */
    public void addPopupListener(final PopupListener listener) {
        if (!popupListeners.contains(listener)) {
            popupListeners.add(listener);
        }
    }

    private void notifyPopupListener(final boolean open) {
        for (final Iterator<PopupListener> i = popupListeners.iterator(); i.hasNext();) {
            final PopupListener listener = i.next();

            if (open) {
                listener.popupOpened();
            } else {
                listener.popupClosed();
            }
        }
    }

    /**
     * Removes the given listener from being notified when the popup is opened.
     *
     * @param listener
     *        the listener to remove
     */
    public void removePopupListener(final PopupListener listener) {
        popupListeners.remove(listener);
    }

    /* Listen on the popup for when to close the shell */

    private class PopupShellListener implements Listener {
        @Override
        public void handleEvent(final Event event) {
            if (isDisposed()) {
                return;
            }

            if (event.type == SWT.Deactivate) {
                closePopup();
            } else if (event.type == SWT.Paint) {
                final Rectangle shellBounds = popupShell.getBounds();
                final Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
                event.gc.setForeground(black);
                event.gc.drawRectangle(0, 0, shellBounds.width - 1, shellBounds.height - 1);
            }

            lastEvent = event;
        }
    }

    /* Handle focus events from the controls */

    private class ComponentFocusListener implements Listener {
        @Override
        public void handleEvent(final Event e) {
            if (!isDisposed()) {
                handleFocus();
            }
        }
    }

    /*
     * Handle clicks that don't occur in our control: OS X misses these. =( For
     * example: an abstract combo in a tab folder will miss clicks on the tab
     * folder disclosure buttons. We catch these here to ensure we lose focus.
     */

    private class NonComponentSelectionListener implements Listener {
        @Override
        public void handleEvent(final Event e) {
            if (isDisposed()) {
                return;
            }

            /*
             * Don't do anything if the selected control is one of our controls
             * -- either the text box, the disclosure button, the popup shell or
             * any component contained therein.
             */
            if (e.widget instanceof Control && isOurControl((Control) e.widget)) {
                return;
            }

            /*
             * Don't do anything if the selected control is our immediate parent
             */
            if (e.widget instanceof Composite && getParent() == (Composite) e.widget) {
                return;
            }

            notifyListeners(SWT.FocusOut, new Event());
        }
    }

    /* Handle clicks on the reveal control */

    private class RevealOpenListener extends SelectionAdapter {
        @Override
        public void widgetSelected(final SelectionEvent e) {
            openPopup();
        }
    }

    /**
     * AbstractComboButton is a class for Mac OS X that implements a disclosure
     * button. It overrides ImageButton, which provides much of the
     * functionality.
     */
    private class AbstractComboButton extends ImageButton {
        private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        private final Image enabledImage;
        private final Image disabledImage;
        private final Image depressedImage;
        private final Image focusImage;

        public AbstractComboButton(final Composite parent, final int style) {
            super(parent, style);

            if (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) {
                enabledImage = imageHelper.getImage("images/generic/maccombo_cocoa/combo_disclosure_mac.gif"); //$NON-NLS-1$
                disabledImage = imageHelper.getImage("images/generic/maccombo_cocoa/combo_disclosure_disabled_mac.gif"); //$NON-NLS-1$
                depressedImage =
                    imageHelper.getImage("images/generic/maccombo_cocoa/combo_disclosure_depressed_mac.gif"); //$NON-NLS-1$
                focusImage = imageHelper.getImage("images/generic/maccombo_cocoa/combo_disclosure_focus_mac.gif"); //$NON-NLS-1$
            } else {
                enabledImage = imageHelper.getImage("images/generic/maccombo/combo_disclosure_mac.gif"); //$NON-NLS-1$
                disabledImage = imageHelper.getImage("images/generic/maccombo/combo_disclosure_disabled_mac.gif"); //$NON-NLS-1$
                depressedImage = imageHelper.getImage("images/generic/maccombo/combo_disclosure_depressed_mac.gif"); //$NON-NLS-1$
                focusImage = imageHelper.getImage("images/generic/maccombo/combo_disclosure_focus_mac.gif"); //$NON-NLS-1$
            }

            setEnabledImage(enabledImage);
            setDisabledImage(disabledImage);
            setDepressedImage(depressedImage);

            /*
             * This dispose listener disposes images we created (above).
             */
            addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    imageHelper.dispose();
                }
            });
        }

        /**
         * Sets the associated text control.
         *
         * @param textControl
         *        The text control which makes up this combo box.
         */
        public void setTextControl(final Text textControl) {
            /*
             * Give focus to the text field whenever the disclosure button is
             * clicked in. This will ensure that the text control "glows"
             * appropriately to match the disclosure control.
             */
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDown(final MouseEvent e) {
                    textControl.setFocus();
                }
            });

            /*
             * Setup a focus listener on the text field. This will change the
             * button's image to the focus image - which is drawn with the blue
             * "glow" around it.
             */
            text.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(final FocusEvent e) {
                    setEnabledImage(focusImage);
                    redraw();
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    setEnabledImage(enabledImage);
                    redraw();
                }
            });
        }

        /*
         * Override the mouse down hook. We check the timestamp on the mouse
         * down event. If you dispose the dropdown shell by clicking on this
         * disclosure button, the mouse event's timestamp will be less than or
         * equal to the deactivate event's timestamp. Thus we test that to throw
         * out clicks which lead to deactivate events for the shell. (Without
         * doing this, we would open the shell again.)
         */
        @Override
        protected boolean hookMouseDown(final MouseEvent e) {
            if (lastEvent != null && lastEvent.type == SWT.Deactivate && lastEvent.time >= e.time) {
                return false;
            }

            return true;
        }
    }

    /* Activation Listener - notified when popup is opened */

    public interface PopupListener {
        public void popupOpened();

        public void popupClosed();
    }
}
