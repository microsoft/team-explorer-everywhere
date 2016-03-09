// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.vc.UndoPendingChangesTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class UndoPendingChangesAction extends TeamViewerAction {
    private TFSItem[] items;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final TFSRepository repository = getCurrentRepository();

        final List allChanges = new ArrayList();

        for (int i = 0; i < items.length; i++) {
            final PendingChange[] changes = items[i].getPendingChanges(true);

            if (changes != null) {
                for (int j = 0; j < changes.length; j++) {
                    allChanges.add(changes[j]);
                }
            }
        }

        if (allChanges.size() == 0) {
            return;
        }

        final UndoPendingChangesTask undoTask = new UndoPendingChangesTask(
            getShell(),
            repository,
            (PendingChange[]) allChanges.toArray(new PendingChange[allChanges.size()]));

        undoTask.run();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);

        action.setEnabled(ActionEnablementHelper.selectionContainsPendingChanges(selection, true));
    }
}
