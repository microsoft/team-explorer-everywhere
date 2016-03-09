// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.ui.commands.annotate.AnnotateCommand;
import com.microsoft.tfs.client.common.ui.teamexplorer.actions.TeamExplorerBaseAction;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PendingChangesTreeNode;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class AnnotateAction extends TeamExplorerBaseAction {
    private PendingChangesTreeNode selectedNode;
    private IResource resourceForSelectedPendingChange;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        // Find the Eclipse resource associated with this selection.
        resourceForSelectedPendingChange = null;

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
            // more pending changes. Always disable for this selection.
            action.setEnabled(false);
            return;
        }

        // disable annotation for symlink related pending change
        if (PendingChangesHelpers.containsSymlinkChange(pendingChange)) {
            action.setEnabled(false);
            return;
        }

        // Enable annotate for any pending change that is a file but not an add.
        if (pendingChange.getChangeType().contains(ChangeType.ADD) || pendingChange.getItemType() != ItemType.FILE) {
            action.setEnabled(false);
        } else {
            final String path = pendingChange.getLocalItem();
            resourceForSelectedPendingChange = (path == null) ? null : Resources.getResourceForLocation(path);
            action.setEnabled(resourceForSelectedPendingChange != null);
        }
    }

    @Override
    public void doRun(final IAction action) {
        Check.notNull(resourceForSelectedPendingChange, "resourceForSelectedPendingChange"); //$NON-NLS-1$

        final AnnotateCommand command =
            new AnnotateCommand(getContext().getDefaultRepository(), resourceForSelectedPendingChange, getShell());
        getCommandExecutor(true).execute(command);
    }
}
