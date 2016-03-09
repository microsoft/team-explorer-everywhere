// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.DeleteBuildDefinitionCommand;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.events.BuildDefinitionEventArg;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerEvents;
import com.microsoft.tfs.util.Check;

public class DeleteBuildDefinitionAction extends TeamExplorerSingleBuildDefinitionAction {
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (action.isEnabled()) {
            if (selectedDefinition.getBuildServer().getBuildServerVersion().isV1()) {
                action.setEnabled(false);
            }
        }
    }

    @Override
    public void doRun(final IAction action) {
        Check.notNull(selectedDefinition, "selectedDefinition"); //$NON-NLS-1$

        final String title = Messages.getString("DeleteBuildDefinitionAction.DeleteBuildDefDialogTitle"); //$NON-NLS-1$
        final String messageFormat = Messages.getString("DeleteBuildDefinitionAction.DeleteBuildDefDialogTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, selectedDefinition.getName());

        if (!MessageDialog.openQuestion(getShell(), title, message)) {
            return;
        }

        final DeleteBuildDefinitionCommand command =
            new DeleteBuildDefinitionCommand(selectedDefinition.getBuildServer(), selectedDefinition);

        final IStatus status = execute(command, false);
        if (status == Status.OK_STATUS) {
            // We should redraw build definitions at this point
            if (BuildExplorer.getInstance() != null && !BuildExplorer.getInstance().isDisposed()) {
                BuildExplorer.getInstance().reloadBuildDefinitions();
            }

            final BuildDefinitionEventArg arg = new BuildDefinitionEventArg(selectedDefinition);
            getContext().getEvents().notifyListener(TeamExplorerEvents.BUILD_DEFINITION_DELETED, arg);
        }
    }
}
