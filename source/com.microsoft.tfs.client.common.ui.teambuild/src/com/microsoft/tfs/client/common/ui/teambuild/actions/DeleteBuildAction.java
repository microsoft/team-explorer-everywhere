// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.DeleteBuildsCommand;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.DeleteBuildsDialog;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;

/**
 */
public class DeleteBuildAction extends BuildDetailAction {

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        final IBuildDetail[] completedBuilds = getSeletedCompletedBuilds();
        if (completedBuilds == null || completedBuilds.length == 0) {
            return;
        }

        if (completedBuilds[0].getStatus().contains(BuildStatus.IN_PROGRESS)) {
            return;
        }

        for (final IBuildDetail build : completedBuilds) {
            if (build.isKeepForever()) {
                MessageBoxHelpers.errorMessageBox(
                    getShell(),
                    Messages.getString("DeleteBuildAction.DeleteBuildsErrorTitle"), //$NON-NLS-1$
                    MessageFormat.format(
                        Messages.getString("DeleteBuildAction.DeleteBuildsErrorTextFormat"), //$NON-NLS-1$
                        build.getBuildNumber()));
                return;
            }
        }

        final boolean isV3OrGreater = getBuildServer().getBuildServerVersion().isV3OrGreater();
        final DeleteBuildsDialog deleteBuildDialog = new DeleteBuildsDialog(getShell(), completedBuilds, isV3OrGreater);

        if (deleteBuildDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final DeleteOptions deleteOption = deleteBuildDialog.getDeleteOption();

        if (deleteOption.equals(DeleteOptions.NONE)) {
            return;
        }

        final DeleteBuildsCommand command =
            new DeleteBuildsCommand(getTargetPart(), getBuildServer(), completedBuilds, deleteOption);

        // Not async because we need the builds to disappear from the build
        // explorer view.
        final IStatus status = UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(command);

        if (status.getSeverity() == IStatus.OK) {
            BuildHelpers.getBuildManager().fireBuildsDeletedEvent(getTargetPart(), command.getDeletedBuilds());
        }
    }
}
