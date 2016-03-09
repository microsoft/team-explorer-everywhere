// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.client.eclipse.ui.commands.vc.SwitchToBranchCommand;
import com.microsoft.tfs.client.eclipse.ui.dialogs.vc.SwitchToBranchDialog;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

public class SwitchToBranchAction extends ExtendedAction {
    public SwitchToBranchAction() {
        super();
        setName(Messages.getString("SwitchToBranchAction.ActionName")); //$NON-NLS-1$
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
            true);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource resource = selectionInfo.getResources()[0];

        if (resource.getLocation() == null) {
            return;
        }

        /*
         * Error if there are pending changes.
         */
        final PendingChange[] relatedChanges = selectionInfo.getPendingChanges();
        if (relatedChanges != null && relatedChanges.length > 0) {
            MessageDialog.openWarning(
                getShell(),
                Messages.getString("SwitchToBranchAction.NotAllowedDialogTitle"), //$NON-NLS-1$
                Messages.getString("SwitchToBranchAction.NotAllowedDialogText")); //$NON-NLS-1$
            return;
        }

        /*
         * Get the project for the resource, since we only switch entire
         * projects.
         */
        final IProject project = resource.getProject();

        final Workspace workspace = selectionInfo.getRepositories()[0].getWorkspace();
        final String localPath = project.getLocation().toOSString();
        final String serverPath = workspace.getMappedServerPath(localPath);

        final SwitchToBranchDialog dialog = new SwitchToBranchDialog(getShell(), workspace, serverPath);

        if (IDialogConstants.CANCEL_ID == dialog.open()
            || ServerPath.equals(serverPath, dialog.getSwitchToServerPath())) {
            return;
        }

        final SwitchToBranchCommand command =
            new SwitchToBranchCommand(workspace, project, serverPath, dialog.getSwitchToServerPath());

        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(new ResourceChangingCommand(command));
    }
}
