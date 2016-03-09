// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolItem;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ToolbarPulldownAction} is an {@link IAction} implementation designed
 * to be used to implement toolbar items with a pull-down menu. Such items have
 * an interface similar to the run and debug toolbar items that are present in
 * the main Eclipse toolbar.
 * </p>
 *
 * <p>
 * To use a {@link ToolbarPulldownAction}, follow these steps:
 * <ul>
 * <li>Instantiate the {@link ToolbarPulldownAction}</li>
 * <li>Set the text, tooltip, and/or image (if desired) using standard
 * {@link Action} methods</li>
 * <li>Add each sub-action that should appear when the pull-down menu is shown
 * by calling {@link #addSubAction(IAction)}</li>
 * <li>If desired, set the default sub-action that will be performed when
 * clicking the {@link ToolbarPulldownAction} by calling
 * {@link #setDefaultSubAction(IAction)}</li>
 * <li>Add the {@link ToolbarPulldownAction} to a toolbar, as you would with any
 * other action</li>
 * </ul>
 * </p>
 *
 * <p>
 * The enablement of a {@link ToolbarPulldownAction} is based on the contained
 * sub-actions. The {@link ToolbarPulldownAction} is initially disabled since it
 * initially has no sub-actions. As sub-actions are added and removed from a
 * {@link ToolbarPulldownAction}, the enablement state is automatically updated.
 * If at least one sub-action is enabled, the {@link ToolbarPulldownAction} will
 * be enabled. An {@link IPropertyChangeListener} is registered with contained
 * sub-actions, and when the sub-action enablement changes, the enablement of
 * the {@link ToolbarPulldownAction} is automatically updated.
 * </p>
 *
 * <p>
 * Note that each time a sub-action is invoked from the pull-down menu, that
 * sub-action becomes the default sub-action that will be invoked if the
 * {@link ToolbarPulldownAction} is clicked directly. The
 * {@link ToolbarPulldownAction} takes its tooltip text from the default
 * sub-action, if any. Also note that sub-actions can be added or removed at any
 * time during the lifetime of the {@link ToolbarPulldownAction}.
 * </p>
 */
public class ToolbarPulldownAction extends Action {
    /**
     * When true the drop down menu will be shown when clicking the button
     * portion portion of the control. When false a the default action will be
     * run when clicking the button.
     */
    private final boolean showMenuForDefaultAction;

    /**
     * The {@link List} of {@link IAction}s that are the sub-actions of this
     * {@link ToolbarPulldownAction}.
     */
    private final List<IAction> subActions = new ArrayList<IAction>();

    /**
     * Used to track which sub-action is currently the default sub-action, by
     * keeping its index in the <code>subActions</code> list. If no sub-action
     * is currently default, this field will be <code>-1</code>.
     */
    private int defaultSubActionIndex = -1;

    /**
     * The cached {@link Menu} used to satisfy the {@link IMenuCreator}
     * interface. This {@link Menu} will be rebuilt as necessary if sub-actions
     * are added and removed.
     */
    private Menu subActionMenu;

    /**
     * A {@link SelectionListener} added to {@link MenuItem}s in the pull-down
     * menu. This listener is used to track when a sub-action has been invoked
     * so we can set that sub-action as the default sub-action.
     */
    private SelectionListener menuItemSelectionListener;

    /**
     * An {@link IPropertyChangeListener} added to contained sub-actions. This
     * listener is used to recompute the enablement when a sub-action has
     * changed enablement.
     */
    private final IPropertyChangeListener subActionPropertyChangeListener;

    /**
     * Creates a new {@link ToolbarPulldownAction} with no initial decoration
     * (text, tooltip, image, etc). The next steps are added decoration by
     * calling {@link #setText(String)}, etc, and adding sub-actions by calling
     * {@link #addSubAction(IAction)}.
     */
    public ToolbarPulldownAction() {
        this(false);
    }

    public ToolbarPulldownAction(final boolean showMenuForDefaultAction) {
        super(null, IAction.AS_DROP_DOWN_MENU);
        this.showMenuForDefaultAction = showMenuForDefaultAction;

        setEnabled(false);

        setMenuCreator(new IMenuCreator() {
            @Override
            public void dispose() {
                disposeSubActionMenu();
            }

            @Override
            public Menu getMenu(final Control parent) {
                return getSubActionMenu(parent);
            }

            @Override
            public Menu getMenu(final Menu parent) {
                return null;
            }
        });

        menuItemSelectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (!e.widget.isDisposed()) {
                    final ActionContributionItem aci = (ActionContributionItem) e.widget.getData();
                    final IAction subAction = aci.getAction();
                    setDefaultSubAction(subAction);
                }
            }
        };

        subActionPropertyChangeListener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                subActionPropertyChange(event);
            }
        };
    }

    private void subActionPropertyChange(final PropertyChangeEvent event) {
        if (IAction.ENABLED.equals(event.getProperty())) {
            recomputeEnablement();
        }
    }

    private void disposeSubActionMenu() {
        if (subActionMenu != null) {
            subActionMenu.dispose();
            subActionMenu = null;
        }
    }

    protected Menu getSubActionMenu(final Control parent) {
        if (subActionMenu != null) {
            return subActionMenu;
        }

        subActionMenu = new Menu(parent);

        for (final Iterator<IAction> it = subActions.iterator(); it.hasNext();) {
            final IAction subAction = it.next();
            final ActionContributionItem aci = new ActionContributionItem(subAction);
            aci.fill(subActionMenu, -1);
            final MenuItem menuItem = subActionMenu.getItem(subActionMenu.getItemCount() - 1);
            menuItem.addSelectionListener(menuItemSelectionListener);
        }

        return subActionMenu;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.
     * Event)
     */
    @Override
    public void runWithEvent(final Event event) {
        if (showMenuForDefaultAction) {
            final ToolItem item = (ToolItem) event.widget;
            final Rectangle itemRectangle = item.getBounds();
            final Point point = item.getParent().toDisplay(new Point(itemRectangle.x, itemRectangle.y));

            final Menu menu = getSubActionMenu(item.getParent());
            menu.setLocation(point.x, point.y + itemRectangle.height);
            menu.setVisible(true);
        } else {
            final IAction defaultSubAction = getDefaultSubAction();
            if (defaultSubAction != null && defaultSubAction.isEnabled()) {
                defaultSubAction.runWithEvent(event);
            }
        }
    }

    /**
     * Adds a new sub-action to this {@link ToolbarPulldownAction}. The
     * sub-action will be shown in the pull-down menu that is displayed for this
     * item. If this is the first sub-action being added, it will become the
     * default sub-action. You can manually set the default sub-action be
     * calling {@link #setDefaultSubAction(IAction)}.
     *
     * @param subAction
     *        a new sub-action to add (must not be <code>null</code>)
     */
    public void addSubAction(final IAction subAction) {
        Check.notNull(subAction, "subAction"); //$NON-NLS-1$

        subActions.add(subAction);
        subAction.addPropertyChangeListener(subActionPropertyChangeListener);
        recomputeEnablement();

        disposeSubActionMenu();
    }

    /**
     * Returns the list of current subActions.
     */
    public List<IAction> getSubActions() {
        return subActions;
    }

    /**
     * Removes all previously added sub-actions from this
     * {@link ToolbarPulldownAction}.
     */
    public void removeAll() {
        while (subActions.size() > 0) {
            removeSubAction(subActions.get(0));
        }
    }

    /**
     * Removes a previously added sub-action from this
     * {@link ToolbarPulldownAction}. If the sub-action being removed is the
     * default sub-action, there will be no default sub-action after removing
     * it.
     *
     * @param subActionToRemove
     *        a previously added sub-action to remove (must not be
     *        <code>null</code>)
     */
    public void removeSubAction(final IAction subActionToRemove) {
        Check.notNull(subActionToRemove, "subActionToRemove"); //$NON-NLS-1$

        final int index = subActions.indexOf(subActionToRemove);

        if (index == -1) {
            throw new IllegalArgumentException("the specified action is not contained in this ToolbarPulldownAction"); //$NON-NLS-1$
        }

        final IAction defaultSubAction = getDefaultSubAction();
        subActions.remove(subActionToRemove);
        subActionToRemove.removePropertyChangeListener(subActionPropertyChangeListener);
        recomputeEnablement();

        if (defaultSubAction != null) {
            if (defaultSubAction == subActionToRemove) {
                defaultSubActionIndex = -1;
            } else {
                defaultSubActionIndex = subActions.indexOf(defaultSubAction);
            }
        }

        disposeSubActionMenu();
    }

    /**
     * Sets a previously added sub-action as the default sub-action of this
     * {@link ToolbarPulldownAction}. The default sub-action is the sub-action
     * that is invoked when the {@link ToolbarPulldownAction} is selected
     * directly.
     *
     * @param action
     *        a previously added sub-action to set as the default sub-action
     *        (must not be <code>null</code>)
     */
    public void setDefaultSubAction(final IAction action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        final int index = subActions.indexOf(action);

        if (index == -1) {
            throw new IllegalArgumentException("the specified action is not contained in this ToolbarPulldownAction"); //$NON-NLS-1$
        }

        defaultSubActionIndex = index;

        setToolTipText(action.getToolTipText());
    }

    /**
     * @return the current default sub-action of this
     *         {@link ToolbarPulldownAction}, or <code>null</code> if there is
     *         no current sub-action
     */
    public IAction getDefaultSubAction() {
        if (defaultSubActionIndex == -1) {
            return null;
        }
        return subActions.get(defaultSubActionIndex);
    }

    private void recomputeEnablement() {
        boolean enabled = false;

        for (final Iterator<IAction> it = subActions.iterator(); !enabled && it.hasNext();) {
            final IAction action = it.next();
            enabled = action.isEnabled();
        }

        setEnabled(enabled);
    }
}
