// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.teambuild.TeamProject;
import com.microsoft.tfs.client.common.ui.teambuild.commands.AddRemoveBuildQualitiesCommand;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.ManageBuildQualitiesDialog;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;

/**
 * Open dialog to manage build qualities for active Team Project.
 */
public class ManageBuildQualitiesAction extends BaseAction {

    /**
     * @see com.microsoft.tfs.client.common.ui.teambuild.actions.BaseAction#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        final TeamProject teamProject = (TeamProject) adaptSelectionFirstElement(TeamProject.class);

        Shell shell;
        if (getTargetPart() != null) {
            shell = getShell();
        } else {
            shell = getEditor().getSite().getShell();
        }

        if (BuildExplorer.getInstance() != null
            && !BuildExplorer.getInstance().isDisposed()
            && BuildExplorer.getInstance().getBuildEditorPage() != null) {
            // We have a build explorer visible - check to make sure we are not
            // editing quality
            if (BuildExplorer.getInstance().getBuildEditorPage().getBuildsTableControl().getViewer().isCellEditorActive()) {
                BuildExplorer.getInstance().getBuildEditorPage().getBuildsTableControl().getViewer().cancelEditing();
            }
        }

        final ManageBuildQualitiesDialog dialog =
            new ManageBuildQualitiesDialog(shell, teamProject.getBuildServer(), teamProject.getName());

        if (dialog.open() == IDialogConstants.OK_ID) {
            final AddRemoveBuildQualitiesCommand command = new AddRemoveBuildQualitiesCommand(
                teamProject.getBuildServer(),
                teamProject.getName(),
                dialog.getQualitiesToAdd(),
                dialog.getQualitiesToRemove());

            final IStatus status = UICommandExecutorFactory.newUICommandExecutor(shell).execute(command);
            if (status.getSeverity() == IStatus.OK) {
                // Anything need to be done here?
            }
        }

        // We should redraw and build qualities at this point because the dialog
        // may have loaded new ones from the server into the cache.
        if (BuildExplorer.getInstance() != null
            && !BuildExplorer.getInstance().isDisposed()
            && BuildExplorer.getInstance().getBuildEditorPage() != null) {
            BuildExplorer.getInstance().getBuildEditorPage().reloadBuildQualities();
        }
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.teambuild.actions.BaseAction#onSelectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            action.setEnabled(getBuildServer() != null);
        }
    }

}
