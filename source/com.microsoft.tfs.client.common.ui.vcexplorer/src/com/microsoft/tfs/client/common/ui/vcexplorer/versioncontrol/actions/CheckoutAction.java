// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vcexplorer.versioncontrol.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import com.microsoft.tfs.client.common.ui.tasks.vc.CheckoutWithPromptTask;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItemUtils;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

/**
 * Action called to check out a file or folder.
 */
public class CheckoutAction extends TeamViewerAction {
    private TFSItem[] items;

    /**
     * {@inheritDoc}
     */
    @Override
    public void doRun(final IAction action) {
        final CheckoutWithPromptTask task =
            new CheckoutWithPromptTask(getShell(), getCurrentRepository(), TFSItemUtils.getTypedItemSpecs(items));
        task.run();
    }

    @Override
    protected void onSelectionChanged(final IAction action, final ISelection selection) {
        if (action.isEnabled() == false) {
            return;
        }

        items = (TFSItem[]) adaptSelectionToArray(TFSItem.class);

        action.setEnabled(items != null && items.length > 0 && allItemsMapped(items) && canCheckout(items));
    }

    protected boolean allItemsMapped(final TFSItem[] items) {
        try {
            for (int i = 0; i < items.length; i++) {
                if (items[i].getMappedLocalPath() == null) {
                    return false;
                }
            }
        } catch (final PathTooLongException e) {
            return false;
        }

        return true;
    }

    protected boolean canCheckout(final TFSItem[] items) {
        final ChangeType disallowedChangeTypes = ChangeType.EDIT.combine(ChangeType.DELETE);

        for (int i = 0; i < items.length; i++) {
            /*
             * All objects must not have a pending change that is an edit,
             * delete, or undelete.
             */
            final PendingChange pendingChange =
                getCurrentRepository().getPendingChangeCache().getPendingChangeByServerPath(items[i].getFullPath());

            if (pendingChange != null && pendingChange.getChangeType().containsAny(disallowedChangeTypes)) {
                return false;
            }

            /*
             * Also disallow checkout if any items are non-local. Only items
             * which are local may be checked out.
             */
            if (items[i].isLocal() == false) {
                return false;
            }
        }

        /*
         * All selected objects passed.
         */
        return true;
    }
}
