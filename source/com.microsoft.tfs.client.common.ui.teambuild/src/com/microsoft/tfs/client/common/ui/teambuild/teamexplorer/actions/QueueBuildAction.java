// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildHelper;
import com.microsoft.tfs.client.common.ui.teambuild.commands.QueueBuildCommand;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.QueueBuildDialog;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildRequest;
import com.microsoft.tfs.util.Check;

public class QueueBuildAction extends TeamExplorerSingleBuildDefinitionAction {
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            if (selectedDefinition == null || !selectedDefinition.isEnabled()) {
                action.setEnabled(false);
            }
        }
    }

    @Override
    public void doRun(final IAction action) {
        Check.notNull(selectedDefinition, "selectedDefinition"); //$NON-NLS-1$

        final QueueBuildDialog dialog = new QueueBuildDialog(getShell(), selectedDefinition);

        if (dialog.getBuildDefinitionCount() == 0) {
            final String messageFormat = Messages.getString("QueueBuildAction.NoBuildDefAvailableFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, selectedDefinition.getTeamProject());
            MessageBoxHelpers.errorMessageBox(getShell(), null, message);
            return;
        }

        if (IDialogConstants.CANCEL_ID == dialog.open()) {
            return;
        }

        final IBuildRequest request = dialog.getSelectedBuildRequest();
        final QueueBuildCommand command = new QueueBuildCommand(request);

        // This is not executed async because we need to get the Queued Build
        // back to then display it in the build explorer.
        if (execute(command, false).getSeverity() == IStatus.OK) {
            TeamBuildHelper.openBuildExplorer(request.getBuildDefinition(), command.getQueuedBuild());
            BuildHelpers.getBuildManager().fireBuildQueuedEvent(getTargetPart(), command.getQueuedBuild());
        }
    }
}
