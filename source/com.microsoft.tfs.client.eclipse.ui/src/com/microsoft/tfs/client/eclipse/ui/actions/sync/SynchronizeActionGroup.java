// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionContext;

public class SynchronizeActionGroup extends SynchronizePageActionGroup {
    private ISynchronizePageSite site;
    private ISynchronizePageConfiguration configuration;

    private ExternalCompareAction customCompareAction;
    private GetLatestAction getLatestAction;
    private GetSpecificAction getSpecificAction;
    private CheckoutAction checkoutAction;
    private UndoPendingChangeAction undoAction;
    private CheckinAction checkinAction;
    private ViewHistoryAction historyAction;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.team.ui.synchronize.IActionContribution#initialize(org.
     * eclipse .team.ui.synchronize.ISynchronizePageConfiguration)
     */
    @Override
    public void initialize(final ISynchronizePageConfiguration configuration) {
        super.initialize(configuration);

        site = configuration.getSite();
        final IWorkbenchSite ws = site.getWorkbenchSite();

        if (ws instanceof IViewSite) {
            this.configuration = configuration;

            final Shell shell = site.getShell();

            customCompareAction = new ExternalCompareAction(shell);

            undoAction = new UndoPendingChangeAction(shell);

            getLatestAction = new GetLatestAction(shell);
            getSpecificAction = new GetSpecificAction(shell);
            checkoutAction = new CheckoutAction(shell);
            undoAction = new UndoPendingChangeAction(shell);
            checkinAction = new CheckinAction(shell);
            historyAction = new ViewHistoryAction(shell);
        } else {
            this.configuration = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#fillActionBars
     * (org.eclipse.ui.IActionBars)
     */
    @Override
    public void fillActionBars(final IActionBars actionBars) {
        if (configuration == null) {
            return;
        }

        // we can only override double-click handling in Eclipse 3.1 and
        // later. 3.0 has a bug where it fires two open events. =(
        if (SWT.getVersion() < 3100) {
            return;
        }

        // HACK: this should be done in
        // initialize(ISynchronizePageConfiguration)
        // above. but that would get clobbered by
        // DefaultSynchronizePageActions.initialize(ISynchronizePageConfiguration)
        //
        // fillActionBars() is called after initialize(), so we do this here to
        // clobber *their* action. oy.
        //
        // steal double-click handling for ourselves, launch external
        // compare tool if configured and fall-back to showing the
        // normal compare tool

        final Object oldProperty = configuration.getProperty(SynchronizePageConfiguration.P_OPEN_ACTION);

        // make sure that we're actually doing this sanely...
        if (customCompareAction == null || !(oldProperty instanceof Action)) {
            return;
        }

        final Action defaultCompareAction = (Action) oldProperty;

        configuration.setProperty(SynchronizePageConfiguration.P_OPEN_ACTION, new Action() {
            @Override
            public void run() {
                final IResource[] selection = getSelection();

                /*
                 * Refresh the selection before asking about external tools.
                 */
                customCompareAction.setSelectedResources(selection);

                if (customCompareAction.hasExternalToolForSelection()) {
                    customCompareAction.run();
                } else {
                    defaultCompareAction.run();
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.actions.ActionGroup#updateActionBars()
     */
    @Override
    public void updateActionBars() {
    }

    public IResource[] getSelection() {
        final ISelection baseSelection = site.getSelectionProvider().getSelection();

        if (!(baseSelection instanceof IStructuredSelection)) {
            return null;
        }

        final IStructuredSelection selection = (IStructuredSelection) baseSelection;

        // exactly one file must be selected for this functionality to exist
        if (selection == null || selection.size() == 0 || !(selection instanceof IStructuredSelection)) {
            return null;
        }

        final Object[] elements = selection.toArray();
        final IResource resources[] = Utils.getResources(elements);

        return resources;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.IActionContribution#fillContextMenu(org
     * .eclipse.jface.action.IMenuManager)
     */
    @Override
    public void fillContextMenu(final IMenuManager manager) {
        final IResource[] resources = getSelection();

        if (resources == null) {
            return;
        }

        // actions only contributed for one selected item
        if (resources.length == 1) {
            customCompareAction.addToContextMenu(manager, resources);
        }

        manager.add(new Separator());

        // actions always contributed
        getLatestAction.addToContextMenu(manager, resources);
        getSpecificAction.addToContextMenu(manager, resources);

        manager.add(new Separator());

        checkoutAction.addToContextMenu(manager, resources);
        undoAction.addToContextMenu(manager, resources);

        manager.add(new Separator());

        checkinAction.addToContextMenu(manager, resources);

        manager.add(new Separator());

        historyAction.addToContextMenu(manager, resources);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.team.ui.synchronize.SynchronizePageActionGroup#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.actions.ActionGroup#setContext(org.eclipse.ui.actions.
     * ActionContext)
     */
    @Override
    public void setContext(final ActionContext context) {
    }
}
