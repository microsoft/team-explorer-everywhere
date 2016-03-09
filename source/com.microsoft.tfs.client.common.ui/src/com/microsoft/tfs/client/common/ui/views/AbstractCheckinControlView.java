// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemProvider;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.AbstractCheckinSubControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControlOptions;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlListener;
import com.microsoft.tfs.client.common.ui.framework.action.StandardActionConstants;

public abstract class AbstractCheckinControlView extends ViewPart {
    private CheckinControl checkinControl;
    private ChangeItemProvider changeItemProvider;

    @Override
    public void createPartControl(final Composite parent) {
        final IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();

        setupToolbar(toolbar);

        final CheckinControlOptions options = new CheckinControlOptions();
        options.setExternalContributionManager(toolbar);
        options.setForDialog(false);
        setupCheckinControlOptions(options);

        checkinControl = new CheckinControl(parent, SWT.NONE, options);

        registerSubControlContextMenus();

        getViewSite().setSelectionProvider(checkinControl);

        createActions();
        contributeActions();

        checkinControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                updateStatusLine((IStructuredSelection) event.getSelection());
            }
        });

        checkinControl.addSubControlListener(new CheckinSubControlListener() {
            @Override
            public void onSubControlHidden(final CheckinSubControlEvent event) {
                subControlHidden(event.getControl());
            }

            @Override
            public void onSubControlVisible(final CheckinSubControlEvent event) {
                subControlShown(event.getControl());
            }
        });

        if (checkinControl.getVisibleSubControl() != null) {
            subControlShown(checkinControl.getVisibleSubControl());
        }
    }

    @Override
    public void setFocus() {
        if (checkinControl != null && !checkinControl.isDisposed()) {
            checkinControl.setFocus();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        disposeChangeItemProvider();
    }

    protected abstract void setupCheckinControlOptions(CheckinControlOptions options);

    protected abstract String getStatusLineMessage(IStructuredSelection selection);

    protected abstract void createActions();

    protected abstract void contributeActions();

    protected abstract void onSubControlShown(AbstractCheckinSubControl subControl);

    protected abstract void onSubControlHidden(AbstractCheckinSubControl subControl);

    public final CheckinControl getCheckinControl() {
        return checkinControl;
    }

    protected final void setChangeItemProvider(final ChangeItemProvider provider) {
        disposeChangeItemProvider();
        changeItemProvider = provider;
        checkinControl.setChangeItemProvider(provider);
    }

    private void subControlShown(final AbstractCheckinSubControl subControl) {
        subControl.hookGlobalActions(getViewSite().getActionBars());

        onSubControlShown(subControl);
    }

    private void subControlHidden(final AbstractCheckinSubControl subControl) {
        getViewSite().getActionBars().clearGlobalActionHandlers();

        onSubControlHidden(subControl);
    }

    protected void updateStatusLine() {
        updateStatusLine((IStructuredSelection) checkinControl.getSelection());
    }

    private void updateStatusLine(final IStructuredSelection selection) {
        final String message = getStatusLineMessage(selection);
        getViewSite().getActionBars().getStatusLineManager().setMessage(message);
    }

    private void registerSubControlContextMenus() {
        final AbstractCheckinSubControl[] subControls = checkinControl.getSubControls();

        for (int i = 0; i < subControls.length; i++) {
            final MenuManager contextMenu = subControls[i].getContextMenu();
            final ISelectionProvider selectionProvider = subControls[i].getSelectionProvider();

            if (contextMenu != null && selectionProvider != null) {
                addAdditionsGroupToContextMenu(contextMenu);
                getViewSite().registerContextMenu(
                    getViewSite().getId() + "." + subControls[i].getSubControlType().getID(), //$NON-NLS-1$
                    contextMenu,
                    selectionProvider);
            }
        }
    }

    private void addAdditionsGroupToContextMenu(final MenuManager contextMenu) {
        contextMenu.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                if (manager.find(IWorkbenchActionConstants.MB_ADDITIONS) != null) {
                    return;
                }

                final String groupId = StandardActionConstants.HOSTING_CONTROL_CONTRIBUTIONS;

                manager.appendToGroup(groupId, new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
    }

    private void disposeChangeItemProvider() {
        if (changeItemProvider != null) {
            changeItemProvider.dispose();
            changeItemProvider = null;
        }
    }

    protected void setupToolbar(final IToolBarManager toolbar) {
        toolbar.add(new Separator(CheckinControl.SUBCONTROL_CONTRIBUTION_GROUP_NAME));
        toolbar.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }
}
