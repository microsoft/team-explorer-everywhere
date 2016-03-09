// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

import java.io.File;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public class PendingChangesTreeDoubleClickListener implements IDoubleClickListener {
    @Override
    public void doubleClick(final DoubleClickEvent event) {
        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        final Object element = selection.getFirstElement();

        if (element instanceof PendingChangesTreeNode) {
            final PendingChangesTreeNode node = (PendingChangesTreeNode) element;
            final PendingChange pendingChange = node.getPendingChange();

            if (pendingChange != null && pendingChange.getLocalItem() != null) {
                final String localPath = pendingChange.getLocalItem();
                final File file = new File(localPath);

                if (file.exists()) {
                    final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    ViewFileHelper.viewLocalFileOrFolder(localPath, page, false);
                }
            }
        }
    }
}