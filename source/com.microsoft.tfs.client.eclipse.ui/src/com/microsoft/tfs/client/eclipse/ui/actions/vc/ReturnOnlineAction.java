// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.tasks.vc.DetectLocalChangesTask;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManager;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.dialogs.vc.ReturnOnlineErrorDialog;
import com.microsoft.tfs.client.eclipse.ui.offline.ResourceOfflineSynchronizerFilter;
import com.microsoft.tfs.client.eclipse.ui.offline.ResourceOfflineSynchronizerProvider;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

/**
 * Return online action has two modes: if the selected resources ARE connected,
 * then we simply run the return online command (to reconcile changes.) If the
 * selected resources are OFFLINE, then we reconnect and then run the reconcile
 * command.
 * <p>
 * Handles both server and local workspaces.
 *
 * @threadsafety unknown
 */
public class ReturnOnlineAction extends ExtendedAction {
    private final ProjectRepositoryManager projectManager = TFSEclipseClientPlugin.getDefault().getProjectManager();

    public ReturnOnlineAction() {
        super();
        setName(Messages.getString("ReturnOnlineAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        final IProject[] projects = ActionHelpers.getProjectsFromSelection(selection);

        if (projects.length == 0) {
            action.setEnabled(false);
        } else {
            /*
             * If any selected project is not offline (in an invalid state,
             * connecting or online) then do not allow this action.
             */
            for (int i = 0; i < projects.length; i++) {
                if (projectManager.getProjectStatus(projects[i]) != ProjectRepositoryStatus.OFFLINE) {
                    action.setEnabled(false);
                    return;
                }
            }

            action.setEnabled(true);
        }
    }

    @Override
    public void doRun(final IAction action) {
        /* Make sure we're not building a connection to the server already. */
        if (projectManager.isConnecting()) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("ReturnOnlineAction.ConnectInProgressDialogTitle"), //$NON-NLS-1$
                Messages.getString("ReturnOnlineAction.ConnectInProgressDialogText")); //$NON-NLS-1$
            return;
        }

        final ISelection selection = getSelection();

        final IProject[] selectedProjects = ActionHelpers.getProjectsFromSelection(selection);

        if (selectedProjects.length == 0) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("ReturnOnlineAction.NoProjectOfflineDialogTitle"), //$NON-NLS-1$
                Messages.getString("ReturnOnlineAction.NoProjectOfflineDialogText")); //$NON-NLS-1$
            return;
        }

        projectManager.clearErrors();

        /*
         * Connect the first selected project. We will attempt to connect all
         * other offline projects based on that first connection.
         */

        final TFSRepository repository = projectManager.connect(selectedProjects[0]);

        /*
         * ProjectConnectionManager will display an error if the project does
         * not have a cached workspace that could be realized.
         */
        if (repository == null) {
            return;
        }

        /*
         * Not attempt to connect all other offline projects. Ideally, we can
         * realize them all with the same tfs repository that we connected the
         * first one with, unless a working folder mapping has changed on the
         * workspace, or the user has projects mapped to multiple servers.
         */
        final IProject[] offlineProjects = projectManager.getProjectsOfStatus(ProjectRepositoryStatus.OFFLINE);

        final List<IProject> succeededProjects = new ArrayList<IProject>();
        final List<IProject> failedProjects = new ArrayList<IProject>();

        succeededProjects.add(selectedProjects[0]);

        for (int i = 0; i < offlineProjects.length; i++) {
            /*
             * Disable prompting to change connections: prevents multiple
             * confirm dialogs if the user has selected projects in multiple
             * workspaces. Just let those fail.
             */
            if (projectManager.connect(offlineProjects[i], false) == null) {
                /*
                 * Only notify the user that this project could not come online
                 * if it was one of the selected projects. This prevents people
                 * from selecting projects for a particular repository and
                 * getting errors about bringing projects from another
                 * repository online.
                 */
                for (int j = 0; j < selectedProjects.length; j++) {
                    if (selectedProjects[j] == offlineProjects[i]) {
                        failedProjects.add(offlineProjects[i]);
                        break;
                    }
                }
            } else {
                succeededProjects.add(offlineProjects[i]);
            }
        }

        if (failedProjects.size() > 0) {
            final ReturnOnlineErrorDialog errorDialog = new ReturnOnlineErrorDialog(getShell());
            errorDialog.setProjects(failedProjects.toArray(new IProject[failedProjects.size()]));
            errorDialog.open();
        }

        if (succeededProjects.size() > 0
            && TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
                UIPreferenceConstants.DETECT_LOCAL_CHANGES_ON_MANUAL_RECONNECT)) {
            final IProject[] projects = succeededProjects.toArray(new IProject[succeededProjects.size()]);

            // Handles both server and local workspaces
            final DetectLocalChangesTask detectTask = new DetectLocalChangesTask(
                getShell(),
                repository,
                new ResourceOfflineSynchronizerProvider(projects),
                new ResourceOfflineSynchronizerFilter());

            detectTask.run();

            ConnectHelpers.showHideViews(SourceControlCapabilityFlags.TFS);

        }
    }
}