// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnlockTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class UnlockAction extends ExtendedAction {
    public UnlockAction() {
        super();
        setName(Messages.getString("UnlockAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        /*
         * Only enabled when there are lock pending changes for the selection.
         */
        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(
                getSelection(),
                PluginResourceFilters.HAS_LOCK_PENDING_CHANGES_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.HAS_LOCK_PENDING_CHANGES_FILTER,
            true);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource[] lockedResources = selectionInfo.getResources();

        /*
         * Unlock selection behavior should be recursive. If a folder is being
         * unlocked, we should unlock all the locked files inside that folder.
         */
        new UnlockTask(
            getShell(),
            selectionInfo.getRepositories()[0],
            PluginResourceHelpers.typedItemSpecsForResources(
                lockedResources,
                true,
                LocationUnavailablePolicy.IGNORE_RESOURCE)).run();
    }
}