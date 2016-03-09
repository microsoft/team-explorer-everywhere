// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckoutWithPromptTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class CheckoutAction extends ExtendedAction {
    public CheckoutAction() {
        super();
        setName(Messages.getString("CheckoutAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.CAN_CHECKOUT_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.CAN_CHECKOUT_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        /*
         * Enable recursion when building the item specs to allow folder
         * checkout.
         */
        final CheckoutWithPromptTask task = new CheckoutWithPromptTask(
            getShell(),
            selectionInfo.getRepositories()[0],
            PluginResourceHelpers.typedItemSpecsForResources(
                selectionInfo.getResources(),
                true,
                LocationUnavailablePolicy.IGNORE_RESOURCE));

        task.run();
    }
}