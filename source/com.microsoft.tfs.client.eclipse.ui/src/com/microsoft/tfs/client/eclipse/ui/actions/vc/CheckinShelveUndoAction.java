// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.Task;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public abstract class CheckinShelveUndoAction extends ExtendedAction {
    public CheckinShelveUndoAction() {
        super();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.HAS_PENDING_CHANGES_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.HAS_PENDING_CHANGES_FILTER,
            true);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        createTask(selectionInfo).run();
    }

    protected abstract Task createTask(final AdaptedSelectionInfo selectionInfo);
}