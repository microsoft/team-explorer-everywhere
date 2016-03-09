// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.GotoChangesetDialog;
import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;

public class GotoChangesetAction extends ExtendedAction {
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
        final TFSRepository repository =
            TFSEclipseClientUIPlugin.getDefault().getRepositoryManager().getDefaultRepository();
        final GotoChangesetDialog dialog = new GotoChangesetDialog(getShell(), repository);
        if (dialog.open() == IDialogConstants.OK_ID) {
            final int changesetId = dialog.getChangesetID();
            if (changesetId > 0) {
                new ViewChangesetDetailsTask(getShell(), repository, changesetId).run();
            }
        }
    }
}
