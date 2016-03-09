// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import com.microsoft.tfs.client.common.ui.tasks.Task;
import com.microsoft.tfs.client.common.ui.tasks.vc.CheckinTask;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class CheckinAction extends CheckinShelveUndoAction {
    public CheckinAction() {
        super();
        setName(Messages.getString("CheckinAction.ActionName")); //$NON-NLS-1$
    }

    @Override
    protected Task createTask(final AdaptedSelectionInfo selectionInfo) {
        return new CheckinTask(getShell(), selectionInfo.getRepositories()[0], selectionInfo.getPendingChanges(), null);
    }
}