// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;

import com.microsoft.tfs.client.common.ui.commands.annotate.AnnotateCommand;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class AnnotateAction extends ExtendedAction {
    public AnnotateAction() {
        setName(Messages.getString("AnnotateAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        if (ActionHelpers.linkSelected(selection)) {
            action.setEnabled(false);
            return;
        }

        action.setEnabled(
            ActionHelpers.filterAcceptsAnyResource(getSelection(), PluginResourceFilters.IN_REPOSITORY_FILTER));
    }

    @Override
    public void doRun(final IAction action) {
        if (SWT.getVersion() < 3300) {
            MessageDialog.openInformation(
                getShell(),
                Messages.getString("AnnotateAction.CompatibilityErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("AnnotateAction.CompatibilityErrorDialogText")); //$NON-NLS-1$
            return;
        }

        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getSelection(),
            PluginResourceFilters.IN_REPOSITORY_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource resource = selectionInfo.getResources()[0];

        final AnnotateCommand command = new AnnotateCommand(selectionInfo.getRepositories()[0], resource, getShell());
        getCommandExecutor(true).execute(command);
    }
}
