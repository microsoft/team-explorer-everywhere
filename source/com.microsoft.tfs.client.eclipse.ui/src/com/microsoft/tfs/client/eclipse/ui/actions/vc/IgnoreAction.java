// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.commands.vc.AddTFSIgnoreExclusionsCommand;
import com.microsoft.tfs.client.common.framework.resources.command.ResourceChangingCommand;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.client.eclipse.ui.tasks.vc.TPIgnoreTask;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;

/**
 * Adds patterns to .tfignore (TFS 2012 local workspace) or .tpignore (all other
 * connections) files.
 */
public class IgnoreAction extends ExtendedAction {
    private WorkspaceLocation location;

    public IgnoreAction() {
        super();
        setName(Messages.getString("IgnoreAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        location = null;

        if (action.isEnabled() == false) {
            return;
        }

        /*
         * .tfignore enablement test.
         *
         * Enable for any items which are in a local workspace and not currently
         * ignored.
         *
         * Allow only 1 repository in the selection.
         */
        if (ActionHelpers.filterAcceptsAnyResource(selection, PluginResourceFilters.IN_LOCAL_WORKSPACE_FILTER)
            && ActionHelpers.filterAcceptsAnyResource(selection, PluginResourceFilters.TFS_IGNORE_FILTER)
            && ActionHelpers.getRepositoriesFromSelection(selection).length == 1) {
            location = WorkspaceLocation.LOCAL;
            action.setEnabled(true);
            return;
        }

        /*
         * .tpignore enablement test.
         *
         * Enable for any items which are in a server workspace and not
         * currently ignored.
         *
         * Allow only 1 repository in the selection.
         */
        if (ActionHelpers.filterAcceptsAnyResource(selection, PluginResourceFilters.IN_SERVER_WORKSPACE_FILTER)
            && ActionHelpers.filterAcceptsAnyResource(selection, PluginResourceFilters.TPIGNORE_FILTER)
            && ActionHelpers.getRepositoriesFromSelection(selection).length == 1) {
            location = WorkspaceLocation.SERVER;
            action.setEnabled(true);
            return;
        }

        action.setEnabled(false);
    }

    @Override
    public void doRun(final IAction action) {
        if (location == WorkspaceLocation.LOCAL) {
            ignoreLocal();
        } else if (location == WorkspaceLocation.SERVER) {
            ignoreServer();
        }
    }

    private void ignoreLocal() {
        /*
         * Adapt only items not currently ignored (items accepted by
         * PluginResourceFilters.TFSIGNORE_FILTER) so we don't create duplicate
         * patterns in the file.
         */
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.TFS_IGNORE_FILTER,
            false);

        /*
         * Ensure a single project. Skip the usual single-repository requirement
         * because offline resources won't be mapped to any repository and we
         * want to support offline resources.
         */
        if (ActionHelpers.ensureSingleProject(selectionInfo, getShell()) == false) {
            return;
        }

        final AddTFSIgnoreExclusionsCommand command =
            new AddTFSIgnoreExclusionsCommand(selectionInfo.getRepositories()[0], selectionInfo.getResources());

        /*
         * Run in a ResourceChangingCommand because core edits .tfignore files
         * directly.
         */
        UICommandExecutorFactory.newUICommandExecutor(getShell()).execute(new ResourceChangingCommand(command));
    }

    private void ignoreServer() {
        /*
         * Adapt only items not currently ignored (items accepted by
         * PluginResourceFilters.TPIGNORE_FILTER) so we don't create duplicate
         * patterns in the file.
         */
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.TPIGNORE_FILTER,
            false);

        /*
         * Ensure a single project. Skip the usual single-repository requirement
         * because offline resources won't be mapped to any repository and we
         * want to support offline resources.
         */
        if (ActionHelpers.ensureSingleProject(selectionInfo, getShell()) == false) {
            return;
        }

        new TPIgnoreTask(getShell(), selectionInfo.getResources()).run();
    }
}
