// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.ToggleProtectionCommand;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDetail;

public class ToggleProtectionAction extends BuildDetailAction {
    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        // Set the keep forever status of the selection based on the opposite of
        // the current state of the first build detail in the selection.
        final boolean keepForever = !getSelectedBuildDetail().isKeepForever();

        if (!keepForever) {
            if (!MessageDialog.openQuestion(
                getShell(),
                Messages.getString("ToggleProtectionAction.ConfirmRemoveLockDialogTitle"), //$NON-NLS-1$
                Messages.getString("ToggleProtectionAction.ConfirmRemoveLockDialogText"))) //$NON-NLS-1$
            {
                action.setChecked(true);
                return;
            }
        }

        final Shell shell = getTargetPart().getSite().getShell();
        final IBuildDetail[] selectedBuildDetails = getSelectedBuildDetails();

        final ToggleProtectionCommand command =
            new ToggleProtectionCommand(getBuildServer(), selectedBuildDetails, keepForever);

        UICommandExecutorFactory.newUICommandExecutor(shell).execute(command);

        BuildHelpers.getBuildManager().fireBuildDetailsChangedEvent(getTargetPart(), selectedBuildDetails);
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.teambuild.actions.BuildDetailAction#onSelectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            if (getBuildServer().getBuildServerVersion().isV1()) {
                action.setEnabled(false);
                return;
            }

            final IBuildDetail build = getSelectedBuildDetail();
            if (build != null) {
                action.setChecked(build.isKeepForever());
            }
        }
    }
}
