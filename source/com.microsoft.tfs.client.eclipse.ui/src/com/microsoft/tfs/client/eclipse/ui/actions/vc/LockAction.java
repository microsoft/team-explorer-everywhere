// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.vc.LockTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class LockAction extends ExtendedAction {
    public LockAction() {
        super();
        setName(Messages.getString("LockAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        /*
         * This action is enabled even for items which are already locked, so
         * the user can change the lock level.
         */
        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(
                getSelection(),
                PluginResourceFilters.HAS_PENDING_CHANGES_OR_IN_REPOSITORY_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        /*
         * Get the full selection.
         */
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.HAS_PENDING_CHANGES_OR_IN_REPOSITORY_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        /*
         * Lock selection behavior should be *non-recursive*, because TFS locks
         * can happen directly on folders without the need for locking subitems.
         */
        new LockTask(
            getShell(),
            selectionInfo.getRepositories()[0],
            PluginResourceHelpers.typedItemSpecsForResources(
                selectionInfo.getResources(),
                false,
                LocationUnavailablePolicy.IGNORE_RESOURCE)).run();
    }
}