// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.vc.AbstractGetTask;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public abstract class GetAction extends ExtendedAction {
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.IN_REPOSITORY_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.IN_REPOSITORY_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
            return;
        }

        final IResource[] resources = selectionInfo.getResources();

        final TypedServerItem[] serverItems =
            TypedServerItem.getTypedServerItemFromResource(selectionInfo.getRepositories()[0], resources);

        final AbstractGetTask getTask = getGetTask(getShell(), selectionInfo.getRepositories()[0], serverItems);

        getTask.run();
    }

    protected abstract AbstractGetTask getGetTask(
        Shell shell,
        TFSRepository repository,
        TypedServerItem[] typedServerItem);
}
