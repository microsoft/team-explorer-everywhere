// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.commands.vc.AddCommand;
import com.microsoft.tfs.client.common.item.ServerItemPath;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.dialogs.generic.StringInputDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;

public class NewFolderAction extends TeamViewerAction {
    private File path;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        String messageFormat = Messages.getString("NewFolderAction.InputDialogTitleFormat"); //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, path.getAbsolutePath());

        final StringInputDialog inputDialog =
            new StringInputDialog(
                getShell(),
                Messages.getString("NewFolderAction.InputDialogTitle"), //$NON-NLS-1$
                null,
                message,
                "explorer.newfolder"); //$NON-NLS-1$

        if (inputDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final File newDir = new File(path, inputDialog.getInput());
        messageFormat = Messages.getString("NewFolderAction.ExistsDialogTextFormat"); //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, newDir.getAbsolutePath());

        if (newDir.exists() && newDir.isFile()) {
            ErrorDialog.openError(getShell(), Messages.getString("NewFolderAction.ExistsDialogTitle"), null, new Status( //$NON-NLS-1$
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                message,
                null));
            return;
        } else if (!newDir.exists() && !newDir.mkdirs()) {
            messageFormat = Messages.getString("NewFolderAction.ErrorDialogTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, newDir.getAbsolutePath());

            ErrorDialog.openError(getShell(), Messages.getString("NewFolderAction.ErrorDialogTitle"), null, new Status( //$NON-NLS-1$
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                message,
                null));
            return;
        }

        final AddCommand addCommand = new AddCommand(getCurrentRepository(), newDir.getAbsolutePath());

        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(addCommand);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        path = null;

        final TFSItem item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        if (item != null) {
            try {
                if (item instanceof TFSFolder) {
                    final TFSFolder folder = (TFSFolder) item;
                    if (!folder.getItemPath().isRoot() && folder.getMappedLocalPath() != null) {
                        path = new File(folder.getMappedLocalPath());
                    }
                } else {
                    if (item.getMappedLocalPath() != null) {
                        final ServerItemPath parentPath = item.getItemPath().getParent();
                        if (!parentPath.isRoot()) {
                            path = new File(item.getMappedLocalPath()).getParentFile();
                        }
                    }
                }
            } catch (final PathTooLongException e) {
                action.setEnabled(false);
            }
        }

        action.setEnabled(path != null);
    }
}
