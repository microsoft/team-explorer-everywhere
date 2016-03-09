// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.ViewHistoryTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFile;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public class HistoryAction extends TeamViewerAction {
    private TFSItem[] items;

    @Override
    public void doRun(final IAction action) {
        final RecursionType recursion = items[0] instanceof TFSFile ? RecursionType.NONE : RecursionType.FULL;

        /*
         * We can pass a local or server path in the item spec.
         *
         * HACK: The root node is special, because its extended item is not
         * fetched until a manual refresh (TFSItemFactory and the VC folder
         * control should be fixed so this is not the case). This means
         * .isLocal() will be incorrect, and the local path cannot be
         * calculated, so hard code the server path in this case.
         */

        String path;
        if (ServerPath.equals(items[0].getFullPath(), ServerPath.ROOT)) {
            path = ServerPath.ROOT;
        } else {
            path = items[0].isLocal() ? items[0].getLocalPath() : items[0].getFullPath();
        }

        new ViewHistoryTask(getShell(), getCurrentRepository(), new ItemSpec(path, recursion)).run();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);
    }
}
