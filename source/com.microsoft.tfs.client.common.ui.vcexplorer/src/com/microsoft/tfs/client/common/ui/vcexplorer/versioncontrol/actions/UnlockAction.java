// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.vc.UnlockTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class UnlockAction extends TeamViewerAction {
    private TFSItem[] items;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        /*
         * Get the locked items affected by the selection (including locks on
         * children if a folder is selected).
         */
        final TFSItem[] lockedItems = getLockedItemsRecursive(items);

        if (lockedItems.length > 0) {
            new UnlockTask(getShell(), getCurrentRepository(), TFSItemUtils.getTypedItemSpecs(lockedItems)).run();
        }
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

        final TFSRepository repository = getCurrentRepository();

        if (repository != null) {
            /*
             * Unlock is enabled for any selection that contains a locked item,
             * recursively.
             */
            action.setEnabled(
                ActionEnablementHelper.selectionContainsLockedItem(selection, repository.getPendingChangeCache()));
        } else {
            action.setEnabled(false);
        }
    }

    /**
     * Loop through the passed resource array and return resources which have a
     * lock pending change or a child resource with a lock pending change.
     */
    protected TFSItem[] getLockedItemsRecursive(final TFSItem[] items) {
        final ArrayList lockedResources = new ArrayList();

        for (int i = 0; i < items.length; i++) {
            final PendingChange[] changes =
                getCurrentRepository().getPendingChangeCache().getPendingChangesByServerPathRecursive(
                    items[i].getFullPath());

            for (int j = 0; j < changes.length; j++) {
                if (changes[j].getChangeType().contains(ChangeType.LOCK)) {
                    lockedResources.add(items[i]);
                }
            }
        }

        return (TFSItem[]) lockedResources.toArray(new TFSItem[lockedResources.size()]);
    }
}
