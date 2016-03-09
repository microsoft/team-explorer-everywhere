// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.candidates;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.dialogs.vc.PromoteCandidateChangesDialog;
import com.microsoft.tfs.client.common.ui.helpers.ViewFileHelper;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;

public class ViewLocalFolderAction extends CandidateAction {
    public ViewLocalFolderAction(final PromoteCandidateChangesDialog dialog) {
        super(dialog);
        setText(Messages.getString("ViewLocalFolderAction.ViewLocalFolderActionText")); //$NON-NLS-1$
    }

    @Override
    public void doRun() {
        final ChangeItem change = (ChangeItem) ((IStructuredSelection) getSelection()).getFirstElement();
        final String folder = change.getFolder();
        if (folder != null && !ServerPath.isServerPath(folder)) {
            final IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            ViewFileHelper.viewLocalFileOrFolder(folder, workbenchPage, true);
        }
    }

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        return selection.size() == 1;
    }
}