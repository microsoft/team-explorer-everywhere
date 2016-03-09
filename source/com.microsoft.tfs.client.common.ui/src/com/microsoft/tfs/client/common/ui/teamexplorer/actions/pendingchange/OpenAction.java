// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.pendingchange;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;

public class OpenAction extends BaseAction {
    private PendingChange selectedPendingChange;

    @Override
    public void onSelectionChanged(final IAction action, final ISelection selection) {
        super.onSelectionChanged(action, selection);

        if (getSelectionSize() != 1) {
            action.setEnabled(false);
            return;
        }

        selectedPendingChange = getSelectedPendingChange();

        if (selectedPendingChange != null
            && selectedPendingChange.getItemType() == ItemType.FILE
            && selectedPendingChange.getLocalItem() != null) {
            action.setEnabled(true);
        } else {
            action.setEnabled(false);
        }
    }

    @Override
    public void doRun(final IAction action) {
        Check.notNull(selectedPendingChange, "selectedPendingChange"); //$NON-NLS-1$

        final IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        ViewFileHelper.viewLocalFileOrFolder(selectedPendingChange.getLocalItem(), workbenchPage, false);
    }
}
