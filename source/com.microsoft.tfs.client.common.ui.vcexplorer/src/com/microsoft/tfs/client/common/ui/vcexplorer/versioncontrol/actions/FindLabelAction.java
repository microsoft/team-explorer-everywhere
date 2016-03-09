// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.FindLabelTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class FindLabelAction extends TeamViewerAction {
    private TFSItem[] items;

    @Override
    public void doRun(final IAction action) {
        String project = ServerPath.ROOT;
        if (items != null && items.length > 0) {
            project = ServerPath.getTeamProject(items[0].getFullPath());
        }

        new FindLabelTask(getShell(), getCurrentRepository(), project).run();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);
    }
}
