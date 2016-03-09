// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.menubutton;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

public abstract class MenuButton extends Composite {
    private final MenuManager menuManager;
    private final Menu menu;

    public MenuButton(final Composite parent, final int style) {
        super(parent, style);

        /* Create the menu button */
        menuManager = new MenuManager("#popup"); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown(true);

        menu = menuManager.createContextMenu(this);
    }

    /**
     * Sets the button label.
     *
     * @param string
     *        the new text
     */
    public abstract void setText(String text);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
     */
    @Override
    public abstract void setEnabled(boolean enabled);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.swt.widgets.Control#isEnabled()
     */
    @Override
    public abstract boolean isEnabled();

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the control is selected by the user, by sending it one of the
     * messages defined in the <code>SelectionListener</code> interface.
     *
     * @see SelectionListener
     * @see #removeSelectionListener
     * @see SelectionEvent
     *
     * @param listener
     *        the listener which should be notified
     *
     */
    public abstract void addSelectionListener(SelectionListener listener);

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the control is selected by the user.
     *
     * @see SelectionListener
     * @see #addSelectionListener
     *
     * @param listener
     *        the listener which should no longer be notified
     */
    public abstract void removeSelectionListener(SelectionListener listener);

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.action.IMenuManager#addMenuListener(org.eclipse.jface
     * .action.IMenuListener)
     */
    public void addMenuListener(final IMenuListener listener) {
        menuManager.addMenuListener(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.action.IMenuManager#removeMenuListener(org.eclipse.
     * jface.action.IMenuListener)
     */
    public void removeMenuListener(final IMenuListener listener) {
        menuManager.removeMenuListener(listener);
    }

    /**
     * @return the Button control underlying this menu button
     */
    public abstract Button getButton();

    /**
     * Opens the menu. Implementors should call this when the disclosure
     * triangle is selected.
     */
    protected void openMenu() {
        menu.setLocation(toDisplay(new Point(0, getClientArea().height)));
        menu.setVisible(true);
    }
}
