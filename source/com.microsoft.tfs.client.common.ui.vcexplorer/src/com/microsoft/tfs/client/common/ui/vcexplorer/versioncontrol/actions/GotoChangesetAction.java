// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.GotoChangesetDialog;
import com.microsoft.tfs.client.common.ui.tasks.vc.ViewChangesetDetailsTask;

public class GotoChangesetAction extends TeamViewerAction {
    @Override
    public void doRun(final IAction action) {
        final TFSRepository repository = getCurrentRepository();
        final GotoChangesetDialog dialog = new GotoChangesetDialog(getShell(), repository);
        if (dialog.open() == IDialogConstants.OK_ID) {
            final int changesetId = dialog.getChangesetID();
            if (changesetId > 0) {
                new ViewChangesetDetailsTask(getShell(), repository, changesetId).run();
            }
        }
    }
}
