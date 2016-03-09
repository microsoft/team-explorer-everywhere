// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;

import com.microsoft.tfs.client.common.framework.command.CommandList;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.helpers.EditorHelper;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter;
import com.microsoft.tfs.client.common.ui.helpers.TFSEditorSaveableFilter.TFSEditorSaveableType;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.commands.eclipse.DisconnectProjectCommand;
import com.microsoft.tfs.client.eclipse.ui.Messages;

public class DisconnectProjectAction extends ExtendedAction {
    public DisconnectProjectAction() {
        super();
        setName(Messages.getString("DisconnectProjectAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    public void doRun(final IAction action) {
        final IProject[] projects = (IProject[]) adaptSelectionToArray(IProject.class);

        String title;
        String prompt;
        String progress;
        String errorText;

        if (projects.length == 1) {
            title = Messages.getString("DisconnectProjectAction.SingleDisconnectDialogTitle"); //$NON-NLS-1$
            prompt = Messages.getString("DisconnectProjectAction.SingleDisconnectDialogText"); //$NON-NLS-1$
            progress = Messages.getString("DisconnectProjectAction.SingleDisconnectProgressText"); //$NON-NLS-1$
            errorText = Messages.getString("DisconnectProjectAction.SingleDisconnectErrorText"); //$NON-NLS-1$
        } else {
            title = Messages.getString("DisconnectProjectAction.MultiDisconnectDialogTitle"); //$NON-NLS-1$
            prompt = Messages.getString("DisconnectProjectAction.MultiDisconnectDialogText"); //$NON-NLS-1$
            progress = Messages.getString("DisconnectProjectAction.MultiDisconnectProgressText"); //$NON-NLS-1$
            errorText = Messages.getString("DisconnectProjectAction.MultiDisconnectErrorText"); //$NON-NLS-1$
        }

        final boolean result = MessageDialog.openConfirm(getShell(), title, prompt);

        if (result == false) {
            /* User cancelled */
            return;
        }

        /*
         * Determine the repositories that will be brought offline after
         * disconnecting this project.
         */
        final Map<TFSRepository, Set<IProject>> repositoryToProjectList =
            TFSEclipseClientPlugin.getDefault().getProjectManager().getRepositoryToProjectMap();

        for (int i = 0; i < projects.length; i++) {
            final TFSRepository repository =
                TFSEclipseClientPlugin.getDefault().getProjectManager().getRepository(projects[i]);

            if (repository != null) {
                final Set<IProject> projectsForRepository = repositoryToProjectList.get(repository);

                if (projectsForRepository != null) {
                    projectsForRepository.remove(projects[i]);
                }
            }
        }

        /*
         * If there are any repositories that will be brought offline, prompt to
         * save WIT editors before that happens.
         */
        boolean needsSavePrompt = false;

        for (final Entry<TFSRepository, Set<IProject>> entry : repositoryToProjectList.entrySet()) {
            if (entry.getValue().size() == 0) {
                needsSavePrompt = true;
                break;
            }
        }

        /* Prompt for save */
        if (needsSavePrompt) {
            if (EditorHelper.saveAllDirtyEditors(new TFSEditorSaveableFilter(TFSEditorSaveableType.ALL)) == false) {
                /* User cancelled */
                return;
            }
        }

        /* Disconnect the projects */
        final CommandList disconnectCommands = new CommandList(progress, errorText);

        for (int i = 0; i < projects.length; i++) {
            disconnectCommands.addCommand(new DisconnectProjectCommand(projects[i]));
        }

        execute(disconnectCommands);
    }
}
