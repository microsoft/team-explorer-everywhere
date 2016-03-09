// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.MergeTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;

public class MergeAction extends TeamViewerAction {
    private TFSItem item;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final String sourcePath = item.getFullPath();

        if (sourcePath == null) {
            return;
        }

        new MergeTask(getShell(), getCurrentRepository(), sourcePath).run();

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

        action.setEnabled(ActionEnablementHelper.selectionContainsRoot(selection) == false
            && ActionEnablementHelper.selectionContainsDeletedItem(selection) == false);
    }
}
