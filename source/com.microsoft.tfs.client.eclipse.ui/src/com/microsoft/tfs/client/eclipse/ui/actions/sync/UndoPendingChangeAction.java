// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.tasks.vc.UndoPendingChangesTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class UndoPendingChangeAction extends SynchronizeAction {
    public UndoPendingChangeAction(final Shell shell) {
        super(shell);

        setText(Messages.getString("UndoPendingChangeAction.ActionText")); //$NON-NLS-1$
        setImageDescriptor(
            getImageHelper().getImageDescriptor(
                "platform:/plugin/com.microsoft.tfs.client.common.ui/images/vc/undo.gif")); //$NON-NLS-1$
    }

    @Override
    public void addToContextMenu(final IMenuManager manager, final IResource[] resources) {
        super.addToContextMenu(manager, resources);

        manager.add(this);

        setEnabled(ActionHelpers.filterAcceptsAnyResource(resources, PluginResourceFilters.HAS_PENDING_CHANGES_FILTER));
    }

    @Override
    public void run() {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getStructuredSelection(),
            PluginResourceFilters.HAS_PENDING_CHANGES_FILTER,
            true);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final UndoPendingChangesTask task = new UndoPendingChangesTask(
            getShell(),
            selectionInfo.getRepositories()[0],
            selectionInfo.getPendingChanges());

        task.run();
    }
}
