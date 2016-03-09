// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.tasks.vc.LockTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions.helper.ActionEnablementHelper;

public class LockAction extends TeamViewerAction {
    private TFSItem[] items;

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
             * Lock is enabled for any selection that doesn't contain deleted
             * items. This includes selections of items which are all already
             * locked.
             */
            action.setEnabled(ActionEnablementHelper.selectionContainsDeletedItem(selection) == false);
        } else {
            action.setEnabled(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        /*
         * It's important to pass the paths that were selected without expanding
         * them recursively, because TFS supports locks directly on a folder
         * (unlike a normal edit pending change) and these should be
         * non-recursive.
         */
        new LockTask(getShell(), getCurrentRepository(), TFSItemUtils.getTypedItemSpecs(items)).run();
    }

}
