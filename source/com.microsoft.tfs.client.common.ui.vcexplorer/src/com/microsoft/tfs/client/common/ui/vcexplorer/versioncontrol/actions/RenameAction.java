// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import java.text.MessageFormat;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;

import com.microsoft.tfs.client.common.ui.dialogs.generic.StringInputDialog;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class RenameAction extends MoveAction {
    private TFSItem item;

    @Override
    public void doRun(final IAction action) {
        final String labelText = Messages.getString("RenameAction.InputDialogLabelText"); //$NON-NLS-1$
        final String titleFormat = Messages.getString("RenameAction.InputDialogTitleFormat"); //$NON-NLS-1$
        final String title = MessageFormat.format(titleFormat, item.getName());
        final String purpose = "Rename"; //$NON-NLS-1$

        final StringInputDialog inputDialog =
            new StringInputDialog(getShell(), labelText, item.getName(), title, purpose);

        if (item.getName() != null && item.getName().contains(".") && item.getName().lastIndexOf(".") > 0) //$NON-NLS-1$ //$NON-NLS-2$
        {
            inputDialog.setSelection(0, item.getName().lastIndexOf(".")); //$NON-NLS-1$
        } else if (item.getName() != null) {
            inputDialog.setSelection(0, item.getName().length());
        }

        if (inputDialog.open() == Window.OK) {
            final String sourcePath = item.getItemPath().getFullPath();
            final String destinationPath = ServerPath.combine(ServerPath.getParent(sourcePath), inputDialog.getInput());

            doMoveOperation(sourcePath, destinationPath);
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        super.onSelectionChanged(action, selection);

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);
    }
}
