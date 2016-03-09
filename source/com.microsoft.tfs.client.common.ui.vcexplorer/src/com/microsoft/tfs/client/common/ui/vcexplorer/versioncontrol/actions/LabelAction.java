// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.ApplyLabelTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;

public class LabelAction extends TeamViewerAction {
    private TFSItem item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        new ApplyLabelTask(getShell(), getCurrentRepository(), item.getFullPath()).run();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        item = (TFSItem) adaptSelectionFirstElement(TFSItem.class);

        action.setEnabled(ActionEnablementHelper.selectionContainsDeletedItem(selection) == false);
    }
}
