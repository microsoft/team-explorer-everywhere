// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.BuildDefinitionDialog;
import com.microsoft.tfs.client.common.ui.teambuild.dialogs.TfsBuildDefinitionDialog;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.git.EGitHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.exceptions.BuildException;

public class EditBuildDefinitionFromDetailsAction extends BuildDetailAction {
    private static final Log log = LogFactory.getLog(EditBuildDefinitionFromDetailsAction.class);

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(final IAction action) {
        IBuildDefinition buildDefinition = getSelectedBuildDetail().getBuildDefinition();

        // Get a fresh copy of the build definition. This makes sure edits we
        // make will get ignored on Cancel but also that we have a fresh copy of
        // the definition so reduce the risk of overwriting someone else's
        // changes.
        buildDefinition = BuildHelpers.getUpToDateBuildDefinition(getShell(), buildDefinition);
        if (buildDefinition == null || buildDefinition.getSourceProviders().length == 0) {
            return;
        }

        final BuildDefinitionDialog dialog;

        if (buildDefinition.getSourceProviders() != null
            && buildDefinition.getSourceProviders().length > 0
            && BuildSourceProviders.isTfGit(buildDefinition.getSourceProviders()[0])) {
            dialog = (BuildDefinitionDialog) EGitHelpers.getGitBuildDefinitionDialog(getShell(), buildDefinition);

            if (dialog == null) {
                final String errorMessage = Messages.getString("BuildHelpers.EGitRequired"); //$NON-NLS-1$
                final String title = Messages.getString("EditBuildDefinitionFromDetailsAction.EGitNotInstalled"); //$NON-NLS-1$

                log.error("Cannot edit the build definition. EGit plugin is requered for this action."); //$NON-NLS-1$
                MessageDialog.openError(getShell(), title, errorMessage);

                return;
            }
        } else {
            dialog = new TfsBuildDefinitionDialog(getShell(), buildDefinition);
        }

        try {
            if (dialog.open() == IDialogConstants.OK_ID) {
                dialog.commitChangesIfNeeded();

                // Reload build definitions in case a name has changed or
                // something.
                if (BuildExplorer.getInstance() != null && !BuildExplorer.getInstance().isDisposed()) {
                    BuildExplorer.getInstance().reloadBuildDefinitions();
                }

                // We should redraw and build agents at this point because we
                // may
                // have added new ones.
                if (BuildExplorer.getInstance() != null
                    && !BuildExplorer.getInstance().isDisposed()
                    && BuildExplorer.getInstance().getQueueEditorPage() != null) {
                    BuildExplorer.getInstance().getQueueEditorPage().reloadBuildAgents();
                }
            }
        } catch (final BuildException e) {
            final String title = Messages.getString("EditBuildDefinitionFromDetailsAction.CannotEditDefinitionTitle"); //$NON-NLS-1$
            log.warn(title, e);
            MessageDialog.openWarning(getShell(), title, e.getMessage());
        } catch (final Exception e) {
            final String title = Messages.getString("EditBuildDefinitionFromDetailsAction.FailedEditDefinitionTitle"); //$NON-NLS-1$
            log.error(title, e);
            MessageDialog.openError(getShell(), title, e.getLocalizedMessage());
        }
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            if (action.isEnabled() && selection instanceof IStructuredSelection) {
                final Object obj = ((IStructuredSelection) selection).getFirstElement();
                if (obj instanceof IBuildDetail) {
                    action.setEnabled(!((IBuildDetail) obj).getBuildServer().getBuildServerVersion().isV1());
                }
            }
        }
    }

}
