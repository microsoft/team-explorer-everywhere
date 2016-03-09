// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public abstract class AbstractCheckinSubControl extends BaseControl {
    private final CheckinSubControlType type;
    private final Map<String, IAction> globalActions = new HashMap<String, IAction>();

    private final SingleListenerFacade titleChangedListeners =
        new SingleListenerFacade(CheckinSubControlTitleChangedListener.class);

    private String title;
    private ISelectionProvider selectionProvider;
    private MenuManager contextMenu;
    private IContributionManager contributionManager;

    protected AbstractCheckinSubControl(
        final Composite parent,
        final int style,
        final String title,
        final CheckinSubControlType type) {
        super(parent, style);
        this.title = title;
        this.type = type;
    }

    public CheckinSubControlType getSubControlType() {
        return type;
    }

    public void addTitleChangedListener(final CheckinSubControlTitleChangedListener listener) {
        titleChangedListeners.addListener(listener);
    }

    public void removeTitleChangedListener(final CheckinSubControlTitleChangedListener listener) {
        titleChangedListeners.removeListener(listener);
    }

    public String getTitle() {
        return title;
    }

    public ISelectionProvider getSelectionProvider() {
        return selectionProvider;
    }

    public MenuManager getContextMenu() {
        return contextMenu;
    }

    public void hookGlobalActions(final IActionBars actionBars) {
        for (final Entry<String, IAction> globalAction : globalActions.entrySet()) {
            actionBars.setGlobalActionHandler(globalAction.getKey(), globalAction.getValue());
        }
    }

    public IContributionManager getContributionManager() {
        return contributionManager;
    }

    public abstract void addContributions(IContributionManager contributionManager, String groupName);

    public abstract void removeContributions(IContributionManager contributionManager, String groupname);

    protected final void setSelectionProvider(final ISelectionProvider selectionProvider) {
        this.selectionProvider = selectionProvider;
    }

    protected final void setTitle(final String title) {
        this.title = title;
        final CheckinSubControlTitleChangedListener listener =
            (CheckinSubControlTitleChangedListener) titleChangedListeners.getListener();
        listener.onTitleChanged(new CheckinSubControlEvent(this));
    }

    protected final void setContextMenu(final MenuManager contextMenu) {
        this.contextMenu = contextMenu;
    }

    protected final void registerGlobalActionHandler(final String actionId, final IAction handler) {
        globalActions.put(actionId, handler);
    }

    protected final void setContributionManager(final IContributionManager contributionManager) {
        this.contributionManager = contributionManager;
    }
}
