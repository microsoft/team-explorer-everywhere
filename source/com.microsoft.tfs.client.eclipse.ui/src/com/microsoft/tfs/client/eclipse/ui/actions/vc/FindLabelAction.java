// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.vc.FindLabelTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class FindLabelAction extends ExtendedAction {
    public FindLabelAction() {
        super();
        setName(Messages.getString("FindLabelAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        /*
         * Finding labels pretty much ignores the selection once the dialog is
         * raised, so allow for non-repository resources.
         */
        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.STANDARD_FILTER)
                && ActionHelpers.getRepositoriesFromSelection(getSelection()).length == 1);
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.STANDARD_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource resource = selectionInfo.getResources()[0];

        if (resource.getLocation() == null) {
            return;
        }

        final String serverPath =
            selectionInfo.getRepositories()[0].getWorkspace().getMappedServerPath(resource.getLocation().toOSString());

        new FindLabelTask(getShell(), selectionInfo.getRepositories()[0], ServerPath.getTeamProject(serverPath)).run();
    }
}
