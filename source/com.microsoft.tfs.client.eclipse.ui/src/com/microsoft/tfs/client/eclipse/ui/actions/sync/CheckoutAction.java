// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckoutWithPromptTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class CheckoutAction extends SynchronizeAction {

    public CheckoutAction(final Shell shell) {
        super(shell);

        setText(Messages.getString("CheckoutAction.ActionText")); //$NON-NLS-1$
        setImageDescriptor(
            getImageHelper().getImageDescriptor(
                "platform:/plugin/com.microsoft.tfs.client.common.ui/images/vc/checkout.gif")); //$NON-NLS-1$
    }

    @Override
    public void addToContextMenu(final IMenuManager manager, final IResource[] selected) {
        super.addToContextMenu(manager, selected);

        manager.add(this);

        setEnabled(ActionHelpers.filterAcceptsAnyResource(selected, PluginResourceFilters.CAN_CHECKOUT_FILTER));
    }

    @Override
    public void run() {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getStructuredSelection(),
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
