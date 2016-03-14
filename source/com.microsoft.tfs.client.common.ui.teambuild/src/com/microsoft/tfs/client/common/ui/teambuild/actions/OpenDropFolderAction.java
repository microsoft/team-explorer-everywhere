// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import java.io.File;
import java.net.URL;
import java.text.MessageFormat;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.VersionControlHelper;
import com.microsoft.tfs.client.common.ui.teambuild.commands.DownloadBuildDropCommand;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.util.StringUtil;

public class OpenDropFolderAction extends BuildDetailAction {

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        // TODO: Need to test on non-windows platforms to see if this is
        // valid...

        final IBuildDetail build = getSelectedBuildDetail();
        final String buildDropLocation = build.getDropLocation();
        if (!StringUtil.isNullOrEmpty(buildDropLocation)) {
            // in case of hosted build controller
            if (ServerPath.isServerPath(buildDropLocation)) {
                VersionControlHelper.openSourceControlExplorer(buildDropLocation);

            }
            // in case of file container service
            if (buildDropLocation.startsWith(BuildHelpers.DROP_TO_FILE_CONTAINER_LOCATION)) {
                downloadDrop(build);

            } else {
                Launcher.launch(buildDropLocation);
            }
        } else {
            MessageBoxHelpers.messageBox(
                getShell(),
                Messages.getString("OpenDropFolderAction.NoDropLocationDialogTitle"), //$NON-NLS-1$
                Messages.getString("OpenDropFolderAction.NoDropLocationDialogText")); //$NON-NLS-1$
        }
    }

    private void downloadDrop(final IBuildDetail build) {
        // Prompt the user for confirmation to download
        final String title = Messages.getString("BuildDropDownload.ConfirmTitle"); //$NON-NLS-1$
        final String prompt = Messages.getString("BuildDropDownload.ConfirmPrompt"); //$NON-NLS-1$

        if (!MessageDialog.openQuestion(getShell(), title, prompt)) {
            return;
        }

        // Calculate the resource url
        final URL dropURL = TSWAHyperlinkBuilder.getFileContainerURL(build);

        if (dropURL == null) {
            return;
        }

        String dropFileName = Messages.getString("BuildDrop.DownloadFileName"); //$NON-NLS-1$
        final String[] dropLocationParts = build.getDropLocation().split("/"); //$NON-NLS-1$
        if (dropLocationParts != null && dropLocationParts.length == 3) {
            dropFileName = dropLocationParts[2] + Messages.getString("BuildDrop.DownloadFileExtension"); //$NON-NLS-1$
            ;
        }

        // Let the user pick download local path
        final File localTarget = promptForLocation(dropFileName);
        if (localTarget == null) {
            return;
        }

        // Download the drop
        final DownloadBuildDropCommand downloadCommand =
            new DownloadBuildDropCommand(dropURL, localTarget, build.getBuildServer().getConnection());

        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(downloadCommand);
    }

    private File promptForLocation(final String dropPath) {
        final DirectoryDialog dialog = new DirectoryDialog(getShell());

        final String directoryPath = dialog.open();
        if (directoryPath != null) {
            final File targetFile = new File(directoryPath, dropPath);
            if (targetFile.exists()) {
                final String title = Messages.getString("BuildDropDownload.ConfirmOverwriteDialogTitle"); //$NON-NLS-1$
                final String messageFormat = Messages.getString("BuildDropDownload.ConfirmOverwriteDialogTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, targetFile.getAbsolutePath());

                if (!MessageBoxHelpers.dialogConfirmPrompt(getShell(), title, message)) {
                    return null;
                }
            }
            return targetFile;
        }
        return null;

    }
}
