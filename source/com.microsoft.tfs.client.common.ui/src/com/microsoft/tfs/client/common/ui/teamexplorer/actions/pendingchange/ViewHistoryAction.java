// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.ViewHistoryTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeNode;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public class ViewHistoryAction extends TeamExplorerBaseAction {
    private PendingChangesTreeNode selectedNode;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        // Only enable for a single selection.
        if (getSelectionSize() != 1) {
            action.setEnabled(false);
            return;
        }

        // Should always be a PendingChangeTreeNode
        final Object obj = getSelectionFirstElement();
        Check.isTrue(obj instanceof PendingChangesTreeNode, "expected PendingChangeTreeNode"); //$NON-NLS-1$

        // Get tree node and pending change if one exists for this node.
        selectedNode = (PendingChangesTreeNode) obj;
        final PendingChange pendingChange = selectedNode.getPendingChange();

        if (pendingChange == null) {
            // This is a interior tree node for the folder portion of one or
            // more pending changes. Always enable for this selection.
            action.setEnabled(true);
            return;
        }

        // Enable history for any pending change that is not an add.
        if (pendingChange.getChangeType().contains(ChangeType.ADD)) {
            action.setEnabled(false);
        } else {
            action.setEnabled(true);
        }
    }

    @Override
    public void doRun(final IAction action) {
        ItemSpec itemSpec;
        if (selectedNode.getPendingChange() != null) {
            itemSpec = new ItemSpec(selectedNode.getPendingChange().getLocalItem(), RecursionType.NONE);
        } else {
            itemSpec = new ItemSpec();
        }

        final ViewHistoryTask task = new ViewHistoryTask(getShell(), getContext().getDefaultRepository(), itemSpec);

        task.run();
    }
}
