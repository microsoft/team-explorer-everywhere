// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.tfs.client.common.ui.dialogs.wit.GoToWorkItemDialog;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;

public class GotoWorkItemAction extends TeamExplorerWITBaseAction {
    @Override
    protected void doRun(final IAction action) {
        final GoToWorkItemDialog dialog = new GoToWorkItemDialog(getShell());
        if (dialog.open() == IDialogConstants.OK_ID) {
            final int workItemID = dialog.getID();
            if (workItemID > 0) {
                WorkItemEditorHelper.openEditor(getContext().getServer(), workItemID);
            }
        }
    }
}
