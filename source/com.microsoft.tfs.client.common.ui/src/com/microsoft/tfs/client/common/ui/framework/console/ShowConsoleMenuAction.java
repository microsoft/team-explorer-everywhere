// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import com.microsoft.tfs.util.Check;

public class ShowConsoleMenuAction extends Action implements IMenuCreator {
    private Menu menu;

    private final List<Action> actions = new ArrayList<Action>();

    public ShowConsoleMenuAction(final String name, final ImageDescriptor imageDescriptor) {
        super(name, IAction.AS_DROP_DOWN_MENU);

        setText(name);
        setToolTipText(name);
        setImageDescriptor(imageDescriptor);

        setMenuCreator(this);
    }

    public final void addAction(final Action action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        actions.add(action);
    }

    @Override
    public Menu getMenu(final Control parent) {
        if (menu != null) {
            menu.dispose();
        }

        menu = new Menu(parent);

        for (final Action action : actions) {
            new ActionContributionItem(action).fill(menu, -1);
        }

        return menu;
    }

    @Override
    public Menu getMenu(final Menu parent) {
        return null;
    }

    @Override
    public void dispose() {
        if (menu != null) {
            menu.dispose();
        }
    }
}
