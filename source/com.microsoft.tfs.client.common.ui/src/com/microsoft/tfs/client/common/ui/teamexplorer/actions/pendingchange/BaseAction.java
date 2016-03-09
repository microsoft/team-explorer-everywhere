// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeNode;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesViewModel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public abstract class BaseAction extends TeamExplorerBaseAction {
    protected PendingChangesTreeNode[] selectedNodes;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        selectedNodes = (PendingChangesTreeNode[]) selectionToArray(PendingChangesTreeNode.class);
    }

    public PendingChangesViewModel getModel() {
        return TFSCommonUIClientPlugin.getDefault().getPendingChangesViewModel();
    }

    public PendingChange getSelectedPendingChange() {
        if (selectedNodes == null || selectedNodes.length == 0) {
            return null;
        }

        return selectedNodes[0].getPendingChange();
    }

    public PendingChange[] getSelectedPendingChanges() {
        if (selectedNodes == null || selectedNodes.length == 0) {
            return new PendingChange[0];
        }

        final Set<PendingChange> setChanges = new HashSet<PendingChange>();

        for (final PendingChangesTreeNode node : selectedNodes) {
            node.addPendingChangesInSubTree(setChanges);
        }

        return setChanges.toArray(new PendingChange[0]);
    }
}
