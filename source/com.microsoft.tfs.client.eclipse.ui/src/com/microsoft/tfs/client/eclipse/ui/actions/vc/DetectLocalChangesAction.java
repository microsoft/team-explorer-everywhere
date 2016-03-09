// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.Task;
import com.microsoft.tfs.client.common.ui.tasks.vc.DetectLocalChangesTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.client.eclipse.ui.offline.ResourceOfflineSynchronizerFilter;
import com.microsoft.tfs.client.eclipse.ui.offline.ResourceOfflineSynchronizerProvider;

/**
 * Reconciles the local filesystem with the TFS server. For server workspaces,
 * this is the equivalent of "tfpt online" or "return online" only expects that
 * the projects are already connected. For local workspaces, this runs a full
 * scan of the workspace.
 *
 * @threadsafety unknown
 */
public class DetectLocalChangesAction extends ExtendedAction {
    public DetectLocalChangesAction() {
        super();
        setName(Messages.getString("DetectLocalChangesAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.STANDARD_FILTER)
                && ActionHelpers.getRepositoriesFromSelection(getSelection()).length == 1);
    }

    @Override
    public void doRun(final IAction action) {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.STANDARD_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final TFSRepository repository = selectionInfo.getRepositories()[0];

        // Handles both server and local workspaces
        final Task detectTask = new DetectLocalChangesTask(
            getShell(),
            repository,
            new ResourceOfflineSynchronizerProvider(selectionInfo.getResources()),
            new ResourceOfflineSynchronizerFilter());

        detectTask.run();
    }
}