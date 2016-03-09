// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.sync;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewHistoryTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class ViewHistoryAction extends SynchronizeAction {
    public ViewHistoryAction(final Shell shell) {
        super(shell);

        setText(Messages.getString("ViewHistoryAction.ActionText")); //$NON-NLS-1$
        setImageDescriptor(
            getImageHelper().getImageDescriptor(
                "platform:/plugin/com.microsoft.tfs.client.common.ui/images/vc/history.gif")); //$NON-NLS-1$
    }

    @Override
    public void addToContextMenu(final IMenuManager manager, final IResource[] selected) {
        super.addToContextMenu(manager, selected);

        manager.add(this);

        if (selected.length != 1) {
            setEnabled(false);
            return;
        }

        setEnabled(ActionHelpers.filterAcceptsAnyResource(
            selected,
            PluginResourceFilters.SYNCHRONIZE_OR_IN_REPOSITORY_FILTER));
    }

    @Override
    public void run() {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getStructuredSelection(),
            PluginResourceFilters.SYNCHRONIZE_OR_IN_REPOSITORY_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final ViewHistoryTask task = new ViewHistoryTask(
            getShell(),
            selectionInfo.getRepositories()[0],
            PluginResourceHelpers.typedItemSpecForResource(
                selectionInfo.getResources()[0],
                true,
                LocationUnavailablePolicy.IGNORE_RESOURCE));

        task.run();
    }
}
