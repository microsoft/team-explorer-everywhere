// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.menubutton;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.ImageButton;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.util.Check;

public class MacMenuButton extends MenuButton {
    private final Button mainButton;
    private final ImageButton menuButton;

    /* Is this control enabled? */
    private boolean enabled = true;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private final Image enabledImage;
    private final Image depressedImage;
    private final Image focusEnabledImage;
    private final Image focusDepressedImage;
    private final Image disabledImage;

    public MacMenuButton(final Composite parent, final int style) {
        super(parent, style);

        /* Setup cocoa vs carbon images */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA)) {
            enabledImage = imageHelper.getImage("images/generic/macmenubutton_cocoa/dropdown_button_mac.gif"); //$NON-NLS-1$
            depressedImage =
                imageHelper.getImage("images/generic/macmenubutton_cocoa/dropdown_button_depressed_mac.gif"); //$NON-NLS-1$
            focusEnabledImage =
                imageHelper.getImage("images/generic/macmenubutton_cocoa/dropdown_button_focus_mac.gif"); //$NON-NLS-1$
            focusDepressedImage =
                imageHelper.getImage("images/generic/macmenubutton_cocoa/dropdown_button_focus_depressed_mac.gif"); //$NON-NLS-1$
            disabledImage = imageHelper.getImage("images/generic/macmenubutton_cocoa/dropdown_button_disabled_mac.gif"); //$NON-NLS-1$
        } else {
            enabledImage = imageHelper.getImage("images/generic/macmenubutton/dropdown_button_mac.gif"); //$NON-NLS-1$
            depressedImage = imageHelper.getImage("images/generic/macmenubutton/dropdown_button_depressed_mac.gif"); //$NON-NLS-1$
            focusEnabledImage = imageHelper.getImage("images/generic/macmenubutton/dropdown_button_focus_mac.gif"); //$NON-NLS-1$
            focusDepressedImage =
                imageHelper.getImage("images/generic/macmenubutton/dropdown_button_focus_depressed_mac.gif"); //$NON-NLS-1$
            disabledImage = imageHelper.getImage("images/generic/macmenubutton/dropdown_button_disabled_mac.gif"); //$NON-NLS-1$
        }

        setLayout(new FormLayout());

        mainButton = new Button(this, SWT.PUSH);

        final FormData mainButtonData = new FormData();
        mainButtonData.left = new FormAttachment(0, 0);
        mainButtonData.top = new FormAttachment(0, 0);

        mainButton.setLayoutData(mainButtonData);

        /*
         * Draw the drop-down image on top of the right side of the button such
         * that it appears to be a single widget.
         */
        menuButton = new ImageButton(this, SWT.NONE);
        menuButton.setEnabledImage(enabledImage);
        menuButton.setDepressedImage(depressedImage);
        menuButton.setDisabledImage(disabledImage);

        /* Hack: in SWT 3.7, there's an extra pixel on the top of a button. */
        final int leftPosition = WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA) ? -12 : -15;
        final int topPosition =
            (WindowSystem.isCurrentWindowSystem(WindowSystem.COCOA) && SWT.getVersion() >= 3700) ? -1 : 0;

        final FormData menuButtonData = new FormData();
        menuButtonData.top = new FormAttachment(mainButton, topPosition, SWT.TOP);
        menuButtonData.left = new FormAttachment(mainButton, leftPosition, SWT.RIGHT);

        menuButton.setLayoutData(menuButtonData);
        menuButton.moveAbove(mainButton);

        mainButton.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN) {
                    openMenu();
                }
            }
        });

        /*
         * Hook up a focus listener to the main button to swap the image of the
         * menu button.
         */
        mainButton.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                menuButton.setEnabledImage(focusEnabledImage);
                menuButton.setDepressedImage(focusDepressedImage);
                redraw();
            }

            @Override
            public void focusLost(final FocusEvent e) {
                menuButton.setEnabledImage(enabledImage);
                menuButton.setDepressedImage(depressedImage);
                redraw();
            }
        });

        /*
         * Hook up a selection listener on the menu button which fires the
         * appropriate selection event to *our* listeners
         */
        menuButton.addSelectionListener(new SelectionAdapter() {
            /* Fire a new event from *this* widget */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                openMenu();
            }
        });

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });
    }

    @Override
    public Button getButton() {
        return mainButton;
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.widgets.disclosurebutton.
     * DisclosureButton#setText(java.lang.String)
     */
    @Override
    public void setText(final String text) {
        mainButton.setText(text);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;

        mainButton.setEnabled(enabled);
        menuButton.setEnabled(enabled);
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
        mainButton.addSelectionListener(listener);
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
        mainButton.removeSelectionListener(listener);
    }
}
