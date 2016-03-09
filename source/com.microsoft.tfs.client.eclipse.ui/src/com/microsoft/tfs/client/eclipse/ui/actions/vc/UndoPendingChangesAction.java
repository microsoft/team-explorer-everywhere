// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import com.microsoft.tfs.client.common.ui.tasks.Task;
import com.microsoft.tfs.client.common.ui.tasks.vc.UndoPendingChangesTask;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class UndoPendingChangesAction extends CheckinShelveUndoAction {
    public UndoPendingChangesAction() {
        super();
        setName(Messages.getString("UndoPendingChangesAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected Task createTask(final AdaptedSelectionInfo selectionInfo) {
        return new UndoPendingChangesTask(
            getShell(),
            selectionInfo.getRepositories()[0],
            selectionInfo.getPendingChanges());
    }
}
