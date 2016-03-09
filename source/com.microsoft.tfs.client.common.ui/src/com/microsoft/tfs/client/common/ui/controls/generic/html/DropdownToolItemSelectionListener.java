// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.html;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import com.microsoft.tfs.util.Check;

/**
 * Install one of these on a tool bar {@link ToolItem} which has the
 * {@link SWT#DROP_DOWN} style bit set in order to pop up a menu for the user to
 * select from.
 *
 * @threadsafety unknown
 */
class DropdownToolItemSelectionListener extends SelectionAdapter {
    /**
     * Handles a menu item being selected.
     *
     * @threadsafety unknown
     */
    public interface MenuItemSelectedHandler {
        /**
         * The given {@link MenuItem} was selected.
         *
         * @param menuItem
         *        the menu item selected (must not be <code>null</code>)
         */
        public void onMenuItemSelected(final MenuItem menuItem);
    }

    /**
     * Key used to route tooltip text through menu items (which don't store
     * tooltip text) so they can be set on the tool item when the selection
     * changes.
     */
    private final static String TOOLTIP_TEXT_WIDGET_DATA_KEY =
        DropdownToolItemSelectionListener.class.getName() + ".toolTipText"; //$NON-NLS-1$

    /**
     * Key used to store the click handler class in menu items so we can invoke
     * selections on menu items without synthesizing {@link SelectionEvent}s.
     */
    private final static String MENU_ITEM_SELECTED_HANDLER_WIDGET_DATA_KEY =
        DropdownToolItemSelectionListener.class.getName() + ".menuItemSelectedHandler"; //$NON-NLS-1$

    /**
     * The ToolItem with {@link SWT#DROP_DOWN} which triggers the menu.
     */
    private final ToolItem toolItem;

    /**
     * {@link Menu} created on construction.
     */
    private final Menu menu;

    /**
     * Saves most recent {@link MenuItem} the user chose from the popup, so when
     * the user clicks on the {@link ToolItem} itself (not the menu) we can
     * perform the last action.
     */
    private MenuItem lastSelectedMenuItem;

    /**
     * Constructs a {@link DropdownToolItemSelectionListener} for the given
     * {@link ToolItem}
     *
     * @param toolItem
     *        the {@link ToolItem} to handle selection events for (must not be
     *        <code>null</code>)
     */
    public DropdownToolItemSelectionListener(final ToolItem toolItem) {
        Check.notNull(toolItem, "toolItem"); //$NON-NLS-1$

        this.toolItem = toolItem;
        menu = new Menu(toolItem.getParent().getShell());
    }

    /**
     * Adds an item to the menu shown when
     * {@link #widgetSelected(SelectionEvent)} is invoked.
     *
     * This class does <b>not</b> dispose the given {@link Image}.
     *
     * @param text
     *        the text of the menu item (may be <code>null</code>)
     * @param image
     *        the image for the menu item (may be <code>null</code>)
     * @param toolTipText
     *        tool tip text to use for the tool item when this menu item is the
     *        selected item (may be <code>null</code>)
     * @param menuItemStyle
     *        style for the {@link MenuItem}
     * @param menuSelectedHandler
     *        handles when the menu is selected (must not be <code>null</code>)
     * @return the {@link MenuItem} added so the caller can listen for events,
     *         etc. (never <code>null</code>)
     */
    public MenuItem addMenuItem(
        final String text,
        final Image image,
        final String toolTipText,
        final int menuItemStyle,
        final MenuItemSelectedHandler menuSelectedHandler) {
        Check.notNull(menuSelectedHandler, "clickHandler"); //$NON-NLS-1$

        final MenuItem menuItem = new MenuItem(menu, menuItemStyle);
        menuItem.setText(text);
        menuItem.setImage(image);
        menuItem.setData(TOOLTIP_TEXT_WIDGET_DATA_KEY, toolTipText);
        menuItem.setData(MENU_ITEM_SELECTED_HANDLER_WIDGET_DATA_KEY, menuSelectedHandler);

        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                /*
                 * Simply update the tool item with the text, image, and tooltip
                 * text from this menu item. The tool item will fire an event
                 * the user can handle to detect the switch.
                 */
                final MenuItem selected = (MenuItem) event.widget;

                setDefaultToolItem(selected);

                menuSelectedHandler.onMenuItemSelected(menuItem);
            }
        });

        return menuItem;
    }

    public void setDefaultToolItem(final MenuItem menuItem) {
        Check.notNull(menuItem, "menuItem"); //$NON-NLS-1$

        toolItem.setImage(menuItem.getImage());

        if (menuItem.getData(TOOLTIP_TEXT_WIDGET_DATA_KEY) != null) {
            toolItem.setToolTipText((String) menuItem.getData(TOOLTIP_TEXT_WIDGET_DATA_KEY));
        } else {
            toolItem.setToolTipText(""); //$NON-NLS-1$
        }

        // Remember this menu item for future invocations
        lastSelectedMenuItem = menuItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void widgetSelected(final SelectionEvent event) {
        if (event.detail == SWT.ARROW) {
            /*
             * Show the menu.
             */
            final ToolItem item = (ToolItem) event.widget;
            final Rectangle itemRectangle = item.getBounds();

            final Point point = item.getParent().toDisplay(new Point(itemRectangle.x, itemRectangle.y));
            menu.setLocation(point.x, point.y + itemRectangle.height);
            menu.setVisible(true);
        } else {
            /*
             * Select the menu item
             */
            if (lastSelectedMenuItem != null) {
                final MenuItemSelectedHandler handler =
                    (MenuItemSelectedHandler) lastSelectedMenuItem.getData(MENU_ITEM_SELECTED_HANDLER_WIDGET_DATA_KEY);
                handler.onMenuItemSelected(lastSelectedMenuItem);
            }
        }
    }
}