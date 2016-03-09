// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.vc;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.Task;
import com.microsoft.tfs.client.common.ui.tasks.vc.ShelveWithPromptTask;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;

public class ShelveAction extends CheckinShelveUndoAction {
    public ShelveAction() {
        super();
        setName("&Shelve Pending Changes..."); //$NON-NLS-1$
    }

    @Override
    protected Task createTask(final AdaptedSelectionInfo selectionInfo) {
        final TFSRepository repository = selectionInfo.getRepositories()[0];
        return new ShelveWithPromptTask(getShell(), repository, selectionInfo.getPendingChanges());
    }
}