// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.dialogs.workspaces.WorkspacesDialog;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class ManageWorkspacesAction extends ExtendedAction {
    public ManageWorkspacesAction() {
        super();
        setName(Messages.getString("ManageWorkspacesAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

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

        final Workspace currentWorkspace = selectionInfo.getRepositories()[0].getWorkspace();

        final WorkspacesDialog dialog = new WorkspacesDialog(
            getShell(),
            selectionInfo.getRepositories()[0].getVersionControlClient().getConnection(),
            false,
            false,
            Messages.getString("ManageWorkspacesAction.WorkspacesDialogTitle")); //$NON-NLS-1$

        /*
         * Don't allow the user to modify some data about the active workspace
         * (name, server, owner, etc.). The user can still always modify working
         * folder mappings.
         */
        dialog.getWorkspacesControl().setImmutableWorkspaces(new Workspace[] {
            currentWorkspace
        });

        dialog.open();
    }
}
