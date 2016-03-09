// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.actions;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.commands.SetQueuePriorityCommand;
import com.microsoft.tfs.client.common.ui.teambuild.editors.BuildExplorer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.clients.build.soapextensions.QueuePriority;
import com.microsoft.tfs.core.product.ProductInformation;

public class SetPriorityAction extends QueuedBuildAction {

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void doRun(final IAction action) {
        // The desired queue priority is calculated from the last part of the
        // action id.
        final String queuePriorityString = action.getId().substring(action.getId().lastIndexOf('.') + 1);
        final QueuePriority queuePriority = getQueuePriority(queuePriorityString);
        if (queuePriority == null) {
            final String messageFormat = Messages.getString("SetPriorityAction.UnrecognizedPriorityFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                queuePriorityString,
                ProductInformation.getCurrent().getFamilyShortName());
            openErrorDialog(message);
            return;
        }

        final SetQueuePriorityCommand command =
            new SetQueuePriorityCommand(getBuildServer(), getSelectedQueuedBuilds(), queuePriority);

        final IStatus staus = execute(command, false);

        if (staus.getSeverity() == IStatus.OK) {
            final BuildExplorer buildExplorer = BuildExplorer.getInstance();
            if (buildExplorer != null) {
                buildExplorer.getQueueEditorPage().getQueuedBuildsTable().getViewer().update(
                    command.getAffectedQueuedBuilds(),
                    null);
                buildExplorer.refresh();
            }
        }
    }

    private QueuePriority getQueuePriority(final String queuePriorityString) {
        QueuePriority queuePriority = null;
        if ("Low".equalsIgnoreCase(queuePriorityString)) //$NON-NLS-1$
        {
            queuePriority = QueuePriority.LOW;
        }
        if ("BelowNormal".equalsIgnoreCase(queuePriorityString)) //$NON-NLS-1$
        {
            queuePriority = QueuePriority.BELOW_NORMAL;
        }
        if ("Normal".equalsIgnoreCase(queuePriorityString)) //$NON-NLS-1$
        {
            queuePriority = QueuePriority.NORMAL;
        }
        if ("AboveNormal".equalsIgnoreCase(queuePriorityString)) //$NON-NLS-1$
        {
            queuePriority = QueuePriority.ABOVE_NORMAL;
        }
        if ("High".equalsIgnoreCase(queuePriorityString)) //$NON-NLS-1$
        {
            queuePriority = QueuePriority.HIGH;
        }
        return queuePriority;
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.teambuild.actions.QueuedBuildAction#onSelectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);
        if (action.isEnabled()) {
            // Check the queueStatus
            final IQueuedBuild queuedBuild = getSelectedQueuedBuild();
            if (queuedBuild == null || queuedBuild.getStatus() == null) {
                action.setEnabled(false);
                return;
            }
            if (queuedBuild.getStatus().containsAny(QueueStatus.combine(new QueueStatus[] {
                QueueStatus.CANCELED,
                QueueStatus.COMPLETED,
                QueueStatus.IN_PROGRESS
            }))) {
                action.setEnabled(false);
                return;
            }
        }
    }
}
