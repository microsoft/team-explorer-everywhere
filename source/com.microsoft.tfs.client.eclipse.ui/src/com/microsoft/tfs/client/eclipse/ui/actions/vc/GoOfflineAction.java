// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter.TFSEditorSaveableType;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.dialogs.vc.GoOfflineDialog;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;

public class GoOfflineAction extends ExtendedAction {
    private final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

    public GoOfflineAction() {
        super();

        setName(Messages.getString("GoOfflineAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        /* Get all online projects - we'll disconnect them all. */
        final IProject[] projects = projectManager.getProjectsOfStatus(ProjectRepositoryStatus.ONLINE);

        if (projects.length == 0) {
            action.setEnabled(false);
            return;
        }

        final TFSRepository[] repositories = ActionHelpers.getRepositoriesFromSelection(selection);

        if (repositories.length == 0) {
            action.setEnabled(false);
            return;
        }

        for (int i = 0; i < repositories.length; i++) {
            if (WorkspaceLocation.LOCAL.equals(repositories[i].getWorkspace().getLocation())) {
                action.setEnabled(false);
                return;
            }
        }

        action.setEnabled(true);
    }

    @Override
    public void doRun(final IAction action) {
        /* Make sure we're not building a connection to the server already. */
        if (projectManager.isConnecting()) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("GoOfflineAction.ConnectInProgressDialogTitle"), //$NON-NLS-1$
                Messages.getString("GoOfflineAction.ConnectInProgressDialogText")); //$NON-NLS-1$
            return;
        }

        final IProject[] projects = projectManager.getProjectsOfStatus(ProjectRepositoryStatus.ONLINE);

        final GoOfflineDialog dialog = new GoOfflineDialog(getShell());
        dialog.setProjects(projects);

        if (dialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        /* Prompt to save dirty TFS related editors */
        if (!EditorHelper.saveAllDirtyEditors(new TFSEditorSaveableFilter(TFSEditorSaveableType.ALL))) {
            return;
        }

        projectManager.disconnect(projects, false);
    }
}