// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.client.eclipse.ui.tasks.vc.TPUnignoreTask;

/**
 * Removes patterns from .tpignore files, but <b>not</b> .tfignore files (this
 * is not supported by VS either).
 */
public class UnignoreAction extends ExtendedAction {
    public UnignoreAction() {
        super();
        setName(Messages.getString("UnignoreAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        /*
         * Don't test for .tfignore filters and conditions, only .tpignore.
         *
         * Enable for any items which are in a server workspace and are
         * currently ignored.
         *
         * Allow only 1 repository in the selection.
         */
        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(selection, PluginResourceFilters.IN_SERVER_WORKSPACE_FILTER)
                && ActionHelpers.filterAcceptsAnyResource(selection, PluginResourceFilters.TPIGNORE_FILTER_INVERSE)
                && ActionHelpers.getRepositoriesFromSelection(selection).length == 1);
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.TPIGNORE_FILTER_INVERSE,
            false);

        /*
         * Ensure a single project. Skip the usual single-repository requirement
         * because offline resources won't be mapped to any repository and we
         * want to support offline resources.
         */
        if (ActionHelpers.ensureSingleProject(selectionInfo, getShell()) == false) {
            return;
        }

        new TPUnignoreTask(getShell(), selectionInfo.getResources()).run();
    }
}
