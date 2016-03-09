// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.BranchTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.Messages;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;

public class BranchAction extends TeamViewerAction {
    private TFSItem branchFromItem;

    public BranchAction() {
        setName(Messages.getString("BranchAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    public void doRun(final IAction action) {
        if (branchFromItem != null) {
            final BranchTask task = new BranchTask(getShell(), getCurrentRepository(), branchFromItem);
            task.run();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        final TFSItem item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        if (item != null) {
            branchFromItem = item;
        }

        action.setEnabled(
            ActionEnablementHelper.selectionContainsRoot(selection) == false
                && ActionEnablementHelper.selectionContainsDeletedItem(selection) == false);
    }

}
