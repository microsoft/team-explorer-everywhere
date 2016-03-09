// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

/**
 * Does a full refresh of the project information in the
 * {@link ResourceDataManager} for the selected projects. This results in one or
 * more background jobs which run VersionControlClient.getItems() recursively
 * for each project.
 */
public class RefreshServerItemInformationAction extends ExtendedAction {
    public RefreshServerItemInformationAction() {
        super();
        setName(Messages.getString("RefreshServerItemInformationAction.ActionName")); //$NON-NLS-1$
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

        final IResource[] resources = selectionInfo.getResources();
        final Set<IProject> projects = new HashSet<IProject>();

        for (int i = 0; i < resources.length; i++) {
            projects.add(resources[i].getProject());
        }

        TFSEclipseClientPlugin.getDefault().getResourceDataManager().refreshAsync(
            selectionInfo.getRepositories()[0],
            projects.toArray(new IProject[projects.size()]));
    }
}
